package net.osmand.plus.charts

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.shared.gpx.PointAttributes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChartUtilsSensorSpeedTest {

	@Test
	fun missingSensorSpeedDuringStopIsPlottedAsZero() {
		val attributes = createAttributes(
			Float.NaN to 0f, 5f to 5f, Float.NaN to 0f, Float.NaN to 0f, 5f to 5f, Float.NaN to 0f
		)
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.TIME, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(1f to 5f, 2f to 0f, 3f to 0f, 4f to 5f), entries.map { it.x to it.y })
	}

	@Test
	fun missingSensorSpeedDuringStopIsPlottedAsZeroPace() {
		val attributes = createAttributes(5f to 5f, Float.NaN to 0f, 5f to 5f)
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.TIME, 1f, Float.NaN, 1000f / 60f, false
		)
		val pace = 1000f / 60f / 5f
		assertEquals(listOf(pace, 0f, pace), entries.map { it.y })
	}

	@Test
	fun missingSensorSpeedWhileMovingIsNotPlotted() {
		val attributes = createAttributes(5f to 5f, Float.NaN to 5f, 5f to 5f)
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.TIME, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(0f to 5f, 2f to 5f), entries.map { it.x to it.y })
	}

	@Test
	fun gpsJitterDuringStopIsPlottedAsZero() {
		val attributes = createAttributes(
			5f to 5f, Float.NaN to 0f, Float.NaN to 0f, Float.NaN to 0f, Float.NaN to 0f, Float.NaN to 0f, 5f to 5f
		)
		attributes[3].distance = 0.3f
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.TIME, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(0f to 5f, 1f to 0f, 2f to 0f, 3f to 0f, 4f to 0f, 5f to 0f, 6f to 5f), entries.map { it.x to it.y })
	}

	@Test
	fun stopInsideSensorDropoutIsNotPlotted() {
		val attributes = createAttributes(
			5f to 5f, Float.NaN to 5f, Float.NaN to 5f, Float.NaN to 0f, Float.NaN to 5f, Float.NaN to 5f, 5f to 5f
		)
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.TIME, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(0f to 5f, 6f to 5f), entries.map { it.x to it.y })
	}

	@Test
	fun missingSensorSpeedWithoutGpsSpeedWhileMovingIsNotPlotted() {
		val attributes = createAttributes(5f to 5f, Float.NaN to 0f, 5f to 5f)
		attributes[1].distance = 5f
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.TIME, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(0f to 5f, 2f to 5f), entries.map { it.x to it.y })
	}

	@Test
	fun missingSensorSpeedWithUnknownGpsSpeedIsNotPlotted() {
		val attributes = createAttributes(5f to 5f, Float.NaN to 0f, 5f to 5f)
		attributes[1].distance = 5f
		attributes[1].timeDiff = 0f
		attributes[2].distance = 5f
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_SPEED, attributes, GPXDataSetAxisType.DISTANCE, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(0f to 5f, 10f to 5f), entries.map { it.x to it.y })
	}

	@Test
	fun missingHeartRateIsNotPlotted() {
		val attributes = createAttributes(5f to 5f, 5f to 0f, 5f to 5f)
		attributes.forEachIndexed { index, attribute ->
			attribute.setAttributeValue(PointAttributes.SENSOR_TAG_HEART_RATE, if (index == 1) Float.NaN else 100f)
		}
		val entries = ChartUtils.getPointAttributeValues(
			PointAttributes.SENSOR_TAG_HEART_RATE, attributes, GPXDataSetAxisType.TIME, 1f, 1f, Float.NaN, false
		)
		assertEquals(listOf(0f to 100f, 2f to 100f), entries.map { it.x to it.y })
	}

	private fun createAttributes(vararg sensorAndGpsSpeeds: Pair<Float, Float>): List<PointAttributes> {
		return sensorAndGpsSpeeds.mapIndexed { index, (sensorSpeed, gpsSpeed) ->
			PointAttributes(0f, if (index == 0) 0f else 1f, false, false).apply {
				speed = gpsSpeed
				setAttributeValue(PointAttributes.SENSOR_TAG_SPEED, sensorSpeed)
			}
		}
	}
}
