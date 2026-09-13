package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxUtilities.RouteSegment.Companion.START_TRKPT_IDX_ATTR
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.StringExternalizable
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.jvm.JvmOverloads
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * One road, or the part of it a route runs along, as the router produced it.
 *
 * A route is a list of these, and every one of them carries the road it came from plus where the
 * route enters and leaves it. [startPointIndex] is greater than [endPointIndex] when the route runs
 * against the direction the road was drawn in.
 */
class RouteSegmentResult : StringExternalizable<RouteDataBundle> {

	private var routeObject: RouteDataObject
	private var startPointIndex: Int = 0
	private var endPointIndex: Int = 0
	private var attachedRoutes: Array<MutableList<RouteSegmentResult>?>? = null
	private var preAttachedRoutes: Array<Array<RouteSegmentResult>?>? = null
	private var segmentTime: Float = 0f
	private var routingTime: Float = 0f
	private var speed: Float = 0f
	private var distance: Float = 0f
	private var description: Array<String>? = null

	// this make not possible to make turns in between segment result for now
	private var turnType: TurnType? = null
	private var leftside: Boolean = false

	/** Used by approximation to reconstruct finalPoints.routeToTarget. */
	private var gpxPointIndex: Int = -1

	constructor(routeObject: RouteDataObject) {
		this.routeObject = routeObject
	}

	constructor(routeObject: RouteDataObject, leftside: Boolean) {
		this.routeObject = routeObject
		this.leftside = leftside
	}

	constructor(routeObject: RouteDataObject, startPointIndex: Int, endPointIndex: Int) {
		this.routeObject = routeObject
		this.startPointIndex = startPointIndex
		this.endPointIndex = endPointIndex
		updateCapacity()
	}

	/** The constructor the C++ core calls on the java original, kept with the same shape. */
	constructor(
		routeObject: RouteDataObject, startPointIndex: Int, endPointIndex: Int,
		preAttachedRoutes: Array<Array<RouteSegmentResult>?>?, segmentTime: Float,
		routingTime: Float, speed: Float, distance: Float, gpxPointIndex: Int, turnType: TurnType?
	) {
		this.routeObject = routeObject
		this.startPointIndex = startPointIndex
		this.endPointIndex = endPointIndex
		this.preAttachedRoutes = preAttachedRoutes
		this.segmentTime = segmentTime
		this.routingTime = routingTime
		this.speed = speed
		this.distance = distance
		this.gpxPointIndex = gpxPointIndex
		this.turnType = turnType
		updateCapacity()
	}

	/** Adds every rule this segment uses to the route wide table a gpx file carries. */
	fun collectTypes(resources: RouteDataResources) {
		val rules = resources.getRules()
		routeObject.types?.let { collectRules(rules, it) }
		routeObject.pointTypes?.let { pointTypes ->
			val start = min(startPointIndex, endPointIndex)
			val end = max(startPointIndex, endPointIndex)
			var i = start
			while (i <= end && i < pointTypes.size) {
				pointTypes[i]?.let { collectRules(rules, it) }
				i++
			}
		}
	}

	fun collectNames(resources: RouteDataResources) {
		val rules = resources.getRules()
		val region = routeObject.region
		if (region.getNameTypeRule() != -1) {
			val r = region.quickGetEncodingRule(region.getNameTypeRule())
			if (r != null && !rules.containsKey(r)) {
				rules[r] = rules.size
			}
		}
		if (region.getRefTypeRule() != -1) {
			val r = region.quickGetEncodingRule(region.getRefTypeRule())
			if (r != null && !rules.containsKey(r)) {
				rules[r] = rules.size
			}
		}
		routeObject.nameIds?.let { nameIds ->
			for (nameId in nameIds) {
				if (nameId >= region.quickGetEncodingRulesSize()) {
					continue
				}
				val name = routeObject.names?.get(nameId) ?: continue
				val tag = region.quickGetEncodingRule(nameId)!!.getTag()
				val r = RouteTypeRule(tag, name)
				if (!rules.containsKey(r)) {
					rules[r] = rules.size
				}
			}
		}
		routeObject.pointNameTypes?.let { pointNameTypes ->
			val start = min(startPointIndex, endPointIndex)
			val end = min(max(startPointIndex, endPointIndex) + 1, pointNameTypes.size)
			for (i in start until end) {
				val types = pointNameTypes[i] ?: continue
				for (type in types) {
					if (type >= region.quickGetEncodingRulesSize()) {
						continue
					}
					val r = region.quickGetEncodingRule(type)
					if (r != null && !rules.containsKey(r)) {
						rules[r] = rules.size
					}
				}
			}
		}
	}

	private fun collectRules(rules: MutableMap<RouteTypeRule, Int>, types: IntArray) {
		val region = routeObject.region
		for (type in types) {
			if (type >= region.quickGetEncodingRulesSize()) {
				continue
			}
			val rule = region.quickGetEncodingRule(type) ?: continue
			val tag = rule.getTag()
			// heights are stored per point and rebuilt from the track, they are not route tags
			if (tag == "osmand_ele_start" || tag == "osmand_ele_end" ||
				tag == "osmand_ele_asc" || tag == "osmand_ele_desc"
			) {
				continue
			}
			if (!rules.containsKey(rule)) {
				rules[rule] = rules.size
			}
		}
	}

	private fun convertTypes(types: IntArray?, rules: Map<RouteTypeRule, Int>): IntArray? {
		if (types == null || types.isEmpty()) {
			return null
		}
		val arr = ArrayList<Int>()
		for (type in types) {
			if (type >= routeObject.region.quickGetEncodingRulesSize()) {
				continue
			}
			val rule = routeObject.region.quickGetEncodingRule(type) ?: continue
			rules[rule]?.let { arr.add(it) }
		}
		return IntArray(arr.size) { arr[it] }
	}

	private fun convertTypes(types: Array<IntArray?>?, rules: Map<RouteTypeRule, Int>): Array<IntArray?>? {
		if (types == null || types.isEmpty()) {
			return null
		}
		val res = arrayOfNulls<IntArray>(types.size)
		for (i in types.indices) {
			types[i]?.let { res[i] = convertTypes(it, rules) }
		}
		return res
	}

	private fun convertNameIds(nameIds: IntArray?, rules: Map<RouteTypeRule, Int>): IntArray? {
		if (nameIds == null || nameIds.isEmpty()) {
			return null
		}
		val res = IntArray(nameIds.size)
		for (i in nameIds.indices) {
			val nameId = nameIds[i]
			if (nameId >= routeObject.region.quickGetEncodingRulesSize()) {
				continue
			}
			val name = routeObject.names?.get(nameId) ?: continue
			val tag = routeObject.region.quickGetEncodingRule(nameId)!!.getTag()
			val rule = RouteTypeRule(tag, name)
			val ruleId = rules[rule] ?: throw IllegalArgumentException("Cannot find collected rule: $rule")
			res[i] = ruleId
		}
		return res
	}

	private fun convertPointNames(
		nameTypes: Array<IntArray?>?, pointNames: Array<Array<String>?>?,
		rules: MutableMap<RouteTypeRule, Int>
	): Array<IntArray?>? {
		if (nameTypes == null || nameTypes.isEmpty()) {
			return null
		}
		val res = arrayOfNulls<IntArray>(nameTypes.size)
		for (i in nameTypes.indices) {
			val types = nameTypes[i] ?: continue
			val arr = IntArray(types.size)
			for (k in types.indices) {
				val type = types[k]
				if (type >= routeObject.region.quickGetEncodingRulesSize()) {
					continue
				}
				val tag = routeObject.region.quickGetEncodingRule(type)!!.getTag()
				val name = pointNames!![i]!![k]
				val rule = RouteTypeRule(tag, name)
				var ruleId = rules[rule]
				if (ruleId == null) {
					ruleId = rules.size
					rules[rule] = ruleId
				}
				arr[k] = ruleId
			}
			res[i] = arr
		}
		return res
	}

	/** Puts the names read from a gpx file back onto the road, under the ids its region uses. */
	fun fillNames(resources: RouteDataResources) {
		val nameIds = routeObject.nameIds
		if (nameIds != null && nameIds.isNotEmpty()) {
			val region = routeObject.region
			val nameTypeRule = region.getNameTypeRule()
			val refTypeRule = region.getRefTypeRule()
			val names = KTIntObjectMap<String>()
			routeObject.names = names
			for (id in nameIds) {
				var nameId = id
				if (nameId >= region.quickGetEncodingRulesSize()) {
					continue
				}
				val rule = region.quickGetEncodingRule(nameId)
				if (rule != null) {
					if (nameTypeRule != -1 && "name" == rule.getTag()) {
						nameId = nameTypeRule
					} else if (refTypeRule != -1 && "ref" == rule.getTag()) {
						nameId = refTypeRule
					}
					rule.getValue()?.let { names.put(nameId, it) }
				}
			}
		}
		var pointNames: Array<Array<String>?>? = null
		var pointNameTypes: Array<IntArray?>? = null
		val pointNamesArr = resources.getPointNamesMap()[routeObject]
		if (pointNamesArr != null) {
			pointNames = arrayOfNulls(pointNamesArr.size)
			pointNameTypes = arrayOfNulls(pointNamesArr.size)
			for (i in pointNamesArr.indices) {
				val namesIds = pointNamesArr[i] ?: continue
				val namesRow = arrayOfNulls<String>(namesIds.size)
				val typesRow = IntArray(namesIds.size)
				for (k in namesIds.indices) {
					val id = namesIds[k]
					if (id >= routeObject.region.quickGetEncodingRulesSize()) {
						continue
					}
					val r = routeObject.region.quickGetEncodingRule(id)
					if (r != null) {
						namesRow[k] = r.getValue()
						val nameType = routeObject.region.searchRouteEncodingRule(r.getTag(), null)
						if (nameType != -1) {
							typesRow[k] = nameType
						}
					}
				}
				@Suppress("UNCHECKED_CAST")
				pointNames[i] = namesRow as Array<String>
				pointNameTypes[i] = typesRow
			}
		}
		routeObject.pointNames = pointNames
		routeObject.pointNameTypes = pointNameTypes
	}

	override fun writeToBundle(bundle: RouteDataBundle) {
		val resources = bundle.getResources()
		val rules = resources.getRules()

		val reversed = endPointIndex < startPointIndex
		val length = abs(endPointIndex - startPointIndex) + 1

		bundle.putInt("length", length)
		bundle.putInt(START_TRKPT_IDX_ATTR, resources.getCurrentSegmentStartLocationIndex())
		bundle.putFloat("segmentTime", segmentTime, 2)
		bundle.putFloat("speed", speed, 2)
		turnType?.let { turn ->
			bundle.putString("turnType", turn.toXmlString())
			if (turn.isSkipToSpeak) {
				bundle.putBoolean("skipTurn", turn.isSkipToSpeak)
			}
			if (turn.turnAngle != 0f) {
				bundle.putFloat("turnAngle", turn.turnAngle, 2)
			}
			val turnLanes = turn.lanes
			if (turnLanes != null && turnLanes.isNotEmpty()) {
				bundle.putString("turnLanes", TurnType.lanesToString(turnLanes))
			}
		}
		bundle.putLong("id", routeObject.id shr 6) // OsmAnd ID to OSM ID
		bundle.putArray("types", convertTypes(routeObject.types, rules))

		val start = min(startPointIndex, endPointIndex)
		val end = max(startPointIndex, endPointIndex) + 1
		val objectPointTypes = routeObject.pointTypes
		if (objectPointTypes != null && start < objectPointTypes.size) {
			val types = objectPointTypes.copyOfRange(start, min(end, objectPointTypes.size))
			if (reversed) {
				KCollectionUtils.reverseArray(types)
			}
			bundle.putArray("pointTypes", convertTypes(types, rules))
		}
		routeObject.nameIds?.let { bundle.putArray("names", convertNameIds(it, rules)) }
		val objectPointNameTypes = routeObject.pointNameTypes
		val objectPointNames = routeObject.pointNames
		if (objectPointNameTypes != null && start < objectPointNameTypes.size && objectPointNames != null) {
			val types = objectPointNameTypes.copyOfRange(start, min(end, objectPointNameTypes.size))
			val names = objectPointNames.copyOfRange(start, min(end, objectPointNames.size))
			if (reversed) {
				KCollectionUtils.reverseArray(types)
				KCollectionUtils.reverseArray(names)
			}
			bundle.putArray("pointNames", convertPointNames(types, names, rules))
		}

		resources.updateNextSegmentStartLocation(length)
	}

	override fun readFromBundle(bundle: RouteDataBundle) {
		val length = bundle.getInt("length", 0)
		val plus = length >= 0
		startPointIndex = if (plus) 0 else length - 1
		endPointIndex = if (plus) length - 1 else 0
		segmentTime = bundle.getFloat("segmentTime", segmentTime) ?: segmentTime
		speed = bundle.getFloat("speed", speed) ?: speed
		val turnTypeStr = bundle.getString("turnType", null)
		if (!KAlgorithms.isEmpty(turnTypeStr)) {
			val turn = TurnType.fromString(turnTypeStr, leftside)
			turn.isSkipToSpeak = bundle.getBoolean("skipTurn", false) ?: false
			turn.turnAngle = bundle.getFloat("turnAngle", 0f) ?: 0f
			turn.lanes = TurnType.lanesFromString(bundle.getString("turnLanes", null))
			turnType = turn
		}
		routeObject.id = (bundle.getLong("id", routeObject.id) ?: routeObject.id) shl 6 // OSM ID to OsmAnd ID
		routeObject.types = bundle.getIntArray("types", null)
		routeObject.pointTypes = bundle.getIntIntArray("pointTypes", null)
		routeObject.nameIds = bundle.getIntArray("names", null)
		val pointNames = bundle.getIntIntArray("pointNames", null)
		if (pointNames != null) {
			bundle.getResources().getPointNamesMap()[routeObject] = pointNames
		}

		val resources = bundle.getResources()
		val pointsX = IntArray(length)
		val pointsY = IntArray(length)
		var heights: FloatArray? = FloatArray(length * 2)
		routeObject.pointsX = pointsX
		routeObject.pointsY = pointsY
		routeObject.heightDistanceArray = heights
		var index = if (plus) 0 else length - 1
		var totalDistance = 0f
		var prevLocation: net.osmand.shared.data.KLocation? = null
		for (i in 0 until length) {
			val location = resources.getCurrentSegmentLocation(index)
			var dist = 0.0
			if (prevLocation != null) {
				dist = KMapUtils.getDistance(
					prevLocation.latitude, prevLocation.longitude,
					location.latitude, location.longitude
				)
				totalDistance += dist.toFloat()
			}
			prevLocation = location
			pointsX[i] = KMapUtils.get31TileNumberX(location.longitude)
			pointsY[i] = KMapUtils.get31TileNumberY(location.latitude)
			if (location.hasAltitude && heights != null && heights.isNotEmpty()) {
				heights[i * 2] = dist.toFloat()
				heights[i * 2 + 1] = location.altitude.toFloat()
			} else {
				heights = FloatArray(0)
				routeObject.heightDistanceArray = heights
			}
			if (plus) {
				index++
			} else {
				index--
			}
		}
		this.distance = totalDistance

		resources.updateNextSegmentStartLocation(length)
	}

	/** Heights along the part of the road this segment covers, in route direction. */
	fun getHeightValues(): FloatArray {
		val pf = routeObject.calculateHeightArray()
		if (pf.isEmpty()) {
			return FloatArray(0)
		}
		val reverse = startPointIndex > endPointIndex
		val st = min(startPointIndex, endPointIndex)
		var end = max(startPointIndex, endPointIndex)
		val res = FloatArray((end - st + 1) * 2)
		if (reverse) {
			for (k in 1..res.size / 2) {
				val ind = 2 * (end--)
				if (ind < pf.size && k < res.size / 2) {
					res[2 * k] = pf[ind]
				}
				if (ind < pf.size) {
					res[2 * (k - 1) + 1] = pf[ind + 1]
				}
			}
		} else {
			for (k in 0 until res.size / 2) {
				val ind = 2 * (st + k)
				if (k > 0 && ind < pf.size) {
					res[2 * k] = pf[ind]
				}
				if (ind < pf.size) {
					res[2 * k + 1] = pf[ind + 1]
				}
			}
		}
		return res
	}

	private fun updateCapacity() {
		val capacity = abs(endPointIndex - startPointIndex) + 1
		val old = attachedRoutes
		val updated = arrayOfNulls<MutableList<RouteSegmentResult>>(capacity)
		if (old != null) {
			old.copyInto(updated, 0, 0, min(old.size, updated.size))
		}
		attachedRoutes = updated
	}

	fun attachRoute(roadIndex: Int, r: RouteSegmentResult) {
		if (r.getObject().isRoadDeleted()) {
			return
		}
		val st = abs(roadIndex - startPointIndex)
		val attached = attachedRoutes!!
		if (attached[st] == null) {
			attached[st] = ArrayList()
		}
		attached[st]!!.add(r)
	}

	fun copyPreattachedRoutes(toCopy: RouteSegmentResult, shift: Int) {
		toCopy.preAttachedRoutes?.let {
			preAttachedRoutes = it.copyOfRange(shift, it.size)
		}
	}

	fun clearAttachedRoutes() {
		attachedRoutes = null
	}

	fun clearPreattachedRoutes() {
		preAttachedRoutes = null
	}

	fun getPreAttachedRoutes(routeInd: Int): Array<RouteSegmentResult>? {
		val st = abs(routeInd - startPointIndex)
		val routes = preAttachedRoutes
		if (routes != null && st < routes.size) {
			return routes[st]
		}
		return null
	}

	fun getAttachedRoutes(routeInd: Int): List<RouteSegmentResult> {
		val st = abs(routeInd - startPointIndex)
		return attachedRoutes!![st] ?: emptyList()
	}

	fun getTurnType(): TurnType? = turnType

	fun setTurnType(turnType: TurnType?) {
		this.turnType = turnType
	}

	fun getObject(): RouteDataObject = routeObject

	fun setObject(r: RouteDataObject) {
		this.routeObject = r
	}

	fun getSegmentTime(): Float = segmentTime

	fun getBearingBegin(): Float =
		getBearingBegin(startPointIndex, if (distance > 0 && distance < DIST_BEARING_DETECT) distance else DIST_BEARING_DETECT)

	fun getBearingBegin(point: Int, dist: Float): Float = getBearing(point, true, dist)

	fun getBearingEnd(): Float =
		getBearingEnd(endPointIndex, if (distance > 0 && distance < DIST_BEARING_DETECT) distance else DIST_BEARING_DETECT)

	fun getBearingEnd(point: Int, dist: Float): Float = getBearing(point, false, dist)

	fun getBearing(point: Int, begin: Boolean, dist: Float): Float {
		return if (begin) {
			(routeObject.directionRoute(point, startPointIndex < endPointIndex, dist) / PI * 180).toFloat()
		} else {
			val dr = routeObject.directionRoute(point, startPointIndex > endPointIndex, dist)
			(KMapUtils.alignAngleDifference(dr - PI) / PI * 180).toFloat()
		}
	}

	fun getDistance(point: Int, plus: Boolean): Float =
		(if (plus) routeObject.distance(point, endPointIndex) else routeObject.distance(startPointIndex, point)).toFloat()

	fun setSegmentTime(segmentTime: Float) {
		this.segmentTime = segmentTime
	}

	fun setRoutingTime(routingTime: Float) {
		this.routingTime = routingTime
	}

	fun getRoutingTime(): Float = routingTime

	fun getStartPoint(): KLatLon = convertPoint(routeObject, startPointIndex)

	fun getStartPointIndex(): Int = startPointIndex

	fun getStartPointX(): Int = routeObject.getPoint31XTile(startPointIndex)

	fun getStartPointY(): Int = routeObject.getPoint31YTile(startPointIndex)

	fun getEndPointIndex(): Int = endPointIndex

	fun getEndPointX(): Int = routeObject.getPoint31XTile(endPointIndex)

	fun getEndPointY(): Int = routeObject.getPoint31YTile(endPointIndex)

	fun getPoint(i: Int): KLatLon = convertPoint(routeObject, i)

	fun getEndPoint(): KLatLon = convertPoint(routeObject, endPointIndex)

	fun continuesBeyondRouteSegment(segment: RouteSegmentResult): Boolean {
		val commonX = routeObject.pointsX!![startPointIndex] == segment.routeObject.pointsX!![segment.endPointIndex]
		val commonY = routeObject.pointsY!![startPointIndex] == segment.routeObject.pointsY!![segment.endPointIndex]
		return commonX && commonY
	}

	fun isForwardDirection(): Boolean = endPointIndex - startPointIndex > 0

	private fun convertPoint(o: RouteDataObject, ind: Int): KLatLon = KLatLon(
		KMapUtils.get31LatitudeY(o.getPoint31YTile(ind)),
		KMapUtils.get31LongitudeX(o.getPoint31XTile(ind))
	)

	fun setSegmentSpeed(speed: Float) {
		this.speed = speed
	}

	fun setEndPointIndex(endPointIndex: Int) {
		this.endPointIndex = endPointIndex
		updateCapacity()
	}

	fun setStartPointIndex(startPointIndex: Int) {
		this.startPointIndex = startPointIndex
		updateCapacity()
	}

	fun getSegmentSpeed(): Float = speed

	fun getDistance(): Float = distance

	fun setDistance(distance: Float) {
		this.distance = distance
	}

	fun getDescription(full: Boolean): String {
		val description = this.description
		if (description == null || description.isEmpty()) {
			return ""
		}
		if (full && description.size > 1) {
			return description[1]
		}
		return description[0]
	}

	fun setDescription(shortD: String, full: String) {
		this.description = arrayOf(shortD, full)
	}

	fun clearDescription() {
		this.description = null
	}

	override fun toString(): String = "$routeObject: $startPointIndex-$endPointIndex"

	/**
	 * Where the road leads, as signed. Falls back to the segments that follow, because a short
	 * segment on a junction often carries no destination of its own.
	 */
	fun getDestinationName(
		lang: String?, transliterate: Boolean, list: List<RouteSegmentResult>,
		routeInd: Int, withRef: Boolean
	): String {
		val dnRef = getObject().getDestinationRef(lang, transliterate, isForwardDirection())
		var destinationName = getObject().getDestinationName(lang, transliterate, isForwardDirection())
		if (KAlgorithms.isEmpty(destinationName)) {
			// try to get destination name from following segments
			var distanceFromTurn = getDistance()
			var n = routeInd + 1
			while (n + 1 < list.size) {
				val s1 = list[n]
				if (s1.getTurnType() != null) {
					// avoid retrieve destination over other turns
					break
				}
				val s1DnRef = s1.getObject().getDestinationRef(lang, transliterate, isForwardDirection())
				val dnRefIsEqual = !KAlgorithms.isEmpty(s1DnRef) && !KAlgorithms.isEmpty(dnRef) && s1DnRef == dnRef
				val isMotorwayLink = "motorway_link" == s1.getObject().getHighway()
				if (distanceFromTurn < DIST_TO_SEEK_DEST && (isMotorwayLink || dnRefIsEqual) &&
					KAlgorithms.isEmpty(destinationName)
				) {
					destinationName = s1.getObject().getDestinationName(lang, transliterate, s1.isForwardDirection())
				}
				distanceFromTurn += s1.getDistance()
				if (distanceFromTurn > DIST_TO_SEEK_DEST || !KAlgorithms.isEmpty(destinationName)) {
					break
				}
				n++
			}
		}
		if (withRef) {
			if (!KAlgorithms.isEmpty(dnRef) && !KAlgorithms.isEmpty(destinationName)) {
				destinationName = "$dnRef, $destinationName"
			} else if (!KAlgorithms.isEmpty(dnRef) && KAlgorithms.isEmpty(destinationName)) {
				destinationName = dnRef!!
			}
		}
		return destinationName
	}

	/** The street the route follows, looking a little ahead when this segment has no name. */
	fun getStreetName(
		lang: String?, transliterate: Boolean, list: List<RouteSegmentResult>, routeInd: Int
	): String? {
		var streetName = getObject().getName(lang, transliterate)
		if (KAlgorithms.isEmpty(streetName)) {
			// try to get street name from following segments
			var distanceFromTurn = getDistance()
			var hasNewTurn = false
			var n = routeInd + 1
			while (n + 1 < list.size) {
				val s1 = list[n]
				if (s1.getTurnType() != null) {
					hasNewTurn = true
				}
				if (!hasNewTurn && distanceFromTurn < DIST_TO_SEEK_STREET_NAME && KAlgorithms.isEmpty(streetName)) {
					streetName = s1.getObject().getName(lang, transliterate)
				}
				distanceFromTurn += s1.getDistance()
				if (distanceFromTurn > DIST_TO_SEEK_STREET_NAME || !KAlgorithms.isEmpty(streetName)) {
					break
				}
				n++
			}
		}
		return streetName
	}

	fun getRef(lang: String?, transliterate: Boolean): String? =
		getObject().getRef(lang, transliterate, isForwardDirection())

	/** The road whose shield should be shown for this part of the route, if any. */
	fun getObjectWithShield(list: List<RouteSegmentResult>, routeInd: Int): RouteDataObject? {
		var rdo: RouteDataObject? = null
		var isNextShieldFound = getObject().hasNameTagStartsWith("road_ref")
		var ind = routeInd
		while (ind < list.size && !isNextShieldFound) {
			if (list[ind].getTurnType() != null) {
				isNextShieldFound = true
			} else {
				val obj = list[ind].getObject()
				if (obj.hasNameTagStartsWith("road_ref")) {
					rdo = obj
					isNextShieldFound = true
				}
			}
			ind++
		}
		return rdo
	}

	fun getGpxPointIndex(): Int = gpxPointIndex

	fun setGpxPointIndex(gpxPointIndex: Int) {
		this.gpxPointIndex = gpxPointIndex
	}

	fun hasExitInfo(): Boolean {
		if (routeObject.hasMotorwayJunctionNode()) {
			val nodeRef = routeObject.getNodeRef()
			val nodeName = routeObject.getNodeName()
			if (nodeRef != null || nodeName != null) {
				for (attached in getAttachedRoutes(getStartPointIndex())) {
					if (nodeRef != null && nodeRef == attached.getObject().getJunctionRef()) {
						return false
					}
					if (nodeName != null && nodeName == attached.getObject().getJunctionName()) {
						return false
					}
				}
				if (isLinkRoad()) {
					return true
				}
			}
		}
		return routeObject.getJunctionRef() != null || routeObject.getJunctionName() != null
	}

	private fun isLinkRoad(): Boolean {
		val highway = routeObject.getHighway()
		return highway != null && highway.endsWith("_link")
	}

	companion object {
		const val DIST_BEARING_DETECT = 10f
		const val DIST_BEARING_DETECT_UNMATCHED = 50f

		/**
		 * Evaluates street name that the route follows after turn within specified distance.
		 * It is useful to find names for short segments on intersections.
		 */
		private const val DIST_TO_SEEK_STREET_NAME = 150f

		/**
		 * Evaluates destination for exit from one road to another on the followed highway link
		 * within specified distance. In most cases using on "cloverleaf" junctions.
		 */
		private const val DIST_TO_SEEK_DEST = 1000f
	}
}
