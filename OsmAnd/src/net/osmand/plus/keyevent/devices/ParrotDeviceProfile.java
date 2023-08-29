package net.osmand.plus.keyevent.devices;

import android.view.KeyEvent;

import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.devices.base.DefaultInputDeviceProfile;
import net.osmand.plus.settings.enums.InputDevice;

/**
 * Parrot device has only dpad left and right
 */
public class ParrotDeviceProfile extends DefaultInputDeviceProfile {

	@Override
	protected void collectCommands() {
		super.collectCommands();
		bindCommand(KeyEvent.KEYCODE_DPAD_LEFT, MapZoomCommand.ZOOM_OUT_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_RIGHT, MapZoomCommand.ZOOM_IN_ID);
	}

	@Override
	public String getId() {
		return InputDevice.PARROT.name();
	}
}
