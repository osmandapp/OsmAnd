package net.osmand.plus.keyevent;

import static net.osmand.plus.keyevent.devices.DefaultInputDevices.KEYBOARD;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.devices.DefaultInputDevices;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.quickaction.QuickAction;
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

public class InputDevicesHelper {

	private static final int FUNCTIONALITY_PURPOSE_ID = 0;
	private static final int CUSTOMIZATION_PURPOSE_ID = 1;

	private static final Log LOG = PlatformUtil.getLog(InputDevicesHelper.class);
	private static final String CUSTOM_DEVICE_PREFIX = "custom_";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final List<InputDevicesEventListener> listeners = new ArrayList<>();
	private final Map<Integer, InputDevicesCollection> cachedDevicesCollections = new HashMap<>();

	public InputDevicesHelper(@NonNull OsmandApplication app) {
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

	public void createAndSaveCustomDevice(@NonNull ApplicationMode appMode,
	                                      @NonNull String newDeviceName) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		saveCustomDevice(devicesCollection, makeCustomDevice(newDeviceName));
	}

	public void createAndSaveDeviceDuplicate(@NonNull ApplicationMode appMode,
	                                         @NonNull InputDeviceProfile device) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		saveCustomDevice(devicesCollection, makeCustomDeviceDuplicate(devicesCollection, device));
	}

	public void renameCustomDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                               @NonNull String newName) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
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
	private InputDeviceProfile makeCustomDevice(@NonNull String newDeviceId, @NonNull String name,
	                                            @NonNull InputDeviceProfile parentDevice) {
		return new CustomInputDeviceProfile(newDeviceId, name, parentDevice).initialize(app);
	}

	private void saveCustomDevice(@NonNull InputDevicesCollection devicesCollection,
	                              @NonNull InputDeviceProfile device) {
		devicesCollection.addCustomDevice(device);
		syncSettings(devicesCollection, EventType.ADD_NEW_DEVICE);
	}

	public void removeCustomDevice(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		devicesCollection.removeCustomDevice(deviceId);
		syncSettings(devicesCollection, EventType.DELETE_DEVICE);
	}

	public boolean hasDeviceNameDuplicate(@NonNull Context context,
	                                      @NonNull ApplicationMode appMode,
	                                      @NonNull String newName) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		return devicesCollection.hasDeviceNameDuplicate(context, newName);
	}

	@NonNull
	public List<InputDeviceProfile> getAllDevices(@NonNull ApplicationMode appMode) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		return devicesCollection.getAllDevices();
	}

	@Nullable
	public InputDeviceProfile getFunctionalityDevice(@NonNull ApplicationMode appMode) {
		return getEnabledSelectedDevice(FUNCTIONALITY_PURPOSE_ID, appMode);
	}

	@Nullable
	public InputDeviceProfile getCustomizationDevice(@NonNull ApplicationMode appMode) {
		return getEnabledSelectedDevice(CUSTOMIZATION_PURPOSE_ID, appMode);
	}

	@Nullable
	private InputDeviceProfile getEnabledSelectedDevice(int cacheId, @NonNull ApplicationMode appMode) {
		if (settings.EXTERNAL_INPUT_DEVICE_ENABLED.getModeValue(appMode)) {
			return getSelectedDevice(cacheId, appMode);
		}
		return null;
	}

	@NonNull
	public InputDeviceProfile getSelectedDevice(@NonNull ApplicationMode appMode) {
		return getSelectedDevice(CUSTOMIZATION_PURPOSE_ID, appMode);
	}

	@NonNull
	private InputDeviceProfile getSelectedDevice(int cacheId, @NonNull ApplicationMode appMode) {
		String id = getSelectedDeviceId(appMode);
		InputDeviceProfile device = id != null ? getDeviceById(cacheId, appMode, id) : null;
		return device != null ? device : KEYBOARD;
	}

	@Nullable
	private String getSelectedDeviceId(@NonNull ApplicationMode appMode) {
		return settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode);
	}

	@Nullable
	public InputDeviceProfile getDeviceById(@NonNull ApplicationMode appMode,
	                                        @NonNull String deviceId) {
		return getDeviceById(CUSTOMIZATION_PURPOSE_ID, appMode, deviceId);
	}

	@Nullable
	private InputDeviceProfile getDeviceById(int cacheId, @NonNull ApplicationMode appMode,
	                                         @NonNull String deviceId) {
		return getInputDevicesCollection(cacheId, appMode).getDeviceById(deviceId);
	}

	public void renameAssignment(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                             @NonNull String assignmentId, @NonNull String newName) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		CustomInputDeviceProfile device = devicesCollection.getCustomDeviceById(deviceId);
		if (device != null) {
			device.renameAssignment(assignmentId, newName);
			syncSettings(devicesCollection, EventType.RENAME_ASSIGNMENT);
		}
	}

	public void addAssignment(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                          @NonNull KeyAssignment assignment) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		CustomInputDeviceProfile device = devicesCollection.getCustomDeviceById(deviceId);
		if (device != null) {
			device.addAssignment(assignment);
			syncSettings(devicesCollection, EventType.ADD_ASSIGNMENT);
		}
	}

	public void updateAssignment(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                             @NonNull String assignmentId, @NonNull QuickAction action,
	                             @NonNull List<Integer> keyCodes) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		CustomInputDeviceProfile device = devicesCollection.getCustomDeviceById(deviceId);
		if (device != null) {
			device.updateAssignment(assignmentId, action, keyCodes);
			syncSettings(devicesCollection, EventType.UPDATE_ASSIGNMENT);
		}
	}

	public void removeKeyAssignmentCompletely(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                                          @NonNull String assignmentId) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		CustomInputDeviceProfile device = devicesCollection.getCustomDeviceById(deviceId);
		if (device != null) {
			device.removeKeyAssignmentCompletely(assignmentId);
			syncSettings(devicesCollection, EventType.REMOVE_KEY_ASSIGNMENT_COMPLETELY);
		}
	}

	public void saveUpdatedAssignmentsList(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                                       @NonNull List<KeyAssignment> assignments) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		CustomInputDeviceProfile device = devicesCollection.getCustomDeviceById(deviceId);
		if (device != null) {
			device.saveUpdatedAssignmentsList(assignments);
			syncSettings(devicesCollection, EventType.SAVE_UPDATED_ASSIGNMENTS_LIST);
		}
	}

	public void clearAllAssignments(@NonNull ApplicationMode appMode, @NonNull String deviceId) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		CustomInputDeviceProfile device = devicesCollection.getCustomDeviceById(deviceId);
		if (device != null) {
			device.clearAllAssignments();
			syncSettings(devicesCollection, EventType.CLEAR_ALL_ASSIGNMENTS);
		}
	}

	@Nullable
	public KeyAssignment findAssignment(@NonNull ApplicationMode appMode, @NonNull String deviceId,
	                                    @NonNull String assignmentId) {
		InputDeviceProfile device = getDeviceById(CUSTOMIZATION_PURPOSE_ID, appMode, deviceId);
		return device != null ? device.findAssignment(assignmentId) : null;
	}

	public boolean hasAssignmentNameDuplicate(@NonNull OsmandApplication context,
	                                          @NonNull ApplicationMode appMode,
	                                          @NonNull String deviceId, @NonNull String newName) {
		InputDevicesCollection devicesCollection = getCustomizationCollection(appMode);
		InputDeviceProfile device = devicesCollection.getDeviceById(deviceId);
		return device != null && device.hasAssignmentNameDuplicate(context, newName);
	}

	@Nullable
	public ApplicationMode getFunctionalityAppMode() {
		InputDevicesCollection collection = cachedDevicesCollections.get(FUNCTIONALITY_PURPOSE_ID);
		return collection != null ? collection.getAppMode() : null;
	}

	@NonNull
	private InputDevicesCollection getCustomizationCollection(@NonNull ApplicationMode appMode) {
		return getInputDevicesCollection(CUSTOMIZATION_PURPOSE_ID, appMode);
	}

	@NonNull
	private InputDevicesCollection getInputDevicesCollection(int cacheId, @NonNull ApplicationMode appMode) {
		InputDevicesCollection collection = cachedDevicesCollections.get(cacheId);
		if (collection == null || !Objects.equals(appMode, collection.getAppMode())) {
			collection = reloadInputDevicesCollection(cacheId, appMode);
		}
		return collection;
	}

	public void reloadFunctionalityCollection(@NonNull ApplicationMode appMode) {
		reloadInputDevicesCollection(FUNCTIONALITY_PURPOSE_ID, appMode);
	}

	@NonNull
	private InputDevicesCollection reloadInputDevicesCollection(int cacheId, @NonNull ApplicationMode appMode) {
		InputDevicesCollection collection = new InputDevicesCollection(appMode, loadCustomDevices(appMode));
		cachedDevicesCollections.put(cacheId, collection);
		return collection;
	}

	public void releaseCustomizationCollection() {
		cachedDevicesCollections.remove(CUSTOMIZATION_PURPOSE_ID);
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
			writeToJson(app, json, devicesCollection.getCustomDevices());
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
				res.add(new CustomInputDeviceProfile(app, jsonArray.getJSONObject(i)).initialize(app));
			} catch (JSONException e) {
				LOG.debug("Error while reading a custom device from JSON ", e);
			}
		}
		return res;
	}

	private static void writeToJson(@NonNull OsmandApplication app, @NonNull JSONObject json,
	                                @NonNull List<InputDeviceProfile> customDevices) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (InputDeviceProfile device : customDevices) {
			jsonArray.put(((CustomInputDeviceProfile) device).toJson(app));
		}
		json.put("items", jsonArray);
	}
}
