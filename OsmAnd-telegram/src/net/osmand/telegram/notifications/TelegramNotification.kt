package net.osmand.telegram.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat

import net.osmand.telegram.MainActivity
import net.osmand.telegram.TelegramApplication


abstract class TelegramNotification(protected var app: TelegramApplication, val groupName: String) {
    protected var ongoing = true
    protected var color: Int = 0
    protected var icon: Int = 0
    var isTop: Boolean = false

    abstract val type: NotificationType

    abstract val osmandNotificationId: Int

    abstract val osmandWearableNotificationId: Int

    abstract val priority: Int

    abstract val isActive: Boolean

    abstract val isEnabled: Boolean

    enum class NotificationType {
        SHARE_LOCATION
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
                .setPriority(if (isTop) NotificationCompat.PRIORITY_HIGH else priority)
                .setOngoing(ongoing && !wearable)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(NotificationDismissReceiver.createIntent(app, type))
                .setGroup(groupName).setGroupSummary(!wearable)

        if (color != 0) {
            builder.color = color
        }
        if (icon != 0) {
            builder.setSmallIcon(icon)
        }

        return builder
    }

    abstract fun buildNotification(wearable: Boolean): NotificationCompat.Builder?

    fun setupNotification(notification: Notification) {}

    open fun onNotificationDismissed() {}

    private fun notifyWearable(notificationManager: NotificationManagerCompat) {
        val wearNotificationBuilder = buildNotification(true)
        if (wearNotificationBuilder != null) {
            val wearNotification = wearNotificationBuilder.build()
            notificationManager.notify(osmandWearableNotificationId, wearNotification)
        }
    }

    fun showNotification(): Boolean {
        val notificationManager = NotificationManagerCompat.from(app)
        if (isEnabled) {
            val notificationBuilder = buildNotification(false)
            if (notificationBuilder != null) {
                val notification = notificationBuilder.build()
                setupNotification(notification)
                notificationManager.notify(if (isTop) TOP_NOTIFICATION_SERVICE_ID else osmandNotificationId, notification)
                notifyWearable(notificationManager)
                return true
            }
        }
        return false
    }

    fun refreshNotification(): Boolean {
        val notificationManager = NotificationManagerCompat.from(app)
        if (isEnabled) {
            val notificationBuilder = buildNotification(false)
            if (notificationBuilder != null) {
                val notification = notificationBuilder.build()
                setupNotification(notification)
                if (isTop) {
                    notificationManager.cancel(osmandNotificationId)
                    notificationManager.notify(TOP_NOTIFICATION_SERVICE_ID, notification)
                } else {
                    notificationManager.notify(osmandNotificationId, notification)
                }
                notifyWearable(notificationManager)
                return true
            } else {
                notificationManager.cancel(osmandNotificationId)
            }
        } else {
            notificationManager.cancel(osmandNotificationId)
        }
        return false
    }

    fun removeNotification() {
        val notificationManager = NotificationManagerCompat.from(app)
        notificationManager.cancel(osmandNotificationId)
        notificationManager.cancel(osmandWearableNotificationId)
    }

    fun closeSystemDialogs(context: Context) {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)
    }

    companion object {

        const val SHARE_LOCATION_NOTIFICATION_SERVICE_ID = 6
        const val TOP_NOTIFICATION_SERVICE_ID = 100

        const val WEAR_SHARE_LOCATION_NOTIFICATION_SERVICE_ID = 1006
    }
}
