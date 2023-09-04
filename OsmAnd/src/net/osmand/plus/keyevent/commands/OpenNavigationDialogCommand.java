package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

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
}
