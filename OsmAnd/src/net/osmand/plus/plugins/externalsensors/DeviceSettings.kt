package net.osmand.plus.plugins.externalsensors

import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties
import net.osmand.util.Algorithms

open class DeviceSettings(
	deviceId: String, deviceType: DeviceType,
	deviceName: String, deviceEnabled: Boolean) {
	var deviceId: String
	var deviceType: DeviceType
	private var deviceName: String
	var deviceEnabled: Boolean
	var additionalParams =
		mutableMapOf(DeviceChangeableProperties.NAME to deviceName)

	init {
		require(!Algorithms.isEmpty(deviceId)) { "Device ID is empty" }
		this.deviceId = deviceId
		this.deviceType = deviceType
		this.deviceName = deviceName
		this.deviceEnabled = deviceEnabled
	}

	constructor(settings: DeviceSettings) : this(
		settings.deviceId, settings.deviceType, settings.deviceName,
		settings.deviceEnabled) {
		if (settings.additionalParams != null) {
			additionalParams = settings.additionalParams
		}
	}

	fun setDeviceProperty(property: DeviceChangeableProperties, value: String) {
		if (additionalParams.containsKey(property)) {
			additionalParams[property] = value
		}
	}
}