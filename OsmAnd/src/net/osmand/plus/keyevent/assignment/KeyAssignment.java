package net.osmand.plus.keyevent.assignment;

import static net.osmand.plus.keyevent.KeySymbolMapper.getKeySymbol;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.keyevent.KeyEventCommandsCache;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KeyAssignment {

	private final String commandId;
	private String customName;
	private List<Integer> keyCodes = new ArrayList<>();
	private KeyEventCommand cachedCommand;

	public KeyAssignment(@NonNull String commandId, @NonNull Integer ... keyCodes) {
		this.commandId = commandId;
		this.keyCodes = Arrays.asList(keyCodes);
	}

	public KeyAssignment(@NonNull JSONObject jsonObject) throws JSONException {
		this.commandId = jsonObject.getString("commandId");
		this.customName = jsonObject.has("customName")
				? jsonObject.getString("customName")
				: null;

		if (jsonObject.has("keycodes")) {
			JSONArray keyCodesJsonArray = jsonObject.getJSONArray("keycodes");
			for (int i = 0; i < keyCodesJsonArray.length(); i++) {
				JSONObject keyCodeJson = keyCodesJsonArray.getJSONObject(i);
				int keyCode = keyCodeJson.getInt("keycode");
				keyCodes.add(keyCode);
			}
		} else if (jsonObject.has("keycode")) {
			// For previous version compatibility
			int keyCode = jsonObject.getInt("keycode");
			if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
				keyCodes.add(keyCode);
			}
		}
	}

	public KeyAssignment(@NonNull KeyAssignment original) {
		this.commandId = original.commandId;
		this.customName = original.customName;
		this.keyCodes = original.keyCodes;
		this.cachedCommand = original.cachedCommand;
	}

	public void addKeyCode(int keyCode) {
		if (!keyCodes.contains(keyCode)) {
			keyCodes = CollectionUtils.addToList(keyCodes, keyCode);
		}
	}

	public void updateKeyCode(int oldKeyCode, int newKeyCode) {
		if (keyCodes.contains(oldKeyCode)) {
			int index = keyCodes.indexOf(oldKeyCode);
			keyCodes = CollectionUtils.setInList(keyCodes, index, newKeyCode);
		}
	}

	public void removeKeyCode(int keyCode) {
		keyCodes = CollectionUtils.removeFromList(keyCodes, (Integer) keyCode);
	}

	public void clearKeyCodes() {
		keyCodes = new ArrayList<>();
	}

	public boolean hasKeyCode(int keyCode) {
		for (int assignedKeyCode : getKeyCodes()) {
			if (assignedKeyCode == keyCode) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	public String getId() {
		return "default_" + commandId;
	}

	@Nullable
	public String getName(@NonNull OsmandApplication context) {
		return customName != null ? customName : getCommandTitle(context);
	}

	public void setCustomName(@Nullable String customName) {
		this.customName = customName;
	}

	@Nullable
	public KeyEventCommand getCommand(@NonNull OsmandApplication app) {
		if (cachedCommand == null) {
			cachedCommand = KeyEventCommandsCache.getCommand(app, commandId);
		}
		return cachedCommand;
	}

	@NonNull
	public String getCommandId() {
		return commandId;
	}

	@Nullable
	public String getCommandTitle(@NonNull OsmandApplication context) {
		KeyEventCommand command = getCommand(context);
		return command != null ? command.toHumanString(context) : null;
	}

	@NonNull
	public List<String> getKeyLabels(@NonNull Context context) {
		List<Integer> keyCodes = getKeyCodes();
		if (Algorithms.isEmpty(keyCodes)) {
			String none = context.getString(R.string.shared_string_none);
			return Collections.singletonList(none);
		}
		List<String> keyLabels = new ArrayList<>();
		for (int keyCode : getKeyCodes()) {
			keyLabels.add(getKeySymbol(context, keyCode));
		}
		return keyLabels;
	}

	public boolean hasKeyCodes() {
		return !Algorithms.isEmpty(getKeyCodes());
	}

	@NonNull
	public List<Integer> getKeyCodes() {
		return keyCodes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof KeyAssignment)) return false;
		KeyAssignment that = (KeyAssignment) o;
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@NonNull
	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("commandId", commandId);
		if (customName != null) {
			jsonObject.put("customName", customName);
		}
		if (!Algorithms.isEmpty(keyCodes)) {
			JSONArray keyCodesJsonArray = new JSONArray();
			for (Integer keyCode : keyCodes) {
				JSONObject keyCodeObject = new JSONObject();
				keyCodeObject.put("keycode", keyCode);
				keyCodesJsonArray.put(keyCodeObject);
			}
			jsonObject.put("keycodes", keyCodesJsonArray);
		}
		return jsonObject;
	}
}
