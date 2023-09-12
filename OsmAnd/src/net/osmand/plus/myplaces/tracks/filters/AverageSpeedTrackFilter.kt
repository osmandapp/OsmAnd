package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_SPEED
import net.osmand.plus.settings.enums.MetricsConstants

class AverageSpeedTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(app, R.string.average_speed, AVERAGE_SPEED, filterChangedListener) {

	@Expose
	override var minValue: Float = 0f

	@Expose
	override var maxValue: Float = TrackFiltersConstants.DEFAULT_MAX_VALUE

	@Expose
	override var valueFrom: Float = minValue

	@Expose
	override var valueTo: Float = maxValue

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
		if (isEnabled()) {
			val avgSpeed = trackItem.dataItem?.analysis?.avgSpeed
			if (avgSpeed == null || (avgSpeed == 0f)) {
				return false
			}
			val normalizedValue = avgSpeed * coef
			return normalizedValue > valueFrom && normalizedValue < valueTo
					|| normalizedValue < minValue && valueFrom == minValue
					|| normalizedValue > maxValue && valueTo == maxValue
		}
		return true
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