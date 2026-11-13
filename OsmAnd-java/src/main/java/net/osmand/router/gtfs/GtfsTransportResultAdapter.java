package net.osmand.router.gtfs;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.router.TimedTransportRoute;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.TransportRoutingConfiguration;
import net.osmand.util.MapUtils;

/**
 * Converts {@link GtfsItinerary} objects into OsmAnd's transit result model, so the existing route cards,
 * details and map layers can show them. Only public constructors and setters of the core classes are used.
 * <p>
 * Conventions, all dictated by what the UI dereferences (see the A3 spike):
 * <ul>
 * <li>One {@link GtfsRoute} and one {@link GtfsSegment} per ride leg. The route's stop list is exactly the
 * boarding..alighting stops, so {@code start = 0} and {@code end = size - 1}.</li>
 * <li>Route type, ref, name and every stop id are never null; stop ids are unique negative numbers and the
 * same GTFS stop is always the same {@link TransportStop} object within one conversion.</li>
 * <li>Every route gets exactly one geometry {@link Way}: the stop polyline (the index holds no shapes).</li>
 * <li>The colour is left null: core reads it as a render-attribute name, a hex string would be wrong. The
 * GTFS colour is kept in {@link GtfsRoute#colorRgb}.</li>
 * <li>Walks are implicit: {@code segment.walkDist} is the walk before the segment and
 * {@code finishWalkDist} the walk after the last one, as TransportRoutingHelper expects.</li>
 * <li>An itinerary without a ride (walking only) has nowhere to live in this model (the card reads
 * {@code segments.get(0)}), so it is not converted: {@link #convert(GtfsItinerary)} returns null and
 * {@link #convert(List)} leaves it out. Walking the whole way is the pedestrian router's job.</li>
 * <li>Segment objects must not be copied after being handed over: the walking routes are keyed by identity.</li>
 * </ul>
 * Times in the sidecar fields are epoch seconds. {@code TransportRouteResultSegment.depTime} follows the
 * planner convention (units of 10 s since local midnight of the departure day, so it can exceed 8640).
 */
public final class GtfsTransportResultAdapter {

	/** Synthetic ids live in negative ranges so they cannot be mistaken for OBF ids. */
	private static final long STOP_ID_BASE = -4_000_000_000_000L;
	private static final long ROUTE_ID_BASE = -5_000_000_000_000L;

	private final GtfsIndex index;
	private final TransportRoutingConfiguration cfg;

	/** The cfg may be null (a default one without a router is used; the results never need the router). */
	public GtfsTransportResultAdapter(GtfsIndex index, TransportRoutingConfiguration cfg) {
		if (index == null) {
			throw new IllegalArgumentException("index");
		}
		this.index = index;
		this.cfg = cfg != null ? cfg : new TransportRoutingConfiguration(null, new HashMap<String, String>());
	}

	/** Per-conversion id registry: equal GTFS stops must give the same object (TransportStop.equals is id based). */
	private static final class Registry {
		final Map<Integer, TransportStop> stops = new HashMap<>();
		long nextStop, nextRoute;
	}

	/** Route carrying what guidance and a clock-time UI need; the stock UI ignores the extra fields. */
	public static final class GtfsRoute extends TransportRoute {
		/** Indices into the {@link GtfsIndex}; -1 when unknown. */
		public int tripIdx = -1, routeIdx = -1;
		public String headsign = "";
		/** GTFS route_color as 0xRRGGBB or {@link GtfsIndex#NO_COLOR}. */
		public int colorRgb = GtfsIndex.NO_COLOR;
		/** Per forward stop (same order as {@link #getForwardStops()}): index stop and scheduled epoch seconds. */
		public int[] stopIdx = new int[0];
		public long[] arrivalSec = new long[0], departureSec = new long[0];
	}

	public static final class GtfsSegment extends TransportRouteResultSegment implements TimedTransportRoute {
		/** Scheduled boarding departure and alighting arrival, epoch seconds. */
		public long boardSec, alightSec;
		public int tripIdx = -1, routeIdx = -1;
		ZoneId zone = ZoneId.of("UTC");

		@Override
		public long getDepartureTimeSec() {
			return boardSec;
		}

		@Override
		public long getArrivalTimeSec() {
			return alightSec;
		}

		@Override
		public ZoneId getTimeZone() {
			return zone;
		}
	}

	/**
	 * Overrides the two time getters whose stock versions add per type-pair default change times from the
	 * router config and ignore the real waits: the card total (travel + walk) equals the planned duration.
	 */
	public static final class GtfsResult extends TransportRouteResult implements TimedTransportRoute {
		/** Epoch seconds. */
		public final long departSec, arriveSec;
		public final GtfsItinerary itinerary;
		private final ZoneId zone;

		GtfsResult(TransportRoutingConfiguration cfg, GtfsItinerary itinerary, ZoneId zone) {
			super(cfg);
			this.itinerary = itinerary;
			this.departSec = itinerary.departSec;
			this.arriveSec = itinerary.arriveSec;
			this.zone = zone;
		}

		@Override
		public long getDepartureTimeSec() {
			return departSec;
		}

		@Override
		public long getArrivalTimeSec() {
			return arriveSec;
		}

		@Override
		public ZoneId getTimeZone() {
			return zone;
		}

		@Override
		public double getTravelTime() {
			return Math.max(0, getRouteTime() - getWalkTime());
		}

		@Override
		public int getChangeTime(TransportRouteResultSegment current, TransportRouteResultSegment next) {
			if (current instanceof GtfsSegment && next instanceof GtfsSegment) {
				return (int) Math.max(0, ((GtfsSegment) next).boardSec - ((GtfsSegment) current).alightSec);
			}
			return 0;
		}
	}

	/** Converts each itinerary that has a ride; walk-only and null entries are skipped. Never returns null. */
	public List<TransportRouteResult> convert(List<GtfsItinerary> itineraries) {
		if (itineraries == null || itineraries.isEmpty()) {
			return Collections.emptyList();
		}
		Registry reg = new Registry();
		List<TransportRouteResult> res = new ArrayList<>();
		for (GtfsItinerary it : itineraries) {
			GtfsResult r = convert(it, reg);
			if (r != null) {
				res.add(r);
			}
		}
		return res;
	}

	/** Null for a null, walk-only or malformed itinerary. */
	public GtfsResult convert(GtfsItinerary itinerary) {
		return convert(itinerary, new Registry());
	}

	private GtfsResult convert(GtfsItinerary it, Registry reg) {
		if (it == null || it.legs == null) {
			return null;
		}
		for (GtfsLeg leg : it.legs) {
			if (leg == null || (leg.type == GtfsLeg.Type.RIDE && (leg.stops == null || leg.stops.length < 2
					|| leg.arrivalSec == null || leg.departureSec == null
					|| leg.arrivalSec.length != leg.stops.length || leg.departureSec.length != leg.stops.length))) {
				return null;
			}
		}
		ZoneId zone = zone();
		GtfsResult res = new GtfsResult(cfg, it, zone);
		res.setRouteTime(Math.max(0, it.arriveSec - it.departSec));
		long dayStart = localMidnight(it.departSec);
		double pendingWalk = 0;
		for (GtfsLeg leg : it.legs) {
			if (leg.type == GtfsLeg.Type.WALK) {
				pendingWalk += leg.walkMeters;
				continue;
			}
			GtfsRoute route = buildRoute(leg, reg);
			GtfsSegment seg = new GtfsSegment();
			seg.route = route;
			seg.start = 0;
			seg.end = route.getForwardStops().size() - 1;
			seg.walkDist = pendingWalk; // the planner's convention: walk BEFORE this segment
			seg.walkTime = cfg.walkSpeed > 0 ? pendingWalk / cfg.walkSpeed : 0;
			seg.travelTime = (int) Math.max(0, leg.endSec - leg.startSec);
			seg.travelDistApproximate = route.getDistance();
			seg.depTime = (int) ((leg.startSec - dayStart) / 10);
			seg.zone = zone;
			seg.boardSec = leg.startSec;
			seg.alightSec = leg.endSec;
			seg.tripIdx = leg.tripIdx;
			seg.routeIdx = leg.routeIdx;
			res.addSegment(seg);
			pendingWalk = 0;
		}
		if (res.getSegments().isEmpty()) {
			return null;
		}
		res.setFinishWalkDist(pendingWalk);
		return res;
	}

	private ZoneId zone() {
		return index.getZone() != null ? index.getZone() : ZoneId.of("UTC");
	}

	private long localMidnight(long epochSec) {
		ZoneId zone = zone();
		return Instant.ofEpochSecond(epochSec).atZone(zone).toLocalDate().atStartOfDay(zone).toEpochSecond();
	}

	private GtfsRoute buildRoute(GtfsLeg leg, Registry reg) {
		GtfsRoute rt = new GtfsRoute();
		rt.setId(ROUTE_ID_BASE - reg.nextRoute++);
		rt.tripIdx = leg.tripIdx;
		rt.routeIdx = leg.routeIdx;
		boolean known = leg.routeIdx >= 0 && leg.routeIdx < index.getRouteCount();
		String shortName = known ? index.getRouteShortName(leg.routeIdx) : null;
		String longName = known ? index.getRouteLongName(leg.routeIdx) : null;
		String headsign = leg.tripIdx >= 0 && leg.tripIdx < index.getTripCount() ? index.getTripHeadsign(leg.tripIdx) : null;
		String ref = !isEmpty(shortName) ? shortName : !isEmpty(longName) ? longName : known ? "R" + leg.routeIdx : "?";
		rt.setRef(ref);
		rt.setName(!isEmpty(headsign) ? headsign : !isEmpty(longName) ? longName : ref);
		rt.headsign = headsign == null ? "" : headsign;
		rt.setType(known ? mapType(index.getRouteType(leg.routeIdx)) : "bus");
		rt.colorRgb = known ? index.getRouteColor(leg.routeIdx) : GtfsIndex.NO_COLOR;
		rt.setColor(null);
		int n = leg.stops.length;
		List<TransportStop> stops = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			stops.add(stopFor(leg.stops[i], rt, reg));
		}
		rt.stopIdx = leg.stops.clone();
		rt.arrivalSec = leg.arrivalSec.clone();
		rt.departureSec = leg.departureSec.clone();
		rt.setForwardStops(stops);
		rt.setLocation(stops.get(0).getLocation());
		rt.setDistance(polylineLength(stops));
		Way way = new Way(rt.getId());
		for (TransportStop s : stops) {
			way.addNode(new Node(s.getLocation().getLatitude(), s.getLocation().getLongitude(), -1));
		}
		rt.addWay(way);
		return rt;
	}

	private TransportStop stopFor(int stop, GtfsRoute route, Registry reg) {
		TransportStop ts = reg.stops.get(stop);
		if (ts == null) {
			ts = new TransportStop();
			ts.setId(STOP_ID_BASE - reg.nextStop++);
			String name = index.getStopName(stop);
			ts.setName(name == null ? "" : name);
			double lat = index.getStopLat(stop), lon = index.getStopLon(stop);
			ts.setLocation(lat, lon);
			ts.x31 = MapUtils.get31TileNumberX(lon);
			ts.y31 = MapUtils.get31TileNumberY(lat);
			reg.stops.put(stop, ts);
		}
		if (ts.getRoutes() == null || !ts.getRoutes().contains(route)) {
			ts.addRoute(route);
		}
		return ts;
	}

	private static int polylineLength(List<TransportStop> stops) {
		double d = 0;
		for (int i = 1; i < stops.size(); i++) {
			d += MapUtils.getDistance(stops.get(i - 1).getLocation(), stops.get(i).getLocation());
		}
		return (int) Math.round(d);
	}

	private static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	/** GTFS route_type (basic and extended) to the type strings the UI knows; never null. */
	public static String mapType(int t) {
		switch (t) {
			case 0: case 5: case 900:
				return "tram";
			case 1: case 400: case 401: case 402: case 403: case 404: case 405:
				return "subway";
			case 2:
				return "train";
			case 4: case 1000:
				return "ferry";
			case 6: case 7:
				return "funicular";
			case 11: case 800:
				return "trolleybus";
			case 12:
				return "monorail";
			default:
				return t >= 100 && t < 200 ? "train" : "bus";
		}
	}
}
