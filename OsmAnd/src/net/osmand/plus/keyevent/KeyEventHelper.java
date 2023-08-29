package net.osmand.plus.keyevent;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;
import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.keyevent.devices.ParrotDeviceProfile;
import net.osmand.plus.keyevent.devices.WunderLINQDeviceProfile;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.InputDevice;

import java.util.Objects;

public class KeyEventHelper implements KeyEvent.Callback {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private MapActivity mapActivity;

	/**
	 * Use the same Commands factory to speed up new commands creation
	 */
	private final KeyEventCommandsFactory commandsFactory = new KeyEventCommandsFactory();
	private InputDeviceProfile deviceProfile;

	public KeyEventHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		KeyEventCommand command = findCommand(keyCode);
		if (command != null && command.onKeyDown(keyCode, event)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		KeyEventCommand command = findCommand(keyCode);
		return command != null && command.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		KeyEventCommand command = findCommand(keyCode);
		if (command != null && command.onKeyUp(keyCode, event)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		KeyEventCommand command = findCommand(keyCode);
		return command != null && command.onKeyMultiple(keyCode, count, event);
	}

	@Nullable
	private KeyEventCommand findCommand(int keyCode) {
		if (mapActivity == null || isLetterKeyCode(keyCode) && !mapActivity.isMapVisible()) {
			// Reject using of letter keycodes when the focus isn't on the Activity
			return null;
		}
		InputDeviceProfile inputDevice = getInputDeviceProfile();
		return inputDevice != null ? inputDevice.findCommand(keyCode) : null;
	}

	@Nullable
	private InputDeviceProfile getInputDeviceProfile() {
		InputDevice inputDevice = settings.getSelectedInputDevice();
		if (deviceProfile == null || !Objects.equals(deviceProfile.getId(), inputDevice.name())) {
			deviceProfile = createPredefinedDeviceProfile(inputDevice);
			if (deviceProfile != null) {
				deviceProfile.initialize(app);
			}
		}
		return deviceProfile;
	}

	private InputDeviceProfile createPredefinedDeviceProfile(@NonNull InputDevice inputDevice) {
		switch (inputDevice) {
			case KEYBOARD:
				return new KeyboardDeviceProfile();
			case PARROT:
				return new ParrotDeviceProfile();
			case WUNDER_LINQ:
				return new WunderLINQDeviceProfile();
			default:
				return null;
		}
	}

	@NonNull
	public KeyEventCommandsFactory getCommandsFactory() {
		return commandsFactory;
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	private static boolean isLetterKeyCode(int keyCode) {
		return keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z;
	}
}

