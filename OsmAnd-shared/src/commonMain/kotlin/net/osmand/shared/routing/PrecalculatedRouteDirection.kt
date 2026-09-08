package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.KQuadTree
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.jvm.JvmStatic

/**
 * A route that has already been found, kept so the next search can lean on it.
 *
 * The points are indexed by place and carry the time still left to the end, so the A* heuristic can
 * ask "how long from here, if I follow the route I already have" instead of guessing from straight
 * line distance. That is what makes recalculating a long route cheap: the search stays in a corridor
 * around the old one, and only the ends are worked out again.
 *
 * The C++ core reads the eight fields below - the two coordinate arrays, the times, the speeds and
 * the finish times - and runs the same estimate itself.
 */
class PrecalculatedRouteDirection {

	private lateinit var pointsX: IntArray
	private lateinit var pointsY: IntArray
	private var minSpeed: Float = 0f
	private var maxSpeed: Float = 0f
	private lateinit var tms: FloatArray
	private var followNext: Boolean = false

	private val cachedS: MutableList<Int> = ArrayList()

	private var startPoint: Long = 0
	private var endPoint: Long = 0
	private val quadTree = KQuadTree<Int>(
		KQuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble()), 8, 0.55f
	)
	private var startFinishTime: Float = 0f
	private var endFinishTime: Float = 0f

	constructor(px: KTIntArrayList, py: KTIntArrayList, speedSegments: List<Float>, maxSpeed: Float) {
		this.maxSpeed = maxSpeed
		initPoints(px, py, speedSegments)
	}

	private constructor(ls: List<RouteSegmentResult>, maxSpeed: Float) {
		this.maxSpeed = maxSpeed
		initFromRoute(ls)
	}

	private constructor(ls: Array<KLatLon>, maxSpeed: Float) {
		this.maxSpeed = maxSpeed
		initFromLatLon(ls)
	}

	private constructor(parent: PrecalculatedRouteDirection, start: Int, end: Int) {
		this.minSpeed = parent.minSpeed
		this.maxSpeed = parent.maxSpeed
		var s1 = start
		var s2 = end
		var inverse = false
		if (s1 > s2) {
			val tmp = s1
			s1 = s2
			s2 = tmp
			inverse = true
		}
		tms = FloatArray(s2 - s1 + 1)
		pointsX = IntArray(s2 - s1 + 1)
		pointsY = IntArray(s2 - s1 + 1)
		for (i in s1..s2) {
			val shiftInd = i - s1
			pointsX[shiftInd] = parent.pointsX[i]
			pointsY[shiftInd] = parent.pointsY[i]
			quadTree.insert(shiftInd, parent.pointsX[i].toFloat(), parent.pointsY[i].toFloat())
			tms[shiftInd] = parent.tms[i] - parent.tms[if (inverse) s1 else s2]
		}
	}

	private fun initFromRoute(ls: List<RouteSegmentResult>) {
		val px = KTIntArrayList()
		val py = KTIntArrayList()
		val speedSegments = ArrayList<Float>()
		for (s in ls) {
			val plus = s.getStartPointIndex() < s.getEndPointIndex()
			var i = s.getStartPointIndex()
			val obj = s.getObject()
			val routeSpd = if (s.getRoutingTime() == 0f || s.getDistance() == 0f) maxSpeed
			else (s.getDistance() / s.getRoutingTime())
			while (true) {
				i = if (plus) i + 1 else i - 1
				px.add(obj.getPoint31XTile(i))
				py.add(obj.getPoint31YTile(i))
				speedSegments.add(routeSpd)
				if (i == s.getEndPointIndex()) {
					break
				}
			}
		}
		initPoints(px, py, speedSegments)
	}

	private fun initFromLatLon(ls: Array<KLatLon>) {
		val px = KTIntArrayList()
		val py = KTIntArrayList()
		val speedSegments = ArrayList<Float>()
		for (s in ls) {
			val routeSpd = maxSpeed // (s.getDistance() / s.getRoutingTime())
			px.add(KMapUtils.get31TileNumberX(s.longitude))
			py.add(KMapUtils.get31TileNumberY(s.latitude))
			speedSegments.add(routeSpd)
		}
		initPoints(px, py, speedSegments)
	}

	private fun initPoints(px: KTIntArrayList, py: KTIntArrayList, speedSegments: List<Float>) {
		var totaltm = 0f
		val times = ArrayList<Float>()
		for (i in 0 until px.size()) {
			// KMapUtils.measuredDist31 vs BinaryRoutePlanner.squareRootDist
			// use measuredDist31 because we use precise s.getDistance() to calculate routeSpd
			val ip = if (i == 0) 0 else i - 1
			val dist = KMapUtils.measuredDist31(px[ip], py[ip], px[i], py[i]).toFloat()
			val tm = dist / speedSegments[i] // routeSpd
			times.add(tm)
			quadTree.insert(i, px[i].toFloat(), py[i].toFloat())
			totaltm += tm
		}
		pointsX = px.toArray()
		pointsY = py.toArray()
		tms = FloatArray(times.size)
		var totDec = totaltm
		for (i in times.indices) {
			totDec -= times[i]
			tms[i] = totDec
		}
	}

	fun timeEstimate(sx31: Int, sy31: Int, ex31: Int, ey31: Int): Float {
		val l1 = calc(sx31, sy31)
		val l2 = calc(ex31, ey31)
		val x31: Int
		val y31: Int
		val start: Boolean
		if (l1 == startPoint || l1 == endPoint) {
			start = l1 == startPoint
			x31 = ex31
			y31 = ey31
		} else if (l2 == startPoint || l2 == endPoint) {
			start = l2 == startPoint
			x31 = sx31
			y31 = sy31
		} else {
			val sInd = getIndex(sx31, sy31)
			val eInd = getIndex(ex31, ey31)
			val err = "startPoint:$startPoint, endPoint:$endPoint, l1:$l1, l2:$l2, sx31:$sx31," +
					" sy31:$sy31, ex31:$ex31, ey31:$ey31, startIndex: $sInd, endIndex:$eInd"
			throw UnsupportedOperationException(err)
		}
		val ind = getIndex(x31, y31)
		if (ind == -1) {
			return -1f
		}
		if ((ind == 0 && start) || (ind == pointsX.size - 1 && !start)) {
			return -1f
		}
		val distToPoint = getDeviationDistance(x31, y31, ind)
		val deviationPenalty = distToPoint / minSpeed
		val finishTime = if (start) startFinishTime else endFinishTime
		return if (start) {
			(tms[0] - tms[ind]) + deviationPenalty + finishTime
		} else {
			tms[ind] + deviationPenalty + finishTime
		}
	}

	fun getDeviationDistance(x31: Int, y31: Int): Float {
		val ind = getIndex(x31, y31)
		if (ind == -1) {
			return 0f
		}
		return getDeviationDistance(x31, y31, ind)
	}

	fun getDeviationDistance(x31: Int, y31: Int, ind: Int): Float {
		var distToPoint = 0f
		if (ind < pointsX.size - 1 && ind != 0) {
			val nx = KMapUtils.squareRootDist31(x31, y31, pointsX[ind + 1], pointsY[ind + 1])
			val pr = KMapUtils.squareRootDist31(x31, y31, pointsX[ind - 1], pointsY[ind - 1])
			val nind = if (nx > pr) ind - 1 else ind + 1
			// pointsX twice is how java had it; the projection is only a penalty, and changing it
			// would move every route that leans on a precalculated one
			val proj = KMapUtils.getProjectionPoint31(
				x31, y31, pointsX[ind], pointsY[ind], pointsX[nind], pointsX[nind]
			)
			distToPoint = KMapUtils.squareRootDist31(x31, y31, proj.x.toInt(), proj.y.toInt()).toFloat()
		}
		return distToPoint
	}

	fun getIndex(x31: Int, y31: Int): Int {
		var ind = -1
		cachedS.clear()
		quadTree.queryInBox(
			KQuadRect(
				(x31 - SHIFT).toDouble(), (y31 - SHIFT).toDouble(),
				(x31 + SHIFT).toDouble(), (y31 + SHIFT).toDouble()
			), cachedS
		)
		if (cachedS.size == 0) {
			for (k in SHIFTS.indices) {
				quadTree.queryInBox(
					KQuadRect(
						(x31 - SHIFTS[k]).toDouble(), (y31 - SHIFTS[k]).toDouble(),
						(x31 + SHIFTS[k]).toDouble(), (y31 + SHIFTS[k]).toDouble()
					), cachedS
				)
				if (cachedS.size != 0) {
					break
				}
			}
			if (cachedS.size == 0) {
				return -1
			}
		}
		var minDist = 0.0
		for (i in cachedS.indices) {
			val n = cachedS[i]
			val ds = KMapUtils.squareRootDist31(x31, y31, pointsX[n], pointsY[n])
			if (ds < minDist || i == 0) {
				ind = n
				minDist = ds
			}
		}
		return ind
	}

	private fun calc(x31: Int, y31: Int): Long {
		return (x31.toLong() shl 32) + y31.toLong()
	}

	fun setFollowNext(followNext: Boolean) {
		this.followNext = followNext
	}

	fun isFollowNext(): Boolean = followNext

	/**
	 * Cuts the part of this route between the two points out, and measures how far the real start
	 * and target sit from it.
	 *
	 * Takes the points and the router rather than a routing context: the context holds an open obf
	 * reader and stays in OsmAnd-java, and this is all of it that was ever read here.
	 */
	fun adopt(startX: Int, startY: Int, targetX: Int, targetY: Int, router: VehicleRouter): PrecalculatedRouteDirection? {
		val ind1 = getIndex(startX, startY)
		val ind2 = getIndex(targetX, targetY)
		minSpeed = router.getDefaultSpeed()
		maxSpeed = router.getMaxSpeed()
		if (ind1 == -1) {
			return null
		}
		if (ind2 == -1) {
			return null
		}
		val routeDirection = PrecalculatedRouteDirection(this, ind1, ind2)
		routeDirection.startPoint = calc(startX, startY)
		routeDirection.startFinishTime =
			(KMapUtils.squareRootDist31(pointsX[ind1], pointsY[ind1], startX, startY) / maxSpeed).toFloat()
		routeDirection.endPoint = calc(targetX, targetY)
		routeDirection.endFinishTime =
			(KMapUtils.squareRootDist31(pointsX[ind2], pointsY[ind2], targetX, targetY) / maxSpeed).toFloat()
		routeDirection.followNext = followNext
		return routeDirection
	}

	fun updatePreciseStartEnd(sx: Int, sy: Int, ex: Int, ey: Int) {
		if (sx > 0 && sy > 0) {
			val ind = getIndex(sx, sy)
			if (ind != -1) {
				startPoint = calc(sx, sy)
				startFinishTime = (KMapUtils.squareRootDist31(pointsX[ind], pointsY[ind], sx, sy) / maxSpeed).toFloat()
			}
		}
		if (ex > 0 && ey > 0) {
			val ind = getIndex(ex, ey)
			if (ind != -1) {
				endPoint = calc(ex, ey)
				endFinishTime = (KMapUtils.squareRootDist31(pointsX[ind], pointsY[ind], ex, ey) / maxSpeed).toFloat()
			}
		}
	}

	companion object {

		private const val SHIFT = 1 shl (31 - 17)
		private val SHIFTS = intArrayOf(
			1 shl (31 - 15), 1 shl (31 - 13), 1 shl (31 - 12),
			1 shl (31 - 11), 1 shl (31 - 7)
		)

		@JvmStatic
		fun build(ls: List<RouteSegmentResult>, cutoffDistance: Float, maxSpeed: Float): PrecalculatedRouteDirection? {
			var begi = 0
			var d = cutoffDistance
			while (begi < ls.size) {
				d -= ls[begi].getDistance()
				if (d < 0) {
					break
				}
				begi++
			}
			var endi = ls.size
			d = cutoffDistance
			while (endi > 0) {
				d -= ls[endi - 1].getDistance()
				if (d < 0) {
					break
				}
				endi--
			}
			if (begi < endi) {
				return PrecalculatedRouteDirection(ls.subList(begi, endi), maxSpeed)
			}
			return null
		}

		@JvmStatic
		fun build(ls: Array<KLatLon>, maxSpeed: Float): PrecalculatedRouteDirection =
			PrecalculatedRouteDirection(ls, maxSpeed)
	}
}
