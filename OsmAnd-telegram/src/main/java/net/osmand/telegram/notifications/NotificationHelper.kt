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

    private var shareLocationNotification: ShareLocationNotification? = null
    private val all = ArrayList<TelegramNotification>()

    init {
        init()
    }

    private fun init() {
        val shareLocationNotification = ShareLocationNotification(app)
        this.shareLocationNotification = shareLocationNotification
        all.add(shareLocationNotification)
    }

    fun buildTopNotification(): Notification? {
        val notification = acquireTopNotification()
        if (notification != null) {
            removeNotification(notification.type)
            setTopNotification(notification)
            val notificationBuilder = notification.buildNotification(false)
            return notificationBuilder?.build()
        }
        return null
    }

    private fun acquireTopNotification(): TelegramNotification? {
        var notification: TelegramNotification? = null
        if (shareLocationNotification!!.isEnabled && shareLocationNotification!!.isActive) {
            notification = shareLocationNotification
        }
        return notification
    }

    fun updateTopNotification() {
        val notification = acquireTopNotification()
        setTopNotification(notification)
    }

    private fun setTopNotification(notification: TelegramNotification?) {
        for (n in all) {
            n.isTop = n === notification
        }
    }

    fun showNotifications() {
        if (!hasAnyTopNotification()) {
            removeTopNotification()
        }
        for (notification in all) {
            notification.showNotification()
        }
    }

    fun refreshNotification(notificationType: NotificationType) {
        for (notification in all) {
            if (notification.type == notificationType) {
                notification.refreshNotification()
                break
            }
        }
    }

    fun onNotificationDismissed(notificationType: NotificationType) {
        for (notification in all) {
            if (notification.type == notificationType) {
                notification.onNotificationDismissed()
                break
            }
        }
    }

    fun hasAnyTopNotification(): Boolean {
        for (notification in all) {
            if (notification.isTop) {
                return true
            }
        }
        return false
    }

    fun refreshNotifications() {
        if (!hasAnyTopNotification()) {
            removeTopNotification()
        }
        for (notification in all) {
            notification.refreshNotification()
        }
    }

    fun removeTopNotification() {
        val notificationManager = NotificationManagerCompat.from(app)
        notificationManager.cancel(TelegramNotification.TOP_NOTIFICATION_SERVICE_ID)
    }

    fun removeNotification(notificationType: NotificationType) {
        for (notification in all) {
            if (notification.type == notificationType) {
                notification.removeNotification()
                break
            }
        }
    }

    fun removeNotifications() {
        for (notification in all) {
            notification.removeNotification()
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
