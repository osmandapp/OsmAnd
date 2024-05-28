package net.osmand.plus.keyevent.assignment;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.CommandToActionConverter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionSerializer;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class KeyAssignment {

	private final String id;
	private String customName;
	private QuickAction action;
	private List<Integer> keyCodes = new ArrayList<>();

	private final QuickActionSerializer serializer = new QuickActionSerializer();
	private final Gson gson = new GsonBuilder().registerTypeAdapter(QuickAction.class, serializer).create();

	public KeyAssignment(@NonNull String commandId, @NonNull Integer ... keyCodes) {
		this.id = generateUniqueId();
		this.action = CommandToActionConverter.createQuickAction(commandId);
		this.keyCodes = new ArrayList<>(Arrays.asList(keyCodes));
	}

	public KeyAssignment(@NonNull JSONObject jsonObject) throws JSONException {
		id = jsonObject.has("id") ? jsonObject.getString("id") : generateUniqueId();

		this.customName = jsonObject.has("customName")
				? jsonObject.getString("customName")
				: null;

		if (jsonObject.has("action")) {
			String actionJson = jsonObject.getString("action");
			Type type = new TypeToken<List<QuickAction>>() {}.getType();
			action = gson.fromJson(actionJson, type);
		} else if (jsonObject.has("commandId")) {
			// For previous version compatibility
			String commandId = jsonObject.getString("commandId");
			action = CommandToActionConverter.createQuickAction(commandId);
		}

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
		this.id = original.id;
		this.action = original.action;
		this.customName = original.customName;
		this.keyCodes = original.keyCodes;
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
		return id;
	}

	@Nullable
	public String getName(@NonNull OsmandApplication context) {
		return customName != null ? customName : action.getName(context);
	}

	public void setCustomName(@Nullable String customName) {
		this.customName = customName;
	}

	@Nullable
	public QuickAction getAction() {
		return action;
	}

	public boolean hasKeyCodes() {
		return !Algorithms.isEmpty(getKeyCodes());
	}

	@NonNull
	public List<Integer> getKeyCodes() {
		return keyCodes;
	}

	@DrawableRes
	public int getIconId(@NonNull Context context) {
		return action.getIconRes(context);
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
		jsonObject.put("id", id);
		if (customName != null) {
			jsonObject.put("customName", customName);
		}
		Type type = new TypeToken<List<QuickAction>>() {}.getType();
		jsonObject.put("action", gson.toJson(action, type));

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

	private static String generateUniqueId() {
		return "key_assignment_" + System.currentTimeMillis();
	}
}
