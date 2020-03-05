package net.osmand.plus.mapcontextmenu.other;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;

public class DestinationReachedMenu extends BaseMenuController {

	private static boolean shown = false;

	public static boolean wasShown() {
		return shown;
	}

	public static void resetShownState() {
		DestinationReachedMenu.shown = false;
	}

	DestinationReachedMenu(MapActivity mapActivity) {
		super(mapActivity);
	}

	public static void show(MapActivity mapActivity) {
		if (!DestinationReachedMenuFragment.isExists()) {
			shown = true;
			DestinationReachedMenu menu = new DestinationReachedMenu(mapActivity);
			DestinationReachedMenuFragment.showInstance(menu);
		}
	}
}
