package net.osmand.plus.notifications;

import static androidx.core.app.NotificationCompat.PRIORITY_DEFAULT;
import static net.osmand.plus.NavigationService.USED_BY_AIS;

import android.app.Service;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class AisNotification extends OsmandNotification {

	public static final String GROUP_NAME = "AIS";

	public AisNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@Override
	public NotificationType getType() {
		return NotificationType.AIS;
	}

	@Override
	public int getPriority() {
		return PRIORITY_DEFAULT;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public boolean isUsedByService(@Nullable Service service) {
		NavigationService navService = service instanceof NavigationService
				? (NavigationService) service : app.getNavigationService();
		return navService != null && (navService.getUsedBy() & USED_BY_AIS) != 0;
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public Builder buildNotification(@Nullable Service service, boolean wearable) {
		if (!isEnabled(service)) {
			return null;
		}
		icon = R.drawable.ic_notification_track;
		ongoing = true;
		return createBuilder(wearable)
				.setContentTitle(app.getString(R.string.plugin_ais_tracker_name))
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.setStyle(new BigTextStyle().bigText(app.getString(R.string.ais_receive_in_background)));
	}

	@Override
	public int getOsmandNotificationId() {
		return AIS_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_AIS_NOTIFICATION_SERVICE_ID;
	}
}
