package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * How the HH (highway hierarchies) search is run: which search over the hub graph, how much of the
 * route is resolved on the detailed roads, how many times it may be recalculated when the hub graph
 * and the roads disagree, and whether alternatives are looked for.
 *
 * The apps use [astar] with [calcDetailed] of [CALCULATE_ALL_DETAILED]; the other factories are for
 * the tools that build and check the hub graph.
 *
 * A copy of `HHRouteDataStructure.HHRoutingConfig` in OsmAnd-java, which stays there for android
 * and tools; this copy is for iOS.
 */
class HHRoutingConfig {

	@JvmField
	var HEURISTIC_COEFFICIENT: Float = 0f // A* - 1, Dijkstra - 0

	@JvmField
	var DIJKSTRA_DIRECTION: Float = 0f // 0 - 2 directions, 1 - positive, -1 - reverse

	@JvmField
	var cacheCtx: HHRoutingContext? = null

	// tweaks for route recalculations
	@JvmField
	var FULL_DIJKSTRA_NETWORK_RECALC: Int = 10

	@JvmField
	var MAX_START_END_REITERATIONS: Int = 50

	@JvmField
	var MAX_START_END_REITERATIONS_WITH_MISSING_MAPS: Int = 5

	@JvmField
	var MAX_INC_COST_CF: Double = 1.25

	@JvmField
	var MAX_INC_COST_CF_VIGILANT: Double = 1.15

	@JvmField
	var MAX_COUNT_REITERATION: Int = 30 // 3 is enough for 90%, 30 is for 10% (100-750km with 1.5m months live updates)

	@JvmField
	var MAX_COUNT_REITERATION_WITH_MISSING_MAPS: Int = 3 // speed up HH-to-A* fallback if outdated maps were found

	@JvmField
	var INITIAL_DIRECTION: Double? = null

	@JvmField
	var STRICT_BEST_GROUP_MAPS: Boolean = false // derived from RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS

	@JvmField
	var ROUTE_LAST_MILE: Boolean = false

	@JvmField
	var ROUTE_ALL_SEGMENTS: Boolean = false

	@JvmField
	var ROUTE_ALL_ALT_SEGMENTS: Boolean = false

	@JvmField
	var PRELOAD_SEGMENTS: Boolean = false

	@JvmField
	var CACHE_CALCULATION_CONTEXT: Boolean = false

	@JvmField
	var CALC_ALTERNATIVES: Boolean = false

	@JvmField
	var USE_GC_MORE_OFTEN: Boolean = false

	// ---- alternative routes (plateau / via-node method), see HHRoutePlanner.calcAlternativeRoute ----
	// A candidate route goes through a "via node" v settled by both search trees: sp(s,v) + sp(v,t).
	// Its plateau is the maximal chain of hub-graph edges around v that belongs to BOTH trees, i.e.
	// the stretch the alternative drives as its own optimal road. Three explicit admissibility rules:
	//   1) stretch    cost(alt) <= (1 + ALT_STRETCH) * cost(opt) + ALT_STRETCH_ABS
	//   2) plateau    plateau(v) >= ALT_MIN_PLATEAU * cost(alt)          (local optimality)
	//   3) distinct   own roads >= max(ALT_MIN_DISTINCT_FLOOR, ALT_MIN_DISTINCT_REL * len(opt))
	// ALT_STRETCH also bounds the search horizon, so it directly trades quality for speed.
	@JvmField
	var ALT_MAX_COUNT: Int = 2 // how many alternatives to return

	@JvmField
	var ALT_STRETCH: Double = 0.4 // hard limit of relative cost overhead (and search bound)

	@JvmField
	var ALT_STRETCH_PREFERRED: Double = 0.15 // alternatives below this limit are proposed first

	// A relative limit alone is harsh on a short route: on a 7 minute drive it rejects everything
	// that is not within 3 minutes of the fastest way, and the only other way through a city block
	// rarely is. On a route long enough for ALT_STRETCH to mean minutes this allowance is nothing.
	@JvmField
	var ALT_STRETCH_ABS: Double = 180.0 // seconds allowed on top of ALT_STRETCH

	@JvmField
	var ALT_MIN_PLATEAU: Double = 0.1 // min share of the route driven as its own optimal road

	@JvmField
	var ALT_MAX_SHARING: Double = 0.6 // coarse hub-graph pre-filter (stage 1)

	@JvmField
	var ALT_MIN_DISTINCT_REL: Double = 0.2 // exact geometry filter (stage 2), share of main length

	// Only a floor under the relative rule, for routes too short to make it meaningful: a fixed
	// requirement of a kilometre or two is a third of a 4 km city route and rejects everything
	// there, while asking nothing extra of a route long enough for the relative rule to bind.
	@JvmField
	var ALT_MIN_DISTINCT_FLOOR: Double = 300.0 // exact geometry filter (stage 2), meters

	// Max detailed expansions in stage 2 (time guard). Retries of a candidate whose shortcuts
	// disagree with the detailed roads count against it, so this is not "number of candidates".
	@JvmField
	var ALT_MAX_EXPAND: Int = 8

	@JvmField
	var ALT_MAX_RETRACED: Double = 100.0 // meters an alternative may drive twice (u-turn tolerance)

	// The detailed graph is searched again when the hub graph offers nothing, and that search grows
	// with the route: measured 44 ms at 423 s of cost, 197 ms at 645 s, 274 ms at 978 s and 700 ms
	// on a 140 km route that had no alternative to find anyway. Beyond a city hop it is not worth it.
	@JvmField
	var ALT_DETAILED_MAX_COST: Double = 900.0 // seconds, above this only the hub graph is searched

	@JvmField
	var ALT_RANK_COST_WEIGHT: Double = 3.0 // rank: (1 - shared) - weight * stretch

	@JvmField
	var MAX_COST: Double = 0.0

	@JvmField
	var MAX_DEPTH: Int = -1 // max depth to go to

	@JvmField
	var MAX_SETTLE_POINTS: Int = -1 // max points to settle

	@JvmField
	var USE_CH: Boolean = false

	@JvmField
	var USE_CH_SHORTCUTS: Boolean = false

	@JvmField
	var USE_MIDPOINT: Boolean = false

	@JvmField
	var MIDPOINT_ERROR: Int = 3

	@JvmField
	var MIDPOINT_MAX_DEPTH: Int = 20 + MIDPOINT_ERROR

	fun preloadSegments(): HHRoutingConfig {
		this.PRELOAD_SEGMENTS = true
		return this
	}

	fun cacheContext(toCache: HHRoutingContext?): HHRoutingConfig {
		this.CACHE_CALCULATION_CONTEXT = true
		this.cacheCtx = toCache
		return this
	}

	fun calcAlternative(): HHRoutingConfig {
		this.CALC_ALTERNATIVES = true
		return this
	}

	fun calcAlternative(maxCount: Int): HHRoutingConfig {
		this.CALC_ALTERNATIVES = maxCount > 0
		this.ALT_MAX_COUNT = maxCount
		return this
	}

	fun calcDetailed(segments: Int): HHRoutingConfig {
		this.ROUTE_LAST_MILE = true
		this.ROUTE_ALL_SEGMENTS = segments >= 1
		this.ROUTE_ALL_ALT_SEGMENTS = segments >= 2
		return this
	}

	fun useShortcuts(): HHRoutingConfig {
		USE_CH_SHORTCUTS = true
		return this
	}

	fun gc(): HHRoutingConfig {
		USE_GC_MORE_OFTEN = true
		return this
	}

	fun maxCost(cost: Double): HHRoutingConfig {
		MAX_COST = cost
		return this
	}

	fun maxDepth(depth: Int): HHRoutingConfig {
		MAX_DEPTH = depth
		return this
	}

	fun maxSettlePoints(maxPoints: Int): HHRoutingConfig {
		MAX_SETTLE_POINTS = maxPoints
		return this
	}

	fun applyCalculateMissingMaps(calculateMissingMaps: Boolean): HHRoutingConfig {
		STRICT_BEST_GROUP_MAPS = calculateMissingMaps
		return this
	}

	override fun toString(): String = toString(null, null)

	fun toString(start: KLatLon?, end: KLatLon?): String {
		return "Routing %s -> %s (HC %d, dir %d)".format(
			start?.toString() ?: "?", end?.toString() ?: "?",
			HEURISTIC_COEFFICIENT.toInt(), DIJKSTRA_DIRECTION.toInt()
		)
	}

	companion object {
		const val CALCULATE_ALL_DETAILED = 3

		@JvmField
		var STATS_VERBOSE_LEVEL: Int = 1 // 0 less verbose

		@JvmStatic
		fun dijkstra(direction: Int): HHRoutingConfig {
			val df = HHRoutingConfig()
			df.HEURISTIC_COEFFICIENT = 0f
			df.DIJKSTRA_DIRECTION = direction.toFloat()
			return df
		}

		@JvmStatic
		fun astar(direction: Int): HHRoutingConfig {
			val df = HHRoutingConfig()
			df.HEURISTIC_COEFFICIENT = 1f
			df.DIJKSTRA_DIRECTION = direction.toFloat()
			return df
		}

		@JvmStatic
		fun ch(): HHRoutingConfig {
			val df = HHRoutingConfig()
			df.HEURISTIC_COEFFICIENT = 0f
			df.USE_CH = true
			df.USE_CH_SHORTCUTS = true
			df.DIJKSTRA_DIRECTION = 0f
			return df
		}

		@JvmStatic
		fun midPoints(astar: Boolean, dir: Int): HHRoutingConfig {
			val df = HHRoutingConfig()
			df.HEURISTIC_COEFFICIENT = if (astar) 1f else 0f
			df.USE_MIDPOINT = true
			df.DIJKSTRA_DIRECTION = dir.toFloat()
			return df
		}
	}
}
