package net.osmand.test.junit

import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.settings.enums.VolumeUnit
import net.osmand.shared.settings.enums.MetricsConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleMetricsPluginUnitTest {

	data class TestCase(
		val metersPerLiter: Float,
		val mc: MetricsConstants,
		val volumeUnit: VolumeUnit,
		val expected: Float
	)

	@Test
	fun testGetFormatDistancePerVolume() {
		val testCases = listOf(
			// Kilometers per Liter
			TestCase(15000f, MetricsConstants.KILOMETERS_AND_METERS, VolumeUnit.LITRES, 15.0f),
			// Kilometers per US Gallon
			TestCase(15000f, MetricsConstants.KILOMETERS_AND_METERS, VolumeUnit.US_GALLONS, 56.78115f),
			// Kilometers per Imperial Gallon
			TestCase(15000f, MetricsConstants.KILOMETERS_AND_METERS, VolumeUnit.IMPERIAL_GALLONS, 68.19135f),

			// Miles per Liter
			TestCase(15000f, MetricsConstants.MILES_AND_FEET, VolumeUnit.LITRES, 9.3206f),
			// Miles per US Gallon
			TestCase(15000f, MetricsConstants.MILES_AND_FEET, VolumeUnit.US_GALLONS, 35.282f),
			// Miles per Imperial Gallon
			TestCase(15000f, MetricsConstants.MILES_AND_FEET, VolumeUnit.IMPERIAL_GALLONS, 42.372f),

			// Nautical Miles per Liter
			TestCase(15000f, MetricsConstants.NAUTICAL_MILES_AND_METERS, VolumeUnit.LITRES, 8.0994f),
			// Nautical Miles per US Gallon
			TestCase(15000f, MetricsConstants.NAUTICAL_MILES_AND_METERS, VolumeUnit.US_GALLONS, 30.659f),
			// Nautical Miles per Imperial Gallon
			TestCase(15000f, MetricsConstants.NAUTICAL_MILES_AND_METERS, VolumeUnit.IMPERIAL_GALLONS, 36.820f)
		)

		testCases.forEach { testCase ->
			val result = VehicleMetricsPlugin.getFormatDistancePerVolume(
				testCase.metersPerLiter,
				testCase.mc,
				testCase.volumeUnit
			)
			assertEquals(
				"Test failed for metersPerLiter=${testCase.metersPerLiter}, mc=${testCase.mc}, volumeUnit=${testCase.volumeUnit}",
				testCase.expected,
				result,
				0.001f
			)
		}
	}
}