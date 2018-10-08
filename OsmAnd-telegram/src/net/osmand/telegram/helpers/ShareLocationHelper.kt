package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidNetworkUtils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val UPDATE_LIVE_MESSAGES_INTERVAL_SEC = 10L // 10 sec
private const val USER_SET_LIVE_PERIOD_DELAY_MIL = 5000 // 5 sec

class ShareLocationHelper(private val app: TelegramApplication) {

	private var updateLiveMessagesExecutor: ScheduledExecutorService? = null

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

		if (location != null) {
			val user = app.telegramHelper.getCurrentUser()
			val sharingMode = app.settings.currentSharingMode
			if (user != null && sharingMode == user.id.toString()) {
				app.telegramHelper.sendLiveLocationMessage(app.settings, location.latitude, location.longitude)
			} else if (sharingMode.isNotEmpty()) {
				val url = "https://live.osmand.net/device/$sharingMode/send?lat=${location.latitude}&lon=${location.longitude}"
				AndroidNetworkUtils.sendRequestAsync(url, null)
			}
			lastLocationMessageSentTime = System.currentTimeMillis()
		}
		refreshNotification()
	}

	fun updateSendLiveMessages() {
		app.settings.getChatsShareInfo().forEach { chatId, shareInfo ->
			val currentTime = System.currentTimeMillis() / 1000
			when {
				app.settings.getChatLiveMessageExpireTime(chatId) <= 0 -> app.settings.shareLocationToChat(
					chatId,
					false
				)
				currentTime > shareInfo.currentMessageLimit -> {
					val newLivePeriod =
						shareInfo.livePeriod - TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC
					shareInfo.livePeriod = newLivePeriod
					shareInfo.shouldDeletePreviousMessage = true
					shareInfo.currentMessageLimit = currentTime + Math.min(
						newLivePeriod,
						TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
					)
				}
				shareInfo.userSetLivePeriod != shareInfo.livePeriod
						&& (shareInfo.userSetLivePeriodStart + USER_SET_LIVE_PERIOD_DELAY_MIL) > currentTime -> {
					shareInfo.livePeriod = shareInfo.userSetLivePeriod
					shareInfo.shouldDeletePreviousMessage = true
					shareInfo.currentMessageLimit = currentTime + Math.min(
						shareInfo.livePeriod,
						TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
					)
				}
			}

		}
	}

	fun startSharingLocation() {
		if (!sharingLocation) {
			sharingLocation = true

			app.startMyLocationService()
			startLiveMessagesUpdates(UPDATE_LIVE_MESSAGES_INTERVAL_SEC)
			refreshNotification()
		} else {
			app.forceUpdateMyLocation()
		}
	}

	fun startLiveMessagesUpdates(interval: Long) {
		stopLiveMessagesUpdates()

		updateLiveMessagesExecutor = Executors.newSingleThreadScheduledExecutor()
		updateLiveMessagesExecutor?.scheduleWithFixedDelay({ updateSendLiveMessages() }, interval, interval, TimeUnit.SECONDS)
	}

	fun stopLiveMessagesUpdates() {
		updateLiveMessagesExecutor?.shutdown()
		updateLiveMessagesExecutor?.awaitTermination(1, TimeUnit.MINUTES)
	}

	fun stopSharingLocation() {
		if (sharingLocation) {
			sharingLocation = false
			stopLiveMessagesUpdates()
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
