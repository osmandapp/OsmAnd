package net.osmand.plus.keyevent;

import static net.osmand.util.Algorithms.isEmpty;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.callbacks.EventType;
import net.osmand.plus.keyevent.callbacks.InputDeviceHelperCallback;
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.keyevent.devices.ParrotDeviceProfile;
import net.osmand.plus.keyevent.devices.WunderLINQDeviceProfile;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InputDeviceHelper {

	public static final InputDeviceProfile KEYBOARD = new KeyboardDeviceProfile();
	public static final InputDeviceProfile PARROT = new ParrotDeviceProfile();
	public static final InputDeviceProfile WUNDER_LINQ = new WunderLINQDeviceProfile();

	private static final String CUSTOM_PREFIX = "custom_";
	private static final Log LOG = PlatformUtil.getLog(InputDeviceHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final List<InputDeviceProfile> defaultDevices = Arrays.asList(KEYBOARD, PARROT, WUNDER_LINQ);
	private final List<InputDeviceProfile> customDevices;
	private final Map<String, InputDeviceProfile> cachedDevices = new HashMap<>();
	private final List<InputDeviceHelperCallback> listeners = new ArrayList<>();

	public InputDeviceHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		customDevices = loadCustomDevices();
		for (InputDeviceProfile device : getAvailableDevices()) {
			device.initialize(app);
			cachedDevices.put(device.getId(), device);
		}
	}

	@NonNull
	public List<InputDeviceProfile> getAvailableDevices() {
		List<InputDeviceProfile> result = new ArrayList<>();
		result.addAll(defaultDevices);
		result.addAll(customDevices);
		return result;
	}

	public void addListener(@NonNull InputDeviceHelperCallback listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull InputDeviceHelperCallback listener) {
		listeners.remove(listener);
	}

	public void selectInputDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(appMode, deviceId);
		notifyListeners(EventType.SELECT_DEVICE);
	}

	public void createAndSaveCustomDevice(@NonNull String newName) {
		saveCustomDevice(makeCustomDevice(newName));
	}

	public void createAndSaveDeviceDuplicate(@NonNull InputDeviceProfile device) {
		saveCustomDevice(makeCustomDeviceDuplicate(device));
	}

	public void renameCustomDevice(@NonNull CustomInputDeviceProfile device, @NonNull String newName) {
		device.setCustomName(newName);
		syncSettings();
		notifyListeners(EventType.RENAME_DEVICE);
	}

	@NonNull
	private InputDeviceProfile makeCustomDeviceDuplicate(@NonNull InputDeviceProfile device) {
		String prevName = device.toHumanString(app);
		String uniqueName = makeUniqueName(prevName);
		return makeCustomDevice(uniqueName, device);
	}

	private String makeUniqueName(@NonNull String oldName) {
		return Algorithms.makeUniqueName(oldName, newName -> !hasNameDuplicate(newName));
	}

	@NonNull
	private InputDeviceProfile makeCustomDevice(@NonNull String newName) {
		return makeCustomDevice(newName, KEYBOARD);
	}

	@NonNull
	private InputDeviceProfile makeCustomDevice(
			@NonNull String newName, @NonNull InputDeviceProfile baseDevice
	) {
		String uniqueId = CUSTOM_PREFIX + System.currentTimeMillis();
		return makeCustomDevice(uniqueId, newName, baseDevice);
	}

	@NonNull
	private InputDeviceProfile makeCustomDevice(@NonNull String id, @NonNull String name,
	                                            @NonNull InputDeviceProfile parentDevice) {
		InputDeviceProfile device = new CustomInputDeviceProfile(id, name, parentDevice);
		device.initialize(app);
		return device;
	}

	private void saveCustomDevice(@NonNull InputDeviceProfile device) {
		customDevices.add(device);
		cachedDevices.put(device.getId(), device);
		syncSettings();
		notifyListeners(EventType.ADD_NEW_DEVICE);
	}

	public void removeCustomDevice(@NonNull String deviceId) {
		InputDeviceProfile device = cachedDevices.remove(deviceId);
		if (device != null) {
			customDevices.remove(device);
			cachedDevices.remove(deviceId);
			syncSettings();
			resetSelectedDeviceIfNeeded();
			notifyListeners(EventType.DELETE_DEVICE);
		}
	}

	public void resetSelectedDeviceIfNeeded() {
		for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
			resetSelectedDeviceIfNeeded(appMode);
		}
	}

	public void resetSelectedDeviceIfNeeded(@NonNull ApplicationMode appMode) {
		InputDeviceProfile device = getSelectedDevice(appMode);
		if (device == null) {
			// If selected device is unknown for application mode
			// we should reset it to default value
			settings.EXTERNAL_INPUT_DEVICE.resetModeToDefault(appMode);
		}
	}

	public void updateKeyBinding(@NonNull String deviceId, int originalKeyCode,
	                             @NonNull KeyBinding newKeyBinding) {
		InputDeviceProfile device = getDeviceById(deviceId);
		if (device == null) {
			return;
		}
		if (newKeyBinding.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
			device.removeKeyBinding(originalKeyCode);
		} else if (originalKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
			device.addKeyBinding(newKeyBinding);
		} else {
			device.updateKeyBinding(originalKeyCode, newKeyBinding);
		}
		syncSettings();
	}

	public boolean isSelectedDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		String selectedDeviceId = getSelectedDeviceId(appMode);
		return Objects.equals(selectedDeviceId, deviceId);
	}

	public boolean isCustomDevice(@NonNull InputDeviceProfile device) {
		String id = device.getId();
		return id.startsWith(CUSTOM_PREFIX);
	}

	@Nullable
	public InputDeviceProfile getDeviceById(@NonNull String deviceId) {
		return cachedDevices.get(deviceId);
	}

	@Nullable
	public InputDeviceProfile getEnabledDevice() {
		return getEnabledDevice(settings.getApplicationMode());
	}

	@Nullable
	public InputDeviceProfile getEnabledDevice(@NonNull ApplicationMode appMode) {
		if (settings.EXTERNAL_INPUT_DEVICE_ENABLED.getModeValue(appMode)) {
			return getSelectedDevice(appMode);
		}
		return null;
	}

	@Nullable
	public InputDeviceProfile getSelectedDevice(@NonNull ApplicationMode appMode) {
		String id = getSelectedDeviceId(appMode);
		return id != null ? cachedDevices.get(id) : null;
	}

	@Nullable
	public String getSelectedDeviceId(@NonNull ApplicationMode appMode) {
		return settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode);
	}

	public boolean hasNameDuplicate(@NonNull String newName) {
		for (InputDeviceProfile device : getAvailableDevices()) {
			if (Algorithms.objectEquals(device.toHumanString(app).trim(), newName.trim())) {
				return true;
			}
		}
		return false;
	}

	private void notifyListeners(@NonNull EventType event) {
		for (InputDeviceHelperCallback listener : listeners) {
			listener.processInputDeviceHelperEvent(event);
		}
	}

	@NonNull
	private List<InputDeviceProfile> loadCustomDevices() {
		String json = settings.CUSTOM_EXTERNAL_INPUT_DEVICES.get();
		try {
			return !isEmpty(json) ? readFromJson(new JSONObject(json)) : new ArrayList<>();
		} catch (JSONException e) {
			LOG.debug("Error when reading custom devices from JSON ", e);
		}
		return new ArrayList<>();
	}

	public void syncSettings() {
		JSONObject json = new JSONObject();
		try {
			writeToJson(json, customDevices);
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.set(json.toString());
		} catch (JSONException e) {
			LOG.debug("Error when writing custom devices to JSON ", e);
		}
	}

	public static List<InputDeviceProfile> readFromJson(@NonNull JSONObject json) throws JSONException {
		if (!json.has("items")) {
			return new ArrayList<>();
		}
		List<InputDeviceProfile> res = new ArrayList<>();
		JSONArray jsonArray = json.getJSONArray("items");
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				res.add(new CustomInputDeviceProfile(jsonArray.getJSONObject(i)));
			} catch (JSONException e) {
				LOG.debug("Error while reading a custom device from JSON ", e);
			}
		}
		return res;
	}

	public static void writeToJson(@NonNull JSONObject json,
	                               @NonNull List<InputDeviceProfile> customDevices) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (InputDeviceProfile device : customDevices) {
			jsonArray.put(((CustomInputDeviceProfile) device).toJson());
		}
		json.put("items", jsonArray);
	}
}
