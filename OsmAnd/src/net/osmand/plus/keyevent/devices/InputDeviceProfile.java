package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.view.KeyEvent;

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
	protected Map<Integer, KeyBinding> quickCache = new HashMap<>();

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
		syncQuickCache();
	}

	public void resetAllAssignments() {
		List<KeyBinding> newKeyBindings = new ArrayList<>();
		for (KeyBinding oldKeyBinding : getKeyBindings()) {
			KeyBinding newKeyBinding = new KeyBinding(KeyEvent.KEYCODE_UNKNOWN, oldKeyBinding);
			newKeyBindings.add(newKeyBinding);
		}
		this.keyBindings = newKeyBindings;
		this.quickCache = new HashMap<>();
	}

	public void updateKeyBinding(@Nullable KeyBinding oldKeyBinding, @NonNull KeyBinding newKeyBinding) {
		if (oldKeyBinding != null) {
			int index = keyBindings.indexOf(oldKeyBinding);
			keyBindings = Algorithms.setInList(keyBindings, index, newKeyBinding);
		} else {
			keyBindings = Algorithms.addToList(keyBindings, newKeyBinding);
		}
		syncQuickCache();
	}

	protected void syncQuickCache() {
		Map<Integer, KeyBinding> newQuickCache = new HashMap<>();
		for (KeyBinding keyBinding : keyBindings) {
			int keyCode = keyBinding.getKeyCode();
			if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
				newQuickCache.put(keyCode, keyBinding);
			}
		}
		this.quickCache = newQuickCache;
	}

	public void requestBindCommand(int keyCode, @NonNull String commandId) {}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		KeyBinding keyBinding = findAssignment(keyCode);
		return keyBinding != null ? keyBinding.getCommand(app) : null;
	}

	@Nullable
	public KeyBinding findAssignment(int keyCode) {
		return quickCache.get(keyCode);
	}

	public int getAssignmentsCount() {
		return quickCache.size();
	}

	public int getActionsCount() {
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
