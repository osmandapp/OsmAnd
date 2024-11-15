package net.osmand.telegram.utils

import net.osmand.PlatformUtil
import net.osmand.telegram.SHARE_DEVICES_KEY
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.helpers.getName
import org.drinkless.tdlib.TdApi
import org.json.JSONException
import org.json.JSONObject

const val BASE_URL = "https://live.osmand.net"

const val BASE_SHARING_URL = "http://osmand.net/go"
const val GEO_IP_URL = "https://osmand.net/api/geo-ip"

object OsmandApiUtils {

	private val log = PlatformUtil.getLog(OsmandApiUtils::class.java)

	fun updateSharingDevices(app: TelegramApplication, userId: Long) {
		AndroidNetworkUtils.sendRequestAsync(app, "$BASE_URL/device/send-devices?uid=$userId", null, "Get Devices", true, false,
			object : AndroidNetworkUtils.OnRequestResultListener {
				override fun onResult(result: String?) {
					if (result != null) {
						val list = parseJsonContents(result)
						app.settings.updateShareDevices(list)
					}
				}
			}
		)
	}

	fun createNewDevice(
		app: TelegramApplication,
		user: TdApi.User,
		isBot: Boolean,
		deviceName: String,
		chatId: Long,
		listener: AndroidNetworkUtils.OnRequestResultListener
	) {
		val json = getNewDeviceJson(user, isBot, deviceName, chatId)
		if (json != null) {
			AndroidNetworkUtils.sendRequestAsync(app, "$BASE_URL/device/new", json.toString(), "add Device", true, true, listener)
		}
	}

	fun getLocationByIp(app: TelegramApplication, listener: AndroidNetworkUtils.OnRequestResultListener) {
		AndroidNetworkUtils.sendRequestAsync(app, GEO_IP_URL, null, "get location by IP", false, false, listener)
	}

	fun parseDeviceBot(deviceJSON: JSONObject): TelegramSettings.DeviceBot? {
		return try {
			TelegramSettings.DeviceBot().apply {
				id = deviceJSON.optLong(TelegramSettings.DeviceBot.DEVICE_ID)
				userId = deviceJSON.optLong(TelegramSettings.DeviceBot.USER_ID)
				chatId = deviceJSON.optLong(TelegramSettings.DeviceBot.CHAT_ID)
				deviceName = deviceJSON.optString(TelegramSettings.DeviceBot.DEVICE_NAME)
				externalId = deviceJSON.optString(TelegramSettings.DeviceBot.EXTERNAL_ID)
				data = deviceJSON.optString(TelegramSettings.DeviceBot.DATA)
			}
		} catch (e: JSONException) {
			log.error(e.message, e)
			null
		}
	}

	fun parseJsonContents(contentsJson: String): List<TelegramSettings.DeviceBot> {
		val list = mutableListOf<TelegramSettings.DeviceBot>()
		try {
			val jArray = JSONObject(contentsJson).getJSONArray(SHARE_DEVICES_KEY)
			for (i in 0 until jArray.length()) {
				val deviceJSON = jArray.getJSONObject(i)
				val deviceBot = parseDeviceBot(deviceJSON)
				if (deviceBot != null) {
					list.add(deviceBot)
				}
			}
		} catch (e: JSONException) {
			log.error(e.message, e)
		}
		return list
	}

	private fun getNewDeviceJson(user: TdApi.User, isBot: Boolean, deviceName: String, chatId: Long): JSONObject? {
		return try {
			val json = JSONObject()
			json.put("deviceName", deviceName)
			json.put("chatId", chatId)
			val jsonUser = JSONObject()
			jsonUser.put("id", user.id)
			jsonUser.put("firstName", user.firstName)
			jsonUser.put("isBot", isBot)
			jsonUser.put("lastName", user.lastName)
			jsonUser.put("userName", user.getName())
			jsonUser.put("languageCode", user.languageCode)
			json.put("user", jsonUser)
		} catch (e: JSONException) {
			log.error(e)
			null
		}
	}
}