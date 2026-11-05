package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class ActivityBackPressedCommand extends KeyEventCommand {

	public static final String ID = "activity_back_pressed";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().onBackPressed();
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_activity_back_pressed);
	}
}
