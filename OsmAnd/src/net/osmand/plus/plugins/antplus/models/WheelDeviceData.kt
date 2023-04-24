package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R
import java.text.DecimalFormat

data class WheelDeviceData(
    var speed: Float,
    var distance: Float,
    var totalDistance: Float
) : BleDeviceData {
    companion object {
        val decimalFormat = DecimalFormat("#.##")
    }

    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_speed,
                "${decimalFormat.format(speed)} ", R.string.km_h
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_distance,
                "${decimalFormat.format(distance)} ",
                R.string.m
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_total_distance,
                "${decimalFormat.format(totalDistance)} ",
                R.string.m
            )
        )
        return fields
    }
}
