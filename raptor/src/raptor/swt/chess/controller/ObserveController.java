/**
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright (c) 2009, RaptorProject (http://code.google.com/p/raptor-chess-interface/)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package raptor.swt.chess.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import raptor.action.RaptorAction.RaptorActionContainer;
import raptor.chess.Game;
import raptor.chess.GameCursor;
import raptor.chess.Move;
import raptor.chess.pgn.PgnHeader;
import raptor.connector.Connector;
import raptor.pref.PreferenceKeys;
import raptor.service.SoundService;
import raptor.service.GameService.GameServiceAdapter;
import raptor.service.GameService.GameServiceListener;
import raptor.swt.SWTUtils;
import raptor.swt.chess.Arrow;
import raptor.swt.chess.ChessBoardController;
import raptor.swt.chess.ChessBoardUtils;
import raptor.swt.chess.Highlight;
import raptor.util.RaptorStringUtils;

/**
 * A controller used when observing a game.
 * 
 * The user isnt allowed to make any moves on a game being observed. However
 * they are allowed to use the nav buttons.
 */
public class ObserveController extends ChessBoardController {
	static final Log LOG = LogFactory.getLog(ObserveController.class);

	protected GameCursor cursor = null;
	protected GameServiceListener listener = new GameServiceAdapter() {
		@Override
		public void droppablePiecesChanged(Game game) {
			if (!isDisposed() && game.getId().equals(getGame().getId())) {
				board.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						refreshBoard();
					}
				});
			}
		}

		@Override
		public void gameInactive(Game game) {
			if (!isDisposed() && game.getId().equals(getGame().getId())) {
				board.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							board.getResultDecorator().setDecorationFromResult(
									getGame().getResult());
							board.redrawSquares();

							onPlayGameEndSound();

							InactiveController inactiveController = new InactiveController(
									getGame());
							getBoard().setController(inactiveController);
							inactiveController.setBoard(board);

							getConnector().getGameService()
									.removeGameServiceListener(listener);
							inactiveController
									.setItemChangedListeners(itemChangedListeners);
							inactiveController.init();
							// Set the listeners to null so they wont get
							// cleared and disposed
							setItemChangedListeners(null);
							ObserveController.this.dispose();
						} catch (Throwable t) {
							getConnector().onError(
									"ExamineController.gameInactive", t);
						}
					}
				});
			}
		}

		@Override
		public void gameMovesAdded(Game game) {
			if (!isDisposed() && game.getId().equals(getGame().getId())) {
				board.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							cursor.setCursorMasterLast();
							refresh();
						} catch (Throwable t) {
							connector.onError(
									"ObserveController.gameMovesAdded", t);
						}
					}
				});
			}
		}

		@Override
		public void gameStateChanged(Game game, final boolean isNewMove) {
			if (!isDisposed() && game.getId().equals(getGame().getId())) {
				board.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							if (isToolItemSelected(ToolBarItemKey.FORCE_UPDATE)) {
								cursor.setCursorMasterLast();
							}
							if (isNewMove) {
								onPlayMoveSound();
							}

							board.getSquareHighlighter().removeAllHighlights();
							board.getArrowDecorator().removeAllArrows();

							Move lastMove = getGame().getLastMove();

							if (lastMove != null) {
								if (getPreferences()
										.getBoolean(
												PreferenceKeys.HIGHLIGHT_SHOW_ON_OBS_MOVES)) {
									board
											.getSquareHighlighter()
											.addHighlight(
													new Highlight(
															lastMove.getFrom(),
															lastMove.getTo(),
															getPreferences()
																	.getColor(
																			PreferenceKeys.HIGHLIGHT_OBS_COLOR),
															getPreferences()
																	.getBoolean(
																			PreferenceKeys.HIGHLIGHT_FADE_AWAY_MODE)));
								}

								if (getPreferences().getBoolean(
										PreferenceKeys.ARROW_SHOW_ON_OBS_MOVES)) {
									board
											.getArrowDecorator()
											.addArrow(
													new Arrow(
															lastMove.getFrom(),
															lastMove.getTo(),
															getPreferences()
																	.getColor(
																			PreferenceKeys.ARROW_OBS_COLOR),
															getPreferences()
																	.getBoolean(
																			PreferenceKeys.ARROW_FADE_AWAY_MODE)));
								}
							}
							refresh();
						} catch (Throwable t) {
							connector.onError(
									"ObserveController.gameStateChanged", t);
						}
					}
				});
			}
		}
	};
	protected ToolBar toolbar;

	/**
	 * You can set the PgnHeader WhiteOnTop to toggle if white should be
	 * displayed on top or not.
	 */
	public ObserveController(Game game, Connector connector) {
		super(new GameCursor(game,
				GameCursor.Mode.MakeMovesOnMasterSetCursorToLast), connector);
		cursor = (GameCursor) getGame();
	}

	@Override
	public void adjustGameDescriptionLabel() {
		if (!isDisposed()) {
			board.getGameDescriptionLabel().setText(
					"Observing " + getGame().getHeader(PgnHeader.Event));
		}
	}

	@Override
	public boolean canUserInitiateMoveFrom(int squareId) {
		return false;
	}

	public void decorateForLastMoveListMove() {
		board.getSquareHighlighter().removeAllHighlights();
		board.getArrowDecorator().removeAllArrows();

		Move lastMove = getGame().getLastMove();

		if (lastMove != null) {
			if (getPreferences().getBoolean(
					PreferenceKeys.HIGHLIGHT_SHOW_ON_MOVE_LIST_MOVES)) {
				board
						.getSquareHighlighter()
						.addHighlight(
								new Highlight(
										lastMove.getFrom(),
										lastMove.getTo(),
										getPreferences()
												.getColor(
														PreferenceKeys.HIGHLIGHT_OBS_COLOR),
										getPreferences()
												.getBoolean(
														PreferenceKeys.HIGHLIGHT_FADE_AWAY_MODE)));
			}

			if (getPreferences().getBoolean(
					PreferenceKeys.ARROW_SHOW_ON_MOVE_LIST_MOVES)) {
				board.getArrowDecorator().addArrow(
						new Arrow(lastMove.getFrom(), lastMove.getTo(),
								getPreferences().getColor(
										PreferenceKeys.ARROW_OBS_COLOR),
								getPreferences().getBoolean(
										PreferenceKeys.ARROW_FADE_AWAY_MODE)));
			}
		}
	}

	@Override
	public void dispose() {
		try {
			getConnector().getGameService().removeGameServiceListener(listener);
			if (getConnector().isConnected()
					&& getGame().isInState(Game.ACTIVE_STATE)) {
				getConnector().onUnobserve(getGame());
			}
			if (toolbar != null) {
				toolbar.setVisible(false);
				SWTUtils.clearToolbar(toolbar);
				toolbar = null;
			}
			super.dispose();
		} catch (Throwable t) {// Eat it its prob a disposed exception.
		}
	}

	public void enableDisableNavButtons() {
		setToolItemEnabled(ToolBarItemKey.NEXT_NAV, cursor.hasNext());
		setToolItemEnabled(ToolBarItemKey.BACK_NAV, cursor.hasPrevious());
		setToolItemEnabled(ToolBarItemKey.FIRST_NAV, cursor.hasFirst());
		setToolItemEnabled(ToolBarItemKey.LAST_NAV, cursor.hasLast());
	}

	public GameCursor getCursor() {
		return cursor;
	}

	@Override
	public String getTitle() {
		return getConnector().getShortName() + "(Obs " + getGame().getId()
				+ ")";
	}

	@Override
	public Control getToolbar(Composite parent) {
		if (toolbar == null) {
			toolbar = new ToolBar(parent, SWT.FLAT);
			ChessBoardUtils.addActionsToToolbar(this,
					RaptorActionContainer.ObservingChessBoard, toolbar, false);

			setToolItemSelected(ToolBarItemKey.FORCE_UPDATE, true);
			enableDisableNavButtons();
		} else {
			toolbar.setParent(parent);
		}

		return toolbar;
	}

	@Override
	public void init() {
		board.setWhiteOnTop(RaptorStringUtils.getBooleanValue(game
				.getHeader(PgnHeader.WhiteOnTop)));

		/**
		 * In Droppable games (bughouse/crazyhouse) you own your own piece jail
		 * since you can drop pieces from it.
		 * 
		 * In other games its just a collection of pieces yu have captured so
		 * your opponent owns your piece jail.
		 */
		if (getGame().isInState(Game.DROPPABLE_STATE)) {
			board.setWhitePieceJailOnTop(board.isWhiteOnTop() ? true : false);
		} else {
			board.setWhitePieceJailOnTop(board.isWhiteOnTop() ? false : true);
		}

		cursor.setCursorMasterLast();
		refresh();

		onPlayGameStartSound();

		// Add the service listener last so there are no synch problems.
		// It is ok if we miss moves the GameService will update the game.
		connector.getGameService().addGameServiceListener(listener);
		fireItemChanged();
	}

	@Override
	public void onBack() {
		cursor.setCursorPrevious();
		refresh(false);
		decorateForLastMoveListMove();
	}

	@Override
	public void onFirst() {
		cursor.setCursorFirst();
		refresh(false);
		decorateForLastMoveListMove();
	}

	@Override
	public void onForward() {
		cursor.setCursorNext();
		refresh(false);
		decorateForLastMoveListMove();
	}

	@Override
	public void onLast() {
		cursor.setCursorMasterLast();
		setToolItemEnabled(ToolBarItemKey.FORCE_UPDATE, true);
		refresh(false);
		decorateForLastMoveListMove();
	}

	@Override
	public void refresh(boolean isUpdatingClocks) {
		if (isDisposed()) {
			return;
		}
		board.getMoveList().updateToGame();
		board.getMoveList().select(cursor.getCursorPosition());
		enableDisableNavButtons();
		super.refresh(isUpdatingClocks);
	}

	@Override
	public void userCancelledMove(int fromSquare, boolean isDnd) {
	}

	@Override
	public void userInitiatedMove(int square, boolean isDnd) {
	}

	@Override
	public void userMadeMove(int fromSquare, int toSquare) {
	}

	@Override
	public void userMiddleClicked(int square) {
	}

	@Override
	public void userRightClicked(int square) {
	}

	/**
	 * Invoked when the move list is clicked on. THe halfMoveNumber is the move
	 * selected.
	 * 
	 * The default implementation does nothing. It can be overridden to provide
	 * functionality.
	 */
	@Override
	public void userSelectedMoveListMove(int halfMoveNumber) {
		cursor.setCursor(halfMoveNumber);
		refresh(false);
		decorateForLastMoveListMove();
	}

	protected void onPlayGameEndSound() {
		SoundService.getInstance().playSound("obsGameEnd");
	}

	protected void onPlayGameStartSound() {
		SoundService.getInstance().playSound("gameStart");
	}

	protected void onPlayMoveSound() {
		if (getPreferences().getBoolean(
				PreferenceKeys.BOARD_PLAY_MOVE_SOUND_WHEN_OBSERVING)) {
			SoundService.getInstance().playSound("obsMove");
		}
	}

}
