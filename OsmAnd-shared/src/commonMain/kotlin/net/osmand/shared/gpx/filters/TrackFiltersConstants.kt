package net.osmand.shared.gpx.filters

interface TrackFiltersConstants {
	companion object {
		const val DEFAULT_MAX_VALUE = 300f
		const val DURATION_MAX_VALUE = 300f * 1000 * 60
		const val LENGTH_MAX_VALUE = 700000f
		const val SPEED_MAX_VALUE = 83.33333f
		const val ALTITUDE_MAX_VALUE = 12000f
		const val HEART_RATE_MAX_VALUE = 220
		const val TEMPERATURE_MAX_VALUE = 60
	}
}