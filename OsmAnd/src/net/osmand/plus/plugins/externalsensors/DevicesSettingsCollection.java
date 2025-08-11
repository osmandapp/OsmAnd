package net.osmand.plus.plugins.externalsensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.CommonPreferenceProvider;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DevicesSettingsCollection {

	public static final float DEFAULT_WHEEL_CIRCUMFERENCE = 2.086f;

	private final CommonPreference<String> preference;
	private final Gson gson;
	private final Map<String, DeviceSettings> settings = new ConcurrentHashMap<>();
	private List<DevicePreferencesListener> listeners = new ArrayList<>();


	public static class DeviceSettings {
		final String deviceId;
		final DeviceType deviceType;
		boolean enabled;
		Map<DeviceChangeableProperty, String> additionalParams = new LinkedHashMap<>();

		public DeviceSettings(String deviceId, @NonNull AbstractDevice<?> device, boolean deviceEnabled) {
			this.deviceId = deviceId;
			this.deviceType = device.getDeviceType();
			this.enabled = deviceEnabled;
			additionalParams.put(DeviceChangeableProperty.NAME, device.getName());
			for(DeviceChangeableProperty property : device.getChangeableProperties()) {
				additionalParams.put(property, property.getDefValue());
			}
		}

		public Map<DeviceChangeableProperty, String> getParams() {
			return additionalParams;
		}

		public DeviceType getDeviceType() {
			return deviceType;
		}

		public boolean getDeviceEnabled() {
			return enabled;
		}

		public void setDeviceEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setDeviceProperty(DeviceChangeableProperty property, String normalizedValue) {
			LinkedHashMap<DeviceChangeableProperty, String> newParams = new LinkedHashMap<>(additionalParams);
			newParams.put(property, normalizedValue);
			additionalParams = newParams;
		}

		public void verifyInit() {
			if (additionalParams == null) {
				additionalParams = new LinkedHashMap<>();
			}
		}
	}
	public interface DevicePreferencesListener {
		void onDeviceEnabled(@NonNull String deviceId);

		void onDeviceDisabled(@NonNull String deviceId);
	}

	public DevicesSettingsCollection(@NonNull CommonPreferenceProvider<String> preferenceProvider) {
		gson = new GsonBuilder().create();
		preference = preferenceProvider.getPreference();
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
		return settings.keySet();
	}

	@Nullable
	public DeviceSettings getDeviceSettings(@NonNull String deviceId) {
		return settings.get(deviceId);
	}

	public void removeDeviceSettings(@NonNull String deviceId) {
		settings.remove(deviceId);
	}

	public static DeviceSettings createDeviceSettings(String deviceId, @NonNull AbstractDevice<?> device, boolean deviceEnabled) {
		return new DeviceSettings(deviceId, device, deviceEnabled);
	}

	public void setDeviceSettings(@NonNull String deviceId, @Nullable DeviceSettings deviceSettings) {
		setDeviceSettings(deviceId, deviceSettings, true);
	}

	public void setDeviceSettings(@NonNull String deviceId, @Nullable DeviceSettings deviceSettings, boolean write) {
		boolean stateChanged;
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
			Map<String, DeviceSettings> settings = gson.fromJson(settingsJson, new TypeToken<HashMap<String, DeviceSettings>>() {
			}.getType());
			if (settings != null) {
				this.settings.clear();
				// some versions gson don't call constructor properly?
				for (DeviceSettings s : settings.values()) {
					s.verifyInit();
				}
				this.settings.putAll(settings);
			}
		}
	}

	private void writeSettings() {
		String json = gson.toJson(settings);
		preference.set(json);
	}
}
