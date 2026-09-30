package net.osmand.plus.plugins.externalsensors

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.externalsensors.devices.ble.BLETpmsDevice
import net.osmand.plus.utils.AndroidUtils

/**
 * Listens to paired tire pressure sensors without a foreground service of its own.
 *
 * Battery:
 * - runs only while the map is on screen or NavigationService is already running (navigation / trip recording);
 * - hardware filter by the paired sensor addresses, so the radio controller drops every other advertisement;
 * - SCAN_MODE_LOW_POWER (~10% radio duty) and, where the chip supports it, 10 s batching in the controller,
 *   so the CPU wakes up a few times a minute at most;
 * - the scan is restarted only when the set of sensors changes (Android allows 5 starts per 30 s).
 */
class TpmsScanner(
	private val app: OsmandApplication,
	private val devicesProvider: () -> List<BLETpmsDevice>
) {

	private var adapter: BluetoothAdapter? = null
	private var mapVisible = false
	private var scannedAddresses: Set<String> = emptySet()
	private val checkRunnable = Runnable { update() }

	private val callback = object : ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult) {
			onResult(result)
		}

		override fun onBatchScanResults(results: MutableList<ScanResult>) {
			results.forEach { onResult(it) }
		}

		override fun onScanFailed(errorCode: Int) {
			LOG.error("TPMS scan failed $errorCode")
			scannedAddresses = emptySet()
		}
	}

	fun setAdapter(adapter: BluetoothAdapter?) {
		if (adapter == null) {
			stopScan()
		}
		this.adapter = adapter
		update()
	}

	fun setMapVisible(visible: Boolean) {
		mapVisible = visible
		update()
	}

	fun update() {
		app.uiHandler.removeCallbacks(checkRunnable)
		if (adapter?.isEnabled != true) {
			// the system drops scans when Bluetooth is turned off
			scannedAddresses = emptySet()
		}
		val listen = mapVisible || app.navigationService != null
		val addresses = if (listen) {
			devicesProvider().filter { it.isConnected }.map { it.deviceId }.toSet()
		} else {
			emptySet()
		}
		if (addresses != scannedAddresses) {
			stopScan()
			if (addresses.isNotEmpty()) {
				startScan(addresses)
			}
		}
		if (scannedAddresses.isNotEmpty()) {
			// stop within a minute after navigation ends while the map is in background
			app.uiHandler.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
		}
	}

	private fun onResult(result: ScanResult) {
		val record = result.scanRecord ?: return
		val address = result.device.address
		devicesProvider().find { it.deviceId == address }?.onAdvertisement(record, result.rssi)
	}

	@SuppressLint("MissingPermission")
	private fun startScan(addresses: Set<String>) {
		val adapter = adapter ?: return
		val scanner = adapter.bluetoothLeScanner ?: return
		if (!adapter.isEnabled || !AndroidUtils.hasBLEPermission(app)) {
			return
		}
		val filters = addresses.map { ScanFilter.Builder().setDeviceAddress(it).build() }
		val settings = ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
			.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
			.setReportDelay(if (adapter.isOffloadedScanBatchingSupported) REPORT_DELAY_MS else 0)
			.build()
		try {
			scanner.startScan(filters, settings, callback)
			scannedAddresses = addresses
			LOG.debug("TPMS scan started for $addresses")
		} catch (e: RuntimeException) {
			LOG.error("TPMS scan start error", e)
		}
	}

	@SuppressLint("MissingPermission")
	private fun stopScan() {
		if (scannedAddresses.isEmpty()) {
			return
		}
		scannedAddresses = emptySet()
		val adapter = adapter ?: return
		try {
			if (adapter.isEnabled && AndroidUtils.hasBLEPermission(app)) {
				adapter.bluetoothLeScanner?.stopScan(callback)
			}
			LOG.debug("TPMS scan stopped")
		} catch (e: RuntimeException) {
			LOG.error("TPMS scan stop error", e)
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(TpmsScanner::class.java)
		private const val REPORT_DELAY_MS = 10_000L
		private const val CHECK_INTERVAL_MS = 60_000L
	}
}
