package net.osmand.plus.plugins.antplus.models

import net.osmand.plus.R

data class BatteryData(
    var batteryLevel: Int
) : BleDeviceData {
    override fun getDataFields(): ArrayList<CharacteristicDataField> {
        val fields = ArrayList<CharacteristicDataField>()
        fields.add(
            CharacteristicDataField(
                R.string.external_device_characteristic_battery_level,
                "$batteryLevel",
                -1
            )
        )
        return fields
    }
}
