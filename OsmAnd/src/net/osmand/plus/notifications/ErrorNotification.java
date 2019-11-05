package net.osmand.plus.notifications;

import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;

public class ErrorNotification extends OsmandNotification {

	private final static String GROUP_NAME = "ERROR";

	public ErrorNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@Override
	public void init() {
	}

	@Override
	public NotificationType getType() {
		return NotificationType.ERROR;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_DEFAULT;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, MapActivity.class);
	}

	@Override
	public NotificationCompat.Builder buildNotification(boolean wearable) {
		String notificationTitle;
		String notificationText;
		icon = R.drawable.ic_action_bug_dark;
		notificationTitle = app.getString(R.string.shared_string_unexpected_error);

		NavigationService service = app.getNavigationService();
		RoutingHelper routingHelper = app.getRoutingHelper();

		boolean following = routingHelper.isFollowingMode();
		boolean planning = routingHelper.isRoutePlanningMode();
		boolean pause = routingHelper.isPauseNavigation();

		boolean gpxEnabled = app.getSavingTrackHelper().getIsRecording() || OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null;
		String usedBy = service != null ? "" + service.getUsedBy() : "X";

		notificationText = "Info: " + (following ? "1" : "") + (planning ? "2" : "") + (pause ? "3" : "") + (gpxEnabled ? "4" : "") + "-" + usedBy + ". "
				+ app.getString(R.string.error_notification_desc);

		return createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));
	}

	@Override
	public int getOsmandNotificationId() {
		return ERROR_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_ERROR_NOTIFICATION_SERVICE_ID;
	}
}
