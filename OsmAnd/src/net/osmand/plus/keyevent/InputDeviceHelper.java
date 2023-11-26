package net.osmand.plus.keyevent;

import static net.osmand.plus.keyevent.DefaultInputDevices.KEYBOARD;
import static net.osmand.util.Algorithms.objectEquals;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.callbacks.EventType;
import net.osmand.plus.keyevent.callbacks.InputDevicesEventListener;
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InputDeviceHelper {

	public static final int FUNCTIONALITY_CACHE_ID = 0;
	public static final int CUSTOMIZATION_CACHE_ID = 1;

	private static final Log LOG = PlatformUtil.getLog(InputDeviceHelper.class);
	private static final String CUSTOM_DEVICE_PREFIX = "custom_";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final List<InputDevicesEventListener> listeners = new ArrayList<>();
	private final Map<Integer, InputDevicesCollection> cachedDevicesCollections = new HashMap<>();

	public InputDeviceHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		DefaultInputDevices.initialize(app);
	}

	public void addListener(@NonNull InputDevicesEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull InputDevicesEventListener listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(@NonNull ApplicationMode appMode, @NonNull EventType event) {
		for (InputDevicesEventListener listener : listeners) {
			listener.processInputDevicesEvent(appMode, event);
		}
	}

	public void selectInputDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		settings.EXTERNAL_INPUT_DEVICE.setModeValue(appMode, deviceId);
		notifyListeners(appMode, EventType.SELECT_DEVICE);
	}

	public void createAndSaveCustomDevice(int cacheId, @NonNull ApplicationMode appMode,
	                                      @NonNull String newDeviceName) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		saveCustomDevice(devicesCollection, makeCustomDevice(newDeviceName));
	}

	public void createAndSaveDeviceDuplicate(int cacheId, @NonNull ApplicationMode appMode,
	                                         @NonNull InputDeviceProfile device) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		saveCustomDevice(devicesCollection, makeCustomDeviceDuplicate(devicesCollection, device));
	}

	public void renameCustomDevice(int cacheId, @NonNull ApplicationMode appMode,
	                               @NonNull String deviceId, @NonNull String newName) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		InputDeviceProfile device = devicesCollection.getDeviceById(deviceId);
		if (device instanceof CustomInputDeviceProfile) {
			((CustomInputDeviceProfile) device).setCustomName(newName);
			syncSettings(devicesCollection, EventType.RENAME_DEVICE);
		} else {
			LOG.debug("Trying to rename non-custom input device profile: " + deviceId);
		}
	}

	@NonNull
	private InputDeviceProfile makeCustomDeviceDuplicate(@NonNull InputDevicesCollection devicesCollection,
	                                                     @NonNull InputDeviceProfile device) {
		String prevName = device.toHumanString(app);
		String uniqueName = makeUniqueName(devicesCollection, prevName);
		return makeCustomDevice(uniqueName, device);
	}

	private String makeUniqueName(@NonNull InputDevicesCollection devicesCollection, @NonNull String oldName) {
		return Algorithms.makeUniqueName(oldName, newName -> !devicesCollection.hasDeviceNameDuplicate(app, newName));
	}

	@NonNull
	private InputDeviceProfile makeCustomDevice(@NonNull String newName) {
		return makeCustomDevice(newName, KEYBOARD);
	}

	@NonNull
	private InputDeviceProfile makeCustomDevice(@NonNull String newName,
	                                            @NonNull InputDeviceProfile baseDevice) {
		String uniqueId = CUSTOM_DEVICE_PREFIX + System.currentTimeMillis();
		return makeCustomDevice(uniqueId, newName, baseDevice);
	}

	@NonNull
	private InputDeviceProfile makeCustomDevice(@NonNull String id, @NonNull String name,
	                                            @NonNull InputDeviceProfile parentDevice) {
		return new CustomInputDeviceProfile(id, name, parentDevice).initialize(app);
	}

	private void saveCustomDevice(@NonNull InputDevicesCollection devicesCollection,
	                              @NonNull InputDeviceProfile device) {
		devicesCollection.addCustomDevice(device);
		syncSettings(devicesCollection, EventType.ADD_NEW_DEVICE);
	}

	public void removeCustomDevice(int cacheId, @NonNull ApplicationMode appMode,
	                               @NonNull String deviceId) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		devicesCollection.removeCustomDevice(deviceId);
		syncSettings(devicesCollection, EventType.DELETE_DEVICE);
	}

	public void updateKeyBinding(int cacheId, @NonNull ApplicationMode appMode, @NonNull String deviceId,
	                             @NonNull KeyBinding oldKeyBinding, @NonNull KeyBinding newKeyBinding) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		InputDeviceProfile device = devicesCollection.getDeviceById(deviceId);
		if (device != null) {
			resetPreviousKeyBindingIfNeeded(cacheId, appMode, device, newKeyBinding);
			device.updateKeyBinding(oldKeyBinding, newKeyBinding);
			syncSettings(devicesCollection, EventType.UPDATE_KEY_BINDING);
		}
	}

	private void resetPreviousKeyBindingIfNeeded(int cacheId, @NonNull ApplicationMode appMode,
	                                             @NonNull InputDeviceProfile device,
	                                             @NonNull KeyBinding newKeyBinding) {
		int keyCode = newKeyBinding.getKeyCode();
		KeyBinding prevKeyBinding = device.findActiveKeyBinding(keyCode);
		if (prevKeyBinding != null && !objectEquals(newKeyBinding, prevKeyBinding)) {
			KeyBinding newAssignment = new KeyBinding(KeyEvent.KEYCODE_UNKNOWN, prevKeyBinding);
			updateKeyBinding(cacheId, appMode, device.getId(), prevKeyBinding, newAssignment);
		}
	}

	public void resetAllKeyBindings(int cacheId, @NonNull ApplicationMode appMode,
	                                @NonNull String deviceId) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		InputDeviceProfile device = devicesCollection.getDeviceById(deviceId);
		if (device != null) {
			device.resetAllKeyBindings();
			syncSettings(devicesCollection, EventType.RESET_ALL_KEY_BINDINGS);
		}
	}

	public boolean hasDeviceNameDuplicate(int cacheId, @NonNull ApplicationMode appMode,
	                                      @NonNull Context context, String newName) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		return devicesCollection.hasDeviceNameDuplicate(context, newName);
	}

	public boolean hasKeybindingNameDuplicate(int cacheId, @NonNull ApplicationMode appMode,
	                                          OsmandApplication context, String deviceId, String newName) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		return devicesCollection.hasKeybindingNameDuplicate(context, deviceId, newName);
	}

	@NonNull
	public List<InputDeviceProfile> getAllDevices(int cacheId, @NonNull ApplicationMode appMode) {
		InputDevicesCollection devicesCollection = getInputDevicesCollection(cacheId, appMode);
		return devicesCollection.getAllDevices();
	}

	@Nullable
	public InputDeviceProfile getEnabledSelectedDevice(int cacheId, @NonNull ApplicationMode appMode) {
		if (settings.EXTERNAL_INPUT_DEVICE_ENABLED.getModeValue(appMode)) {
			return getSelectedDevice(cacheId, appMode);
		}
		return null;
	}

	@NonNull
	public InputDeviceProfile getSelectedDevice(int cacheId, @NonNull ApplicationMode appMode) {
		String id = getSelectedDeviceId(appMode);
		InputDeviceProfile device = id != null ? getDeviceById(cacheId, appMode, id) : null;
		return device != null ? device : KEYBOARD;
	}

	@Nullable
	private String getSelectedDeviceId(@NonNull ApplicationMode appMode) {
		return settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode);
	}
	@Nullable
	public InputDeviceProfile getDeviceById(int cacheId, @NonNull ApplicationMode appMode,
	                                        @NonNull String deviceId) {
		return getInputDevicesCollection(cacheId, appMode).getDeviceById(deviceId);
	}

	@Nullable
	public ApplicationMode getAppMode(int cacheId) {
		InputDevicesCollection collection = cachedDevicesCollections.get(cacheId);
		return collection != null ? collection.getAppMode() : null;
	}

	@NonNull
	private InputDevicesCollection getInputDevicesCollection(int cacheId, @NonNull ApplicationMode appMode) {
		InputDevicesCollection collection = cachedDevicesCollections.get(cacheId);
		if (collection == null || !Objects.equals(appMode, collection.getAppMode())) {
			collection = reloadInputDevicesCollection(cacheId, appMode);
		}
		return collection;
	}

	@NonNull
	public InputDevicesCollection reloadInputDevicesCollection(int cacheId, @NonNull ApplicationMode appMode) {
		InputDevicesCollection collection = new InputDevicesCollection(appMode, loadCustomDevices(appMode));
		cachedDevicesCollections.put(cacheId, collection);
		return collection;
	}

	public void releaseInputDevicesCache(int cacheId) {
		cachedDevicesCollections.remove(cacheId);
	}

	@NonNull
	private List<InputDeviceProfile> loadCustomDevices(@NonNull ApplicationMode appMode) {
		String json = settings.CUSTOM_EXTERNAL_INPUT_DEVICES.getModeValue(appMode);
		if (!Algorithms.isEmpty(json)) {
			try {
				return readFromJson(app, new JSONObject(json));
			} catch (JSONException e) {
				LOG.debug("Error while reading custom devices from JSON ", e);
			}
		}
		return new ArrayList<>();
	}

	private void syncSettings(@NonNull InputDevicesCollection devicesCollection,
	                          @NonNull EventType eventType) {
		JSONObject json = new JSONObject();
		ApplicationMode appMode = devicesCollection.getAppMode();
		try {
			writeToJson(json, devicesCollection.getCustomDevices());
			settings.CUSTOM_EXTERNAL_INPUT_DEVICES.setModeValue(appMode, json.toString());
		} catch (JSONException e) {
			LOG.debug("Error while writing custom devices to JSON ", e);
		}
		notifyListeners(appMode, eventType);
	}

	private static List<InputDeviceProfile> readFromJson(@NonNull OsmandApplication app,
	                                                     @NonNull JSONObject json) throws JSONException {
		if (!json.has("items")) {
			return new ArrayList<>();
		}
		List<InputDeviceProfile> res = new ArrayList<>();
		JSONArray jsonArray = json.getJSONArray("items");
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				res.add(new CustomInputDeviceProfile(jsonArray.getJSONObject(i)).initialize(app));
			} catch (JSONException e) {
				LOG.debug("Error while reading a custom device from JSON ", e);
			}
		}
		return res;
	}

	private static void writeToJson(@NonNull JSONObject json,
	                                @NonNull List<InputDeviceProfile> customDevices) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (InputDeviceProfile device : customDevices) {
			jsonArray.put(((CustomInputDeviceProfile) device).toJson());
		}
		json.put("items", jsonArray);
	}
}
