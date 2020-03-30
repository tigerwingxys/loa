/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/**
 * Represents the state of a game of Lines of Action.
 * 
 * @author
 */
class Board {

	/** Default number of moves for each side that results in a draw. */
	static final int DEFAULT_MOVE_LIMIT = 60;

	/** Pattern describing a valid square designator (cr). */
	static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

	/**
	 * A Board whose initial contents are taken from INITIALCONTENTS and in
	 * which the player playing TURN is to move. The resulting Board has
	 * get(col, row) == INITIALCONTENTS[row][col] Assumes that PLAYER is not
	 * null and INITIALCONTENTS is 8x8.
	 *
	 * CAUTION: The natural written notation for arrays initializers puts the
	 * BOTTOM row of INITIALCONTENTS at the top.
	 */
	Board(Piece[][] initialContents, Piece turn) {
		initialize(initialContents, turn);
	}

	/** A new board in the standard initial position. */
	Board() {
		this(INITIAL_PIECES, BP);
	}

	/**
	 * A Board whose initial contents and state are copied from BOARD.
	 */
	Board(Board board) {
		this();
		copyFrom(board);
	}

	/** Set my state to CONTENTS with SIDE to move. */
	void initialize(Piece[][] contents, Piece side) {
		// FIXME
		_turn = side;
		_moveLimit = DEFAULT_MOVE_LIMIT;
		_winnerKnown = false;
		_winner = null;
		_subsetsInitialized = false;
		_blackPieces.clear();
		_whitePieces.clear();
		_moves.clear();
		_blackClusters.clear();
		_whiteClusters.clear();
		_blackRegionSizes.clear();
		_whiteRegionSizes.clear();

		for (int r = 0; r < contents.length; r++) {
			for (int c = 0; c < contents[r].length; c++) {
				Piece piece = contents[r][c];
				int idx = sq(c, r).index();
				_board[idx] = piece;
				if (piece != EMP) {
					getPieces(piece).add(idx);
				}
			}
		}
		computeRegions();
	}

	/** Set me to the initial configuration. */
	void clear() {
		initialize(INITIAL_PIECES, BP);
	}

	/** Set my state to a copy of BOARD. */
	void copyFrom(Board board) {
		if (board == this) {
			return;
		}
		// FIXME
		_turn = board._turn;
		_moveLimit = board._moveLimit;
		_winner = board._winner;
		_winnerKnown = board._winnerKnown;
		_subsetsInitialized = false;
		_blackPieces.clear();
		_whitePieces.clear();
		_moves.clear();
		_blackClusters.clear();
		_whiteClusters.clear();
		_blackRegionSizes.clear();
		_whiteRegionSizes.clear();
		for (int i = 0; i < board._board.length; i++) {
			_board[i] = board._board[i];
			if (_board[i] != EMP) {
				getPieces(_board[i]).add(i);
			}
		}
		_moves.addAll(board._moves);
		computeRegions();
	}

	/** Return the contents of the square at SQ. */
	Piece get(Square sq) {
		return _board[sq.index()];
	}

	/**
	 * Set the square at SQ to V and set the side that is to move next to NEXT,
	 * if NEXT is not null.
	 */
	void set(Square sq, Piece v, Piece next) {
		// FIXME
		_board[sq.index()] = v;
		if (next != null) {
			_turn = next;
		}
	}

	/**
	 * Set the square at SQ to V, without modifying the side that moves next.
	 */
	void set(Square sq, Piece v) {
		set(sq, v, null);
	}

	/** Set limit on number of moves (before tie results) to LIMIT. */
	void setMoveLimit(int limit) {
		_moveLimit = limit;
		_winnerKnown = false;
	}

	/**
	 * Assuming isLegal(MOVE), make MOVE. Assumes MOVE.isCapture() is false.
	 */
	void makeMove(Move move) {
		assert isLegal(move);
		// FIXME
		set(move.getFrom(), EMP);
		getPieces(_turn).remove(move.getFrom().index());
		boolean capture = get(move.getTo()) == _turn.opposite();
		set(move.getTo(), _turn);
		getPieces(_turn).add(move.getTo().index());
		if (capture) {
			getPieces(_turn.opposite()).remove(move.getTo().index());
		}
		move = capture ? move.captureMove() : move;
		_moves.add(move);
		_turn = _turn.opposite();
		_subsetsInitialized = false;
	}

	/**
	 * Retract (unmake) one move, returning to the state immediately before that
	 * move. Requires that movesMade () > 0.
	 */
	void retract() {
		assert movesMade() > 0;
		// FIXME
		Move lastMove = lastMove();
		_turn = _turn.opposite();
		if (lastMove.isCapture()) {
			set(lastMove.getTo(), _turn.opposite());
			getPieces(_turn.opposite()).add(lastMove.getTo().index());
		} else {
			set(lastMove.getTo(), EMP);
		}
		getPieces(_turn).remove(lastMove.getTo().index());
		getPieces(_turn).add(lastMove.getFrom().index());
		set(lastMove.getFrom(), _turn);
		_moves.remove(_moves.size() - 1);
		_subsetsInitialized = false;
	}

	Move lastMove() {
		return _moves.get(_moves.size() - 1);
	}

	/** Return the Piece representing who is next to move. */
	Piece turn() {
		return _turn;
	}

	/**
	 * Return true iff FROM - TO is a legal move for the player currently on
	 * move.
	 */
	boolean isLegal(Square from, Square to) {
		if (to == null) {
			return false;
		}
		if (!from.isValidMove(to)) {
			return false;
		}
		if (blocked(from, to)) {
			return false;
		}
		if (from.distance(to) != moveSteps(from, to)) {
			return false;
		}
		return true; // FIXME
	}

	/**
	 * Return true iff MOVE is legal for the player currently on move. The
	 * isCapture() property is ignored.
	 */
	boolean isLegal(Move move) {
		return isLegal(move.getFrom(), move.getTo());
	}

	/** Return a sequence of all legal moves from this SQUARE. */
	List<Move> legalMoves(Square square) {
		ArrayList<Move> aList = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			int steps = moveSteps(square, i);
			Square to1 = square.moveDest(i, steps);
			Square to2 = square.moveDest(i + 4, steps);
			if (isLegal(square, to1)) {
				aList.add(Move.mv(square, to1, get(to1) == _turn.opposite()));
			}
			if (isLegal(square, to2)) {
				aList.add(Move.mv(square, to2, get(to2) == _turn.opposite()));
			}
		}
		return aList; // FIXME
	}

	/** Return a sequence of all legal moves from this turn. */
	List<Move> legalMoves() {
		ArrayList<Move> arrayList = new ArrayList<>();
		for(Integer integer:getPieces(turn())) {
			Square square = squareByIndex(integer.intValue());
			arrayList.addAll(legalMoves(square));
		}
		return arrayList;
	}

	/**
	 * Return true iff the game is over (either player has all his pieces
	 * continguous or there is a tie).
	 */
	boolean gameOver() {
		return winner() != null;
	}

	/** Return true iff SIDE's pieces are continguous. */
	boolean piecesContiguous(Piece side) {
		return getRegionSizes(side).size() == 1;
	}

	/**
	 * Return the winning side, if any. If the game is not over, result is null.
	 * If the game has ended in a tie, returns EMP.
	 */
	Piece winner() {
		if (_winnerKnown) {
			return _winner;
		}
		computeRegions();
		if (piecesContiguous(_turn.opposite())) {
			_winner = _turn.opposite();
			_winnerKnown = true;
		} else if (piecesContiguous(_turn)) {
			_winner = _turn;
			_winnerKnown = true;
		} else if (movesMade() >= DEFAULT_MOVE_LIMIT) {
			_winner = EMP;
			_winnerKnown = true;
		}

		return _winner;
	}

	/**
	 * Return the total number of moves that have been made (and not retracted).
	 * Each valid call to makeMove with a normal move increases this number by
	 * 1.
	 */
	int movesMade() {
		return _moves.size();
	}

	@Override
	public boolean equals(Object obj) {
		Board b = (Board) obj;
		return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
	}

	@Override
	public String toString() {
		Formatter out = new Formatter();
		out.format("===\n");
		for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
			out.format("    ");
			for (int c = 0; c < BOARD_SIZE; c += 1) {
				out.format("%s ", get(sq(c, r)).abbrev());
			}
			out.format("\n");
		}
		out.format("Next move: %s\n===", turn().fullName());
		return out.toString();
	}

	/**
	 * Return true if a move from FROM to TO is blocked by an opposing piece or
	 * by a friendly piece on the target square.
	 */
	private boolean blocked(Square from, Square to) {
		if (get(from) != _turn) {
			return true;
		}
		if (get(to) == _turn) {
			return true;
		}
		int steps = moveSteps(from, to);
		int dir = from.direction(to);
		for (int i = 1; i < steps; i++) {
			if (get(from.moveDest(dir, i)) == _turn.opposite()) {
				return true;
			}
		}
		return false; // FALSE
	}

	/**
	 * Return the size of the as-yet unvisited cluster of squares containing P
	 * at and adjacent to SQ. VISITED indicates squares that have already been
	 * processed or are in different clusters. Update VISITED to reflect squares
	 * counted.
	 */
	private int numContig(Square sq, boolean[][] visited, Piece p,
			ArrayList<Square> aList) {
		visited[sq.col()][sq.row()] = true;
		Square[] adjacentSquares = sq.adjacent();
		HashSet<Integer> pieceSet = getPieces(p);
		aList.add(sq);
		int result = 1;
		for (Square s : adjacentSquares) {
			if (!visited[s.col()][s.row()] && pieceSet.contains(s.index())
					&& _board[s.index()] == p) {
				result += numContig(s, visited, p, aList);
			}
		}
		return result; // FIXME
	}

	/** Set the values of _whiteRegionSizes and _blackRegionSizes. */
	private void computeRegions() {
		if (_subsetsInitialized) {
			return;
		}
		_whiteRegionSizes.clear();
		_blackRegionSizes.clear();
		_blackClusters.clear();
		_whiteClusters.clear();
		// FIXME
		boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
		for (Integer I : _blackPieces) {
			int idx = I.intValue();
			int col = idx % BOARD_SIZE;
			int row = idx / BOARD_SIZE;
			if (visited[col][row]) {
				continue;
			}
			ArrayList<Square> arrayList = new ArrayList<>();
			_blackRegionSizes
					.add(numContig(sq(col, row), visited, BP, arrayList));
			_blackClusters.add(arrayList);
		}
		for (Integer I : _whitePieces) {
			int idx = I.intValue();
			int col = idx % BOARD_SIZE;
			int row = idx / BOARD_SIZE;
			if (visited[col][row]) {
				continue;
			}
			ArrayList<Square> arrayList = new ArrayList<>();
			_whiteRegionSizes
					.add(numContig(sq(col, row), visited, WP, arrayList));
			_whiteClusters.add(arrayList);
		}

		Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
		Collections.sort(_blackRegionSizes, Collections.reverseOrder());
		Collections.sort(_whiteClusters, new Comparator<ArrayList<Square>>() {
			public int compare(ArrayList<Square> a, ArrayList<Square> b) {
				return b.size() - a.size();
			}
		});
		Collections.sort(_blackClusters, new Comparator<ArrayList<Square>>() {
			public int compare(ArrayList<Square> a, ArrayList<Square> b) {
				return b.size() - a.size();
			}
		});
		_subsetsInitialized = true;
	}

	/**
	 * Return the sizes of all the regions in the current union-find structure
	 * for side S.
	 */
	List<Integer> getRegionSizes(Piece s) {
		computeRegions();
		if (s == WP) {
			return _whiteRegionSizes;
		} else {
			return _blackRegionSizes;
		}
	}

	/** Return the HashSet of all pieces, by color */
	HashSet<Integer> getPieces(Piece p) {
		if (p == BP) {
			return _blackPieces;
		} else if (p == WP) {
			return _whitePieces;
		}
		return null;
	}

	// FIXME: Other methods, variables?
	/** Return the steps, from SQ in DIR direction */
	int moveSteps(Square sq, int dir) {
		int result = get(sq) == EMP ? 0 : 1;
		for (Square s = sq.moveDest(dir, 1); s != null; s = s.moveDest(dir,
				1)) {
			result += get(s) == EMP ? 0 : 1;
		}
		dir = (dir + 4) % BOARD_SIZE;
		for (Square s = sq.moveDest(dir, 1); s != null; s = s.moveDest(dir,
				1)) {
			result += get(s) == EMP ? 0 : 1;
		}
		return result;
	}

	/** Return the steps, from FROM to TO */
	int moveSteps(Square from, Square to) {
		return moveSteps(from, from.direction(to));
	}

	/** Return the steps, by Move M */
	int moveSteps(Move m) {
		return moveSteps(m.getFrom(), m.getTo());
	}

	/** 更应该偏向棋子多的一方 */
	Square centreSquare(Piece piece) {
		int c = 0, r = 0;
		HashSet<Integer> aSet = getPieces(piece);
		for (Integer integer : aSet) {
			Square s = squareByIndex(integer.intValue());
			c += s.col();
			r += s.row();
		}
		return sq(c / aSet.size(), r / aSet.size());
	}

	/** Return the value of the position, by color PIECE */
	int value(Piece piece) {
		computeRegions();
		ArrayList<ArrayList<Square>> dList = getClusters(piece);
		if (dList.size() < 2) {
			return 0;
		}
		ArrayList<Square> centreList = new ArrayList<>();
		for (ArrayList<Square> arrayList : dList) {
			centreList.add(clusterCentre(arrayList));
		}
		Square centreSquare = centreSquare(piece);
		int distance = 0;
		for (int i = 0; i < centreList.size(); i++) {
			distance += (distancePower(centreSquare, centreList.get(i))
					* dList.get(i).size());
		}
		return distance + 1;
	}

	int valueBPMinusWP() {
		return value(BP) - value(WP);
	}

	int distancePower(Square centre, Square square) {
		int colDiff = centre.col() - square.col();
		int rowDiff = centre.row() - square.row();
		return colDiff * colDiff + rowDiff * rowDiff;
	}

	ArrayList<ArrayList<Square>> getClusters(Piece piece) {
		computeRegions();
		ArrayList<ArrayList<Square>> dList = new ArrayList<>();
		dList.addAll(piece == BP ? _blackClusters : _whiteClusters);
		return dList;
	}

	Square clusterCentre(ArrayList<Square> arrayList) {
		int c = 0, r = 0;
		for (Square square : arrayList) {
			c += square.col();
			r += square.row();
		}
		c = c / arrayList.size();
		r = r / arrayList.size();
		return sq(c, r);
	}


	/**
	 * The standard initial configuration for Lines of Action (bottom row
	 * first).
	 */
	static final Piece[][] INITIAL_PIECES = {
			{ EMP, BP, BP, BP, BP, BP, BP, EMP },
			{ WP, EMP, EMP, EMP, EMP, EMP, EMP, WP },
			{ WP, EMP, EMP, EMP, EMP, EMP, EMP, WP },
			{ WP, EMP, EMP, EMP, EMP, EMP, EMP, WP },
			{ WP, EMP, EMP, EMP, EMP, EMP, EMP, WP },
			{ WP, EMP, EMP, EMP, EMP, EMP, EMP, WP },
			{ WP, EMP, EMP, EMP, EMP, EMP, EMP, WP },
			{ EMP, BP, BP, BP, BP, BP, BP, EMP } };

	/** Current contents of the board. Square S is at _board[S.index()]. */
	private final Piece[] _board = new Piece[BOARD_SIZE * BOARD_SIZE];

	/** List of all unretracted moves on this board, in order. */
	private final ArrayList<Move> _moves = new ArrayList<>();
	/** Current side on move. */
	private Piece _turn;
	/** Limit on number of moves before tie is declared. */
	private int _moveLimit;
	/** True iff the value of _winner is known to be valid. */
	private boolean _winnerKnown;
	/**
	 * Cached value of the winner (BP, WP, EMP (for tie), or null (game still in
	 * progress). Use only if _winnerKnown.
	 */
	private Piece _winner;

	/** True iff subsets computation is up-to-date. */
	private boolean _subsetsInitialized;

	/** List of the sizes of continguous clusters of pieces, by color. */
	private final ArrayList<Integer> _whiteRegionSizes = new ArrayList<>(),
			_blackRegionSizes = new ArrayList<>();

	/** BitSets of all pieces, by color */
	private final HashSet<Integer> _blackPieces = new HashSet<>(),
			_whitePieces = new HashSet<>();
	private final ArrayList<ArrayList<Square>> _blackClusters = new ArrayList<>(),
			_whiteClusters = new ArrayList<>();

}
