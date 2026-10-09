package net.osmand.plus.plugins.externalsensors.devices.ant;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntHeartRateSensor;

public class AntHeartRateDevice extends AntAbstractDevice<AntPlusHeartRatePcc> {

	public AntHeartRateDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntHeartRateSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.ANT_HEART_RATE;
	}

	public static AntHeartRateDevice createSearchableDevice() {
		return new AntHeartRateDevice(SEARCHING_ID_PREFIX
				+ AntHeartRateDevice.class.getSimpleName());
	}

	@Override
	protected PccReleaseHandle<AntPlusHeartRatePcc> requestAccess(@NonNull Context context, int deviceNumber) {
		return AntPlusHeartRatePcc.requestAccess(context, deviceNumber, 0,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusHeartRatePcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusHeartRatePcc.requestAccess(activity, context,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}
}
