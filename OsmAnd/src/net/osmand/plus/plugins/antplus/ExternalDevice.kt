package net.osmand.plus.plugins.antplus

import net.osmand.plus.plugins.antplus.devices.DeviceType

data class ExternalDevice(
    var name: String,
    var isPaired: Boolean,
    var isConnected: Boolean,
    var address: String,
    var connectionType: DeviceConnectionType,
    var deviceType: DeviceType?,
    var rssi: Int
) : java.io.Serializable {
    var batteryLevel = 0

    fun setIsConnected(isConnected: Boolean) {
        this.isConnected = isConnected;
    }

    enum class DeviceConnectionType {
        BLE, ANT
    }
}
