package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.keyevent.commands.KeyEventCommand;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

public class CustomInputDeviceProfile extends InputDeviceProfile {

	private final String customId;
	private String customName;

	/**
	 * Cached mapped command ids.
	 * Uses to store commands in preferences.
	 * Uses ArrayMap collection to save original order of elements.
	 */
	private final ArrayMap<Integer, String> mappedCommandIds = new ArrayMap<>();

	public CustomInputDeviceProfile(@NonNull String customId, @NonNull String customName,
	                                @NonNull InputDeviceProfile baseDevice) {
		this.customId = customId;
		this.customName = customName;
		initCommandIds(baseDevice.getMappedCommands());
	}

	public CustomInputDeviceProfile(@NonNull JSONObject object) throws JSONException {
		Gson gson = new Gson();
		Type type = new TypeToken<ArrayMap<Integer, String>>() {}.getType();
		customId = object.getString("id");
		customName = object.getString("name");
		mappedCommandIds.putAll(gson.fromJson(object.getString("commands"), type));
	}

	private void initCommandIds(@NonNull ArrayMap<Integer, KeyEventCommand> mappedCommands) {
		for (int i = 0; i < mappedCommands.size(); i++) {
			Integer keyCode = mappedCommands.keyAt(i);
			KeyEventCommand command = mappedCommands.valueAt(i);
			if (command != null) {
				mappedCommandIds.put(keyCode, command.getId());
			}
		}
	}

	@Override
	protected void collectCommands() {
		for (int i = 0; i < mappedCommandIds.size(); i++) {
			Integer keyCode = mappedCommandIds.keyAt(i);
			String commandId = mappedCommandIds.valueAt(i);
			bindCommand(keyCode, commandId);
		}
	}

	public void setCustomName(@NonNull String customName) {
		this.customName = customName;
	}

	@NonNull
	@Override
	public String getId() {
		return customId;
	}

	@NonNull
	public JSONObject toJson() throws JSONException {
		Gson gson = new Gson();
		Type type = new TypeToken<ArrayMap<Integer, String>>() {}.getType();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", customId);
		jsonObject.put("name", customName);
		jsonObject.put("commands", gson.toJson(mappedCommandIds, type));
		return jsonObject;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return customName;
	}
}
