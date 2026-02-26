package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.osm.OsmRouteType.*;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.osm.OsmRouteType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class RouteTypesComparator implements Comparator<MenuObject> {

	private final ApplicationMode appMode;
	private final List<String> orderedRouteTypes = new ArrayList<>();

	public RouteTypesComparator(@Nullable ApplicationMode appMode) {
		this.appMode = appMode;
		collectOrderedRouteTypesForProfile();
	}

	private void collectOrderedRouteTypesForProfile() {
		orderedRouteTypes.clear();

		addRouteTypes(ApplicationMode.CAR,
				ROAD.getName(), DETOUR.getName(), BICYCLE.getName(), MOUNTAINBIKE.getName());
		addRouteTypes(ApplicationMode.BICYCLE,
				BICYCLE.getName(), MOUNTAINBIKE.getName(), ROAD.getName(), DETOUR.getName());
		addRouteTypes(ApplicationMode.PEDESTRIAN,
				FOOT.getName(), HIKING.getName(), RUNNING.getName());
		addRouteTypes(ApplicationMode.TRUCK,
				ROAD.getName(), DETOUR.getName(), BICYCLE.getName(), MOUNTAINBIKE.getName());
		addRouteTypes(ApplicationMode.MOTORCYCLE,
				ROAD.getName(), DETOUR.getName(), BICYCLE.getName(), MOUNTAINBIKE.getName());
		addRouteTypes(ApplicationMode.MOPED,
				ROAD.getName(), DETOUR.getName(), BICYCLE.getName(), MOUNTAINBIKE.getName());
		addRouteTypes(ApplicationMode.PUBLIC_TRANSPORT,
				BUS.getName(), TRAM.getName(), TROLLEYBUS.getName(), SUBWAY.getName(), TRAIN.getName(),
				RAILWAY.getName(), TRACKS.getName(), LIGHT_RAIL.getName(), PISTE.getName());
		addRouteTypes(ApplicationMode.TRAIN,
				TRAIN.getName(), LIGHT_RAIL.getName(), TRACKS.getName(), SUBWAY.getName(), TRAM.getName());
		addRouteTypes(ApplicationMode.BOAT,
				FERRY.getName(), CANOE.getName());
		/*addRouteTypes(ApplicationMode.AIRCRAFT);*/ // No available types yet
		addRouteTypes(ApplicationMode.SKI,
				SKI.getName(), INLINE_SKATES.getName());
		addRouteTypes(ApplicationMode.HORSE,
				HORSE.getName());
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
				OsmRouteType routeType = routeKey.type;
				return routeType != null ? routeType.getName() : null;
			}
		}
		return null;
	}
}
