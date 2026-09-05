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
	private val SENSOR_GPX_TAG_SET = SENSOR_GPX_TAGS.toHashSet()

	fun onAnalysePoint(analysis: GpxTrackAnalysis, point: WptPt, attribute: PointAttributes) {
		// Skip entirely if this point has none of the sensor tags: cheaper to walk the
		// point's own (usually small) extension key sets once than to probe all 6 fixed
		// tags against them on every point during bulk indexing.
		if (!hasAnySensorKey(point)) return

		val anyValueSet = attribute.hasAnySensorValueSet()
		for (tag in SENSOR_GPX_TAGS) {
			if (!anyValueSet) {
				val value = getPointAttribute(point, tag, Float.NaN)
				attribute.setAttributeValue(tag, value)
			}

			if (!analysis.hasData(tag) && attribute.hasValidValue(tag)) {
				analysis.setHasData(tag, true)
			}
		}
	}

	private fun hasAnySensorKey(point: WptPt): Boolean {
		val extensions = point.extensions
		val deferred = point.deferredExtensions
		if (extensions.isNullOrEmpty() && deferred.isNullOrEmpty()) {
			return false
		}
		if (extensions != null) {
			for (key in extensions.keys) {
				if (SENSOR_GPX_TAG_SET.contains(key)) return true
			}
		}
		if (deferred != null) {
			for (key in deferred.keys) {
				if (SENSOR_GPX_TAG_SET.contains(key)) return true
			}
		}
		return false
	}

	fun getPointAttribute(wptPt: WptPt, key: String, defaultValue: Float): Float {
		var value = wptPt.getDeferredExtensionsToRead()[key]
		if (value.isNullOrEmpty()) {
			value = wptPt.getExtensionsToRead()[key]
		}
		return KAlgorithms.parseFloatSilently(value, defaultValue)
	}
}