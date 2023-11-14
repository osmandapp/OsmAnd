package net.osmand.plus.plugins.externalsensors

import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty

class WheelDeviceSettings : DeviceSettings {
	companion object {
		const val DEFAULT_WHEEL_CIRCUMFERENCE = 2.086f
	}

	constructor(
		deviceId: String, deviceType: DeviceType,
		deviceName: String, deviceEnabled: Boolean) : super(
		deviceId, deviceType,
		deviceName, deviceEnabled) {
		additionalParams[DeviceChangeableProperty.WHEEL_CIRCUMFERENCE] =
			DEFAULT_WHEEL_CIRCUMFERENCE.toString()
	}

	constructor(settings: DeviceSettings) : super(settings) {
		if (!additionalParams.containsKey(DeviceChangeableProperty.WHEEL_CIRCUMFERENCE)) {
			additionalParams[DeviceChangeableProperty.WHEEL_CIRCUMFERENCE] =
				DEFAULT_WHEEL_CIRCUMFERENCE.toString()
		}
	}

}