package net.osmand.plus.plugins.externalsensors.devices;

import static net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty.NAME;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractDevice<T extends AbstractSensor> {

	private static final int BATTERY_LOW_LEVEL_THRESHOLD = 15;
	public static final int BATTERY_UNKNOWN_LEVEL_VALUE = -1;

	protected final String deviceId;
	protected int batteryLevel = BATTERY_UNKNOWN_LEVEL_VALUE;
	protected int rssi = -1;
	private DeviceConnectionState state = DeviceConnectionState.DISCONNECTED;
	protected List<DeviceListener> listeners = new ArrayList<>();
	protected List<T> sensors = new ArrayList<>();
	protected String deviceName;

	public interface DeviceListener {

		void onDeviceConnecting(@NonNull AbstractDevice<?> device);

		@AnyThread
		void onDeviceConnect(@NonNull AbstractDevice<?> device, @NonNull DeviceConnectionResult result,
		                     @Nullable String error);

		@AnyThread
		void onDeviceDisconnect(@NonNull AbstractDevice<?> device);

		void onSensorData(@NonNull AbstractSensor sensor, @NonNull SensorData data);
	}

	public AbstractDevice(@NonNull String deviceId) {
		this.deviceId = deviceId;
	}

	protected void setCurrentState(@NonNull DeviceConnectionState newState) {
		state = newState;
	}

	@NonNull
	public String getDeviceId() {
		return deviceId;
	}

	@NonNull
	public abstract DeviceType getDeviceType();

	public List<T> getSensors() {
		return new ArrayList<>(sensors);
	}

	public boolean hasBatteryLevel() {
		return batteryLevel > BATTERY_UNKNOWN_LEVEL_VALUE;
	}

	public int getBatteryLevel() {
		return batteryLevel;
	}

	public boolean hasRssi() {
		return rssi > -1;
	}

	public int getRssi() {
		return rssi;
	}

	public boolean isConnected() {
		return state == DeviceConnectionState.CONNECTED;
	}

	public boolean isConnecting() {
		return state == DeviceConnectionState.CONNECTING;
	}

	public boolean isDisconnected() {
		return state == DeviceConnectionState.DISCONNECTED;
	}

	public boolean isBatteryLow() {
		return hasBatteryLevel() && batteryLevel < BATTERY_LOW_LEVEL_THRESHOLD;
	}

	@NonNull
	public String getName() {
		return deviceName != null ? deviceName : getClass().getSimpleName();
	}

	public void setDeviceName(String name) {
		deviceName = name;
	}

	public abstract boolean connect(@NonNull Context context, @Nullable Activity activity);

	public abstract boolean disconnect();

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AbstractDevice<?> that = (AbstractDevice<?>) o;
		return Algorithms.stringsEqual(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return getClass().getSimpleName().hashCode();
	}

	public void addListener(@NonNull DeviceListener listener) {
		if (!listeners.contains(listener)) {
			List<DeviceListener> newListeners = new ArrayList<>(listeners);
			newListeners.add(listener);
			listeners = newListeners;
		}
	}

	public void removeListener(@NonNull DeviceListener listener) {
		if (listeners.contains(listener)) {
			List<DeviceListener> newListeners = new ArrayList<>(listeners);
			newListeners.remove(listener);
			listeners = newListeners;
		}
	}

	public void fireSensorDataEvent(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
		for (DeviceListener listener : listeners) {
			listener.onSensorData(sensor, data);
		}
	}

	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		for (T sensor : sensors) {
			if (sensor.getSupportedWidgetDataFieldTypes().contains(widgetDataFieldType)) {
				sensor.writeSensorDataToJson(json, widgetDataFieldType);
			}
		}
	}

	@NonNull
	public List<DeviceChangeableProperty> getChangeableProperties() {
		return Collections.emptyList();
	}

	public void setChangeableProperty(DeviceChangeableProperty property, String value) {
		if (property == NAME) {
			setDeviceName(value);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return getName() + " (" + getDeviceId() + ")";
	}
}
