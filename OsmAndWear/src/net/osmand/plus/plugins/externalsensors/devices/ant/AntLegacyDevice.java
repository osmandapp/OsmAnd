package net.osmand.plus.plugins.externalsensors.devices.ant;

import androidx.annotation.NonNull;

import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;

import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntRssiLegacySensor;

public abstract class AntLegacyDevice<T extends AntPlusLegacyCommonPcc> extends AntAbstractDevice<T> {

	public AntLegacyDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntRssiLegacySensor<>(this));
	}

	@Override
	public void fireSensorDataEvent(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
		if (sensor instanceof AntRssiLegacySensor) {
			AntRssiLegacySensor.RssiData rssiData = ((AntRssiLegacySensor<?>) sensor).getRssiData();
			if (rssiData != null) {
				rssi = (int) rssiData.getRssi();
			}
		}
		super.fireSensorDataEvent(sensor, data);
	}
}