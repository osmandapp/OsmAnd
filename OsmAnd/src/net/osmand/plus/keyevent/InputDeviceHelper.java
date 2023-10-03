package net.osmand.plus.keyevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile;
import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.keyevent.devices.ParrotDeviceProfile;
import net.osmand.plus.keyevent.devices.WunderLINQDeviceProfile;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputDeviceHelper {

	public static final InputDeviceProfile KEYBOARD = new KeyboardDeviceProfile();
	public static final InputDeviceProfile PARROT = new ParrotDeviceProfile();
	public static final InputDeviceProfile WUNDER_LINQ = new WunderLINQDeviceProfile();

	private static final String CUSTOM_PREFIX = "custom_";
	private static final Log LOG = PlatformUtil.getLog(InputDeviceHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	/**
	 * Use the same Commands factory to speed up new commands creation
	 */
	private final KeyEventCommandsFactory commandsFactory = new KeyEventCommandsFactory();
	private final List<InputDeviceProfile> defaultDevices = Arrays.asList(KEYBOARD, PARROT, WUNDER_LINQ);
	private final List<CustomInputDeviceProfile> customDevices;
	private final Map<String, InputDeviceProfile> cachedDevices = new HashMap<>();

	public InputDeviceHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		customDevices = loadCustomDevices();
		for (InputDeviceProfile device : getAllDevices()) {
			device.initialize(app, commandsFactory);
			cachedDevices.put(device.getId(), device);
		}
	}

	public List<InputDeviceProfile> getAllDevices() {
		List<InputDeviceProfile> result = new ArrayList<>();
		result.addAll(defaultDevices);
		result.addAll(customDevices);
		return result;
	}

	@NonNull
	public KeyEventCommandsFactory getCommandsFactory() {
		return commandsFactory;
	}

	public CustomInputDeviceProfile makeCustomDeviceDuplicate(@NonNull InputDeviceProfile device) {
		String prevName = device.toHumanString(app);
		String uniqueName = prevName;
		int index = 0;
		while (hasNameDuplicate(uniqueName)) {
			uniqueName = prevName + " " + ++index;
		}
		return makeCustomDevice(uniqueName, device);
	}

	@NonNull
	public CustomInputDeviceProfile makeCustomDevice(@NonNull String newName) {
		return makeCustomDevice(newName, KEYBOARD);
	}

	@NonNull
	public CustomInputDeviceProfile makeCustomDevice(
			@NonNull String newName, @NonNull InputDeviceProfile baseDevice
	) {
		String uniqueId = CUSTOM_PREFIX + System.currentTimeMillis();
		return makeCustomDevice(uniqueId, newName, baseDevice);
	}

	@NonNull
	private CustomInputDeviceProfile makeCustomDevice(
			@NonNull String id, @NonNull String name, @NonNull InputDeviceProfile baseDevice
	) {
		CustomInputDeviceProfile customDevice = new CustomInputDeviceProfile(id, name, baseDevice);
		customDevice.initialize(app, commandsFactory);
		return customDevice;
	}

	public void saveCustomDevice(@NonNull CustomInputDeviceProfile device) {
		customDevices.add(device);
		cachedDevices.put(device.getId(), device);
		syncSettings();
	}

	public void removeCustomDevice(@NonNull CustomInputDeviceProfile device) {
		customDevices.remove(device);
		cachedDevices.remove(device.getId());
		syncSettings();
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
		String id = settings.EXTERNAL_INPUT_DEVICE.getModeValue(appMode);
		return id != null ? cachedDevices.get(id) : null;
	}

	public boolean hasNameDuplicate(@NonNull String newName) {
		for (InputDeviceProfile device : getAllDevices()) {
			if (Algorithms.objectEquals(device.toHumanString(app), newName)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private List<CustomInputDeviceProfile> loadCustomDevices() {
		String json = settings.CUSTOM_EXTERNAL_INPUT_DEVICES.get();
		try {
			return readFromJson(new JSONObject(json));
		} catch (JSONException e) {
			LOG.debug("Error when reading custom devices from JSON ", e);
		}
		return Collections.emptyList();
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

	public static List<CustomInputDeviceProfile> readFromJson(@NonNull JSONObject json) throws JSONException {
		if (!json.has("items")) {
			return Collections.emptyList();
		}
		List<CustomInputDeviceProfile> res = new ArrayList<>();
		JSONArray jsonArray = json.getJSONArray("items");
		for (int i = 0; i < jsonArray.length(); i++) {
			res.add(new CustomInputDeviceProfile(jsonArray.getJSONObject(i)));
		}
		return res;
	}

	public static void writeToJson(@NonNull JSONObject json,
	                               @NonNull List<CustomInputDeviceProfile> customDevices) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (CustomInputDeviceProfile device : customDevices) {
			jsonArray.put(device.toJson());
		}
		json.put("items", jsonArray);
	}
}
