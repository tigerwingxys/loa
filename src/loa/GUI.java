/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static loa.Piece.*;

/**
 * The GUI controller for a LOA board and buttons.
 * 
 * @author
 */
class GUI extends TopLevel implements View, Reporter {

	/** Minimum size of board in pixels. */
	private static final int MIN_SIZE = 500;

	/** Size of pane used to contain help text. */
	static final Dimension TEXT_BOX_SIZE = new Dimension(500, 700);

	/** Resource name of "About" message. */
	static final String ABOUT_TEXT = "loa/About.html";

	/** Resource name of Loa help text. */
	static final String HELP_TEXT = "loa/Help.html";

	/** A new window with given TITLE providing a view of a Loa board. */
	GUI(String title) {
		super(title, true);
		addMenuButton("Game->New", this::newGame);
		addSeparator("Game");
		addMenuButton("Set->Auto", this::autoPlayer);
		addMenuButton("Set->Manual", this::manualPlayer);
		addMenuButton("Set->Seed", this::setSeed);
		addMenuButton("Set->Limit", this::setLimit);
		addMenuButton("Set->Depth", this::setDepth);
		addMenuButton("Game->Quit", this::quit);
		addMenuButton("Help->About", (s) -> displayText("About", ABOUT_TEXT));
		addMenuButton("Help->Loa", (s) -> displayText("Loa Help", HELP_TEXT));
		// FIXME: Other controls?

		_widget = new BoardWidget(_pendingCommands);
		add(_widget, new LayoutSpec("y", 1, "height", 1, "width", 3));
		addLabel("To move: White", "CurrentTurn",
				new LayoutSpec("x", 0, "y", 0, "height", 1, "width", 3));
		// FIXME: Other components?
	}

	/** Pattern describing the 'seed' command's arguments. */
	private static final Pattern SEED_PATN = Pattern
			.compile("\\s*(-?\\d{1,18})\\s*$");

	private static final Pattern PLAYER_PATN = Pattern
			.compile("\\s*([a-zA-Z]{5})\\s*$");
	private static final Pattern LIMIT_PATN = Pattern
			.compile("\\s*(\\d{1,3})\\s*$");
	private static final Pattern DEPTH_PATN = Pattern
			.compile("\\s*(\\d{1})\\s*$");

	private void autoPlayer(String dummy) {
		String response = getTextInput("Enter a color.", "New color", "plain",
				"");
		if (response != null) {
			Matcher mat = PLAYER_PATN.matcher(response);
			if (mat.matches()) {
				_pendingCommands.offer(
						String.format("auto %s", mat.group(1).toLowerCase()));
			} else {
				showMessage("Enter a color in white or black.", "Error",
						"error");
			}
		}

	}

	private void manualPlayer(String dummy) {
		String response = getTextInput("Enter a color.", "New color", "plain",
				"");
		if (response != null) {
			Matcher mat = PLAYER_PATN.matcher(response);
			if (mat.matches()) {
				_pendingCommands.offer(
						String.format("manual %s", mat.group(1).toLowerCase()));
			} else {
				showMessage("Enter a color in white or black.", "Error",
						"error");
			}
		}

	}

	private void setSeed(String dummy) {
		String response = getTextInput("Enter new random seed.", "New seed",
				"plain", "");
		if (response != null) {
			Matcher mat = SEED_PATN.matcher(response);
			if (mat.matches()) {
				_pendingCommands.offer(String.format("SEED %s", mat.group(1)));
			} else {
				showMessage("Enter an integral seed value.", "Error", "error");
			}
		}

	}

	private void setLimit(String dummy) {
		String response = getTextInput("Enter the move limit.", "New limit",
				"plain", "60");
		if (response != null) {
			Matcher mat = LIMIT_PATN.matcher(response);
			if (mat.matches()) {
				_pendingCommands.offer(String.format("limit %s", mat.group(1)));
			} else {
				showMessage("Enter an integral limit value.", "Error", "error");
			}
		}
	}

	private void setDepth(String dummy) {
		String response = getTextInput("Enter the depth.", "New depth", "plain",
				"0");
		if (response != null) {
			Matcher mat = DEPTH_PATN.matcher(response);
			if (mat.matches()) {
				_pendingCommands.offer(String.format("depth %s", mat.group(1)));
			} else {
				showMessage("Enter an integral depth value.", "Error", "error");
			}
		}
	}

	/** Response to "Quit" button click. */
	private void quit(String dummy) {
		_pendingCommands.offer("quit");
	}

	/** Response to "New Game" button click. */
	private void newGame(String dummy) {
		_pendingCommands.offer("new");
	}

	/**
	 * Return the next command from our widget, waiting for it as necessary. The
	 * BoardWidget uses _pendingCommands to queue up moves that it receives.
	 * Thie class uses _pendingCommands to queue up commands that are generated
	 * by clicking on menu items.
	 */
	String readCommand() {
		try {
			_widget.setMoveCollection(true);
			String cmnd = _pendingCommands.take();
			_widget.setMoveCollection(false);
			return cmnd;
		} catch (InterruptedException excp) {
			throw new Error("unexpected interrupt");
		}
	}

	@Override
	public void update(Game controller) {
		Board board = controller.getBoard();

		_widget.update(board);
		if (board.winner() != null) {
			setLabel("CurrentTurn",
					String.format("Winner: %s", board.winner().fullName()));
		} else {
			setLabel("CurrentTurn",
					String.format("To move: %s", board.turn().fullName()));
		}

		boolean manualWhite = controller.manualWhite(),
				manualBlack = controller.manualBlack();
		// FIXME: More?
	}

	/**
	 * Display text in resource named TEXTRESOURCE in a new window titled TITLE.
	 */
	private void displayText(String title, String textResource) {
		/*
		 * Implementation note: It would have been more convenient to avoid
		 * having to read the resource and simply use dispPane.setPage on the
		 * resource's URL. However, we wanted to use this application with a
		 * nonstandard ClassLoader, and arranging for straight Java to
		 * understand non-standard URLS that access such a ClassLoader turns out
		 * to be a bit more trouble than it's worth.
		 */
		JFrame frame = new JFrame(title);
		JEditorPane dispPane = new JEditorPane();
		dispPane.setEditable(false);
		dispPane.setContentType("text/html");
		InputStream resource = GUI.class.getClassLoader()
				.getResourceAsStream(textResource);
		StringWriter text = new StringWriter();
		try {
			while (true) {
				int c = resource.read();
				if (c < 0) {
					dispPane.setText(text.toString());
					break;
				}
				text.write(c);
			}
		} catch (IOException e) {
			return;
		}
		JScrollPane scroller = new JScrollPane(dispPane);
		scroller.setVerticalScrollBarPolicy(scroller.VERTICAL_SCROLLBAR_ALWAYS);
		scroller.setPreferredSize(TEXT_BOX_SIZE);
		frame.add(scroller);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void reportError(String fmt, Object... args) {
		showMessage(String.format(fmt, args), "Loa Error", "error");
	}

	@Override
	public void reportNote(String fmt, Object... args) {
		showMessage(String.format(fmt, args), "Loa Message", "information");
	}

	@Override
	public void reportMove(Move unused) {
	}

	/** The board widget. */
	private BoardWidget _widget;

	/**
	 * Queue of pending commands resulting from menu clicks and moves on the
	 * board. We use a blocking queue because the responses to clicks on the
	 * board and on menus happen in parallel to the methods that call
	 * readCommand, which therefore needs to wait for clicks to happen.
	 */
	private ArrayBlockingQueue<String> _pendingCommands = new ArrayBlockingQueue<>(
			5);

}
