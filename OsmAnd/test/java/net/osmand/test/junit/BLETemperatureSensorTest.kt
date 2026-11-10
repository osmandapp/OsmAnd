package net.osmand.test.junit

import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLETemperatureSensor.parseIeee11073Float
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BLETemperatureSensorTest {

	@Test
	fun keepsPrecisionSentBySensor() {
		assertEquals("36.61", parse(3661, -2).toString())
		assertEquals("36.6", parse(366, -1).toString())
		assertEquals("36.60", parse(3660, -2).toString())
		assertEquals("37", parse(37, 0).toString())
	}

	@Test
	fun convertsToNearestDouble() {
		assertEquals(36.61, parse(3661, -2)!!.toDouble(), 0.0)
		assertEquals(36.6, parse(366, -1)!!.toDouble(), 0.0)
	}

	@Test
	fun readsSignedMantissa() {
		assertEquals("-12.5", parse(-125, -1).toString())
		assertEquals("-0.01", parse(-1, -2).toString())
	}

	@Test
	fun writesPositiveExponentWithoutScientificNotation() {
		assertEquals("400", parse(4, 2).toString())
	}

	@Test
	fun skipsSpecialValues() {
		assertNull(parse(0x7FFFFF, 0))
		assertNull(parse(0x800000, 0))
		assertNull(parse(0x7FFFFE, 0))
		assertNull(parse(0x800001, 0))
		assertNull(parse(0x800002, 0))
	}

	@Test
	fun skipsShortValue() {
		assertNull(parseIeee11073Float(byteArrayOf(0x00, 0x4D, 0x0E, 0x00), 1))
		assertNull(parseIeee11073Float(null, 1))
	}

	// Temperature Measurement: a flags byte, then the FLOAT as a little-endian 24-bit mantissa and an 8-bit exponent
	private fun parse(mantissa: Int, exponent: Int) = parseIeee11073Float(
		byteArrayOf(0x00, mantissa.toByte(), (mantissa shr 8).toByte(), (mantissa shr 16).toByte(), exponent.toByte()), 1)
}