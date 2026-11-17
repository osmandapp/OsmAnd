package net.osmand.plus.mapcontextmenu.other;

import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.Comparator;

class MultiSelectionMenuComparator implements Comparator<MenuObject> {

	private final RouteTypesComparator routeTypesComparator;

	public MultiSelectionMenuComparator(@Nullable ApplicationMode appMode) {
		routeTypesComparator = new RouteTypesComparator(appMode);
	}

	@Override
	public int compare(MenuObject o1, MenuObject o2) {
		if (o1.getOrder() != o2.getOrder()) {
			return o1.getOrder() - o2.getOrder();
		} else if (hasRouteObject(o1, o2)) {
			return compareRouteObjects(o1, o2);
		} else {
			return compareByTitle(o1, o2);
		}
	}

	private boolean hasRouteObject(MenuObject o1, MenuObject o2) {
		return routeTypesComparator.hasRouteObject(o1, o2);
	}

	private int compareRouteObjects(MenuObject o1, MenuObject o2) {
		int compare = routeTypesComparator.compare(o1, o2);
		return compare == 0 ? compareByTitle(o1, o2) : compare;
	}

	private int compareByTitle(MenuObject o1, MenuObject o2) {
		return o1.getTitleStr().compareToIgnoreCase(o2.getTitleStr());
	}
}
