package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.LENGTH
import net.osmand.plus.settings.enums.MetricsConstants
import net.osmand.plus.utils.OsmAndFormatter
import kotlin.math.ceil
import kotlin.math.floor

class LengthTrackFilter(
	minValue: Float,
	maxValue: Float,
	app: OsmandApplication, filterChangedListener: FilterChangedListener?) : RangeTrackFilter(
	minValue,
	maxValue,
	app, R.string.routing_attr_length_name, LENGTH, filterChangedListener) {

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

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val length = trackItem.dataItem?.analysis?.totalDistance
		if (length == null || (length == 0f)) {
			return false
		}
		var normalizedValue = length
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
			MetricsConstants.MILES_AND_YARDS ->
				OsmAndFormatter.METERS_IN_ONE_MILE

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS ->
				OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE

			MetricsConstants.KILOMETERS_AND_METERS -> OsmAndFormatter.METERS_IN_KILOMETER
		}
	}

	override fun setValueFrom(from: Float, updateListeners: Boolean) {
		super.setValueFrom(from * coef, updateListeners)
	}

	override fun setValueTo(to: Float, updateListeners: Boolean) {
		super.setValueTo(to * coef, updateListeners)
	}

	override fun getDisplayMaxValue(): Int {
		return ceil(if (coef != 0f) maxValue / coef.toInt() else ceil(maxValue)).toInt()
	}

	override fun getDisplayMinValue(): Int {
		return floor(if (coef != 0f) minValue / coef else minValue).toInt()
	}

	override fun getDisplayValueFrom(): Int {
		return floor(if (coef != 0f) valueFrom / coef else valueFrom).toInt()
	}

	override fun getDisplayValueTo(): Int {
		return ceil(if (coef != 0f) valueTo / coef else valueTo).toInt()
	}
}