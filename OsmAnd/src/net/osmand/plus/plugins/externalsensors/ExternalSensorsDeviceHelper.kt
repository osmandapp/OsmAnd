package net.osmand.plus.plugins.externalsensors

import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEOBDDevice
import net.osmand.plus.settings.backend.preferences.CommonPreferenceProvider

class ExternalSensorsDeviceHelper(
	private val externalSensorsPlugin: ExternalSensorsPlugin,
	app: OsmandApplication,
	preferenceProvider: CommonPreferenceProvider<String>) : DevicesHelper(app, preferenceProvider) {

	override fun addFoundBLEDevice(device: BLEAbstractDevice) {
		val isOBDDevice = device is BLEOBDDevice
		if (!isOBDDevice) {
			devices[device.deviceId] = device
		}
	}

	override fun onDevicePaired(device: AbstractDevice<*>) {
		super.onDevicePaired(device)
		externalSensorsPlugin.onDevicePaired(device)
	}
}