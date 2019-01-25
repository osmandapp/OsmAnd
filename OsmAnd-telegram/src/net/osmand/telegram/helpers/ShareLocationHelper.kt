package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.*
import net.osmand.telegram.helpers.LocationMessages.LocationMessage
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
				addNewLocationMessages(location, app.telegramHelper.getCurrentUserId())
				shareMessages()
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

	private fun addNewLocationMessages(location: Location, userId: Int) {
		val chatsShareInfo = app.settings.getChatsShareInfo()
		val latitude = location.latitude
		val longitude = location.longitude
		val isBot = app.settings.currentSharingMode != userId.toString()
		val types = mutableListOf<Int>()

		when (app.settings.shareTypeValue) {
			SHARE_TYPE_MAP -> {
				types.add(if (isBot) LocationMessage.TYPE_BOT_MAP else LocationMessage.TYPE_USER_MAP)
			}
			SHARE_TYPE_TEXT -> {
				types.add(if (isBot) LocationMessage.TYPE_BOT_TEXT else LocationMessage.TYPE_USER_TEXT)
			}
			SHARE_TYPE_MAP_AND_TEXT -> {
				types.add(if (isBot) LocationMessage.TYPE_BOT_MAP else LocationMessage.TYPE_USER_MAP)
				types.add(if (isBot) LocationMessage.TYPE_BOT_TEXT else LocationMessage.TYPE_USER_TEXT)
			}
		}
		chatsShareInfo.values.forEach { shareInfo ->
			types.forEach {
				val message = LocationMessage(userId, shareInfo.chatId, latitude, longitude, location.altitude, location.speed.toDouble(),
					location.accuracy.toDouble(), location.bearing.toDouble(), location.time, it, LocationMessage.STATUS_PREPARED, shareInfo.currentMapMessageId)
				app.locationMessages.addLocationMessage(message)
			}
		}
	}

	private fun shareMessages() {
		var bufferedMessagesFull = false
		app.locationMessages.getPreparedToShareMessages().forEach {
			val shareChatInfo = app.settings.getChatsShareInfo()[it.chatId]
			if (shareChatInfo != null) {
				bufferedMessagesFull = shareChatInfo.bufferedMessages < 10
				if (bufferedMessagesFull) {
					when {
						it.type == LocationMessage.TYPE_USER_TEXT -> {
							shareChatInfo.bufferedMessages++
							it.status = LocationMessage.STATUS_PENDING
							app.telegramHelper.sendLiveLocationText(shareChatInfo, it)
						}
						it.type == LocationMessage.TYPE_USER_MAP -> {
							shareChatInfo.bufferedMessages++
							it.status = LocationMessage.STATUS_PENDING
							app.telegramHelper.sendLiveLocationMap(shareChatInfo, it)
						}
						it.type == LocationMessage.TYPE_BOT_TEXT -> {
							sendLocationToBot(it, app.settings.currentSharingMode, shareChatInfo, SHARE_TYPE_TEXT)
						}
						it.type == LocationMessage.TYPE_BOT_MAP -> {
							sendLocationToBot(it, app.settings.currentSharingMode, shareChatInfo, SHARE_TYPE_MAP)
						}
					}
				}
			}
		}
		if (bufferedMessagesFull) {
			checkNetworkType()
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

	private fun sendLocationToBot(locationMessage: LocationMessage, sharingMode: String, shareInfo: TelegramSettings.ShareChatInfo, shareType: String) {
		if (app.isInternetConnectionAvailable) {
			locationMessage.status = LocationMessage.STATUS_PENDING
			val url = getDeviceSharingUrl(locationMessage, sharingMode)
			AndroidNetworkUtils.sendRequestAsync(app, url, null, "Send Location", false, false,
				object : AndroidNetworkUtils.OnRequestResultListener {
					override fun onResult(result: String?) {
						val chatsShareInfo = app.settings.getChatsShareInfo()
						val success = checkResultAndUpdateShareInfoSuccessfulSendTime(result, chatsShareInfo)
						val osmandBotId = app.telegramHelper.getOsmandBot()?.id ?: -1
						val device = app.settings.getCurrentSharingDevice()

						locationMessage.status = if (success) LocationMessage.STATUS_SENT else LocationMessage.STATUS_ERROR
						if (success && shareInfo.shouldSendViaBotMessage && osmandBotId != -1 && device != null) {
							app.telegramHelper.sendViaBotLocationMessage(osmandBotId, shareInfo, TdApi.Location(locationMessage.lat, locationMessage.lon), device, shareType)
							shareInfo.shouldSendViaBotMessage = false
						}
					}
				})
		}
	}

	private fun getDeviceSharingUrl(loc: LocationMessage, sharingMode: String): String {
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