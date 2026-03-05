package net.osmand.plus.notifications;

import android.app.Service;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;

public class FallbackNotification extends OsmandNotification {

	public static final String GROUP_NAME = "FALLBACK";

	public FallbackNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
		icon = R.drawable.ic_action_osmand_logo;
	}

	@Override
	public NotificationType getType() {
		return NotificationType.FALLBACK;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_LOW;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public boolean isUsedByService(@Nullable Service service) {
		return false;
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public NotificationCompat.Builder buildNotification(@Nullable Service service, boolean wearable) {
		return createBuilder(wearable).setContentTitle(Version.getAppName(app)).setOngoing(false);
	}

	@Override
	public int getOsmandNotificationId() {
		return FALLBACK_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_FALLBACK_NOTIFICATION_SERVICE_ID;
	}
}