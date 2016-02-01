package net.osmand.plus.mapcontextmenu.other;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;

public class DestinationReachedMenu extends BaseMenuController {

	public DestinationReachedMenu(MapActivity mapActivity) {
		super(mapActivity);
	}

	public static void show(MapActivity mapActivity) {
		DestinationReachedMenu menu = new DestinationReachedMenu(mapActivity);
		DestinationReachedMenuFragment.showInstance(menu);
	}
}
