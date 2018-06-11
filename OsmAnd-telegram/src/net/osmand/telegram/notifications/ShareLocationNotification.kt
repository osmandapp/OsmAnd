package net.osmand.telegram.notifications

import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.util.Algorithms

class ShareLocationNotification(app: TelegramApplication) : TelegramNotification(app, GROUP_NAME) {

	companion object {
		const val GROUP_NAME = "share_location"
	}

	override val type: TelegramNotification.NotificationType
		get() = TelegramNotification.NotificationType.SHARE_LOCATION

	override val priority: Int
		get() = NotificationCompat.PRIORITY_DEFAULT

	override val isActive: Boolean
		get() {
			val service = app.myLocationService
			return isEnabled && service != null
		}

	override val isEnabled: Boolean
		get() = app.settings.hasAnyChatToShareLocation()

	override val telegramNotificationId: Int
		get() = TelegramNotification.SHARE_LOCATION_NOTIFICATION_SERVICE_ID

	override val telegramWearableNotificationId: Int
		get() = TelegramNotification.WEAR_SHARE_LOCATION_NOTIFICATION_SERVICE_ID

	override fun buildNotification(wearable: Boolean): NotificationCompat.Builder {
		icon = R.drawable.ic_action_polygom_dark
		val shareLocationHelper = app.shareLocationHelper
		val sharedDistance = shareLocationHelper.distance.toFloat()
		color = ContextCompat.getColor(app, R.color.osmand_orange)
		val notificationTitle = (app.getString(R.string.sharing_location) + " • "
				+ Algorithms.formatDuration((shareLocationHelper.duration / 1000).toInt(), true))
		val notificationText = (app.getString(R.string.shared_string_distance)
				+ ": " + OsmandFormatter.getFormattedDistance(sharedDistance, app))

		return createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
	}
}
