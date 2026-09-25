package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.OpenWunderLINQDatagridCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * WunderLINQ device, motorcycle smart phone control.
 */
public class WunderLINQDeviceProfile extends PredefinedInputDeviceProfile {

	public static final String ID = "wunderlinq";

	@Override
	@NonNull
	protected List<KeyAssignment> collectAssignments() {
		List<KeyAssignment> list = new ArrayList<>();

		addAssignment(list, MapZoomCommand.ZOOM_IN_ID, KeyEvent.KEYCODE_DPAD_UP);
		addAssignment(list, MapZoomCommand.ZOOM_OUT_ID, KeyEvent.KEYCODE_DPAD_DOWN);
		addAssignment(list, OpenWunderLINQDatagridCommand.ID, KeyEvent.KEYCODE_ESCAPE);
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
		return context.getString(R.string.sett_wunderlinq_ext_input);
	}
}
