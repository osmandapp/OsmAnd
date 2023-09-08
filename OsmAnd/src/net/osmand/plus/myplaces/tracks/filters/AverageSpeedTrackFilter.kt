package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_SPEED
import net.osmand.plus.settings.enums.MetricsConstants

class AverageSpeedTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(app, R.string.average_speed, AVERAGE_SPEED, filterChangedListener) {
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

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val avgSpeed = trackItem.dataItem?.analysis?.avgSpeed
			if (avgSpeed == null || (avgSpeed == 0f)) {
				return true
			}
			val settings = app.settings
			val mc = settings.METRIC_SYSTEM.get()
			val coef = when (mc!!) {
				MetricsConstants.MILES_AND_METERS,
				MetricsConstants.MILES_AND_FEET,
				MetricsConstants.MILES_AND_YARDS -> 2.237f

				MetricsConstants.NAUTICAL_MILES_AND_FEET,
				MetricsConstants.NAUTICAL_MILES_AND_METERS -> 1.94384f

				MetricsConstants.KILOMETERS_AND_METERS -> 3.6f
			}
			val normalizedValue = avgSpeed * coef
			return normalizedValue < getValueFrom() || normalizedValue > getValueTo()
		}
		return false
	}
}