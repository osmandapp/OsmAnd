package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.LENGTH
import net.osmand.plus.settings.enums.MetricsConstants
import net.osmand.plus.utils.OsmAndFormatter

class LengthTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(app, R.string.routing_attr_length_name, LENGTH, filterChangedListener) {

	override val unitResId: Int
		get() {
			val settings = app.settings
			val mc = settings.METRIC_SYSTEM.get()
			return when (mc!!) {
				MetricsConstants.MILES_AND_METERS,
				MetricsConstants.MILES_AND_FEET,
				MetricsConstants.MILES_AND_YARDS -> R.string.mile

				MetricsConstants.NAUTICAL_MILES_AND_FEET,
				MetricsConstants.NAUTICAL_MILES_AND_METERS -> R.string.nm

				MetricsConstants.KILOMETERS_AND_METERS -> R.string.km
			}
		}

	init {
		maxValue = 700f
		setValueTo(maxValue, false)
	}

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val length = trackItem.dataItem?.analysis?.totalDistance
			if (length == null || (length == 0f)) {
				return true
			}
			val settings = app.settings
			val mc = settings.METRIC_SYSTEM.get()
			val coef = when (mc!!) {
				MetricsConstants.MILES_AND_METERS,
				MetricsConstants.MILES_AND_FEET,
				MetricsConstants.MILES_AND_YARDS ->
					OsmAndFormatter.METERS_IN_ONE_MILE

				MetricsConstants.NAUTICAL_MILES_AND_FEET,
				MetricsConstants.NAUTICAL_MILES_AND_METERS ->
					OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE

				MetricsConstants.KILOMETERS_AND_METERS -> OsmAndFormatter.METERS_IN_KILOMETER
			}
			var normalizedValue = length / coef

			return normalizedValue < getValueFrom() || normalizedValue > getValueTo()
		}
		return false
	}
}