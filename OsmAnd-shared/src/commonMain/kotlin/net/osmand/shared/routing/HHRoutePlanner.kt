package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.HHRouteRegion
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.format
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.routing.HHRouteDataStructure.calcRPId
import net.osmand.shared.routing.HHRouteDataStructure.calcUniDirRoutePointInternalId
import net.osmand.shared.routing.HHRouteDataStructure.calculateRoutePointInternalId
import net.osmand.shared.routing.RouteCalculationProgress.HHIteration
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.ExcludeKTLongObjectMap
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

/**
 * The HH (highway hierarchies) route search: a bidirectional A* over the hub graph the map files
 * carry - the entries and exits of clusters of roads, with the cost of the fastest way between
 * them precomputed - and the detailed roads only where the route touches them: the last mile at
 * both ends, then every hub-graph edge of the route resolved to roads by the A* planner bounded by
 * the graph's vertices. When a resolved edge costs more than the graph promised (live map updates,
 * a parameter the graph was not built for) the edge is corrected and the search runs again.
 *
 * A copy of `HHRoutePlanner` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. Java's `T extends NetworkDBPoint` is [NetworkDBPoint] here, and the planner over the
 * tools' SQLite `HHRoutingDB` (`createDB`) is not copied: the apps route over obf files through
 * [create]. The point id helpers sit on [HHRouteDataStructure].
 */
class HHRoutePlanner private constructor(ctx: RoutingContext) {

	private var currentCtx: HHRoutingContext // never null

	private var maxCountReiteration = 0
	private var maxStartEndReiterations = 0
	private var incorrectCostAtStartEnd = false

	init {
		currentCtx = initNewContext(ctx, null)
	}

	private fun initNewContext(ctx: RoutingContext, regions: List<HHRouteRegionPointsCtx>?): HHRoutingContext {
		val c = HHRoutingContext()
		c.rctx = ctx
		if (regions != null) {
			c.regions.addAll(regions)
		}
		currentCtx = c
		return c
	}

	fun close() {
		currentCtx.regions.clear()
	}

	private inline fun printf(cond: Boolean, message: () -> String) {
		if (cond) {
			LOG.info(message())
		}
	}

	fun runRouting(start: KLatLon, end: KLatLon, configArg: HHRoutingConfig?): HHNetworkRouteRes {
		val startTime = nanoTime()
		val sl = HHRoutingConfig.STATS_VERBOSE_LEVEL
		val rctx = currentCtx.requireContext()
		val progress = progressOf(rctx)
		// important assumption that routing context match!
		val cached = configArg?.cacheCtx
		if (cached != null && cached.rctx === currentCtx.rctx) {
			currentCtx = cached
		}
		val config = prepareDefaultRoutingConfig(configArg)
		val hctx = initHCtx(config, start, end)
		if (config.CACHE_CALCULATION_CONTEXT) {
			val previous = config.cacheCtx
			if (previous != null && previous !== hctx) {
				LOG.info("Recreate routing cache context ${previous.hashCode()} -> ${hctx?.hashCode() ?: ""}")
			}
			config.cacheCtx = hctx
		}
		if (hctx == null) {
			progress.raiseFastRoutingStatus(FastRoutingState.Status.FAILED_NO_HH_ROUTING_DATA)
			return HHNetworkRouteRes("Files for hh routing were not initialized. Route couldn't be calculated.")
		}

		if (progress.hasMixedOrMissingMaps()) {
			maxStartEndReiterations = config.MAX_START_END_REITERATIONS_WITH_MISSING_MAPS
			maxCountReiteration = config.MAX_COUNT_REITERATION_WITH_MISSING_MAPS
		} else {
			maxStartEndReiterations = config.MAX_START_END_REITERATIONS
			maxCountReiteration = config.MAX_COUNT_REITERATION
		}

		filterPointsBasedOnConfiguration(hctx)

		val stPoints = KTLongObjectMap<NetworkDBPoint>()
		val endPoints = KTLongObjectMap<NetworkDBPoint>()
		progress.hhIteration(HHIteration.START_END_POINT)
		findFirstLastSegments(hctx, start, end, stPoints, endPoints, progress)

		incorrectCostAtStartEnd = false
		var route: HHNetworkRouteRes? = null
		var recalc = false
		var firstIterationTime = 0.0
		var iteration = 0
		var calcCount = 0
		while (route == null) {
			progress.hhUpdateCalcCounter(calcCount)
			progress.hhIteration(if (calcCount == 0) HHIteration.ROUTING else HHIteration.RECALCULATION)
			iteration++
			if (recalc && firstIterationTime == 0.0) {
				printf(DEBUG_VERBOSE_LEVEL == 0 && sl > 0) { "  Recalculating route due to route structure changes..." }
				firstIterationTime = hctx.stats.routingTime
			}
			printf((!recalc || DEBUG_VERBOSE_LEVEL > 0) && sl > 0) { " Routing..." }
			var time = nanoTime()
			val finalPnt = runRoutingPointsToPoints(hctx, stPoints, endPoints)
			if (finalPnt == null) {
				printf(sl > 0) { " finalPnt is null (stop)" }
				hctx.clearAll(stPoints, endPoints)
				progress.failFastRoutingStatus(rctx.hhHasUnsupportedParameters)
				return HHNetworkRouteRes("No finalPnt found (points might be filtered by params)")
			}
			if (progress.isCancelled) {
				return cancelledStatus(hctx, stPoints, endPoints)
			}
			val found = createRouteSegmentFromFinalPoint(hctx, finalPnt)
			route = found
			time = nanoTime() - time
			printf((!recalc || DEBUG_VERBOSE_LEVEL > 0) && sl > 0) {
				"%d segments, cost %.2f, %.2f ms".format(found.segments.size, found.getHHRoutingTime(), time / 1e6)
			}
			hctx.stats.routingTime += time / 1e6

			progress.hhIteration(if (calcCount == 0) HHIteration.DETAILED else HHIteration.RECALCULATION)
			printf((!recalc || DEBUG_VERBOSE_LEVEL > 0) && sl > 0) { " Parse detailed route segments..." }
			time = nanoTime()
			recalc = retrieveSegmentsGeometry(hctx, found, hctx.requireConfig().ROUTE_ALL_SEGMENTS, progress)
			if (progress.isCancelled) {
				return cancelledStatus(hctx, stPoints, endPoints)
			}
			time = nanoTime() - time
			printf((firstIterationTime == 0.0 || DEBUG_VERBOSE_LEVEL > 0) && sl > 0) { "%.2f ms".format(time / 1e6) }
			hctx.stats.routingTime += time / 1e6
			calcCount++
			if (recalc) {
				if (calcCount > maxCountReiteration) {
					if (sl >= 0) {
						printFinalMessage(" [too many cancelled]", start, end, startTime, hctx)
					}
					hctx.clearAll(stPoints, endPoints)
					progress.failFastRoutingStatus(rctx.hhHasUnsupportedParameters)
					return HHNetworkRouteRes("Too many recalculations (outdated maps or unsupported parameters).")
				}
				hctx.clearVisited(stPoints, endPoints)
				route = null
			}
		}
		if (firstIterationTime > 0) {
			printf(DEBUG_VERBOSE_LEVEL == 0 && sl > 0) {
				"%d iterations, %.2f ms".format(iteration, hctx.stats.routingTime - firstIterationTime)
			}
		}
		if (hctx.requireConfig().CALC_ALTERNATIVES) {
			progress.hhIteration(HHIteration.ALTERNATIVES)
			printf(sl > 0) { " Alternative routes..." }
			val time = nanoTime()
			// detailed geometry of the alternatives is retrieved inside - it is needed to reject
			// candidates that turn out to run on the very same roads as the main route
			HHAlternativeRoutes(this, hctx).calcAlternativeRoute(route, start, end, progress)
			if (progress.isCancelled) {
				return cancelledStatus(hctx, stPoints, endPoints)
			}
			hctx.stats.altRoutingTime += (nanoTime() - time) / 1e6
			hctx.stats.routingTime += hctx.stats.altRoutingTime
			printf(sl > 0) { "%d %.2f ms".format(route.altRoutes.size, hctx.stats.altRoutingTime) }
		}
		val time = nanoTime()
		printf(sl > 0) { " Prepare results (turns, alt routes)..." }

		if (hctx.requireConfig().USE_GC_MORE_OFTEN) {
			hctx.unloadAllConnections()
			printGCInformation(true)
		}

		prepareRouteResults(hctx, route, start, end)
		if (DEBUG_VERBOSE_LEVEL >= 1) {
			LOG.info("  Detailed progress: " + progress.getInfo(null))
		}
		if (progress.isCancelled) {
			return cancelledStatus(hctx, stPoints, endPoints)
		}
		if (hctx.requireConfig().ROUTE_ALL_SEGMENTS) {
			route.detailed = RouteResultPreparation.prepareResult(rctx, route.detailed).detailed
		}
		hctx.stats.prepTime += (nanoTime() - time) / 1e6
		printf(sl > 0) { "%.2f ms".format(hctx.stats.prepTime) }
		printf(sl > 0) {
			("Found final route - cost %.2f (detailed %.2f, %.1f%%), %d depth ( first met %d, visited %d (%d unique) of %d added vertices )").format(
				route.getHHRoutingTime(), route.getHHRoutingDetailed(),
				100 * (1 - route.getHHRoutingDetailed() / (route.getHHRoutingTime() + 0.01)),
				route.segments.size, hctx.stats.firstRouteVisitedVertices, hctx.stats.visitedVertices,
				hctx.stats.uniqueVisitedVertices, hctx.stats.addedVertices
			)
		}
		printGCInformation(false)
		hctx.clearAll(stPoints, endPoints)
		if (sl >= 0) {
			printFinalMessage("", start, end, startTime, hctx)
		}
		progress.raiseFastRoutingStatus(FastRoutingState.Status.SUCCESS)
		return route
	}

	private fun printFinalMessage(msg: String, start: KLatLon, end: KLatLon, startTime: Long, hctx: HHRoutingContext) {
		LOG.info(
			("Routing%s %.1f ms (ctx %s): load/filter points %.1f ms, last mile %.1f ms, routing %.1f ms (queue  - %.1f ms, %.1f ms - %d edges), prep result %.1f ms - %s (selected %s)").format(
				msg, (nanoTime() - startTime) / 1e6, hctx.hashCode().toString(),
				hctx.stats.loadPointsTime, hctx.stats.searchPointsTime,
				hctx.stats.routingTime, hctx.stats.addQueueTime + hctx.stats.pollQueueTime,
				hctx.stats.loadEdgesTime, hctx.stats.loadEdgesCnt, hctx.stats.prepTime,
				hctx.requireConfig().toString(start, end), hctx.getRoutingInfo()
			)
		)
	}

	private fun filterPointsBasedOnConfiguration(hctx: HHRoutingContext) {
		val rctx = hctx.requireContext()
		val tm = getFilteredTags(rctx.config.router)
		if (hctx.filterRoutingParameters == tm) {
			return
		}
		hctx.pointsById.forEachValue { pnt ->
			pnt.rtExclude = false
		}
		if (tm.isEmpty()) {
			// no parameters
			hctx.filterRoutingParameters = tm
			return
		}
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { " Filter points based on parameters..." }
		val nt = nanoTime()
		val regR = RouteRegion()
		val tint = KTIntArrayList(50)
		val rdo = RouteDataObject(regR)
		hctx.pointsById.forEachValue { pnt ->
			pnt.tagValues?.let { tagValues ->
				for (tp in tagValues) {
					tp.additionalAttribute = -1
				}
			}
		}
		var filtered = 0
		val router = currentCtx.requireContext().getRouter()
		hctx.pointsById.forEachValue { pnt ->
			val tagValues = pnt.tagValues
			if (tagValues != null) {
				tint.clear()
				for (tp in tagValues) {
					// reuse additionalAttribute to cache values
					if (tp.additionalAttribute < 0) {
						tp.additionalAttribute = regR.searchRouteEncodingRule(tp.tag ?: "", tp.value)
					}
					if (tp.additionalAttribute < 0) {
						tp.additionalAttribute = regR.routeEncodingRules.size
						regR.initRouteEncodingRule(tp.additionalAttribute, tp.tag ?: "", tp.value)
					}
					tint.add(tp.additionalAttribute)
				}
				// here we always copy array but in C++ we could be more efficient
				rdo.types = tint.toArray()
				pnt.rtExclude = !router.acceptLine(rdo)
				// This might speed up for certain avoid parameters
				// but produces unpredictably wrong results (prefer_unpaved) when shortcuts are calculated - error 300%
//				if (!pnt.rtExclude) {
//					// constant should be reduced if route is not found
//					pnt.rtExclude = currentCtx.rctx.getRouter().defineSpeedPriority(rdo, pnt.end > pnt.start) < EXCLUDE_PRIORITY_CONSTANT;
//				}
				if (pnt.rtExclude) {
					filtered++
				}
			}
		}
		hctx.filterRoutingParameters = tm
		val time = (nanoTime() - nt) / 1e6
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { "%d excluded from %d, %.2f ms".format(filtered, hctx.pointsById.size, time) }
		hctx.stats.loadPointsTime += time
	}

	private fun findFirstLastSegments(
		hctx: HHRoutingContext, start: KLatLon, end: KLatLon,
		stPoints: KTLongObjectMap<NetworkDBPoint>, endPoints: KTLongObjectMap<NetworkDBPoint>,
		progress: RouteCalculationProgress
	) {
		val time = nanoTime()
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { " Finding first / last segments..." }
		val rctx = hctx.requireContext()
		val planner = RoutePlannerFrontEnd()
		var startReiterate = -1
		var endReiterate = -1
		var found = false
		progress.hhIterationProgress(0.00) // %
		val startPnt = planner.findRouteSegment(start.latitude, start.longitude, rctx, null) ?: return
		progress.hhIterationProgress(0.25) // %
		val endPnt = planner.findRouteSegment(end.latitude, end.longitude, rctx, null) ?: return
		val stOthers = startPnt.others
		val endOthers = endPnt.others
		while (!found) {
			if (startReiterate + endReiterate >= maxStartEndReiterations) {
				break
			}
			stPoints.forEachValue { p ->
				p.clearRouting()
			}
			stPoints.clear()
			endPoints.forEachValue { p ->
				p.clearRouting()
			}
			endPoints.clear()
			var startP = startPnt
			if (startReiterate >= 0) {
				if (stOthers != null && startReiterate < stOthers.size) {
					startP = stOthers[startReiterate]
				} else {
					break
				}
			}
			var endP = endPnt
			if (endReiterate >= 0) {
				if (endOthers != null && endReiterate < endOthers.size) {
					endP = endOthers[endReiterate]
				} else {
					break
				}
			}
			val prev = rctx.config.initialDirection
			rctx.config.initialDirection = hctx.requireConfig().INITIAL_DIRECTION
			hctx.boundaries.add(calcRPId(endP, endP.getSegmentEnd().toInt(), endP.getSegmentStart().toInt()))
			hctx.boundaries.add(calcRPId(endP, endP.getSegmentStart().toInt(), endP.getSegmentEnd().toInt()))
			progress.hhIterationProgress(0.50) // %
			hctx.startSegment = startP
			hctx.endSegment = endP
			initStart(hctx, startP, false, stPoints)
			rctx.config.initialDirection = prev
			if (stPoints.isEmpty()) {
				printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { "   Reiterate with next start point: $startP" }
				startReiterate++
				found = false
				continue
			}

			hctx.boundaries.remove(calcRPId(endP, endP.getSegmentEnd().toInt(), endP.getSegmentStart().toInt()))
			hctx.boundaries.remove(calcRPId(endP, endP.getSegmentStart().toInt(), endP.getSegmentEnd().toInt()))
			val shortRoute = stPoints[PNT_SHORT_ROUTE_START_END.toLong()]
			if (shortRoute != null) {
				endPoints.put(PNT_SHORT_ROUTE_START_END.toLong(), shortRoute)
			}
			progress.hhIterationProgress(0.75) // %
			initStart(hctx, endP, true, endPoints)
			if (endPoints.isEmpty()) {
				printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { "   Reiterate with next end point: $endP" }
				endReiterate++
				found = false
				continue
			}
			found = true
		}

		hctx.stats.searchPointsTime = (nanoTime() - time) / 1e6
		if (DEBUG_VERBOSE_LEVEL > 1) {
			stPoints.forEachValue { p ->
				val pi = p.rt(false)
				LOG.info("   Start point %d cost %.2f, dist = %.2f".format(p.index, pi.rtCost, pi.rtDistanceFromStart))
			}
			endPoints.forEachValue { p ->
				val pi = p.rt(true)
				LOG.info("   End point %d cost %.2f, dist = %.2f".format(p.index, pi.rtCost, pi.rtDistanceFromStart))
			}
		}
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
			" Finding first (%d) / last (%d) segments...%.2f ms".format(stPoints.size, endPoints.size, hctx.stats.searchPointsTime)
		}
	}

	private fun initHCtx(c: HHRoutingConfig, start: KLatLon, end: KLatLon): HHRoutingContext? {
		val progress = progressOf(currentCtx.requireContext())
		progress.hhIteration(HHIteration.SELECT_REGIONS)
		val hctx = selectBestRoutingFiles(start, end, currentCtx, c.STRICT_BEST_GROUP_MAPS)
		if (hctx == null) {
			LOG.info("No files found for routing")
			return null
		}
		if (HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
			LOG.info("Selected files: " + hctx.getRoutingInfo())
		}
		hctx.stats = RoutingStats()
		hctx.config = c
		hctx.setStartEnd(start, end)
		hctx.clearVisited()
		if (hctx.initialized) {
			return hctx
		}

		var time = nanoTime()
		progress.hhIteration(HHIteration.LOAD_POINTS)
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { "Loading points... " }
		hctx.pointsById = hctx.loadNetworkPoints()
		hctx.boundaries = KTLongHashSet()
		hctx.pointsByGeo = KTLongObjectMap()
		if (c.PRELOAD_SEGMENTS) {
			time = nanoTime()
			printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { "Loading segments..." }
			val cntEdges = hctx.loadNetworkSegments(hctx.pointsById.values())
			hctx.stats.loadEdgesTime = (nanoTime() - time) / 1e6
			printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { " %d - %.2fms".format(cntEdges, hctx.stats.loadEdgesTime) }
			hctx.stats.loadEdgesCnt = cntEdges
		} else {
			hctx.pointsById.forEachValue { p ->
				p.markSegmentsNotLoaded()
			}
		}
		hctx.clusterOutPoints = HHRouteDataStructure.groupByClusters(hctx.pointsById, true)
		hctx.clusterInPoints = HHRouteDataStructure.groupByClusters(hctx.pointsById, false)
		hctx.pointsById.forEachValue { pnt ->
			val pos = calculateRoutePointInternalId(pnt.roadId, pnt.start.toInt(), pnt.end.toInt())
			val latlon = pnt.getPoint()
			hctx.pointsRect.registerObject(latlon.latitude, latlon.longitude, pnt)
			if (pos != pnt.getGeoPntId()) {
				throw IllegalStateException("$pnt $pos != ${pnt.getGeoPntId()}")
			}
			hctx.boundaries.add(pos)
			hctx.pointsByGeo.put(pos, pnt)
			hctx.regions[pnt.mapId.toInt()].pntsByFileId.put(pnt.fileId, pnt)
		}
		hctx.initialized = true
		hctx.stats.loadPointsTime = (nanoTime() - time) / 1e6
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) { " %d - %.2fms".format(hctx.pointsById.size, hctx.stats.loadPointsTime) }
		return hctx
	}

	/** The HH sections of one edition and one parameter set, over the files that carry them. */
	private class HHRouteRegionsGroup(val edition: Long, val profileParams: String) {
		val regions = ArrayList<HHRouteRegion>()
		val readers = ArrayList<BinaryMapIndexReader>()

		var extraParam = 0
		var matchParam = 0
		var highCostParam = 0
		var unsupportedParams = 0 // affects FastRoutingState via hhHasUnsupportedParameters

		var containsStartEnd = false
		var containsStartEndByBbox = false
		var sumIntersects = 0.0

		fun contains(p: KLatLon): Boolean {
			val x31 = KMapUtils.get31TileNumberX(p.longitude)
			val y31 = KMapUtils.get31TileNumberY(p.latitude)
			val checked = HashSet<String>()
			for (i in regions.indices) {
				val rd = readers[i]
				if (rd.containsRouteData()) {
					if (rd.containsActualRouteData(x31, y31, checked)) {
						return true
					}
				} else {
					val reg = regions[i]
					if (reg.top?.contains(x31, y31) == true) {
						return true
					}
				}
			}
			return false
		}

		fun containsStartEndRegion(regionsCoveringStartAndTargets: Array<String>): Boolean {
			if (regionsCoveringStartAndTargets.isEmpty()) {
				return true
			}
			for (reader in readers) {
				for (index in reader.getRoutingIndexes()) {
					for (region in regionsCoveringStartAndTargets) {
						if (region.equals(index.getName(), ignoreCase = true)) {
							return true
						}
					}
				}
			}
			return false
		}

		companion object {
			fun appendToGroups(r: HHRouteRegion, rdr: BinaryMapIndexReader, groups: MutableList<HHRouteRegionsGroup>, iou: Double) {
				for (params in r.profileParams) {
					var matchGroup: HHRouteRegionsGroup? = null
					for (g in groups) {
						if (g.edition == r.edition && params == g.profileParams) {
							matchGroup = g
							break
						}
					}
					if (matchGroup == null) {
						matchGroup = HHRouteRegionsGroup(r.edition, params)
						groups.add(matchGroup)
					}
					matchGroup.regions.add(r)
					matchGroup.readers.add(rdr)
					matchGroup.sumIntersects += iou
				}
			}
		}
	}

	internal fun selectBestRoutingFiles(
		start: KLatLon, end: KLatLon, hctx: HHRoutingContext, strictBestGroupMaps: Boolean
	): HHRoutingContext? {
		val groups = ArrayList<HHRouteRegionsGroup>()
		val rctx = hctx.requireContext()

		val router = rctx.config.router
		val profile = router.getProfile().toString().lowercase() // use base profile
		val qr = KQuadRect(
			min(start.longitude, end.longitude), kotlin.math.max(start.latitude, end.latitude),
			kotlin.math.max(start.longitude, end.longitude), min(start.latitude, end.latitude)
		)

		for (r in rctx.map.keys) {
			for (hhregion in r.getHHRoutingIndexes()) {
				if (hhregion.profile == profile && KQuadRect.intersects(hhregion.getLatLonBbox(), qr)) {
					val intersect = KQuadRect.intersectionArea(hhregion.getLatLonBbox(), qr)
					HHRouteRegionsGroup.appendToGroups(hhregion, r, groups, intersect)
				}
			}
		}
		for (g in groups) {
			g.containsStartEndByBbox = g.contains(start) && g.contains(end)
			g.containsStartEnd = g.containsStartEndByBbox
					&& g.containsStartEndRegion(rctx.regionsCoveringStartAndTargets)
			val params = TurnLanes.splitDroppingTrailingEmpty(g.profileParams, ",")
			matchGroupRoutingParams(params, router, g)
		}
		groups.sortWith { o1, o2 ->
			if (o1.containsStartEnd != o2.containsStartEnd) {
				if (o1.containsStartEnd) -1 else 1
			} else if (o1.containsStartEndByBbox != o2.containsStartEndByBbox) {
				// Keep polygon matches first, then prefer endpoint coverage over a newer edition.
				if (o1.containsStartEndByBbox) -1 else 1
			} else if (o1.edition != o2.edition) {
				if (o1.edition > o2.edition) -1 else 1
			} else if (o1.extraParam != o2.extraParam) {
				if (o1.extraParam < o2.extraParam) -1 else 1
			} else if (o1.matchParam != o2.matchParam) {
				if (o1.matchParam > o2.matchParam) -1 else 1
			} else if (o1.highCostParam != o2.highCostParam) {
				if (o1.highCostParam > o2.highCostParam) -1 else 1
			} else {
				-o1.sumIntersects.compareTo(o2.sumIntersects) // higher is better
			}
		}
		if (groups.isEmpty()) {
			return null
		}
		val bestGroup = groups[0]
		val regions = ArrayList<HHRouteRegionPointsCtx>()
		for (mapId in bestGroup.regions.indices) {
			val reg = HHRouteRegionPointsCtx(
				mapId.toShort(), bestGroup.regions[mapId], bestGroup.readers[mapId],
				bestGroup.regions[mapId].profileParams.indexOf(bestGroup.profileParams)
			)
			regions.add(reg)
		}
		var allMatched = true
		for (r in regions) {
			var match = false
			for (p in currentCtx.regions) {
				if (p.file === r.file && p.fileRegion === r.fileRegion && p.routingProfile == r.routingProfile) {
					match = true
					break
				}
			}
			if (!match) {
				allMatched = false
				break
			}
		}
		if (allMatched) {
			return currentCtx
		}
		if (strictBestGroupMaps && groups.size > 1) {
			val filter = HashSet<BinaryMapIndexReader>()
			for (reg in regions) {
				filter.add(reg.file)
			}
			rctx.mapIndexReaderFilter = filter
		}
		rctx.hhHasUnsupportedParameters = bestGroup.unsupportedParams > 0
		return initNewContext(rctx, regions)
	}

	private fun matchGroupRoutingParams(hhParams: List<String>, router: GeneralRouter, group: HHRouteRegionsGroup) {
		val routerParams = router.serializeParameterValues(router.getParameterValues())

		for (p in hhParams) {
			if (p.trim().isEmpty()) {
				continue
			}
			if (!routerParams.contains(p)) {
				group.extraParam++
			} else {
				if (HIGH_COST_PARAMS.contains(p)) {
					group.highCostParam++
				}
				group.matchParam++
			}
		}

		for (keyVal in routerParams) {
			val keyValue = TurnLanes.splitDroppingTrailingEmpty(keyVal, "=")
			val key = keyValue[0]
			val param = router.getParameters()[key]
			if (param == null || IGNORE_FAILED_UNSUPPORTED_PARAMETERS.contains(key) || hhParams.contains(keyVal)) {
				continue
			}
			var doubleValue = 0.0
			var booleanValue = true
			if (keyVal.contains("=")) {
				val v = keyValue[1]
				if ("true" == v || "false" == v) {
					booleanValue = v.toBoolean()
				} else {
					doubleValue = KAlgorithms.parseDoubleSilently(v, 0.0)
				}
			}
			if (GeneralRouter.RoutingParameterType.BOOLEAN == param.getType()
				&& booleanValue == param.getDefaultBoolean()
			) {
				continue
			} else if (GeneralRouter.RoutingParameterType.NUMERIC == param.getType()
				&& doubleValue == param.getDefaultNumeric()
			) {
				continue
			}
			group.unsupportedParams++
		}
	}

	private fun initStart(
		hctx: HHRoutingContext, s: RouteSegmentPoint?, reverse: Boolean, pnts: KTLongObjectMap<NetworkDBPoint>
	): KTLongObjectMap<NetworkDBPoint> {
		val rctx = hctx.requireContext()
		val config = hctx.requireConfig()
		if (!config.ROUTE_LAST_MILE) {
			// simple method to calculate without detailed maps
			val startLat = KMapUtils.get31LatitudeY(if (!reverse) hctx.startY else hctx.endY)
			val startLon = KMapUtils.get31LongitudeX(if (!reverse) hctx.startX else hctx.endX)
			var rad = 10000.0
			val spd = rctx.getRouter().getMinSpeed()
			while (rad < 300000 && pnts.isEmpty()) {
				rad *= 2
				val pntSelect = hctx.pointsRect.getClosestObjects(startLat, startLon, rad)
				// limit by cluster
				val cid = pntSelect[0].clusterId
				for (pSelect in pntSelect) {
					if (pSelect.clusterId != cid) {
						continue
					}
					val pnt = if (reverse) pSelect.dualPoint!! else pSelect
					val cost = KMapUtils.getDistance(pnt.getPoint(), startLat, startLon) / spd
					pnt.setCostParentRt(reverse, cost + hctx.distanceToEnd(reverse, pnt), null, cost)
					pnts.put(pnt.index.toLong(), pnt)
				}
			}
			return pnts
		}
		if (s == null) {
			return pnts
		}
		val finitePnt = hctx.pointsByGeo[calcUniDirRoutePointInternalId(s)]
		if (finitePnt != null) {
			// start / end point is directly on a network point
			var plusCost = 0.0
			var negCost = 0.0
			val initialDirection = rctx.config.initialDirection
			if (initialDirection != null) {
				var diff = s.getRoad().directionRoute(s.getSegmentStart().toInt(), s.isPositive()) - initialDirection
				if (abs(KMapUtils.alignAngleDifference(diff - PI)) <= PI / 3) {
					plusCost += rctx.config.penaltyForReverseDirection
				}
				diff = s.getRoad().directionRoute(s.getSegmentEnd().toInt(), !s.isPositive()) - initialDirection
				if (abs(KMapUtils.alignAngleDifference(diff - PI)) <= PI / 3) {
					negCost += rctx.config.penaltyForReverseDirection
				}
			}
			finitePnt.setDistanceToEnd(reverse, hctx.distanceToEnd(reverse, finitePnt))
			finitePnt.setCostParentRt(reverse, plusCost, null, plusCost)
			pnts.put(finitePnt.index.toLong(), finitePnt)

			val dualPoint = finitePnt.dualPoint!!
			dualPoint.setDistanceToEnd(reverse, hctx.distanceToEnd(reverse, dualPoint))
			dualPoint.setCostParentRt(reverse, negCost, null, negCost)
			pnts.put(dualPoint.index.toLong(), dualPoint)
			return pnts
		}
		val savedMaxVisited = rctx.config.MAX_VISITED
		val savedPlanRoadDirectrion = rctx.config.planRoadDirection
		val savedHeuristicCoefficient = rctx.config.heuristicCoefficient
		rctx.config.MAX_VISITED = MAX_POINTS_CLUSTER_ROUTING
		rctx.config.planRoadDirection = if (reverse) -1 else 1
		rctx.config.heuristicCoefficient = 0f // dijkstra
		rctx.unloadAllData() // needed for proper multidijsktra work
		// hctx.rctx.calculationProgress = new RouteCalculationProgress(); // reuse same progress
		val planner = BinaryRoutePlanner()
		val routePlannerFrontEnd = RoutePlannerFrontEnd()
		val frs = planner.searchRouteInternal(
			rctx, if (reverse) null else s, if (reverse) s else null, hctx.boundaries
		) as MultiFinalRouteSegment?
		rctx.config.heuristicCoefficient = savedHeuristicCoefficient
		rctx.config.planRoadDirection = savedPlanRoadDirectrion
		rctx.config.MAX_VISITED = savedMaxVisited
		if (HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
			LOG.info("  " + progressOf(rctx).getInfo(null))
		}
		if (frs != null) {
			val set = KTLongHashSet()
			for (o in frs.all) {
				// duplicates are possible as alternative routes
				val startSegment = if (reverse) o.getSegmentEnd().toInt() else o.getSegmentStart().toInt()
				val endSegment = if (reverse) o.getSegmentStart().toInt() else o.getSegmentEnd().toInt()
				val pntId = calculateRoutePointInternalId(o.getRoad().getId(), startSegment, endSegment)
				if (set.add(pntId)) {
					var pnt = hctx.pointsByGeo[pntId]
					if (pnt == null) {
						if (pnts.containsKey(PNT_SHORT_ROUTE_START_END.toLong())) {
							continue
						}
						pnt = NetworkDBPoint()
						pnt.index = PNT_SHORT_ROUTE_START_END
						pnt.roadId = o.getRoad().getId()
						pnt.start = o.getSegmentStart()
						pnt.end = o.getSegmentEnd()
						pnt.startX = o.getStartPointX()
						pnt.endX = o.getEndPointX()
						pnt.startY = o.getStartPointY()
						pnt.endY = o.getEndPointY()
						val x = if (reverse) hctx.startX else hctx.endX
						val y = if (reverse) hctx.startY else hctx.endY
						val road = routePlannerFrontEnd.calcPreciseRouteSegmentPoint(o.getRoad(), x, y)
						if (road != null) {
							o.distanceFromStart += planner.calculatePreciseStartTime(rctx, road.preciseX, road.preciseY, o)
						}
					} else {
						val obstacle = rctx.getRouter().defineRoutingObstacle(
							o.getRoad(), o.getSegmentStart().toInt(), o.getSegmentStart() > o.getSegmentEnd()
						)
						if (obstacle < 0) {
							continue
						}
						o.distanceFromStart += planner.calcRoutingSegmentTimeOnlyDist(rctx.getRouter(), o) / 2 + obstacle
					}
					if (pnt.rt(reverse).rtCost != 0.0) {
						throw IllegalStateException()
					}
					pnt.setDistanceToEnd(
						reverse, if (pnt.index == PNT_SHORT_ROUTE_START_END) 0.0 else hctx.distanceToEnd(reverse, pnt)
					)
					pnt.setDetailedParentRt(reverse, o)
					pnts.put(pnt.index.toLong(), pnt)
				}
			}
		}
		if (config.USE_GC_MORE_OFTEN) {
			rctx.unloadAllData()
			printGCInformation(true)
		}
		return pnts
	}

	internal fun runRoutingPointToPoint(hctx: HHRoutingContext, start: NetworkDBPoint?, end: NetworkDBPoint?): NetworkDBPoint? {
		if (start != null) {
			addPointToQueue(hctx, hctx.queue(false), false, start, null, 0.0, MINIMAL_COST)
		}
		if (end != null) {
			addPointToQueue(hctx, hctx.queue(true), true, end, null, 0.0, MINIMAL_COST)
		}
		return runRoutingWithInitQueue(hctx)
	}

	internal fun runRoutingPointsToPoints(
		hctx: HHRoutingContext, stPoints: KTLongObjectMap<NetworkDBPoint>, endPoints: KTLongObjectMap<NetworkDBPoint>
	): NetworkDBPoint? {
		stPoints.forEachValue { start ->
			if (!start.rtExclude) {
				val cost = start.rt(false).rtCost
				addPointToQueue(
					hctx, hctx.queue(false), false, start, null, start.rt(false).rtDistanceFromStart,
					if (cost <= 0) MINIMAL_COST else cost
				)
			}
		}
		endPoints.forEachValue { end ->
			if (!end.rtExclude) {
				val cost = end.rt(true).rtCost
				addPointToQueue(
					hctx, hctx.queue(true), true, end, null, end.rt(true).rtDistanceFromStart,
					if (cost <= 0) MINIMAL_COST else cost
				)
			}
		}
		return runRoutingWithInitQueue(hctx)
	}

	private fun runRoutingWithInitQueue(hctx: HHRoutingContext): NetworkDBPoint? {
		val config = hctx.requireConfig()
		var dirConfig = config.DIJKSTRA_DIRECTION
		// Alternatives are extracted from the two search trees, so the search must not stop at the first
		// meeting point: it keeps settling until the queue leaves the (1 + ALT_STRETCH) * opt horizon.
		// The point returned is still the cheapest meeting point, i.e. the optimal route is unchanged.
		val collectAlt = config.CALC_ALTERNATIVES
		var bestFinal: NetworkDBPoint? = null
		var optCost = 0.0
		var altBound = Double.MAX_VALUE
		val rctx = hctx.rctx
		val progress = rctx?.calculationProgress
		val straightStartEndCost = squareRootDist31(hctx.startX, hctx.startY, hctx.endX, hctx.endY) /
				hctx.requireContext().getRouter().getMaxSpeed()
		if (progress != null && progress.hhGetCalcCounter() > 0) {
			progress.hhIterationProgress(progress.hhGetCalcCounter().toDouble() / maxCountReiteration)
		}
		while (true) {
			val queue: KPriorityQueue<NetworkDBPointCost>
			if (HHRoutingContext.USE_GLOBAL_QUEUE) {
				queue = hctx.queue(false)
				if (queue.isEmpty()) {
					break
				}
			} else {
				val pos = hctx.queue(false)
				val rev = hctx.queue(true)
				if (config.DIJKSTRA_DIRECTION == 0f || (!rev.isEmpty() && !pos.isEmpty())) {
					if (rev.isEmpty() || pos.isEmpty()) {
						break
					}
					queue = if (pos.peek()!!.cost < rev.peek()!!.cost) pos else rev
				} else {
					queue = if (config.DIJKSTRA_DIRECTION > 0) pos else rev
					if (queue.isEmpty()) {
						break
					}
				}
			}
			if (progress != null && progress.isCancelled) {
				return null
			}
			var tm = nanoTime()
			val pointCost = queue.poll()!!
			val point = pointCost.point
			val rev = pointCost.rev
			hctx.stats.pollQueueTime += (nanoTime() - tm) / 1e6
			hctx.stats.visitedVertices++
			if (collectAlt && pointCost.cost > altBound) {
				break
			}
			if (collectAlt && point.rt(!rev).rtVisited) {
				if (hctx.stats.firstRouteVisitedVertices == 0) {
					hctx.stats.firstRouteVisitedVertices = hctx.stats.visitedVertices
				}
				val rcost = point.rt(true).rtDistanceFromStart + point.rt(false).rtDistanceFromStart
				if (bestFinal == null || rcost < optCost) {
					bestFinal = point
					optCost = rcost
					altBound = optCost * (1 + config.ALT_STRETCH)
				}
				// no early return: fall through to settle & expand, both trees must keep growing
			} else if (point.rt(!rev).rtVisited) {
				if (hctx.stats.firstRouteVisitedVertices == 0) {
					hctx.stats.firstRouteVisitedVertices = hctx.stats.visitedVertices
					if (dirConfig == 0f && config.HEURISTIC_COEFFICIENT != 0f) {
						// focus on 1 direction as it slightly faster
						dirConfig = if (rev) -1f else 1f
					}
				}
				if (config.HEURISTIC_COEFFICIENT == 0f && config.DIJKSTRA_DIRECTION == 0f) {
					// Valid only HC=0, Dijkstra as we run Many-to-Many - Test( Lat 49.12691 Lon 9.213685 -> Lat 49.155483 Lon 9.2140045)
					var finalPoint = point
					finalPoint = scanFinalPoint(finalPoint, hctx.visited)
					finalPoint = scanFinalPoint(finalPoint, hctx.visitedRev)
					return finalPoint
				} else {
					val rcost = point.rt(true).rtDistanceFromStart + point.rt(false).rtDistanceFromStart
					if (rcost <= pointCost.cost) {
						// Universal condition to stop: works for any algorithm - cost equals to route length
						return point
					} else {
						queue.add(NetworkDBPointCost(point, rcost, rev))
						point.markVisited(rev)
						continue
					}
				}
			}
			if (point.rt(rev).rtVisited) {
				continue
			}
			hctx.stats.uniqueVisitedVertices++
			point.markVisited(rev)
			hctx.visited.add(point)
			(if (rev) hctx.visited else hctx.visitedRev).add(point)
			printPoint(point, rev)
			if (progress != null && straightStartEndCost > 0 && progress.hhGetCalcCounter() == 0) {
				val straightToRouteCost = 1.25 // approximate, tested on car/bike
				// correlation between straight-cost and route-cost (enough for the progress bar)
				val k = (pointCost.cost - straightStartEndCost) / straightStartEndCost * straightToRouteCost
				progress.hhIterationProgress(k) // ROUTING
			}
			if (config.MAX_COST > 0 && pointCost.cost > config.MAX_COST) {
				break
			}
			if (config.MAX_SETTLE_POINTS > 0 && (if (rev) hctx.visitedRev else hctx.visited).size > config.MAX_SETTLE_POINTS) {
				break
			}

			val directionAllowed = (dirConfig <= 0 && rev) || (dirConfig >= 0 && !rev)
			if (directionAllowed) {
				addConnectedToQueue(hctx, queue, point, rev)
			}
		}
		return if (collectAlt) bestFinal else null
	}

	private fun scanFinalPoint(finalPointArg: NetworkDBPoint, lt: List<NetworkDBPoint>): NetworkDBPoint {
		var finalPoint = finalPointArg
		for (p in lt) {
			if (p.rt(true).rtDistanceFromStart == 0.0 || p.rt(false).rtDistanceFromStart == 0.0) {
				continue
			}
			if (p.rt(true).rtDistanceFromStart + p.rt(false).rtDistanceFromStart
				< finalPoint.rt(true).rtDistanceFromStart + finalPoint.rt(false).rtDistanceFromStart
			) {
				finalPoint = p
			}
		}
		return finalPoint
	}

	private fun addConnectedToQueue(hctx: HHRoutingContext, queue: KPriorityQueue<NetworkDBPointCost>, point: NetworkDBPoint, reverse: Boolean) {
		val config = hctx.requireConfig()
		val depth = if (config.USE_MIDPOINT || config.MAX_DEPTH > 0) point.rt(reverse).getDepth(reverse) else 0
		if (config.MAX_DEPTH > 0 && depth >= config.MAX_DEPTH) {
			return
		}
		val tm = nanoTime()
		val cnt = hctx.loadNetworkSegmentPoint(point, reverse)
		hctx.stats.loadEdgesCnt += cnt
		hctx.stats.loadEdgesTime += (nanoTime() - tm) / 1e6
		val connectedList = point.connected(reverse) ?: throw IllegalStateException("Edges of $point were not loaded")
		for (connected in connectedList) {
			val nextPoint = if (reverse) connected.start else connected.end
			if (!config.USE_CH && !config.USE_CH_SHORTCUTS && connected.shortcut) {
				continue
			}
			if (nextPoint.rtExclude) {
				continue
			}
			// modify CH to not compute all top points
			if (config.USE_CH && (nextPoint.chInd() > 0 && nextPoint.chInd() < point.chInd())) {
				continue
			}
			if (config.USE_MIDPOINT && min(depth, config.MIDPOINT_MAX_DEPTH) > nextPoint.midPntDepth() + config.MIDPOINT_ERROR) {
				continue
			}
			if (connected.dist < 0) {
				// disabled segment
				continue
			}
			if (ASSERT_AND_CORRECT_DIST_SMALLER && config.HEURISTIC_COEFFICIENT > 0
				&& smallestSegmentCost(hctx, point, nextPoint) - connected.dist > 1
			) {
				if (touchesStartOrEnd(point, nextPoint, reverse)) {
					incorrectCostAtStartEnd = true
				}
				val smallestSegmentCost = smallestSegmentCost(hctx, point, nextPoint)
				LOG.warn("Incorrect distance %s -> %s: db = %.2f > fastest %.2f".format(point, nextPoint, connected.dist, smallestSegmentCost))
				connected.dist = smallestSegmentCost
			}
			val cost = point.rt(reverse).rtDistanceFromStart + connected.dist + hctx.distanceToEnd(reverse, nextPoint)
			if (ASSERT_COST_INCREASING && point.rt(reverse).rtCost - cost > 1) {
				val msg = "%s (cost %.2f) -> %s (cost %.2f) st=%.2f-> + %.2f, toend=%.2f->%.2f: ".format(
					point, point.rt(reverse).rtCost, nextPoint, cost, point.rt(reverse).rtDistanceFromStart,
					connected.dist, point.rt(reverse).rtDistanceToEnd, hctx.distanceToEnd(reverse, nextPoint)
				)
				throw IllegalStateException(msg)
			}
			val exCost = nextPoint.rt(reverse).rtCost
			if ((exCost == 0.0 && !nextPoint.rt(reverse).rtVisited) || cost < exCost) {
				addPointToQueue(hctx, queue, reverse, nextPoint, point, connected.dist, cost)
			}
		}
	}

	private fun touchesStartOrEnd(point: NetworkDBPoint, nextPoint: NetworkDBPoint, reverse: Boolean): Boolean {
		return if (reverse) {
			(point.rtRev != null && point.rtRev!!.rtRouteToPoint == null)
					|| (nextPoint.rtPos != null && nextPoint.rtPos!!.rtRouteToPoint == null)
		} else {
			(point.rtPos != null && point.rtPos!!.rtRouteToPoint == null)
					|| (nextPoint.rtRev != null && nextPoint.rtRev!!.rtRouteToPoint == null)
		}
	}

	private fun addPointToQueue(
		hctx: HHRoutingContext, queue: KPriorityQueue<NetworkDBPointCost>,
		reverse: Boolean, point: NetworkDBPoint, parent: NetworkDBPoint?, segmentDist: Double, cost: Double
	) {
		val tm = nanoTime()
		if (DEBUG_VERBOSE_LEVEL > 2) {
			LOG.info(
				"Add  %s to visit - cost %.2f (%.2f prev, %.2f dist) > prev cost %.2f".format(
					point, cost, parent?.rt(reverse)?.rtDistanceFromStart ?: 0.0, segmentDist, point.rt(reverse).rtCost
				)
			)
		}
		if (point.rt(reverse).rtVisited) {
			throw IllegalStateException("%s visited - cost %.2f > prev cost %.2f".format(point, cost, point.rt(reverse).rtCost))
		}
		point.setCostParentRt(reverse, cost, parent, segmentDist)
		hctx.queueAdded.add(point)
		queue.add(NetworkDBPointCost(point, cost, reverse)) // we need to add new object to not  remove / rebalance priority queue
		hctx.stats.addQueueTime += (nanoTime() - tm) / 1e6
		hctx.stats.addedVertices++
	}

	private fun smallestSegmentCost(hctx: HHRoutingContext, st: NetworkDBPoint, end: NetworkDBPoint): Double {
		val dist = squareRootDist31(st.midX(), st.midY(), end.midX(), end.midY())
		return dist / hctx.requireContext().getRouter().getMaxSpeed()
	}

	private fun printPoint(p: NetworkDBPoint, rev: Boolean) {
		if (DEBUG_VERBOSE_LEVEL > 1) {
			var pind = 0
			var pchInd = 0
			val to = p.rt(rev).rtRouteToPoint
			if (to != null) {
				pind = to.index
				pchInd = to.chInd()
			}
			val symbol = "%s %d [%d] (from %d [%d])".format(if (rev) "<-" else "->", p.index, p.chInd(), pind, pchInd)
			LOG.info(
				"Visit Point %s (cost %.1f s) %.5f/%.5f - %d".format(
					symbol, p.rt(rev).rtCost,
					KMapUtils.get31LatitudeY(p.startY), KMapUtils.get31LongitudeX(p.startX), p.roadId / 64
				)
			)
		}
	}

	private fun runDetailedRouting(hctx: HHRoutingContext, startS: NetworkDBPoint, endS: NetworkDBPoint, useBoundaries: Boolean): FinalRouteSegment? {
		val rctx = hctx.requireContext()
		val planner = BinaryRoutePlanner()
		rctx.config.planRoadDirection = 0 // A* bidirectional
		rctx.config.heuristicCoefficient = 1f
		// SPEEDUP: Speed up by just clearing visited
		rctx.unloadAllData() // needed for proper multidijsktra work
		// if (c.USE_GC_MORE_OFTEN) {
		// printGCInformation();
		// }
		val start = loadPoint(rctx, startS)
		val end = loadPoint(rctx, endS)
		if (start == null) {
			return null // no logging it's same as end of previos segment
		} else if (end == null) {
			LOG.info("End point is not present in detailed maps: $endS")
			return null
		}
		val oldP = rctx.config.penaltyForReverseDirection
		rctx.config.penaltyForReverseDirection *= 4.0 // probably we should try -1 (to fully avoid roundabout) but we don't have use cases yet
		rctx.config.initialDirection = start.getRoad().directionRoute(start.getSegmentStart().toInt(), start.isPositive())
		rctx.config.targetDirection = end.getRoad().directionRoute(end.getSegmentEnd().toInt(), !end.isPositive())
		rctx.config.MAX_VISITED = if (useBoundaries) -1 else MAX_POINTS_CLUSTER_ROUTING * 2
		// boundaries help to reduce max visited (helpful for long ferries)
		var bounds: ExcludeKTLongObjectMap<RouteSegment>? = null
		if (useBoundaries) {
			val ps = calcRPId(start, start.getSegmentEnd().toInt(), start.getSegmentStart().toInt())
			val pe = calcRPId(end, end.getSegmentStart().toInt(), end.getSegmentEnd().toInt())
			bounds = ExcludeKTLongObjectMap(hctx.boundaries, ps, pe)
		}
		val f = planner.searchRouteInternal(rctx, start, end, bounds)
		if (f == null) {
			LOG.info("No route found between $start -> $end")
		}
		rctx.config.MAX_VISITED = -1
		// clean up
		rctx.config.initialDirection = null
		rctx.config.targetDirection = null
		rctx.config.penaltyForReverseDirection = oldP
		return f
	}

	internal fun retrieveSegmentsGeometry(
		hctx: HHRoutingContext, route: HHNetworkRouteRes, routeSegments: Boolean, progress: RouteCalculationProgress?
	): Boolean {
		return retrieveSegmentsGeometry(hctx, route, routeSegments, progress, false)
	}

	/**
	 * @param acceptCostIncrease keep the detailed geometry when a shortcut turns out to cost more
	 * than the hub graph promised, instead of aborting for a recalculation. The main route needs the
	 * recalculation to stay optimal; an alternative only needs its true cost, which the caller then
	 * re-checks against the stretch limit.
	 */
	internal fun retrieveSegmentsGeometry(
		hctx: HHRoutingContext, route: HHNetworkRouteRes, routeSegments: Boolean,
		progress: RouteCalculationProgress?, acceptCostIncrease: Boolean
	): Boolean {
		val rctx = hctx.requireContext()
		val config = hctx.requireConfig()
		if (progress != null && progress.hhGetCalcCounter() > 0) {
			progress.hhIterationProgress(progress.hhGetCalcCounter().toDouble() / maxCountReiteration)
		}
		var costIncreased = false
		for (i in route.segments.indices) {
			if (progress != null && progress.hhGetCalcCounter() == 0) {
				progress.hhIterationProgress(i.toDouble() / route.segments.size) // DETAILED
			}

			val s = route.segments[i]
			val segment = s.segment
			if (segment == null) {
				// start / end points
				if (i > 0 && i < route.segments.size - 1) {
					throw IllegalStateException("Segment ind $i is null.")
				}
				continue
			}

			if (routeSegments) {
				if (progress != null && progress.isCancelled) {
					return false
				}
				val f = runDetailedRouting(hctx, segment.start, segment.end, true)
				if (f == null) {
					val full = config.FULL_DIJKSTRA_NETWORK_RECALC-- > 0
					LOG.info("Route not found (${if (full) "dijkstra+" else ""}recalc) ${segment.start} -> ${segment.end}")
					if (full) {
						recalculateNetworkCluster(hctx, segment.start)
					}
					segment.dist = -1.0
					return true
				}
				val maxIncCostCoefficient = if (incorrectCostAtStartEnd) config.MAX_INC_COST_CF_VIGILANT else config.MAX_INC_COST_CF
				if ((f.distanceFromStart + MAX_INC_COST_CORR) >
					(segment.dist + MAX_INC_COST_CORR) * maxIncCostCoefficient
				) {
					if (DEBUG_VERBOSE_LEVEL > 0) {
						LOG.info(
							"Route cost increased (%.2f > %.2f) between %s -> %s: %s".format(
								f.distanceFromStart, segment.dist, segment.start, segment.end,
								if (acceptCostIncrease) "keep detailed geometry" else "recalculate route"
							)
						)
					}
					segment.dist = f.distanceFromStart.toDouble()
					if (!acceptCostIncrease) {
						// correct every underestimated shortcut of this route before recalculating it
						costIncreased = true
						continue
					}
					s.rtTimeHHSegments = f.distanceFromStart.toDouble()
				}
				s.rtTimeDetailed = f.distanceFromStart.toDouble()
				s.list = RouteResultPreparation.convertFinalSegmentToResults(rctx, f)
			} else {
				// load segment geometry from db
				if (!hctx.loadGeometry(segment, false)) {
					segment.getGeometry().clear()
					segment.getGeometry().add(segment.start.getPoint())
					segment.getGeometry().add(segment.end.getPoint())
				}
			}
		}
		return costIncreased
	}

	private fun recalculateNetworkCluster(hctx: HHRoutingContext, start: NetworkDBPoint) {
		val rctx = hctx.requireContext()
		val plan = BinaryRoutePlanner()
		rctx.config.planRoadDirection = 1
		rctx.config.heuristicCoefficient = 0f
		// SPEEDUP: Speed up by just clearing visited
		rctx.unloadAllData() // needed for proper multidijsktra work
		val s = loadPoint(rctx, start)
		if (s == null) {
			LOG.error("HH recalculateNetworkCluster: loadPoint() is NULL for $start")
			return
		}
		// hctx.rctx.calculationProgress = new RouteCalculationProgress(); // we should reuse same progress for cancellation
		rctx.config.MAX_VISITED = MAX_POINTS_CLUSTER_ROUTING * 2
		val ps = calcRPId(s, s.getSegmentStart().toInt(), s.getSegmentEnd().toInt())
		val ps2 = calcRPId(s, s.getSegmentEnd().toInt(), s.getSegmentStart().toInt())
		val bounds = ExcludeKTLongObjectMap<RouteSegment>(hctx.boundaries, ps, ps2)
		val frs = plan.searchRouteInternal(rctx, s, null, bounds) as MultiFinalRouteSegment?
		rctx.config.MAX_VISITED = -1
		val resUnique = KTLongObjectMap<RouteSegment>()
		if (frs != null) {
			for (o in frs.all) {
				val pntId = calculateRoutePointInternalId(o.getRoad().getId(), o.getSegmentStart().toInt(), o.getSegmentEnd().toInt())
				val existing = resUnique[pntId]
				if (existing != null) {
					if (existing.getDistanceFromStart() > o.getDistanceFromStart()) {
						LOG.error("$existing > $o - $s")
					}
				} else {
					resUnique.put(pntId, o)
					val p = hctx.pointsByGeo[calcRPId(o, o.getSegmentStart().toInt(), o.getSegmentEnd().toInt())]
					if (p == null) {
						LOG.error("Error calculations new final boundary not found")
						continue
					}
					p.startX = o.getStartPointX()
					p.startY = o.getStartPointY()
					p.endX = o.getEndPointX()
					p.endY = o.getEndPointY()
					val routeTime = (o.getDistanceFromStart()
							+ plan.calcRoutingSegmentTimeOnlyDist(rctx.getRouter(), o) / 2 + 1).toDouble()
					val c = start.getSegment(p, true)
					if (c != null) {
						// System.out.printf("Corrected dist %.2f -> %.2f\n", c.dist, routeTime);
						c.dist = routeTime
					} else {
						start.connected!!.add(NetworkDBSegment(start, p, routeTime, true, false))
					}
					val co = p.getSegment(start, false)
					if (co != null) {
						co.dist = routeTime
					} else {
						p.connectedReverse?.add(NetworkDBSegment(start, p, routeTime, false, false))
					}
				}
			}
		}
		for (c in start.connected!!) {
			if (!resUnique.containsKey(c.end.getGeoPntId())) {
//				System.out.printf("Remove connection %s -> %s\n", start, c.end); // to debug later if all correct
				c.dist = -1.0 // disable as not found
				val co = c.end.getSegment(start, false)
				if (co != null) {
					co.dist = -1.0
				}
			}
		}
	}

	internal fun prepareRouteResults(hctx: HHRoutingContext, route: HHNetworkRouteRes, start: KLatLon, end: KLatLon): HHNetworkRouteRes {
		val rctx = hctx.requireContext()
		rctx.routingTime = 0f
		route.stats = hctx.stats
		var straightLine: RouteSegmentResult? = null
		for (routeSegmentInd in route.segments.indices) {
			val routeSegment = route.segments[routeSegmentInd]
			val s = routeSegment.segment
			val list = routeSegment.list
			if (list != null && list.size > 0) {
				if (straightLine != null) {
					route.detailed.add(straightLine)
					straightLine = null
				}
				if (routeSegmentInd > 0) {
					val p = list[0]
					if (abs(p.getStartPointIndex() - p.getEndPointIndex()) <= 1) {
						list.removeAt(0)
					} else {
						p.setStartPointIndex(p.getStartPointIndex() + (if (p.isForwardDirection()) +1 else -1))
					}
				}
				route.detailed.addAll(list)
			} else if (s != null) {
				val reg = RouteRegion()
				reg.initRouteEncodingRule(0, "highway", TurnPreparation.UNMATCHED_HIGHWAY_TYPE)
				val rdo = RouteDataObject(reg)
				rdo.types = intArrayOf(0)
				rdo.pointsX = intArrayOf(s.start.startX, s.end.startX)
				rdo.pointsY = intArrayOf(s.start.startY, s.end.startY)
				val sh = RouteDataObject(reg)
				sh.types = intArrayOf(0)
				sh.pointsX = intArrayOf(s.end.startX, s.end.endX)
				sh.pointsY = intArrayOf(s.end.startY, s.end.endY)
				straightLine = RouteSegmentResult(sh, 0, 1)
				route.detailed.add(RouteSegmentResult(rdo, 0, 1))
			}
			rctx.routingTime += routeSegment.rtTimeDetailed.toFloat()

			if (DEBUG_VERBOSE_LEVEL >= 1) {
				val segments = list?.size ?: 0
				if (s == null) {
					printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
						"First / last segment - %d segments, %.2fs".format(segments, routeSegment.rtTimeDetailed)
					}
				} else {
					printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
						"Route %d [%d] -> %d [%d] %s - hh dist %.2f s, detail %.2f s (%.1f%%) segments %d ( end %.5f/%.5f - %d ) ".format(
							s.start.index, s.start.chInd(), s.end.index, s.end.chInd(), if (s.shortcut) "sh" else "bs",
							s.dist, routeSegment.rtTimeDetailed, 100 * (1 - routeSegment.rtTimeDetailed / s.dist),
							segments, KMapUtils.get31LatitudeY(s.end.startY), KMapUtils.get31LongitudeX(s.end.startX),
							s.end.roadId / 64
						)
					}
				}
			}
		}
		return route
	}

	internal fun createRouteSegmentFromFinalPoint(hctx: HHRoutingContext, pnt: NetworkDBPoint?): HHNetworkRouteRes {
		val rctx = hctx.requireContext()
		val route = HHNetworkRouteRes()
		if (pnt != null) {
			var itPnt: NetworkDBPoint = pnt
			while (itPnt.rt(true).rtRouteToPoint != null) {
				val nextPnt = itPnt.rt(true).rtRouteToPoint!!
				val segment = nextPnt.getSegment(itPnt, false)!!
				val res = HHNetworkSegmentRes(segment)
				route.segments.add(res)
				res.rtTimeHHSegments = segment.dist
				res.rtTimeDetailed = segment.dist
				itPnt = nextPnt
			}
			var detailedRoute = itPnt.rt(true).rtDetailedRoute
			if (detailedRoute != null) {
				val res = HHNetworkSegmentRes(null)
				res.list = RouteResultPreparation.convertFinalSegmentToResults(rctx, detailedRoute)
				res.rtTimeHHSegments = detailedRoute.distanceFromStart.toDouble()
				res.rtTimeDetailed = res.rtTimeHHSegments
				route.segments.add(res)
			}
			route.segments.reverse()
			itPnt = pnt
			while (itPnt.rt(false).rtRouteToPoint != null) {
				val nextPnt = itPnt.rt(false).rtRouteToPoint!!
				val segment = nextPnt.getSegment(itPnt, true)!!
				val res = HHNetworkSegmentRes(segment)
				route.segments.add(res)
				res.rtTimeHHSegments = segment.dist
				res.rtTimeDetailed = segment.dist
				itPnt = nextPnt
			}
			detailedRoute = itPnt.rt(false).rtDetailedRoute
			if (detailedRoute != null) {
				val res = HHNetworkSegmentRes(null)
				res.list = RouteResultPreparation.convertFinalSegmentToResults(rctx, detailedRoute)
				res.rtTimeHHSegments = detailedRoute.distanceFromStart.toDouble()
				res.rtTimeDetailed = res.rtTimeHHSegments
				route.segments.add(res)
			}
			route.segments.reverse()
		}
		return route
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("HHRoutePlanner")

		@JvmField
		var DEBUG_VERBOSE_LEVEL = 0

		const val MINIMAL_COST = 0.01
		private const val PNT_SHORT_ROUTE_START_END = -1000
		const val MAX_POINTS_CLUSTER_ROUTING = 150000

		// if point is present without map with HH routing it will iterate each time with MAX_POINTS_CLUSTER_ROUTING
		const val MAX_INC_COST_CORR = 10.0

		// this constant should dynamically change if route is not found
		const val EXCLUDE_PRIORITY_CONSTANT = 0.0 // see comments below

		private const val ASSERT_COST_INCREASING = false
		private const val ASSERT_AND_CORRECT_DIST_SMALLER = true

		// select specifically high cost params
		// for example prefer_unpaved has higher cost > avoid_toll param, so it's better to select profile with prefer_unpaved shortcuts
		private val HIGH_COST_PARAMS = setOf("prefer_unpaved", "avoid_motorway", "driving_style_prefer_unpaved")

		private val IGNORE_FAILED_UNSUPPORTED_PARAMETERS = setOf("allow_private")

		@JvmStatic
		fun create(ctx: RoutingContext): HHRoutePlanner = HHRoutePlanner(ctx)

		@JvmStatic
		fun squareRootDist31(x1: Int, y1: Int, x2: Int, y2: Int): Double {
//		return MapUtils.measuredDist31(x1, y1, x2, y2);
			return KMapUtils.squareRootDist31(x1, y1, x2, y2)
		}

		@JvmStatic
		fun prepareDefaultRoutingConfig(cArg: HHRoutingConfig?): HHRoutingConfig {
			var c = cArg
			if (c == null) {
				// test data for debug swap
//			c = HHRoutingConfig.dijkstra(0);
				c = HHRoutingConfig.astar(0)
//			c = HHRoutingConfig.ch();
//			c.preloadSegments();
				c.ROUTE_LAST_MILE = true
				c.calcDetailed(2)
//			c.calcAlternative();
//			c.gc();
				DEBUG_VERBOSE_LEVEL = 0
//			c.INITIAL_DIRECTION = 30 / 180.0 * Math.PI;
//			routingProfile = (routingProfile + 1) % networkDB.getRoutingProfiles().size();
//			HHRoutingContext.USE_GLOBAL_QUEUE = true;
			}
			c.applyCalculateMissingMaps(RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS)
			return c
		}

		@JvmStatic
		fun cancelledStatus(
			hctx: HHRoutingContext, stPoints: KTLongObjectMap<NetworkDBPoint>?, endPoints: KTLongObjectMap<NetworkDBPoint>?
		): HHNetworkRouteRes {
			progressOf(hctx.requireContext()).raiseFastRoutingStatus(FastRoutingState.Status.CANCELLED)
			hctx.clearAll(stPoints, endPoints)
			return HHNetworkRouteRes("Routing was cancelled.")
		}

		/** The parameters that select an HH parameter set, by key: java keeps them in a `TreeMap`. */
		@JvmStatic
		fun getFilteredTags(generalRouter: GeneralRouter): MutableMap<String, String> {
			val parameters = generalRouter.getParameters()
			val tm = LinkedHashMap<String, String>()
			for (es in generalRouter.getParameterValues().entries.sortedBy { it.key }) {
				val paramId = es.key
				// These parameters don't affect the routing filters
				// This assumption probably shouldn't be used in general and only for popular parameters)
				if (parameters.containsKey(paramId) && paramId != GeneralRouter.USE_SHORTEST_WAY
					&& paramId != GeneralRouter.USE_HEIGHT_OBSTACLES
					&& !paramId.startsWith(GeneralRouter.GROUP_RELIEF_SMOOTHNESS_FACTOR)
				) {
					tm[paramId] = es.value
				}
			}
			return tm
		}

		@JvmStatic
		fun loadPoint(ctx: RoutingContext, pnt: NetworkDBPoint): RouteSegmentPoint? {
			var s = ctx.loadRouteSegment(pnt.startX, pnt.startY, ctx.config.memoryLimitation)
			while (s != null) {
				if (s.getRoad().getId() == pnt.roadId && s.getSegmentStart() == pnt.start) {
					if (s.getSegmentEnd() != pnt.end) {
						s = s.initRouteSegment(!s.isPositive())
					}
					break
				}
				s = s.getNext()
			}
			if (s == null || s.getSegmentStart() != pnt.start || s.getSegmentEnd() != pnt.end || s.getRoad().getId() != pnt.roadId) {
//			throw new IllegalStateException("Error on segment " + pnt.roadId / 64);
				return null
			}
			return RouteSegmentPoint(s.getRoad(), s.getSegmentStart().toInt(), s.getSegmentEnd().toInt(), 0.0)
		}

		/** Java calls `System.gc()` and prints the heap here; Kotlin/Native has no such call, so only the log line stays. */
		@JvmStatic
		fun printGCInformation(gc: Boolean) {
			if (DEBUG_VERBOSE_LEVEL > 0) {
				LOG.info("***** Memory used: not measured *****")
			}
		}

		private fun progressOf(rctx: RoutingContext): RouteCalculationProgress {
			var progress = rctx.calculationProgress
			if (progress == null) {
				progress = RouteCalculationProgress()
				rctx.calculationProgress = progress
			}
			return progress
		}
	}
}
