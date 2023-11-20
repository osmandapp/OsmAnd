package net.osmand.plus.keyevent.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.keybinding.KeyBinding;

public abstract class PredefinedInputDeviceProfile extends InputDeviceProfile {

	@Override
	public void initialize(@NonNull OsmandApplication app) {
		super.initialize(app);
		keyBindings.clear();
		collectKeyBindings();
		syncActiveKeyBindings();
	}

	/**
	 * Override this method to add or update bindings between
	 * keycodes and commands for a specific predefined input device profile.
	 */
	protected abstract void collectKeyBindings();

	public void requestBindCommand(int keyCode, @NonNull String commandId) {
		if (!activeKeyBindings.containsKey(keyCode)) {
			bindCommand(keyCode, commandId);
		}
	}

	public void bindCommand(int keyCode, @NonNull String commandId) {
		KeyBinding keyBinding = new KeyBinding(keyCode, commandId, null);
		keyBindings.add(keyBinding);
		activeKeyBindings.put(keyCode, keyBinding);
	}
}
