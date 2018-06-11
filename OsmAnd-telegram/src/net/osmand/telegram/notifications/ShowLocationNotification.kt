package net.osmand.telegram.notifications

import android.support.v4.app.NotificationCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication

class ShowLocationNotification(app: TelegramApplication) : TelegramNotification(app, GROUP_NAME) {

	companion object {

		const val GROUP_NAME = "show_location"
	}

	override val type: TelegramNotification.NotificationType
		get() = NotificationType.SHOW_LOCATION

	override val priority: Int
		get() = NotificationCompat.PRIORITY_DEFAULT

	override val isActive: Boolean
		get() {
			val service = app.userLocationService
			return isEnabled && service != null
		}

	override val isEnabled: Boolean
		get() = app.settings.hasAnyChatToShowOnMap()

	override val telegramNotificationId: Int
		get() = TelegramNotification.SHOW_LOCATION_NOTIFICATION_SERVICE_ID

	override val telegramWearableNotificationId: Int
		get() = TelegramNotification.WEAR_SHOW_LOCATION_NOTIFICATION_SERVICE_ID

	override fun buildNotification(wearable: Boolean): NotificationCompat.Builder {
		val notificationTitle: String = app.getString(R.string.show_users_on_map)
		val notificationText: String = app.getString(R.string.active_chats) + ": " + app.settings.getShowOnMapChatsCount()
		color = 0
		icon = R.drawable.ic_action_view

		return createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
	}
}
