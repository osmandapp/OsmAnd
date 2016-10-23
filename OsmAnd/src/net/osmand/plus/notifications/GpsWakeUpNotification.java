package net.osmand.plus.notifications;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v7.app.NotificationCompat;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;

import static net.osmand.plus.NavigationService.USED_BY_WAKE_UP;

public class GpsWakeUpNotification extends OsmandNotification {

	public final static String OSMAND_STOP_GPS_WAKE_UP_SERVICE_ACTION = "OSMAND_STOP_GPS_WAKE_UP_SERVICE_ACTION";

	public GpsWakeUpNotification(OsmandApplication app) {
		super(app);
	}

	@Override
	public void init() {
		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Intent serviceIntent = new Intent(app, NavigationService.class);
				app.stopService(serviceIntent);
			}
		}, new IntentFilter(OSMAND_STOP_GPS_WAKE_UP_SERVICE_ACTION));
	}

	@Override
	public NotificationType getType() {
		return NotificationType.GPS;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_HIGH;
	}

	@Override
	public boolean isActive() {
		return isEnabled();
	}

	@Override
	public boolean isEnabled() {
		NavigationService service = app.getNavigationService();
		return service != null && (service.getUsedBy() & USED_BY_WAKE_UP) != 0;
	}

	@Override
	public Builder buildNotification() {
		NavigationService service = app.getNavigationService();
		String notificationTitle;
		String notificationText;
		color = 0;
		icon = R.drawable.bgs_icon;
		if (service != null && (service.getUsedBy() & USED_BY_WAKE_UP) != 0) {
			int soi = service.getServiceOffInterval();
			color = app.getResources().getColor(R.color.osmand_orange);
			notificationTitle = Version.getAppName(app);
			notificationText = app.getString(R.string.gps_wake_up_timer) + ": ";
			if (soi == 0) {
				notificationText = notificationText + app.getString(R.string.int_continuosly);
			} else if (soi <= 90000) {
				notificationText = notificationText + Integer.toString(soi / 1000) + " " + app.getString(R.string.int_seconds);
			} else {
				notificationText = notificationText + Integer.toString(soi / 1000 / 60) + " " + app.getString(R.string.int_min);
			}
		} else {
			return null;
		}

		final Builder notificationBuilder = createBuilder()
				.setContentTitle(notificationTitle)
				.setStyle(new BigTextStyle().bigText(notificationText));

		Intent wakeUpIntent = new Intent(OSMAND_STOP_GPS_WAKE_UP_SERVICE_ACTION);
		PendingIntent stopWakeUpPendingIntent = PendingIntent.getBroadcast(app, 0, wakeUpIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
				app.getString(R.string.stop_navigation_service), stopWakeUpPendingIntent);

		return notificationBuilder;
	}

	@Override
	public int getUniqueId() {
		return GPS_WAKE_UP_NOTIFICATION_SERVICE_ID;
	}
}
