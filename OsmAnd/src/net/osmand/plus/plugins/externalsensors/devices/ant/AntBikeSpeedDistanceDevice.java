package net.osmand.plus.plugins.externalsensors.devices.ant;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBikeDistanceSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBikeSpeedSensor;

public class AntBikeSpeedDistanceDevice extends AntAbstractDevice<AntPlusBikeSpeedDistancePcc> {

	private AntBikeSpeedCadenceDevice spdCadDevice;

	private class BikeSpeedDistancePluginAccessResultReceiver extends PluginAccessResultReceiver {

		private final Context context;

		public BikeSpeedDistancePluginAccessResultReceiver(@NonNull Context context) {
			this.context = context;
		}

		@Override
		public void onResultReceived(AntPlusBikeSpeedDistancePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
			super.onResultReceived(result, resultCode, initialDeviceState);
			if (pcc != null) {
				if (pcc.isSpeedAndCadenceCombinedSensor()) {
					spdCadDevice = new AntBikeSpeedCadenceDevice(String.valueOf(deviceNumber));
					spdCadDevice.combined = true;
					spdCadDevice.requestAccess(context, deviceNumber);
				}
			}
		}
	}

	public AntBikeSpeedDistanceDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntBikeSpeedSensor(this));
		sensors.add(new AntBikeDistanceSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.ANT_BICYCLE_SD;
	}

	public static AntBikeSpeedDistanceDevice createSearchableDevice() {
		return new AntBikeSpeedDistanceDevice(SEARCHING_ID_PREFIX
				+ AntBikeSpeedDistanceDevice.class.getSimpleName());
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeSpeedDistancePcc> requestAccess(@NonNull Context context, int deviceNumber) {
		return AntPlusBikeSpeedDistancePcc.requestAccess(context, deviceNumber, 0,
				combined, new BikeSpeedDistancePluginAccessResultReceiver(context), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeSpeedDistancePcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusBikeSpeedDistancePcc.requestAccess(activity, context,
				new BikeSpeedDistancePluginAccessResultReceiver(context), deviceStateChangeReceiver);
	}

	@Override
	public void disconnect() {
		super.disconnect();
		if (spdCadDevice != null) {
			spdCadDevice.disconnect();
		}
	}
}
