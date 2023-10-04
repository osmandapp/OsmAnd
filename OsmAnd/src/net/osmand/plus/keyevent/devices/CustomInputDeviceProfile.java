package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.keyevent.commands.KeyEventCommand;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CustomInputDeviceProfile extends InputDeviceProfile {

	private final String customId;
	private String customName;

	private final Map<Integer, String> mappedCommandIds = new HashMap<>();

	public CustomInputDeviceProfile(@NonNull String customId, @NonNull String customName,
	                                @NonNull InputDeviceProfile baseDevice) {
		this.customId = customId;
		this.customName = customName;
		initCommandIds(baseDevice.getMappedCommands());
	}

	public CustomInputDeviceProfile(@NonNull JSONObject object) throws JSONException {
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<Integer, String>>() {}.getType();
		customId = object.getString("id");
		customName = object.getString("name");
		mappedCommandIds.putAll(gson.fromJson(object.getString("commands"), type));
	}

	private void initCommandIds(@NonNull Map<Integer, KeyEventCommand> mappedCommands) {
		for (Integer keyCode : mappedCommands.keySet()) {
			KeyEventCommand command = mappedCommands.get(keyCode);
			if (command != null) {
				mappedCommandIds.put(keyCode, command.getId());
			}
		}
	}

	@Override
	protected void collectCommands() {
		for (Entry<Integer, String> entry : mappedCommandIds.entrySet()) {
			bindCommand(entry.getKey(), entry.getValue());
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
		Type type = new TypeToken<HashMap<Integer, String>>() {}.getType();
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
