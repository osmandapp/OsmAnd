package net.osmand.shared.ble

expect class BleManager() {
	fun startScan(serviceUUID: String, onDeviceFound: (BleDevice) -> Unit)

	fun stopScan()

	fun connectToDevice(device: BleDevice, onConnected: () -> Unit, onDisconnected: () -> Unit)

	fun sendCommand(command: String, onResponse: (String) -> Unit)
}