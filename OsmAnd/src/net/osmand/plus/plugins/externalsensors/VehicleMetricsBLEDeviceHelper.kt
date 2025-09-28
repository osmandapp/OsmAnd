package net.osmand.plus.plugins.externalsensors

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEOBDDevice
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEOBDDevice.DeviceReadyListener
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.settings.backend.preferences.CommonPreferenceProvider

class VehicleMetricsBLEDeviceHelper(
	private val vehicleMetricsPlugin: VehicleMetricsPlugin,
	app: OsmandApplication,
	preferenceProvider: CommonPreferenceProvider<String>) : DevicesHelper(app, preferenceProvider) {

	override fun addFoundBLEDevice(device: BLEAbstractDevice) {
		val isOBDDevice = device is BLEOBDDevice
		if (isOBDDevice) {
			devices[device.deviceId] = device
			vehicleMetricsPlugin.onBLEDeviceFound(device as BLEOBDDevice)
		}
	}

	override fun onDeviceConnectSucceed(device: AbstractDevice<*>) {
		super.onDeviceConnectSucceed(device)
		if (device is BLEOBDDevice) {
			device.setDeviceReadyListener(object : DeviceReadyListener {
				override fun onDeviceReadyStateChange(isReady: Boolean) {
					if (isReady) {
						val vehicleMetricsPlugin = PluginsHelper.getActivePlugin(
							VehicleMetricsPlugin::class.java)
						vehicleMetricsPlugin?.connectToDevice(device)
					}
				}
			})
		}
	}

	override fun createBLEDevice(
		result: ScanResult,
		uuid: ParcelUuid,
		address: String,
		deviceName: String): BLEAbstractDevice {
		val uuidString = uuid.uuid.toString()
		val device = BLEOBDDevice(bluetoothAdapter, address, uuidString)
		device.setDeviceName(deviceName)
		device.rssi = result.rssi

		var settings = devicesSettingsCollection.getDeviceSettings(address)
		if (settings == null) {
			settings = DevicesSettingsCollection.createDeviceSettings(device.deviceId, device, true)
		}
		settings.setUuid(uuidString)
		devicesSettingsCollection.setDeviceSettings(address, settings)
		return device
	}
}