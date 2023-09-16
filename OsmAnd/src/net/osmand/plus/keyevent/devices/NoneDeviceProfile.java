package net.osmand.plus.keyevent.devices;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;

public class NoneDeviceProfile extends InputDeviceProfile {

	public NoneDeviceProfile() {
		super(0, R.string.shared_string_none);
	}

	@Override
	protected void collectCommands() {
		// No commands
	}

	@Override
	protected InputDeviceProfile newInstance() {
		return new NoneDeviceProfile();
	}
}
