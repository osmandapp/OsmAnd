package net.osmand.telegram.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.support.v4.app.NotificationManagerCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import java.util.*

class NotificationHelper(private val app: TelegramApplication) {

    val shareLocationNotification = ShareLocationNotification(app)
    val showLocationNotification = ShowLocationNotification(app)

	private val all = listOf(shareLocationNotification, showLocationNotification)

    fun buildNotification(telegramNotification: TelegramNotification): Notification {
        return telegramNotification.buildNotification(false).build()
    }

    fun refreshNotification(notificationType: NotificationType) {
        for (notification in all) {
            if (notification.type == notificationType) {
                notification.refreshNotification()
                break
            }
        }
    }

    @TargetApi(26)
    fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANEL_ID,
                    app.getString(R.string.osmand_service), NotificationManager.IMPORTANCE_LOW)
            channel.enableVibration(false)
            channel.description = app.getString(R.string.osmand_service_descr)
            val mNotificationManager = app
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANEL_ID = "osmand_telegram_background_service"
    }
}
