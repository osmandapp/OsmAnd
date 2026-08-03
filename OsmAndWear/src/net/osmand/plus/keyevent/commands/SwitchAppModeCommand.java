package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

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

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(moveForward
				? R.string.key_event_action_next_app_profile
				: R.string.key_event_action_previous_app_profile);
	}
}
