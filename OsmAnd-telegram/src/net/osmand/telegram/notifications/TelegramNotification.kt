package net.osmand.telegram.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import net.osmand.telegram.MainActivity
import net.osmand.telegram.TelegramApplication


abstract class TelegramNotification(protected var app: TelegramApplication, val groupName: String) {

	companion object {

		const val SHARE_LOCATION_NOTIFICATION_SERVICE_ID = 6
		const val SHOW_LOCATION_NOTIFICATION_SERVICE_ID = 7

		const val WEAR_SHARE_LOCATION_NOTIFICATION_SERVICE_ID = 1006
		const val WEAR_SHOW_LOCATION_NOTIFICATION_SERVICE_ID = 1006
	}

	protected var ongoing = true
	protected var color: Int = 0
	protected var icon: Int = 0

	abstract val type: NotificationType

	abstract val telegramNotificationId: Int

	abstract val telegramWearableNotificationId: Int

	abstract val priority: Int

	abstract val isActive: Boolean

	abstract val isEnabled: Boolean

	enum class NotificationType {
		SHARE_LOCATION,
		SHOW_LOCATION
	}

	@SuppressLint("InlinedApi")
	protected fun createBuilder(wearable: Boolean): NotificationCompat.Builder {
		val contentIntent = Intent(app, MainActivity::class.java)
		val contentPendingIntent = PendingIntent.getActivity(app, 0, contentIntent,
				PendingIntent.FLAG_UPDATE_CURRENT)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			app.notificationHelper.createNotificationChannel()
		}
		val builder = NotificationCompat.Builder(app, NotificationHelper.NOTIFICATION_CHANEL_ID)
				.setVisibility(android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC)
				.setPriority(priority)
				.setOngoing(ongoing && !wearable)
				.setContentIntent(contentPendingIntent)
				.setGroup(groupName).setGroupSummary(!wearable)

		if (color != 0) {
			builder.color = color
		}
		if (icon != 0) {
			builder.setSmallIcon(icon)
		}

		return builder
	}

	abstract fun buildNotification(wearable: Boolean): NotificationCompat.Builder

	private fun notifyWearable(notificationManager: NotificationManagerCompat) {
		val wearNotificationBuilder = buildNotification(true)
		val wearNotification = wearNotificationBuilder.build()
		notificationManager.notify(telegramWearableNotificationId, wearNotification)
	}

	fun refreshNotification(): Boolean {
		val notificationManager = NotificationManagerCompat.from(app)
		if (isEnabled) {
			val notificationBuilder = buildNotification(false)
			val notification = notificationBuilder.build()
			notificationManager.notify(telegramNotificationId, notification)
			notifyWearable(notificationManager)
			return true
		} else {
			notificationManager.cancel(telegramNotificationId)
		}
		return false
	}
}
