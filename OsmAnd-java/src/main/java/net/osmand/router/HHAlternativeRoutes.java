package net.osmand.router;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.HHRouteDataStructure.HHNetworkRouteRes;
import net.osmand.router.HHRouteDataStructure.HHNetworkSegmentRes;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.HHRouteDataStructure.NetworkDBSegment;


/* ======================= ALTERNATIVE ROUTES (plateau / via-node) =======================
 * Reuses the two shortest-path trees that the bidirectional HH search has already built,
 * so no extra Dijkstra run is needed - only the horizon of the main search is extended to
 * (1 + ALT_STRETCH) * opt (see runRoutingWithInitQueue).
 *
 * A candidate is a "via node" v settled by both trees; its route is sp(s,v) + sp(v,t) and is
 * assembled by the existing createRouteSegmentFromFinalPoint(). The plateau of v is the maximal
 * chain of hub-graph edges around v belonging to BOTH trees - the stretch where the alternative
 * is simultaneously the best way to get there and the best way to go on, i.e. a road a driver
 * would actually name. A detour around one block has a zero plateau.
 *
 * Selection runs in two stages because hub-graph edges are shortcuts several km long: two
 * different shortcuts may still cover the same streets, so hub-level sharing underestimates the
 * real overlap (measured: 20% by hubs vs 76% by geometry). Stage 1 filters cheaply on the hub
 * graph, stage 2 expands candidates one by one and checks the real road segments, stopping as
 * soon as ALT_MAX_COUNT routes are accepted.
 */
public class HHAlternativeRoutes<T extends NetworkDBPoint> {

	private final HHRoutePlanner<T> planner;

	public HHAlternativeRoutes(HHRoutePlanner<T> planner) {
		this.planner = planner;
	}

	/** attempts to expand one candidate before giving up on it */
	private static final int ALT_EXPAND_RETRIES = 3;

	private static class AltCandidate {
		NetworkDBPoint via;
		double cost;
		double plateau;
		double sharing;
		List<NetworkDBPoint> path;
		TLongSet edges;
	}

	void calcAlternativeRoute(HHRoutingContext<T> hctx, HHNetworkRouteRes route,
			LatLon start, LatLon end, RouteCalculationProgress progress, RouteResultPreparation rrp)
			throws SQLException, IOException, InterruptedException {
		HHRoutingConfig cfg = hctx.config;
		double optCost = route.getHHRoutingTime();
		if (optCost <= 0 || cfg.ALT_MAX_COUNT <= 0) {
			return;
		}
		double maxCost = optCost * (1 + cfg.ALT_STRETCH);
		// ---- 1. via-node candidates: settled by both trees and within the stretch limit ----
		List<T> settled = new ArrayList<>();
		for (T p : hctx.queueAdded) {
			if (p.rtPos != null && p.rtRev != null && p.rtPos.rtVisited && p.rtRev.rtVisited) {
				double c = p.rtPos.rtDistanceFromStart + p.rtRev.rtDistanceFromStart;
				if (c > 0 && c <= maxCost) {
					settled.add(p);
				}
			}
		}
		if (settled.isEmpty()) {
			return;
		}
		// ---- 2. plateau length of every candidate ----
		// plateauFwd(v) needs its parent computed first, so iterate in order of increasing distance
		Map<NetworkDBPoint, Double> plateauFwd = new HashMap<>(), plateauBwd = new HashMap<>();
		List<T> byDistFromStart = new ArrayList<>(settled);
		byDistFromStart.sort(new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
				return Double.compare(a.rtPos.rtDistanceFromStart, b.rtPos.rtDistanceFromStart);
			}
		});
		for (T v : byDistFromStart) {
			NetworkDBPoint prev = v.rtPos.rtRouteToPoint;
			double val = 0;
			if (prev != null && prev.rtRev != null && prev.rtRev.rtRouteToPoint == v) {
				Double acc = plateauFwd.get(prev);
				val = (acc == null ? 0 : acc) + (v.rtPos.rtDistanceFromStart - prev.rtPos.rtDistanceFromStart);
			}
			plateauFwd.put(v, val);
		}
		List<T> byDistToEnd = new ArrayList<>(settled);
		byDistToEnd.sort(new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
				return Double.compare(a.rtRev.rtDistanceFromStart, b.rtRev.rtDistanceFromStart);
			}
		});
		for (T v : byDistToEnd) {
			NetworkDBPoint next = v.rtRev.rtRouteToPoint;
			double val = 0;
			if (next != null && next.rtPos != null && next.rtPos.rtRouteToPoint == v) {
				Double acc = plateauBwd.get(next);
				val = (acc == null ? 0 : acc) + (v.rtRev.rtDistanceFromStart - next.rtRev.rtDistanceFromStart);
			}
			plateauBwd.put(v, val);
		}
		// ---- 3. stage 1: cheap admissibility on the hub graph ----
		List<NetworkDBPoint> optNodes = new ArrayList<>();
		for (HHNetworkSegmentRes r : route.segments) {
			if (r.segment != null && r.segment.start != null && r.segment.end != null) {
				if (optNodes.isEmpty()) {
					optNodes.add(r.segment.start);
				}
				optNodes.add(r.segment.end);
			}
		}
		TLongSet optEdges = pathEdges(optNodes);
		List<AltCandidate> candidates = new ArrayList<>();
		TLongSet seenPaths = new TLongHashSet();
		for (T v : settled) {
			Double bwd = plateauBwd.get(v);
			Double fwd = plateauFwd.get(v);
			double cost = v.rtPos.rtDistanceFromStart + v.rtRev.rtDistanceFromStart;
			double plateau = (fwd == null ? 0 : fwd) + (bwd == null ? 0 : bwd);
			if (!isPlateauRepresentative(v, plateau, plateauFwd)) {
				continue;
			}
			if (plateau < cfg.ALT_MIN_PLATEAU * cost) {
				continue;
			}
			List<NetworkDBPoint> path = pathThrough(v);
			if (!isSimple(path)) {
				// sp(s,v) and sp(v,t) are each optimal, but their concatenation is not necessarily a
				// simple path: when both halves run over the same roads the candidate drives out and
				// turns back. Cheap check first, the exact one is on the geometry in stage 2.
				continue;
			}
			long signature = 0;
			for (NetworkDBPoint n : path) {
				signature = signature * 1000003L + n.index;
			}
			if (!seenPaths.add(signature)) {
				continue;
			}
			double sharing = sharedCost(path, optEdges) / cost;
			if (sharing > cfg.ALT_MAX_SHARING) {
				continue;
			}
			AltCandidate c = new AltCandidate();
			c.via = v;
			c.cost = cost;
			c.plateau = plateau;
			c.path = path;
			c.sharing = sharing;
			c.edges = pathEdges(path);
			candidates.add(c);
		}
		if (candidates.isEmpty()) {
			return;
		}
		// rank by "as different as possible for as little extra time as possible"
		final double finalOptCost = optCost;
		final double costWeight = cfg.ALT_RANK_COST_WEIGHT;
		candidates.sort(new Comparator<AltCandidate>() {
			@Override
			public int compare(AltCandidate x, AltCandidate y) {
				return Double.compare(rankScore(y, finalOptCost, costWeight), rankScore(x, finalOptCost, costWeight));
			}
		});
		// alternatives within ALT_STRETCH_PREFERRED are proposed before the merely acceptable ones
		List<AltCandidate> ordered = new ArrayList<>(candidates.size());
		double preferredCost = optCost * (1 + cfg.ALT_STRETCH_PREFERRED);
		for (AltCandidate c : candidates) {
			if (c.cost <= preferredCost) {
				ordered.add(c);
			}
		}
		for (AltCandidate c : candidates) {
			if (c.cost > preferredCost) {
				ordered.add(c);
			}
		}
		// ---- 4. stage 2: expand lazily and verify on the real road segments ----
		List<Map<Long, Double>> acceptedGeometry = new ArrayList<>();
		Map<Long, Double> mainGeometry = roadSegments(detailedSegments(route));
		double mainLength = totalLength(mainGeometry);
		double minOwnRoads = Math.max(cfg.ALT_MIN_DISTINCT_ABS, cfg.ALT_MIN_DISTINCT_REL * mainLength);
		// Avoiding is asked for less strictly than offering: replacing a good stretch of a long route
		// is useful even when most of the main route stays. The point of this second threshold is to
		// reject an alternative that contains the whole main route and only adds a loop to it.
		double minAvoided = Math.max(cfg.ALT_MIN_DISTINCT_ABS, cfg.ALT_MIN_DISTINCT_REL * mainLength / 2);
		int expanded = 0;
		for (AltCandidate c : ordered) {
			if (route.altRoutes.size() >= cfg.ALT_MAX_COUNT || expanded >= cfg.ALT_MAX_EXPAND) {
				break;
			}
			if (progress != null && progress.isCancelled) {
				return;
			}
			// retrieveSegmentsGeometry bails out half way when a shortcut cannot be expanded, or when
			// its detailed cost turns out higher than the hub graph promised; it corrects the segment
			// cost on the way out, so the next attempt usually succeeds. The main route recalculates
			// in exactly the same situation - an alternative simply retries a few times.
			HHNetworkRouteRes alt = null;
			boolean needsRecalculation = true;
			for (int attempt = 0; attempt < ALT_EXPAND_RETRIES && needsRecalculation
					&& expanded < cfg.ALT_MAX_EXPAND; attempt++) {
				alt = planner.createRouteSegmentFromFinalPoint(hctx, c.via);
				if (alt.segments.isEmpty()) {
					break;
				}
				needsRecalculation = planner.retrieveSegmentsGeometry(hctx, rrp, alt, true, progress, true);
				expanded++;
			}
			if (progress != null && progress.isCancelled) {
				return;
			}
			if (alt == null || alt.segments.isEmpty()) {
				continue;
			}
			if (needsRecalculation || !isFullyExpanded(alt)) {
				// still incomplete: the rest of the segments have no geometry and would be drawn as
				// straight lines between hub points, so the candidate is not usable
				planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0, "  alt dropped: incomplete geometry (+%.1f%%)\n",
						100 * (c.cost / optCost - 1));
				continue;
			}
			// shortcut costs can be optimistic; now that the roads are known, check the real one
			double realCost = alt.getHHRoutingDetailed();
			if (realCost > maxCost) {
				planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0, "  alt dropped: real cost +%.1f%% over the limit (hub graph said +%.1f%%)\n",
						100 * (realCost / optCost - 1), 100 * (c.cost / optCost - 1));
				continue;
			}
			c.cost = realCost;
			// prepare the candidate exactly like the main route: turns, distances and the clean-up of
			// small manoeuvres all happen here, and the checks below must see what will be displayed
			planner.prepareRouteResults(hctx, alt, start, end, rrp);
			if (cfg.ROUTE_ALL_ALT_SEGMENTS && !alt.detailed.isEmpty()) {
				alt.detailed = rrp.prepareResult(hctx.rctx, alt.detailed).detailed;
			}
			List<RouteSegmentResult> detailed = alt.detailed;
			if (detailed.isEmpty()) {
				continue;
			}
			double retraced = retracedLength(detailed);
			if (retraced > cfg.ALT_MAX_RETRACED) {
				// sp(s,v) and sp(v,t) are each optimal, but their concatenation is not necessarily a
				// simple path: when the two halves run over the same roads the candidate drives out
				// and turns back, which reads as a bug on the map
				planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0, "  alt dropped: drives %.0f m of its own roads twice (+%.1f%%)\n",
						retraced, 100 * (c.cost / optCost - 1));
				continue;
			}
			// Both directions matter. "Own roads" says the alternative offers something new; "roads
			// avoided" says it actually replaces a part of the route it is an alternative to. Without
			// the second one an alternative that contains the whole main route plus a loop passes.
			Map<Long, Double> altGeometry = roadSegments(detailed);
			double altLength = totalLength(altGeometry);
			double ownRoads = altLength - sharedGeometry(altGeometry, mainGeometry);
			double avoidedRoads = mainLength - sharedGeometry(mainGeometry, altGeometry);
			for (Map<Long, Double> accepted : acceptedGeometry) {
				ownRoads = Math.min(ownRoads, altLength - sharedGeometry(altGeometry, accepted));
				avoidedRoads = Math.min(avoidedRoads,
						totalLength(accepted) - sharedGeometry(accepted, altGeometry));
			}
			if (ownRoads < minOwnRoads || avoidedRoads < minAvoided) {
				planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0,
						"  alt rejected: +%.1f%%, own roads %.1f km (need %.1f), avoids %.1f km (need %.1f)\n",
						100 * (c.cost / optCost - 1), ownRoads / 1000, minOwnRoads / 1000,
						avoidedRoads / 1000, minAvoided / 1000);
				continue;
			}
			route.altRoutes.add(alt);
			acceptedGeometry.add(roadSegments(detailed));
			planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0,
					"  alt accepted: +%.1f%%, plateau %.0f%%, own roads %.1f km, avoids %.1f km\n",
					100 * (c.cost / optCost - 1), 100 * c.plateau / c.cost, ownRoads / 1000, avoidedRoads / 1000);
		}
	}

	private static double rankScore(AltCandidate c, double optCost, double costWeight) {
		return (1 - c.sharing) - costWeight * (c.cost / optCost - 1);
	}

	/** full s -> t sequence of hub points through the given via node, taken from both trees */
	private List<NetworkDBPoint> pathThrough(NetworkDBPoint via) {
		List<NetworkDBPoint> path = new ArrayList<>();
		NetworkDBPoint it = via;
		while (it != null) {
			path.add(it);
			it = it.rt(false).rtRouteToPoint;
		}
		Collections.reverse(path);
		it = via.rt(true).rtRouteToPoint;
		while (it != null) {
			path.add(it);
			it = it.rt(true).rtRouteToPoint;
		}
		return path;
	}

	private static final long ALT_START_KEY = -1, ALT_END_KEY = -2, ALT_KEY_MULT = 4000000000L;

	private static long edgeKey(long from, long to) {
		return from * ALT_KEY_MULT + to;
	}

	/** edge keys of a full s -> t path including the first/last mile anchors */
	private static TLongSet pathEdges(List<NetworkDBPoint> path) {
		TLongSet edges = new TLongHashSet();
		if (path.isEmpty()) {
			return edges;
		}
		edges.add(edgeKey(ALT_START_KEY, path.get(0).index));
		for (int i = 1; i < path.size(); i++) {
			edges.add(edgeKey(path.get(i - 1).index, path.get(i).index));
		}
		edges.add(edgeKey(path.get(path.size() - 1).index, ALT_END_KEY));
		return edges;
	}

	/** cost of the part of `path` that runs over the given edges (first/last mile included) */
	private double sharedCost(List<NetworkDBPoint> path, TLongSet edges) {
		if (path.isEmpty()) {
			return 0;
		}
		double shared = 0;
		NetworkDBPoint first = path.get(0), last = path.get(path.size() - 1);
		if (edges.contains(edgeKey(ALT_START_KEY, first.index))) {
			shared += first.rt(false).rtDistanceFromStart;
		}
		if (edges.contains(edgeKey(last.index, ALT_END_KEY))) {
			shared += last.rt(true).rtDistanceFromStart;
		}
		for (int i = 1; i < path.size(); i++) {
			NetworkDBPoint a = path.get(i - 1), b = path.get(i);
			if (edges.contains(edgeKey(a.index, b.index))) {
				shared += hubEdgeCost(a, b);
			}
		}
		return shared;
	}

	private double hubEdgeCost(NetworkDBPoint a, NetworkDBPoint b) {
		if (b.rt(false).rtRouteToPoint == a) {
			return b.rt(false).rtDistanceFromStart - a.rt(false).rtDistanceFromStart;
		}
		if (a.rt(true).rtRouteToPoint == b) {
			return a.rt(true).rtDistanceFromStart - b.rt(true).rtDistanceFromStart;
		}
		NetworkDBSegment segment = a.getSegment(b, true);
		return segment == null ? 0 : segment.dist;
	}

	/**
	 * All nodes of one plateau describe the same route, so only one of them is kept. The chosen one
	 * is the node closest to the middle of the chain: there both trees agree on the edge coming in
	 * and on the edge going out, which is what makes the two halves of the candidate join smoothly
	 * instead of turning back on themselves. At the end of a chain they need not agree.
	 */
	private static boolean isPlateauRepresentative(NetworkDBPoint v, double plateau,
			Map<NetworkDBPoint, Double> plateauFwd) {
		if (plateau <= 0) {
			return true; // no plateau at all, nothing to deduplicate
		}
		Double fwd = plateauFwd.get(v);
		double own = Math.abs((fwd == null ? 0 : fwd) - plateau / 2);
		// every node of the chain carries the same plateau length, so a neighbour that sits closer
		// to the middle is the better representative
		NetworkDBPoint prev = v.rt(false).rtRouteToPoint;
		if (prev != null && plateauFwd.containsKey(prev) && prev.rt(true).rtRouteToPoint == v
				&& Math.abs(plateauFwd.get(prev) - plateau / 2) < own) {
			return false;
		}
		NetworkDBPoint next = v.rt(true).rtRouteToPoint;
		if (next != null && plateauFwd.containsKey(next) && next.rt(false).rtRouteToPoint == v
				&& Math.abs(plateauFwd.get(next) - plateau / 2) < own) {
			return false;
		}
		return true;
	}

	/** no hub point is visited twice, i.e. the two halves of the candidate do not overlap */
	private static boolean isSimple(List<NetworkDBPoint> path) {
		TLongSet seen = new TLongHashSet();
		for (NetworkDBPoint p : path) {
			if (!seen.add(p.index)) {
				return false;
			}
		}
		return true;
	}

	/** length of the road pieces that `segments` drives more than once */
	private static double retracedLength(List<RouteSegmentResult> segments) {
		TLongSet seen = new TLongHashSet();
		double retraced = 0;
		for (RouteSegmentResult r : segments) {
			RouteDataObject o = r.getObject();
			int i = r.getStartPointIndex(), end = r.getEndPointIndex();
			int step = i <= end ? 1 : -1;
			while (i != end) {
				int j = i + step;
				if (!seen.add(o.getId() * 4096L + Math.min(i, j))) {
					retraced += HHRoutePlanner.squareRootDist31(o.getPoint31XTile(i), o.getPoint31YTile(i),
							o.getPoint31XTile(j), o.getPoint31YTile(j));
				}
				i = j;
			}
		}
		return retraced;
	}

	/** every hub-graph segment was expanded into real roads, so nothing will be drawn as a straight line */
	private static boolean isFullyExpanded(HHNetworkRouteRes route) {
		for (HHNetworkSegmentRes s : route.segments) {
			if (s.list == null || s.list.isEmpty()) {
				return false;
			}
		}
		return !route.segments.isEmpty();
	}

	private static List<RouteSegmentResult> detailedSegments(HHNetworkRouteRes route) {
		List<RouteSegmentResult> l = new ArrayList<>();
		for (HHNetworkSegmentRes s : route.segments) {
			if (s.list != null) {
				l.addAll(s.list);
			}
		}
		return l;
	}

	/**
	 * Road point pair -> covered length. RouteSegmentResult.getDistance() is only filled in
	 * prepareResult(), so the length is measured on the geometry here.
	 */
	private static Map<Long, Double> roadSegments(List<RouteSegmentResult> segments) {
		Map<Long, Double> m = new HashMap<>();
		for (RouteSegmentResult r : segments) {
			RouteDataObject o = r.getObject();
			int i = r.getStartPointIndex(), end = r.getEndPointIndex();
			int step = i <= end ? 1 : -1;
			while (i != end) {
				int j = i + step;
				long key = o.getId() * 4096L + Math.min(i, j);
				double d = HHRoutePlanner.squareRootDist31(o.getPoint31XTile(i), o.getPoint31YTile(i),
						o.getPoint31XTile(j), o.getPoint31YTile(j));
				Double prev = m.get(key);
				m.put(key, (prev == null ? 0 : prev) + d);
				i = j;
			}
		}
		return m;
	}

	private static double totalLength(Map<Long, Double> segments) {
		double d = 0;
		for (Double v : segments.values()) {
			d += v;
		}
		return d;
	}

	/** length of the roads that the two routes have in common */
	private static double sharedGeometry(Map<Long, Double> segments, Map<Long, Double> reference) {
		double common = 0;
		for (Map.Entry<Long, Double> e : segments.entrySet()) {
			Double v = reference.get(e.getKey());
			if (v != null) {
				common += Math.min(v, e.getValue());
			}
		}
		return common;
	}

}
