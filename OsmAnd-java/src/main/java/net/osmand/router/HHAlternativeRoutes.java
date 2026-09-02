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
import net.osmand.router.HHRouteDataStructure.NetworkDBPointRouteInfo;
import net.osmand.router.HHRouteDataStructure.NetworkDBSegment;

/**
 * Alternative routes by the plateau (via-node) method.
 *
 * Reuses the two shortest-path trees that the bidirectional HH search has already built, so no
 * extra Dijkstra run is needed - only the horizon of the main search is extended to
 * (1 + ALT_STRETCH) * opt (see HHRoutePlanner.runRoutingWithInitQueue).
 *
 * A candidate is a "via node" v settled by both trees; its route is sp(s,v) + sp(v,t) and is
 * assembled by the planner's createRouteSegmentFromFinalPoint(). The plateau of v is the maximal
 * chain of hub-graph edges around v belonging to BOTH trees - the stretch where the alternative is
 * simultaneously the best way to get there and the best way to go on, i.e. a road a driver would
 * actually name. A detour around one block has a zero plateau.
 *
 * Selection runs in two stages because hub-graph edges are shortcuts several km long: two different
 * shortcuts may still cover the same streets, so hub-level sharing underestimates the real overlap
 * (measured: 20% by hubs vs 76% by geometry). Stage 1 filters cheaply on the hub graph, stage 2
 * expands candidates one by one and checks the real road segments, stopping as soon as
 * ALT_MAX_COUNT routes are accepted.
 *
 * One instance serves one routing call - the plateau maps and the expansion budget below are the
 * state of that call.
 */
public class HHAlternativeRoutes<T extends NetworkDBPoint> {

	/** attempts to expand one candidate before giving up on it */
	private static final int ALT_EXPAND_RETRIES = 3;
	private static final long ALT_START_KEY = -1, ALT_END_KEY = -2, ALT_KEY_MULT = 4000000000L;

	private final HHRoutePlanner<T> planner;
	private final HHRoutingContext<T> hctx;
	private final HHRoutingConfig cfg;

	/** plateau length of a via node towards the start and towards the target */
	private final Map<NetworkDBPoint, Double> plateauFwd = new HashMap<>(), plateauBwd = new HashMap<>();
	private double optCost, maxCost;
	/** how much own and avoided road an alternative must show, derived from the main route's length */
	private double minOwnRoads, minAvoided;
	/** detailed expansions spent so far, against cfg.ALT_MAX_EXPAND */
	private int expanded;

	public HHAlternativeRoutes(HHRoutePlanner<T> planner, HHRoutingContext<T> hctx) {
		this.planner = planner;
		this.hctx = hctx;
		this.cfg = hctx.config;
	}

	private static class AltCandidate {
		NetworkDBPoint via;
		double cost;
		double plateau;
		double sharing;
		List<NetworkDBPoint> path;
	}

	/** how much road an alternative offers that the routes it competes with do not, and vice versa */
	private static class Distinctness {
		double ownRoads;
		double avoidedRoads;
	}

	void calcAlternativeRoute(HHNetworkRouteRes route, LatLon start, LatLon end,
			RouteCalculationProgress progress, RouteResultPreparation rrp)
			throws SQLException, IOException, InterruptedException {
		optCost = route.getHHRoutingTime();
		if (optCost <= 0 || cfg.ALT_MAX_COUNT <= 0) {
			return;
		}
		maxCost = optCost * (1 + cfg.ALT_STRETCH);
		List<T> settled = collectViaNodes();
		if (settled.isEmpty()) {
			return;
		}
		computePlateaus(settled);
		List<AltCandidate> candidates = selectCandidates(settled, route);
		if (candidates.isEmpty()) {
			return;
		}
		verifyAndAccept(rank(candidates), route, start, end, progress, rrp);
	}

	/** via-node candidates: settled by both trees and within the stretch limit */
	private List<T> collectViaNodes() {
		List<T> settled = new ArrayList<>();
		for (T p : hctx.queueAdded) {
			if (p.rtPos != null && p.rtRev != null && p.rtPos.rtVisited && p.rtRev.rtVisited) {
				double c = p.rtPos.rtDistanceFromStart + p.rtRev.rtDistanceFromStart;
				if (c > 0 && c <= maxCost) {
					settled.add(p);
				}
			}
		}
		return settled;
	}

	private void computePlateaus(List<T> settled) {
		accumulatePlateau(settled, false, plateauFwd);
		accumulatePlateau(settled, true, plateauBwd);
	}

	/**
	 * Walks the chain of edges that both trees agree on and accumulates its length per node. A node
	 * continues the chain of its parent when the opposite tree routes the parent back through it,
	 * so the value has to be read from the parent first - hence the sort by distance.
	 */
	private void accumulatePlateau(List<T> settled, final boolean rev, Map<NetworkDBPoint, Double> plateau) {
		List<T> byDist = new ArrayList<>(settled);
		byDist.sort(new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
				return Double.compare(info(a, rev).rtDistanceFromStart, info(b, rev).rtDistanceFromStart);
			}
		});
		for (T v : byDist) {
			NetworkDBPoint prev = info(v, rev).rtRouteToPoint;
			double val = 0;
			if (prev != null && info(prev, !rev) != null && info(prev, !rev).rtRouteToPoint == v) {
				Double acc = plateau.get(prev);
				val = (acc == null ? 0 : acc)
						+ (info(v, rev).rtDistanceFromStart - info(prev, rev).rtDistanceFromStart);
			}
			plateau.put(v, val);
		}
	}

	/**
	 * The point's search state in one direction, or null when that tree never reached it.
	 * {@link NetworkDBPoint#rt} would allocate the missing one instead of saying so.
	 */
	private NetworkDBPointRouteInfo info(NetworkDBPoint p, boolean rev) {
		return rev ? p.rtRev : p.rtPos;
	}

	/** stage 1: cheap admissibility on the hub graph */
	private List<AltCandidate> selectCandidates(List<T> settled, HHNetworkRouteRes route) {
		TLongSet optEdges = pathEdges(hubPath(route));
		List<AltCandidate> candidates = new ArrayList<>();
		TLongSet seenPaths = new TLongHashSet();
		for (T v : settled) {
			AltCandidate c = admissibleThrough(v, optEdges);
			if (c != null && seenPaths.add(signature(c.path))) {
				candidates.add(c);
			}
		}
		return candidates;
	}

	/** the candidate routed through this via node, or null when it fails one of the hub-graph rules */
	private AltCandidate admissibleThrough(T via, TLongSet optEdges) {
		Double fwd = plateauFwd.get(via);
		Double bwd = plateauBwd.get(via);
		double cost = via.rtPos.rtDistanceFromStart + via.rtRev.rtDistanceFromStart;
		double plateau = (fwd == null ? 0 : fwd) + (bwd == null ? 0 : bwd);
		if (!isPlateauRepresentative(via, plateau) || plateau < cfg.ALT_MIN_PLATEAU * cost) {
			return null;
		}
		List<NetworkDBPoint> path = pathThrough(via);
		if (!isSimple(path)) {
			// sp(s,v) and sp(v,t) are each optimal, but their concatenation is not necessarily a
			// simple path: when both halves run over the same roads the candidate drives out and
			// turns back. Cheap check first, the exact one is on the geometry in stage 2.
			return null;
		}
		double sharing = sharedCost(path, optEdges) / cost;
		if (sharing > cfg.ALT_MAX_SHARING) {
			return null;
		}
		AltCandidate c = new AltCandidate();
		c.via = via;
		c.cost = cost;
		c.plateau = plateau;
		c.path = path;
		c.sharing = sharing;
		return c;
	}

	/**
	 * Best first: as different as possible for as little extra time as possible, with everything
	 * within ALT_STRETCH_PREFERRED proposed before the merely acceptable candidates.
	 */
	private List<AltCandidate> rank(List<AltCandidate> candidates) {
		candidates.sort(new Comparator<AltCandidate>() {
			@Override
			public int compare(AltCandidate x, AltCandidate y) {
				return Double.compare(rankScore(y), rankScore(x));
			}
		});
		double preferredCost = optCost * (1 + cfg.ALT_STRETCH_PREFERRED);
		List<AltCandidate> ordered = new ArrayList<>(candidates.size());
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
		return ordered;
	}

	private double rankScore(AltCandidate c) {
		return (1 - c.sharing) - cfg.ALT_RANK_COST_WEIGHT * (c.cost / optCost - 1);
	}

	/** stage 2: expand candidates one by one and verify them on the real road segments */
	private void verifyAndAccept(List<AltCandidate> ordered, HHNetworkRouteRes route, LatLon start,
			LatLon end, RouteCalculationProgress progress, RouteResultPreparation rrp)
			throws SQLException, IOException, InterruptedException {
		// Distinctness is measured against the main route and against the alternatives already
		// accepted alike - an alternative has to be worth proposing next to each of them.
		List<Map<Long, Double>> accepted = new ArrayList<>();
		accepted.add(roadSegments(detailedSegments(route)));
		setDistinctnessThresholds(totalLength(accepted.get(0)));
		for (AltCandidate c : ordered) {
			if (route.altRoutes.size() >= cfg.ALT_MAX_COUNT || expanded >= cfg.ALT_MAX_EXPAND) {
				break;
			}
			HHNetworkRouteRes alt = expand(c, progress, rrp);
			if (isCancelled(progress)) {
				return;
			}
			if (alt == null) {
				continue;
			}
			Map<Long, Double> geometry = roadSegments(prepare(alt, start, end, rrp));
			Distinctness d = assess(c, alt.detailed, geometry, accepted);
			if (d == null) {
				continue;
			}
			route.altRoutes.add(alt);
			accepted.add(geometry);
			planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0,
					"  alt accepted: +%.1f%%, plateau %.0f%%, own roads %.1f km, avoids %.1f km\n",
					100 * (c.cost / optCost - 1), 100 * c.plateau / c.cost,
					d.ownRoads / 1000, d.avoidedRoads / 1000);
		}
	}

	private void setDistinctnessThresholds(double mainLength) {
		minOwnRoads = Math.max(cfg.ALT_MIN_DISTINCT_ABS, cfg.ALT_MIN_DISTINCT_REL * mainLength);
		// Avoiding is asked for less strictly than offering: replacing a good stretch of a long route
		// is useful even when most of the main route stays. The point of this second threshold is to
		// reject an alternative that contains the whole main route and only adds a loop to it.
		minAvoided = Math.max(cfg.ALT_MIN_DISTINCT_ABS, cfg.ALT_MIN_DISTINCT_REL * mainLength / 2);
	}

	/** how the candidate differs from the routes already on offer, or null when it must not be proposed */
	private Distinctness assess(AltCandidate c, List<RouteSegmentResult> detailed,
			Map<Long, Double> geometry, List<Map<Long, Double>> accepted) {
		if (detailed.isEmpty()) {
			return null;
		}
		double retraced = retracedLength(detailed);
		if (retraced > cfg.ALT_MAX_RETRACED) {
			// the hub-level isSimple() check misses this when the two halves overlap inside a single
			// shortcut: the candidate drives out and turns back, which reads as a bug on the map
			dropped(c, "drives %.0f m of its own roads twice", retraced);
			return null;
		}
		Distinctness d = distinctness(geometry, accepted);
		if (d.ownRoads < minOwnRoads || d.avoidedRoads < minAvoided) {
			dropped(c, "own roads %.1f km (need %.1f), avoids %.1f km (need %.1f)",
					d.ownRoads / 1000, minOwnRoads / 1000, d.avoidedRoads / 1000, minAvoided / 1000);
			return null;
		}
		return d;
	}

	/** the candidate expanded into real roads, or null when it turned out to be unusable */
	private HHNetworkRouteRes expand(AltCandidate c, RouteCalculationProgress progress,
			RouteResultPreparation rrp) throws SQLException, IOException, InterruptedException {
		// retrieveSegmentsGeometry bails out half way when a shortcut cannot be expanded, or when its
		// detailed cost turns out higher than the hub graph promised; it corrects the segment cost on
		// the way out, so the next attempt usually succeeds. The main route recalculates in exactly
		// the same situation - an alternative simply retries a few times.
		HHNetworkRouteRes alt = null;
		boolean needsRecalculation = true;
		for (int attempt = 0; attempt < ALT_EXPAND_RETRIES && needsRecalculation
				&& expanded < cfg.ALT_MAX_EXPAND; attempt++) {
			if (isCancelled(progress)) {
				return null;
			}
			alt = planner.createRouteSegmentFromFinalPoint(hctx, c.via);
			if (alt.segments.isEmpty()) {
				return null;
			}
			needsRecalculation = planner.retrieveSegmentsGeometry(hctx, rrp, alt, true, progress, true);
			expanded++;
		}
		if (alt == null || alt.segments.isEmpty()) {
			return null;
		}
		if (needsRecalculation || !isFullyExpanded(alt)) {
			// the rest of the segments have no geometry and would be drawn as straight lines between
			// hub points, so the candidate is not usable
			dropped(c, "incomplete geometry");
			return null;
		}
		// shortcut costs can be optimistic; now that the roads are known, check the real one
		double realCost = alt.getHHRoutingDetailed();
		if (realCost > maxCost) {
			dropped(c, "real cost +%.1f%% over the limit", 100 * (realCost / optCost - 1));
			return null;
		}
		c.cost = realCost;
		return alt;
	}

	/**
	 * Prepares the candidate exactly like the main route: turns, distances and the clean-up of small
	 * manoeuvres all happen here, and the checks that follow must see what will be displayed.
	 */
	private List<RouteSegmentResult> prepare(HHNetworkRouteRes alt, LatLon start, LatLon end,
			RouteResultPreparation rrp) throws SQLException, IOException, InterruptedException {
		planner.prepareRouteResults(hctx, alt, start, end, rrp);
		if (cfg.ROUTE_ALL_ALT_SEGMENTS && !alt.detailed.isEmpty()) {
			alt.detailed = rrp.prepareResult(hctx.rctx, alt.detailed).detailed;
		}
		return alt.detailed;
	}

	/**
	 * Both directions matter. "Own roads" says the alternative offers something new; "roads avoided"
	 * says it actually replaces a part of the route it is an alternative to. Without the second one
	 * an alternative that contains a whole accepted route plus a loop passes.
	 */
	private Distinctness distinctness(Map<Long, Double> geometry, List<Map<Long, Double>> accepted) {
		double length = totalLength(geometry);
		Distinctness d = new Distinctness();
		d.ownRoads = Double.MAX_VALUE;
		d.avoidedRoads = Double.MAX_VALUE;
		for (Map<Long, Double> other : accepted) {
			d.ownRoads = Math.min(d.ownRoads, length - sharedGeometry(geometry, other));
			d.avoidedRoads = Math.min(d.avoidedRoads, totalLength(other) - sharedGeometry(other, geometry));
		}
		return d;
	}

	private void dropped(AltCandidate c, String reason, Object... args) {
		planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0, "  alt dropped (+%.1f%%): " + reason + "\n",
				prepend(100 * (c.cost / optCost - 1), args));
	}

	private Object[] prepend(double first, Object[] rest) {
		Object[] all = new Object[rest.length + 1];
		all[0] = first;
		System.arraycopy(rest, 0, all, 1, rest.length);
		return all;
	}

	private boolean isCancelled(RouteCalculationProgress progress) {
		return progress != null && progress.isCancelled;
	}

	/** the hub points of the optimal route, in order */
	private List<NetworkDBPoint> hubPath(HHNetworkRouteRes route) {
		List<NetworkDBPoint> nodes = new ArrayList<>();
		for (HHNetworkSegmentRes r : route.segments) {
			if (r.segment != null && r.segment.start != null && r.segment.end != null) {
				if (nodes.isEmpty()) {
					nodes.add(r.segment.start);
				}
				nodes.add(r.segment.end);
			}
		}
		return nodes;
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

	private long signature(List<NetworkDBPoint> path) {
		long signature = 0;
		for (NetworkDBPoint n : path) {
			signature = signature * 1000003L + n.index;
		}
		return signature;
	}

	private long edgeKey(long from, long to) {
		return from * ALT_KEY_MULT + to;
	}

	/** edge keys of a full s -> t path including the first/last mile anchors */
	private TLongSet pathEdges(List<NetworkDBPoint> path) {
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
	private boolean isPlateauRepresentative(NetworkDBPoint v, double plateau) {
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
	private boolean isSimple(List<NetworkDBPoint> path) {
		TLongSet seen = new TLongHashSet();
		for (NetworkDBPoint p : path) {
			if (!seen.add(p.index)) {
				return false;
			}
		}
		return true;
	}

	/** length of the road pieces that `segments` drives more than once */
	private double retracedLength(List<RouteSegmentResult> segments) {
		TLongSet seen = new TLongHashSet();
		double retraced = 0;
		for (RouteSegmentResult r : segments) {
			RouteDataObject o = r.getObject();
			int i = r.getStartPointIndex(), end = r.getEndPointIndex();
			int step = i <= end ? 1 : -1;
			while (i != end) {
				int j = i + step;
				if (!seen.add(roadPieceKey(o, i, j))) {
					retraced += pieceLength(o, i, j);
				}
				i = j;
			}
		}
		return retraced;
	}

	/** every hub-graph segment was expanded into real roads, so nothing will be drawn as a straight line */
	private boolean isFullyExpanded(HHNetworkRouteRes route) {
		for (HHNetworkSegmentRes s : route.segments) {
			if (s.list == null || s.list.isEmpty()) {
				return false;
			}
		}
		return !route.segments.isEmpty();
	}

	private List<RouteSegmentResult> detailedSegments(HHNetworkRouteRes route) {
		List<RouteSegmentResult> l = new ArrayList<>();
		for (HHNetworkSegmentRes s : route.segments) {
			if (s.list != null) {
				l.addAll(s.list);
			}
		}
		return l;
	}

	/**
	 * Road piece -> covered length. RouteSegmentResult.getDistance() is only filled in
	 * prepareResult(), so the length is measured on the geometry here.
	 */
	private Map<Long, Double> roadSegments(List<RouteSegmentResult> segments) {
		Map<Long, Double> m = new HashMap<>();
		for (RouteSegmentResult r : segments) {
			RouteDataObject o = r.getObject();
			int i = r.getStartPointIndex(), end = r.getEndPointIndex();
			int step = i <= end ? 1 : -1;
			while (i != end) {
				int j = i + step;
				Long key = roadPieceKey(o, i, j);
				Double prev = m.get(key);
				m.put(key, (prev == null ? 0 : prev) + pieceLength(o, i, j));
				i = j;
			}
		}
		return m;
	}

	/** identifies one piece of road between two neighbouring points, whichever way it is driven */
	private long roadPieceKey(RouteDataObject o, int i, int j) {
		return o.getId() * 4096L + Math.min(i, j);
	}

	private double pieceLength(RouteDataObject o, int i, int j) {
		return HHRoutePlanner.squareRootDist31(o.getPoint31XTile(i), o.getPoint31YTile(i),
				o.getPoint31XTile(j), o.getPoint31YTile(j));
	}

	private double totalLength(Map<Long, Double> segments) {
		double d = 0;
		for (Double v : segments.values()) {
			d += v;
		}
		return d;
	}

	/** length of the roads that the two routes have in common */
	private double sharedGeometry(Map<Long, Double> segments, Map<Long, Double> reference) {
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
