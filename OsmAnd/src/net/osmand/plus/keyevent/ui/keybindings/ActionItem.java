package net.osmand.plus.keyevent.ui.keybindings;

import androidx.annotation.NonNull;

import net.osmand.plus.keyevent.KeySymbolMapper;
import net.osmand.plus.keyevent.commands.KeyEventCommand;

public class ActionItem {

	private final KeyEventCommand command;
	private final int keyCode;

	public ActionItem(int keyCode, @NonNull KeyEventCommand command) {
		this.command = command;
		this.keyCode = keyCode;
	}

	@NonNull
	public KeyEventCommand getCommand() {
		return command;
	}

	@NonNull
	public String getKeySymbol() {
		return KeySymbolMapper.getKeySymbol(getKeyCode());
	}

	public int getKeyCode() {
		return keyCode;
	}
}
