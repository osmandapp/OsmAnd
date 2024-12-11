package net.osmand.plus.plugins.monitoring;

import android.os.Handler;
import android.os.Message;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

import androidx.annotation.NonNull;

@SuppressWarnings("deprecation")
public class TripRecordingUpdatesHandler extends Handler {

	private static final int UPDATE_GPS_MESSAGE_ID = 0;
	private static final int UPDATE_CHART_MESSAGE_ID = 1;

	private static final int MIN_UPDATE_INTERVAL = 1000;

	private final OsmandSettings settings;
	private final Runnable gpsUpdateTask;
	private final Runnable chartUpdateTask;

	public TripRecordingUpdatesHandler(@NonNull OsmandApplication app,
	                                   @NonNull Runnable gpsUpdateTask,
	                                   @NonNull Runnable chartUpdateTask) {
		this.settings = app.getSettings();
		this.gpsUpdateTask = gpsUpdateTask;
		this.chartUpdateTask = chartUpdateTask;
	}

	public void startGpsUpdatesIfNotRunning() {
		if (!hasMessages(UPDATE_GPS_MESSAGE_ID)) {
			sendEmptyMessage(UPDATE_GPS_MESSAGE_ID);
		}
	}

	public void startChartUpdatesIfNotRunning() {
		if (!hasMessages(UPDATE_CHART_MESSAGE_ID)) {
			sendEmptyMessage(UPDATE_CHART_MESSAGE_ID);
		}
	}

	public void stopGpsUpdates() {
		removeMessages(UPDATE_GPS_MESSAGE_ID);
	}

	public void stopChartUpdates() {
		removeMessages(UPDATE_CHART_MESSAGE_ID);
	}

	@Override
	public void handleMessage(@NonNull Message message) {
		int messageId = message.what;
		int updateInterval = Math.max(MIN_UPDATE_INTERVAL, settings.SAVE_GLOBAL_TRACK_INTERVAL.get());
		if (messageId == UPDATE_GPS_MESSAGE_ID) {
			gpsUpdateTask.run();
			sendEmptyMessageDelayed(UPDATE_GPS_MESSAGE_ID, updateInterval);
		} else if (messageId == UPDATE_CHART_MESSAGE_ID) {
			chartUpdateTask.run();
			sendEmptyMessageDelayed(UPDATE_CHART_MESSAGE_ID, updateInterval);
		}
	}
}