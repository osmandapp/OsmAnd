package net.osmand.plus.keyevent.devices;

import android.view.KeyEvent;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.devices.base.DefaultInputDeviceProfile;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;

/**
 * Parrot device has only dpad left and right
 */
public class ParrotDeviceProfile extends DefaultInputDeviceProfile {

	public ParrotDeviceProfile() {
		super(2, R.string.sett_parrot_ext_input);
	}

	@Override
	protected void collectCommands() {
		super.collectCommands();
		bindCommand(KeyEvent.KEYCODE_DPAD_LEFT, MapZoomCommand.ZOOM_OUT_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_RIGHT, MapZoomCommand.ZOOM_IN_ID);
	}

	@Override
	protected InputDeviceProfile newInstance() {
		return new ParrotDeviceProfile();
	}
}
