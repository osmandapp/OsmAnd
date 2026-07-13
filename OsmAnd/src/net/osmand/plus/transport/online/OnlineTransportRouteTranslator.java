package net.osmand.plus.transport.online;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.TransportRoutingConfiguration;
import net.osmand.util.GeoPolylineParserUtil;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class OnlineTransportRouteTranslator {

	private static final Log LOG = PlatformUtil.getLog(OnlineTransportRouteTranslator.class);
	private static final List<String> ALL_MODES = Arrays.asList("HIGHSPEED_RAIL", "LONG_DISTANCE", "NIGHT_RAIL",
			"REGIONAL_RAIL", "SUBURBAN", "SUBWAY", "TRAM", "BUS", "COACH", "FERRY", "FUNICULAR", "AERIAL_LIFT");
	private static final AtomicLong ID_SEQ = new AtomicLong(1);

	private OnlineTransportRouteTranslator() {
	}

	public static List<TransportRouteResult> buildRoutes(OsmandSettings settings, Map<String, String> routeParams,
			LatLon from, LatLon to, TransportRoutingConfiguration cfg) {
		OsmandApplication app = settings.getContext();
		String body;
		try {
			body = app.getOnlineRoutingHelper().makeRequest(buildUrl(settings, routeParams, from, to));
		} catch (Exception e) {
			LOG.error("Online transit request failed", e);
			return Collections.emptyList();
		}
		try {
			JSONArray itineraries = new JSONObject(body).optJSONArray("itineraries");
			if (itineraries == null) {
				return Collections.emptyList();
			}
			List<JSONObject> list = new ArrayList<>(itineraries.length());
			for (int i = 0; i < itineraries.length(); i++) {
				JSONObject itinerary = itineraries.optJSONObject(i);
				if (itinerary != null) {
					list.add(itinerary);
				}
			}
			Comparator<JSONObject> comparator = optimizeComparator(settings.ONLINE_TRANSPORT_OPTIMIZE.get());
			if (comparator != null) {
				Collections.sort(list, comparator);
			}
			List<TransportRouteResult> results = new ArrayList<>(list.size());
			for (JSONObject itinerary : list) {
				TransportRouteResult result = toRouteResult(itinerary, cfg);
				if (result != null) {
					results.add(result);
				}
			}
			return results;
		} catch (Exception e) {
			LOG.error("Online transit response parse failed", e);
			return Collections.emptyList();
		}
	}

	private static String buildUrl(OsmandSettings settings, Map<String, String> routeParams, LatLon from, LatLon to) {
		StringBuilder sb = new StringBuilder(settings.ONLINE_TRANSPORT_URL.get());
		sb.append("?fromPlace=").append(from.getLatitude()).append(',').append(from.getLongitude());
		sb.append("&toPlace=").append(to.getLatitude()).append(',').append(to.getLongitude());
		if (!OnlineTransportState.isNow()) {
			sb.append("&time=").append(Instant.ofEpochMilli(OnlineTransportState.getTimeMillis()));
		}
		if (OnlineTransportState.isArriveBy()) {
			sb.append("&arriveBy=true");
		}
		if (settings.ONLINE_TRANSPORT_WHEELCHAIR.get()) {
			sb.append("&pedestrianProfile=WHEELCHAIR");
		}
		sb.append("&transitModes=").append(transitModes(routeParams));
		Integer maxTransfers = maxTransfers(routeParams);
		if (maxTransfers != null) {
			sb.append("&maxTransfers=").append(maxTransfers);
		}
		return sb.toString();
	}

	// MOTIS counts first/last-mile transit legs as transfers, so allow two beyond the profile's change limit.
	private static Integer maxTransfers(Map<String, String> routeParams) {
		String value = routeParams.get("max_num_changes");
		if (value == null) {
			return null;
		}
		try {
			return (int) Double.parseDouble(value) + 2;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static TransportRouteResult toRouteResult(JSONObject itinerary, TransportRoutingConfiguration cfg) {
		JSONArray legs = itinerary.optJSONArray("legs");
		if (legs == null) {
			return null;
		}
		TransportRouteResult result = new TransportRouteResult(cfg);
		boolean anyLeg = false;
		double pendingWalkDist = 0;
		for (int i = 0; i < legs.length(); i++) {
			JSONObject leg = legs.optJSONObject(i);
			if (leg == null) {
				continue;
			}
			String mode = leg.optString("mode");
			if (!isTransit(mode)) {
				if ("WALK".equals(mode)) {
					pendingWalkDist += leg.optDouble("distance", 0);
				}
				continue;
			}
			List<TransportStop> stops = new ArrayList<>();
			addStop(stops, leg.optJSONObject("from"));
			JSONArray intermediate = leg.optJSONArray("intermediateStops");
			if (intermediate != null) {
				for (int j = 0; j < intermediate.length(); j++) {
					addStop(stops, intermediate.optJSONObject(j));
				}
			}
			addStop(stops, leg.optJSONObject("to"));
			if (stops.size() < 2) {
				return null;
			}

			TransportRoute route = new TransportRoute();
			route.setId(ID_SEQ.getAndIncrement());
			route.setRef(leg.optString("routeShortName", ""));
			route.setType(osmandType(mode));
			route.setName(leg.optString("headsign", ""));
			String color = leg.optString("routeColor", "");
			if (!color.isEmpty()) {
				route.setColor("#" + color);
			}
			route.setForwardStops(stops);
			addGeometry(route, leg.optJSONObject("legGeometry"));

			TransportRouteResultSegment segment = new TransportRouteResultSegment();
			segment.route = route;
			segment.start = 0;
			segment.end = stops.size() - 1;
			segment.walkDist = pendingWalkDist;
			pendingWalkDist = 0;
			long departure = epochMillis(leg.optString("startTime"));
			long arrival = epochMillis(leg.optString("endTime"));
			segment.departureTimeMillis = departure;
			segment.arrivalTimeMillis = arrival;
			segment.travelTime = departure > 0 && arrival > 0 ? (arrival - departure) / 1000.0 : 0;
			result.addSegment(segment);
			anyLeg = true;
		}
		if (anyLeg) {
			result.setFinishWalkDist(pendingWalkDist);
		}
		return anyLeg ? result : null;
	}

	private static void addStop(List<TransportStop> stops, JSONObject place) {
		if (place == null) {
			return;
		}
		double lat = place.optDouble("lat", Double.NaN);
		double lon = place.optDouble("lon", Double.NaN);
		if (Double.isNaN(lat) || Double.isNaN(lon)) {
			return;
		}
		TransportStop stop = new TransportStop();
		stop.setId(ID_SEQ.getAndIncrement());
		stop.setLocation(lat, lon);
		stop.setName(place.optString("name", ""));
		String track = place.optString("track", "");
		if (!track.isEmpty()) {
			stop.setPlatform(track);
		}
		stops.add(stop);
	}

	private static void addGeometry(TransportRoute route, JSONObject legGeometry) {
		String points = legGeometry != null ? legGeometry.optString("points") : null;
		if (points == null || points.isEmpty()) {
			return;
		}
		List<LatLon> decoded = GeoPolylineParserUtil.parse(points, GeoPolylineParserUtil.PRECISION_6);
		if (decoded == null || decoded.isEmpty()) {
			return;
		}
		Way way = new Way(ID_SEQ.getAndIncrement());
		LatLon prev = null;
		for (int i = 0; i < decoded.size(); i++) {
			LatLon point = decoded.get(i);
			// bound node count on long-distance legs: keep the endpoints and points at least 20 m apart
			if (prev == null || i == decoded.size() - 1 || MapUtils.getDistance(prev, point) >= 20) {
				way.addNode(new Node(point.getLatitude(), point.getLongitude(), ID_SEQ.getAndIncrement()));
				prev = point;
			}
		}
		route.setForwardWays(Collections.singletonList(way));
	}

	private static Comparator<JSONObject> optimizeComparator(OnlineTransportOptimize optimize) {
		switch (optimize) {
			case FEWEST_TRANSFERS:
				return Comparator.comparingInt((JSONObject o) -> o.optInt("transfers", 0))
						.thenComparingDouble(o -> o.optDouble("duration", 0));
			case LEAST_WALKING:
				return Comparator.comparingDouble(OnlineTransportRouteTranslator::walkDistance)
						.thenComparingDouble(o -> o.optDouble("duration", 0));
			default:
				// keep the order the server returns, which is earliest departure first
				return null;
		}
	}

	private static double walkDistance(JSONObject itinerary) {
		JSONArray legs = itinerary.optJSONArray("legs");
		if (legs == null) {
			return 0;
		}
		double distance = 0;
		for (int i = 0; i < legs.length(); i++) {
			JSONObject leg = legs.optJSONObject(i);
			if (leg != null && "WALK".equals(leg.optString("mode"))) {
				distance += leg.optDouble("distance", 0);
			}
		}
		return distance;
	}

	private static String transitModes(Map<String, String> routeParams) {
		Set<String> modes = new LinkedHashSet<>(ALL_MODES);
		if (isAvoided(routeParams, "avoid_train")) {
			modes.removeAll(Arrays.asList("HIGHSPEED_RAIL", "LONG_DISTANCE", "NIGHT_RAIL", "REGIONAL_RAIL", "SUBURBAN"));
		}
		if (isAvoided(routeParams, "avoid_subway")) {
			modes.remove("SUBWAY");
		}
		if (isAvoided(routeParams, "avoid_tram")) {
			modes.remove("TRAM");
		}
		if (isAvoided(routeParams, "avoid_bus")) {
			modes.removeAll(Arrays.asList("BUS", "COACH"));
		}
		if (isAvoided(routeParams, "avoid_ferry")) {
			modes.remove("FERRY");
		}
		return String.join(",", modes);
	}

	private static boolean isAvoided(Map<String, String> routeParams, String id) {
		return "true".equals(routeParams.get(id));
	}

	private static boolean isTransit(String mode) {
		switch (mode) {
			case "WALK":
			case "BIKE":
			case "CAR":
			case "CAR_PARKING":
			case "CAR_DROPOFF":
			case "RENTAL":
			case "ODM":
			case "RIDE_SHARING":
			case "FLEX":
			case "":
				return false;
			default:
				return true;
		}
	}

	private static String osmandType(String mode) {
		switch (mode) {
			case "SUBWAY":
				return "subway";
			case "TRAM":
				return "tram";
			case "FERRY":
				return "ferry";
			case "FUNICULAR":
			case "AERIAL_LIFT":
				return "funicular";
			case "RAIL":
			case "HIGHSPEED_RAIL":
			case "LONG_DISTANCE":
			case "NIGHT_RAIL":
			case "REGIONAL_FAST_RAIL":
			case "REGIONAL_RAIL":
			case "SUBURBAN":
				return "train";
			default:
				return "bus";
		}
	}

	private static long epochMillis(String iso) {
		if (iso == null || iso.isEmpty()) {
			return -1;
		}
		try {
			return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
		} catch (Exception e) {
			return -1;
		}
	}
}
