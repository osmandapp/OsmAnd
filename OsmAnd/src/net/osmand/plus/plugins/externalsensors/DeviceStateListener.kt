package net.osmand.plus.plugins.externalsensors

interface DeviceStateListener {
	fun onDeviceConnected(deviceId: String)
	fun onDeviceDisconnected(deviceId: String)
}