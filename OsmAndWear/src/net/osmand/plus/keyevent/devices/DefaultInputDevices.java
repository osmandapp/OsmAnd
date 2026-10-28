package net.osmand.plus.keyevent.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.keyevent.devices.ParrotDeviceProfile;
import net.osmand.plus.keyevent.devices.WunderLINQDeviceProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultInputDevices {

	public static final InputDeviceProfile KEYBOARD = new KeyboardDeviceProfile();
	public static final InputDeviceProfile PARROT = new ParrotDeviceProfile();
	public static final InputDeviceProfile WUNDER_LINQ = new WunderLINQDeviceProfile();

	public static final List<InputDeviceProfile> values = new ArrayList<>();

	public static void initialize(@NonNull OsmandApplication app) {
		for (InputDeviceProfile device : values()) {
			device.initialize(app);
		}
	}

	public static List<InputDeviceProfile> values() {
		if (values.size() == 0) {
			values.addAll(Arrays.asList(KEYBOARD, PARROT, WUNDER_LINQ));
		}
		return values;
	}

}
