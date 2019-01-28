package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.*
import net.osmand.telegram.helpers.LocationMessages.BufferMessage
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
			if (app.settings.getChatsShareInfo().isNotEmpty()) {
				shareLocationMessages(location, app.telegramHelper.getCurrentUserId())
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

	private fun shareLocationMessages(location: Location, userId: Int) {
		val chatsShareInfo = app.settings.getChatsShareInfo()
		val latitude = location.latitude
		val longitude = location.longitude
		val sharingMode = app.settings.currentSharingMode
		val isBot = sharingMode != userId.toString()
		val types = mutableListOf<Int>()
		var bufferedMessagesFull = false

		when (app.settings.shareTypeValue) {
			SHARE_TYPE_MAP -> {
				types.add(if (isBot) LocationMessages.TYPE_BOT_MAP else LocationMessages.TYPE_USER_MAP)
			}
			SHARE_TYPE_TEXT -> {
				types.add(if (isBot) LocationMessages.TYPE_BOT_TEXT else LocationMessages.TYPE_USER_TEXT)
			}
			SHARE_TYPE_MAP_AND_TEXT -> {
				types.add(if (isBot) LocationMessages.TYPE_BOT_MAP else LocationMessages.TYPE_USER_MAP)
				types.add(if (isBot) LocationMessages.TYPE_BOT_TEXT else LocationMessages.TYPE_USER_TEXT)
			}
		}
		chatsShareInfo.values.forEach { shareInfo ->
			if (shareInfo.pendingTdLib >= 10) {
				bufferedMessagesFull = true
			}
			types.forEach {
				val message = BufferMessage(shareInfo.chatId, latitude, longitude, location.altitude, location.speed.toDouble(),
					location.accuracy.toDouble(), location.bearing.toDouble(), location.time, it, LocationMessages.STATUS_PREPARED)

				when (app.settings.shareTypeValue) {
					SHARE_TYPE_MAP -> {
						prepareMapMessage(shareInfo, message, isBot, sharingMode)
					}
					SHARE_TYPE_TEXT -> {
						prepareTextMessage(shareInfo, message, isBot, sharingMode)
					}
					SHARE_TYPE_MAP_AND_TEXT -> {
						prepareMapMessage(shareInfo, message, isBot, sharingMode)
						prepareTextMessage(shareInfo, message, isBot, sharingMode)
					}
				}
			}
		}
		if (bufferedMessagesFull) {
			checkNetworkType()
		}
	}

	private fun prepareTextMessage(shareInfo: TelegramSettings.ShareChatInfo,message: BufferMessage,isBot:Boolean, sharingMode: String) {
		if (shareInfo.currentTextMessageId == -1L) {
			if (shareInfo.pendingTextMessage) {
				app.locationMessages.addBufferedMessage(message)
			} else {
				if (isBot) {
					sendLocationToBot(message, sharingMode, shareInfo, SHARE_TYPE_TEXT)
				} else {
					shareInfo.pendingTdLib++
					app.telegramHelper.sendNewTextLocation(shareInfo, message)
				}
			}
		} else {
			if (isBot) {
				sendLocationToBot(message, sharingMode, shareInfo, SHARE_TYPE_TEXT)
			} else {
				if (shareInfo.pendingTdLib < 10) {
					shareInfo.pendingTdLib++
					app.telegramHelper.editTextLocation(shareInfo, message)
				} else {
					app.locationMessages.addBufferedMessage(message)
				}
			}
		}
	}

	private fun prepareMapMessage(shareInfo: TelegramSettings.ShareChatInfo,message: BufferMessage,isBot:Boolean, sharingMode: String) {
		if (shareInfo.currentMapMessageId == -1L) {
			if (shareInfo.pendingMapMessage) {
				app.locationMessages.addBufferedMessage(message)
			} else {
				if (isBot) {
					sendLocationToBot(message, sharingMode, shareInfo, SHARE_TYPE_MAP)
				} else {
					shareInfo.pendingTdLib++
					app.telegramHelper.sendNewMapLocation(shareInfo, message)
				}
			}
		} else {
			if (isBot) {
				sendLocationToBot(message, sharingMode, shareInfo, SHARE_TYPE_MAP)
			} else {
				if (shareInfo.pendingTdLib < 10) {
					shareInfo.pendingTdLib++
					app.telegramHelper.editMapLocation(shareInfo, message)
				} else {
					app.locationMessages.addBufferedMessage(message)
				}
			}
		}
	}

	private fun checkNetworkType(){
		if (app.isInternetConnectionAvailable) {
			val networkType = when {
				app.isWifiConnected -> TdApi.NetworkTypeWiFi()
				app.isMobileConnected -> TdApi.NetworkTypeMobile()
				else -> TdApi.NetworkTypeOther()
			}
			app.telegramHelper.networkChange(networkType)
		}
	}

	private fun sendLocationToBot(locationMessage: BufferMessage, sharingMode: String, shareInfo: TelegramSettings.ShareChatInfo, shareType: String) {
		if (app.isInternetConnectionAvailable) {
//			locationMessage.status = LocationMessage.STATUS_PENDING
			val url = getDeviceSharingUrl(locationMessage, sharingMode)
			AndroidNetworkUtils.sendRequestAsync(app, url, null, "Send Location", false, false,
				object : AndroidNetworkUtils.OnRequestResultListener {
					override fun onResult(result: String?) {
						val chatsShareInfo = app.settings.getChatsShareInfo()
						val success = checkResultAndUpdateShareInfoSuccessfulSendTime(result, chatsShareInfo)
						val osmandBotId = app.telegramHelper.getOsmandBot()?.id ?: -1
						val device = app.settings.getCurrentSharingDevice()

//						locationMessage.status = if (success) LocationMessage.STATUS_SENT else LocationMessage.STATUS_ERROR
						if (success && shareInfo.shouldSendViaBotMessage && osmandBotId != -1 && device != null) {
							app.telegramHelper.sendViaBotLocationMessage(osmandBotId, shareInfo, TdApi.Location(locationMessage.lat, locationMessage.lon), device, shareType)
							shareInfo.shouldSendViaBotMessage = false
						}
					}
				})
		}
	}

	private fun getDeviceSharingUrl(loc: BufferMessage, sharingMode: String): String {
		val url = "$BASE_URL/device/$sharingMode/send?lat=${loc.lat}&lon=${loc.lon}"
		val builder = StringBuilder(url)
		if (loc.bearing != 0.0) {
			builder.append("&azi=${loc.bearing}")
		}
		if (loc.speed != 0.0) {
			builder.append("&spd=${loc.speed}")
		}
		if (loc.altitude != 0.0) {
			builder.append("&alt=${loc.altitude}")
		}
		if (loc.hdop != 0.0) {
			builder.append("&hdop=${loc.hdop}")
		}
		return builder.toString()
	}

	private fun checkResultAndUpdateShareInfoSuccessfulSendTime(result: String?, chatsShareInfo: Map<Long, TelegramSettings.ShareChatInfo>):Boolean {
		if (result != null) {
			try {
				val jsonResult = JSONObject(result)
				val status = jsonResult.getString("status")
				val currentTime = System.currentTimeMillis()
				if (status == "OK") {
					chatsShareInfo.forEach { (_, shareInfo) ->
						shareInfo.lastSuccessfulSendTimeMs = currentTime
					}
					return true
				}
			} catch (e: JSONException) {
			}
		}
		return false
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