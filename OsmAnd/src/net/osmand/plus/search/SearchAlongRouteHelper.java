package net.osmand.plus.search;

import static net.osmand.plus.helpers.WaypointHelper.POI;
import static net.osmand.search.core.ObjectType.POI_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.Amenity;
import net.osmand.data.Amenity.AmenityRoutePoint;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches a POI category along the rest of the calculated route, the same way as
 * "Show along the route" in the navigation settings does (see WaypointHelper.calculatePoi).
 */
public class SearchAlongRouteHelper {

	// "Show along the route" defaults to 100 m, too narrow for a service area next to a motorway
	private static final int MIN_SEARCH_RADIUS = 500;

	private final OsmandApplication app;

	public SearchAlongRouteHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public boolean isRouteAvailable() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		return routingHelper.isRouteCalculated() && !routingHelper.isPublicTransportMode();
	}

	public boolean isFollowingRoute() {
		return isRouteAvailable() && app.getRoutingHelper().isFollowingMode();
	}

	/**
	 * A round value in the user's units: 500 m, or 0.25 mi for miles.
	 */
	public int getSearchRadius() {
		int radius = Math.max(MIN_SEARCH_RADIUS, app.getWaypointHelper().getSearchDeviationRadius(POI));
		return (int) Math.round(OsmAndFormatter.calculateRoundedDist(radius, app));
	}

	@Nullable
	public PoiUIFilter getPoiFilter(@NonNull SearchPhrase phrase) {
		if (!phrase.isLastWord(POI_TYPE) || !phrase.getUnknownSearchPhrase().isEmpty()) {
			return null;
		}
		SearchWord word = phrase.getLastSelectedWord();
		Object object = word != null && word.getResult() != null ? word.getResult().object : null;
		if (object instanceof PoiUIFilter filter) {
			return filter;
		} else if (object instanceof AbstractPoiType poiType) {
			PoiUIFilter filter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + poiType.getKeyName());
			if (filter != null) {
				filter.setFilterByName(null);
				filter.clearFilter();
				filter.updateTypesToAccept(poiType);
			}
			return filter;
		}
		return null;
	}

	/**
	 * Must be called off the UI thread. Results are ordered by the distance along the route.
	 */
	@NonNull
	public List<SearchResult> search(@NonNull PoiUIFilter filter, @NonNull SearchPhrase phrase) {
		RouteCalculationResult route = app.getRoutingHelper().getRoute();
		List<Location> locations = route.getRouteLocations();
		if (locations.size() < 2) {
			return Collections.emptyList();
		}
		int firstIndex = route.getCurrentRoute();
		Map<Location, Integer> indexes = new IdentityHashMap<>();
		for (int i = 0; i < locations.size(); i++) {
			indexes.put(locations.get(i), firstIndex + i);
		}
		List<AlongRouteResult> found = new ArrayList<>();
		int radius = getSearchRadius();
		for (Amenity amenity : filter.searchAmenitiesOnThePath(locations, radius)) {
			AmenityRoutePoint routePoint = amenity.getRoutePoint();
			Integer index = routePoint != null ? indexes.get(routePoint.pointA) : null;
			if (index != null) {
				SearchResult result = SearchCoreFactory.createSearchResult(amenity, phrase, app.getPoiTypes());
				int ahead = route.getDistanceToPoint(index);
				result.addressName = app.getString(R.string.search_along_route_distance,
						OsmAndFormatter.getFormattedDistance(ahead, app),
						OsmAndFormatter.getFormattedDistance((float) routePoint.deviateDistance, app));
				found.add(new AlongRouteResult(result, ahead));
			}
		}
		found.sort((r1, r2) -> Integer.compare(r1.distanceAhead, r2.distanceAhead));
		List<SearchResult> results = new ArrayList<>(found.size());
		for (AlongRouteResult r : found) {
			results.add(r.result);
		}
		return results;
	}

	private record AlongRouteResult(@NonNull SearchResult result, int distanceAhead) {
	}
}
