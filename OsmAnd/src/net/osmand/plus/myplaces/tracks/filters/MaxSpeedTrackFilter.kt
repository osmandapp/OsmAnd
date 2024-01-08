package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.MAX_SPEED
import net.osmand.plus.settings.enums.MetricsConstants

class MaxSpeedTrackFilter(
	minValue: Float,
	maxValue: Float,
	app: OsmandApplication, filterChangedListener: FilterChangedListener?) : RangeTrackFilter(
	minValue,
	maxValue,
	app, R.string.max_speed, MAX_SPEED, filterChangedListener) {

	private var coef = 1f

	override val unitResId: Int
		get() {
			val settings = app.settings
			val mc = settings.METRIC_SYSTEM.get()
			return when (mc!!) {
				MetricsConstants.MILES_AND_METERS,
				MetricsConstants.MILES_AND_FEET,
				MetricsConstants.MILES_AND_YARDS -> R.string.mile_per_hour

				MetricsConstants.NAUTICAL_MILES_AND_FEET,
				MetricsConstants.NAUTICAL_MILES_AND_METERS -> R.string.nm_h

				MetricsConstants.KILOMETERS_AND_METERS -> R.string.km_h
			}
		}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val maxSpeed = trackItem.dataItem?.analysis?.maxSpeed
		if (maxSpeed == null || (maxSpeed == 0f)) {
			return false
		}

		val normalizedValue = maxSpeed * coef
		return normalizedValue > valueFrom && normalizedValue < valueTo
				|| normalizedValue < minValue && valueFrom == minValue
				|| normalizedValue > maxValue && valueTo == maxValue
	}

	override fun initFilter() {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		coef = when (mc!!) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> 2.237f

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> 1.94384f

			MetricsConstants.KILOMETERS_AND_METERS -> 3.6f
		}
	}

}