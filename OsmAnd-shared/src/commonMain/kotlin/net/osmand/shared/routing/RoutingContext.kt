package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.format
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.OpeningHoursTime
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max

/**
 * One route calculation over a set of open obf files: the request, plus the tiles of roads the
 * planner has loaded and the graph nodes built over them.
 *
 * The request half is [RoutingRequest]; this adds what the java planner needs to run - the readers
 * and their subregions, the tile cache with its memory accounting, and the segments a restriction
 * leaves to visit. A tile is loaded whole the first time a point in it is asked for, its roads are
 * filtered through the vehicle router and, with the direction points attached, turned into
 * [RouteSegment] chains keyed by point.
 *
 * A copy of `net.osmand.router.RoutingContext`, which stays in OsmAnd-java for android and tools;
 * this copy is for iOS. What is left out is the native side - the C++ session handle, the
 * `NativeLibrary` and the tiles loaded through it - because on iOS this planner is what replaces
 * the C++ one.
 */
class RoutingContext : RoutingRequest {

	// Final context variables
	@JvmField
	val map: MutableMap<BinaryMapIndexReader, MutableList<RouteSubregion>> = LinkedHashMap()

	@JvmField
	val reverseMap: MutableMap<RouteRegion, BinaryMapIndexReader> = LinkedHashMap()

	private val conditionalHelper = RouteConditionalHelper()

	/**
	 * [RoutingConfiguration.routeCalculationTime] read as a wall clock, worked out once instead of once
	 * per road: the router asks for it for every `:conditional` tag of every road it loads, and turning
	 * an instant into a local date-time costs a time zone lookup. The C++ core keeps the same reading in
	 * a `tm` next to the time itself; java rebuilds a `Calendar` every time, which is cheaper there than
	 * this is here. Recomputed if the configuration's time changes under us.
	 */
	private var conditionalTimeMillis: Long = 0
	private var conditionalTime: OpeningHoursTime? = null

	private fun conditionalTime(): OpeningHoursTime? {
		val millis = config.routeCalculationTime
		if (millis == 0L) {
			return null
		}
		var time = conditionalTime
		if (time == null || conditionalTimeMillis != millis) {
			time = OpeningHoursTime.ofEpochMillis(millis)
			conditionalTimeMillis = millis
			conditionalTime = time
		}
		return time
	}

	// 1. Initial variables
	@JvmField
	var dijkstraMode: Int = 0

	@JvmField
	var mapIndexReaderFilter: MutableSet<BinaryMapIndexReader> = HashSet()

	@JvmField
	var hhHasUnsupportedParameters: Boolean = false

	@JvmField
	var calculationProgressFirstPhase: RouteCalculationProgress? = null

	@JvmField
	var previouslyCalculatedRoute: List<RouteSegmentResult>? = null

	// 2. Routing memory cache (big objects)
	internal var indexedSubregions = KTLongObjectMap<MutableList<RoutingSubregionTile>>()

	// Needs to be a sorted array list . Another option to use hashmap but it will be more memory expensive
	internal val subregionTiles: MutableList<RoutingSubregionTile> = ArrayList()

	// 3. Warm object caches
	internal val segmentsToVisitPrescripted: MutableList<RouteSegment> = ArrayList(5)
	internal val segmentsToVisitNotForbidden: MutableList<RouteSegment> = ArrayList(5)

	// 5. debug information (package accessor)
	@JvmField
	var global = TileStatistics()

	// updated by route planner in bytes
	@JvmField
	var memoryOverhead: Int = 0

	@JvmField
	var memoryHits: Int = 0 // reset each routing run

	@JvmField
	var routingTime: Float = 0f

	// callback of processing segments
	internal var visitor: RouteSegmentVisitor? = null

	// what loadRouteSegment excludes while it collects the segments at a point; java allocates
	// one per call, which the jvm's young generation makes free and Kotlin/Native's collector does not
	private val excludeDuplicationsScratch = KTLongObjectMap<RouteDataObject>()

	// old planner
	@JvmField
	var finalRouteSegment: FinalRouteSegment? = null

	constructor(cp: RoutingContext) : super(cp.config, cp.calculationMode) {
		this.map.putAll(cp.map)
		this.leftSideNavigation = cp.leftSideNavigation
		this.reverseMap.putAll(cp.reverseMap)
		this.visitor = cp.visitor
		this.calculationProgress = cp.calculationProgress
	}

	constructor(config: RoutingConfiguration, list: List<BinaryMapIndexReader>, calcMode: RouteCalculationMode) :
			super(config, calcMode) {
		for (mr in list) {
			val rr = mr.getRoutingIndexes()
			val subregions = ArrayList<RouteSubregion>()
			for (r in rr) {
				val subregs = if (calcMode == RouteCalculationMode.BASE) r.getBaseSubregions() else r.getSubregions()
				for (rs in subregs) {
					subregions.add(RouteSubregion(rs))
				}
				this.reverseMap[r] = mr
			}
			this.map[mr] = subregions
		}
		this.intermediatesX = IntArray(0)
		this.intermediatesY = IntArray(0)
	}

	fun getVisitor(): RouteSegmentVisitor? = visitor

	fun getCurrentlyLoadedTiles(): Int {
		var cnt = 0
		for (t in this.subregionTiles) {
			if (t.isLoaded()) {
				cnt++
			}
		}
		return cnt
	}

	fun getCurrentEstimatedSize(): Int = global.size

	fun setVisitor(visitor: RouteSegmentVisitor?) {
		this.visitor = visitor
	}

	fun initStartAndTargetPoints(start: RouteSegmentPoint, end: RouteSegmentPoint) {
		initTargetPoint(end)
		startX = start.preciseX
		startY = start.preciseY
		startRoadId = start.getRoad().getId()
		startSegmentInd = start.getSegmentStart().toInt()
	}

	fun initTargetPoint(end: RouteSegmentPoint) {
		targetX = end.preciseX
		targetY = end.preciseY
		targetRoadId = end.getRoad().getId()
		targetSegmentInd = end.getSegmentStart().toInt()
	}

	fun unloadAllData() {
		unloadAllData(null)
	}

	fun unloadAllData(except: RoutingContext?) {
		memoryHits = 0
		for (tl in subregionTiles) {
			if (tl.isLoaded()) {
				if (except == null || except.searchSubregionTile(tl.subregion) < 0) {
					tl.unload()
					calculationProgress?.let { it.unloadedTiles++ }
					global.size -= tl.tileStatistics.size
				}
			}
		}
		subregionTiles.clear()
		indexedSubregions.clear()
		mapIndexReaderFilter = HashSet()
	}

	private fun searchSubregionTile(subregion: RouteSubregion): Int {
		val key = RoutingSubregionTile(subregion)
		var ind = subregionTiles.binarySearch(key, Comparator { o1, o2 ->
			if (o1.subregion.left == o2.subregion.left) {
				0
			} else if (o1.subregion.left < o2.subregion.left) 1 else -1
		})
		if (ind >= 0) {
			for (i in ind..subregionTiles.size) {
				if (i == subregionTiles.size || subregionTiles[i].subregion.left > subregion.left) {
					ind = -i - 1
					return ind
				}
				if (subregionTiles[i].subregion === subregion) {
					return i
				}
			}
		}
		return ind
	}

	fun loadRouteSegment(x31: Int, y31: Int, memoryLimit: Long): RouteSegment? {
		return loadRouteSegment(x31, y31, memoryLimit, false)
	}

	fun loadRouteSegment(x31: Int, y31: Int, memoryLimit: Long, reverseWaySearch: Boolean): RouteSegment? {
		val tileId = getRoutingTile(x31, y31, memoryLimit)
		val excludeDuplications = excludeDuplicationsScratch
		excludeDuplications.clear()
		var original: RouteSegment? = null
		val subregions = indexedSubregions[tileId]
		if (subregions != null) {
			for (j in subregions.indices) {
				original = subregions[j].loadRouteSegment(x31, y31, excludeDuplications, original, subregions, j, reverseWaySearch)
			}
		}
		return original
	}

	fun loadSubregionTile(ts: RoutingSubregionTile, toLoad: MutableList<RouteDataObject>?, excludeNotAllowed: KTLongHashSet?) {
		val now = nanoTime()
		val wasUnloaded = ts.isUnloaded()
		val ucount = ts.getUnloadCont()

		var points: List<DirectionPoint> = emptyList()
		val directionPoints = config.getDirectionPoints()
		if (directionPoints != null) {
			// retrieve direction points for attach to routing
			points = directionPoints.queryInBox(
				KQuadRect(ts.subregion.left.toDouble(), ts.subregion.top.toDouble(), ts.subregion.right.toDouble(), ts.subregion.bottom.toDouble()),
				ArrayList()
			)
			val createType = ts.subregion.routeReg.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.CREATE_TYPE)
			for (d in points) {
				d.types.clear()
				for (e in d.getTags().entries) {
					val type = ts.subregion.routeReg.searchRouteEncodingRule(e.key, e.value)
					if (type != -1) {
						d.types.add(type)
					}
				}
				d.types.add(createType)
			}
		}

		val reader = reverseMap[ts.subregion.routeReg]!!
		ts.setLoadedNonNative()
		val res = reader.loadRouteIndexData(ts.subregion)

		if (toLoad != null) {
			for (ro in res) {
				if (ro != null) {
					toLoad.add(ro)
				}
			}
		} else {
			val conditionalTime = conditionalTime()
			for (ro in res) {
				if (ro != null) {
					val ambiguousConditionalTags = config.ambiguousConditionalTags
					if (ambiguousConditionalTags != null) {
						conditionalHelper.resolveAmbiguousConditionalTags(ro, ambiguousConditionalTags)
					}
					if (conditionalTime != null) {
						conditionalHelper.processConditionalTags(ro, conditionalTime)
					}
					if (config.router.acceptLine(ro)) {
						if (excludeNotAllowed != null && !excludeNotAllowed.contains(ro.getId())) {
							// don't attach point for route precalculation
							if (!config.router.attributes.containsKey(GeneralRouter.CHECK_ALLOW_PRIVATE_NEEDED)) {
								connectPoint(ts, ro, points)
							}
							ts.add(ro)
						}
					}
					if (excludeNotAllowed != null && ro.getId() > 0) {
						excludeNotAllowed.add(ro.getId())
						var excludedIds = ts.excludedIds
						if (excludedIds == null) {
							excludedIds = KTLongHashSet()
							ts.excludedIds = excludedIds
						}
						excludedIds.add(ro.getId())
					}
				}
			}
		}
		val progress = calculationProgress
		if (progress != null) {
			progress.loadedTiles++
		}

		if (wasUnloaded) {
			if (ucount == 1) {
				if (progress != null) {
					progress.loadedPrevUnloadedTiles++
				}
			}
		} else {
			global.allRoutes += ts.tileStatistics.allRoutes
			global.coordinates += ts.tileStatistics.coordinates
			if (progress != null) {
				progress.distinctLoadedTiles++
			}
		}
		global.size += ts.tileStatistics.size
		if (progress != null) {
			progress.timeToLoad += (nanoTime() - now)
		}
	}

	fun loadAllSubregionTiles(reader: BinaryMapIndexReader, reg: RouteSubregion): List<RoutingSubregionTile> {
		val list = ArrayList<RoutingSubregionTile>()
		val request = SearchRequest.buildSearchRouteRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE)
		val subregs = reader.searchRouteIndexTree(request, listOf(reg))
		for (s in subregs) {
			list.add(RoutingSubregionTile(s))
		}
		return list
	}

	fun loadTileHeaders(x31: Int, y31: Int): MutableList<RoutingSubregionTile>? {
		val zoomToLoad = 31 - config.ZOOM_TO_LOAD_TILES
		val tileX = x31 shr zoomToLoad
		val tileY = y31 shr zoomToLoad

		val now = nanoTime()
		val request = SearchRequest.buildSearchRouteRequest(
			tileX shl zoomToLoad, (tileX + 1) shl zoomToLoad, tileY shl zoomToLoad, (tileY + 1) shl zoomToLoad
		)
		var collection: MutableList<RoutingSubregionTile>? = null
		for (r in map.entries) {
			val reader = r.key

			if (mapIndexReaderFilter.isNotEmpty()) {
				val isUnwantedMap = !mapIndexReaderFilter.contains(reader)
				val containsFastRouting = reader.hasHHRoutingIndexes()
				if (isUnwantedMap && containsFastRouting) {
					continue
				}
				val isWorldMap = reader.getFile().name().lowercase().startsWith(WORLD_MAP_PREFIX)
				if (isWorldMap) {
					continue
				}
			}

			// NOTE: load headers same as we do in non-native (it is not native optimized)
			var intersect = false
			for (rs in r.value) {
				if (request.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
					intersect = true
					break
				}
			}
			if (intersect) {
				val subregs = r.key.searchRouteIndexTree(request, r.value)
				for (sr in subregs) {
					val ind = searchSubregionTile(sr)
					val found: RoutingSubregionTile
					if (ind < 0) {
						found = RoutingSubregionTile(sr)
						subregionTiles.add(-(ind + 1), found)
					} else {
						found = subregionTiles[ind]
					}
					if (collection == null) {
						collection = ArrayList(4)
					}
					collection.add(found)
				}
			}
		}
		calculationProgress?.let { it.timeToLoadHeaders += (nanoTime() - now) }

		return collection
	}

	fun loadTileData(x31: Int, y31: Int, zoomAround: Int, toFillIn: MutableList<RouteDataObject>) {
		loadTileData(x31, y31, zoomAround, toFillIn, false)
	}

	fun loadTileData(x31: Int, y31: Int, zoomAround: Int, toFillIn: MutableList<RouteDataObject>, allowDuplications: Boolean) {
		var t = config.ZOOM_TO_LOAD_TILES - zoomAround
		var coordinatesShift = 1 shl (31 - config.ZOOM_TO_LOAD_TILES)
		if (t <= 0) {
			t = 1
			coordinatesShift = 1 shl (31 - zoomAround)
		} else {
			t = 1 shl t
		}

		val ts = KTLongHashSet()
		for (i in -t..t) {
			for (j in -t..t) {
				ts.add(getRoutingTile(x31 + i * coordinatesShift, y31 + j * coordinatesShift, 0))
			}
		}
		val it = ts.iterator()
		val excludeDuplications = KTLongObjectMap<RouteDataObject>()
		while (it.hasNext()) {
			getAllObjects(it.next(), toFillIn, excludeDuplications)
			if (allowDuplications) {
				excludeDuplications.clear()
			}
		}
	}

	private fun getRoutingTile(x31: Int, y31: Int, memoryLimitArg: Long): Long {
		var memoryLimit = memoryLimitArg
		val zmShift = 31 - config.ZOOM_TO_LOAD_TILES
		val xloc = (x31 shr zmShift).toLong()
		val yloc = (y31 shr zmShift).toLong()
		val tileId = (xloc shl config.ZOOM_TO_LOAD_TILES) + yloc
		if (memoryLimit == 0L) {
			memoryLimit = config.memoryLimitation
		}
		if (getCurrentEstimatedSize() > 0.9 * memoryLimit) {
			val sz1 = getCurrentEstimatedSize()
			unloadUnusedTiles(memoryLimit)
			memoryHits++
			if (config.memoryMaxHits >= 0 && config.memoryMaxHits < memoryHits) {
				throwNotEnoughMemory()
			}
			val mb = (1 shl 20).toFloat()
			val sz2 = getCurrentEstimatedSize()
			log.warn(
				"Unload tiles :  occupied before " + sz1 / mb + " Mb - now  " + sz2 / mb + "MB "
						+ memoryLimit / mb + " limit MB " + config.memoryLimitation / mb + " " + memoryHits
			)
		}
		if (!indexedSubregions.containsKey(tileId)) {
			// java keeps a null here for a tile no file covers; an empty list says the same
			val collection = loadTileHeaders(x31, y31)
			indexedSubregions.put(tileId, collection ?: ArrayList(0))
		}
		val subregions = indexedSubregions[tileId]
		if (subregions != null) {
			var load = false
			for (ts in subregions) {
				if (!ts.isLoaded()) {
					load = true
				}
			}
			if (load) {
				val excludeIds = KTLongHashSet()
				for (ts in subregions) {
					if (!ts.isLoaded()) {
						loadSubregionTile(ts, null, excludeIds)
					} else {
						val excludedIds = ts.excludedIds
						if (excludedIds != null) {
							excludeIds.addAll(excludedIds)
						}
					}
				}
			}
		}
		return tileId
	}

	private fun connectPoint(ts: RoutingSubregionTile, ro: RouteDataObject, points: List<DirectionPoint>) {
		val region = ro.region!!
		val createType = region.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.CREATE_TYPE)
		val deleteType = region.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.DELETE_TYPE)

		for (np in points) {
			if (np.types.size == 0) {
				continue
			}

			val wptX = KMapUtils.get31TileNumberX(np.getLongitude())
			val wptY = KMapUtils.get31TileNumberY(np.getLatitude())

			var x = ro.getPoint31XTile(0)
			var y = ro.getPoint31YTile(0)

			var mindist = config.directionPointsRadius * 2.0
			var indexToInsert = 0
			var mprojx = 0
			var mprojy = 0
			for (i in 1 until ro.getPointsLength()) {
				val nx = ro.getPoint31XTile(i)
				val ny = ro.getPoint31YTile(i)
				val sgnx = nx - wptX > 0
				val sgx = x - wptX > 0
				val sgny = ny - wptY > 0
				val sgy = y - wptY > 0
				var checkPreciseProjection = true
				if (sgny == sgy && sgx == sgnx) {
					// Speedup: point outside of rect (line is diagonal) distance is likely be bigger
					val dist = KMapUtils.squareRootDist31(
						wptX, wptY, if (abs(nx - wptX) < abs(x - wptX)) nx else x,
						if (abs(ny - wptY) < abs(y - wptY)) ny else y
					)
					checkPreciseProjection = dist < config.directionPointsRadius
				}
				if (checkPreciseProjection) {
					val pnt = KMapUtils.getProjectionPoint31(wptX, wptY, x, y, nx, ny)
					val projx = pnt.x.toInt()
					val projy = pnt.y.toInt()
					val dist = KMapUtils.squareRootDist31(wptX, wptY, projx, projy)
					if (dist < mindist) {
						indexToInsert = i
						mindist = dist
						mprojx = projx
						mprojy = projy
					}
				}

				x = nx
				y = ny
			}
			val connected = np.connected
			val sameRoadId = connected != null && connected.getId() == ro.getId()
			val pointShouldBeAttachedByDist = (mindist < config.directionPointsRadius && mindist < np.distance)

			val npAngle = np.getAngle()
			var restrictionByAngle = !npAngle.isNaN()

			if (pointShouldBeAttachedByDist) {
				if (restrictionByAngle) {
					val oneWay = ro.getOneway() // -1 backward, 0 two way, 1 forward
					val forwardAngle = ro.directionRoute(indexToInsert, true) * RADIANS_TO_DEGREES
					if (oneWay == 1 || oneWay == 0) {
						val diff = abs(KMapUtils.degreesDiff(npAngle, forwardAngle))
						if (diff <= DirectionPoint.MAX_ANGLE_DIFF) {
							restrictionByAngle = false
						}
					}
					if (restrictionByAngle && (oneWay == -1 || oneWay == 0)) {
						val diff = abs(KMapUtils.degreesDiff(npAngle, forwardAngle + 180))
						if (diff <= DirectionPoint.MAX_ANGLE_DIFF) {
							restrictionByAngle = false
						}
					}
				}
				if (restrictionByAngle) {
					continue
				}
				if (!sameRoadId) {
					if (connected != null) {
						// check old connected points
						val pointIndex = findPointIndex(np, createType)
						if (pointIndex != -1) {
							// set type "deleted" for old connected point
							connected.setPointTypes(pointIndex, intArrayOf(deleteType))
						} else {
							throw RuntimeException()
						}
					}
				} else {
					val sameRoadPointIndex = findPointIndex(np, createType)
					if (sameRoadPointIndex != -1 && connected != null) {
						if (mprojx == np.connectedx && mprojy == np.connectedy) {
							continue // was found the same point
						} else {
							// set type "deleted" for old connected point
							connected.setPointTypes(sameRoadPointIndex, intArrayOf(deleteType))
						}
					}
				}
				np.connectedx = mprojx
				np.connectedy = mprojy
				ro.insert(indexToInsert, mprojx, mprojy)
				ro.setPointTypes(indexToInsert, np.types.toArray()) // np.types contains DirectionPoint.CREATE_TYPE
				np.distance = mindist
				np.connected = ro
			}
		}
	}

	private fun findPointIndex(np: DirectionPoint, createType: Int): Int {
		// using search by coordinates because by index doesn't work (parallel updates)
		var samePointIndex = -1
		val connected = np.connected
		var i = 0
		while (connected != null && i < connected.getPointsLength()) {
			val tx = connected.getPoint31XTile(i)
			val ty = connected.getPoint31YTile(i)
			if (tx == np.connectedx && ty == np.connectedy && connected.hasPointType(i, createType)) {
				samePointIndex = i
				break
			}
			i++
		}
		return samePointIndex
	}

	fun checkIfMemoryLimitCritical(memoryLimit: Long): Boolean {
		return getCurrentEstimatedSize() > 0.9 * memoryLimit
	}

	fun unloadUnusedTiles(memoryLimit: Long) {
		val desirableSize = memoryLimit * 0.7f
		val list = ArrayList<RoutingSubregionTile>(subregionTiles.size / 2)
		var loaded = 0
		for (t in subregionTiles) {
			if (t.isLoaded()) {
				list.add(t)
				loaded++
			}
		}
		calculationProgress?.let { it.maxLoadedTiles = max(it.maxLoadedTiles, getCurrentlyLoadedTiles()) }
		list.sortWith { o1, o2 ->
			val v1 = (o1.access + 1) * pow(10, o1.getUnloadCont() - 1)
			val v2 = (o2.access + 1) * pow(10, o2.getUnloadCont() - 1)
			if (v1 < v2) -1 else if (v1 == v2) 0 else 1
		}
		var i = 0
		while (getCurrentEstimatedSize() >= desirableSize && (list.size - i) > loaded / 5 && i < list.size) {
			val unload = list[i]
			i++
			unload.unload()
			calculationProgress?.let { it.unloadedTiles++ }
			global.size -= unload.tileStatistics.size
			// tile could be cleaned from routing tiles and deleted from whole list
		}
		for (t in subregionTiles) {
			t.access /= 3
		}
	}

	private fun getAllObjects(tileId: Long, toFillIn: MutableList<RouteDataObject>, excludeDuplications: KTLongObjectMap<RouteDataObject>) {
		val subregions = indexedSubregions[tileId]
		if (subregions != null) {
			for (rs in subregions) {
				rs.loadAllObjects(toFillIn, excludeDuplications)
			}
		}
	}

	/** One box of a routing r-tree as the planner holds it: loaded or not, and the segment chains over its roads. */
	class RoutingSubregionTile(@JvmField val subregion: RouteSubregion) {

		// make it without get/set for fast access
		@JvmField
		var access: Int = 0

		@JvmField
		var tileStatistics = TileStatistics()

		private var isLoaded = 0
		private var routes: KTLongObjectMap<RouteSegment>? = null
		internal var excludedIds: KTLongHashSet? = null

		fun getRoutes(): KTLongObjectMap<RouteSegment>? = routes

		fun loadAllObjects(toFillIn: MutableList<RouteDataObject>, excludeDuplications: KTLongObjectMap<RouteDataObject>) {
			val routes = this.routes ?: return
			val it = routes.iterator()
			while (it.hasNext()) {
				it.advance()
				var rs: RouteSegment? = it.value()
				while (rs != null) {
					val ro = rs.getRoad()
					if (!excludeDuplications.containsKey(ro.id)) {
						excludeDuplications.put(ro.id, ro)
						toFillIn.add(ro)
					}
					rs = rs.nextLoaded
				}
			}
		}

		internal fun loadRouteSegment(
			x31: Int, y31: Int, excludeDuplications: KTLongObjectMap<RouteDataObject>, originalArg: RouteSegment?,
			subregions: List<RoutingSubregionTile>, subregionIndex: Int, reverseWaySearch: Boolean
		): RouteSegment? {
			var original = originalArg
			access++
			val routes = this.routes ?: throw UnsupportedOperationException("Not clear how it could be used with native")
			val l = (x31.toLong() shl 31) + y31.toLong()
			var segment = routes[l]
			while (segment != null) {
				val ro = segment.getRoad()
				val toCmp = excludeDuplications[calcRouteId(ro, segment.getSegmentStart().toInt())]
				if (!isExcluded(ro.id, subregions, subregionIndex)
					&& (toCmp == null || toCmp.getPointsLength() < ro.getPointsLength())
				) {
					excludeDuplications.put(calcRouteId(ro, segment.getSegmentStart().toInt()), ro)
					if (reverseWaySearch) {
						var reverse = segment.reverseSearch
						if (reverse == null) {
							reverse = RouteSegment(ro, segment.getSegmentStart().toInt())
							reverse.reverseSearch = segment
							reverse.nextLoaded = segment.nextLoaded
							segment.reverseSearch = reverse
						}
						segment = reverse
					}
					segment.next = original
					original = segment
				}
				segment = segment.nextLoaded
			}
			return original
		}

		fun isLoaded(): Boolean = isLoaded > 0

		fun getUnloadCont(): Int = abs(isLoaded)

		fun isUnloaded(): Boolean = isLoaded < 0

		fun unload() {
			if (isLoaded == 0) {
				this.isLoaded = -1
			} else {
				isLoaded = -abs(isLoaded)
			}
			routes = null
			excludedIds = null
		}

		fun setLoadedNonNative() {
			isLoaded = abs(isLoaded) + 1
			routes = KTLongObjectMap()
			tileStatistics = TileStatistics()
		}

		fun add(ro: RouteDataObject) {
			tileStatistics.addObject(ro)
			val routes = this.routes!!
			for (i in 0 until ro.getPointsLength()) {
				val x31 = ro.getPoint31XTile(i)
				val y31 = ro.getPoint31YTile(i)
				val l = (x31.toLong() shl 31) + y31.toLong()
				val segment = RouteSegment(ro, i)
				val orig = routes[l]
				if (orig == null) {
					routes.put(l, segment)
				} else {
					var last: RouteSegment = orig
					while (true) {
						last = last.nextLoaded ?: break
					}
					last.nextLoaded = segment
				}
			}
		}

		companion object {
			private fun isExcluded(id: Long, subregions: List<RoutingSubregionTile>, subregionIndex: Int): Boolean {
				for (i in 0 until subregionIndex) {
					val excludedIds = subregions[i].excludedIds
					if (excludedIds != null && excludedIds.contains(id)) {
						return true
					}
				}
				return false
			}
		}
	}

	/** What the loaded tiles are estimated to take, for the memory limit. */
	class TileStatistics {
		@JvmField
		var size: Int = 0

		@JvmField
		var allRoutes: Int = 0

		@JvmField
		var coordinates: Int = 0

		override fun toString(): String {
			return "All routes " + allRoutes +
					" size " + (size / 1024f) + " KB coordinates " + coordinates + " ratio coord " + (size.toFloat() / coordinates) +
					" ratio routes " + (size.toFloat() / allRoutes)
		}

		fun addObject(o: RouteDataObject) {
			allRoutes++
			coordinates += o.getPointsLength() * 2
			size += getEstimatedSize(o)
		}
	}

	fun getMaps(): List<BinaryMapIndexReader> = ArrayList(map.keys)

	fun throwNotEnoughMemory() {
		throw IllegalStateException(
			"There is not enough memory %.5f, %.5f -> %.5f, %.5f - limit  %d  MB".format(
				KMapUtils.get31LatitudeY(startY), KMapUtils.get31LongitudeX(startX),
				KMapUtils.get31LatitudeY(targetY), KMapUtils.get31LongitudeX(targetX),
				config.memoryLimitation / (1 shl 20)
			)
		)
	}

	companion object {
		private val log = LoggerFactory.getLogger("RoutingContext")

		@JvmField
		var PRINT_ROUTING_ALERTS = false

		/** `WorldRegion.WORLD + "_"`: the file name prefix of the world basemap. */
		private const val WORLD_MAP_PREFIX = "world_"

		/** `Math.toDegrees` multiplies by this constant rather than dividing by pi, and the last bit follows. */
		private const val RADIANS_TO_DEGREES = 57.29577951308232

		private fun pow(base: Int, pw: Int): Int {
			var r = 1
			for (i in 0 until pw) {
				r *= base
			}
			return r
		}

		private fun calcRouteId(o: RouteDataObject, ind: Int): Long {
			return (o.getId() shl 10) + ind
		}

		@JvmStatic
		fun getEstimatedSize(o: RouteDataObject): Int {
			// calculate size
			var sz = 0
			sz += 8 + 4 // overhead
			val names = o.names
			if (names != null) {
				sz += 12
				val it = names.iterator()
				while (it.hasNext()) {
					it.advance()
					val vl = it.value()
					sz += 12 + vl.length
				}
				sz += 12 + names.size * 25
			}
			sz += 8 // id
			// coordinates
			sz += (8 + 4 + 4 * o.getPointsLength()) * 4
			val types = o.types
			sz += if (types == null) 4 else (8 + 4 + 4 * types.size)
			val restrictions = o.restrictions
			sz += if (restrictions == null) 4 else (8 + 4 + 8 * restrictions.size)
			sz += 4
			val pointTypes = o.pointTypes
			if (pointTypes != null) {
				sz += 8 + 4 * pointTypes.size
				for (i in pointTypes.indices) {
					sz += 4
					val point = pointTypes[i]
					if (point != null) {
						sz += 8 + 8 * point.size
					}
				}
			}
			// Standard overhead?
			return (sz * 3.5).toInt()
		}
	}
}
