package net.osmand.router;

import java.util.ArrayList;
import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

// Restrictions whose via is a chain of ways (#12537), checked against the roads the search path has already passed.
// The record of the restriction lies on "from":
// - forward search: "from" is one of the previous roads of the path, the roads after it are compared with the vias;
// - backward search: "from" is the candidate road itself, the path after it (its parent routes) is compared.
// The visited segments keep no history, so a path that came from "from" and a path that entered the via ways from a
// side road merge on the first via segment reached: the route never breaks a restriction, but can take a detour.
class ViaChainRestrictions {

	// ponytail: the longest chain checked, a restriction with more via ways is ignored as before
	private static final int MAX_PATH_ROADS = 8;

	// true if moving from the road of "current" onto "next" at the node of nodeRoads breaks a restriction
	static boolean isRestricted(RouteSegment current, RouteDataObject next, RouteSegment nodeRoads, boolean reverseWaySearch) {
		long[] path = pathRoads(current);
		if (!reverseWaySearch) {
			// path[0] is the current road, path[d] - the road d steps before it
			for (int d = 0; d < path.length; d++) {
				RouteDataObject from = d == 0 ? current.getRoad() : roadAt(current, d);
				for (int k = 0; from != null && k < from.getRestrictionLength(); k++) {
					long[] vias = from.getRestrictionViaWays(k);
					if (vias != null && isRestrictedAfter(from, k, vias, path, d, next.getId(), nodeRoads)) {
						return true;
					}
				}
			}
			return false;
		}
		// next is before current in the direction of travel, path is the rest of the route after current
		for (int k = 0; k < next.getRestrictionLength(); k++) {
			long[] vias = next.getRestrictionViaWays(k);
			if (vias == null) {
				continue;
			}
			boolean onlyRestriction = next.getRestrictionType(k) >= MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN;
			if (path[0] != vias[0]) {
				if (onlyRestriction && isAtNode(nodeRoads, vias[0])) {
					return true;
				}
				continue;
			}
			for (int j = 1; j <= vias.length; j++) {
				if (j >= path.length) {
					break; // the rest of the route is not known yet
				}
				long expected = j < vias.length ? vias[j] : next.getRestrictionId(k);
				if (path[j] != expected) {
					if (onlyRestriction) {
						return true;
					}
					break;
				}
				if (j == vias.length && !onlyRestriction) {
					return true;
				}
			}
		}
		return false;
	}

	// true if the route joined from the forward and the backward search at their meeting segment breaks a restriction
	// whose via chain spans the meeting point (each of the searches sees only its own part of the chain)
	static boolean isRestrictedAtMeeting(RouteSegment forward, RouteSegment backward) {
		List<RouteDataObject> roads = new ArrayList<>();
		for (RouteSegment s = forward; s != null && roads.size() < MAX_PATH_ROADS; s = s.getParentRoute()) {
			if (roads.isEmpty() || roads.get(0).getId() != s.getRoad().getId()) {
				roads.add(0, s.getRoad());
			}
		}
		int forwardRoads = roads.size();
		for (RouteSegment s = backward; s != null && roads.size() < forwardRoads + MAX_PATH_ROADS; s = s.getParentRoute()) {
			if (roads.isEmpty() || roads.get(roads.size() - 1).getId() != s.getRoad().getId()) {
				roads.add(s.getRoad());
			}
		}
		for (int p = 0; p + 1 < roads.size(); p++) {
			RouteDataObject from = roads.get(p);
			for (int k = 0; k < from.getRestrictionLength(); k++) {
				long[] vias = from.getRestrictionViaWays(k);
				if (vias == null || roads.get(p + 1).getId() != vias[0]) {
					continue;
				}
				boolean onlyRestriction = from.getRestrictionType(k) >= MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN;
				for (int j = 1; j <= vias.length && p + 1 + j < roads.size(); j++) {
					long expected = j < vias.length ? vias[j] : from.getRestrictionId(k);
					if (roads.get(p + 1 + j).getId() != expected) {
						if (onlyRestriction) {
							return true;
						}
						break;
					}
					if (j == vias.length && !onlyRestriction) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// a restriction of "from" (d roads before the current one) and the roads passed after it
	private static boolean isRestrictedAfter(RouteDataObject from, int k, long[] vias, long[] path, int d, long nextId,
			RouteSegment nodeRoads) {
		boolean onlyRestriction = from.getRestrictionType(k) >= MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN;
		if (d == 0) {
			return onlyRestriction && nextId != vias[0] && isAtNode(nodeRoads, vias[0]);
		}
		if (d > vias.length) {
			return false;
		}
		for (int i = 0; i < d; i++) {
			if (vias[i] != path[d - 1 - i]) {
				return false;
			}
		}
		long expected = d < vias.length ? vias[d] : from.getRestrictionId(k);
		return onlyRestriction ? nextId != expected : d == vias.length && nextId == expected;
	}

	// ids of the different roads along the parent routes, starting with the road of the segment
	private static long[] pathRoads(RouteSegment segment) {
		long[] roads = new long[MAX_PATH_ROADS];
		int size = 0;
		for (RouteSegment s = segment; s != null && size < roads.length; s = s.getParentRoute()) {
			if (size == 0 || roads[size - 1] != s.getRoad().getId()) {
				roads[size++] = s.getRoad().getId();
			}
		}
		long[] result = new long[size];
		System.arraycopy(roads, 0, result, 0, size);
		return result;
	}

	private static RouteDataObject roadAt(RouteSegment segment, int index) {
		int i = 0;
		long roadId = segment.getRoad().getId();
		for (RouteSegment s = segment; s != null; s = s.getParentRoute()) {
			if (s.getRoad().getId() != roadId) {
				roadId = s.getRoad().getId();
				if (++i == index) {
					return s.getRoad();
				}
			}
		}
		return null;
	}

	private static boolean isAtNode(RouteSegment nodeRoads, long roadId) {
		for (RouteSegment s = nodeRoads; s != null; s = s.getNext()) {
			if (s.getRoad().getId() == roadId) {
				return true;
			}
		}
		return false;
	}
}
