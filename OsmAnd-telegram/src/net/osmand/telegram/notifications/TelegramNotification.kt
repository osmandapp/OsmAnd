package net.osmand.telegram.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.MainActivity
import net.osmand.telegram.ui.OPEN_MY_LOCATION_TAB_KEY


abstract class TelegramNotification(protected var app: TelegramApplication, val groupName: String) {

	companion object {

		const val LOCATION_NOTIFICATION_SERVICE_ID = 6

		const val WEAR_LOCATION_NOTIFICATION_SERVICE_ID = 1006
	}

	protected var ongoing = true
	protected var color: Int = 0
	protected var icon: Int = 0

	protected var actionIconId: Int = 0
	protected var actionTextId: Int = 0
	protected var actionIntent: PendingIntent? = null

	abstract val type: NotificationType

	abstract val telegramNotificationId: Int

	abstract val telegramWearableNotificationId: Int

	abstract val priority: Int

	abstract val isActive: Boolean

	abstract val isEnabled: Boolean

	enum class NotificationType {
		LOCATION,
	}

	@SuppressLint("InlinedApi")
	protected fun createBuilder(wearable: Boolean): NotificationCompat.Builder {
		val contentIntent = Intent(app, MainActivity::class.java)
		contentIntent.putExtra(OPEN_MY_LOCATION_TAB_KEY, true)
		val contentPendingIntent = PendingIntent.getActivity(app, 0, contentIntent,
				PendingIntent.FLAG_IMMUTABLE)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			app.notificationHelper.createNotificationChannel()
		}
		val builder = NotificationCompat.Builder(app, NotificationHelper.NOTIFICATION_CHANEL_ID)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
		if (actionTextId != 0 && actionIntent != null) {
			builder.addAction(actionIconId, app.getString(actionTextId), actionIntent)
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
