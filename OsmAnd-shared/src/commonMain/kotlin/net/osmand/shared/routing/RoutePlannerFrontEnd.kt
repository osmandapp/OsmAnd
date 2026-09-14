package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.experimental.ExperimentalObjCName
import kotlin.jvm.JvmField
import kotlin.native.ObjCName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

/**
 * The entry point of a route calculation: finds the road each requested point lies on, runs the
 * search between every pair of them, and prepares what it found.
 *
 * For a car over a long distance it first routes on the base map, and then lets that route guide
 * the detailed search as a [PrecalculatedRouteDirection]; a previous route can be reused past the
 * point the driver left it.
 *
 * A copy of `net.osmand.router.RoutePlannerFrontEnd`, which stays in OsmAnd-java for android and
 * tools; this copy is for iOS. It carries the search over the road graph, the HH search, the gpx
 * approximation and the preparation. Not copied: the missing maps check (`MissingMapsCalculator`
 * needs the world regions), and the native path, since on iOS this planner is what replaces the
 * C++ one; so there is no `useNativeApproximation` here either.
 */
class RoutePlannerFrontEnd {

	private var useSmartRouteRecalculation = true
	private var useOnlyHHRouting = false
	private var hhRoutingConfig: HHRoutingConfig? = null
	private var useGeometryBasedApproximation = false

	@JvmOverloads
	fun buildRoutingContext(config: RoutingConfiguration, map: List<BinaryMapIndexReader>, rm: RouteCalculationMode? = null): RoutingContext {
		val mode = rm ?: if (config.router.getProfile() == GeneralRouterProfile.CAR) RouteCalculationMode.COMPLEX else RouteCalculationMode.NORMAL
		return RoutingContext(config, map, mode)
	}

	@JvmOverloads
	fun findRouteSegment(
		lat: Double, lon: Double, ctx: RoutingContext, list: MutableList<RouteSegmentPoint>?,
		transportStop: Boolean = false, allowDuplications: Boolean = false
	): RouteSegmentPoint? {
		val now = nanoTime()
		val px = KMapUtils.get31TileNumberX(lon)
		val py = KMapUtils.get31TileNumberY(lat)
		val dataObjects = ArrayList<RouteDataObject>()
		ctx.loadTileData(px, py, 17, dataObjects, allowDuplications)
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 15, dataObjects, allowDuplications)
		}
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 14, dataObjects, allowDuplications)
		}
		val candidates = list ?: ArrayList()
		for (r in dataObjects) {
			if (r.getPointsLength() > 1) {
				val road = calcPreciseRouteSegmentPoint(r, px, py)
				if (road != null) {
					if (!transportStop) {
						val prio = ctx.getRouter().defineDestinationPriority(road.getRoad())
						if (prio > 0) {
							road.distToProj = (road.distToProj + GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR) / (prio * prio)
							candidates.add(road)
						}
					} else {
						candidates.add(road)
					}
				}
			}
		}
		candidates.sortWith { o1, o2 -> o1.distToProj.compareTo(o2.distToProj) }
		ctx.calculationProgress?.let { it.timeToFindInitialSegments += (nanoTime() - now) }
		if (candidates.size > 0) {
			var ps: RouteSegmentPoint? = null
			if (ctx.publicTransport) {
				for (p in candidates) {
					if (transportStop && p.distToProj > GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR) {
						break
					}
					val platform = p.getRoad().platform()
					if (transportStop && platform) {
						ps = p
						break
					}
					if (!transportStop && !platform) {
						ps = p
						break
					}
				}
			}
			if (ps == null) {
				ps = candidates[0]
			}
			candidates.remove(ps) // remove cyclic link to itself to avoid memory leaks (C++ backport)
			ps.others = candidates
			return ps
		}
		return null
	}

	fun calcPreciseRouteSegmentPoint(r: RouteDataObject, x: Int, y: Int): RouteSegmentPoint? {
		var road: RouteSegmentPoint? = null
		for (i in 1 until r.getPointsLength()) {
			val pr = KMapUtils.getProjectionPoint31(
				x, y, r.getPoint31XTile(i - 1),
				r.getPoint31YTile(i - 1), r.getPoint31XTile(i), r.getPoint31YTile(i)
			)
			val currentsDistSquare = squareDist(pr.x.toInt(), pr.y.toInt(), x, y)
			if (road == null || currentsDistSquare < road.distToProj) {
				val ro = RouteDataObject(r)
				road = RouteSegmentPoint(ro, i - 1, i, currentsDistSquare)
				road.preciseX = pr.x.toInt()
				road.preciseY = pr.y.toInt()
			}
		}
		return road
	}

	fun searchRoute(ctx: RoutingContext, start: KLatLon, end: KLatLon, intermediates: List<KLatLon>?): RouteCalcResult {
		return searchRoute(ctx, start, end, intermediates, null)
	}

	fun setUseFastRecalculation(use: Boolean): RoutePlannerFrontEnd {
		useSmartRouteRecalculation = use
		return this
	}

	fun setHHRoutingConfig(hhRoutingConfig: HHRoutingConfig?): RoutePlannerFrontEnd {
		// null means don't use hh
		this.hhRoutingConfig = hhRoutingConfig
		return this
	}

	fun disableHHRoutingConfig(): RoutePlannerFrontEnd {
		this.hhRoutingConfig = null
		return this
	}

	fun isHHRoutingConfigured(): Boolean = this.hhRoutingConfig != null

	fun getHHRoutingConfig(): HHRoutingConfig? = this.hhRoutingConfig

	fun setDefaultHHRoutingConfig() {
		this.hhRoutingConfig = defaultHHConfig()
	}

	fun setUseOnlyHHRouting(useOnlyHHRouting: Boolean): RoutePlannerFrontEnd {
		this.useOnlyHHRouting = useOnlyHHRouting
		if (useOnlyHHRouting && hhRoutingConfig == null) {
			this.hhRoutingConfig = defaultHHConfig()
		}
		return this
	}

	/** Whether [searchGpxRoute] walks the road graph along the track instead of running the A* search between track points. */
	fun setUseGeometryBasedApproximation(enabled: Boolean): RoutePlannerFrontEnd {
		this.useGeometryBasedApproximation = enabled
		return this
	}

	fun isUseGeometryBasedApproximation(): Boolean = useGeometryBasedApproximation

	/**
	 * Attaches a track to roads: the [gpxPoints] of [generateGpxPoints], approximated over the map
	 * of [gctx]'s context. The result is [gctx] itself, with its final points and the whole route;
	 * [resultMatcher], when given, is handed it too, or null when the calculation was cancelled.
	 * With [useExternalTimestamps] the speed of every road is taken from the track's timestamps.
	 */
	fun searchGpxRoute(
		gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>, resultMatcher: ResultMatcher<GpxRouteApproximation?>?,
		useExternalTimestamps: Boolean
	): GpxRouteApproximation {
		return gctx.searchGpxRouteInternal(this, gpxPoints, resultMatcher, useExternalTimestamps)
	}

	/**
	 * The track points for [searchGpxRoute]: each with its distance along the track, the time it
	 * was recorded at when [times] is given (milliseconds, one per location, for the external
	 * timestamps, null for none), and the whole track as one straight-line road. Java takes a
	 * `LocationsHolder` of points, locations or waypoints; this takes the coordinates and times
	 * themselves, or the waypoints of the shared gpx model.
	 */
	fun generateGpxPoints(gctx: GpxRouteApproximation, locations: List<KLatLon>, times: LongArray?): MutableList<GpxPoint> {
		val gpxPoints = ArrayList<GpxPoint>(locations.size)
		var prev: GpxPoint? = null
		val o = generateStraightLineSegment(0f, locations).getObject()
		for (i in locations.indices) {
			val p = GpxPoint()
			p.ind = i
			p.time = times?.get(i) ?: 0
			p.loc = locations[i]
			p.track = o
			if (prev != null) {
				p.cumDist = KMapUtils.getDistance(p.loc, prev.loc) + prev.cumDist
			}
			gpxPoints.add(p)
			gctx.routeDistance = p.cumDist.toInt()
			prev = p
		}
		return gpxPoints
	}

	fun generateGpxPoints(gctx: GpxRouteApproximation, points: List<WptPt>): MutableList<GpxPoint> {
		val locations = ArrayList<KLatLon>(points.size)
		val times = LongArray(points.size)
		for (i in points.indices) {
			locations.add(KLatLon(points[i].lat, points[i].lon))
			times[i] = points[i].time
		}
		return generateGpxPoints(gctx, locations, times)
	}

	private fun needRequestPrivateAccessRouting(ctx: RoutingContext, points: List<KLatLon>): Boolean {
		var res = false
		val router = ctx.config.router
		val parameters = router.getParameters()
		var allowPrivateKey: String? = null
		if (parameters.containsKey(GeneralRouter.ALLOW_PRIVATE)) {
			allowPrivateKey = GeneralRouter.ALLOW_PRIVATE
		} else if (parameters.containsKey(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK)) {
			allowPrivateKey = GeneralRouter.ALLOW_PRIVATE
		}
		if (!router.isAllowPrivate() && allowPrivateKey != null) {
			ctx.unloadAllData()
			val mp = LinkedHashMap<String, String>()
			mp[allowPrivateKey] = "true"
			mp[GeneralRouter.CHECK_ALLOW_PRIVATE_NEEDED] = "true"
			ctx.setRouter(GeneralRouter(router.getProfile(), mp))
			for (latLon in points) {
				val rp = findRouteSegment(latLon.latitude, latLon.longitude, ctx, null)
				if (rp != null && rp.road != null) {
					if (rp.getRoad().hasPrivateAccess(ctx.config.router.getProfile())) {
						res = true
						break
					}
				}
			}
			ctx.unloadAllData()
			ctx.setRouter(router)
		}
		return res
	}

	fun searchRoute(
		ctx: RoutingContext, start: KLatLon, end: KLatLon, intermediates: List<KLatLon>?,
		routeDirectionArg: PrecalculatedRouteDirection?
	): RouteCalcResult {
		var routeDirection = routeDirectionArg
		val timeToCalculate = nanoTime()
		var progress = ctx.calculationProgress
		if (progress == null) {
			progress = RouteCalculationProgress()
			ctx.calculationProgress = progress
		}
		val intermediatesEmpty = intermediates == null || intermediates.isEmpty()
		val targets = ArrayList<KLatLon>()
		if (!intermediatesEmpty) {
			targets.addAll(intermediates!!)
		}
		targets.add(end)
		if (needRequestPrivateAccessRouting(ctx, targets)) {
			progress.requestPrivateAccessRouting = true
		}
		if (hhRoutingConfig != null && ctx.calculationMode != RouteCalculationMode.BASE) {
			// java fills ctx.regionsCoveringStartAndTargets from OsmandRegions here; the request carries it
			val r = runHHRoute(ctx, start, targets)
			val hasAnyMissingMaps = progress.hasAnyMissingMaps()
			if ((r != null && r.isCorrect()) || hasAnyMissingMaps || useOnlyHHRouting) {
				return r ?: HHNetworkRouteRes("Error during routing calculation")
			}
		}

		var maxDistance = KMapUtils.getDistance(start, end)
		if (!intermediatesEmpty) {
			var b = start
			for (l in intermediates!!) {
				maxDistance = max(KMapUtils.getDistance(b, l), maxDistance)
				b = l
			}
		}
		if (ctx.calculationMode == RouteCalculationMode.COMPLEX && routeDirection == null
			&& maxDistance > RoutingConfiguration.DEVIATION_RADIUS * 6
		) {
			progress.totalIterations++
			val nctx = buildRoutingContext(ctx.config, ctx.getMaps(), RouteCalculationMode.BASE)
			nctx.calculationProgress = progress
			val baseRes = searchRoute(nctx, start, end, intermediates)
			if (!baseRes.isCorrect()) {
				return baseRes
			}
			routeDirection = PrecalculatedRouteDirection.build(baseRes.detailed, RoutingConfiguration.DEVIATION_RADIUS, ctx.getRouter().getMaxSpeed())
			ctx.calculationProgressFirstPhase = RouteCalculationProgress.capture(progress)
		}
		var indexNotFound = 0
		val points = ArrayList<RouteSegmentPoint>()
		if (!addSegment(start, ctx, indexNotFound++, points, ctx.startTransportStop)) {
			return RouteCalcResult("Start point is not located")
		}
		if (intermediates != null) {
			for (l in intermediates) {
				if (!addSegment(l, ctx, indexNotFound++, points, false)) {
					println(points[points.size - 1].getRoad().toString())
					return RouteCalcResult("Intermediate point is not located")
				}
			}
		}
		if (!addSegment(end, ctx, indexNotFound++, points, ctx.targetTransportStop)) {
			return RouteCalcResult("End point is not located")
		}
		progress.nextIteration()
		val res = searchRouteImpl(ctx, points, routeDirection)
		progress.timeToCalculate = (nanoTime() - timeToCalculate)
		return res
	}

	private fun runHHRoute(ctx: RoutingContext, start: KLatLon, targets: List<KLatLon>): HHNetworkRouteRes? {
		val routePlanner = HHRoutePlanner.create(ctx)
		val progress = ctx.calculationProgress
		var r: HHNetworkRouteRes? = null
		var dir = ctx.config.initialDirection
		for (i in targets.indices) {
			val initialPenalty = ctx.config.penaltyForReverseDirection
			if (i > 0) {
				ctx.config.penaltyForReverseDirection /= 2.0 // relax reverse-penalty (only for inter-points)
			}
			progress?.hhTargetsProgress(i, targets.size)
			val res = calculateHHRoute(routePlanner, ctx, if (i == 0) start else targets[i - 1], targets[i], dir)
			ctx.config.penaltyForReverseDirection = initialPenalty
			if (r == null) {
				r = res
			} else {
				r.append(res)
			}
			if (r == null || !r.isCorrect()) {
				break
			}
			if (r.detailed.size > 0) {
				dir = (r.detailed[r.detailed.size - 1].getBearingEnd() / 180.0) * PI
			}
		}
		ctx.unloadAllData() // clean indexedSubregions is required for BRP-fallback
		ctx.routingTime = r?.getHHRoutingDetailed()?.toFloat() ?: 0f
		return r
	}

	private fun calculateHHRoute(
		routePlanner: HHRoutePlanner, ctx: RoutingContext, start: KLatLon, end: KLatLon, dir: Double?
	): HHNetworkRouteRes? {
		try {
			val cfg = HHRoutePlanner.prepareDefaultRoutingConfig(hhRoutingConfig)
			cfg.INITIAL_DIRECTION = dir
			val res = routePlanner.runRouting(start, end, cfg)
			if (res.error == null) {
				ctx.calculationProgress?.hhIteration(RouteCalculationProgress.HHIteration.DONE)
				makeStartEndPointsPrecise(ctx, res, start, end)
				return res
			}
			ctx.calculationProgress?.hhIteration(RouteCalculationProgress.HHIteration.HH_NOT_STARTED)
		} catch (e: RouteCalculationInterruptedException) {
			throw e
		} catch (e: Exception) {
			log.error("HH routing failed: " + e.message, e)
			if (useOnlyHHRouting) {
				return HHNetworkRouteRes("Error during routing calculation : " + e.message)
			}
		}
		return null
	}

	fun makeStartEndPointsPrecise(ctx: RoutingContext, res: RouteCalcResult, start: KLatLon, end: KLatLon) {
		if (res.detailed.size > 0) {
			makeSegmentPointPrecise(ctx, res.detailed[0], start, true)
			makeSegmentPointPrecise(ctx, res.detailed[res.detailed.size - 1], end, false)
		}
	}

	fun projectDistance(res: List<RouteSegmentResult>, k: Int, px: Int, py: Int): Double {
		val sr = res[k]
		val r = sr.getObject()
		val pp = KMapUtils.getProjectionPoint31(
			px, py,
			r.getPoint31XTile(sr.getStartPointIndex()), r.getPoint31YTile(sr.getStartPointIndex()),
			r.getPoint31XTile(sr.getEndPointIndex()), r.getPoint31YTile(sr.getEndPointIndex())
		)
		return squareDist(pp.x.toInt(), pp.y.toInt(), px, py)
	}

	fun makeSegmentPointPrecise(ctx: RoutingContext, routeSegmentResult: RouteSegmentResult, point: KLatLon, st: Boolean) {
		val px = KMapUtils.get31TileNumberX(point.longitude)
		val py = KMapUtils.get31TileNumberY(point.latitude)
		val pind = if (st) routeSegmentResult.getStartPointIndex() else routeSegmentResult.getEndPointIndex()

		val r = RouteDataObject(routeSegmentResult.getObject())
		routeSegmentResult.setObject(r)
		var before: net.osmand.shared.data.KQuadPointDouble? = null
		var after: net.osmand.shared.data.KQuadPointDouble? = null
		if (pind > 0) {
			before = KMapUtils.getProjectionPoint31(
				px, py, r.getPoint31XTile(pind - 1),
				r.getPoint31YTile(pind - 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind)
			)
		}
		if (pind < r.getPointsLength() - 1) {
			after = KMapUtils.getProjectionPoint31(
				px, py, r.getPoint31XTile(pind + 1),
				r.getPoint31YTile(pind + 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind)
			)
		}
		var insert = 0
		val dd = KMapUtils.getDistance(
			point, KMapUtils.get31LatitudeY(r.getPoint31YTile(pind)),
			KMapUtils.get31LongitudeX(r.getPoint31XTile(pind))
		)
		var ddBefore = Double.POSITIVE_INFINITY
		var ddAfter: Double
		var i: net.osmand.shared.data.KQuadPointDouble? = null
		if (before != null) {
			ddBefore = KMapUtils.getDistance(
				point, KMapUtils.get31LatitudeY(before.y.toInt()),
				KMapUtils.get31LongitudeX(before.x.toInt())
			)
			if (ddBefore < dd) {
				insert = -1
				i = before
			}
		}

		if (after != null) {
			ddAfter = KMapUtils.getDistance(
				point, KMapUtils.get31LatitudeY(after.y.toInt()),
				KMapUtils.get31LongitudeX(after.x.toInt())
			)
			if (ddAfter < dd && ddAfter < ddBefore) {
				insert = 1
				i = after
			}
		}

		if (insert != 0) {
			if (st && routeSegmentResult.getStartPointIndex() < routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1)
			}
			if (!st && routeSegmentResult.getStartPointIndex() > routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1)
			}
			if (insert > 0) {
				r.insert(pind + 1, i!!.x.toInt(), i.y.toInt())
				if (st) {
					routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1)
				}
				if (!st) {
					routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1)
				}
			} else {
				r.insert(pind, i!!.x.toInt(), i.y.toInt())
			}
			// correct distance
			TurnPreparation.calculateTimeSpeed(ctx, routeSegmentResult)
		}
	}

	private fun addSegment(s: KLatLon, ctx: RoutingContext, indexNotFound: Int, res: MutableList<RouteSegmentPoint>, transportStop: Boolean): Boolean {
		val f = findRouteSegment(s.latitude, s.longitude, ctx, null, transportStop)
		if (f == null) {
			ctx.calculationProgress!!.segmentNotFound = indexNotFound
			return false
		} else {
			log.info("Route segment found " + f.road)
			res.add(f)
			return true
		}
	}

	internal fun searchRouteInternalPrepare(
		ctx: RoutingContext, start: RouteSegmentPoint, end: RouteSegmentPoint,
		routeDirection: PrecalculatedRouteDirection?
	): RouteCalcResult {
		val recalculationEnd = getRecalculationEnd(ctx)
		if (recalculationEnd != null) {
			ctx.initStartAndTargetPoints(start, recalculationEnd)
		} else {
			ctx.initStartAndTargetPoints(start, end)
		}
		if (routeDirection != null) {
			ctx.precalculatedRouteDirection = routeDirection.adopt(ctx)
		} else {
			ctx.precalculatedRouteDirection = null
		}
		refreshProgressDistance(ctx)
		// Split into 2 methods to let GC work in between
		ctx.finalRouteSegment = BinaryRoutePlanner().searchRouteInternal(ctx, start, recalculationEnd ?: end, null)
		// 4. Route is found : collect all segments and prepare result
		val result = RouteResultPreparation.convertFinalSegmentToResults(ctx, ctx.finalRouteSegment)
		addPrecalculatedToResult(recalculationEnd, result)
		return RouteResultPreparation.prepareResult(ctx, result)
	}

	fun getRecalculationEnd(ctx: RoutingContext): RouteSegmentPoint? {
		var recalculationEnd: RouteSegmentPoint? = null
		val previouslyCalculatedRoute = ctx.previouslyCalculatedRoute
		val runRecalculation = previouslyCalculatedRoute != null && previouslyCalculatedRoute.isNotEmpty()
				&& ctx.config.recalculateDistance != 0f
		if (runRecalculation) {
			val rlist = ArrayList<RouteSegmentResult>()
			val distanceThreshold = ctx.config.recalculateDistance
			var threshold = 0f
			for (rr in previouslyCalculatedRoute!!) {
				threshold += rr.getDistance()
				if (threshold > distanceThreshold) {
					rlist.add(rr)
				}
			}

			if (rlist.isNotEmpty()) {
				var previous: RouteSegment? = null
				for (i in rlist.indices) {
					val rr = rlist[i]
					if (previous != null) {
						val segment = RouteSegment(rr.getObject(), rr.getStartPointIndex(), rr.getEndPointIndex())
						previous.setParentRoute(segment)
						previous = segment
					} else {
						val end = RouteSegmentPoint(rr.getObject(), rr.getStartPointIndex(), 0.0)
						recalculationEnd = end
						if (abs(rr.getEndPointIndex() - rr.getStartPointIndex()) > 1) {
							val segment = RouteSegment(rr.getObject(), end.segEnd.toInt(), rr.getEndPointIndex())
							end.setParentRoute(segment)
							previous = segment
						} else {
							previous = end
						}
					}
				}
			}
		}
		return recalculationEnd
	}

	private fun refreshProgressDistance(ctx: RoutingContext) {
		val progress = ctx.calculationProgress ?: return
		progress.distanceFromBegin = 0f
		progress.distanceFromEnd = 0f
		progress.reverseSegmentQueueSize = 0
		progress.directSegmentQueueSize = 0
		val rd = KMapUtils.squareRootDist31(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY).toFloat()
		val speed = 0.9f * ctx.config.router.getMaxSpeed()
		progress.totalEstimatedDistance = rd / speed
	}

	private fun addPrecalculatedToResult(recalculationEnd: RouteSegment?, result: MutableList<RouteSegmentResult>) {
		if (recalculationEnd != null) {
			log.info("Native routing use precalculated route")
			var current: RouteSegment = recalculationEnd
			if (!hasSegment(result, current)) {
				if (TRACE_ROUTING) {
					log.info("Add recalculationEnd to result = " + current.getRoad() + " " + current.getSegmentStart() + "->" + current.getSegmentEnd())
				}
				result.add(RouteSegmentResult(current.getRoad(), current.getSegmentStart().toInt(), current.getSegmentEnd().toInt()))
			}
			while (current.getParentRoute() != null) {
				val pr = current.getParentRoute()!!
				result.add(RouteSegmentResult(pr.getRoad(), pr.getSegmentStart().toInt(), pr.getSegmentEnd().toInt()))
				if (TRACE_ROUTING) {
					log.info("Road = " + pr.getRoad() + " " + pr.getSegmentStart() + "->" + pr.getSegmentEnd())
				}
				current = pr
			}
		}
	}

	private fun hasSegment(result: List<RouteSegmentResult>, current: RouteSegment): Boolean {
		for (r in result) {
			val currentId = r.getObject().id
			if (currentId == current.getRoad().id && r.getStartPointIndex() == current.getSegmentStart().toInt()
				&& r.getEndPointIndex() == current.getSegmentEnd().toInt()
			) {
				return true
			}
		}
		return false
	}

	private fun searchRouteImpl(ctx: RoutingContext, points: List<RouteSegmentPoint>, routeDirection: PrecalculatedRouteDirection?): RouteCalcResult {
		if (points.size <= 2) {
			// simple case 2 points only
			if (!useSmartRouteRecalculation) {
				ctx.previouslyCalculatedRoute = null
			}
			val res = searchRouteInternalPrepare(ctx, points[0], points[1], routeDirection)
			makeStartEndPointsPrecise(ctx, res, points[0].getPreciseLatLon(), points[1].getPreciseLatLon())
			return res
		}

		var firstPartRecalculatedRoute: ArrayList<RouteSegmentResult>? = null
		var restPartRecalculatedRoute: ArrayList<RouteSegmentResult>? = null
		val prev = ctx.previouslyCalculatedRoute
		if (prev != null) {
			val id = points[1].getRoad().id
			val ss = points[1].getSegmentStart().toInt()
			val px = points[1].getRoad().getPoint31XTile(ss)
			val py = points[1].getRoad().getPoint31YTile(ss)
			for (i in prev.indices) {
				val rsr = prev[i]
				if (id == rsr.getObject().getId()) {
					if (KMapUtils.getDistance(
							rsr.getPoint(rsr.getEndPointIndex()), KMapUtils.get31LatitudeY(py),
							KMapUtils.get31LongitudeX(px)
						) < 50
					) {
						firstPartRecalculatedRoute = ArrayList(i + 1)
						restPartRecalculatedRoute = ArrayList(prev.size - i)
						for (k in prev.indices) {
							if (k <= i) {
								firstPartRecalculatedRoute.add(prev[k])
							} else {
								restPartRecalculatedRoute.add(prev[k])
							}
						}
						println("Recalculate only first part of the route")
						break
					}
				}
			}
		}
		val results = RouteCalcResult(ArrayList())
		for (i in 0 until points.size - 1) {
			val local = RoutingContext(ctx)
			if (i == 0) {
				if (useSmartRouteRecalculation) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute
				}
			}
			val res = searchRouteInternalPrepare(local, points[i], points[i + 1], routeDirection)
			makeStartEndPointsPrecise(local, res, points[i].getPreciseLatLon(), points[i + 1].getPreciseLatLon())
			results.detailed.addAll(res.detailed)
			ctx.routingTime += local.routingTime
			if (restPartRecalculatedRoute != null) {
				results.detailed.addAll(restPartRecalculatedRoute)
				break
			}
		}
		ctx.unloadAllData()
		return results
	}

	companion object {
		private val log = LoggerFactory.getLogger("RoutePlannerFrontEnd")

		// Check issue #8649
		const val GPS_POSSIBLE_ERROR = 7.0

		/** Read by the HH config: with the missing maps check on, only the best group of HH files is routed over. */
		@JvmField
		var CALCULATE_MISSING_MAPS = true

		@JvmStatic
		fun defaultHHConfig(): HHRoutingConfig {
			return HHRoutingConfig.astar(0).calcDetailed(HHRoutingConfig.CALCULATE_ALL_DETAILED)
				.applyCalculateMissingMaps(CALCULATE_MISSING_MAPS)
		}

		// exported to Objective-C under another name: a macro of the same name in the C++ core headers
		// would otherwise break every file that includes both
		@OptIn(ExperimentalObjCName::class)
		@ObjCName("traceRouting")
		@JvmField
		var TRACE_ROUTING = false

		@JvmStatic
		fun squareDist(x1: Int, y1: Int, x2: Int, y2: Int): Double {
			return KMapUtils.squareDist31TileMetric(x1, y1, x2, y2)
		}

		/** A road drawn straight through the points, for the parts of a track no road matches. */
		@JvmStatic
		fun generateStraightLineSegment(averageSpeed: Float, points: List<KLatLon?>): RouteSegmentResult {
			val reg = RouteRegion()
			reg.initRouteEncodingRule(0, "highway", TurnPreparation.UNMATCHED_HIGHWAY_TYPE)
			val rdo = RouteDataObject(reg)
			val size = points.size
			val x = KTIntArrayList(size)
			val y = KTIntArrayList(size)
			var distance = 0.0
			var distOnRoadToPass = 0.0
			var prev: KLatLon? = null
			for (i in 0 until size) {
				val l = points[i]
				if (l != null) {
					x.add(KMapUtils.get31TileNumberX(l.longitude))
					y.add(KMapUtils.get31TileNumberY(l.latitude))
					if (prev != null) {
						val d = KMapUtils.getDistance(l, prev)
						distance += d
						distOnRoadToPass += d / averageSpeed
					}
				}
				prev = l
			}
			rdo.pointsX = x.toArray()
			rdo.pointsY = y.toArray()
			rdo.types = intArrayOf(0)
			rdo.id = -1
			val segment = RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1)
			segment.setSegmentTime(distOnRoadToPass.toFloat())
			segment.setSegmentSpeed(averageSpeed)
			segment.setDistance(distance.toFloat())
			segment.setTurnType(TurnType.straight())
			return segment
		}
	}
}
