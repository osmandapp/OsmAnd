package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.keyevent.keybinding.KeyBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CustomInputDeviceProfile extends InputDeviceProfile {

	private final String customId;
	private String customName;

	public CustomInputDeviceProfile(@NonNull String customId, @NonNull String customName,
	                                @NonNull InputDeviceProfile parentDevice) {
		this.customId = customId;
		this.customName = customName;
		setKeyBindings(parentDevice.getKeyBindings());
	}

	public CustomInputDeviceProfile(@NonNull JSONObject object) throws JSONException {
		customId = object.getString("id");
		customName = object.getString("name");
		JSONArray jsonArray = object.getJSONArray("keybindings");
		List<KeyBinding> keyBindings = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			keyBindings.add(new KeyBinding(jsonObject));
		}
		setKeyBindings(keyBindings);
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
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", customId);
		jsonObject.put("name", customName);
		JSONArray jsonArray = new JSONArray();
		for (KeyBinding keyBinding : getKeyBindings()) {
			jsonArray.put(keyBinding.toJson());
		}
		jsonObject.put("keybindings", jsonArray);
		return jsonObject;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return customName;
	}
}
