package net.osmand.plus.plugins.externalsensors.devices.ant;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBikeCadenceSensor;

public class AntBikeSpeedCadenceDevice extends AntAbstractDevice<AntPlusBikeCadencePcc> {

	private AntBikeSpeedDistanceDevice spdDistDevice;

	private class BikeSpeedCadencePluginAccessResultReceiver extends PluginAccessResultReceiver {

		private final Context context;

		public BikeSpeedCadencePluginAccessResultReceiver(@NonNull Context context) {
			this.context = context;
		}

		@Override
		public void onResultReceived(AntPlusBikeCadencePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
			super.onResultReceived(result, resultCode, initialDeviceState);
			if (pcc != null) {
				if (pcc.isSpeedAndCadenceCombinedSensor()) {
					spdDistDevice = new AntBikeSpeedDistanceDevice(String.valueOf(deviceNumber));
					spdDistDevice.combined = true;
					spdDistDevice.requestAccess(context, deviceNumber);
				}
			}
		}
	}

	public AntBikeSpeedCadenceDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntBikeCadenceSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.ANT_BICYCLE_SC;
	}

	public static AntBikeSpeedCadenceDevice createSearchableDevice() {
		return new AntBikeSpeedCadenceDevice(SEARCHING_ID_PREFIX
				+ AntBikeSpeedCadenceDevice.class.getSimpleName());
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeCadencePcc> requestAccess(@NonNull Context context, int deviceNumber) {
		return AntPlusBikeCadencePcc.requestAccess(context, deviceNumber, 0,
				combined, new BikeSpeedCadencePluginAccessResultReceiver(context), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeCadencePcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusBikeCadencePcc.requestAccess(activity, context,
				new BikeSpeedCadencePluginAccessResultReceiver(context), deviceStateChangeReceiver);
	}

	@Override
	public void disconnect() {
		super.disconnect();
		if (spdDistDevice != null) {
			spdDistDevice.disconnect();
		}
	}
}