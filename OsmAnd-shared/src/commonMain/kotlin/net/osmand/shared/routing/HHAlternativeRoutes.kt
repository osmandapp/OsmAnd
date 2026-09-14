package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
 * or so few that none of the above can produce anything. [calcDetailedAlternatives] then
 * applies the very same plateau idea one level down, on the detailed road trees - see its comment.
 *
 * One instance serves one routing call - the plateau maps and the expansion budget below are the
 * state of that call.
 *
 * A copy of `HHAlternativeRoutes` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. Where java reads candidates out of a hash map in whatever order the map keeps
 * them, this copy keeps them in insertion order, so Kotlin/Native and the jvm rank ties alike.
 */
class HHAlternativeRoutes(private val planner: HHRoutePlanner, private val hctx: HHRoutingContext) {

	private val cfg: HHRoutingConfig = hctx.requireConfig()

	/** plateau length of a via node towards the start and towards the target */
	private val plateauFwd = HashMap<NetworkDBPoint, Double>()
	private val plateauBwd = HashMap<NetworkDBPoint, Double>()
	private var optCost = 0.0
	private var maxCost = 0.0

	/** how much own and avoided road an alternative must show, derived from the main route's length */
	private var minOwnRoads = 0.0
	private var minAvoided = 0.0

	/** detailed expansions spent so far, against cfg.ALT_MAX_EXPAND */
	private var expanded = 0

	/** roads of the main route and of every alternative accepted so far */
	private val accepted = ArrayList<Map<Long, Double>>()

	private class AltCandidate {
		lateinit var via: NetworkDBPoint
		var cost = 0.0
		var plateau = 0.0
		var sharing = 0.0
		lateinit var path: List<NetworkDBPoint>
	}

	/** how much road an alternative offers that the routes it competes with do not, and vice versa */
	private class Distinctness {
		var ownRoads = 0.0
		var avoidedRoads = 0.0
	}

	private inline fun debug(message: () -> String) {
		if (HHRoutePlanner.DEBUG_VERBOSE_LEVEL > 0) {
			LOG.info(message())
		}
	}

	fun calcAlternativeRoute(route: HHNetworkRouteRes, start: KLatLon, end: KLatLon, progress: RouteCalculationProgress?) {
		optCost = route.getHHRoutingTime()
		if (optCost <= 0 || cfg.ALT_MAX_COUNT <= 0) {
			return
		}
		maxCost = stretchLimit(optCost)
		// Distinctness is measured against the main route and against the alternatives already
		// accepted alike - an alternative has to be worth proposing next to each of them.
		accepted.add(roadSegments(detailedSegments(route)))
		setDistinctnessThresholds(totalLength(accepted[0]))
		val hub = usesHubGraph(route)
		if (hub) {
			calcHubAlternatives(route, start, end, progress)
		}
		// A route of a kilometre or two can touch the hub graph and still give the method nothing to
		// work with - measured on a 1.7 km route: three hub segments, six hub points settled by both
		// trees, no candidate. The detailed graph is then the only place left to look, as long as the
		// route is short enough for that search to be cheap (a route that uses no hub graph at all is
		// short by construction).
		if (route.altRoutes.isEmpty() && (!hub || optCost <= cfg.ALT_DETAILED_MAX_COST)) {
			calcDetailedAlternatives(route)
		}
	}

	/** the most an alternative may cost, rule 1 of the three in HHRoutingConfig */
	private fun stretchLimit(cost: Double): Double {
		return cost * (1 + cfg.ALT_STRETCH) + cfg.ALT_STRETCH_ABS
	}

	/**
	 * Whether the route uses the hub graph at all. When it does not - the two last-mile searches met
	 * each other before either reached a hub point - the hub method has nothing to work on, and the
	 * route is by that very fact short enough to be searched again on the detailed graph.
	 */
	private fun usesHubGraph(route: HHNetworkRouteRes): Boolean {
		for (r in route.segments) {
			if (r.segment != null) {
				return true
			}
		}
		return false
	}

	/** the plateau method on the hub graph - the usual case, everything but a short city route */
	private fun calcHubAlternatives(route: HHNetworkRouteRes, start: KLatLon, end: KLatLon, progress: RouteCalculationProgress?) {
		val settled = collectViaNodes()
		if (settled.isEmpty()) {
			return
		}
		computePlateaus(settled)
		val candidates = selectCandidates(settled, route)
		if (candidates.isEmpty()) {
			return
		}
		verifyAndAccept(rank(candidates), route, start, end, progress)
	}

	/** via-node candidates: settled by both trees and within the stretch limit */
	private fun collectViaNodes(): List<NetworkDBPoint> {
		val settled = ArrayList<NetworkDBPoint>()
		for (p in hctx.queueAdded) {
			val pos = p.rtPos
			val rev = p.rtRev
			if (pos != null && rev != null && pos.rtVisited && rev.rtVisited) {
				val c = pos.rtDistanceFromStart + rev.rtDistanceFromStart
				if (c > 0 && c <= maxCost) {
					settled.add(p)
				}
			}
		}
		return settled
	}

	private fun computePlateaus(settled: List<NetworkDBPoint>) {
		accumulatePlateau(settled, false, plateauFwd)
		accumulatePlateau(settled, true, plateauBwd)
	}

	/**
	 * Walks the chain of edges that both trees agree on and accumulates its length per node. A node
	 * continues the chain of its parent when the opposite tree routes the parent back through it,
	 * so the value has to be read from the parent first - hence the sort by distance.
	 */
	private fun accumulatePlateau(settled: List<NetworkDBPoint>, rev: Boolean, plateau: MutableMap<NetworkDBPoint, Double>) {
		val byDist = ArrayList(settled)
		byDist.sortWith { a, b -> info(a, rev)!!.rtDistanceFromStart.compareTo(info(b, rev)!!.rtDistanceFromStart) }
		for (v in byDist) {
			val prev = info(v, rev)!!.rtRouteToPoint
			var value = 0.0
			if (prev != null && info(prev, !rev) != null && info(prev, !rev)!!.rtRouteToPoint === v) {
				val acc = plateau[prev]
				value = (acc ?: 0.0) + (info(v, rev)!!.rtDistanceFromStart - info(prev, rev)!!.rtDistanceFromStart)
			}
			plateau[v] = value
		}
	}

	/**
	 * The point's search state in one direction, or null when that tree never reached it.
	 * [NetworkDBPoint.rt] would allocate the missing one instead of saying so.
	 */
	private fun info(p: NetworkDBPoint, rev: Boolean): NetworkDBPointRouteInfo? {
		return if (rev) p.rtRev else p.rtPos
	}

	/** stage 1: cheap admissibility on the hub graph */
	private fun selectCandidates(settled: List<NetworkDBPoint>, route: HHNetworkRouteRes): MutableList<AltCandidate> {
		val optEdges = pathEdges(hubPath(route))
		val candidates = ArrayList<AltCandidate>()
		val seenPaths = KTLongHashSet()
		for (v in settled) {
			val c = admissibleThrough(v, optEdges)
			if (c != null && seenPaths.add(signature(c.path))) {
				candidates.add(c)
			}
		}
		return candidates
	}

	/** the candidate routed through this via node, or null when it fails one of the hub-graph rules */
	private fun admissibleThrough(via: NetworkDBPoint, optEdges: KTLongHashSet): AltCandidate? {
		val fwd = plateauFwd[via]
		val bwd = plateauBwd[via]
		val cost = via.rtPos!!.rtDistanceFromStart + via.rtRev!!.rtDistanceFromStart
		val plateau = (fwd ?: 0.0) + (bwd ?: 0.0)
		if (!isPlateauRepresentative(via, plateau) || plateau < cfg.ALT_MIN_PLATEAU * cost) {
			return null
		}
		val path = pathThrough(via)
		if (!isSimple(path)) {
			// sp(s,v) and sp(v,t) are each optimal, but their concatenation is not necessarily a
			// simple path: when both halves run over the same roads the candidate drives out and
			// turns back. Cheap check first, the exact one is on the geometry in stage 2.
			return null
		}
		val sharing = sharedCost(path, optEdges) / cost
		if (sharing > cfg.ALT_MAX_SHARING) {
			return null
		}
		val c = AltCandidate()
		c.via = via
		c.cost = cost
		c.plateau = plateau
		c.path = path
		c.sharing = sharing
		return c
	}

	/**
	 * Best first: as different as possible for as little extra time as possible, with everything
	 * within ALT_STRETCH_PREFERRED proposed before the merely acceptable candidates.
	 */
	private fun rank(candidates: MutableList<AltCandidate>): List<AltCandidate> {
		candidates.sortWith { x, y -> rankScore(y).compareTo(rankScore(x)) }
		val preferredCost = optCost * (1 + cfg.ALT_STRETCH_PREFERRED)
		val ordered = ArrayList<AltCandidate>(candidates.size)
		for (c in candidates) {
			if (c.cost <= preferredCost) {
				ordered.add(c)
			}
		}
		for (c in candidates) {
			if (c.cost > preferredCost) {
				ordered.add(c)
			}
		}
		return ordered
	}

	private fun rankScore(c: AltCandidate): Double {
		return rankScore(c.sharing, c.cost)
	}

	private fun rankScore(sharing: Double, cost: Double): Double {
		return (1 - sharing) - cfg.ALT_RANK_COST_WEIGHT * (cost / optCost - 1)
	}

	/** stage 2: expand candidates one by one and verify them on the real road segments */
	private fun verifyAndAccept(
		ordered: List<AltCandidate>, route: HHNetworkRouteRes, start: KLatLon, end: KLatLon, progress: RouteCalculationProgress?
	) {
		for (c in ordered) {
			if (route.altRoutes.size >= cfg.ALT_MAX_COUNT || expanded >= cfg.ALT_MAX_EXPAND) {
				break
			}
			val alt = expand(c, progress)
			if (isCancelled(progress)) {
				return
			}
			if (alt == null) {
				continue
			}
			val geometry = roadSegments(prepare(alt, start, end))
			val d = assess(alt.detailed, geometry, c.cost, "") ?: continue
			route.altRoutes.add(alt)
			accepted.add(geometry)
			debug {
				"  alt accepted: +%.1f%%, plateau %.0f%%, own roads %.1f km, avoids %.1f km".format(
					100 * (c.cost / optCost - 1), 100 * c.plateau / c.cost,
					d.ownRoads / 1000, d.avoidedRoads / 1000
				)
			}
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
	private fun calcDetailedAlternatives(route: HHNetworkRouteRes) {
		if (hctx.startSegment == null || hctx.endSegment == null) {
			return
		}
		val fwd = KTLongObjectMap<RouteSegment>()
		val bwd = KTLongObjectMap<RouteSegment>()
		val time = nanoTime()
		searchDetailedTrees(fwd, bwd)
		if (fwd.isEmpty() || bwd.isEmpty()) {
			return
		}
		val meeting = meetingPoints(fwd, bwd)
		if (meeting.isEmpty()) {
			return
		}
		val plateaus = collapseToPlateaus(meeting)
		debug {
			"  detailed graph: %d/%d settled, %d meeting points, %d plateaus, %.0f ms".format(
				fwd.size, bwd.size, meeting.size, plateaus.size, (nanoTime() - time) / 1e6
			)
		}
		acceptDetailed(rankDetailed(plateaus), route)
	}

	/** one road point of the detailed graph settled by both trees */
	private class DetailedCandidate {
		lateinit var fwd: RouteSegment
		lateinit var bwd: RouteSegment

		/** cost from the start to the junction the two halves are joined at, and from it to the target */
		var fwdDist = 0.0
		var bwdDist = 0.0
		var cost = 0.0

		/** length of the plateau towards the start and towards the target, in cost */
		var plateauFwd = 0.0
		var plateauBwd = 0.0

		/** first road point of the plateau in each direction - candidates sharing both are the same route */
		var anchorFwd = 0L
		var anchorBwd = 0L
		var sharing = 0.0
		var geometry: MutableList<RouteSegmentResult> = ArrayList()
	}

	/**
	 * The bidirectional Dijkstra whose two trees the candidates are read off. The last-mile searches
	 * of the routing itself cannot be reused: each of them stops at the hub points around its own end,
	 * so on a 4 km route their trees have five road points in common - nothing to choose between. This
	 * one runs start to end with no boundaries and, thanks to altHorizon, does not stop at the first
	 * meeting point but settles the whole band the alternatives may live in.
	 */
	private fun searchDetailedTrees(fwd: KTLongObjectMap<RouteSegment>, bwd: KTLongObjectMap<RouteSegment>) {
		val rctx = hctx.requireContext()
		val maxVisited = rctx.config.MAX_VISITED
		val planRoadDirection = rctx.config.planRoadDirection
		val heuristicCoefficient = rctx.config.heuristicCoefficient
		val altHorizon = rctx.config.altHorizon
		rctx.config.MAX_VISITED = HHRoutePlanner.MAX_POINTS_CLUSTER_ROUTING
		rctx.config.planRoadDirection = 0
		rctx.config.heuristicCoefficient = 0f // dijkstra: the queue cost is the distance from the start
		rctx.config.altHorizon = stretchLimit(optCost) / optCost - 1
		// the road segments of the route already found carry the routing state of that search
		rctx.unloadAllData()
		try {
			BinaryRoutePlanner().searchRouteInternal(rctx, hctx.startSegment, hctx.endSegment, null, fwd, bwd)
		} finally {
			rctx.config.altHorizon = altHorizon
			rctx.config.heuristicCoefficient = heuristicCoefficient
			rctx.config.planRoadDirection = planRoadDirection
			rctx.config.MAX_VISITED = maxVisited
		}
	}

	/**
	 * Road points settled by both trees and cheap enough. The reference cost is the cheapest meeting
	 * point rather than the main route: on a long route the two trees may still touch far away from
	 * the optimal path, and those points must not become the yardstick.
	 */
	private fun meetingPoints(fwd: KTLongObjectMap<RouteSegment>, bwd: KTLongObjectMap<RouteSegment>): List<DetailedCandidate> {
		val all = ArrayList<DetailedCandidate>()
		var best = Double.MAX_VALUE
		fwd.forEachValue { f ->
			val b = bwd[oppositeKey(f)]
			if (b != null) {
				val c = DetailedCandidate()
				c.fwd = f
				c.bwd = b
				// Both trees measure the distance to the far end of their own traversal of this road piece,
				// so the two halves are joined at the junction the piece starts at: the forward tree gets
				// there one piece earlier (its parent), the backward tree ends exactly there. Adding the
				// two raw distances instead would count the piece itself twice, and the sum would then
				// grow and shrink with the piece length all along a route that is in fact optimal.
				c.fwdDist = (f.getParentRoute()?.distanceFromStart ?: 0f).toDouble()
				c.bwdDist = b.distanceFromStart.toDouble()
				c.cost = c.fwdDist + c.bwdDist
				if (c.cost > 0) {
					best = min(best, c.cost)
					all.add(c)
				}
			}
		}
		val limit = stretchLimit(min(optCost, best))
		val within = ArrayList<DetailedCandidate>()
		for (c in all) {
			if (c.cost <= limit) {
				within.add(c)
			}
		}
		return within
	}

	/**
	 * Accumulates the plateau of every candidate and keeps one candidate per plateau. An edge belongs
	 * to both trees exactly when the total cost does not change along it, which is what the walk
	 * checks; the value has to be read from the parent first, hence the sort by distance.
	 */
	private fun collapseToPlateaus(candidates: List<DetailedCandidate>): MutableList<DetailedCandidate> {
		val byKey = KTLongObjectMap<DetailedCandidate>()
		for (c in candidates) {
			byKey.put(routePointKey(c.fwd), c)
		}
		val byFwd = ArrayList(candidates)
		byFwd.sortWith { a, b -> a.fwdDist.compareTo(b.fwdDist) }
		for (c in byFwd) {
			val parent = c.fwd.getParentRoute()
			val p = if (parent == null) null else byKey[routePointKey(parent)]
			if (p != null && samePlateau(p, c)) {
				c.plateauFwd = p.plateauFwd + (c.fwdDist - p.fwdDist)
				c.anchorFwd = p.anchorFwd
			} else {
				c.anchorFwd = routePointKey(c.fwd)
			}
		}
		val byBwd = ArrayList(candidates)
		byBwd.sortWith { a, b -> a.bwdDist.compareTo(b.bwdDist) }
		for (c in byBwd) {
			val parent = c.bwd.getParentRoute()
			val p = if (parent == null) null else byKey[oppositeKey(parent)]
			if (p != null && samePlateau(p, c)) {
				c.plateauBwd = p.plateauBwd + (c.bwdDist - p.bwdDist)
				c.anchorBwd = p.anchorBwd
			} else {
				c.anchorBwd = routePointKey(c.fwd)
			}
		}
		val perPlateau = LinkedHashMap<Long, DetailedCandidate>()
		for (c in candidates) {
			if (c.plateauFwd + c.plateauBwd < cfg.ALT_MIN_PLATEAU * c.cost) {
				continue
			}
			val id = c.anchorFwd * 1000003L + c.anchorBwd
			val kept = perPlateau[id]
			if (kept == null || c.cost < kept.cost) {
				perPlateau[id] = c
			}
		}
		return ArrayList(perPlateau.values)
	}

	/** the edge between the two runs in both trees, so they describe one and the same plateau */
	private fun samePlateau(parent: DetailedCandidate, child: DetailedCandidate): Boolean {
		return abs(parent.cost - child.cost) < MINIMAL_COST_DIFF
	}

	/** best first, by the same score as on the hub graph */
	private fun rankDetailed(candidates: MutableList<DetailedCandidate>): List<DetailedCandidate> {
		val main = accepted[0]
		for (c in candidates) {
			c.geometry = assemble(c)
			val roads = roadSegments(c.geometry)
			val length = totalLength(roads)
			c.sharing = if (length <= 0) 1.0 else sharedGeometry(roads, main) / length
		}
		candidates.sortWith { x, y -> rankScore(y.sharing, y.cost).compareTo(rankScore(x.sharing, x.cost)) }
		return candidates
	}

	private fun acceptDetailed(ordered: List<DetailedCandidate>, route: HHNetworkRouteRes) {
		val rctx = hctx.requireContext()
		for (c in ordered) {
			if (route.altRoutes.size >= cfg.ALT_MAX_COUNT) {
				return
			}
			val alt = HHNetworkRouteRes()
			alt.detailed = c.geometry
			if (cfg.ROUTE_ALL_ALT_SEGMENTS && alt.detailed.isNotEmpty()) {
				// turns, distances and the clean-up of small manoeuvres, exactly like the main route -
				// the checks below must see what will be displayed
				alt.detailed = RouteResultPreparation.prepareResult(rctx, alt.detailed).detailed
			}
			val geometry = roadSegments(alt.detailed)
			val d = assess(alt.detailed, geometry, c.cost, " (detailed)") ?: continue
			route.altRoutes.add(alt)
			accepted.add(geometry)
			debug {
				"  alt (detailed) accepted: +%.1f%%, plateau %.0f%%, own roads %.1f km, avoids %.1f km".format(
					100 * (c.cost / optCost - 1), 100 * (c.plateauFwd + c.plateauBwd) / c.cost,
					d.ownRoads / 1000, d.avoidedRoads / 1000
				)
			}
		}
	}

	/**
	 * The two halves of the candidate joined into one route, the way the bidirectional search joins
	 * its own final segment: the via point carries the forward chain as its parent and the backward
	 * chain as its opposite.
	 */
	private fun assemble(c: DetailedCandidate): MutableList<RouteSegmentResult> {
		// the via point itself belongs to the backward chain, the forward chain stops at its parent -
		// the same split the cost above is measured on
		val frs = FinalRouteSegment(c.fwd.getRoad(), c.fwd.getSegmentStart().toInt(), c.fwd.getSegmentEnd().toInt())
		frs.setParentRoute(c.fwd.getParentRoute())
		frs.reverseWaySearch = false
		frs.distanceFromStart = c.cost.toFloat()
		frs.distanceToEnd = 0f
		frs.opposite = c.bwd
		return RouteResultPreparation.convertFinalSegmentToResults(hctx.requireContext(), frs)
	}

	private fun setDistinctnessThresholds(mainLength: Double) {
		minOwnRoads = max(cfg.ALT_MIN_DISTINCT_FLOOR, cfg.ALT_MIN_DISTINCT_REL * mainLength)
		// Avoiding is asked for less strictly than offering: replacing a good stretch of a long route
		// is useful even when most of the main route stays. The point of this second threshold is to
		// reject an alternative that contains the whole main route and only adds a loop to it.
		minAvoided = max(cfg.ALT_MIN_DISTINCT_FLOOR, cfg.ALT_MIN_DISTINCT_REL * mainLength / 2)
	}

	/** how the candidate differs from the routes already on offer, or null when it must not be proposed */
	private fun assess(detailed: List<RouteSegmentResult>, geometry: Map<Long, Double>, cost: Double, label: String): Distinctness? {
		if (detailed.isEmpty()) {
			dropped(cost, label) { "no detailed geometry" }
			return null
		}
		val retraced = retracedLength(detailed)
		if (retraced > cfg.ALT_MAX_RETRACED) {
			// the hub-level isSimple() check misses this when the two halves overlap inside a single
			// shortcut: the candidate drives out and turns back, which reads as a bug on the map
			dropped(cost, label) { "drives %.0f m of its own roads twice".format(retraced) }
			return null
		}
		val d = distinctness(geometry, accepted)
		if (d.ownRoads < minOwnRoads || d.avoidedRoads < minAvoided) {
			dropped(cost, label) {
				"own roads %.1f km (need %.1f), avoids %.1f km (need %.1f)".format(
					d.ownRoads / 1000, minOwnRoads / 1000, d.avoidedRoads / 1000, minAvoided / 1000
				)
			}
			return null
		}
		return d
	}

	/** the candidate expanded into real roads, or null when it turned out to be unusable */
	private fun expand(c: AltCandidate, progress: RouteCalculationProgress?): HHNetworkRouteRes? {
		// retrieveSegmentsGeometry bails out half way when a shortcut cannot be expanded, or when its
		// detailed cost turns out higher than the hub graph promised; it corrects the segment cost on
		// the way out, so the next attempt usually succeeds. The main route recalculates in exactly
		// the same situation - an alternative simply retries a few times.
		var alt: HHNetworkRouteRes? = null
		var needsRecalculation = true
		var attempt = 0
		while (attempt < ALT_EXPAND_RETRIES && needsRecalculation && expanded < cfg.ALT_MAX_EXPAND) {
			if (isCancelled(progress)) {
				return null
			}
			val candidate = planner.createRouteSegmentFromFinalPoint(hctx, c.via)
			alt = candidate
			if (candidate.segments.isEmpty()) {
				return null
			}
			needsRecalculation = planner.retrieveSegmentsGeometry(hctx, candidate, true, progress, true)
			expanded++
			attempt++
		}
		if (alt == null || alt.segments.isEmpty()) {
			return null
		}
		if (needsRecalculation || !isFullyExpanded(alt)) {
			// the rest of the segments have no geometry and would be drawn as straight lines between
			// hub points, so the candidate is not usable
			dropped(c) { "incomplete geometry" }
			return null
		}
		// shortcut costs can be optimistic; now that the roads are known, check the real one
		val realCost = alt.getHHRoutingDetailed()
		if (realCost > maxCost) {
			dropped(c) { "real cost +%.1f%% over the limit".format(100 * (realCost / optCost - 1)) }
			return null
		}
		c.cost = realCost
		return alt
	}

	/**
	 * Prepares the candidate exactly like the main route: turns, distances and the clean-up of small
	 * manoeuvres all happen here, and the checks that follow must see what will be displayed.
	 */
	private fun prepare(alt: HHNetworkRouteRes, start: KLatLon, end: KLatLon): List<RouteSegmentResult> {
		planner.prepareRouteResults(hctx, alt, start, end)
		if (cfg.ROUTE_ALL_ALT_SEGMENTS && alt.detailed.isNotEmpty()) {
			alt.detailed = RouteResultPreparation.prepareResult(hctx.requireContext(), alt.detailed).detailed
		}
		return alt.detailed
	}

	/**
	 * Both directions matter. "Own roads" says the alternative offers something new; "roads avoided"
	 * says it actually replaces a part of the route it is an alternative to. Without the second one
	 * an alternative that contains a whole accepted route plus a loop passes.
	 */
	private fun distinctness(geometry: Map<Long, Double>, accepted: List<Map<Long, Double>>): Distinctness {
		val length = totalLength(geometry)
		val d = Distinctness()
		d.ownRoads = Double.MAX_VALUE
		d.avoidedRoads = Double.MAX_VALUE
		for (other in accepted) {
			d.ownRoads = min(d.ownRoads, length - sharedGeometry(geometry, other))
			d.avoidedRoads = min(d.avoidedRoads, totalLength(other) - sharedGeometry(other, geometry))
		}
		return d
	}

	private inline fun dropped(c: AltCandidate, reason: () -> String) {
		dropped(c.cost, "", reason)
	}

	private inline fun dropped(cost: Double, label: String, reason: () -> String) {
		debug { "  alt%s dropped (+%.1f%%): ".format(label, 100 * (cost / optCost - 1)) + reason() }
	}

	private fun isCancelled(progress: RouteCalculationProgress?): Boolean {
		return progress != null && progress.isCancelled
	}

	/** the hub points of the optimal route, in order */
	private fun hubPath(route: HHNetworkRouteRes): List<NetworkDBPoint> {
		val nodes = ArrayList<NetworkDBPoint>()
		for (r in route.segments) {
			val segment = r.segment
			if (segment != null) {
				if (nodes.isEmpty()) {
					nodes.add(segment.start)
				}
				nodes.add(segment.end)
			}
		}
		return nodes
	}

	/** full s -> t sequence of hub points through the given via node, taken from both trees */
	private fun pathThrough(via: NetworkDBPoint): List<NetworkDBPoint> {
		val path = ArrayList<NetworkDBPoint>()
		var it: NetworkDBPoint? = via
		while (it != null) {
			path.add(it)
			it = it.rt(false).rtRouteToPoint
		}
		path.reverse()
		it = via.rt(true).rtRouteToPoint
		while (it != null) {
			path.add(it)
			it = it.rt(true).rtRouteToPoint
		}
		return path
	}

	private fun signature(path: List<NetworkDBPoint>): Long {
		var signature = 0L
		for (n in path) {
			signature = signature * 1000003L + n.index
		}
		return signature
	}

	private fun edgeKey(from: Long, to: Long): Long {
		return from * ALT_KEY_MULT + to
	}

	/** edge keys of a full s -> t path including the first/last mile anchors */
	private fun pathEdges(path: List<NetworkDBPoint>): KTLongHashSet {
		val edges = KTLongHashSet()
		if (path.isEmpty()) {
			return edges
		}
		edges.add(edgeKey(ALT_START_KEY, path[0].index.toLong()))
		for (i in 1 until path.size) {
			edges.add(edgeKey(path[i - 1].index.toLong(), path[i].index.toLong()))
		}
		edges.add(edgeKey(path[path.size - 1].index.toLong(), ALT_END_KEY))
		return edges
	}

	/** cost of the part of `path` that runs over the given edges (first/last mile included) */
	private fun sharedCost(path: List<NetworkDBPoint>, edges: KTLongHashSet): Double {
		if (path.isEmpty()) {
			return 0.0
		}
		var shared = 0.0
		val first = path[0]
		val last = path[path.size - 1]
		if (edges.contains(edgeKey(ALT_START_KEY, first.index.toLong()))) {
			shared += first.rt(false).rtDistanceFromStart
		}
		if (edges.contains(edgeKey(last.index.toLong(), ALT_END_KEY))) {
			shared += last.rt(true).rtDistanceFromStart
		}
		for (i in 1 until path.size) {
			val a = path[i - 1]
			val b = path[i]
			if (edges.contains(edgeKey(a.index.toLong(), b.index.toLong()))) {
				shared += hubEdgeCost(a, b)
			}
		}
		return shared
	}

	private fun hubEdgeCost(a: NetworkDBPoint, b: NetworkDBPoint): Double {
		if (b.rt(false).rtRouteToPoint === a) {
			return b.rt(false).rtDistanceFromStart - a.rt(false).rtDistanceFromStart
		}
		if (a.rt(true).rtRouteToPoint === b) {
			return a.rt(true).rtDistanceFromStart - b.rt(true).rtDistanceFromStart
		}
		val segment = a.getSegment(b, true)
		return segment?.dist ?: 0.0
	}

	/**
	 * All nodes of one plateau describe the same route, so only one of them is kept. The chosen one
	 * is the node closest to the middle of the chain: there both trees agree on the edge coming in
	 * and on the edge going out, which is what makes the two halves of the candidate join smoothly
	 * instead of turning back on themselves. At the end of a chain they need not agree.
	 */
	private fun isPlateauRepresentative(v: NetworkDBPoint, plateau: Double): Boolean {
		if (plateau <= 0) {
			return true // no plateau at all, nothing to deduplicate
		}
		val fwd = plateauFwd[v]
		val own = abs((fwd ?: 0.0) - plateau / 2)
		// every node of the chain carries the same plateau length, so a neighbour that sits closer
		// to the middle is the better representative
		val prev = v.rt(false).rtRouteToPoint
		if (prev != null && plateauFwd.containsKey(prev) && prev.rt(true).rtRouteToPoint === v
			&& abs(plateauFwd[prev]!! - plateau / 2) < own
		) {
			return false
		}
		val next = v.rt(true).rtRouteToPoint
		if (next != null && plateauFwd.containsKey(next) && next.rt(false).rtRouteToPoint === v
			&& abs(plateauFwd[next]!! - plateau / 2) < own
		) {
			return false
		}
		return true
	}

	/** no hub point is visited twice, i.e. the two halves of the candidate do not overlap */
	private fun isSimple(path: List<NetworkDBPoint>): Boolean {
		val seen = KTLongHashSet()
		for (p in path) {
			if (!seen.add(p.index.toLong())) {
				return false
			}
		}
		return true
	}

	/** length of the road pieces that `segments` drives more than once */
	private fun retracedLength(segments: List<RouteSegmentResult>): Double {
		val seen = KTLongHashSet()
		var retraced = 0.0
		for (r in segments) {
			val o = r.getObject()
			var i = r.getStartPointIndex()
			val end = r.getEndPointIndex()
			val step = if (i <= end) 1 else -1
			while (i != end) {
				val j = i + step
				if (!seen.add(roadPieceKey(o, i, j))) {
					retraced += pieceLength(o, i, j)
				}
				i = j
			}
		}
		return retraced
	}

	/** every hub-graph segment was expanded into real roads, so nothing will be drawn as a straight line */
	private fun isFullyExpanded(route: HHNetworkRouteRes): Boolean {
		for (s in route.segments) {
			val list = s.list
			if (list == null || list.isEmpty()) {
				return false
			}
		}
		return route.segments.isNotEmpty()
	}

	private fun detailedSegments(route: HHNetworkRouteRes): List<RouteSegmentResult> {
		val l = ArrayList<RouteSegmentResult>()
		for (s in route.segments) {
			s.list?.let { l.addAll(it) }
		}
		return l
	}

	/**
	 * Road piece -> covered length. RouteSegmentResult.getDistance() is only filled in
	 * prepareResult(), so the length is measured on the geometry here.
	 */
	private fun roadSegments(segments: List<RouteSegmentResult>): Map<Long, Double> {
		val m = HashMap<Long, Double>()
		for (r in segments) {
			val o = r.getObject()
			var i = r.getStartPointIndex()
			val end = r.getEndPointIndex()
			val step = if (i <= end) 1 else -1
			while (i != end) {
				val j = i + step
				val key = roadPieceKey(o, i, j)
				val prev = m[key]
				m[key] = (prev ?: 0.0) + pieceLength(o, i, j)
				i = j
			}
		}
		return m
	}

	/** identifies one piece of road between two neighbouring points, whichever way it is driven */
	private fun roadPieceKey(o: RouteDataObject, i: Int, j: Int): Long {
		return o.getId() * 4096L + min(i, j)
	}

	private fun pieceLength(o: RouteDataObject, i: Int, j: Int): Double {
		return HHRoutePlanner.squareRootDist31(
			o.getPoint31XTile(i), o.getPoint31YTile(i),
			o.getPoint31XTile(j), o.getPoint31YTile(j)
		)
	}

	private fun totalLength(segments: Map<Long, Double>): Double {
		var d = 0.0
		for (v in segments.values) {
			d += v
		}
		return d
	}

	/** length of the roads that the two routes have in common */
	private fun sharedGeometry(segments: Map<Long, Double>, reference: Map<Long, Double>): Double {
		var common = 0.0
		for (e in segments.entries) {
			val v = reference[e.key]
			if (v != null) {
				common += min(v, e.value)
			}
		}
		return common
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("HHAlternativeRoutes")

		/** attempts to expand one candidate before giving up on it */
		private const val ALT_EXPAND_RETRIES = 3

		/** two costs closer than this are the same cost (seconds) - used to follow a plateau */
		private const val MINIMAL_COST_DIFF = 0.01
		private const val ALT_START_KEY = -1L
		private const val ALT_END_KEY = -2L
		private const val ALT_KEY_MULT = 4000000000L

		/** identifies the road point and the direction it is driven in, as the search itself does */
		private fun routePointKey(s: RouteSegment): Long {
			val start = s.getSegmentStart().toInt()
			return HHRouteDataStructure.calculateRoutePointInternalId(
				s.getRoad().getId(), start, start + (if (s.isPositive()) 1 else -1)
			)
		}

		/** the same road point driven the other way round - how the other tree stores it */
		private fun oppositeKey(s: RouteSegment): Long {
			val start = s.getSegmentStart().toInt()
			val end = start + (if (s.isPositive()) 1 else -1)
			return HHRouteDataStructure.calculateRoutePointInternalId(s.getRoad().getId(), end, start)
		}
	}
}
