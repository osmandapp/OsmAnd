package net.osmand.plus;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v7.app.NotificationCompat;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;

public class NotificationHelper {
	public final static int NOTIFICATION_SERVICE_ID = 5;
	public final static String OSMAND_STOP_SERVICE_ACTION = "OSMAND_STOP_SERVICE_ACTION"; //$NON-NLS-1$
	public final static String OSMAND_STOP_GPX_SERVICE_ACTION = "OSMAND_STOP_GPX_SERVICE_ACTION"; //$NON-NLS-1$
	public final static String OSMAND_START_GPX_SERVICE_ACTION = "OSMAND_START_GPX_SERVICE_ACTION"; //$NON-NLS-1$
	public final static String OSMAND_SAVE_GPX_SERVICE_ACTION = "OSMAND_SAVE_GPX_SERVICE_ACTION"; //$NON-NLS-1$
	
	private OsmandApplication app;
	private BroadcastReceiver saveBroadcastReceiver;
	private BroadcastReceiver stopBroadcastReceiver;
	private BroadcastReceiver startBroadcastReceiver;

	public NotificationHelper(OsmandApplication app) {
		this.app = app;
		init();
	}
	
	private void init() {
		saveBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.saveCurrentTrack();
				}
			}
		};
		app.registerReceiver(saveBroadcastReceiver, new IntentFilter(OSMAND_SAVE_GPX_SERVICE_ACTION));
		startBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.startGPXMonitoring(null);
				}
			}
		};
		app.registerReceiver(startBroadcastReceiver, new IntentFilter(OSMAND_START_GPX_SERVICE_ACTION));
		stopBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
				if (plugin != null) {
					plugin.stopRecording();
				}
			}
		};
		app.registerReceiver(stopBroadcastReceiver, new IntentFilter(OSMAND_STOP_GPX_SERVICE_ACTION));
	}

	public Builder buildNotificationInStatusBar() {
		NavigationService service = app.getNavigationService();
		String notificationText ;
		int icon = R.drawable.bgs_icon;
		OsmandMonitoringPlugin monitoringPlugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (service != null) {
			int soi = service.getServiceOffInterval();
			notificationText = app.getString(R.string.osmand_running_in_background);
			String s = "";
			if ((service.getUsedBy() & NavigationService.USED_BY_NAVIGATION) != 0) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += app.getString(R.string.shared_string_navigation).toLowerCase();
			}
			if ((service.getUsedBy() & NavigationService.USED_BY_GPX) != 0) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += app.getString(R.string.shared_string_trip_recording).toLowerCase();
			}
			if ((service.getUsedBy() & NavigationService.USED_BY_LIVE) != 0) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += app.getString(R.string.osmo);
			}
			if(s.length() > 0) {
				notificationText += " (" + s + "). ";
			}
			notificationText += app.getString(R.string.gps_wake_up_timer) + ": ";
			if (soi == 0) {
				notificationText = notificationText + app.getString(R.string.int_continuosly);
			} else if (soi <= 90000) {
				notificationText = notificationText + Integer.toString(soi / 1000) + " " + app.getString(R.string.int_seconds);
			} else {
				notificationText = notificationText + Integer.toString(soi / 1000 / 60) + " " + app.getString(R.string.int_min);
			}
		} else if(monitoringPlugin == null) {
			return null;
		} else {
			notificationText =	app.getString(R.string.shared_string_trip_recording);
			float dst = app.getSavingTrackHelper().getDistance();
			notificationText += " ("+OsmAndFormatter.getFormattedDistance(dst, app)+")";
			icon = R.drawable.ic_action_polygom_dark;
		}

		Intent contentIntent = new Intent(app, MapActivity.class);
		PendingIntent contentPendingIntent = PendingIntent.getActivity(app, 0, contentIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		final Builder notificationBuilder = new NotificationCompat.Builder(app)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setContentTitle(Version.getAppName(app)).setContentText(notificationText).setSmallIcon(icon)
				.setContentIntent(contentPendingIntent).setOngoing(service != null);
		if (monitoringPlugin != null) {
			Intent saveIntent = new Intent(OSMAND_SAVE_GPX_SERVICE_ACTION);
			PendingIntent savePendingIntent = PendingIntent.getBroadcast(app, 0, saveIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			if(service != null && service.getUsedBy() == NavigationService.USED_BY_GPX) {
				Intent stopIntent = new Intent(OSMAND_STOP_GPX_SERVICE_ACTION);
				PendingIntent stopPendingIntent = PendingIntent.getBroadcast(app, 0, stopIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
						app.getString(R.string.shared_string_control_stop), stopPendingIntent);
				notificationBuilder.addAction(R.drawable.ic_action_save, app.getString(R.string.shared_string_save),
						savePendingIntent);
			} else if(service == null) {
				Intent startIntent = new Intent(OSMAND_START_GPX_SERVICE_ACTION);
				PendingIntent startPendingIntent = PendingIntent.getBroadcast(app, 0, startIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				notificationBuilder.addAction(R.drawable.ic_action_rec_start,
						app.getString(R.string.shared_string_control_start), startPendingIntent);
				notificationBuilder.addAction(R.drawable.ic_action_save, app.getString(R.string.shared_string_save),
						savePendingIntent);
			}


			
		}
		return notificationBuilder;
	}
	
	public void showNotification() {
		NotificationManager mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		Builder newNotification = buildNotificationInStatusBar();
		if(newNotification != null) {
			mNotificationManager.notify(NOTIFICATION_SERVICE_ID, newNotification.build());
		}
	}

	public void removeServiceNotification() {
		NotificationManager mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel( NOTIFICATION_SERVICE_ID);
		Builder newNotification = buildNotificationInStatusBar();
		if(newNotification != null) {
			mNotificationManager.notify(NOTIFICATION_SERVICE_ID, newNotification.build());
		}
	}
	
	public void removeServiceNotificationCompletely() {
		NotificationManager mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel( NOTIFICATION_SERVICE_ID);
	}
}
