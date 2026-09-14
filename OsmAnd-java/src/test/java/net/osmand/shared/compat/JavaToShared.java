package net.osmand.shared.compat;

import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.RouteRegion;
import net.osmand.shared.routing.RouteSegmentResult;
import net.osmand.shared.routing.TurnType;
import net.osmand.shared.util.collections.KTIntObjectMap;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the OsmAnd-shared copy of a java routing object, field for field, so the two can be given
 * the same input. Test scaffolding only: it copies and never corrects, so a difference it introduces
 * shows up as a failed comparison rather than hiding one.
 *
 * Regions and roads are converted once per java instance, because the java code compares roads by
 * identity in places and the copies have to agree on what is the same road.
 */
public final class JavaToShared {

	private final Map<BinaryMapRouteReaderAdapter.RouteRegion, RouteRegion> regions = new IdentityHashMap<>();
	private final Map<net.osmand.binary.RouteDataObject, RouteDataObject> roads = new IdentityHashMap<>();

	public RouteRegion region(BinaryMapRouteReaderAdapter.RouteRegion j) {
		RouteRegion k = regions.get(j);
		if (k == null) {
			k = new RouteRegion();
			k.setName(j.getName());
			k.setFilePointer(j.getFilePointer());
			k.setLength(j.getLength());
			List<BinaryMapRouteReaderAdapter.RouteTypeRule> rules = j.routeEncodingRules;
			for (int i = 0; i < rules.size(); i++) {
				BinaryMapRouteReaderAdapter.RouteTypeRule rule = rules.get(i);
				if (rule != null) {
					k.initRouteEncodingRule(i, rule.getTag(), rule.getValue());
				}
			}
			// what the reader does once the rules are in: give every condition the id of its value's rule
			k.completeRouteEncodingRules();
			regions.put(j, k);
		}
		return k;
	}

	public RouteDataObject road(net.osmand.binary.RouteDataObject j) {
		RouteDataObject k = roads.get(j);
		if (k == null) {
			k = new RouteDataObject(region(j.region));
			k.id = j.id;
			k.pointsX = copy(j.pointsX);
			k.pointsY = copy(j.pointsY);
			k.types = copy(j.types);
			k.restrictions = copy(j.restrictions);
			k.restrictionsVia = copy(j.restrictionsVia);
			k.pointTypes = copy(j.pointTypes);
			k.pointNameTypes = copy(j.pointNameTypes);
			k.pointNames = copy(j.pointNames);
			k.nameIds = copy(j.nameIds);
			if (j.names != null) {
				KTIntObjectMap<String> names = new KTIntObjectMap<>();
				for (int key : j.names.keys()) {
					names.put(key, j.names.get(key));
				}
				k.names = names;
			}
			k.heightDistanceArray = j.heightDistanceArray == null ? null : j.heightDistanceArray.clone();
			k.heightByCurrentLocation = j.heightByCurrentLocation;
			roads.put(j, k);
		}
		return k;
	}

	public TurnType turn(net.osmand.router.TurnType j) {
		if (j == null) {
			return null;
		}
		TurnType k = new TurnType(j.getValue(), j.getExitOut(), j.getTurnAngle(), j.isSkipToSpeak(),
				copy(j.getLanes()), j.isPossibleLeftTurn(), j.isPossibleRightTurn());
		if (j.getOtherTurnAngles() != null) {
			k.setOtherTurnAngles(new ArrayList<>(j.getOtherTurnAngles()));
		}
		return k;
	}

	/** The segment with its road, indices, timing, turn and attached roads; the attached ones carry no turns of their own. */
	public RouteSegmentResult segment(net.osmand.router.RouteSegmentResult j) {
		RouteSegmentResult k = new RouteSegmentResult(road(j.getObject()), j.getStartPointIndex(), j.getEndPointIndex());
		k.setSegmentTime(j.getSegmentTime());
		k.setRoutingTime(j.getRoutingTime());
		k.setSegmentSpeed(j.getSegmentSpeed());
		k.setDistance(j.getDistance());
		k.setTurnType(turn(j.getTurnType()));
		k.setGpxPointIndex(j.getGpxPointIndex());
		if (j.getDescription(false) != null && !j.getDescription(false).isEmpty()
				|| j.getDescription(true) != null && !j.getDescription(true).isEmpty()) {
			k.setDescription(j.getDescription(false), j.getDescription(true));
		}
		int from = Math.min(j.getStartPointIndex(), j.getEndPointIndex());
		int to = Math.max(j.getStartPointIndex(), j.getEndPointIndex());
		for (int i = from; i <= to; i++) {
			for (net.osmand.router.RouteSegmentResult attached : j.getAttachedRoutes(i)) {
				k.attachRoute(i, segment(attached));
			}
		}
		return k;
	}

	public List<RouteSegmentResult> segments(List<net.osmand.router.RouteSegmentResult> j) {
		List<RouteSegmentResult> k = new ArrayList<>(j.size());
		for (net.osmand.router.RouteSegmentResult s : j) {
			k.add(segment(s));
		}
		return k;
	}

	private static int[] copy(int[] a) {
		return a == null ? null : a.clone();
	}

	private static long[] copy(long[] a) {
		return a == null ? null : a.clone();
	}

	private static int[][] copy(int[][] a) {
		if (a == null) {
			return null;
		}
		int[][] r = new int[a.length][];
		for (int i = 0; i < a.length; i++) {
			r[i] = copy(a[i]);
		}
		return r;
	}

	private static String[][] copy(String[][] a) {
		if (a == null) {
			return null;
		}
		String[][] r = new String[a.length][];
		for (int i = 0; i < a.length; i++) {
			r[i] = a[i] == null ? null : a[i].clone();
		}
		return r;
	}
}
