package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R

data class CadenceData(
    var gearRatio: Float,
    var cadence: Float
) : BleDeviceData {
    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_gear_ratio,
                "$gearRatio",
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
        return fields
    }

}
