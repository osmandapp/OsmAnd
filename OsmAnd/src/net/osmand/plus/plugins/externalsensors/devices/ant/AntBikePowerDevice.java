package net.osmand.plus.plugins.externalsensors.devices.ant;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBikePowerSensor;

public class AntBikePowerDevice extends AntCommonDevice<AntPlusBikePowerPcc> {

	public AntBikePowerDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntBikePowerSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.ANT_BICYCLE_POWER;
	}

	public static AntBikePowerDevice createSearchableDevice() {
		return new AntBikePowerDevice(SEARCHING_ID_PREFIX
				+ AntBikePowerDevice.class.getSimpleName());
	}

	@Override
	protected PccReleaseHandle<AntPlusBikePowerPcc> requestAccess(@NonNull Context context, int deviceNumber) {
		return AntPlusBikePowerPcc.requestAccess(context, deviceNumber, 0,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikePowerPcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusBikePowerPcc.requestAccess(activity, context, new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}
}
