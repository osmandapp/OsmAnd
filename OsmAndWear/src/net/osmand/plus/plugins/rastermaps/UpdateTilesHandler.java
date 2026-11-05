package net.osmand.plus.plugins.rastermaps;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

@SuppressWarnings("deprecation")
class UpdateTilesHandler extends Handler {

	private static final int UPDATE_TILES_MESSAGE_ID = 0;
	private static final long UPDATE_TILES_PREVIEW_INTERVAL = 500;

	private final Runnable updateTilesTask;

	public UpdateTilesHandler(@NonNull Runnable updateTilesTask) {
		this.updateTilesTask = updateTilesTask;
	}

	public void startUpdatesIfNotRunning() {
		if (!hasMessages(UPDATE_TILES_MESSAGE_ID)) {
			sendEmptyMessage(UPDATE_TILES_MESSAGE_ID);
		}
	}

	public void stopUpdates() {
		removeMessages(UPDATE_TILES_MESSAGE_ID);
	}

	@Override
	public void handleMessage(@NonNull Message message) {
		if (message.what == UPDATE_TILES_MESSAGE_ID) {
			updateTilesTask.run();
			sendEmptyMessageDelayed(UPDATE_TILES_MESSAGE_ID, UPDATE_TILES_PREVIEW_INTERVAL);
		}
	}
}
