package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

public class SwitchCompassCommand extends KeyEventCommand {

	public static final String ID = "switch_compass_forward";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().getMapViewTrackingUtilities().requestSwitchCompassToNextMode();
		return true;
	}
}
