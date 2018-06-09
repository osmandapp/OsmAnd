package net.osmand.telegram.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.util.Algorithms

class ShareLocationNotification(app: TelegramApplication) : TelegramNotification(app, GROUP_NAME) {

    private var wasNoDataDismissed: Boolean = false
    private var lastBuiltNoData: Boolean = false

    override val type: TelegramNotification.NotificationType
        get() = TelegramNotification.NotificationType.SHARE_LOCATION

    override val priority: Int
        get() = NotificationCompat.PRIORITY_DEFAULT

    override val isActive: Boolean
        get() {
            val service = app.locationService
            return isEnabled && service != null
        }

    override val isEnabled: Boolean
        get() = app.settings.hasAnyChatToShareLocation()

    override val osmandNotificationId: Int
        get() = TelegramNotification.SHARE_LOCATION_NOTIFICATION_SERVICE_ID

    override val osmandWearableNotificationId: Int
        get() = TelegramNotification.WEAR_SHARE_LOCATION_NOTIFICATION_SERVICE_ID

    init {
        app.registerReceiver(object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                app.shareLocationHelper.startSharingLocation()
            }
        }, IntentFilter(OSMAND_START_LOCATION_SHARING_SERVICE_ACTION))

        app.registerReceiver(object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                app.shareLocationHelper.pauseSharingLocation()
            }
        }, IntentFilter(OSMAND_PAUSE_LOCATION_SHARING_SERVICE_ACTION))

        app.registerReceiver(object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                app.shareLocationHelper.stopSharingLocation()
            }
        }, IntentFilter(OSMAND_STOP_LOCATION_SHARING_SERVICE_ACTION))
    }

    override fun onNotificationDismissed() {
        if (!wasNoDataDismissed) {
            wasNoDataDismissed = lastBuiltNoData
        }
    }

    override fun buildNotification(wearable: Boolean): NotificationCompat.Builder? {
        if (!isEnabled) {
            return null
        }
        val notificationTitle: String
        val notificationText: String
        color = 0
        icon = R.drawable.ic_action_polygom_dark
        val shareLocationHelper = app.shareLocationHelper
        val isSharingLocation = shareLocationHelper.sharingLocation
        val sharedDistance = shareLocationHelper.distance.toFloat()
        ongoing = true
        lastBuiltNoData = false
        if (isSharingLocation) {
            color = ContextCompat.getColor(app, R.color.osmand_orange)
            notificationTitle = (app.getString(R.string.sharing_location) + " • "
                    + Algorithms.formatDuration((shareLocationHelper.duration / 1000).toInt(), true))
            notificationText = (app.getString(R.string.shared_string_distance)
                    + ": " + OsmandFormatter.getFormattedDistance(sharedDistance, app))
        } else {
            if (sharedDistance > 0) {
                notificationTitle = (app.getString(R.string.shared_string_paused) + " • "
                        + Algorithms.formatDuration((shareLocationHelper.duration / 1000).toInt(), true))
                notificationText = (app.getString(R.string.shared_string_distance)
                        + ": " + OsmandFormatter.getFormattedDistance(sharedDistance, app))
            } else {
                ongoing = false
                notificationTitle = app.getString(R.string.share_location)
                notificationText = app.getString(R.string.shared_string_no_data)
                lastBuiltNoData = true
            }
        }

        if ((wasNoDataDismissed || !app.settings.showNotificationAlways) && !ongoing) {
            return null
        }

        val notificationBuilder = createBuilder(wearable)
                .setContentTitle(notificationTitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))

        val stopIntent = Intent(OSMAND_STOP_LOCATION_SHARING_SERVICE_ACTION)
        val stopPendingIntent = PendingIntent.getBroadcast(app, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        if (isSharingLocation) {
            if (app.shareLocationHelper.distance > 0) {
                val pauseIntent = Intent(OSMAND_PAUSE_LOCATION_SHARING_SERVICE_ACTION)
                val pausePendingIntent = PendingIntent.getBroadcast(app, 0, pauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                notificationBuilder.addAction(R.drawable.ic_pause,
                        app.getString(R.string.shared_string_pause), pausePendingIntent)
                notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
                        app.getString(R.string.shared_string_stop), stopPendingIntent)
            } else {
                notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
                        app.getString(R.string.shared_string_stop), stopPendingIntent)
            }
        } else {
            val startIntent = Intent(OSMAND_START_LOCATION_SHARING_SERVICE_ACTION)
            val startPendingIntent = PendingIntent.getBroadcast(app, 0, startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            if (sharedDistance > 0) {
                notificationBuilder.addAction(R.drawable.ic_action_rec_start,
                        app.getString(R.string.shared_string_continue), startPendingIntent)
                notificationBuilder.addAction(R.drawable.ic_action_rec_stop,
                        app.getString(R.string.shared_string_stop), stopPendingIntent)
            } else {
                notificationBuilder.addAction(R.drawable.ic_action_rec_start,
                        app.getString(R.string.shared_string_start), startPendingIntent)
            }
        }

        return notificationBuilder
    }

    companion object {

        const val OSMAND_START_LOCATION_SHARING_SERVICE_ACTION = "osmand_start_location_sharing_service_action"
        const val OSMAND_PAUSE_LOCATION_SHARING_SERVICE_ACTION = "osmand_pause_location_sharing_service_action"
        const val OSMAND_STOP_LOCATION_SHARING_SERVICE_ACTION = "osmand_stop_location_sharing_service_action"
        const val GROUP_NAME = "share_location"
    }
}
