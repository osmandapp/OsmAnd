package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class SwitchCompassCommand extends KeyEventCommand {

	public static final String ID = "switch_compass_forward";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().getMapViewTrackingUtilities().requestSwitchCompassToNextMode();
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_change_map_orientation);
	}
}
