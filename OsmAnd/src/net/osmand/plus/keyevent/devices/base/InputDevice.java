package net.osmand.plus.keyevent.devices.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.keyevent.devices.ParrotDeviceProfile;
import net.osmand.plus.keyevent.devices.WunderLINQDeviceProfile;

import java.util.Objects;

public class InputDevice {

	public static final InputDeviceProfile KEYBOARD = new KeyboardDeviceProfile();
	public static final InputDeviceProfile PARROT = new ParrotDeviceProfile();
	public static final InputDeviceProfile WUNDER_LINQ = new WunderLINQDeviceProfile();

	private static InputDeviceProfile[] defaultTypes;

	public static InputDeviceProfile[] values() {
		if (defaultTypes == null) {
			defaultTypes = new InputDeviceProfile[] {
					KEYBOARD, PARROT, WUNDER_LINQ
			};
		}
		return defaultTypes;
	}

	@Nullable
	public static InputDeviceProfile getByValue(@NonNull String id) {
		for (InputDeviceProfile device : values()) {
			if (Objects.equals(id, device.getId())) {
				return device;
			}
		}
		return null;
	}
}
