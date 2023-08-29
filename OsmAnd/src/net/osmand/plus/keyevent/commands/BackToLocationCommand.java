package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

public class BackToLocationCommand extends KeyEventCommand {

	public static final String ID = "back_to_location";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().getMapViewTrackingUtilities().backToLocationImpl();
		return true;
	}
}
