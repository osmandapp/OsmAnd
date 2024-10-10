package net.osmand.shared

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE
import net.osmand.shared.obd.OBDDataField
import net.osmand.shared.obd.OBDDataFieldType
import kotlin.test.Test
import kotlin.test.assertTrue

class OBDTest {

	@Test
	fun testOBDComputer() = runBlocking {
		val widget = OBDDataComputer.registerWidget(FUEL_CONSUMPTION_RATE, 15, OBDDataComputer.OBDComputerWidgetFormatter())
		val coef = 0.05
		var fuelLevelStart = 66.0
		val delay = 1000L
		var fuelLevel = fuelLevelStart
		var time: Long = currentTimeMillis()
		for (i in 0 .. 600) {
			val map = HashMap<OBDCommand, OBDDataField<Any>>()
			map[OBDCommand.OBD_FUEL_LEVEL_COMMAND] = OBDDataField(OBDDataFieldType.FUEL_LVL, fuelLevel.toString())
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