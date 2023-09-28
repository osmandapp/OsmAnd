package net.osmand.plus.plugins.externalsensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DevicesSettingsCollection {

	private static final String DEVICES_SETTINGS_PREF_ID = "external_devices_settings";

	private final CommonPreference<String> preference;
	private final Gson gson;
	private final Map<String, DeviceSettings> settings = new HashMap<>();
	private List<DevicePreferencesListener> listeners = new ArrayList<>();


	public interface DevicePreferencesListener {
		void onDeviceEnabled(@NonNull String deviceId);

		void onDeviceDisabled(@NonNull String deviceId);
	}

	public DevicesSettingsCollection(@NonNull ExternalSensorsPlugin plugin) {
		gson = new GsonBuilder().create();
		preference = plugin.registerStringPref(DEVICES_SETTINGS_PREF_ID, "");
		readSettings();
	}

	public void addListener(@NonNull DevicePreferencesListener listener) {
		if (!listeners.contains(listener)) {
			List<DevicePreferencesListener> newListeners = new ArrayList<>(listeners);
			newListeners.add(listener);
			listeners = newListeners;
		}
	}

	public void removeListener(@NonNull DevicePreferencesListener listener) {
		if (listeners.contains(listener)) {
			List<DevicePreferencesListener> newListeners = new ArrayList<>(listeners);
			newListeners.remove(listener);
			listeners = newListeners;
		}
	}

	@NonNull
	public Set<String> getDeviceIds() {
		synchronized (settings) {
			return new HashSet<>(settings.keySet());
		}
	}

	@Nullable
	public DeviceSettings getDeviceSettings(@NonNull String deviceId) {
		synchronized (settings) {
			DeviceSettings deviceSettings = settings.get(deviceId);
			return deviceSettings != null ? createDeviceSettings(deviceSettings) : null;
		}
	}

	private DeviceSettings createDeviceSettings(@NonNull DeviceSettings settings) {
		switch (settings.getDeviceType()) {
			case ANT_BICYCLE_SD:
			case BLE_BICYCLE_SCD:
				return new WheelDeviceSettings(settings);
			default:
				return new DeviceSettings(settings);
		}
	}

	public static DeviceSettings createDeviceSettings(String deviceId, DeviceType deviceType, String name, boolean deviceEnabled) {
		switch (deviceType) {
			case ANT_BICYCLE_SD:
			case BLE_BICYCLE_SCD:
				return new WheelDeviceSettings(deviceId, deviceType, name, deviceEnabled);
			default:
				return new DeviceSettings(deviceId, deviceType, name, deviceEnabled);
		}
	}

	public void setDeviceSettings(
			@NonNull String deviceId,
			@Nullable DeviceSettings deviceSettings) {
		setDeviceSettings(deviceId, deviceSettings, true);
	}

	public void setDeviceSettings(
			@NonNull String deviceId,
			@Nullable DeviceSettings deviceSettings,
			boolean write) {
		boolean stateChanged;
		synchronized (settings) {
			if (deviceSettings == null) {
				settings.remove(deviceId);
				stateChanged = true;
			} else {
				DeviceSettings prevSettings = settings.get(deviceId);
				settings.put(deviceId, deviceSettings);
				stateChanged = prevSettings != null && prevSettings.getDeviceEnabled() != deviceSettings.getDeviceEnabled();
			}
			if (write) {
				writeSettings();
			}
		}
		if (stateChanged) {
			fireDeviceStateChangedEvent(deviceId, deviceSettings != null && deviceSettings.getDeviceEnabled());
		}
	}

	private void fireDeviceStateChangedEvent(@NonNull String deviceId, boolean enabled) {
		for (DevicePreferencesListener listener : listeners) {
			if (enabled) {
				listener.onDeviceEnabled(deviceId);
			} else {
				listener.onDeviceDisabled(deviceId);
			}
		}
	}

	private void readSettings() {
		String settingsJson = preference.get();
		if (!Algorithms.isEmpty(settingsJson)) {
			Map<String, DeviceSettings> settings = gson.fromJson(settingsJson,
					new TypeToken<HashMap<String, DeviceSettings>>() {
					}.getType());
			if (settings != null) {
				this.settings.clear();
				this.settings.putAll(settings);
			}
		}
	}

	private void writeSettings() {
		String json = gson.toJson(settings,
				new TypeToken<HashMap<String, DeviceSettings>>() {
				}.getType());
		preference.set(json);
	}
}
