package net.osmand.plus.notifications;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.util.Algorithms;

import static net.osmand.plus.NavigationService.USED_BY_GPX;

public class GpxNotification extends OsmandNotification {

	public final static String OSMAND_SAVE_GPX_SERVICE_ACTION = "OSMAND_SAVE_GPX_SERVICE_ACTION";
	public final static String OSMAND_START_GPX_SERVICE_ACTION = "OSMAND_START_GPX_SERVICE_ACTION";
	public final static String OSMAND_STOP_GPX_SERVICE_ACTION = "OSMAND_STOP_GPX_SERVICE_ACTION";
	public final static String GROUP_NAME = "GPX";

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
				final OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.saveCurrentTrack();
					if (!app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
						plugin.stopRecording();
					}
				}
			}
		}, new IntentFilter(OSMAND_SAVE_GPX_SERVICE_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.startGPXMonitoring(null);
					plugin.updateControl();
				}
			}
		}, new IntentFilter(OSMAND_START_GPX_SERVICE_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.stopRecording();
					plugin.updateControl();
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
		return app.getSavingTrackHelper().getIsRecording() || OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null;
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
		icon = R.drawable.ic_action_polygom_dark;
		boolean isGpxRecording = app.getSavingTrackHelper().getIsRecording();
		float recordedDistance = app.getSavingTrackHelper().getDistance();
		ongoing = true;
        lastBuiltNoData = false;
		if (isGpxRecording) {
			color = app.getResources().getColor(R.color.osmand_orange);
			notificationTitle = app.getString(R.string.shared_string_trip) + " • "
					+ Algorithms.formatDuration((int) (app.getSavingTrackHelper().getDuration() / 1000), true);
			notificationText = app.getString(R.string.shared_string_recorded)
					+ ": " + OsmAndFormatter.getFormattedDistance(recordedDistance, app);
		} else {
			if (recordedDistance > 0) {
				notificationTitle = app.getString(R.string.shared_string_paused) + " • "
						+ Algorithms.formatDuration((int) (app.getSavingTrackHelper().getDuration() / 1000), true);
				notificationText = app.getString(R.string.shared_string_recorded)
						+ ": " + OsmAndFormatter.getFormattedDistance(recordedDistance, app);
			} else {
				ongoing = false;
				notificationTitle = app.getString(R.string.shared_string_trip_recording);
				notificationText = app.getString(R.string.gpx_logging_no_data);
				lastBuiltNoData = true;
			}
		}
		notificationText = notificationText + "  (" + Integer.toString(app.getSavingTrackHelper().getTrkPoints()) + ")";

		if ((wasNoDataDismissed || !app.getSettings().SHOW_TRIP_REC_NOTIFICATION.get()) && !ongoing) {
			return null;
		}

		final Builder notificationBuilder = createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(new BigTextStyle().bigText(notificationText));

		Intent saveIntent = new Intent(OSMAND_SAVE_GPX_SERVICE_ACTION);
		PendingIntent savePendingIntent = PendingIntent.getBroadcast(app, 0, saveIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		if (isGpxRecording) {
			Intent stopIntent = new Intent(OSMAND_STOP_GPX_SERVICE_ACTION);
			PendingIntent stopPendingIntent = PendingIntent.getBroadcast(app, 0, stopIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			if (app.getSavingTrackHelper().getDistance() > 0) {
				notificationBuilder.addAction(R.drawable.ic_pause,
						app.getString(R.string.shared_string_pause), stopPendingIntent);
				notificationBuilder.addAction(R.drawable.ic_action_save,
						app.getString(R.string.shared_string_save), savePendingIntent);
			} else {
				notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
						app.getString(R.string.shared_string_control_stop), stopPendingIntent);
			}
		} else {
			Intent startIntent = new Intent(OSMAND_START_GPX_SERVICE_ACTION);
			PendingIntent startPendingIntent = PendingIntent.getBroadcast(app, 0, startIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			if (recordedDistance > 0) {
				notificationBuilder.addAction(R.drawable.ic_action_rec_start,
						app.getString(R.string.shared_string_continue), startPendingIntent);
				notificationBuilder.addAction(R.drawable.ic_action_save,
						app.getString(R.string.shared_string_save), savePendingIntent);
			} else {
				notificationBuilder.addAction(R.drawable.ic_action_rec_start,
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
