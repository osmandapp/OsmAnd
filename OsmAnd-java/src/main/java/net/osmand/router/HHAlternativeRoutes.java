package net.osmand.router;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
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
 * A route short enough that the two last-mile searches meet each other uses no hub-graph edge at all,
 * or so few that none of the above can produce anything. {@link #calcDetailedAlternatives} then
 * applies the very same plateau idea one level down, on the detailed road trees - see its comment.
 *
 * One instance serves one routing call - the plateau maps and the expansion budget below are the
 * state of that call.
 */
public class HHAlternativeRoutes<T extends NetworkDBPoint> {

	/** attempts to expand one candidate before giving up on it */
	private static final int ALT_EXPAND_RETRIES = 3;
	/** two costs closer than this are the same cost (seconds) - used to follow a plateau */
	private static final double MINIMAL_COST_DIFF = 0.01;
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
	/** roads of the main route and of every alternative accepted so far */
	private final List<Map<Long, Double>> accepted = new ArrayList<>();

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
		maxCost = stretchLimit(optCost);
		// Distinctness is measured against the main route and against the alternatives already
		// accepted alike - an alternative has to be worth proposing next to each of them.
		accepted.add(roadSegments(detailedSegments(route)));
		setDistinctnessThresholds(totalLength(accepted.get(0)));
		boolean hub = usesHubGraph(route);
		if (hub) {
			calcHubAlternatives(route, start, end, progress, rrp);
		}
		// A route of a kilometre or two can touch the hub graph and still give the method nothing to
		// work with - measured on a 1.7 km route: three hub segments, six hub points settled by both
		// trees, no candidate. The detailed graph is then the only place left to look, as long as the
		// route is short enough for that search to be cheap (a route that uses no hub graph at all is
		// short by construction).
		if (route.altRoutes.isEmpty() && (!hub || optCost <= cfg.ALT_DETAILED_MAX_COST)) {
			calcDetailedAlternatives(route, rrp);
		}
	}

	/** the most an alternative may cost, rule 1 of the three in HHRoutingConfig */
	private double stretchLimit(double cost) {
		return cost * (1 + cfg.ALT_STRETCH) + cfg.ALT_STRETCH_ABS;
	}

	/**
	 * Whether the route uses the hub graph at all. When it does not - the two last-mile searches met
	 * each other before either reached a hub point - the hub method has nothing to work on, and the
	 * route is by that very fact short enough to be searched again on the detailed graph.
	 */
	private boolean usesHubGraph(HHNetworkRouteRes route) {
		for (HHNetworkSegmentRes r : route.segments) {
			if (r.segment != null) {
				return true;
			}
		}
		return false;
	}

	/** the plateau method on the hub graph - the usual case, everything but a short city route */
	private void calcHubAlternatives(HHNetworkRouteRes route, LatLon start, LatLon end,
			RouteCalculationProgress progress, RouteResultPreparation rrp)
			throws SQLException, IOException, InterruptedException {
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
		return rankScore(c.sharing, c.cost);
	}

	private double rankScore(double sharing, double cost) {
		return (1 - sharing) - cfg.ALT_RANK_COST_WEIGHT * (cost / optCost - 1);
	}

	/** stage 2: expand candidates one by one and verify them on the real road segments */
	private void verifyAndAccept(List<AltCandidate> ordered, HHNetworkRouteRes route, LatLon start,
			LatLon end, RouteCalculationProgress progress, RouteResultPreparation rrp)
			throws SQLException, IOException, InterruptedException {
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
			Distinctness d = assess(alt.detailed, geometry, c.cost, "");
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

	// ---------------------------------------------------------------------------------------------
	// Alternatives on the detailed road graph
	// ---------------------------------------------------------------------------------------------

	/**
	 * A city route of a few kilometres does not reach the hub graph, or barely touches it: the two
	 * last-mile searches meet each other first, and the hub method above has no edge to build a
	 * plateau on (measured on a 4 km route: 6 hub points settled by both trees, none of them usable,
	 * and the same six on a 1.7 km route that does have three hub segments). Everything that makes
	 * such a route interesting - the parallel street one block away - lives in the detailed graph,
	 * and both trees of it are already there.
	 *
	 * So the plateau method is applied one level down. A via node is a road point settled by both
	 * detailed trees, its route costs f(v) + b(v), and its plateau is the maximal stretch around it
	 * where f + b stays constant, i.e. the road both trees agree on. Two things are different from
	 * the hub graph: a candidate is assembled by walking parent links, so it costs nothing to expand
	 * (no two-stage filter, no expansion budget), and there are tens of thousands of them, so
	 * candidates that describe the same plateau are collapsed to one before anything is assembled.
	 */
	private void calcDetailedAlternatives(HHNetworkRouteRes route, RouteResultPreparation rrp)
			throws SQLException, IOException, InterruptedException {
		if (hctx.startSegment == null || hctx.endSegment == null) {
			return;
		}
		TLongObjectHashMap<RouteSegment> fwd = new TLongObjectHashMap<>();
		TLongObjectHashMap<RouteSegment> bwd = new TLongObjectHashMap<>();
		long time = System.nanoTime();
		searchDetailedTrees(fwd, bwd);
		if (fwd.isEmpty() || bwd.isEmpty()) {
			return;
		}
		List<DetailedCandidate> meeting = meetingPoints(fwd, bwd);
		if (meeting.isEmpty()) {
			return;
		}
		List<DetailedCandidate> plateaus = collapseToPlateaus(meeting);
		planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0,
				"  detailed graph: %,d/%,d settled, %,d meeting points, %d plateaus, %.0f ms\n",
				fwd.size(), bwd.size(), meeting.size(), plateaus.size(), (System.nanoTime() - time) / 1e6);
		acceptDetailed(rankDetailed(plateaus, route), route, rrp);
	}

	/** one road point of the detailed graph settled by both trees */
	private static class DetailedCandidate {
		RouteSegment fwd, bwd;
		/** cost from the start to the junction the two halves are joined at, and from it to the target */
		double fwdDist, bwdDist;
		double cost;
		/** length of the plateau towards the start and towards the target, in cost */
		double plateauFwd, plateauBwd;
		/** first road point of the plateau in each direction - candidates sharing both are the same route */
		long anchorFwd, anchorBwd;
		double sharing;
		List<RouteSegmentResult> geometry;
	}

	/**
	 * The bidirectional Dijkstra whose two trees the candidates are read off. The last-mile searches
	 * of the routing itself cannot be reused: each of them stops at the hub points around its own end,
	 * so on a 4 km route their trees have five road points in common - nothing to choose between. This
	 * one runs start to end with no boundaries and, thanks to altHorizon, does not stop at the first
	 * meeting point but settles the whole band the alternatives may live in.
	 */
	private void searchDetailedTrees(TLongObjectHashMap<RouteSegment> fwd, TLongObjectHashMap<RouteSegment> bwd)
			throws InterruptedException, IOException {
		RoutingContext rctx = hctx.rctx;
		int maxVisited = rctx.config.MAX_VISITED;
		int planRoadDirection = rctx.config.planRoadDirection;
		float heuristicCoefficient = rctx.config.heuristicCoefficient;
		double altHorizon = rctx.config.altHorizon;
		rctx.config.MAX_VISITED = HHRoutePlanner.MAX_POINTS_CLUSTER_ROUTING;
		rctx.config.planRoadDirection = 0;
		rctx.config.heuristicCoefficient = 0; // dijkstra: the queue cost is the distance from the start
		rctx.config.altHorizon = stretchLimit(optCost) / optCost - 1;
		// the road segments of the route already found carry the routing state of that search
		rctx.unloadAllData();
		try {
			new BinaryRoutePlanner().searchRouteInternal(rctx, hctx.startSegment, hctx.endSegment, null, fwd, bwd);
		} finally {
			rctx.config.altHorizon = altHorizon;
			rctx.config.heuristicCoefficient = heuristicCoefficient;
			rctx.config.planRoadDirection = planRoadDirection;
			rctx.config.MAX_VISITED = maxVisited;
		}
	}

	/**
	 * Road points settled by both trees and cheap enough. The reference cost is the cheapest meeting
	 * point rather than the main route: on a long route the two trees may still touch far away from
	 * the optimal path, and those points must not become the yardstick.
	 */
	private List<DetailedCandidate> meetingPoints(TLongObjectHashMap<RouteSegment> fwd,
			TLongObjectHashMap<RouteSegment> bwd) {
		List<DetailedCandidate> all = new ArrayList<>();
		double best = Double.MAX_VALUE;
		for (RouteSegment f : fwd.valueCollection()) {
			RouteSegment b = bwd.get(oppositeKey(f));
			if (b == null) {
				continue;
			}
			DetailedCandidate c = new DetailedCandidate();
			c.fwd = f;
			c.bwd = b;
			// Both trees measure the distance to the far end of their own traversal of this road piece,
			// so the two halves are joined at the junction the piece starts at: the forward tree gets
			// there one piece earlier (its parent), the backward tree ends exactly there. Adding the
			// two raw distances instead would count the piece itself twice, and the sum would then
			// grow and shrink with the piece length all along a route that is in fact optimal.
			c.fwdDist = f.getParentRoute() == null ? 0 : f.getParentRoute().distanceFromStart;
			c.bwdDist = b.distanceFromStart;
			c.cost = c.fwdDist + c.bwdDist;
			if (c.cost <= 0) {
				continue;
			}
			best = Math.min(best, c.cost);
			all.add(c);
		}
		double limit = stretchLimit(Math.min(optCost, best));
		List<DetailedCandidate> within = new ArrayList<>();
		for (DetailedCandidate c : all) {
			if (c.cost <= limit) {
				within.add(c);
			}
		}
		return within;
	}

	/**
	 * Accumulates the plateau of every candidate and keeps one candidate per plateau. An edge belongs
	 * to both trees exactly when the total cost does not change along it, which is what the walk
	 * checks; the value has to be read from the parent first, hence the sort by distance.
	 */
	private List<DetailedCandidate> collapseToPlateaus(List<DetailedCandidate> candidates) {
		final TLongObjectHashMap<DetailedCandidate> byKey = new TLongObjectHashMap<>();
		for (DetailedCandidate c : candidates) {
			byKey.put(routePointKey(c.fwd), c);
		}
		List<DetailedCandidate> byFwd = new ArrayList<>(candidates);
		Collections.sort(byFwd, new Comparator<DetailedCandidate>() {
			@Override
			public int compare(DetailedCandidate a, DetailedCandidate b) {
				return Double.compare(a.fwdDist, b.fwdDist);
			}
		});
		for (DetailedCandidate c : byFwd) {
			DetailedCandidate p = c.fwd.getParentRoute() == null ? null
					: byKey.get(routePointKey(c.fwd.getParentRoute()));
			if (p != null && samePlateau(p, c)) {
				c.plateauFwd = p.plateauFwd + (c.fwdDist - p.fwdDist);
				c.anchorFwd = p.anchorFwd;
			} else {
				c.anchorFwd = routePointKey(c.fwd);
			}
		}
		List<DetailedCandidate> byBwd = new ArrayList<>(candidates);
		Collections.sort(byBwd, new Comparator<DetailedCandidate>() {
			@Override
			public int compare(DetailedCandidate a, DetailedCandidate b) {
				return Double.compare(a.bwdDist, b.bwdDist);
			}
		});
		for (DetailedCandidate c : byBwd) {
			DetailedCandidate p = c.bwd.getParentRoute() == null ? null
					: byKey.get(oppositeKey(c.bwd.getParentRoute()));
			if (p != null && samePlateau(p, c)) {
				c.plateauBwd = p.plateauBwd + (c.bwdDist - p.bwdDist);
				c.anchorBwd = p.anchorBwd;
			} else {
				c.anchorBwd = routePointKey(c.fwd);
			}
		}
		Map<Long, DetailedCandidate> perPlateau = new HashMap<>();
		for (DetailedCandidate c : candidates) {
			if (c.plateauFwd + c.plateauBwd < cfg.ALT_MIN_PLATEAU * c.cost) {
				continue;
			}
			Long id = c.anchorFwd * 1000003L + c.anchorBwd;
			DetailedCandidate kept = perPlateau.get(id);
			if (kept == null || c.cost < kept.cost) {
				perPlateau.put(id, c);
			}
		}
		return new ArrayList<>(perPlateau.values());
	}

	/** the edge between the two runs in both trees, so they describe one and the same plateau */
	private boolean samePlateau(DetailedCandidate parent, DetailedCandidate child) {
		return Math.abs(parent.cost - child.cost) < MINIMAL_COST_DIFF;
	}

	/** best first, by the same score as on the hub graph */
	private List<DetailedCandidate> rankDetailed(List<DetailedCandidate> candidates,
			HHNetworkRouteRes route) {
		Map<Long, Double> main = accepted.get(0);
		for (DetailedCandidate c : candidates) {
			c.geometry = assemble(c);
			Map<Long, Double> roads = roadSegments(c.geometry);
			double length = totalLength(roads);
			c.sharing = length <= 0 ? 1 : sharedGeometry(roads, main) / length;
		}
		Collections.sort(candidates, new Comparator<DetailedCandidate>() {
			@Override
			public int compare(DetailedCandidate x, DetailedCandidate y) {
				return Double.compare(rankScore(y.sharing, y.cost), rankScore(x.sharing, x.cost));
			}
		});
		return candidates;
	}

	private void acceptDetailed(List<DetailedCandidate> ordered, HHNetworkRouteRes route,
			RouteResultPreparation rrp) throws SQLException, IOException, InterruptedException {
		for (DetailedCandidate c : ordered) {
			if (route.altRoutes.size() >= cfg.ALT_MAX_COUNT) {
				return;
			}
			HHNetworkRouteRes alt = new HHNetworkRouteRes();
			alt.detailed = c.geometry;
			if (cfg.ROUTE_ALL_ALT_SEGMENTS && !alt.detailed.isEmpty()) {
				// turns, distances and the clean-up of small manoeuvres, exactly like the main route -
				// the checks below must see what will be displayed
				alt.detailed = rrp.prepareResult(hctx.rctx, alt.detailed).detailed;
			}
			Map<Long, Double> geometry = roadSegments(alt.detailed);
			Distinctness d = assess(alt.detailed, geometry, c.cost, " (detailed)");
			if (d == null) {
				continue;
			}
			route.altRoutes.add(alt);
			accepted.add(geometry);
			planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0,
					"  alt (detailed) accepted: +%.1f%%, plateau %.0f%%, own roads %.1f km, avoids %.1f km\n",
					100 * (c.cost / optCost - 1), 100 * (c.plateauFwd + c.plateauBwd) / c.cost,
					d.ownRoads / 1000, d.avoidedRoads / 1000);
		}
	}

	/**
	 * The two halves of the candidate joined into one route, the way the bidirectional search joins
	 * its own final segment: the via point carries the forward chain as its parent and the backward
	 * chain as its opposite.
	 */
	private List<RouteSegmentResult> assemble(DetailedCandidate c) {
		// the via point itself belongs to the backward chain, the forward chain stops at its parent -
		// the same split the cost above is measured on
		FinalRouteSegment frs = new FinalRouteSegment(c.fwd.getRoad(), c.fwd.getSegmentStart(),
				c.fwd.getSegmentEnd());
		frs.setParentRoute(c.fwd.getParentRoute());
		frs.reverseWaySearch = false;
		frs.distanceFromStart = (float) c.cost;
		frs.distanceToEnd = 0;
		frs.opposite = c.bwd;
		return new RouteResultPreparation().convertFinalSegmentToResults(hctx.rctx, frs);
	}

	/** identifies the road point and the direction it is driven in, as the search itself does */
	private static long routePointKey(RouteSegment s) {
		int start = s.getSegmentStart();
		return HHRoutePlanner.calculateRoutePointInternalId(s.getRoad().getId(), start,
				start + (s.isPositive() ? 1 : -1));
	}

	/** the same road point driven the other way round - how the other tree stores it */
	private static long oppositeKey(RouteSegment s) {
		int start = s.getSegmentStart();
		int end = start + (s.isPositive() ? 1 : -1);
		return HHRoutePlanner.calculateRoutePointInternalId(s.getRoad().getId(), end, start);
	}

	private void setDistinctnessThresholds(double mainLength) {
		minOwnRoads = Math.max(cfg.ALT_MIN_DISTINCT_FLOOR, cfg.ALT_MIN_DISTINCT_REL * mainLength);
		// Avoiding is asked for less strictly than offering: replacing a good stretch of a long route
		// is useful even when most of the main route stays. The point of this second threshold is to
		// reject an alternative that contains the whole main route and only adds a loop to it.
		minAvoided = Math.max(cfg.ALT_MIN_DISTINCT_FLOOR, cfg.ALT_MIN_DISTINCT_REL * mainLength / 2);
	}

	/** how the candidate differs from the routes already on offer, or null when it must not be proposed */
	private Distinctness assess(List<RouteSegmentResult> detailed, Map<Long, Double> geometry,
			double cost, String label) {
		if (detailed.isEmpty()) {
			dropped(cost, label, "no detailed geometry");
			return null;
		}
		double retraced = retracedLength(detailed);
		if (retraced > cfg.ALT_MAX_RETRACED) {
			// the hub-level isSimple() check misses this when the two halves overlap inside a single
			// shortcut: the candidate drives out and turns back, which reads as a bug on the map
			dropped(cost, label, "drives %.0f m of its own roads twice", retraced);
			return null;
		}
		Distinctness d = distinctness(geometry, accepted);
		if (d.ownRoads < minOwnRoads || d.avoidedRoads < minAvoided) {
			dropped(cost, label, "own roads %.1f km (need %.1f), avoids %.1f km (need %.1f)",
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
		dropped(c.cost, "", reason, args);
	}

	private void dropped(double cost, String label, String reason, Object... args) {
		planner.printf(HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0, "  alt%s dropped (+%.1f%%): " + reason + "\n",
				prepend(label, 100 * (cost / optCost - 1), args));
	}

	private Object[] prepend(String label, double stretch, Object[] rest) {
		Object[] all = new Object[rest.length + 2];
		all[0] = label;
		all[1] = stretch;
		System.arraycopy(rest, 0, all, 2, rest.length);
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
