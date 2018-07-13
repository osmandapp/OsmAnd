package net.osmand.telegram

import android.content.Context
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants

private const val SETTINGS_NAME = "osmand_telegram_settings"

private const val METRICS_CONSTANTS_KEY = "metrics_constants"
private const val SPEED_CONSTANTS_KEY = "speed_constants"

private const val SEND_MY_LOCATION_INTERVAL_KEY = "send_my_location_interval"
private const val SEND_MY_LOCATION_INTERVAL_DEFAULT = 5L * 1000 // 5 seconds

private const val USER_LOCATION_EXPIRE_TIME_KEY = "user_location_expire_time"
private const val USER_LOCATION_EXPIRE_TIME_DEFAULT = 15L * 60 * 1000 // 15 minutes

class TelegramSettings(private val app: TelegramApplication) {

	var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
	var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

	var sendMyLocationInterval = SEND_MY_LOCATION_INTERVAL_DEFAULT
	var userLocationExpireTime = USER_LOCATION_EXPIRE_TIME_DEFAULT

	init {
		read()
	}

	fun save() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
		val edit = prefs.edit()

		edit.putString(METRICS_CONSTANTS_KEY, metricsConstants.name)
		edit.putString(SPEED_CONSTANTS_KEY, speedConstants.name)

		edit.putLong(SEND_MY_LOCATION_INTERVAL_KEY, sendMyLocationInterval)

		edit.apply()
	}

	fun read() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

		metricsConstants = MetricsConstants.valueOf(
			prefs.getString(METRICS_CONSTANTS_KEY, MetricsConstants.KILOMETERS_AND_METERS.name)
		)
		speedConstants = SpeedConstants.valueOf(
			prefs.getString(SPEED_CONSTANTS_KEY, SpeedConstants.KILOMETERS_PER_HOUR.name)
		)

		sendMyLocationInterval =
				prefs.getLong(SEND_MY_LOCATION_INTERVAL_KEY, SEND_MY_LOCATION_INTERVAL_DEFAULT)
		userLocationExpireTime =
				prefs.getLong(USER_LOCATION_EXPIRE_TIME_KEY, USER_LOCATION_EXPIRE_TIME_DEFAULT)
	}
}
