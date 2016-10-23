package net.osmand.plus.notifications;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmo.OsMoPlugin;

import static android.support.v7.app.NotificationCompat.*;
import static net.osmand.plus.NavigationService.USED_BY_GPX;
import static net.osmand.plus.NavigationService.USED_BY_LIVE;

public class OsMoNotification extends OsmandNotification {

	public OsMoNotification(OsmandApplication app) {
		super(app);
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
		return OsmandPlugin.getEnabledPlugin(OsMoPlugin.class) != null;
	}

	@Override
	public Builder buildNotification() {
		String notificationTitle;
		String notificationText;
		color = 0;
		icon = R.drawable.ic_osmo_dark;
		color = app.getResources().getColor(R.color.osmand_orange);
		notificationTitle = Version.getAppName(app);
		notificationText = app.getString(R.string.osmo);

		final Builder notificationBuilder = createBuilder()
				.setContentTitle(notificationTitle)
				.setStyle(new BigTextStyle().bigText(notificationText));

		return notificationBuilder;
	}



	@Override
	public int getUniqueId() {
		return OSMO_NOTIFICATION_SERVICE_ID;
	}
}
