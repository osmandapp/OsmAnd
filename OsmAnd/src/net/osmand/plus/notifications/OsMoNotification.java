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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmo.OsMoPlugin;

import static net.osmand.plus.NavigationService.USED_BY_LIVE;

public class OsMoNotification extends OsmandNotification {

	public final static String OSMAND_START_OSMO_SERVICE_ACTION = "OSMAND_START_OSMO_SERVICE_ACTION";
	public final static String OSMAND_STOP_OSMO_SERVICE_ACTION = "OSMAND_STOP_OSMO_SERVICE_ACTION";
	public final static String OSMAND_START_SHARE_LOCATION_ACTION = "OSMAND_START_SHARE_LOCATION_ACTION";
	public final static String OSMAND_STOP_SHARE_LOCATION_ACTION = "OSMAND_STOP_SHARE_LOCATION_ACTION";

	public OsMoNotification(OsmandApplication app) {
		super(app);
	}

	@Override
	public void init() {
		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
				if (osMoPlugin != null) {
					osMoPlugin.getService().connect(true);
					osMoPlugin.refreshMap();
				}
			}
		}, new IntentFilter(OSMAND_START_OSMO_SERVICE_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
				if (osMoPlugin != null) {
					osMoPlugin.getTracker().disableTracker();
					osMoPlugin.getService().disconnect();
					if (app.getNavigationService() != null) {
						app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_LIVE);
					}
					osMoPlugin.refreshMap();
				}
			}
		}, new IntentFilter(OSMAND_STOP_OSMO_SERVICE_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
				if (osMoPlugin != null) {
					osMoPlugin.getService().connect(true);
					if (osMoPlugin.getTracker() != null) {
						osMoPlugin.getTracker().enableTracker();
					}
					app.startNavigationService(NavigationService.USED_BY_LIVE, 0);
				}
			}
		}, new IntentFilter(OSMAND_START_SHARE_LOCATION_ACTION));

		app.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
				if (osMoPlugin != null) {
					if (osMoPlugin.getTracker() != null) {
						osMoPlugin.getTracker().disableTracker();
					}
					if (app.getNavigationService() != null) {
						app.getNavigationService()
								.stopIfNeeded(app, NavigationService.USED_BY_LIVE);
					}
				}
			}
		}, new IntentFilter(OSMAND_STOP_SHARE_LOCATION_ACTION));
	}

	@Override
	public NotificationType getType() {
		return NotificationType.OSMO;
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
				&& (service.getUsedBy() & USED_BY_LIVE) != 0;
	}

	@Override
	public boolean isEnabled() {
		OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
		return osMoPlugin != null && osMoPlugin.getService().isEnabled();
	}

	@Override
	public Builder buildNotification() {
		OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
		if (osMoPlugin == null) {
			return null;
		}

		String notificationTitle;
		String notificationText;
		color = 0;
		icon = R.drawable.ic_osmo_dark;
		color = app.getResources().getColor(R.color.osmand_orange);
		notificationTitle = app.getString(R.string.osmo_plugin_name);
		notificationText = app.getString(R.string.osmo_service_running);

		final Builder notificationBuilder = createBuilder()
				.setContentTitle(notificationTitle)
				.setStyle(new BigTextStyle().bigText(notificationText));

		if (osMoPlugin.getService().isEnabled()) {
			Intent stopServiceIntent = new Intent(OSMAND_STOP_OSMO_SERVICE_ACTION);
			PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(app, 0, stopServiceIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
					app.getString(R.string.shared_string_control_stop), stopServicePendingIntent);

			if (osMoPlugin.getTracker() != null) {
				if (osMoPlugin.getTracker().isEnabledTracker()) {
					Intent stopShareLocatiponIntent = new Intent(OSMAND_STOP_SHARE_LOCATION_ACTION);
					PendingIntent stopShareLocatiponIntentPendingIntent = PendingIntent.getBroadcast(app, 0, stopShareLocatiponIntent,
							PendingIntent.FLAG_UPDATE_CURRENT);
					notificationBuilder.addAction(R.drawable.ic_action_remove_dark,
							app.getString(R.string.osmo_pause_location), stopShareLocatiponIntentPendingIntent);
				} else {
					Intent startShareLocationIntent = new Intent(OSMAND_START_SHARE_LOCATION_ACTION);
					PendingIntent startShareLocationPendingIntent = PendingIntent.getBroadcast(app, 0, startShareLocationIntent,
							PendingIntent.FLAG_UPDATE_CURRENT);
					notificationBuilder.addAction(R.drawable.ic_action_gshare_dark,
							app.getString(R.string.osmo_share_location), startShareLocationPendingIntent);
				}
			}
		} else {
			Intent startServiceIntent = new Intent(OSMAND_START_OSMO_SERVICE_ACTION);
			PendingIntent startServicePendingIntent = PendingIntent.getBroadcast(app, 0, startServiceIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			notificationBuilder.addAction(R.drawable.ic_play_dark,
					app.getString(R.string.shared_string_control_start), startServicePendingIntent);
		}

		return notificationBuilder;
	}


	@Override
	public int getUniqueId() {
		return OSMO_NOTIFICATION_SERVICE_ID;
	}
}
