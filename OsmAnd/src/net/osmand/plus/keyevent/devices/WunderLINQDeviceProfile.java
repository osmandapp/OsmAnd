package net.osmand.plus.keyevent.devices;

import android.view.KeyEvent;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.OpenWunderLINQDatagridCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.devices.base.DefaultInputDeviceProfile;
import net.osmand.plus.keyevent.devices.base.InputDeviceProfile;

/**
 * WunderLINQ device, motorcycle smart phone control
 */
public class WunderLINQDeviceProfile extends DefaultInputDeviceProfile {

	public WunderLINQDeviceProfile() {
		super(3, R.string.sett_wunderlinq_ext_input);
	}

	@Override
	protected void collectCommands() {
		super.collectCommands();
		bindCommand(KeyEvent.KEYCODE_DPAD_DOWN, MapZoomCommand.ZOOM_OUT_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_UP, MapZoomCommand.ZOOM_IN_ID);
		bindCommand(KeyEvent.KEYCODE_ESCAPE, OpenWunderLINQDatagridCommand.ID);
	}

	@Override
	protected InputDeviceProfile newInstance() {
		return new WunderLINQDeviceProfile();
	}
}
