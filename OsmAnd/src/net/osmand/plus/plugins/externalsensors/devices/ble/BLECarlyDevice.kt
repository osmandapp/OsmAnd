package net.osmand.plus.plugins.externalsensors.devices.ble

import android.bluetooth.BluetoothAdapter
import net.osmand.plus.plugins.externalsensors.GattAttributes
import java.util.UUID

class BLECarlyDevice(bluetoothAdapter: BluetoothAdapter, deviceId: String) :
	BLEOBDDevice(bluetoothAdapter, deviceId) {
	companion object {
		val serviceUUID: UUID
			get() = GattAttributes.UUID_CHARACTERISTIC_OBD_CARLY_SCANNER
	}
}