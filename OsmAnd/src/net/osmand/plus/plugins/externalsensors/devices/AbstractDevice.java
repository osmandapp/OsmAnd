package net.osmand.plus.plugins.externalsensors.devices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDevice<T extends AbstractSensor> {

	private static final int BATTERY_LOW_LEVEL_THRESHOLD = 15;

	protected final String deviceId;
	protected int batteryLevel = -1;
	protected int rssi = -1;
	protected DeviceConnectionState state = DeviceConnectionState.DISCONNECTED;
	protected List<DeviceListener> listeners = new ArrayList<>();
	protected List<T> sensors = new ArrayList<>();

	public interface DeviceListener {

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
		return batteryLevel > -1;
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
	public abstract String getName();

	public abstract boolean connect(@NonNull Context context, @Nullable Activity activity);

	public abstract void disconnect();

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

	public void writeSensorDataToJson(@NonNull JSONObject json) throws JSONException {
		for (T sensor : sensors) {
			sensor.writeSensorDataToJson(json);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return getName() + " (" + getDeviceId() + ")";
	}
}
