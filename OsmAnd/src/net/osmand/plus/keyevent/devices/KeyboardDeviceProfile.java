package net.osmand.plus.keyevent.devices;

import android.view.KeyEvent;

import net.osmand.plus.keyevent.commands.ActivityBackPressedCommand;
import net.osmand.plus.keyevent.devices.base.DefaultInputDeviceProfile;
import net.osmand.plus.settings.enums.InputDevice;

public class KeyboardDeviceProfile extends DefaultInputDeviceProfile {

	@Override
	protected void collectCommands() {
		super.collectCommands();
		bindCommand(KeyEvent.KEYCODE_BACK, ActivityBackPressedCommand.ID);
	}

	@Override
	public String getId() {
		return InputDevice.KEYBOARD.name();
	}
}
