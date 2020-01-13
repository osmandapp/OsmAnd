package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.*
import net.osmand.telegram.helpers.LocationMessages.BufferMessage
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidNetworkUtils
import net.osmand.telegram.utils.BASE_URL
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.util.MapUtils
import org.drinkless.td.libcore.telegram.TdApi
import org.json.JSONException
import org.json.JSONObject

private const val USER_SET_LIVE_PERIOD_DELAY_MS = 5000 // 5 sec

private const val SENT_LOCATIONS_INTERVAL_TIME_MS = 4000 // 4 sec

class ShareLocationHelper(private val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(ShareLocationHelper::class.java)

	var sharingLocation: Boolean = false
		private set

	var duration: Long = 0
		private set

	var distance: Int = 0
		private set

	var lastLocationUpdateTime: Long = 0

	var lastLocationSentTime: Long = 0

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
		val lastPoint = lastLocation
		var record = true
		if (location != null) {
			val minDistance = app.settings.minLocationDistance
			if (minDistance > 0 && lastPoint != null) {
				val calculatedDistance = MapUtils.getDistance(lastPoint.latitude, lastPoint.longitude, location.latitude, location.longitude)
				if (calculatedDistance < minDistance) {
					record = false
				}
			}
			val accuracy = app.settings.minLocationAccuracy
			if (accuracy > 0 && (!location.hasAccuracy() || location.accuracy > accuracy)) {
				record = false
			}
			val minSpeed = app.settings.minLocationSpeed
			if (minSpeed > 0 && (!location.hasSpeed() || location.speed < minSpeed)) {
				record = false
			}

			if (record) {
				lastLocationUpdateTime = System.currentTimeMillis()
				lastLocation = location
				if (app.settings.getChatsShareInfo().isNotEmpty()) {
					shareLocationMessages(location, app.telegramHelper.getCurrentUserId())
				}
			}
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
					shareInfo.getChatLiveMessageExpireTime() <= 0 -> {
						app.settings.shareLocationToChat(chatId, false)
						app.settings.addTimePeriodToLastItem(shareInfo.chatId, shareInfo.livePeriod)
					}
					currentTime > shareInfo.currentMessageLimit -> {
						shareInfo.apply {
							val newLivePeriod =
								if (livePeriod > TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC) {
									livePeriod - TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC
								} else {
									livePeriod
								}
							livePeriod = newLivePeriod
							currentMessageLimit = currentTime + Math.min(newLivePeriod, TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong())
						}
					}
					shareInfo.userSetLivePeriod != shareInfo.livePeriod
							&& (shareInfo.userSetLivePeriodStart + USER_SET_LIVE_PERIOD_DELAY_MS) > currentTime -> {
						shareInfo.apply {
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

	fun checkAndSendBufferMessages() {
		log.debug("checkAndSendBufferMessages")
		var bufferedMessagesFull = false
		app.settings.getChatsShareInfo().forEach { (chatId, shareInfo) ->
			checkAndSendBufferMessagesToChat(chatId)
			if (shareInfo.pendingTdLibText >= MAX_MESSAGES_IN_TDLIB_PER_CHAT || shareInfo.pendingTdLibMap >= MAX_MESSAGES_IN_TDLIB_PER_CHAT) {
				bufferedMessagesFull = true
			}
		}
		if (bufferedMessagesFull) {
			checkNetworkType()
		}
	}

	fun checkAndSendBufferMessagesToChat(chatId: Long) {
		val shareChatsInfo = app.settings.getChatsShareInfo()
		val shareInfo = shareChatsInfo[chatId]
		val chatsCounts = shareChatsInfo.size
		val currentTime = System.currentTimeMillis()
		if (shareInfo != null && currentTime - lastLocationSentTime > chatsCounts * SENT_LOCATIONS_INTERVAL_TIME_MS) {
			app.locationMessages.getBufferedTextMessagesForChat(chatId).take(MAX_MESSAGES_IN_TDLIB_PER_CHAT).forEach {
				if (shareInfo.pendingTdLibText < MAX_MESSAGES_IN_TDLIB_PER_CHAT) {
					if (it.deviceName.isEmpty()) {
						if (!shareInfo.pendingTextMessage && shareInfo.currentTextMessageId != -1L) {
							lastLocationSentTime = System.currentTimeMillis()
							val content = OsmandLocationUtils.getTextMessageContent(shareInfo.updateTextMessageId, it, app)
							app.telegramHelper.editTextLocation(shareInfo, content)
							app.locationMessages.removeBufferedMessage(it)
						}
					} else {
						sendLocationToBot(it, shareInfo, SHARE_TYPE_TEXT)
					}
				}
			}
			app.locationMessages.getBufferedMapMessagesForChat(chatId).take(MAX_MESSAGES_IN_TDLIB_PER_CHAT).forEach {
				if (shareInfo.pendingTdLibMap < MAX_MESSAGES_IN_TDLIB_PER_CHAT) {
					if (it.deviceName.isEmpty()) {
						if (!shareInfo.pendingMapMessage && shareInfo.currentMapMessageId != -1L) {
							lastLocationSentTime = System.currentTimeMillis()
							app.telegramHelper.editMapLocation(shareInfo, it)
							app.locationMessages.removeBufferedMessage(it)
						}
					} else {
						sendLocationToBot(it, shareInfo, SHARE_TYPE_MAP)
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

			checkAndSendBufferMessages()
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
		val altitude = location.altitude
		val speed = location.speed.toDouble()
		val accuracy = location.accuracy.toDouble()
		val bearing = location.bearing.toDouble()
		val time = location.time
		val isBot = app.settings.currentSharingMode != userId.toString()
		val deviceName = if (isBot) app.settings.currentSharingMode else ""
		var bufferedMessagesFull = false
		val chatsCounts = chatsShareInfo.size
		val currentTime = System.currentTimeMillis()

		app.locationMessages.addMyLocationMessage(location)

		if (currentTime - lastLocationSentTime <= chatsCounts * SENT_LOCATIONS_INTERVAL_TIME_MS) {
			return
		}

		chatsShareInfo.values.forEach { shareInfo ->
			if (shareInfo.pendingTdLibText >= MAX_MESSAGES_IN_TDLIB_PER_CHAT || shareInfo.pendingTdLibMap >= MAX_MESSAGES_IN_TDLIB_PER_CHAT) {
				bufferedMessagesFull = true
			}
			checkAndSendBufferMessagesToChat(shareInfo.chatId)
			when (app.settings.shareTypeValue) {
				SHARE_TYPE_MAP -> {
					val message = BufferMessage(shareInfo.chatId, latitude, longitude, altitude, speed, accuracy, bearing, time, LocationMessages.TYPE_MAP, deviceName)
					prepareMapMessage(shareInfo, message, isBot)
				}
				SHARE_TYPE_TEXT -> {
					val message = BufferMessage(shareInfo.chatId, latitude, longitude, altitude, speed, accuracy, bearing, time, LocationMessages.TYPE_TEXT, deviceName)
					prepareTextMessage(shareInfo, message, isBot)
				}
				SHARE_TYPE_MAP_AND_TEXT -> {
					val messageMap = BufferMessage(shareInfo.chatId, latitude, longitude, altitude, speed, accuracy, bearing, time, LocationMessages.TYPE_MAP, deviceName)
					val messageText = BufferMessage(shareInfo.chatId, latitude, longitude, altitude, speed, accuracy, bearing, time, LocationMessages.TYPE_TEXT, deviceName)
					prepareMapMessage(shareInfo, messageMap, isBot)
					prepareTextMessage(shareInfo, messageText, isBot)
				}
			}
		}
		if (bufferedMessagesFull) {
			checkNetworkType()
		}
	}

	private fun prepareTextMessage(shareInfo: TelegramSettings.ShareChatInfo, message: BufferMessage, isBot: Boolean) {
		log.debug("prepareTextMessage $message")
		if (shareInfo.currentTextMessageId == -1L) {
			if (shareInfo.pendingTextMessage) {
				app.locationMessages.addBufferedMessage(message)
			} else {
				if (isBot) {
					sendLocationToBot(message, shareInfo, SHARE_TYPE_TEXT)
				} else {
					lastLocationSentTime = System.currentTimeMillis()
					val content = OsmandLocationUtils.getTextMessageContent(shareInfo.updateTextMessageId, message, app)
					app.telegramHelper.sendNewTextLocation(shareInfo, content)
				}
			}
		} else {
			if (isBot) {
				if (app.isInternetConnectionAvailable) {
					sendLocationToBot(message, shareInfo, SHARE_TYPE_TEXT)
				} else {
					app.locationMessages.addBufferedMessage(message)
				}
			} else {
				if (shareInfo.pendingTdLibText < MAX_MESSAGES_IN_TDLIB_PER_CHAT) {
					lastLocationSentTime = System.currentTimeMillis()
					val content = OsmandLocationUtils.getTextMessageContent(shareInfo.updateTextMessageId, message, app)
					app.telegramHelper.editTextLocation(shareInfo, content)
				} else {
					app.locationMessages.addBufferedMessage(message)
				}
			}
		}
	}

	private fun prepareMapMessage(shareInfo: TelegramSettings.ShareChatInfo, message: BufferMessage, isBot: Boolean) {
		log.debug("prepareMapMessage $message")
		if (shareInfo.currentMapMessageId == -1L) {
			if (shareInfo.pendingMapMessage) {
				app.locationMessages.addBufferedMessage(message)
			} else {
				if (isBot) {
					if (app.isInternetConnectionAvailable) {
						sendLocationToBot(message, shareInfo, SHARE_TYPE_MAP)
					} else {
						app.locationMessages.addBufferedMessage(message)
					}
				} else {
					lastLocationSentTime = System.currentTimeMillis()
					app.telegramHelper.sendNewMapLocation(shareInfo, message)
				}
			}
		} else {
			if (isBot) {
				if (app.isInternetConnectionAvailable) {
					sendLocationToBot(message, shareInfo, SHARE_TYPE_MAP)
				} else {
					app.locationMessages.addBufferedMessage(message)
				}
			} else {
				if (shareInfo.pendingTdLibMap < MAX_MESSAGES_IN_TDLIB_PER_CHAT) {
					lastLocationSentTime = System.currentTimeMillis()
					app.telegramHelper.editMapLocation(shareInfo, message)
				} else {
					app.locationMessages.addBufferedMessage(message)
				}
			}
		}
	}

	fun checkNetworkType(){
		if (app.isInternetConnectionAvailable) {
			val networkType = when {
				app.isWifiConnected -> TdApi.NetworkTypeWiFi()
				app.isMobileConnected -> TdApi.NetworkTypeMobile()
				else -> TdApi.NetworkTypeOther()
			}
			app.telegramHelper.networkChange(networkType)
		}
	}

	private fun sendLocationToBot(locationMessage: BufferMessage, shareInfo: TelegramSettings.ShareChatInfo, shareType: String) {
		if (app.isInternetConnectionAvailable) {
			log.debug("sendLocationToBot ${locationMessage.deviceName}")
			val url = getDeviceSharingUrl(locationMessage, locationMessage.deviceName)
			if (shareType == SHARE_TYPE_TEXT) {
				shareInfo.lastSendTextMessageTime = (System.currentTimeMillis() / 1000).toInt()
			} else if (shareType == SHARE_TYPE_MAP) {
				shareInfo.lastSendMapMessageTime = (System.currentTimeMillis() / 1000).toInt()
			}
			lastLocationSentTime = System.currentTimeMillis()
			AndroidNetworkUtils.sendRequestAsync(app, url, null, "Send Location", false, false,
				object : AndroidNetworkUtils.OnRequestResultListener {
					override fun onResult(result: String?) {
						val success = checkResult(result)
						val osmandBotId = app.telegramHelper.getOsmandBot()?.id ?: -1
						val device = app.settings.getCurrentSharingDevice()
						if (success) {
							shareInfo.sentMessages++
							if (shareType == SHARE_TYPE_TEXT) {
								shareInfo.lastTextSuccessfulSendTime = System.currentTimeMillis() / 1000
							} else if (shareType == SHARE_TYPE_MAP) {
								shareInfo.lastMapSuccessfulSendTime = System.currentTimeMillis() / 1000
							}
							app.locationMessages.removeBufferedMessage(locationMessage)
							if ((shareInfo.shouldSendViaBotTextMessage || shareInfo.shouldSendViaBotMapMessage) && osmandBotId != -1 && device != null) {
								app.telegramHelper.sendViaBotLocationMessage(osmandBotId, shareInfo, TdApi.Location(locationMessage.lat, locationMessage.lon), device, shareType)
								shareInfo.shouldSendViaBotTextMessage = false
								shareInfo.shouldSendViaBotMapMessage = false
							}
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

	private fun checkResult(result: String?): Boolean {
		if (result != null) {
			try {
				val jsonResult = JSONObject(result)
				val status = jsonResult.getString("status")
				return status == "OK"
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

		const val MAX_MESSAGES_IN_TDLIB_PER_CHAT = 10

		// min and max values for the UI
		const val MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC = TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC - 1
		const val MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC = TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC + 1
	}
}