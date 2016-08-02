package net.osmand.plus.mapcontextmenu.other;

import android.support.v4.app.Fragment;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;

public class DestinationReachedMenu extends BaseMenuController {

	public DestinationReachedMenu(MapActivity mapActivity) {
		super(mapActivity);
	}

	public static void show(MapActivity mapActivity) {
		if (!DestinationReachedMenuFragment.isExists()) {
			DestinationReachedMenu menu = new DestinationReachedMenu(mapActivity);
			DestinationReachedMenuFragment.showInstance(menu);
		}
	}
}
