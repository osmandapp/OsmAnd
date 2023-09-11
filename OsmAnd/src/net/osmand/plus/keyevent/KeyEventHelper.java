package net.osmand.plus.keyevent;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.devices.base.InputDevice;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Objects;

public class KeyEventHelper implements KeyEvent.Callback {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private MapActivity mapActivity;

	/**
	 * Use the same Commands factory to speed up new commands creation
	 */
	private final KeyEventCommandsFactory commandsFactory = new KeyEventCommandsFactory();
	private InputDeviceProfile deviceProfile = InputDevice.NONE;

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
		return inputDevice.findCommand(keyCode);
	}

	@NonNull
	private InputDeviceProfile getInputDeviceProfile() {
		InputDeviceProfile selectedDevice = settings.getSelectedInputDevice();
		if (!Objects.equals(deviceProfile, selectedDevice)) {
			deviceProfile = selectedDevice.newInstance(app);
		}
		return deviceProfile;
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

