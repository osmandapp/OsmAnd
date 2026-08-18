package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Parrot device has only dpad left and right.
 */
public class ParrotDeviceProfile extends PredefinedInputDeviceProfile {

	public static final String ID = "parrot";

	@Override
	@NonNull
	protected List<KeyAssignment> collectAssignments() {
		List<KeyAssignment> list = new ArrayList<>();

		addAssignment(list, MapZoomCommand.ZOOM_OUT_ID, KeyEvent.KEYCODE_DPAD_LEFT);
		addAssignment(list, MapZoomCommand.ZOOM_IN_ID, KeyEvent.KEYCODE_DPAD_RIGHT);
		return list;
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
