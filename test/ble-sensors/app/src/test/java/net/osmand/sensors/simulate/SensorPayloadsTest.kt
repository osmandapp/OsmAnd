package net.osmand.sensors.simulate

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorPayloadsTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun heartRateIsUint8WithContactFlags() {
        assertArrayEquals(bytes(0x06, 142), SensorPayloads.heartRate(142))
    }

    @Test
    fun runningSpeedCadenceUsesSpecificationUnits() {
        // 3.5 m/s = 896/256, 172 steps/min, stride 1.22 m = 122 cm, distance 1234.5 m = 12345 dm
        val payload = SensorPayloads.runningSpeedCadence(3.5, 172, 1.22, 1234.5)
        assertArrayEquals(
            bytes(0x07, 0x80, 0x03, 172, 122, 0x00, 0x39, 0x30, 0x00, 0x00),
            payload
        )
    }

    @Test
    fun runningOptionalFieldsAreOmitted() {
        assertArrayEquals(bytes(0x04, 0x80, 0x03, 172), SensorPayloads.runningSpeedCadence(3.5, 172, null, null))
        assertArrayEquals(
            bytes(0x06, 0x80, 0x03, 172, 0x39, 0x30, 0x00, 0x00),
            SensorPayloads.runningSpeedCadence(3.5, 172, null, 1234.5)
        )
        assertArrayEquals(bytes(0x05, 0x00), SensorPayloads.runningFeature(strideLength = true, totalDistance = false))
    }

    @Test
    fun temperatureIsIeee11073Float() {
        // 36.65 = 3665 * 10^-2
        assertArrayEquals(bytes(0x00, 0x51, 0x0E, 0x00, 0xFE), SensorPayloads.temperature(36.65))
    }

    @Test
    fun cyclingSpeedCadenceHasWheelAndCrankData() {
        assertArrayEquals(
            bytes(0x03, 0x10, 0x27, 0x00, 0x00, 0x00, 0x04, 0x2C, 0x01, 0x00, 0x08),
            SensorPayloads.cyclingSpeedCadence(10000, 1024, 300, 2048)
        )
    }

    @Test
    fun cyclingPowerHasCrankData() {
        assertArrayEquals(
            bytes(0x20, 0x00, 0xFA, 0x00, 0x2C, 0x01, 0x00, 0x08),
            SensorPayloads.cyclingPower(250, 300, 2048)
        )
    }

    @Test
    fun bloodPressureIsSfloatWithPulse() {
        // 120 = 0x078, 80 = 0x050, 93 = 0x05D, 72 = 0x048, exponent 0
        assertArrayEquals(
            bytes(0x04, 0x78, 0x00, 0x50, 0x00, 0x5D, 0x00, 0x48, 0x00),
            SensorPayloads.bloodPressure(120, 80, 93, 72)
        )
        assertArrayEquals(
            bytes(0x04, 0xA0, 0x00, 0xFF, 0x07, 0xFF, 0x07, 0x48, 0x00),
            SensorPayloads.intermediateCuffPressure(160, 72)
        )
    }

    @Test
    fun bluetoothNameFitsScanResponse() {
        val names = SensorService.sensors.map { SensorService.bluetoothName(listOf(it)) } +
                SensorService.bluetoothName(SensorService.sensors)
        names.forEach { assertTrue(it, it.toByteArray().size <= SensorService.MAX_ADVERTISED_NAME_BYTES) }
        assertEquals("Bike speed/cadence sim", SensorService.bluetoothName(listOf(SensorService.CYCLING_SPEED_CADENCE)))
        assertEquals("HR+RSC+CSC+CP+HT+BP sim", SensorService.bluetoothName(SensorService.sensors))
        assertEquals("Heart rate simulator", SensorService.bluetoothName(listOf(SensorService.HEART_RATE)))
        assertEquals("RSC+HR simulator", SensorService.bluetoothName(SensorPreset.HRM_600.services))
    }

    @Test
    fun shortNameOfStandardUuid() {
        assertEquals("0x2A37", GattUuids.HEART_RATE_MEASUREMENT.shortName())
    }
}