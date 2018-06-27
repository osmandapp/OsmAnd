package net.osmand.telegram.notifications

import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.util.Algorithms

private const val GROUP_NAME = "share_location"

class LocationNotification(app: TelegramApplication) : TelegramNotification(app, GROUP_NAME) {

	override val type: TelegramNotification.NotificationType
		get() = TelegramNotification.NotificationType.LOCATION

	override val priority: Int
		get() = NotificationCompat.PRIORITY_DEFAULT

	override val isActive: Boolean
		get() {
			val service = app.telegramService
			return isEnabled && service != null
		}

	override val isEnabled: Boolean
		get() = app.settings.hasAnyChatToShareLocation() || app.settings.hasAnyChatToShowOnMap()

	override val telegramNotificationId: Int
		get() = TelegramNotification.LOCATION_NOTIFICATION_SERVICE_ID

	override val telegramWearableNotificationId: Int
		get() = TelegramNotification.WEAR_LOCATION_NOTIFICATION_SERVICE_ID

	override fun buildNotification(wearable: Boolean): NotificationCompat.Builder {
		val notificationTitle: String
		val notificationText: String
		val shareLocationHelper = app.shareLocationHelper
		if (shareLocationHelper.sharingLocation) {
			val sharedDistance = shareLocationHelper.distance.toFloat()
			color = ContextCompat.getColor(app, R.color.osmand_orange)
			icon = R.drawable.ic_action_polygom_dark
			notificationTitle = (app.getString(R.string.sharing_location) + " • "
					+ Algorithms.formatDuration((shareLocationHelper.duration / 1000).toInt(), true))
			notificationText = (app.getString(R.string.shared_string_distance)
					+ ": " + OsmandFormatter.getFormattedDistance(sharedDistance, app))
		} else {
			notificationTitle = app.getString(R.string.show_users_on_map)
			notificationText = app.getString(R.string.active_chats) + ": " + app.settings.getShowOnMapChatsCount()
			color = 0
			icon = R.drawable.ic_action_view
		}

		return createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
	}
}
