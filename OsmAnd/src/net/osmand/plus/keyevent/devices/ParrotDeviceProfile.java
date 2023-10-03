package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;

/**
 * Parrot device has only dpad left and right
 */
public class ParrotDeviceProfile extends InputDeviceProfile {

	public static final String ID = "parrot";

	@Override
	protected void collectCommands() {
		bindCommand(KeyEvent.KEYCODE_DPAD_LEFT, MapZoomCommand.ZOOM_OUT_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_RIGHT, MapZoomCommand.ZOOM_IN_ID);
	}

	@NonNull
	@Override
	public String getId() {
		return ID;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.sett_parrot_ext_input);
	}
}
