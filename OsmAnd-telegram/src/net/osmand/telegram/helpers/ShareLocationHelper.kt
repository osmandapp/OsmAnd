package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.*
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidNetworkUtils
import net.osmand.telegram.utils.BASE_URL
import org.drinkless.td.libcore.telegram.TdApi
import org.json.JSONException
import org.json.JSONObject

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
				val latitude = location.latitude
				val longitude = location.longitude
				val user = app.telegramHelper.getCurrentUser()
				val sharingMode = app.settings.currentSharingMode

				if (user != null && sharingMode == user.id.toString()) {
					when (app.settings.shareTypeValue) {
						SHARE_TYPE_MAP -> app.telegramHelper.sendLiveLocationMessage(chatsShareInfo, latitude, longitude)
						SHARE_TYPE_TEXT -> app.telegramHelper.sendLiveLocationText(chatsShareInfo, location)
						SHARE_TYPE_MAP_AND_TEXT -> {
							app.telegramHelper.sendLiveLocationMessage(chatsShareInfo, latitude, longitude)
							app.telegramHelper.sendLiveLocationText(chatsShareInfo, location)
						}
					}
				} else if (sharingMode.isNotEmpty()) {
					val url = getDeviceSharingUrl(location,sharingMode)
					AndroidNetworkUtils.sendRequestAsync(app, url, null, "Send Location", false, false,
						object : AndroidNetworkUtils.OnRequestResultListener {
							override fun onResult(result: String?) {
								updateShareInfoSuccessfulSendTime(result, chatsShareInfo)

								val osmandBot = app.telegramHelper.getOsmandBot()
								if (osmandBot != null) {
									checkAndSendViaBotMessages(chatsShareInfo, TdApi.Location(latitude, longitude), osmandBot)
								}
							}
						})
				}
			}
			lastLocationMessageSentTime = System.currentTimeMillis()
		}
		app.settings.updateSharingStatusHistory()
		refreshNotification()
	}

	fun updateSendLiveMessages() {
		log.info("updateSendLiveMessages")
		if (app.settings.hasAnyChatToShareLocation()) {
			app.settings.getChatsShareInfo().forEach { (chatId, shareInfo) ->
				val currentTime = System.currentTimeMillis() / 1000
				when {
					shareInfo.getChatLiveMessageExpireTime() <= 0 -> app.settings.shareLocationToChat(chatId, false)
					currentTime > shareInfo.currentMessageLimit -> {
						shareInfo.apply {
							val newLivePeriod =
								if (livePeriod > TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC) {
									livePeriod - TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC
								} else {
									livePeriod
								}
							livePeriod = newLivePeriod
							shouldDeletePreviousMapMessage = true
							shouldDeletePreviousTextMessage = true
							currentMessageLimit = currentTime + Math.min(newLivePeriod, TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong())
						}
					}
					shareInfo.userSetLivePeriod != shareInfo.livePeriod
							&& (shareInfo.userSetLivePeriodStart + USER_SET_LIVE_PERIOD_DELAY_MS) > currentTime -> {
						shareInfo.apply {
							shouldDeletePreviousMapMessage = true
							shouldDeletePreviousTextMessage = true
							livePeriod = shareInfo.userSetLivePeriod
							currentMessageLimit = currentTime + Math.min(livePeriod, TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong())
						}
					}
				}
			}
		} else {
			stopSharingLocation()
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

	private fun getDeviceSharingUrl(loc: Location, sharingMode: String): String {
		val url = "$BASE_URL/device/$sharingMode/send?lat=${loc.latitude}&lon=${loc.longitude}"
		val builder = StringBuilder(url)
		if (loc.hasBearing() && loc.bearing != 0.0f) {
			builder.append("&azi=${loc.bearing}")
		}
		if (loc.hasSpeed() && loc.speed != 0.0f) {
			builder.append("&spd=${loc.speed}")
		}
		if (loc.hasAltitude() && loc.altitude != 0.0) {
			builder.append("&alt=${loc.altitude}")
		}
		if (loc.hasAccuracy() && loc.accuracy != 0.0f) {
			builder.append("&hdop=${loc.accuracy}")
		}
		return builder.toString()
	}

	private fun updateShareInfoSuccessfulSendTime(result: String?, chatsShareInfo: Map<Long, TelegramSettings.ShareChatInfo>) {
		if (result != null) {
			try {
				val jsonResult = JSONObject(result)
				val status = jsonResult.getString("status")
				val currentTime = System.currentTimeMillis()
				if (status == "OK") {
					chatsShareInfo.forEach { (_, shareInfo) ->
						shareInfo.lastSuccessfulSendTimeMs = currentTime
					}
				}
			} catch (e: JSONException) {
			}
		}
	}

	private fun checkAndSendViaBotMessages(chatsShareInfo: Map<Long, TelegramSettings.ShareChatInfo>, location: TdApi.Location, osmandBot: TdApi.User) {
		val device = app.settings.getCurrentSharingDevice()
		if (device != null) {
			chatsShareInfo.forEach { (_, shareInfo) ->
				if (shareInfo.shouldSendViaBotMessage) {
					app.telegramHelper.sendViaBotLocationMessage(osmandBot.id, shareInfo, location, device,app.settings.shareTypeValue)
					shareInfo.shouldSendViaBotMessage = false
				}
			}
		}
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
