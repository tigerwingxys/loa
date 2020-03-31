/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import static loa.Piece.*;
import static loa.Square.BOARD_SIZE;
import loa.Board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An automated Player.
 * 
 * @author ChengXu
 */
class MachinePlayer extends Player {

	/**
	 * A position-score magnitude indicating a win (for white if positive, black
	 * if negative).
	 */
	private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
	/** A magnitude greater than a normal value. */
	private static final int INFTY = Integer.MAX_VALUE;

	/**
	 * A new MachinePlayer with no piece or controller (intended to produce a
	 * template).
	 */
	MachinePlayer() {
		this(null, null);
	}

	/** A MachinePlayer that plays the SIDE pieces in GAME. */
	MachinePlayer(Piece side, Game game) {
		super(side, game);
	}

	@Override
	String getMove() {
		Move choice;

		assert side() == getGame().getBoard().turn();
		int depth;
		choice = searchForMove();
		getGame().reportMove(choice);
		return choice.toString();
	}

	@Override
	Player create(Piece piece, Game game) {
		return new MachinePlayer(piece, game);
	}

	@Override
	boolean isManual() {
		return false;
	}

	/**
	 * Return a move after searching the game tree to DEPTH>0 moves from the
	 * current position. Assumes the game is not over.
	 */
	private Move searchForMove() {
		Board work = new Board(getBoard());
		int value;
		assert side() == work.turn();
		_foundMove = null;
		if (side() == WP) {
			value = findMove(work, chooseDepth(), true, 1, -INFTY, INFTY);
		} else {
			value = findMove(work, chooseDepth(), true, -1, -INFTY, INFTY);
		}
		return _foundMove;
	}

	/**
	 * Find a move from position BOARD and return its value, recording the move
	 * found in _foundMove iff SAVEMOVE. The move should have maximal value or
	 * have value > BETA if SENSE==1, and minimal value or value < ALPHA if
	 * SENSE==-1. Searches up to DEPTH levels. Searching at level 0 simply
	 * returns a static estimate of the board value and does not set _foundMove.
	 * If the game is over on BOARD, does not set _foundMove.
	 */
	private int findMove(Board board, int depth, boolean saveMove, int sense,
			int alpha, int beta) {
		int result = beta;
		if (depth == 0) {
			HashMap<String, Object> rHashMap = guessBestMove(board);
			result = (int) (rHashMap.get("value"));
			if (saveMove) {
				_foundMove = (Move) rHashMap.get("bestMove");
			}
			return result;
		}
		int response = 0;
		int bestDistance = Integer.MAX_VALUE;
		int bestValue = board.valueBPMinusWP();
		Square centreSquare = board.centreSquare(board.turn());
		List<Move> moves = board.legalMoves();
		for (Move move : moves) {
			if (move.getFrom().distance(centreSquare) <= 2) {
				continue;
			}
			int fromValue = board.distancePower(centreSquare, move.getFrom());
			int toValue = board.distancePower(centreSquare, move.getTo());
			if (fromValue == 0 || toValue >= fromValue) {
				continue;
			}
			int afterDistance = toValue * BOARD_SIZE * BOARD_SIZE / fromValue;
			if (afterDistance >= bestDistance) {
				continue;
			}
			bestDistance = afterDistance;

			board.makeMove(move);
			response = findMove(board, depth - 1, false, -sense, alpha, beta);
			Utils.debug(1,
					"findMove best[%d]resp[%d]move[%s] depth:%d, sense:%d, alpha:%d, beta:%d%n",
					bestValue, response, board.lastMove(), depth, sense, alpha,
					beta);
			if (sense == 1) {
				if (response > bestValue) {
					bestValue = response;
					alpha = Math.max(alpha, response);
					if (saveMove) {
						_foundMove = board.lastMove();
					}
					if (alpha >= beta) {
						board.retract();
						break;
					}
				}
			} else {
				if (response < bestValue) {
					bestValue = response;
					beta = Math.min(beta, response);
					if (saveMove) {
						_foundMove = board.lastMove();
					}
					if (alpha >= beta) {
						board.retract();
						break;
					}
				}
			}
			board.retract();
		}

		if (saveMove && _foundMove == null) {
			_foundMove = moves.get(moves.size() / 2);
		}

		return bestValue; // FIXME
	}

	/** Return a search depth for the current position. */
	private int chooseDepth() {
		return getGame().getDepth();
	}

	// FIXME: Other methods, variables here.
	private HashMap<String, Object> guessBestMove(Board board) {
		HashMap<String, Object> rHashMap = new HashMap<>();
		int bestvalue = Integer.MAX_VALUE;
		Square centreSquare = board.centreSquare(board.turn());

		Move bestMove = null;
		int result = 0;
		ArrayList<ArrayList<Square>> dList = board.getClusters(board.turn());
		OUTER: for (int i = dList.size() - 1; i > 0; i--) {
			ArrayList<Square> aList = dList.get(i);
			for (Square square : aList) {
				for (Move aMove : board.legalMoves(square)) {
					if (aList.size() <= 2) {
						board.makeMove(aMove);
						if (board.winner() == board.turn().opposite()) {
							bestMove = aMove;
							board.retract();
							break OUTER;
						}
						board.retract();
					}
					if (aMove.getFrom().distance(centreSquare) <= 2) {
						continue;
					}
					int fromValue = board.distancePower(centreSquare,
							aMove.getFrom());
					int toValue = board.distancePower(centreSquare,
							aMove.getTo());
					if (fromValue == 0 || toValue >= fromValue) {
						continue;
					}
					int afterValue = toValue * BOARD_SIZE * BOARD_SIZE
							/ fromValue;
					if (afterValue < bestvalue) {
						bestvalue = afterValue;
						bestMove = aMove;
					}
					if (board.get(aMove.getTo()) == board.turn().opposite()) {
						bestMove = aMove;
						break OUTER;
					}

				}
			}

		}
		if (bestMove == null) {
			ArrayList<Square> bigCluster = dList.get(0);
			Square s = bigCluster.get(getGame().randInt(bigCluster.size()));
			List<Move> nextMoves = board.legalMoves(s);
			for (Move move : nextMoves) {
				if (board.distancePower(centreSquare, move.getFrom()) > board
						.distancePower(centreSquare, move.getTo())) {
					bestMove = move;
				}
			}
			if (bestMove == null) {
				bestMove = nextMoves.get(getGame().randInt(nextMoves.size()));
			}
		}
		board.makeMove(bestMove);
		result = board.valueBPMinusWP();
		board.retract();
		rHashMap.put("value", result);
		rHashMap.put("bestMove", bestMove);
		return rHashMap;
	}

	/** Used to convey moves discovered by findMove. */
	private Move _foundMove;

}
