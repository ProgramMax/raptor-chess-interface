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
import org.eclipse.swt.widgets.ToolItem;

import raptor.Raptor;
import raptor.connector.Connector;
import raptor.game.Game;
import raptor.pref.PreferenceKeys;
import raptor.service.SoundService;
import raptor.service.GameService.GameServiceAdapter;
import raptor.service.GameService.GameServiceListener;
import raptor.swt.SWTUtils;
import raptor.swt.chess.BoardUtils;
import raptor.swt.chess.ChessBoardController;

/**
 * A controller used when observing a game.
 * 
 * The user isnt allowed to make any moves on a game being observed. However
 * they are allowed to use the nav buttons.
 */
public class ObserveController extends ChessBoardController {
	static final Log LOG = LogFactory.getLog(ObserveController.class);

	protected Connector connector;

	protected GameServiceListener listener = new GameServiceAdapter() {
		@Override
		public void gameInactive(Game game) {
			if (!isDisposed() && game.getId().equals(getGame().getId())) {
				board.getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							onPlayGameEndSound();

							InactiveController inactiveController = new InactiveController(
									getGame());
							getBoard().setController(inactiveController);
							inactiveController.setBoard(board);

							getConnector().getGameService()
									.removeGameServiceListener(listener);

							inactiveController.init();
							inactiveController
									.setItemChangedListeners(itemChangedListeners);

							// Set the listeners to null so they wont get
							// cleared and disposed
							setItemChangedListeners(null);
							ObserveController.this.dispose();
							inactiveController.fireItemChanged();
						} catch (Throwable t) {
							getConnector().onError(
									"ExamineController.gameInactive", t);
						}
					}
				});
			}
		}

		@Override
		public void gameStateChanged(Game game, final boolean isNewMove) {
			if (!isDisposed() && game.getId().equals(getGame().getId())) {
				board.getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							refresh();
							if (isNewMove) {
								onPlayMoveSound();
							}
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

	public ObserveController(Game game, Connector connector) {
		super(game);
		this.connector = connector;
	}

	@Override
	public void adjustGameDescriptionLabel() {
		if (!isDisposed()) {
			board.getGameDescriptionLabel().setText(
					"Observing " + getGame().getEvent());
		}
	}

	@Override
	public boolean canUserInitiateMoveFrom(int squareId) {
		return false;
	}

	@Override
	public void dispose() {
		connector.getGameService().removeGameServiceListener(listener);
		if (connector.isConnected() && getGame().isInState(Game.ACTIVE_STATE)) {
			connector.onUnobserve(getGame());
		}
		if (toolbar != null) {
			toolbar.setVisible(false);
			SWTUtils.clearToolbar(toolbar);
			toolbar = null;
		}
		super.dispose();
	}

	public Connector getConnector() {
		return connector;
	}

	@Override
	public String getTitle() {
		return connector.getShortName() + "(Obs " + getGame().getId() + ")";
	}

	@Override
	public Control getToolbar(Composite parent) {
		if (toolbar == null) {
			toolbar = new ToolBar(parent, SWT.FLAT);
			BoardUtils.addNavIconsToToolbar(this, toolbar, false, false);
			new ToolItem(toolbar, SWT.SEPARATOR);
		} else if (toolbar.getParent() != parent) {
			toolbar.setParent(parent);
		}
		return toolbar;
	}

	@Override
	public void init() {

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

		refresh();

		onPlayGameStartSound();

		// Add the service listener last so there are no synch problems.
		// It is ok if we miss moves the GameService will update the game.
		connector.getGameService().addGameServiceListener(listener);
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

	@Override
	public void onToolbarButtonAction(ToolBarItemKey key, String... args) {
		switch (key) {
		case FEN:
			Raptor.getInstance().promptForText(
					"FEN for game " + game.getWhiteName() + " vs "
							+ game.getBlackName(), game.toFEN());
			break;
		case FLIP:
			onFlip();
			break;
		}
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
}
