package net.osmand.plus.plugins.odb

import net.osmand.plus.plugins.weather.units.TemperatureUnit

interface OBDWidgetOptions {
	fun getTemperatureUnit(): TemperatureUnit
}