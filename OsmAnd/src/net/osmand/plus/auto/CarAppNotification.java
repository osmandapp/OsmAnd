package net.osmand.plus.auto;

import android.content.Intent;

import androidx.core.app.NotificationCompat;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.notifications.OsmandNotification;

public class CarAppNotification extends OsmandNotification {

	private static final String GROUP_NAME = "CAR_APP";

	public CarAppNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@Override
	public void init() {
	}

	@Override
	public NotificationType getType() {
		return NotificationType.CAR_APP;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_HIGH;
	}

	@Override
	public boolean isActive() {
		NavigationService service = app.getNavigationService();
		return isEnabled() && service != null;
	}

	@Override
	public boolean isEnabled() {
		return app.getCarNavigationSession() != null;
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public NotificationCompat.Builder buildNotification(boolean wearable) {
		String notificationTitle;
		String notificationText;
		icon = R.drawable.ic_action_osmand_logo;
		notificationTitle = "OsmAnd Android Auto";
		notificationText = "Running...";

		return createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));
	}

	@Override
	public int getOsmandNotificationId() {
		return CAR_APP_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_CAR_APP_NOTIFICATION_SERVICE_ID;
	}
}
