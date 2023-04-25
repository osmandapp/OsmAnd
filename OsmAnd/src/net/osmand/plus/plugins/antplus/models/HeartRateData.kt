package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R

data class HeartRateData(
    var heartRate: Int
) : BleDeviceData {
    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_heart_rate,
                "$heartRate",
                -1
            )
        )
        return fields
    }
}