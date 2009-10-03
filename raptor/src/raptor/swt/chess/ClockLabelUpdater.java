package raptor.swt.chess;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import raptor.Raptor;
import raptor.pref.PreferenceKeys;
import raptor.pref.RaptorPreferenceStore;

class ClockLabelUpdater implements Runnable, PreferenceKeys {
	Label clockLabel;
	long remainingTimeMillis;
	boolean isRunning;
	long lastSystemTime = 0;
	ChessBoard board;

	public ClockLabelUpdater(Label clockLabel, ChessBoard board) {
		this.clockLabel = clockLabel;
		this.board = board;
	}

	public long calculateNextUpdate() {

		// The timer is not always perfect.
		// The following code adjusts the ideal durations in half to make up for
		// the differences.
		long result = 0L;
		if (remainingTimeMillis >= getPreferences().getLong(
				BOARD_CLOCK_SHOW_SECONDS_WHEN_LESS_THAN)) {
			result = remainingTimeMillis % 30000L;
			if (result == 0L) {
				result = 30000L;
			}
		} else if (remainingTimeMillis >= getPreferences().getLong(
				BOARD_CLOCK_SHOW_MILLIS_WHEN_LESS_THAN)) {
			result = remainingTimeMillis % 500L;
			if (result == 0L) {
				result = 500L;
			}
		} else {
			result = remainingTimeMillis % 50L;
			if (result == 0L) {
				result = 50L;
			}
		}
		return result;
	}

	public void dispose() {
		board = null;
		clockLabel = null;
	}

	protected RaptorPreferenceStore getPreferences() {
		return Raptor.getInstance().getPreferences();
	}

	public long getRemainingTimeMillis() {
		return remainingTimeMillis;
	}

	public void run() {
		if (isRunning && board != null && clockLabel != null
				&& !clockLabel.isDisposed()) {
			long currentTime = System.currentTimeMillis();
			remainingTimeMillis -= currentTime - lastSystemTime;
			lastSystemTime = currentTime;

			clockLabel.setText(BoardUtils.timeToString(remainingTimeMillis));

			if (remainingTimeMillis > 0) {
				long nextUpdate = calculateNextUpdate();
				if (isRunning) {
					Display.getCurrent().timerExec((int) nextUpdate, this);
				}
			}
		}
	}

	public void setRemainingTimeMillis(long elapsedTimeMillis) {
		this.remainingTimeMillis = elapsedTimeMillis;
	}

	public void start() {
		isRunning = true;
		if (remainingTimeMillis > 0) {
			lastSystemTime = System.currentTimeMillis();
			long nextUpdate = calculateNextUpdate();
			Display.getCurrent().timerExec((int) nextUpdate, this);
		}
	}

	public void stop() {
		isRunning = false;
		Display.getCurrent().timerExec(-1, this);
	}
}