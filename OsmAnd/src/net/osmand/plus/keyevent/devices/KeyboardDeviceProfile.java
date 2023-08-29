package net.osmand.plus.keyevent.devices;

import android.view.KeyEvent;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.ActivityBackPressedCommand;
import net.osmand.plus.keyevent.devices.base.DefaultInputDeviceProfile;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;

public class KeyboardDeviceProfile extends DefaultInputDeviceProfile {

	public KeyboardDeviceProfile() {
		super(1, R.string.sett_generic_ext_input);
	}

	@Override
	protected void collectCommands() {
		super.collectCommands();
		bindCommand(KeyEvent.KEYCODE_BACK, ActivityBackPressedCommand.ID);
	}

	@Override
	protected InputDeviceProfile newInstance() {
		return new KeyboardDeviceProfile();
	}
}
