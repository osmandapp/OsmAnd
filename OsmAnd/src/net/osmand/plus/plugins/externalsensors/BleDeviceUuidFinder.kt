package net.osmand.plus.plugins.externalsensors

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import net.osmand.PlatformUtil
import net.osmand.util.Algorithms
import org.apache.commons.logging.Log

class BleDeviceUuidFinder(val targetDeviceId: String, val callback: DeviceFoundCallback) {
	interface DeviceFoundCallback {
		fun onDeviceFound(uuid: String?)
	}

	private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	private var scanThread: HandlerThread? = null
	private var scanHandler: Handler? = null
	private var bleScanner: BluetoothLeScanner? = null

	private val scanCallback: ScanCallback = object : ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult) {
			val deviceAddress = result.device.address
			if (Algorithms.stringsEqual(deviceAddress, targetDeviceId)) {
				log.debug("Found target device: $deviceAddress")
				val uuids = result.scanRecord?.serviceUuids
				val uuid = if (uuids?.isEmpty() == false) {
					uuids[0].toString()
				} else {
					null
				}
				notifyCallback(uuid)
			}
		}

		override fun onScanFailed(errorCode: Int) {
			log.error("BLE Scan failed with code: $errorCode")
			stopScanning()
		}
	}

	private fun notifyCallback(uuid: String?) {
		Handler(Looper.getMainLooper()).post {
			callback.onDeviceFound(
				uuid)
			stopScanning(false)
		}
	}

	private val stopScanRunnable = Runnable { this.stopScanning() }

	init {
		if (this.bluetoothAdapter == null) {
			log.error("Bluetooth not supported on this device.")
		} else {
			this.bleScanner = bluetoothAdapter.bluetoothLeScanner
		}
	}

	fun startScanning() {
		if (bluetoothAdapter == null || bleScanner == null || !bluetoothAdapter.isEnabled) {
			log.error("Bluetooth is not enabled or supported.")
			return
		}

		if (scanThread != null) {
			return
		}

		scanThread = HandlerThread("BleScanWorker")
		scanThread?.let { thread ->
			thread.start()
			scanHandler = Handler(thread.looper)

			scanHandler?.let { handler ->
				handler.post {
					log.debug("Starting BLE scan for $targetDeviceId device.")
					try {
						bleScanner?.let { scanner ->
							scanner.startScan(scanCallback)
							handler.postDelayed(
								stopScanRunnable,
								SCAN_TIMEOUT_MS)
						}
					} catch (e: SecurityException) {
						log.error("Missing BLUETOOTH_SCAN permission.", e)
					} catch (e: Exception) {
						log.error("Error starting scan.", e)
					}
				}
			}
		}
	}

	fun stopScanning() {
		stopScanning(true)
	}

	private fun stopScanning(notifyStop: Boolean) {
		scanHandler?.let { handler ->
			handler.post {
				bleScanner?.let { scanner ->
					try {
						handler.removeCallbacks(stopScanRunnable)
						scanner.stopScan(scanCallback)
						log.debug("BLE scan stopped.")
					} catch (e: SecurityException) {
						log.error("Missing BLUETOOTH_SCAN permission.", e)
					} catch (e: Exception) {
						log.error("Error stopping scan.", e)
					} finally {
						scanThread?.let { thread ->
							thread.quitSafely()
							scanThread = null
							scanHandler = null
						}
						if (notifyStop) {
							notifyCallback(null)
						}
					}
				}
			}
		}
	}

	companion object {
		private val log: Log = PlatformUtil.getLog(
			BleDeviceUuidFinder::class.java)
		private const val SCAN_TIMEOUT_MS: Long = 30000
	}
}