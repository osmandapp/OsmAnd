package net.osmand.plus.keyevent.keybinding;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.keyevent.KeySymbolMapper;
import net.osmand.plus.keyevent.commands.KeyEventCommand;

public class KeyBinding {

	private final int keyCode;
	private final KeyEventCommand command;

	public KeyBinding(int keyCode, @NonNull KeyEventCommand command) {
		this.command = command;
		this.keyCode = keyCode;
	}

	@NonNull
	public KeyEventCommand getCommand() {
		return command;
	}

	@NonNull
	public String getCommandId() {
		return getCommand().getId();
	}

	@NonNull
	public String getCommandTitle(@NonNull Context context) {
		return getCommand().toHumanString(context);
	}

	@NonNull
	public String getKeySymbol() {
		return KeySymbolMapper.getKeySymbol(getKeyCode());
	}

	public int getKeyCode() {
		return keyCode;
	}
}
