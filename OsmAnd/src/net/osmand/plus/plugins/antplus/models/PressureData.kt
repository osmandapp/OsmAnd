package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R

data class PressureData(
    var systolic: Float,
    var diastolic: Float,
    var arterialPressure: Float,
    var cuffPressure: Float,
    var unit: Int,
    var timestamp: String,
    var pulseRate: Float
) : BleDeviceData {
    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_systolic,
                "$systolic",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_diastolic,
                "$diastolic",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_arterial_pressure,
                "$arterialPressure",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_cuff_pressure,
                "$cuffPressure",
                -1
            )
        )
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_pulse_rate,
                "$pulseRate",
                -1
            )
        )
        return fields
    }
}