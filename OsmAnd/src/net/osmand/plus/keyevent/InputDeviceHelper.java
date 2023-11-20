package net.osmand.plus.keyevent;

import static net.osmand.util.Algorithms.objectEquals;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.callbacks.EventType;
import net.osmand.plus.keyevent.callbacks.InputDevicesEventCallback;
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
	private ApplicationMode appMode;

	private final List<InputDeviceProfile> defaultDevices = Arrays.asList(KEYBOARD, PARROT, WUNDER_LINQ);
	private final List<InputDeviceProfile> customDevices = new ArrayList<>();
	private final Map<String, InputDeviceProfile> cachedDevices = new HashMap<>();
	private final List<InputDevicesEventCallback> listeners = new ArrayList<>();

	public InputDeviceHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		updateAppModeIfNeeded(settings.getApplicationMode());
	}

	@NonNull
	public List<InputDeviceProfile> getAvailableDevices() {
		List<InputDeviceProfile> result = new ArrayList<>();
		result.addAll(defaultDevices);
		result.addAll(customDevices);
		return result;
	}

	public void addListener(@NonNull InputDevicesEventCallback listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull InputDevicesEventCallback listener) {
		listeners.remove(listener);
	}

	public void selectInputDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(appMode, deviceId);
		notifyListeners(EventType.SELECT_DEVICE);
	}

	public void createAndSaveCustomDevice(@NonNull ApplicationMode appMode, @NonNull String newName) {
		updateAppModeIfNeeded(appMode);
		saveCustomDevice(makeCustomDevice(newName));
	}

	public void createAndSaveDeviceDuplicate(@NonNull ApplicationMode appMode, @NonNull InputDeviceProfile device) {
		updateAppModeIfNeeded(appMode);
		saveCustomDevice(makeCustomDeviceDuplicate(device));
	}

	public void renameCustomDevice(@NonNull ApplicationMode appMode,
	                               @NonNull CustomInputDeviceProfile device,
	                               @NonNull String newName) {
		updateAppModeIfNeeded(appMode);
		device.setCustomName(newName);
		syncSettings(EventType.RENAME_DEVICE);
	}

	@NonNull
	private InputDeviceProfile makeCustomDeviceDuplicate(@NonNull InputDeviceProfile device) {
		String prevName = device.toHumanString(app);
		String uniqueName = makeUniqueName(prevName);
		return makeCustomDevice(uniqueName, device);
	}

	private String makeUniqueName(@NonNull String oldName) {
		return Algorithms.makeUniqueName(oldName, newName -> !hasDeviceNameDuplicate(appMode, newName));
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
		syncSettings(EventType.ADD_NEW_DEVICE);
	}

	public void removeCustomDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		updateAppModeIfNeeded(appMode);
		InputDeviceProfile device = cachedDevices.remove(deviceId);
		if (device != null) {
			customDevices.remove(device);
			cachedDevices.remove(deviceId);
			resetSelectedDeviceIfNeeded(appMode);
			syncSettings(EventType.DELETE_DEVICE);
		}
	}

	public void resetSelectedDeviceIfNeeded(@NonNull ApplicationMode appMode) {
		updateAppModeIfNeeded(appMode);
		InputDeviceProfile device = getSelectedDevice(appMode);
		if (device == null) {
			// If selected device is unknown for application mode
			// we should reset it to default value
			settings.EXTERNAL_INPUT_DEVICE.resetModeToDefault(appMode);
		}
	}

	public boolean hasKeybindingNameDuplicate(@NonNull ApplicationMode appMode,
	                                          @NonNull String deviceId, @NonNull String newName) {
		updateAppModeIfNeeded(appMode);
		InputDeviceProfile device = getDeviceById(appMode, deviceId);
		if (device == null) {
			return false;
		}
		List<KeyBinding> keyBindings = device.getKeyBindings();
		for (KeyBinding keyBinding : keyBindings) {
			if (Objects.equals(keyBinding.getName(app), newName)) {
				return true;
			}
		}
		return false;
	}

	public void updateKeyBinding(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                             @NonNull KeyBinding oldKeyBinding, @NonNull KeyBinding newKeyBinding) {
		updateAppModeIfNeeded(appMode);
		InputDeviceProfile device = getDeviceById(appMode, deviceId);
		if (device != null) {
			resetPreviousKeyBindingIfNeeded(device, newKeyBinding);
			device.updateKeyBinding(oldKeyBinding, newKeyBinding);
			syncSettings(EventType.UPDATE_KEY_BINDING);
		}
	}

	private void resetPreviousKeyBindingIfNeeded(@NonNull InputDeviceProfile device,
	                                             @NonNull KeyBinding newKeyBinding) {
		int keyCode = newKeyBinding.getKeyCode();
		KeyBinding prevKeyBinding = device.findActiveKeyBinding(keyCode);
		if (prevKeyBinding != null && !objectEquals(newKeyBinding, prevKeyBinding)) {
			KeyBinding newAssignment = new KeyBinding(KeyEvent.KEYCODE_UNKNOWN, prevKeyBinding);
			updateKeyBinding(appMode, device.getId(), prevKeyBinding, newAssignment);
		}
	}

	public void resetAllKeyBindings(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		updateAppModeIfNeeded(appMode);
		InputDeviceProfile device = getDeviceById(appMode, deviceId);
		if (device != null) {
			device.resetAllKeyBindings();
			syncSettings(EventType.RESET_ALL_KEY_BINDINGS);
		}
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
	public InputDeviceProfile getDeviceById(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		updateAppModeIfNeeded(appMode);
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
		return id != null ? getDeviceById(appMode, id) : null;
	}

	@Nullable
	public String getSelectedDeviceId(@NonNull ApplicationMode appMode) {
		return settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode);
	}

	public boolean hasDeviceNameDuplicate(@NonNull ApplicationMode appMode, @NonNull String newName) {
		updateAppModeIfNeeded(appMode);
		for (InputDeviceProfile device : getAvailableDevices()) {
			if (objectEquals(device.toHumanString(app).trim(), newName.trim())) {
				return true;
			}
		}
		return false;
	}

	private void notifyListeners(@NonNull EventType event) {
		for (InputDevicesEventCallback listener : listeners) {
			listener.processInputDevicesEvent(event);
		}
	}

	/**
	 * You must call this method at the beginning of each public method
	 * that use devices cache and can be called from the outside.
	 * This will help ensure correct interaction with profile-dependent parameters.
	 *
	 * This method checks if the app mode has changed since the last time this method was called,
	 * and if so, reloads cached devices for currently selected app mode from the settings.
	 *
	 * @param appMode - currently selected app mode for the context
	 *                   from where the public method was called.
	 */
	private void updateAppModeIfNeeded(@NonNull ApplicationMode appMode) {
		if (this.appMode != appMode) {
			this.appMode = appMode;
			updateCachedInputDevices();
		}
	}

	private void updateCachedInputDevices() {
		customDevices.clear();
		cachedDevices.clear();

		customDevices.addAll(loadCustomDevices(appMode));
		for (InputDeviceProfile device : getAvailableDevices()) {
			device.initialize(app);
			cachedDevices.put(device.getId(), device);
		}
	}

	@NonNull
	private List<InputDeviceProfile> loadCustomDevices(@NonNull ApplicationMode appMode) {
		String json = settings.CUSTOM_EXTERNAL_INPUT_DEVICES.getModeValue(appMode);
		if (!Algorithms.isEmpty(json)) {
			try {
				return readFromJson(new JSONObject(json));
			} catch (JSONException e) {
				LOG.debug("Error when reading custom devices from JSON ", e);
			}
		}
		return new ArrayList<>();
	}

	public void syncSettings(@NonNull EventType eventType) {
		JSONObject json = new JSONObject();
		try {
			writeToJson(json, customDevices);
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(appMode, json.toString());
		} catch (JSONException e) {
			LOG.debug("Error when writing custom devices to JSON ", e);
		}
		notifyListeners(eventType);
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
