package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

public class ActivityBackPressedCommand extends KeyEventCommand {

	public static final String ID = "activity_back_pressed";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().onBackPressed();
		return true;
	}
}
