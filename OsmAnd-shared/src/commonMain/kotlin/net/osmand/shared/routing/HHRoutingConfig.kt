package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The knobs of a hierarchical (HH) route calculation: which search to run over the hub graph, how
 * far it may go, how much of the detailed road network to rebuild afterwards, and what an
 * alternative route has to look like to be offered.
 *
 * The C++ router in core-legacy reads fourteen of these fields off the object it is handed, by name
 * and descriptor, so those are a contract with `native/src/java_wrap.cpp` that no build checks -
 * `HHRoutingConfigJniContractTest` is what does. `@JvmField` is a separate matter: it is for the
 * java callers that read and assign these as fields, and javac catches its absence.
 *
 * The rest are read by the java planner only, and are here because a configuration split in two -
 * half shared, half not - is worse than a few fields that only one caller uses.
 */
class HHRoutingConfig {

	@JvmField var HEURISTIC_COEFFICIENT = 0f // A* - 1, Dijkstra - 0
	@JvmField var DIJKSTRA_DIRECTION = 0f // 0 - 2 directions, 1 - positive, -1 - reverse

	/**
	 * The `HHRoutingContext` of a previous calculation, kept when [CACHE_CALCULATION_CONTEXT] is
	 * set so the next one can reuse its loaded points. Opaque here: that class holds
	 * `BinaryMapIndexReader`s and the java planner's graph nodes, so it stays in
	 * `net.osmand.router` and this field only carries it from one call to the next.
	 */
	@JvmField var cacheCtx: Any? = null

	// tweaks for route recalculations
	@JvmField var FULL_DIJKSTRA_NETWORK_RECALC = 10
	@JvmField var MAX_START_END_REITERATIONS = 50
	@JvmField var MAX_START_END_REITERATIONS_WITH_MISSING_MAPS = 5

	@JvmField var MAX_INC_COST_CF = 1.25
	@JvmField var MAX_INC_COST_CF_VIGILANT = 1.15

	@JvmField var MAX_COUNT_REITERATION = 30 // 3 is enough for 90%, 30 is for 10% (100-750km with 1.5m months live updates)
	@JvmField var MAX_COUNT_REITERATION_WITH_MISSING_MAPS = 3 // speed up HH-to-A* fallback if outdated maps were found

	@JvmField var INITIAL_DIRECTION: Double? = null

	@JvmField var STRICT_BEST_GROUP_MAPS = false // derived from RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS

	@JvmField var ROUTE_LAST_MILE = false
	@JvmField var ROUTE_ALL_SEGMENTS = false
	@JvmField var ROUTE_ALL_ALT_SEGMENTS = false
	@JvmField var PRELOAD_SEGMENTS = false

	@JvmField var CACHE_CALCULATION_CONTEXT = false
	@JvmField var CALC_ALTERNATIVES = false
	@JvmField var USE_GC_MORE_OFTEN = false

	// ---- alternative routes (plateau / via-node method), see HHRoutePlanner.calcAlternativeRoute ----
	// A candidate route goes through a "via node" v settled by both search trees: sp(s,v) + sp(v,t).
	// Its plateau is the maximal chain of hub-graph edges around v that belongs to BOTH trees, i.e.
	// the stretch the alternative drives as its own optimal road. Three explicit admissibility rules:
	//   1) stretch    cost(alt) <= (1 + ALT_STRETCH) * cost(opt) + ALT_STRETCH_ABS
	//   2) plateau    plateau(v) >= ALT_MIN_PLATEAU * cost(alt)          (local optimality)
	//   3) distinct   own roads >= max(ALT_MIN_DISTINCT_FLOOR, ALT_MIN_DISTINCT_REL * len(opt))
	// ALT_STRETCH also bounds the search horizon, so it directly trades quality for speed.
	@JvmField var ALT_MAX_COUNT = 2 // how many alternatives to return
	@JvmField var ALT_STRETCH = 0.4 // hard limit of relative cost overhead (and search bound)
	@JvmField var ALT_STRETCH_PREFERRED = 0.15 // alternatives below this limit are proposed first
	// A relative limit alone is harsh on a short route: on a 7 minute drive it rejects everything
	// that is not within 3 minutes of the fastest way, and the only other way through a city block
	// rarely is. On a route long enough for ALT_STRETCH to mean minutes this allowance is nothing.
	@JvmField var ALT_STRETCH_ABS = 180.0 // seconds allowed on top of ALT_STRETCH
	@JvmField var ALT_MIN_PLATEAU = 0.1 // min share of the route driven as its own optimal road
	@JvmField var ALT_MAX_SHARING = 0.6 // coarse hub-graph pre-filter (stage 1)
	@JvmField var ALT_MIN_DISTINCT_REL = 0.2 // exact geometry filter (stage 2), share of main length
	// Only a floor under the relative rule, for routes too short to make it meaningful: a fixed
	// requirement of a kilometre or two is a third of a 4 km city route and rejects everything
	// there, while asking nothing extra of a route long enough for the relative rule to bind.
	@JvmField var ALT_MIN_DISTINCT_FLOOR = 300.0 // exact geometry filter (stage 2), meters
	// Max detailed expansions in stage 2 (time guard). Retries of a candidate whose shortcuts
	// disagree with the detailed roads count against it, so this is not "number of candidates".
	@JvmField var ALT_MAX_EXPAND = 8
	@JvmField var ALT_MAX_RETRACED = 100.0 // meters an alternative may drive twice (u-turn tolerance)
	// The detailed graph is searched again when the hub graph offers nothing, and that search grows
	// with the route: measured 44 ms at 423 s of cost, 197 ms at 645 s, 274 ms at 978 s and 700 ms
	// on a 140 km route that had no alternative to find anyway. Beyond a city hop it is not worth it.
	@JvmField var ALT_DETAILED_MAX_COST = 900.0 // seconds, above this only the hub graph is searched
	@JvmField var ALT_RANK_COST_WEIGHT = 3.0 // rank: (1 - shared) - weight * stretch

	@JvmField var MAX_COST = 0.0
	@JvmField var MAX_DEPTH = -1 // max depth to go to
	@JvmField var MAX_SETTLE_POINTS = -1 // max points to settle

	@JvmField var USE_CH = false
	@JvmField var USE_CH_SHORTCUTS = false

	@JvmField var USE_MIDPOINT = false
	@JvmField var MIDPOINT_ERROR = 3
	@JvmField var MIDPOINT_MAX_DEPTH = 20 + MIDPOINT_ERROR

	fun preloadSegments(): HHRoutingConfig {
		PRELOAD_SEGMENTS = true
		return this
	}

	fun cacheContext(toCache: Any?): HHRoutingConfig {
		CACHE_CALCULATION_CONTEXT = true
		cacheCtx = toCache
		return this
	}

	fun calcAlternative(): HHRoutingConfig {
		CALC_ALTERNATIVES = true
		return this
	}

	fun calcAlternative(maxCount: Int): HHRoutingConfig {
		CALC_ALTERNATIVES = maxCount > 0
		ALT_MAX_COUNT = maxCount
		return this
	}

	fun calcDetailed(segments: Int): HHRoutingConfig {
		ROUTE_LAST_MILE = true
		ROUTE_ALL_SEGMENTS = segments >= 1
		ROUTE_ALL_ALT_SEGMENTS = segments >= 2
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
		val from = start?.toString() ?: "?"
		val to = end?.toString() ?: "?"
		return "Routing $from -> $to (HC ${HEURISTIC_COEFFICIENT.toInt()}, dir ${DIJKSTRA_DIRECTION.toInt()})"
	}

	companion object {
		const val CALCULATE_ALL_DETAILED = 3

		@JvmField
		var STATS_VERBOSE_LEVEL = 1 // 0 less verbose

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
