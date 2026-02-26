package net.osmand.plus.plugins.externalsensors.devices.ant;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusEnvironmentPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntTemperatureSensor;

public class AntTemperatureDevice extends AntAbstractDevice<AntPlusEnvironmentPcc> {

	public AntTemperatureDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntTemperatureSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.ANT_TEMPERATURE;
	}

	public static AntTemperatureDevice createSearchableDevice() {
		return new AntTemperatureDevice(SEARCHING_ID_PREFIX
				+ AntTemperatureDevice.class.getSimpleName());
	}

	@Override
	protected PccReleaseHandle<AntPlusEnvironmentPcc> requestAccess(@NonNull Context context, int deviceNumber) {
		return AntPlusEnvironmentPcc.requestAccess(context, deviceNumber, 0,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusEnvironmentPcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusEnvironmentPcc.requestAccess(activity, context,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}
}
