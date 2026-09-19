package net.osmand.plus.keyevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.commands.KeyEventCommand;

import java.util.HashMap;
import java.util.Map;

public class KeyEventCommandsCache {

	private static final Map<String, KeyEventCommand> cachedCommands = new HashMap<>();
	private static final KeyEventCommandsFactory keyEventCommandsFactory = new KeyEventCommandsFactory();

	@Nullable
	public static KeyEventCommand getCommand(@NonNull OsmandApplication app, @NonNull String commandId) {
		KeyEventCommand command = getOrCreateCommand(commandId);
		if (command != null) {
			command.initialize(app, commandId);
		}
		return command;
	}

	@Nullable
	private static KeyEventCommand getOrCreateCommand(@NonNull String commandId) {
		KeyEventCommand command = cachedCommands.get(commandId);
		if (command == null) {
			// Use cache to speed up creation of new commands
			command = keyEventCommandsFactory.createCommand(commandId);
			cachedCommands.put(commandId, command);
		}
		return command;
	}

}
