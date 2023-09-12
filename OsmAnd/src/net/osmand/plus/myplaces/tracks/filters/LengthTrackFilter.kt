package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.LENGTH
import net.osmand.plus.settings.enums.MetricsConstants
import net.osmand.plus.utils.OsmAndFormatter

class LengthTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(app, R.string.routing_attr_length_name, LENGTH, filterChangedListener) {

	private var coef = 1f

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
		maxValue = TrackFiltersConstants.LENGTH_MAX_VALUE
		setValueTo(maxValue, false)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isEnabled()) {
			val length = trackItem.dataItem?.analysis?.totalDistance
			if (length == null || (length == 0f)) {
				return false
			}
			var normalizedValue = length / coef

			return normalizedValue > getValueFrom() && normalizedValue < getValueTo()
					|| normalizedValue < minValue && getValueFrom() == minValue
					|| normalizedValue > maxValue && getValueTo() == maxValue
		}
		return true
	}

	override fun updateCoef() {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		coef = when (mc!!) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS ->
				OsmAndFormatter.METERS_IN_ONE_MILE

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS ->
				OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE

			MetricsConstants.KILOMETERS_AND_METERS -> OsmAndFormatter.METERS_IN_KILOMETER
		}
	}
}