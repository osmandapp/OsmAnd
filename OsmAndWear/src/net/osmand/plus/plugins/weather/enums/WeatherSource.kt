package net.osmand.plus.plugins.weather.enums

import androidx.annotation.StringRes
import net.osmand.plus.R

enum class WeatherSource(
	@StringRes val titleId: Int,
	@StringRes val descriptionId: Int,
	val settingValue: String) {
	GFS(R.string.weather_source_GFS_title, R.string.weather_source_GFS_description, "gfs"),
	ECMWF(R.string.weather_source_ecmwf_title, R.string.weather_source_ecmwf_description, "ecmwf");

	companion object {
		fun getDefaultSource(): WeatherSource {
			return GFS
		}

		fun getWeatherSourceBySettingsValue(settingsValue: String): WeatherSource {
			for (source in entries) {
				if (source.settingValue == settingsValue) {
					return source
				}
			}
			return getDefaultSource()
		}
	}
}