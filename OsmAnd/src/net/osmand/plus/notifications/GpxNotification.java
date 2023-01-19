package net.osmand.plus.notifications;

import static net.osmand.plus.NavigationService.USED_BY_GPX;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

public class GpxNotification extends OsmandNotification {

	public static final String OSMAND_SAVE_GPX_SERVICE_ACTION = "OSMAND_SAVE_GPX_SERVICE_ACTION";
	public static final String OSMAND_START_GPX_SERVICE_ACTION = "OSMAND_START_GPX_SERVICE_ACTION";
	public static final String OSMAND_STOP_GPX_SERVICE_ACTION = "OSMAND_STOP_GPX_SERVICE_ACTION";
	public static final String GROUP_NAME = "GPX";

	private boolean wasNoDataDismissed;
	private boolean lastBuiltNoData;

	public GpxNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@Override
	public void init() {
		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.saveCurrentTrack();
				}
			}
		}, new IntentFilter(OSMAND_SAVE_GPX_SERVICE_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.startGPXMonitoring(null);
					plugin.updateWidgets();
				}
			}
		}, new IntentFilter(OSMAND_START_GPX_SERVICE_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.stopRecording();
					plugin.updateWidgets();
				}
			}
		}, new IntentFilter(OSMAND_STOP_GPX_SERVICE_ACTION));
	}

	@Override
	public NotificationType getType() {
		return NotificationType.GPX;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_DEFAULT;
	}

	@Override
	public boolean isActive() {
		NavigationService service = app.getNavigationService();
		return isEnabled()
				&& service != null
				&& (service.getUsedBy() & USED_BY_GPX) != 0;
	}

	@Override
	public boolean isEnabled() {
		return PluginsHelper.isActive(OsmandMonitoringPlugin.class);
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public void onNotificationDismissed() {
		if (!wasNoDataDismissed) {
			wasNoDataDismissed = lastBuiltNoData;
		}
	}

	@Override
	public Builder buildNotification(boolean wearable) {
		if (!isEnabled()) {
			return null;
		}
		String notificationTitle;
		String notificationText;
		color = 0;
		icon = R.drawable.ic_notification_track;
		boolean isGpxRecording = app.getSavingTrackHelper().getIsRecording();
		float recordedDistance = app.getSavingTrackHelper().getDistance();
		ongoing = true;
		lastBuiltNoData = false;
		if (isGpxRecording) {
			color = app.getColor(R.color.osmand_orange);
			notificationTitle = app.getString(R.string.shared_string_trip) + " • "
					+ Algorithms.formatDuration((int) (app.getSavingTrackHelper().getDuration() / 1000), true) + " • "
					+ OsmAndFormatter.getFormattedDistance(recordedDistance, app);
			notificationText = app.getString(R.string.shared_string_recorded)
					+ ": " + OsmAndFormatter.getFormattedDistance(recordedDistance, app);
		} else {
			if (app.getSavingTrackHelper().getTrkPoints() > 0) {
				notificationTitle = app.getString(R.string.shared_string_paused) + " • "
						+ Algorithms.formatDuration((int) (app.getSavingTrackHelper().getDuration() / 1000), true) + " • "
						+ OsmAndFormatter.getFormattedDistance(recordedDistance, app);
				notificationText = app.getString(R.string.shared_string_recorded)
						+ ": " + OsmAndFormatter.getFormattedDistance(recordedDistance, app);
			} else {
				ongoing = false;
				notificationTitle = app.getString(R.string.shared_string_trip_recording);
				notificationText = app.getString(R.string.gpx_logging_no_data);
				lastBuiltNoData = true;
			}
		}
		notificationText = notificationText + "  (" + app.getSavingTrackHelper().getTrkPoints() + ")";

		if ((wasNoDataDismissed || !app.getSettings().SHOW_TRIP_REC_NOTIFICATION.get()) && !ongoing) {
			return null;
		}

		Builder notificationBuilder = createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(new BigTextStyle().bigText(notificationText));

		Intent saveIntent = new Intent(OSMAND_SAVE_GPX_SERVICE_ACTION);
		PendingIntent savePendingIntent = PendingIntent.getBroadcast(app, 0, saveIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		if (isGpxRecording) {
			Intent stopIntent = new Intent(OSMAND_STOP_GPX_SERVICE_ACTION);
			PendingIntent stopPendingIntent = PendingIntent.getBroadcast(app, 0, stopIntent,
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			if (app.getSavingTrackHelper().getTrkPoints() > 0) {
				notificationBuilder.addAction(R.drawable.ic_notification_pause,
						app.getString(R.string.shared_string_pause), stopPendingIntent);
				notificationBuilder.addAction(R.drawable.ic_notification_save,
						app.getString(R.string.shared_string_save), savePendingIntent);
			} else {
				notificationBuilder.addAction(R.drawable.ic_notification_rec_stop,
						app.getString(R.string.shared_string_control_stop), stopPendingIntent);
			}
		} else {
			Intent startIntent = new Intent(OSMAND_START_GPX_SERVICE_ACTION);
			PendingIntent startPendingIntent = PendingIntent.getBroadcast(app, 0, startIntent,
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			if (app.getSavingTrackHelper().getTrkPoints() > 0) {
				notificationBuilder.addAction(R.drawable.ic_notification_rec_start,
						app.getString(R.string.shared_string_resume), startPendingIntent);
				notificationBuilder.addAction(R.drawable.ic_notification_save,
						app.getString(R.string.shared_string_save), savePendingIntent);
			} else {
				notificationBuilder.addAction(R.drawable.ic_notification_rec_start,
						app.getString(R.string.shared_string_record), startPendingIntent);
			}
		}

		return notificationBuilder;
	}

	@Override
	public int getOsmandNotificationId() {
		return GPX_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_GPX_NOTIFICATION_SERVICE_ID;
	}
}
