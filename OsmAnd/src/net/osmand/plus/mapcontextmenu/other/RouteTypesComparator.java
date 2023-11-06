package net.osmand.plus.mapcontextmenu.other;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.router.network.NetworkRouteSelector.RouteType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class RouteTypesComparator implements Comparator<MenuObject> {

	private String ROUTE_BICYCLE = "bicycle";
	private String ROUTE_BUS = "bus";
	private String ROUTE_CANOE = "canoe";
	private String ROUTE_DETOUR = "detour";
	private String ROUTE_FERRY = "ferry";
	private String ROUTE_FOOT = "foot";
	private String ROUTE_HIKING = "hiking";
	private String ROUTE_HORSE = "horse";
	private String ROUTE_INLINE_SKATES = "inline_skates";
	private String ROUTE_LIGHT_RAIL = "light_rail";
	private String ROUTE_MTB = "mtb";
	private String ROUTE_PISTE = "piste";
	private String ROUTE_RAILWAY = "railway";
	private String ROUTE_ROAD = "road";
	private String ROUTE_RUNNING = "running";
	private String ROUTE_SKI = "ski";
	private String ROUTE_SUBWAY = "subway";
	private String ROUTE_TRAIN = "train";
	private String ROUTE_TRACKS = "tracks";
	private String ROUTE_TRAM = "tram";
	private String ROUTE_TROLLEYBUS = "trolleybus";

	private final ApplicationMode appMode;
	private final List<String> orderedRouteTypes = new ArrayList<>();

	public RouteTypesComparator(@Nullable ApplicationMode appMode) {
		this.appMode = appMode;
		collectOrderedRouteTypesForProfile();
	}

	private void collectOrderedRouteTypesForProfile() {
		orderedRouteTypes.clear();

		addRouteTypes(ApplicationMode.CAR,
				ROUTE_ROAD, ROUTE_DETOUR, ROUTE_BICYCLE, ROUTE_MTB);

		addRouteTypes(ApplicationMode.BICYCLE,
				ROUTE_BICYCLE, ROUTE_MTB, ROUTE_ROAD, ROUTE_DETOUR);

		addRouteTypes(ApplicationMode.PEDESTRIAN,
				ROUTE_FOOT, ROUTE_HIKING, ROUTE_RUNNING);

		addRouteTypes(ApplicationMode.TRUCK,
				ROUTE_ROAD, ROUTE_DETOUR, ROUTE_BICYCLE, ROUTE_MTB);

		addRouteTypes(ApplicationMode.MOTORCYCLE,
				ROUTE_ROAD, ROUTE_DETOUR, ROUTE_BICYCLE, ROUTE_MTB);

		addRouteTypes(ApplicationMode.MOPED,
				ROUTE_ROAD, ROUTE_DETOUR, ROUTE_BICYCLE, ROUTE_MTB);

		addRouteTypes(ApplicationMode.PUBLIC_TRANSPORT,
				ROUTE_BUS, ROUTE_TRAM, ROUTE_TROLLEYBUS, ROUTE_SUBWAY, ROUTE_TRAIN,
				ROUTE_RAILWAY, ROUTE_TRACKS, ROUTE_LIGHT_RAIL, ROUTE_PISTE);

		addRouteTypes(ApplicationMode.TRAIN,
				ROUTE_TRAIN, ROUTE_LIGHT_RAIL, ROUTE_TRACKS, ROUTE_SUBWAY, ROUTE_TRAM);

		addRouteTypes(ApplicationMode.BOAT, ROUTE_FERRY, ROUTE_CANOE);

		/*addRouteTypes(ApplicationMode.AIRCRAFT);*/ // No available types yet

		addRouteTypes(ApplicationMode.SKI,
				ROUTE_SKI, ROUTE_INLINE_SKATES);

		addRouteTypes(ApplicationMode.HORSE,
				ROUTE_HORSE);
	}

	private void addRouteTypes(@NonNull ApplicationMode baseAppMode, @NonNull String ... routeTypes) {
		if (appMode != null && appMode.isDerivedRoutingFrom(baseAppMode)) {
			orderedRouteTypes.addAll(Arrays.asList(routeTypes));
		}
	}

	public boolean hasRouteObject(MenuObject o1, MenuObject o2) {
		return isRouteObject(o1) || isRouteObject(o2);
	}

	private boolean isRouteObject(MenuObject menuObject) {
		return Amenity.ROUTE.equalsIgnoreCase(menuObject.getTypeStr());
	}

	@Override
	public int compare(MenuObject o1, MenuObject o2) {
		int compare = compareByObjectType(o1, o2);
		return compare == 0 ? compareByRouteType(o1, o2) : compare;
	}

	private int compareByObjectType(MenuObject o1, MenuObject o2) {
		boolean isRoute1 = isRouteObject(o1);
		boolean isRoute2 = isRouteObject(o2);

		if (isRoute1 && !isRoute2) {
			return -1;
		} else if (isRoute2 && !isRoute1) {
			return 1;
		}
		return 0;
	}

	private int compareByRouteType(@NonNull MenuObject o1, @NonNull MenuObject o2) {
		String typeTag1 = getRouteTypeTag(o1);
		String typeTag2 = getRouteTypeTag(o2);

		if (typeTag1 == null && typeTag2 == null) {
			return 0;
		} else if (typeTag1 == null) {
			return 1;
		} else if (typeTag2 == null) {
			return -1;
		}

		// Try to sort by custom order dependent on selected app mode
		int index1 = orderedRouteTypes.indexOf(typeTag1);
		int index2 = orderedRouteTypes.indexOf(typeTag2);
		if (index1 < 0 && index2 < 0) {
			return typeTag1.compareToIgnoreCase(typeTag2);
		} else if (index1 >= 0 && index2 >= 0) {
			return Integer.compare(index1, index2);
		}
		return index1 >= 0 ? -1 : 1;
	}

	@Nullable
	private String getRouteTypeTag(@NonNull MenuObject menuObject) {
		Object object = menuObject.getObject();
		if (object instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) object;
			Object firstObject = pair.first;
			if (firstObject instanceof RouteKey) {
				RouteKey routeKey = (RouteKey) firstObject;
				RouteType routeType = routeKey.type;
				return routeType != null ? routeType.getTag() : null;
			}
		}
		return null;
	}
}
