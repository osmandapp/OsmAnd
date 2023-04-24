package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R

data class TemperatureData(
    var temperature: Float
) : BleDeviceData {
    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_temperature,
                "$temperature",
                -1
            )
        )
        return fields
    }
}