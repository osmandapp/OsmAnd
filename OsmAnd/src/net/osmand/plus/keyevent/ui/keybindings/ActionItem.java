package net.osmand.plus.keyevent.ui.keybindings;

import androidx.annotation.NonNull;

import net.osmand.plus.keyevent.commands.KeyEventCommand;

public class ActionItem {

	private final KeyEventCommand command;
	private final String keySymbol;

	public ActionItem(@NonNull KeyEventCommand command, @NonNull String keySymbol) {
		this.command = command;
		this.keySymbol = keySymbol;
	}

	@NonNull
	public KeyEventCommand getCommand() {
		return command;
	}

	@NonNull
	public String getKeySymbol() {
		return keySymbol;
	}
}
