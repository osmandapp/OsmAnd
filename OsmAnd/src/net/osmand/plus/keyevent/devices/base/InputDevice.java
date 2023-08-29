package net.osmand.plus.keyevent.devices.base;

import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.keyevent.devices.NoneDeviceProfile;
import net.osmand.plus.keyevent.devices.ParrotDeviceProfile;
import net.osmand.plus.keyevent.devices.WunderLINQDeviceProfile;

public class InputDevice {

	public static final InputDeviceProfile NONE = new NoneDeviceProfile();
	public static final InputDeviceProfile KEYBOARD = new KeyboardDeviceProfile();
	public static final InputDeviceProfile PARROT = new ParrotDeviceProfile();
	public static final InputDeviceProfile WUNDER_LINQ = new WunderLINQDeviceProfile();

	private static InputDeviceProfile[] defaultTypes;

	public static InputDeviceProfile[] values() {
		if (defaultTypes == null) {
			defaultTypes = new InputDeviceProfile[] {
					NONE, KEYBOARD, PARROT, WUNDER_LINQ
			};
		}
		return defaultTypes;
	}

	public static InputDeviceProfile getByValue(int value) {
		for (InputDeviceProfile device : values()) {
			if (device.getId() == value) {
				return device;
			}
		}
		return NONE;
	}
}
