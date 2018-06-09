package net.osmand.telegram.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils

import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.notifications.TelegramNotification.NotificationType

class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val helper = (context.applicationContext as TelegramApplication).notificationHelper
        val notificationTypeStr = intent.extras!!.getString(NOTIFICATION_TYPE_KEY_NAME)
        if (!TextUtils.isEmpty(notificationTypeStr)) {
            try {
                val notificationType = NotificationType.valueOf(notificationTypeStr)
                helper.onNotificationDismissed(notificationType)
            } catch (e: Exception) {
                //ignored
            }

        }
    }

    companion object {

        const val NOTIFICATION_TYPE_KEY_NAME = "net.osmand.telegram.notifications.NotificationType"

        fun createIntent(context: Context, notificationType: NotificationType): PendingIntent {
            val intent = Intent(context, NotificationDismissReceiver::class.java)
            intent.putExtra(NOTIFICATION_TYPE_KEY_NAME, notificationType.name)
            return PendingIntent.getBroadcast(context.applicationContext,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
