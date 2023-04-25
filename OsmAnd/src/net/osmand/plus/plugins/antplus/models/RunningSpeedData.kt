package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R

data class RunningSpeedData(
    var speed: Float,
    var cadence: Int,
    var totalDistance: Float,
    var strideLength: Float
) : BleDeviceData {
    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_speed,
                "$speed",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_cadence,
                "$cadence",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_total_distance,
                "$totalDistance",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_stride_length,
                "$strideLength",
                -1
            )
        )
        return fields
    }
}
