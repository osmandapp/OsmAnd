package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyEventCategory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class InputDeviceProfile {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected List<KeyBinding> keyBindings = new ArrayList<>();
	protected Map<Integer, KeyBinding> keyBindingsCache = new HashMap<>();

	public void initialize(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public List<KeyBinding> getKeyBindingsForCategory(@NonNull KeyEventCategory category) {
		List<KeyBinding> result = new ArrayList<>();
		for (KeyBinding keyBinding : getKeyBindings()) {
			KeyEventCommand command = keyBinding.getCommand(app);
			if (command != null && command.getCategory() == category) {
				result.add(keyBinding);
			}
		}
		return result;
	}

	public List<KeyBinding> getKeyBindings() {
		return keyBindings;
	}

	protected void setKeyBindings(@NonNull List<KeyBinding> keyBindings) {
		this.keyBindings = keyBindings;
		syncKeyBindingsCache();
	}

	public void removeKeyBinding(int keyCode) {
		KeyBinding keyBinding = keyBindingsCache.get(keyCode);
		keyBindings = Algorithms.removeFromList(keyBindings, keyBinding);
		syncKeyBindingsCache();
	}

	public void addKeyBinding(@NonNull KeyBinding keyBinding) {
		keyBindings = Algorithms.addToList(keyBindings, keyBinding);
		syncKeyBindingsCache();
	}

	public void updateKeyBinding(int originalKeyCode, @NonNull KeyBinding newKeyBinding) {
		KeyBinding oldKeyBinding = keyBindingsCache.remove(originalKeyCode);
		if (oldKeyBinding != null) {
			int index = keyBindings.indexOf(oldKeyBinding);
			keyBindings = Algorithms.setInList(keyBindings, index, newKeyBinding);
		} else {
			addKeyBinding(newKeyBinding);
		}
		syncKeyBindingsCache();
	}

	protected void syncKeyBindingsCache() {
		Map<Integer, KeyBinding> newQuickCache = new HashMap<>();
		for (KeyBinding keyBinding : keyBindings) {
			newQuickCache.put(keyBinding.getKeyCode(), keyBinding);
		}
		this.keyBindingsCache = newQuickCache;
	}

	public void requestBindCommand(int keyCode, @NonNull String commandId) {}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		KeyBinding keyBinding = keyBindingsCache.get(keyCode);
		return keyBinding != null ? keyBinding.getCommand(app) : null;
	}

	public int getCommandsCount() {
		return keyBindings.size();
	}

	@NonNull
	public abstract String getId();

	@NonNull
	public abstract String toHumanString(@NonNull Context context);

	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}
