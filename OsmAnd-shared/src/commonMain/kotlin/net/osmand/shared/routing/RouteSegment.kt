package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.util.KMapUtils
import kotlin.experimental.ExperimentalObjCName
import kotlin.jvm.JvmField
import kotlin.native.ObjCName

/**
 * One step of the A* search: a road, the point the step starts at and the neighbouring point it
 * ends at, and how it was reached.
 *
 * In the map data a segment is always of length one, `[X, X + 1]` or `[X - 1, X]`. Segments of the
 * same road that share a point are chained through [nextLoaded] as the tile is loaded, and the
 * ones a vehicle may take through [next]; [parentRoute] is how the search got here, and the two
 * distances are what A* orders the open set by, in seconds.
 *
 * A copy of `BinaryRoutePlanner.RouteSegment` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. It implements [RoadTraversal], which is all the vehicle router asks
 * of it.
 */
open class RouteSegment(road: RouteDataObject?, segmentStart: Int, segmentEnd: Int) : RoadTraversal {

	// # Final fields that store objects
	@JvmField
	val segStart: Short = segmentStart.toShort()

	@JvmField
	val segEnd: Short = segmentEnd.toShort()

	/** Null only on [NULL], the marker a start or end segment is given as its parent. */
	@JvmField
	val road: RouteDataObject? = road

	// # Represents cheap-storage of LinkedList connected segments
	// All the road segments from map data connected to the same end point
	@JvmField
	var nextLoaded: RouteSegment? = null

	// Segments only allowed for Navigation connected to the same end point
	@JvmField
	var next: RouteSegment? = null

	// # Caches of similar segments to speed up routing calculation
	// Segment of opposite direction i.e. for [4 -> 5], opposite [5 -> 4]
	@JvmField
	var oppositeDirection: RouteSegment? = null

	// Same Road/ same Segment but used for opposite A* search (important to have different cause #parentRoute is different)
	// Note: if we use 1-direction A* then this is field is not needed
	@JvmField
	var reverseSearch: RouteSegment? = null

	// # Important for A*-search to distinguish whether segment was visited or not
	// Initially all segments null and startSegment/endSegment.parentRoute = RouteSegment.NULL;
	// After iteration stores previous segment i.e. how it was reached from startSegment
	@JvmField
	var parentRoute: RouteSegment? = null

	// # A* routing - Distance measured in time (seconds)
	// There is a small (important!!!) difference how it's calculated for visited (parentRoute != null) and non-visited
	// NON-VISITED: time from Start [End for reverse A*] to @segStart of @this, including turn time from previous segment (@parentRoute)
	// VISITED: time from Start [End for reverse A*] to @segEnd of @this,
	//          including turn time from previous segment (@parentRoute) and obstacle / distance time between @segStart-@segEnd on @this
	@JvmField
	var distanceFromStart: Float = 0f

	// NON-VISITED: Approximated (h(x)) time from @segStart of @this route segment to End [Start for reverse A*]
	// VISITED: Approximated (h(x)) time from @segEnd of @this route segment to End [Start for reverse A*]
	@JvmField
	var distanceToEnd: Float = 0f

	constructor(road: RouteDataObject, segmentStart: Int) :
			this(road, segmentStart, if (segmentStart < road.getPointsLength() - 1) segmentStart + 1 else segmentStart - 1)

	/** This segment when it already runs in [positiveDirection], else the one that does; null past the end of the road. */
	fun initRouteSegment(positiveDirection: Boolean): RouteSegment? {
		if (segStart.toInt() == 0 && !positiveDirection) {
			return null
		}
		if (segStart.toInt() == road!!.getPointsLength() - 1 && positiveDirection) {
			return null
		}
		if (segStart == segEnd) {
			throw IllegalArgumentException()
		} else {
			if (positiveDirection == (segEnd > segStart)) {
				return this
			} else {
				var opposite = oppositeDirection
				if (opposite == null) {
					opposite = RouteSegment(road, segStart.toInt(), if (segEnd > segStart) segStart - 1 else segStart + 1)
					opposite.oppositeDirection = this
					oppositeDirection = opposite
				}
				return opposite
			}
		}
	}

	fun isSegmentAttachedToStart(): Boolean = parentRoute != null

	fun getParentRoute(): RouteSegment? = if (parentRoute === NULL) null else parentRoute

	override fun isPositive(): Boolean = segEnd > segStart

	fun setParentRoute(parentRoute: RouteSegment?) {
		this.parentRoute = parentRoute
	}

	fun getNext(): RouteSegment? = next

	override fun getSegmentStart(): Short = segStart

	fun getStartPointX(): Int = road!!.getPoint31XTile(segStart.toInt())

	fun getStartPointY(): Int = road!!.getPoint31YTile(segStart.toInt())

	fun getEndPointY(): Int = road!!.getPoint31YTile(segEnd.toInt())

	fun getEndPointX(): Int = road!!.getPoint31XTile(segEnd.toInt())

	override fun getSegmentEnd(): Short = segEnd

	fun getDistanceFromStart(): Float = distanceFromStart

	fun setDistanceFromStart(distanceFromStart: Float) {
		this.distanceFromStart = distanceFromStart
	}

	fun getDepth(): Int {
		val parent = parentRoute ?: return 0
		return parent.getDepth() + 1
	}

	override fun getRoad(): RouteDataObject = road!!

	override fun toString(): String {
		var dst = ""
		val road = this.road
		if (road != null) {
			val x = road.getPoint31XTile(segStart.toInt())
			val y = road.getPoint31YTile(segStart.toInt())
			val xe = road.getPoint31XTile(segEnd.toInt())
			val ye = road.getPoint31YTile(segEnd.toInt())
			dst = ((KMapUtils.squareRootDist31(x, y, xe, ye) * 10).toInt() / 10.0f).toString() + " m"
		}
		if (distanceFromStart != 0f) {
			dst = "dstStart=%.2f".format(distanceFromStart)
		}
		return (road?.toString() ?: "NULL") + " [" + segStart + "-" + segEnd + "] " + dst
	}

	/** This segment and the ones chained after it through [next]. */
	fun getIterator(): Iterator<RouteSegment> {
		return object : Iterator<RouteSegment> {
			var next: RouteSegment? = this@RouteSegment

			override fun hasNext(): Boolean = next != null

			override fun next(): RouteSegment {
				val c = next!!
				next = c.next
				return c
			}
		}
	}

	companion object {
		// # Represents parent segment for Start & End segment
		// Named NULL in java; exported to Objective-C under another name, since NULL is a macro there
		// and a property called that breaks the compile of every file that imports the framework
		@OptIn(ExperimentalObjCName::class)
		@ObjCName("nullSegment")
		@JvmField
		val NULL = RouteSegment(null, 0, 1)
	}
}

/** A segment with the precise point a route starts or ends at, and the other candidates for it. */
open class RouteSegmentPoint : RouteSegment {

	@JvmField
	var distToProj: Double

	@JvmField
	var preciseX: Int

	@JvmField
	var preciseY: Int

	@JvmField
	var others: MutableList<RouteSegmentPoint>? = null

	constructor(road: RouteDataObject, segmentStart: Int, distToProj: Double) : super(road, segmentStart) {
		this.distToProj = distToProj
		this.preciseX = road.getPoint31XTile(segmentStart, segmentStart + 1)
		this.preciseY = road.getPoint31YTile(segmentStart, segmentStart + 1)
	}

	constructor(road: RouteDataObject, segmentStart: Int, segmentEnd: Int, distToProjSquare: Double) :
			super(road, segmentStart, segmentEnd) {
		this.distToProj = distToProjSquare
		this.preciseX = road.getPoint31XTile(segmentStart, segmentEnd)
		this.preciseY = road.getPoint31YTile(segmentStart, segmentEnd)
	}

	constructor(pnt: RouteSegmentPoint) : super(pnt.road, pnt.segStart.toInt(), pnt.segEnd.toInt()) {
		this.distToProj = pnt.distToProj
		this.preciseX = pnt.preciseX
		this.preciseY = pnt.preciseY
	}

	fun getPreciseLatLon(): KLatLon = KLatLon(KMapUtils.get31LatitudeY(preciseY), KMapUtils.get31LongitudeX(preciseX))

	override fun toString(): String = "$segStart (${getPreciseLatLon()}): $road"
}

/** Where the two searches met: the segment, and the segment of the other search it was matched with. */
open class FinalRouteSegment(road: RouteDataObject, segmentStart: Int, segmentEnd: Int) : RouteSegment(road, segmentStart, segmentEnd) {

	@JvmField
	var reverseWaySearch: Boolean = false

	@JvmField
	var opposite: RouteSegment? = null
}

/** Every final segment a one sided search reached, for the dijkstra mode. */
class MultiFinalRouteSegment(f: FinalRouteSegment) : FinalRouteSegment(f.getRoad(), f.getSegmentStart().toInt(), f.getSegmentEnd().toInt()) {

	@JvmField
	val all: MutableList<FinalRouteSegment> = ArrayList()

	init {
		this.distanceFromStart = f.distanceFromStart
		this.distanceToEnd = f.distanceToEnd
	}
}

/**
 * A callback the planner tells about every segment it settles, and the gpx approximation about
 * every stretch of route it attached between two track points.
 */
interface RouteSegmentVisitor {
	fun visitSegment(segment: RouteSegment, segmentEnd: Int, poll: Boolean)
	fun visitApproximatedSegments(segment: List<RouteSegmentResult>, start: GpxPoint, target: GpxPoint)
}
