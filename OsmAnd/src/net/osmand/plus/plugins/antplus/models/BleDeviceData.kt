package net.osmand.plus.plugins.antplus.models

interface BleDeviceData {
    fun getDataFields(): List<CharacteristicDataField>
}