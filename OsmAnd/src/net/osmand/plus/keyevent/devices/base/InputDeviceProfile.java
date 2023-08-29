package net.osmand.plus.keyevent.devices.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyEventCommandsFactory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.HashMap;
import java.util.Map;

public abstract class InputDeviceProfile {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	private KeyEventCommandsFactory commandsFactory;

	private final Map<Integer, KeyEventCommand> mappedCommands = new HashMap<>();

	public void initialize(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.commandsFactory = app.getKeyEventHelper().getCommandsFactory();
		collectCommands();
	}

	/**
	 * Override this method to add or update bindings between
	 * keycodes and commands for a specific input device profile.
	 */
	protected abstract void collectCommands();

	protected void updateCommands() {
		clearCommands();
		collectCommands();
	}

	protected void clearCommands() {
		mappedCommands.clear();
	}

	protected void bindCommand(int keyCode, @NonNull String commandId) {
		KeyEventCommand command = commandsFactory.getOrCreateCommand(commandId);
		if (command != null) {
			command.initialize(app);
			mappedCommands.put(keyCode, command);
		}
	}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		return mappedCommands.get(keyCode);
	}

	public abstract String getId();
}
