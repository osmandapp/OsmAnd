package net.osmand.shared.util

import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.PointAttributes
import net.osmand.shared.gpx.PointAttributes.Companion.SENSOR_TAG_BIKE_POWER
import net.osmand.shared.gpx.PointAttributes.Companion.SENSOR_TAG_CADENCE
import net.osmand.shared.gpx.PointAttributes.Companion.SENSOR_TAG_HEART_RATE
import net.osmand.shared.gpx.PointAttributes.Companion.SENSOR_TAG_SPEED
import net.osmand.shared.gpx.PointAttributes.Companion.SENSOR_TAG_TEMPERATURE_A
import net.osmand.shared.gpx.PointAttributes.Companion.SENSOR_TAG_TEMPERATURE_W
import net.osmand.shared.gpx.primitives.WptPt
import kotlin.text.isNullOrEmpty

object SensorPointAnalyser {
	private val SENSOR_GPX_TAGS = listOf(
		SENSOR_TAG_HEART_RATE,
		SENSOR_TAG_SPEED,
		SENSOR_TAG_CADENCE,
		SENSOR_TAG_BIKE_POWER,
		SENSOR_TAG_TEMPERATURE_W,
		SENSOR_TAG_TEMPERATURE_A
	)

	fun onAnalysePoint(analysis: GpxTrackAnalysis, point: WptPt, attribute: PointAttributes) {
		val anyValueSet = attribute.hasAnyValueSet()
		for (tag in SENSOR_GPX_TAGS) {
			if (!anyValueSet) {
				val defaultValue = if (KCollectionUtils.equalsToAny(tag, SENSOR_TAG_TEMPERATURE_W, SENSOR_TAG_TEMPERATURE_A)) Float.NaN else 0f
				val value = getPointAttribute(point, tag, defaultValue)
				attribute.setAttributeValue(tag, value)
			}

			if (!analysis.hasData(tag) && attribute.hasValidValue(tag)) {
				analysis.setHasData(tag, true)
			}
		}
	}

	fun getPointAttribute(wptPt: WptPt, key: String, defaultValue: Float): Float {
		var value = wptPt.getDeferredExtensionsToRead()[key]
		if (value.isNullOrEmpty()) {
			value = wptPt.getExtensionsToRead()[key]
		}
		return KAlgorithms.parseFloatSilently(value, defaultValue)
	}
}