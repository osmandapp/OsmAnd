package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public abstract class AbstractSensor {

	protected final AbstractDevice<?> device;
	protected final String sensorId;

	public AbstractSensor(@NonNull AbstractDevice<?> device, @NonNull String sensorId) {
		this.device = device;
		this.sensorId = sensorId;
	}

	@NonNull
	public String getSensorId() {
		return sensorId;
	}

	@NonNull
	public AbstractDevice<?> getDevice() {
		return device;
	}

	@NonNull
	public abstract String getName();

	@NonNull
	public abstract List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes();

	@Nullable
	public abstract List<SensorData> getLastSensorDataList();

	public abstract void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AbstractSensor that = (AbstractSensor) o;
		return Algorithms.stringsEqual(sensorId, that.sensorId);
	}

	@Override
	public int hashCode() {
		return getClass().getSimpleName().hashCode();
	}

	@NonNull
	@Override
	public String toString() {
		return getName() + " (" + getSensorId() + ")";
	}
}
