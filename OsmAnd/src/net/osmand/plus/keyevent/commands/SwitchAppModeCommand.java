package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

public class SwitchAppModeCommand extends KeyEventCommand {

	public static final String SWITCH_TO_NEXT_ID = "switch_app_mode_forward";
	public static final String SWITCH_TO_PREVIOUS_ID = "switch_app_mode_backward";

	private final boolean moveForward;

	public SwitchAppModeCommand(boolean moveForward) {
		this.moveForward = moveForward;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (moveForward) {
			settings.switchAppModeToNext();
		} else {
			settings.switchAppModeToPrevious();
		}
		return true;
	}
}
