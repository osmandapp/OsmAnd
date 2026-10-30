package net.osmand.sensors.simulate

import kotlin.math.roundToInt

/**
 * Characteristic values encoded as defined by the Bluetooth GATT specifications.
 */
object SensorPayloads {

    // Heart Rate Measurement (0x2A37): flags 0x06 = uint8 value, sensor contact supported and detected
    fun heartRate(bpm: Int): ByteArray = byteArrayOf(0x06, bpm.coerceIn(0, 255).toByte())

    // Running Speed and Cadence Measurement (0x2A53): flags bit 0 = stride length present, bit 1 = total distance
    // present, bit 2 = running. Speed in 1/256 m/s, cadence in steps/min, stride length in 1/100 m, total distance in 1/10 m.
    fun runningSpeedCadence(speedMs: Double, cadence: Int, strideLengthM: Double?, totalDistanceM: Double?): ByteArray {
        val flags = 0x04 or (if (strideLengthM != null) 0x01 else 0) or (if (totalDistanceM != null) 0x02 else 0)
        return byteArrayOf(flags.toByte()) +
                uint16((speedMs * 256).roundToInt()) +
                byteArrayOf(cadence.coerceIn(0, 255).toByte()) +
                (strideLengthM?.let { uint16((it * 100).roundToInt()) } ?: byteArrayOf()) +
                (totalDistanceM?.let { uint32((it * 10).toLong()) } ?: byteArrayOf())
    }

    // RSC Feature (0x2A54): bit 0 = stride length supported, bit 1 = total distance, bit 2 = walking/running status
    fun runningFeature(strideLength: Boolean, totalDistance: Boolean): ByteArray =
        uint16(0x04 or (if (strideLength) 0x01 else 0) or (if (totalDistance) 0x02 else 0))

    // CSC Measurement (0x2A5B): flags 0x03 = wheel and crank revolution data present.
    // Cumulative wheel revolutions uint32, cumulative crank revolutions uint16, event times in 1/1024 s.
    fun cyclingSpeedCadence(wheelRevolutions: Long, wheelEventTime: Int, crankRevolutions: Int, crankEventTime: Int): ByteArray =
        byteArrayOf(0x03) + uint32(wheelRevolutions) + uint16(wheelEventTime) + uint16(crankRevolutions) + uint16(crankEventTime)

    // CSC Feature (0x2A5C): wheel and crank revolution data supported
    fun cyclingSpeedCadenceFeature(): ByteArray = uint16(0x0003)

    // Cycling Power Measurement (0x2A63): flags 0x0020 = crank revolution data present, instantaneous power sint16 in W
    fun cyclingPower(powerW: Int, crankRevolutions: Int, crankEventTime: Int): ByteArray =
        uint16(0x0020) + uint16(powerW) + uint16(crankRevolutions) + uint16(crankEventTime)

    // Cycling Power Feature (0x2A65): crank revolution data supported
    fun cyclingPowerFeature(): ByteArray = uint32(0x00000008)

    // Sensor Location (0x2A5D): left crank
    fun cyclingPowerSensorLocation(): ByteArray = byteArrayOf(0x05)

    // Blood Pressure Measurement (0x2A35): flags 0x04 = mmHg, pulse rate present; all values are SFLOAT
    fun bloodPressure(systolic: Int, diastolic: Int, meanArterial: Int, pulseRate: Int): ByteArray =
        byteArrayOf(0x04) + sfloat(systolic) + sfloat(diastolic) + sfloat(meanArterial) + sfloat(pulseRate)

    // Intermediate Cuff Pressure (0x2A36): same layout, diastolic and mean arterial fields are NaN
    fun intermediateCuffPressure(cuffPressure: Int, pulseRate: Int): ByteArray =
        byteArrayOf(0x04) + sfloat(cuffPressure) + SFLOAT_NAN + SFLOAT_NAN + sfloat(pulseRate)

    // Blood Pressure Feature (0x2A49): no optional features
    fun bloodPressureFeature(): ByteArray = uint16(0x0000)

    // Temperature Measurement (0x2A1C): flags 0x00 = Celsius, then IEEE-11073 32-bit FLOAT
    fun temperature(celsius: Double): ByteArray = byteArrayOf(0x00) + ieee11073Float(celsius, decimals = 2)

    // Body Sensor Location (0x2A38): chest
    fun bodySensorLocation(): ByteArray = byteArrayOf(0x01)

    fun batteryLevel(percent: Int): ByteArray = byteArrayOf(percent.coerceIn(0, 100).toByte())

    fun ieee11073Float(value: Double, decimals: Int): ByteArray {
        var mantissa = value
        repeat(decimals) { mantissa *= 10 }
        val m = mantissa.roundToInt()
        return byteArrayOf(m.toByte(), (m shr 8).toByte(), (m shr 16).toByte(), (-decimals).toByte())
    }

    private val SFLOAT_NAN = uint16(0x07FF)

    // IEEE-11073 16-bit SFLOAT with zero exponent: a 12-bit signed mantissa
    fun sfloat(value: Int): ByteArray = uint16(value.coerceIn(-2046, 2046) and 0x0FFF)

    fun uint16(value: Int): ByteArray = byteArrayOf(value.toByte(), (value shr 8).toByte())

    fun uint32(value: Long): ByteArray =
        byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte())
}