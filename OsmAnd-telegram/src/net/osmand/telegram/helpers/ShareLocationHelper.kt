package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidNetworkUtils

private const val USER_SET_LIVE_PERIOD_DELAY_MS = 5000 // 5 sec

class ShareLocationHelper(private val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(ShareLocationHelper::class.java)

	var sharingLocation: Boolean = false
		private set

	var duration: Long = 0
		private set

	var distance: Int = 0
		private set

	var lastLocationMessageSentTime: Long = 0

	var lastLocation: Location? = null
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

	private var lastTimeInMillis: Long = 0L

	fun updateLocation(location: Location?) {
		lastLocation = location

		if (location != null) {
			val chatsShareInfo = app.settings.getChatsShareInfo()
			if (chatsShareInfo.isNotEmpty()) {
				val user = app.telegramHelper.getCurrentUser()
				val sharingMode = app.settings.currentSharingMode
				if (user != null && sharingMode == user.id.toString()) {
					app.telegramHelper.sendLiveLocationMessage(chatsShareInfo, location.latitude, location.longitude)
				} else if (sharingMode.isNotEmpty()) {
					val url = "https://live.osmand.net/device/$sharingMode/send?lat=${location.latitude}&lon=${location.longitude}"
					AndroidNetworkUtils.sendRequestAsync(url, null)
				}
			}
			lastLocationMessageSentTime = System.currentTimeMillis()
		}
		app.settings.updateSharingStatusHistory()
		refreshNotification()
	}

	fun updateSendLiveMessages() {
		log.info("updateSendLiveMessages")
		app.settings.getChatsShareInfo().forEach { chatId, shareInfo ->
			val currentTime = System.currentTimeMillis() / 1000
			when {
				app.settings.getChatLiveMessageExpireTime(chatId) <= 0 ->
					app.settings.shareLocationToChat(chatId, false)
				currentTime > shareInfo.currentMessageLimit -> {
					shareInfo.apply {
						val newLivePeriod =
							if (livePeriod > TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC) {
								livePeriod - TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC
							} else {
								livePeriod
							}
						livePeriod = newLivePeriod
						shouldDeletePreviousMessage = true
						currentMessageLimit = currentTime + Math.min(
							newLivePeriod, TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong())
					}
				}
				shareInfo.userSetLivePeriod != shareInfo.livePeriod
						&& (shareInfo.userSetLivePeriodStart + USER_SET_LIVE_PERIOD_DELAY_MS) > currentTime -> {
					shareInfo.apply {
						shouldDeletePreviousMessage = true
						livePeriod = shareInfo.userSetLivePeriod
						currentMessageLimit = currentTime + Math.min(
							livePeriod, TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
						)
					}
				}
			}
		}
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
