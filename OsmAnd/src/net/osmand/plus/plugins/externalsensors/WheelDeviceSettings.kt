package net.osmand.plus.plugins.externalsensors

import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties

class WheelDeviceSettings : DeviceSettings {
	companion object {
		const val DEFAULT_WHEEL_CIRCUMFERENCE = 2.086f
	}

	constructor(
		deviceId: String, deviceType: DeviceType,
		deviceName: String, deviceEnabled: Boolean) : super(
		deviceId, deviceType,
		deviceName, deviceEnabled) {
		additionalParams[DeviceChangeableProperties.WHEEL_CIRCUMFERENCE] =
			DEFAULT_WHEEL_CIRCUMFERENCE.toString()
	}

	constructor(settings: DeviceSettings) : super(settings) {
		if (!additionalParams.containsKey(DeviceChangeableProperties.WHEEL_CIRCUMFERENCE)) {
			additionalParams[DeviceChangeableProperties.WHEEL_CIRCUMFERENCE] =
				DEFAULT_WHEEL_CIRCUMFERENCE.toString()
		}
	}

}