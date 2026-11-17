package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class ToggleDrawerCommand extends KeyEventCommand {

	public static final String ID = "toggle_drawer";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// event.getRepeatCount() repeat count 0 doesn't work for samsung, 1 doesn't work for lg
		requireMapActivity().toggleDrawer();
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_toggle_drower);
	}
}
