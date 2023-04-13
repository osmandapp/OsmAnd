package net.osmand.plus.plugins.antplus

import net.osmand.plus.plugins.antplus.models.BleDeviceData

interface BleDataListener {
    fun onDataReceived(address: String?, data: BleDeviceData)
}