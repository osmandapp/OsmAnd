package net.osmand.shared.ble

actual class BleManager actual constructor() {
	actual fun startScan(
		serviceUUID: String,
		onDeviceFound: (BleDevice) -> Unit) {
	}

	actual fun stopScan() {
	}

	actual fun connectToDevice(
		device: BleDevice,
		onConnected: () -> Unit,
		onDisconnected: () -> Unit) {
	}

	actual fun sendCommand(command: String, onResponse: (String) -> Unit) {
	}

}