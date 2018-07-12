package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.notifications.TelegramNotification.NotificationType

class ShareLocationHelper(private val app: TelegramApplication) {

	var sharingLocation: Boolean = false
		private set

	var duration: Long = 0
		private set

	var distance: Int = 0
		private set

	var lastLocationMessageSentTime: Long = 0

	private var lastTimeInMillis: Long = 0L

	private var lastLocation: Location? = null
		set(value) {
			if (lastTimeInMillis == 0L) {
				lastTimeInMillis = System.currentTimeMillis()
			} else {
				val currentTimeInMillis = System.currentTimeMillis()
				duration += currentTimeInMillis - lastTimeInMillis
				lastTimeInMillis = currentTimeInMillis
			}
			if (lastLocation != null && value != null) {
				distance += value.distanceTo(lastLocation).toInt()
			}
			field = value
		}

	fun updateLocation(location: Location?) {
		lastLocation = location

		if (location != null && app.isInternetConnectionAvailable) {
			val shareLocationChats = app.settings.getShareLocationChats()
			val savedChatProps = app.telegramDbHelper.getSavedChatProps()
			if (shareLocationChats.isNotEmpty()) {
				app.telegramHelper.sendLiveLocationMessage(shareLocationChats, savedChatProps, location.latitude, location.longitude)
			}
			lastLocationMessageSentTime = System.currentTimeMillis()
		}
		refreshNotification()
	}

	fun startSharingLocation() {
		if (!sharingLocation) {
			sharingLocation = true

			app.startMyLocationService()

			refreshNotification()
		} else {
			app.forceUpdateMyLocation()
		}
	}

	fun stopSharingLocation() {
		if (sharingLocation) {
			sharingLocation = false

			app.stopMyLocationService()
			lastLocation = null
			lastTimeInMillis = 0L
			distance = 0
			duration = 0

			refreshNotification()
		}
	}

	fun pauseSharingLocation() {
		sharingLocation = false

		app.stopMyLocationService()
		lastLocation = null
		lastTimeInMillis = 0L

		refreshNotification()
	}

	private fun refreshNotification() {
		app.runInUIThread {
			app.notificationHelper.refreshNotification(NotificationType.LOCATION)
		}
	}

	companion object {

		// min and max values for the UI
		const val MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC = TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC - 1
		const val MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC = TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC + 1
	}
}
