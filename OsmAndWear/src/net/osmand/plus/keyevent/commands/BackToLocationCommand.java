package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class BackToLocationCommand extends KeyEventCommand {

	public static final String ID = "back_to_location";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().getMapViewTrackingUtilities().backToLocationImpl();
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_move_to_my_location);
	}
}
