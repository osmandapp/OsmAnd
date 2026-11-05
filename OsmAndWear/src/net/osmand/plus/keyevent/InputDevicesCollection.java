package net.osmand.plus.keyevent;

import static net.osmand.util.Algorithms.objectEquals;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile;
import net.osmand.plus.keyevent.devices.DefaultInputDevices;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InputDevicesCollection {

	private final ApplicationMode appMode;
	private List<InputDeviceProfile> customDevices;
	private final List<InputDeviceProfile> defaultDevices;
	private Map<String, InputDeviceProfile> cachedDevices = new HashMap<>();

	public InputDevicesCollection(@NonNull ApplicationMode appMode,
	                              @NonNull List<InputDeviceProfile> customDevices) {
		this.appMode = appMode;
		this.customDevices = customDevices;
		this.defaultDevices = DefaultInputDevices.values();
		syncCachedDevices();
	}

	public boolean hasDeviceNameDuplicate(@NonNull Context context, @NonNull String newName) {
		for (InputDeviceProfile device : getAllDevices()) {
			if (objectEquals(device.toHumanString(context).trim(), newName.trim())) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	@NonNull
	public List<InputDeviceProfile> getAllDevices() {
		return CollectionUtils.asOneList(defaultDevices, customDevices);
	}

	@NonNull
	public List<InputDeviceProfile> getCustomDevices() {
		return customDevices;
	}

	public void addCustomDevice(@NonNull InputDeviceProfile device) {
		customDevices = CollectionUtils.addToList(customDevices, device);
		syncCachedDevices();
	}

	public void removeCustomDevice(String deviceId) {
		InputDeviceProfile device = getDeviceById(deviceId);
		if (device != null) {
			customDevices = CollectionUtils.removeFromList(customDevices, device);
			syncCachedDevices();
		}
	}

	@Nullable
	public CustomInputDeviceProfile getCustomDeviceById(@NonNull String deviceId) {
		InputDeviceProfile device = getDeviceById(deviceId);
		if (device instanceof CustomInputDeviceProfile) {
			return (CustomInputDeviceProfile) device;
		}
		return null;
	}

	@Nullable
	public InputDeviceProfile getDeviceById(@NonNull String deviceId) {
		return cachedDevices.get(deviceId);
	}

	private void syncCachedDevices() {
		Map<String ,InputDeviceProfile> newCachedDevices = new HashMap<>();
		for (InputDeviceProfile device : getAllDevices()) {
			newCachedDevices.put(device.getId(), device);
		}
		cachedDevices = newCachedDevices;
	}
}
