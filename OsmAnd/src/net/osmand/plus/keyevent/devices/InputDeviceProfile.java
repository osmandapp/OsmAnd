package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.util.ArrayMap;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyEventCommandsFactory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Objects;

public abstract class InputDeviceProfile {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	private KeyEventCommandsFactory commandsFactory;

	protected ArrayMap<Integer, KeyEventCommand> mappedCommands = new ArrayMap<>();

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

	/**
	 * @return a new copy of mapped commands
	 */
	public ArrayMap<Integer, KeyEventCommand> getMappedCommands() {
		return new ArrayMap<>(mappedCommands);
	}

	/**
	 * Uses to add, remove or update command bindings in cache.
	 * @param oldKeyCode a key code with which command was bound before.
	 * @param newKeyCode a new key code with which command should be bound to.
	 * @param commandId an id of command for which key code changed.
	 */
	public void updateMappedCommands(int oldKeyCode, int newKeyCode, @NonNull String commandId) {
		if (newKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
			removeKeyBinding(oldKeyCode);
		} else if (oldKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
			addKeyBinding(newKeyCode, commandId);
		} else {
			updateKeyBinding(oldKeyCode, newKeyCode, commandId);
		}
	}

	private void removeKeyBinding(int keyCode) {
		ArrayMap<Integer, KeyEventCommand> newMappedCommands = new ArrayMap<>(mappedCommands);
		newMappedCommands.remove(keyCode);
		mappedCommands = newMappedCommands;
	}

	private void addKeyBinding(int keyCode, @NonNull String commandId) {
		ArrayMap<Integer, KeyEventCommand> newMappedCommands = new ArrayMap<>(mappedCommands);
		KeyEventCommand command = commandsFactory.getOrCreateCommand(commandId);
		if (command != null) {
			newMappedCommands.put(keyCode, command);
		}
		mappedCommands = newMappedCommands;
	}

	private void updateKeyBinding(int oldKeyCode, int newKeyCode, @NonNull String commandId) {
		ArrayMap<Integer, KeyEventCommand> newCommands = new ArrayMap<>();
		ArrayMap<Integer, KeyEventCommand> oldCommands = new ArrayMap<>(mappedCommands);

		for (int i = 0; i < oldCommands.size(); i++) {
			int keyCode = oldCommands.keyAt(i);
			KeyEventCommand oldCommand = oldCommands.valueAt(i);
			if (keyCode == oldKeyCode) {
				// Check and update command if needed
				KeyEventCommand newCommand = Objects.equals(oldCommand.getId(), commandId)
						? oldCommand
						: commandsFactory.getOrCreateCommand(commandId);
				if (newCommand != null) {
					newCommands.put(newKeyCode, oldCommand);
					continue;
				}
			}
			newCommands.put(keyCode, oldCommand);
		}
		mappedCommands = newCommands;
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

	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}
