package net.osmand.telegram.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.util.Algorithms

private const val GROUP_NAME = "share_location"
private const val DISABLE_SHARING_ACTION = "disable_sharing_action"
private const val DISABLE_MONITORING_ACTION = "disable_monitoring_action"

class LocationNotification(app: TelegramApplication) : TelegramNotification(app, GROUP_NAME) {

	init {
		app.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(context: Context?, intent: Intent?) {
				app.stopSharingLocation()
			}
		}, IntentFilter(DISABLE_SHARING_ACTION))
		app.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(context: Context?, intent: Intent?) {
				app.stopMonitoring()
			}
		}, IntentFilter(DISABLE_MONITORING_ACTION))
	}

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
		get() = app.settings.hasAnyChatToShareLocation() || isShowingChatsNotificationEnabled()

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
			icon = R.drawable.ic_action_share_location
			notificationTitle = (app.getString(R.string.sharing_location) + " • "
					+ Algorithms.formatDuration((shareLocationHelper.duration / 1000).toInt(), true))
			notificationText = (app.getString(R.string.shared_string_distance)
					+ ": " + OsmandFormatter.getFormattedDistance(sharedDistance, app))
			actionTextId = R.string.disable_all_sharing
			actionIntent = PendingIntent.getBroadcast(
				app,
				0,
				Intent(DISABLE_SHARING_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT
			)
		} else {
			notificationTitle = app.getString(R.string.location_recording_enabled)
			notificationText = app.getString(R.string.active_chats) + ": " + app.settings.getShowOnMapChatsCount()
			color = 0
			icon = R.drawable.ic_action_timeline
			actionTextId = R.string.disable_monitoring
			actionIntent = PendingIntent.getBroadcast(
				app,
				0,
				Intent(DISABLE_MONITORING_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT
			)
		}

		return createBuilder(wearable)
				.setContentTitle(notificationTitle)
				.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
	}

	private fun isShowingChatsNotificationEnabled() = (!app.showLocationHelper.isUseOsmandCallback() || app.settings.monitoringEnabled)
			&& app.isAnyOsmAndInstalled() && app.settings.hasAnyChatToShowOnMap()
}
