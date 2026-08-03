package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.layers.MapActionsHelper;

public class OpenNavigationDialogCommand extends KeyEventCommand {

	public static final String ID = "open_navigation_dialog";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		MapActionsHelper controlsHelper = requireMapActivity().getMapLayers().getMapActionsHelper();
		if (controlsHelper != null) {
			controlsHelper.doRoute();
		}
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_open_navigation_view);
	}
}
