package net.osmand.shared

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataField
import kotlin.test.Test
import kotlin.test.assertTrue

class OBDTest {

//	@Test
	fun testOBDComputer() = runBlocking {
		val widget = OBDDataComputer.registerWidget(OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR, 15)
		val coef = 0.05
		var fuelLevelStart = 66.0
		val delay = 1000L
		var fuelLevel = fuelLevelStart
		var time: Long = currentTimeMillis()
		for (i in 0 .. 600) {
			val map = HashMap<OBDCommand, OBDDataField<Any>>()
			map[OBDCommand.OBD_FUEL_LEVEL_COMMAND] = OBDDataField(fuelLevel)
			OBDDataComputer.acceptValue(map)
			val now = currentTimeMillis()
			fuelLevel = fuelLevelStart - coef / (1000 / delay) * i
			println("$fuelLevel - ${(now - time) / 1000.0}")
			time  = now
			println(widget.computeValue())
			delay(delay)
		}

		assertTrue { true }
	}
}