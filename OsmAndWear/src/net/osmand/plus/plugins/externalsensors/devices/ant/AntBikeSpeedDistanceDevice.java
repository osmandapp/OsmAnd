package net.osmand.plus.plugins.externalsensors.devices.ant;

import static com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SUCCESS;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntAbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBikeDistanceSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBikeSpeedSensor;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

public class AntBikeSpeedDistanceDevice extends AntLegacyDevice<AntPlusBikeSpeedDistancePcc> {

	private AntBikeSpeedCadenceDevice spdCadDevice;
	float wheelCircumference = 0f;

	private class BikeSpeedDistancePluginAccessResultReceiver extends PluginAccessResultReceiver {

		private final Context context;

		public BikeSpeedDistancePluginAccessResultReceiver(@NonNull Context context) {
			this.context = context;
		}

		@Override
		public void onResultReceived(AntPlusBikeSpeedDistancePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
			super.onResultReceived(result, resultCode, initialDeviceState);
			if (resultCode == SUCCESS && pcc != null) {
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

	public float getWheelCircumference() {
		return wheelCircumference;
	}

	public void setWheelCircumference(float wheelCircumference) {
		this.wheelCircumference = wheelCircumference;
		for (AntAbstractSensor sensor : sensors) {
			sensor.subscribeToEvents();
		}
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.ANT_BICYCLE_SD;
	}

	public static AntBikeSpeedDistanceDevice createSearchableDevice(OsmandApplication application) {
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
	public boolean disconnect() {
		boolean res = super.disconnect();
		if (spdCadDevice != null) {
			res |= spdCadDevice.disconnect();
		}
		return res;
	}

	@NonNull
	@Override
	public List<DeviceChangeableProperty> getChangeableProperties() {
		return Collections.singletonList(DeviceChangeableProperty.WHEEL_CIRCUMFERENCE);
	}

	@Override
	public void setChangeableProperty(DeviceChangeableProperty property, String value) {
		if (property == DeviceChangeableProperty.WHEEL_CIRCUMFERENCE) {
			try {
				setWheelCircumference(Float.parseFloat(value));
			} catch(RuntimeException e) {
				LOG.error(e.getMessage(), e);
			}
		} else {
			super.setChangeableProperty(property, value);
		}
	}
}
