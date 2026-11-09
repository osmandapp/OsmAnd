package net.osmand.shared.obd

import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDLocation
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.shared.util.PlatformUtil
import java.lang.reflect.Proxy
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OBDFuelConsumptionTest {

	companion object {
		private const val TANK_LITERS = 62f
		private const val START_LEVEL = 80.0
		private const val SPEED_MPS = 60 / 3.6
		private const val METERS_PER_DEGREE = 111_195.0
	}

	private class Sample(val time: Long, val value: Any?)

	@BeforeTest
	fun setUp() {
		val settings = proxy(SettingsAPI::class.java) { name ->
			if (name == "getFloatPreference") TANK_LITERS else null
		}
		val context = proxy(OsmAndContext::class.java) { name ->
			if (name == "getSettings") settings else null
		}
		PlatformUtil.initialize(context)
		OBDDataComputer.locations = emptyList()
	}

	@Test
	fun steadyConsumptionPerHour() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val samples = feed(widget, start(), 30 * 60) { sec -> level(6.0, sec) }

		assertNoNegative(samples)
		assertClose(6.0, lastNumber(samples), 0.05)
	}

	@Test
	fun steadyConsumptionPercentPerHour() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR)
		val samples = feed(widget, start(), 30 * 60) { sec -> level(6.0, sec) }

		assertClose(6.0 / TANK_LITERS * 100, lastNumber(samples), 0.05)
	}

	@Test
	fun steadyConsumptionPerDistance() {
		val start = start()
		val seconds = 30 * 60
		OBDDataComputer.locations = drivenLocations(start, seconds)
		val perDistance = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM)
		val distancePerVolume = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_M_PER_LITER)
		val samples = feed(listOf(perDistance, distancePerVolume), start, seconds) { sec -> level(6.0, sec) }

		// 6 L/h at 60 km/h
		assertNoNegative(samples)
		assertClose(10.0, (perDistance.computeValue() as Number).toDouble(), 0.05)
		assertClose(10_000.0, (distancePerVolume.computeValue() as Number).toDouble(), 0.05)
	}

	@Test
	fun levelWobblingAroundStepWhileStoppedIsNotNegative() {
		// a track where the level read 80 -> 78.8 -> 80 -> 78.8 % within ten minutes, the car standing
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val samples = feed(widget, start(), 30 * 60) { sec ->
			when {
				sec < 9 * 60 -> 80.0
				sec < 15 * 60 -> 78.82353
				sec < 22 * 60 -> 80.0
				else -> 78.82353
			}
		}

		assertNoNegative(samples)
	}

	@Test
	fun shortSpikesDoNotChangeConsumption() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val samples = feed(widget, start(), 30 * 60) { sec ->
			val level = level(6.0, sec)
			when (sec % 7) {
				0 -> level + 3 * step()
				3 -> level - 3 * step()
				else -> level
			}
		}

		assertNoNegative(samples)
		assertClose(6.0, lastNumber(samples), 0.05)
	}

	@Test
	fun slowSwingAroundStepIsNotNegative() {
		// sloshing: the reading jumps one step up for 20 s every minute
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val samples = feed(widget, start(), 30 * 60) { sec ->
			val level = level(6.0, sec)
			if (sec % 60 < 20) level + step() else level
		}

		assertNoNegative(samples)
		assertClose(6.0, lastNumber(samples), 0.2)
	}

	@Test
	fun refuelRestartsHistoryAndKeepsLastValue() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		var time = start()
		val before = feed(widget, time, 20 * 60) { sec -> level(6.0, sec) }
		val lastBefore = lastNumber(before)
		time += 20 * 60 * 1000L
		val refueledLevel = level(6.0, 20 * 60) + 30
		val after = feed(widget, time, 30 * 60) { sec -> quantize(refueledLevel - 12.0 / TANK_LITERS * 100 * sec / 3600) }

		assertNoNegative(after)
		assertTrue(after.all { it.value is Number }, "the value before the refuel stays until a new one")
		assertEquals(lastBefore, (after.first().value as Number).toDouble())
		assertClose(12.0, lastNumber(after), 0.05)
	}

	@Test
	fun gapRestartsHistoryAndKeepsLastValue() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		var time = start()
		val before = feed(widget, time, 20 * 60) { sec -> level(6.0, sec) }
		val lastBefore = lastNumber(before)
		val stopLevel = level(6.0, 20 * 60)
		// ten minutes without readings, the engine off
		time += 30 * 60 * 1000L
		val after = feed(widget, time, 20 * 60) { sec -> quantize(stopLevel - 12.0 / TANK_LITERS * 100 * sec / 3600) }

		assertTrue(after.all { it.value is Number })
		assertEquals(lastBefore, (after.first().value as Number).toDouble())
		val firstNew = after.first { (it.value as Number).toDouble() != lastBefore }
		// a rate across the stop would be about 12 * 20 / 30 = 8 L/h
		assertClose(12.0, (firstNew.value as Number).toDouble(), 0.1)
	}

	@Test
	fun shortNoDataKeepsLastValue() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val seconds = 20 * 60
		val samples = feed(widget, currentTimeMillis() - seconds * 1000L - 10_000, seconds) { sec -> level(6.0, sec) }
		val last = lastNumber(samples)
		repeat(10) {
			widget.acceptValue(mapOf(OBDCommand.OBD_FUEL_LEVEL_COMMAND to OBDDataField.NO_DATA))
		}

		assertEquals(last, (widget.computeValue() as Number).toDouble())
	}

	@Test
	fun longNoDataDropsLastValue() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val seconds = 20 * 60
		feed(widget, currentTimeMillis() - seconds * 1000L - 90_000, seconds) { sec -> level(6.0, sec) }
		widget.acceptValue(mapOf(OBDCommand.OBD_FUEL_LEVEL_COMMAND to OBDDataField.NO_DATA))

		assertEquals("N/A", widget.computeValue())
	}

	@Test
	fun repeatedCachedReadingIsNotCounted() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		val seconds = 20 * 60
		val start = currentTimeMillis() - seconds * 1000L - 10_000
		val samples = feed(widget, start, seconds) { sec -> level(6.0, sec) }
		val last = lastNumber(samples)
		// a request fails: the dispatcher passes its last cached reading again and again
		val cached = mapOf(OBDCommand.OBD_FUEL_LEVEL_COMMAND to reading(level(6.0, seconds) - step(), start + seconds * 1000L))
		repeat(100) {
			widget.acceptValue(cached)
		}

		assertEquals(last, (widget.computeValue() as Number).toDouble())
	}

	@Test
	fun unsupportedFuelLevelIsNotAvailable() {
		val widget = widget(OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
		widget.acceptValue(mapOf(OBDCommand.OBD_FUEL_LEVEL_COMMAND to OBDDataField.NO_DATA))

		assertEquals("N/A", widget.computeValue())
	}

	private fun widget(type: OBDTypeWidget) = OBDComputerWidget(type, type.defaultAverageTime)

	private fun start() = currentTimeMillis() - 24 * 3600 * 1000L

	private fun step() = 100.0 / 255

	private fun quantize(level: Double) = (level / step()).roundToInt() * step()

	private fun level(litersPerHour: Double, sec: Int) =
		quantize(START_LEVEL - litersPerHour / TANK_LITERS * 100 * sec / 3600)

	private fun reading(level: Double, time: Long) = OBDDataField<Any>(level.toFloat()).also { it.timestamp = time }

	private fun feed(widget: OBDComputerWidget, start: Long, seconds: Int, level: (Int) -> Double) =
		feed(listOf(widget), start, seconds, level)

	// one reading a second; the samples are the values of the first widget
	private fun feed(widgets: List<OBDComputerWidget>, start: Long, seconds: Int, level: (Int) -> Double): List<Sample> {
		val samples = ArrayList<Sample>()
		for (sec in 0 until seconds) {
			val time = start + sec * 1000L
			val data = mapOf(OBDCommand.OBD_FUEL_LEVEL_COMMAND to reading(level(sec), time))
			widgets.forEach { it.acceptValue(data) }
			samples.add(Sample(time, widgets.first().computeValue()))
		}
		return samples
	}

	private fun drivenLocations(start: Long, seconds: Int) = (0..seconds).map { sec ->
		OBDLocation(start + sec * 1000L, KLatLon(sec * SPEED_MPS / METERS_PER_DEGREE, 0.0))
	}

	private fun lastNumber(samples: List<Sample>): Double {
		val last = samples.lastOrNull { it.value is Number && !(it.value.toDouble().isNaN()) }
		assertNotNull(last, "no value computed")
		return (last.value as Number).toDouble()
	}

	private fun assertNoNegative(samples: List<Sample>) {
		val negative = samples.firstOrNull { (it.value as? Number)?.toDouble()?.let { v -> v < 0 } == true }
		assertTrue(negative == null, "negative value ${negative?.value} at ${negative?.time}")
	}

	private fun assertClose(expected: Double, actual: Double, tolerance: Double) {
		assertTrue(abs(actual - expected) <= expected * tolerance, "expected $expected ±${tolerance * 100}%, got $actual")
	}

	@Suppress("UNCHECKED_CAST")
	private fun <T> proxy(type: Class<T>, answer: (String) -> Any?): T =
		Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ -> answer(method.name) } as T
}