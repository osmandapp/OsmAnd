package net.osmand.plus.plugins.externalsensors.devices.ant;

import androidx.annotation.NonNull;

import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc;

import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntBatterySensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntRssiCommonSensor;

public abstract class AntCommonDevice<T extends AntPlusCommonPcc> extends AntAbstractDevice<T> {

	public AntCommonDevice(@NonNull String deviceId) {
		super(deviceId);
		sensors.add(new AntRssiCommonSensor<>(this));
		sensors.add(new AntBatterySensor<>(this));
	}

	@Override
	public void fireSensorDataEvent(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
		if (sensor instanceof AntRssiCommonSensor) {
			AntRssiCommonSensor.RssiData rssiData = ((AntRssiCommonSensor<?>) sensor).getRssiData();
			if (rssiData != null) {
				rssi = (int) rssiData.getRssi();
			}
		}
		super.fireSensorDataEvent(sensor, data);
	}
}