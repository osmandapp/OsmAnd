package net.osmand.plus.keyevent.keybinding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyEventCommandsCache;
import net.osmand.plus.keyevent.KeySymbolMapper;
import net.osmand.plus.keyevent.commands.KeyEventCommand;

import org.json.JSONException;
import org.json.JSONObject;

public class KeyBinding {

	private final int keyCode;
	private final String commandId;
	private final String customName;
	private KeyEventCommand cachedCommand;

	public KeyBinding(int keyCode, @NonNull KeyBinding keyBinding) {
		this(keyCode, keyBinding.commandId, keyBinding.customName);
	}

	public KeyBinding(@NonNull String customName, @NonNull KeyBinding keyBinding) {
		this(keyBinding.keyCode, keyBinding.commandId, customName);
	}

	public KeyBinding(int keyCode, @NonNull String commandId, @Nullable String customName) {
		this.keyCode = keyCode;
		this.commandId = commandId;
		this.customName = customName;
	}

	public KeyBinding(@NonNull JSONObject jsonObject) throws JSONException {
		this.keyCode = jsonObject.getInt("keycode");
		this.commandId = jsonObject.getString("commandId");
		this.customName = jsonObject.has("customName")
				? jsonObject.getString("customName")
				: null;
	}

	@Nullable
	public String getName(@NonNull OsmandApplication context) {
		return customName != null ? customName : getCommandTitle(context);
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
	public String getKeySymbol() {
		return KeySymbolMapper.getKeySymbol(getKeyCode());
	}

	public int getKeyCode() {
		return keyCode;
	}

	@Nullable
	public String toJsonString() {
		try {
			return toJson().toString();
		} catch (JSONException e) {
			return null;
		}
	}

	@NonNull
	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("keycode", keyCode);
		jsonObject.put("commandId", commandId);
		if (customName != null) {
			jsonObject.put("customName", customName);
		}
		return jsonObject;
	}
}
