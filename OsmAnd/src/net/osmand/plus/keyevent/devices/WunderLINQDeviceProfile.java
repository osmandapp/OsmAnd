package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.OpenWunderLINQDatagridCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;

/**
 * WunderLINQ device, motorcycle smart phone control
 */
public class WunderLINQDeviceProfile extends InputDeviceProfile {

	public static final String ID = "wunderlinq";

	@Override
	protected void collectCommands() {
		bindCommand(KeyEvent.KEYCODE_DPAD_DOWN, MapZoomCommand.ZOOM_OUT_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_UP, MapZoomCommand.ZOOM_IN_ID);
		bindCommand(KeyEvent.KEYCODE_ESCAPE, OpenWunderLINQDatagridCommand.ID);
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
