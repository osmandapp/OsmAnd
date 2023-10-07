package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.util.ArrayMap;

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

	protected final ArrayMap<Integer, KeyEventCommand> mappedCommands = new ArrayMap<>();

	public void initialize(@NonNull OsmandApplication app,
	                       @NonNull KeyEventCommandsFactory commandsFactory) {
		this.app = app;
		this.settings = app.getSettings();
		this.commandsFactory = commandsFactory;
		collectCommands();
	}

	/**
	 * Override this method to add or update bindings between
	 * keycodes and commands for a specific input device profile.
	 */
	protected abstract void collectCommands();

	public ArrayMap<Integer, KeyEventCommand> getMappedCommands() {
		return mappedCommands;
	}

	public void requestBindCommand(int keyCode, @NonNull String commandId) {
		if (!mappedCommands.containsKey(keyCode)) {
			bindCommand(keyCode, commandId);
		}
	}

	protected void bindCommand(int keyCode, @NonNull String commandId) {
		KeyEventCommand command = commandsFactory.getOrCreateCommand(commandId);
		if (command != null) {
			command.initialize(app, commandId);
			mappedCommands.put(keyCode, command);
		}
	}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		return mappedCommands.get(keyCode);
	}

	public int getCommandsCount() {
		return mappedCommands.size();
	}

	@NonNull
	public abstract String getId();

	@NonNull
	public abstract String toHumanString(@NonNull Context context);

}
