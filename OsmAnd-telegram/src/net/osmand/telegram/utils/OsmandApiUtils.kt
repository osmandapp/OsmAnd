package net.osmand.telegram.utils

import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings
import org.json.JSONException
import org.json.JSONObject

object OsmandApiUtils {

	fun updateSharingDevices(app: TelegramApplication, userId: Int) {
		AndroidNetworkUtils.sendRequestAsync(
			"https://osmand.net/device/send-devices?uid=$userId",
			object : AndroidNetworkUtils.OnRequestResultListener {
				override fun onResult(result: String) {
					val list = parseJsonContents(result)
					app.settings.updateShareDevicesIds(list)
				}
			}
		)
	}

	fun parseJsonContents(contentsJson: String): List<TelegramSettings.DeviceBot> {
		val list = mutableListOf<TelegramSettings.DeviceBot>()
		try {
			val jArray = JSONObject(contentsJson).getJSONArray("devices")
			for (i in 0 until jArray.length()) {
				val deviceJSON = jArray.getJSONObject(i)
				val deviceBot = TelegramSettings.DeviceBot().apply {
					id = deviceJSON.getLong("id")
					userId = deviceJSON.getLong("userId")
					chatId = deviceJSON.getLong("chatId")
					deviceName = deviceJSON.getString("deviceName")
					externalId = deviceJSON.getString("externalId")
					data = deviceJSON.getString("data")
				}
				list.add(deviceBot)
			}
		} catch (e: JSONException) {

		}
		return list
	}
}