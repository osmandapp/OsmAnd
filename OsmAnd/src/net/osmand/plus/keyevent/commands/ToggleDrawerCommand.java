package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

public class ToggleDrawerCommand extends KeyEventCommand {

	public static final String ID = "toggle_drawer";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// event.getRepeatCount() repeat count 0 doesn't work for samsung, 1 doesn't work for lg
		requireMapActivity().toggleDrawer();
		return true;
	}
}
