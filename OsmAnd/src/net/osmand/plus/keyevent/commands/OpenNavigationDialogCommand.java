package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.layers.MapControlsLayer;

public class OpenNavigationDialogCommand extends KeyEventCommand {

	public static final String ID = "open_navigation_dialog";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		MapControlsLayer mapControlsLayer = requireMapActivity().getMapLayers().getMapControlsLayer();
		if (mapControlsLayer != null) {
			mapControlsLayer.doRoute();
		}
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_open_navigation_view);
	}
}
