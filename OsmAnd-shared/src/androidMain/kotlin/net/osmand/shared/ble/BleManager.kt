package net.osmand.shared.ble


import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

actual class BleManager {
	private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
	private var scanCallback: ScanCallback? = null

	actual fun startScan(serviceUUID: String, onDeviceFound: (BleDevice) -> Unit) {
		scanCallback = object : ScanCallback() {
			override fun onScanResult(callbackType: Int, result: ScanResult?) {
				result?.device?.let { device ->
					val bleDevice =
						BleDevice(name = device.name ?: "Unknown", address = device.address)
					onDeviceFound(bleDevice)
				}
			}

			override fun onBatchScanResults(results: MutableList<ScanResult>?) {
				results?.forEach { result ->
					result.device?.let { device ->
						val bleDevice =
							BleDevice(name = device.name ?: "Unknown", address = device.address)
						onDeviceFound(bleDevice)
					}
				}
			}

			override fun onScanFailed(errorCode: Int) {
			}
		}
		scanner?.startScan(scanCallback)
	}

	actual fun stopScan() {
		scanCallback?.let { scanner?.stopScan(it) }
	}

	actual fun connectToDevice(
		device: BleDevice,
		onConnected: () -> Unit,
		onDisconnected: () -> Unit) {
	}

	actual fun sendCommand(command: String, onResponse: (String) -> Unit) {
	}
}