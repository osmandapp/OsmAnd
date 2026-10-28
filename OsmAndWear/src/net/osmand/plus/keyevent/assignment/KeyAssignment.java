package net.osmand.plus.keyevent.assignment;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.keyevent.CommandToActionConverter;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
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

	private final String id;
	private String commandId;
	private String customName;
	private QuickAction action;
	private List<Integer> keyCodes = new ArrayList<>();

	public KeyAssignment(@NonNull String commandId, @NonNull Integer... keyCodes) {
		this(CommandToActionConverter.createQuickAction(commandId), keyCodes);
		this.commandId = commandId;
	}

	public KeyAssignment(@Nullable QuickAction action, @NonNull Integer... keyCodes) {
		this.id = generateUniqueId();
		this.action = action;
		this.keyCodes = new ArrayList<>(Arrays.asList(keyCodes));
	}

	public KeyAssignment(@NonNull OsmandApplication app, @NonNull JSONObject jsonObject) throws JSONException {
		id = jsonObject.has("id") ? jsonObject.getString("id") : generateUniqueId();

		this.customName = jsonObject.has("customName")
				? jsonObject.getString("customName")
				: null;

		MapButtonsHelper mapButtonsHelper = app.getMapButtonsHelper();
		if (jsonObject.has("action")) {
			JSONArray actionJsonArray = jsonObject.getJSONArray("action");
			List<QuickAction> actions = mapButtonsHelper.parseActionsFromJson(actionJsonArray.toString());
			action = !Algorithms.isEmpty(actions) ? actions.get(0) : null;
		} else if (jsonObject.has("commandId")) {
			// For previous version compatibility
			this.commandId = commandId;
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
		this.commandId = original.commandId;
		this.customName = original.customName;
		this.keyCodes = original.keyCodes;
	}

	public void setAction(@NonNull QuickAction action) {
		this.action = action;
	}

	public void setKeyCodes(@NonNull List<Integer> keyCodes) {
		this.keyCodes = keyCodes;
	}

	public void removeKeyCode(int keyCode) {
		keyCodes = CollectionUtils.removeFromList(keyCodes, (Integer) keyCode);
	}

	@NonNull
	public String getId() {
		return id;
	}

	@Nullable
	public String getName(@NonNull OsmandApplication context) {
		return customName != null ? customName : getDefaultName(context);
	}

	@Nullable
	private String getDefaultName(@NonNull OsmandApplication context) {
		return action != null ? action.getExtendedName(context, true) : commandId;
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

	public boolean hasRequiredParameters() {
		return hasKeyCodes() && action != null;
	}

	@NonNull
	public List<Integer> getKeyCodes() {
		return keyCodes;
	}

	@DrawableRes
	public int getIconId(@NonNull Context context) {
		return action != null ? action.getIconRes(context) : R.drawable.ic_action_info_outlined;
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
	public JSONObject toJson(@NonNull OsmandApplication app) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		if (customName != null) {
			jsonObject.put("customName", customName);
		}
		if (action != null) {
			MapButtonsHelper mapButtonsHelper = app.getMapButtonsHelper();
			String actionJson = mapButtonsHelper.convertActionsToJson(Collections.singletonList(action));
			JSONArray actionJsonArray = new JSONArray(actionJson);
			jsonObject.put("action", actionJsonArray);
		}
		if (commandId != null) {
			// For previous version compatibility
			jsonObject.put("commandId", commandId);
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

	private static int idCounter = 0;

	private static String generateUniqueId() {
		return "key_assignment_" + System.currentTimeMillis() + "_" + ++idCounter;
	}
}
