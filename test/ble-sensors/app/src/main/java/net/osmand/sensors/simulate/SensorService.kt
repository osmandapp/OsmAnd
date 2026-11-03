package net.osmand.sensors.simulate

import java.util.UUID

fun uuid16(value: Int): UUID = UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", value))

object GattUuids {
    val HEART_RATE_SERVICE: UUID = uuid16(0x180D)
    val HEART_RATE_MEASUREMENT: UUID = uuid16(0x2A37)
    val BODY_SENSOR_LOCATION: UUID = uuid16(0x2A38)

    val RUNNING_SPEED_CADENCE_SERVICE: UUID = uuid16(0x1814)
    val RSC_MEASUREMENT: UUID = uuid16(0x2A53)
    val RSC_FEATURE: UUID = uuid16(0x2A54)

    val CYCLING_SPEED_CADENCE_SERVICE: UUID = uuid16(0x1816)
    val CSC_MEASUREMENT: UUID = uuid16(0x2A5B)
    val CSC_FEATURE: UUID = uuid16(0x2A5C)

    val CYCLING_POWER_SERVICE: UUID = uuid16(0x1818)
    val CYCLING_POWER_MEASUREMENT: UUID = uuid16(0x2A63)
    val CYCLING_POWER_FEATURE: UUID = uuid16(0x2A65)
    val SENSOR_LOCATION: UUID = uuid16(0x2A5D)

    val HEALTH_THERMOMETER_SERVICE: UUID = uuid16(0x1809)
    val TEMPERATURE_MEASUREMENT: UUID = uuid16(0x2A1C)

    val BLOOD_PRESSURE_SERVICE: UUID = uuid16(0x1810)
    val BLOOD_PRESSURE_MEASUREMENT: UUID = uuid16(0x2A35)
    val INTERMEDIATE_CUFF_PRESSURE: UUID = uuid16(0x2A36)
    val BLOOD_PRESSURE_FEATURE: UUID = uuid16(0x2A49)

    val BATTERY_SERVICE: UUID = uuid16(0x180F)
    val BATTERY_LEVEL: UUID = uuid16(0x2A19)

    val CLIENT_CHARACTERISTIC_CONFIG: UUID = uuid16(0x2902)
}

/**
 * [sensorType] names the simulator in Bluetooth when it is the only selected sensor, [code] in combinations.
 */
enum class SensorService(
    val uuid: UUID,
    val title: String,
    val sensorType: String,
    val code: String
) {
    HEART_RATE(GattUuids.HEART_RATE_SERVICE, "Heart Rate (0x180D)", "Heart rate", "HR"),
    RUNNING_SPEED_CADENCE(GattUuids.RUNNING_SPEED_CADENCE_SERVICE, "Running Speed and Cadence (0x1814)", "Running", "RSC"),
    CYCLING_SPEED_CADENCE(GattUuids.CYCLING_SPEED_CADENCE_SERVICE, "Cycling Speed and Cadence (0x1816)", "Bike speed/cadence", "CSC"),
    CYCLING_POWER(GattUuids.CYCLING_POWER_SERVICE, "Cycling Power (0x1818)", "Bike power", "CP"),
    HEALTH_THERMOMETER(GattUuids.HEALTH_THERMOMETER_SERVICE, "Health Thermometer (0x1809)", "Thermometer", "HT"),
    BLOOD_PRESSURE(GattUuids.BLOOD_PRESSURE_SERVICE, "Blood Pressure (0x1810)", "Blood pressure", "BP"),
    BATTERY(GattUuids.BATTERY_SERVICE, "Battery (0x180F)", "Battery", "BAT");

    companion object {
        val sensors: List<SensorService> = entries - BATTERY

        // Android cuts the advertised name to 26 bytes
        const val MAX_ADVERTISED_NAME_BYTES = 26

        fun bluetoothName(services: List<SensorService>): String {
            val label = services.singleOrNull()?.sensorType ?: services.joinToString("+") { it.code }
            val name = "$label simulator"
            return if (name.toByteArray().size <= MAX_ADVERTISED_NAME_BYTES) name else "$label sim"
        }
    }

    val options: List<SensorOption> get() = SensorOption.entries.filter { it.service == this }
}

enum class SensorOption(val service: SensorService, val title: String, val description: String, val enabledByDefault: Boolean) {
    RUNNING_STRIDE_LENGTH(
        SensorService.RUNNING_SPEED_CADENCE, "Send stride length", "Optional field, flags bit 0", true
    ),
    RUNNING_TOTAL_DISTANCE(
        SensorService.RUNNING_SPEED_CADENCE, "Send total distance", "Optional field, flags bit 1", true
    ),
    TEMPERATURE_INDICATION(
        SensorService.HEALTH_THERMOMETER, "Send via indication", "As the specification defines, off sends notifications", false
    ),
    BLOOD_PRESSURE_INDICATION(
        SensorService.BLOOD_PRESSURE, "Send measurement via indication",
        "As the specification defines, off sends notifications; cuff pressure is always notified", false
    );

    companion object {
        val defaults: Set<SensorOption> = entries.filter { it.enabledByDefault }.toSet()
    }
}

/**
 * Selections of real devices, services are in their advertisement order.
 */
enum class SensorPreset(val title: String, val description: String, val services: List<SensorService>) {
    HRM_600(
        "Garmin HRM 600",
        "Running Speed and Cadence before Heart Rate (issue #25694)",
        listOf(SensorService.RUNNING_SPEED_CADENCE, SensorService.HEART_RATE)
    ),
    MOMENTUM_SPORT(
        "Sennheiser Momentum Sport",
        "Health Thermometer before Heart Rate",
        listOf(SensorService.HEALTH_THERMOMETER, SensorService.HEART_RATE)
    );
}

enum class AdvertiseMode(val title: String) {
    ALL("All services"),
    FIRST("First service only"),
    NONE("No service UUIDs");

    // with a single sensor the first service is all of them
    fun isApplicable(services: List<SensorService>): Boolean = this != FIRST || services.size > 1

    fun advertisedServices(services: List<SensorService>): List<SensorService> = when (this) {
        ALL -> services
        FIRST -> services.take(1)
        NONE -> emptyList()
    }
}