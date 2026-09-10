package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KTransliterationHelper
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * One road as it is stored in an obf file: its geometry, its tag indices into the encoding rules of
 * the [region] it was read from, and the turn restrictions attached to it.
 *
 * The arrays are shared between copies of the same road and are meant to be read only. The C++ core
 * fills them in directly through JNI, which is why they are plain fields with these exact names.
 */
class RouteDataObject {

	@JvmField
	val region: RouteRegion

	// all these arrays supposed to be immutable!
	// These fields accessible from C++
	@JvmField
	var types: IntArray? = null

	@JvmField
	var pointsX: IntArray? = null

	@JvmField
	var pointsY: IntArray? = null

	@JvmField
	var restrictions: LongArray? = null

	@JvmField
	var restrictionsVia: LongArray? = null

	@JvmField
	var pointTypes: Array<IntArray?>? = null

	@JvmField
	var pointNames: Array<Array<String>?>? = null

	@JvmField
	var pointNameTypes: Array<IntArray?>? = null

	@JvmField
	var id: Long = 0

	@JvmField
	var names: KTIntObjectMap<String>? = null

	@JvmField
	var nameIds: IntArray? = null

	/** Mixed array [0, height, distance, height, ...], twice the length of the geometry. */
	@JvmField
	var heightDistanceArray: FloatArray? = null

	@JvmField
	var heightByCurrentLocation: Float = Float.NaN

	constructor(region: RouteRegion) {
		this.region = region
	}

	constructor(region: RouteRegion, nameIds: IntArray, nameValues: Array<String>) {
		this.region = region
		this.nameIds = nameIds
		if (nameIds.isNotEmpty()) {
			names = KTIntObjectMap()
		}
		for (i in nameIds.indices) {
			names!!.put(nameIds[i], nameValues[i])
		}
	}

	constructor(copy: RouteDataObject) {
		this.region = copy.region
		this.pointsX = copy.pointsX
		this.pointsY = copy.pointsY
		this.types = copy.types
		this.names = copy.names
		this.nameIds = copy.nameIds
		this.restrictions = copy.restrictions
		this.restrictionsVia = copy.restrictionsVia
		this.pointTypes = copy.pointTypes
		this.pointNames = copy.pointNames
		this.pointNameTypes = copy.pointNameTypes
		this.id = copy.id
	}

	/** The rule [id] stands for. Roads only carry ids their own region defines. */
	private fun rule(id: Int): RouteTypeRule = region.quickGetEncodingRule(id)!!

	/** True when the two roads carry the same geometry, tags and restrictions. */
	fun compareRoute(thatObj: RouteDataObject): Boolean {
		if (id != thatObj.id ||
			!pointsX.contentEquals(thatObj.pointsX) ||
			!pointsY.contentEquals(thatObj.pointsY)
		) {
			return false
		}
		var equals = restrictions.contentEquals(thatObj.restrictions) &&
				restrictionsVia.contentEquals(thatObj.restrictionsVia)

		val types = this.types
		val thatTypes = thatObj.types
		if (equals) {
			if (types == null || thatTypes == null) {
				equals = types === thatTypes
			} else if (types.size != thatTypes.size) {
				equals = false
			} else {
				var i = 0
				while (i < types.size && equals) {
					val thisRule = rule(types[i])
					val thatRule = thatObj.rule(thatTypes[i])
					equals = thisRule.getTag() == thatRule.getTag() &&
							thisRule.getValue() == thatRule.getValue()
					i++
				}
			}
		}

		val nameIds = this.nameIds
		val thatNameIds = thatObj.nameIds
		if (equals) {
			if (nameIds == null || thatNameIds == null) {
				equals = nameIds === thatNameIds
			} else if (nameIds.size != thatNameIds.size) {
				equals = false
			} else {
				var i = 0
				while (i < nameIds.size && equals) {
					equals = KAlgorithms.stringsEqual(rule(nameIds[i]).getTag(), thatObj.rule(thatNameIds[i]).getTag()) &&
							KAlgorithms.stringsEqual(names?.get(nameIds[i]), thatObj.names?.get(thatNameIds[i]))
					i++
				}
			}
		}

		val pointTypes = this.pointTypes
		val thatPointTypes = thatObj.pointTypes
		if (equals) {
			if (pointTypes == null || thatPointTypes == null) {
				equals = pointTypes === thatPointTypes
			} else if (pointTypes.size != thatPointTypes.size) {
				equals = false
			} else {
				var i = 0
				while (i < pointTypes.size && equals) {
					val point = pointTypes[i]
					val thatPoint = thatPointTypes[i]
					if (point == null || thatPoint == null) {
						equals = point === thatPoint
					} else if (point.size != thatPoint.size) {
						equals = false
					} else {
						var j = 0
						while (j < point.size && equals) {
							val thisRule = rule(point[j])
							val thatRule = thatObj.rule(thatPoint[j])
							equals = KAlgorithms.stringsEqual(thisRule.getTag(), thatRule.getTag()) &&
									KAlgorithms.stringsEqual(thisRule.getValue(), thatRule.getValue())
							j++
						}
					}
					i++
				}
			}
		}

		val pointNameTypes = this.pointNameTypes
		val thatPointNameTypes = thatObj.pointNameTypes
		if (equals) {
			if (pointNameTypes == null || thatPointNameTypes == null) {
				equals = pointNameTypes === thatPointNameTypes
			} else if (pointNameTypes.size != thatPointNameTypes.size) {
				equals = false
			} else {
				var i = 0
				while (i < pointNameTypes.size && equals) {
					val point = pointNameTypes[i]
					val thatPoint = thatPointNameTypes[i]
					if (point == null || thatPoint == null) {
						equals = point === thatPoint
					} else if (point.size != thatPoint.size) {
						equals = false
					} else {
						var j = 0
						while (j < point.size && equals) {
							equals = KAlgorithms.stringsEqual(rule(point[j]).getTag(), thatObj.rule(thatPoint[j]).getTag()) &&
									KAlgorithms.stringsEqual(pointNames?.get(i)?.get(j), thatObj.pointNames?.get(i)?.get(j))
							j++
						}
					}
					i++
				}
			}
		}
		return equals
	}

	/**
	 * Heights along the road, as [distance from the previous point, height] pairs, interpolated
	 * over the points that carry no height of their own. Empty when the road has no heights.
	 *
	 * When [currentLocation] is given, [heightByCurrentLocation] is left holding the height of the
	 * point nearest to it.
	 */
	@JvmOverloads
	fun calculateHeightArray(currentLocation: KLatLon? = null): FloatArray {
		heightDistanceArray?.let { return it }

		val startHeight = KAlgorithms.parseIntSilently(getValue("osmand_ele_start"), HEIGHT_UNDEFINED)
		val endHeight = KAlgorithms.parseIntSilently(getValue("osmand_ele_end"), startHeight)
		if (startHeight == HEIGHT_UNDEFINED) {
			val empty = FloatArray(0)
			heightDistanceArray = empty
			return empty
		}

		val heights = FloatArray(2 * getPointsLength())
		heightDistanceArray = heights
		var plon = 0.0
		var plat = 0.0
		var prevHeight = startHeight.toFloat()
		heightByCurrentLocation = Float.NaN
		var prevDistance = 0.0
		for (k in 0 until getPointsLength()) {
			val lon = KMapUtils.get31LongitudeX(getPoint31XTile(k))
			val lat = KMapUtils.get31LatitudeY(getPoint31YTile(k))
			if (k > 0) {
				val dd = KMapUtils.getDistance(plat, plon, lat, lon)
				var height = HEIGHT_UNDEFINED.toFloat()
				if (k == getPointsLength() - 1) {
					height = endHeight.toFloat()
				} else {
					val asc = getValue(k, "osmand_ele_asc")
					if (!asc.isNullOrEmpty()) {
						height = prevHeight + asc.toFloat()
					} else {
						val desc = getValue(k, "osmand_ele_desc")
						if (!desc.isNullOrEmpty()) {
							height = prevHeight - desc.toFloat()
						}
					}
				}
				heights[2 * k] = dd.toFloat()
				heights[2 * k + 1] = height

				if (currentLocation != null) {
					val distance = KMapUtils.getDistance(currentLocation, lat, lon)
					if (height != HEIGHT_UNDEFINED.toFloat() && distance < prevDistance) {
						prevDistance = distance
						heightByCurrentLocation = height
					}
				}

				if (height != HEIGHT_UNDEFINED.toFloat()) {
					// interpolate undefined
					var totalDistance = dd
					var startUndefined = k
					while (startUndefined - 1 >= 0 && heights[2 * (startUndefined - 1) + 1] == HEIGHT_UNDEFINED.toFloat()) {
						startUndefined--
						totalDistance += heights[2 * startUndefined]
					}
					if (totalDistance > 0) {
						val angle = (height - prevHeight) / totalDistance
						for (j in startUndefined until k) {
							heights[2 * j + 1] = ((heights[2 * j] * angle) + heights[2 * j - 1]).toFloat()
						}
					}
					prevHeight = height
				}
			} else {
				heights[0] = 0f
				heights[1] = startHeight.toFloat()
			}
			plat = lat
			plon = lon
			if (currentLocation != null) {
				prevDistance = KMapUtils.getDistance(currentLocation, plat, plon)
			}
		}
		return heights
	}

	fun getId(): Long = id

	fun getName(): String? = names?.get(region.nameTypeRule)

	@JvmOverloads
	fun getName(lang: String?, transliterate: Boolean = false): String? {
		val names = this.names ?: return null
		if (KAlgorithms.isEmpty(lang)) {
			return names[region.nameTypeRule]
		}
		for (k in names.keys()) {
			if (region.routeEncodingRules.size > k && "name:$lang" == region.routeEncodingRules[k]?.getTag()) {
				return names[k]
			}
		}
		val nmDef = names[region.nameTypeRule]
		if (transliterate && !nmDef.isNullOrEmpty()) {
			return KTransliterationHelper.transliterate(nmDef)
		}
		return nmDef
	}

	fun getNameIds(): IntArray? = nameIds

	fun getNames(): KTIntObjectMap<String>? = names

	fun getRef(lang: String?, transliterate: Boolean, direction: Boolean): String? {
		val names = this.names ?: return null
		if (KAlgorithms.isEmpty(lang)) {
			return names[region.refTypeRule]
		}
		for (k in names.keys()) {
			if (region.routeEncodingRules.size > k && "ref:$lang" == region.routeEncodingRules[k]?.getTag()) {
				return names[k]
			}
		}
		val refDefault = names[region.refTypeRule]
		if (transliterate && !refDefault.isNullOrEmpty()) {
			return KTransliterationHelper.transliterate(refDefault)
		}
		return refDefault
	}

	fun getDestinationRef(lang: String?, transliterate: Boolean, direction: Boolean): String? {
		val names = this.names ?: return null
		val refTag = if (direction) "destination:ref:forward" else "destination:ref:backward"
		val refTagDefault = "destination:ref"
		var refDefault: String? = null

		for (k in names.keys()) {
			if (region.routeEncodingRules.size > k) {
				val tag = region.routeEncodingRules[k]?.getTag()
				if (refTag == tag) {
					return names[k]?.let { KAlgorithms.splitAndClearRepeats(it, ";") }
				}
				if (refTagDefault == tag) {
					refDefault = names[k]
				}
			}
		}
		if (refDefault != null) {
			return KAlgorithms.splitAndClearRepeats(refDefault, ";")
		}
		return null
	}

	fun getDestinationName(lang: String?, transliterate: Boolean, direction: Boolean): String {
		val names = this.names ?: return ""
		val tagPriorities = HashMap<String, Int>()
		var tagPriority = 1
		if (!KAlgorithms.isEmpty(lang)) {
			tagPriorities["destination:lang:$lang" + (if (direction) ":forward" else ":backward")] = tagPriority++
		}
		tagPriorities["destination:" + (if (direction) "forward" else "backward")] = tagPriority++
		if (!KAlgorithms.isEmpty(lang)) {
			tagPriorities["destination:lang:$lang"] = tagPriority++
		}
		tagPriorities["destination"] = tagPriority

		var highestPriorityNameKey = -1
		var highestPriority = Int.MAX_VALUE
		for (nameKey in names.keys()) {
			if (region.routeEncodingRules.size > nameKey) {
				val tag = region.routeEncodingRules[nameKey]?.getTag()
				val priority = tagPriorities[tag]
				if (priority != null && priority < highestPriority) {
					highestPriority = priority
					highestPriorityNameKey = nameKey
				}
			}
		}
		if (highestPriorityNameKey > 0) {
			val name = names[highestPriorityNameKey] ?: return ""
			return if (transliterate) KTransliterationHelper.transliterate(name) else name
		}
		return ""
	}

	fun getPoint31XTile(i: Int): Int = pointsX!![i]

	/** Midpoint of the two points, kept in 31 tile units to avoid an overflow of their sum. */
	fun getPoint31XTile(s: Int, e: Int): Int = pointsX!![s] / 2 + pointsX!![e] / 2

	fun getPoint31YTile(i: Int): Int = pointsY!![i]

	fun getPoint31YTile(s: Int, e: Int): Int = pointsY!![s] / 2 + pointsY!![e] / 2

	fun getPointsLength(): Int = pointsX!!.size

	fun getRestrictionLength(): Int = restrictions?.size ?: 0

	fun getRestrictionType(i: Int): Int = (restrictions!![i] and RESTRICTION_MASK.toLong()).toInt()

	fun getRestrictionInfo(k: Int): RestrictionInfo {
		val ri = RestrictionInfo()
		ri.toWay = getRestrictionId(k)
		ri.type = getRestrictionType(k)
		val via = restrictionsVia
		if (via != null && k < via.size) {
			ri.viaWay = via[k]
		}
		return ri
	}

	fun getRestrictionVia(i: Int): Long {
		val via = restrictionsVia
		if (via != null && via.size > i) {
			return via[i]
		}
		return 0
	}

	fun getRestrictionId(i: Int): Long = restrictions!![i] shr RESTRICTION_SHIFT

	fun hasPointTypes(): Boolean = pointTypes != null

	fun hasPointNames(): Boolean = pointNames != null

	/** Adds a point at [pos], shifting everything after it one place along. */
	fun insert(pos: Int, x31: Int, y31: Int) {
		val opointsX = pointsX!!
		val opointsY = pointsY!!
		val opointTypes = pointTypes
		val opointNames = pointNames
		val opointNameTypes = pointNameTypes
		val npointsX = IntArray(opointsX.size + 1)
		val npointsY = IntArray(opointsY.size + 1)
		pointsX = npointsX
		pointsY = npointsY
		val insTypes = opointTypes != null && opointTypes.size > pos
		val insNames = opointNames != null && opointNames.size > pos
		var npointTypes: Array<IntArray?>? = null
		var npointNames: Array<Array<String>?>? = null
		var npointNameTypes: Array<IntArray?>? = null
		if (insTypes) {
			npointTypes = arrayOfNulls(opointTypes!!.size + 1)
			pointTypes = npointTypes
		}
		if (insNames) {
			npointNames = arrayOfNulls(opointNames!!.size + 1)
			npointNameTypes = arrayOfNulls(opointNameTypes!!.size + 1)
			pointNames = npointNames
			pointNameTypes = npointNameTypes
		}
		var i = 0
		while (i < pos) {
			npointsX[i] = opointsX[i]
			npointsY[i] = opointsY[i]
			if (insTypes) {
				npointTypes!![i] = opointTypes!![i]
			}
			if (insNames) {
				npointNames!![i] = opointNames!![i]
				npointNameTypes!![i] = opointNameTypes!![i]
			}
			i++
		}
		npointsX[i] = x31
		npointsY[i] = y31
		if (insTypes) {
			npointTypes!![i] = null
		}
		if (insNames) {
			npointNames!![i] = null
			npointNameTypes!![i] = null
		}
		i++
		while (i < npointsX.size) {
			npointsX[i] = opointsX[i - 1]
			npointsY[i] = opointsY[i - 1]
			if (insTypes && i < npointTypes!!.size) {
				npointTypes[i] = opointTypes!![i - 1]
			}
			if (insNames && i < npointNames!!.size) {
				npointNames[i] = opointNames!![i - 1]
			}
			if (insNames && i < npointNameTypes!!.size) {
				npointNameTypes[i] = opointNameTypes!![i - 1]
			}
			i++
		}
	}

	fun getPointNames(ind: Int): Array<String>? {
		val pointNames = this.pointNames
		if (pointNames == null || ind >= pointNames.size) {
			return null
		}
		return pointNames[ind]
	}

	fun getPointNameTypes(ind: Int): IntArray? {
		val pointNameTypes = this.pointNameTypes
		if (pointNameTypes == null || ind >= pointNameTypes.size) {
			return null
		}
		return pointNameTypes[ind]
	}

	fun getPointTypes(ind: Int): IntArray? {
		val pointTypes = this.pointTypes
		if (pointTypes == null || ind >= pointTypes.size) {
			return null
		}
		return pointTypes[ind]
	}

	fun removePointType(ind: Int, type: Int) {
		val pointTypes = this.pointTypes ?: return
		val typesArr = pointTypes[ind] ?: return
		for (i in typesArr.indices) {
			if (typesArr[i] == type) {
				val result = IntArray(typesArr.size - 1)
				typesArr.copyInto(result, 0, 0, i)
				if (typesArr.size != i) {
					typesArr.copyInto(result, i, i + 1, typesArr.size)
					pointTypes[ind] = result
					break
				}
			}
		}
	}

	fun getTypes(): IntArray? = types

	@JvmOverloads
	fun getMaximumSpeed(direction: Boolean, profile: Int = RouteTypeRule.PROFILE_NONE): Float {
		var maxSpeed = 0f
		var maxProfileSpeed = 0f
		for (type in types!!) {
			val r = rule(type)
			val forwardDirection = r.isForward() > 0
			if (forwardDirection == direction || r.isForward() == 0) {
				// priority over default
				val plain = r.maxSpeed(RouteTypeRule.PROFILE_NONE)
				if (plain > 0) {
					maxSpeed = plain
				}
				val forProfile = r.maxSpeed(profile)
				if (forProfile > 0) {
					maxProfileSpeed = forProfile
				}
			}
		}
		return if (maxProfileSpeed > 0) maxProfileSpeed else maxSpeed
	}

	fun platform(): Boolean {
		for (type in types!!) {
			val r = rule(type)
			if (r.getTag() == "railway" && r.getValue() == "platform") {
				return true
			}
			if (r.getTag() == "public_transport" && r.getValue() == "platform") {
				return true
			}
		}
		return false
	}

	fun roundabout(): Boolean {
		for (type in types!!) {
			if (rule(type).roundabout()) {
				return true
			}
		}
		return false
	}

	fun isClockwise(leftSide: Boolean): Boolean {
		val pointTypes = this.pointTypes
		if (pointTypes != null) {
			for (tt in pointTypes) {
				if (tt == null) {
					continue
				}
				for (t in tt) {
					val r = rule(t)
					if (r.getTag() == "direction") {
						if (r.getValue() == "clockwise") {
							return true
						}
						if (r.getValue() == "anticlockwise") {
							return false
						}
					}
				}
			}
		}
		return leftSide
	}

	fun tunnel(): Boolean {
		for (type in types!!) {
			val r = rule(type)
			if (r.getTag() == "tunnel" && r.getValue() == "yes") {
				return true
			}
			if (r.getTag() == "layer" && r.getValue() == "-1") {
				return true
			}
		}
		return false
	}

	fun hasMotorwayJunctionNode(): Boolean {
		val pointTypes = this.pointTypes ?: return false
		for (point in pointTypes) {
			if (point != null) {
				for (t in point) {
					if (region.routeEncodingRules[t]?.getValue() == "motorway_junction") {
						return true
					}
				}
			}
		}
		return false
	}

	fun getJunctionRef(): String? = getValue(JUNCTION_REF)

	fun getJunctionName(): String? = getValue(JUNCTION_NAME)

	fun getNodeRef(): String? = getPointNameByTypeRule(region.refTypeRule)

	fun getNodeName(): String? = getPointNameByTypeRule(region.nameTypeRule)

	fun getExitRef(): String? = getJunctionRef() ?: getNodeRef()

	fun getExitName(): String? = getJunctionName() ?: getNodeName()

	fun hasTrafficLightAt(i: Int): Boolean {
		val pointTypes = getPointTypes(i) ?: return false
		for (pointType in pointTypes) {
			if (region.routeEncodingRules[pointType]?.getValue()?.startsWith("traffic_signals") == true) {
				return true
			}
		}
		return false
	}

	fun getOneway(): Int {
		for (type in types!!) {
			val r = rule(type)
			if (r.onewayDirection() != 0) {
				return r.onewayDirection()
			} else if (r.roundabout()) {
				return 1
			}
		}
		return 0
	}

	fun getRoute(): String? {
		for (type in types!!) {
			val r = rule(type)
			if ("route" == r.getTag()) {
				return r.getValue()
			}
		}
		return null
	}

	fun getHighway(): String? = getHighway(types!!, region)

	fun hasPrivateAccess(profile: GeneralRouterProfile): Boolean {
		for (type in types!!) {
			val rule = rule(type)
			val tag = rule.getTag()
			if (rule.getValue() == "private") {
				if ("vehicle" == tag || "access" == tag) {
					return true
				} else if (profile == GeneralRouterProfile.CAR) {
					return "motorcar" == tag || "motor_vehicle" == tag
				} else if (profile == GeneralRouterProfile.BICYCLE) {
					return "bicycle" == tag
				}
			}
		}
		return false
	}

	/** Value of [tag] on the road itself, looked up in its types and then in its names. */
	fun getValue(tag: String): String? {
		for (type in types!!) {
			val r = rule(type)
			if (r.getTag() == tag) {
				return r.getValue()
			}
		}
		val nameIds = this.nameIds
		if (nameIds != null) {
			for (nameId in nameIds) {
				if (rule(nameId).getTag() == tag) {
					return names?.get(nameId)
				}
			}
		}
		return null
	}

	/** Value of [tag] on point [pnt] of the road. */
	fun getValue(pnt: Int, tag: String): String? {
		val pointTypes = this.pointTypes
		if (pointTypes != null && pnt < pointTypes.size) {
			val point = pointTypes[pnt]
			if (point != null) {
				for (type in point) {
					val r = rule(type)
					if (r.getTag() == tag) {
						return r.getValue()
					}
				}
			}
		}
		val pointNameTypes = this.pointNameTypes
		if (pointNameTypes != null && pnt < pointNameTypes.size) {
			val point = pointNameTypes[pnt]
			if (point != null) {
				for (i in point.indices) {
					if (rule(point[i]).getTag() == tag) {
						return pointNames?.get(pnt)?.get(i)
					}
				}
			}
		}
		return null
	}

	fun getLanes(): Int {
		for (type in types!!) {
			val ln = rule(type).lanes()
			if (ln > 0) {
				return ln
			}
		}
		return -1
	}

	fun directionRoute(startPoint: Int, plus: Boolean): Double {
		// same goes to C++
		// Victor : the problem to put more than 5 meters that BinaryRoutePlanner will treat
		// 2 consequent Turn Right as UT and here 2 points will have same turn angle
		// So it should be fix in both places
		return directionRoute(startPoint, plus, 5f)
	}

	/**
	 * True when a heading of [bearing] degrees points along the road rather than against it.
	 * A fix that carries no bearing, passed as null, reads as the forward direction.
	 */
	fun bearingVsRouteDirection(bearing: Float?): Boolean {
		if (bearing == null) {
			return true
		}
		val diff = KMapUtils.alignAngleDifference(directionRoute(0, true) - bearing / 180f * PI)
		return abs(diff) < PI / 2f
	}

	fun isRoadDeleted(): Boolean {
		for (type in types!!) {
			val r = rule(type)
			if ("osmand_change" == r.getTag() && "delete" == r.getValue()) {
				return true
			}
		}
		return false
	}

	fun isDirectionApplicable(direction: Boolean, ind: Int, startPointInd: Int, endPointInd: Int): Boolean {
		val pt = getPointTypes(ind)!!
		for (type in pt) {
			val r = rule(type)
			// Evaluate direction tag if present
			if (r.getTag() == "direction") {
				val dv = r.getValue()
				if ((dv == "forward" && direction) || (dv == "backward" && !direction)) {
					return true
				} else if ((dv == "forward" && !direction) || (dv == "backward" && direction)) {
					return false
				}
			}
		}
		if (startPointInd >= 0) {
			// Heuristic fallback: Distance analysis for STOP with no recognized directional tagging:
			// Mask STOPs closer to the start than to the end of the routing segment if it is within 50m of start,
			// but do not mask STOPs mapped directly on start/end (likely intersection node)
			val d2Start = distance(startPointInd, ind)
			val d2End = distance(ind, endPointInd)
			if (d2Start < d2End && d2Start != 0.0 && d2End != 0.0 && d2Start < 50) {
				return false
			}
		}
		// No directional info detected
		return true
	}

	fun distance(startPoint: Int, endPoint: Int): Double {
		var start = startPoint
		var end = endPoint
		if (start > end) {
			val k = end
			end = start
			start = k
		}
		var d = 0.0
		var k = start
		while (k < end && k < getPointsLength() - 1) {
			val x = getPoint31XTile(k)
			val y = getPoint31YTile(k)
			val kx = getPoint31XTile(k + 1)
			val ky = getPoint31YTile(k + 1)
			d += KMapUtils.squareRootDist31(kx, ky, x, y)
			k++
		}
		return d
	}

	/** Route direction of EAST degrees from NORTH ]-PI, PI]. */
	fun directionRoute(startPoint: Int, plus: Boolean, dist: Float): Double {
		val x = getPoint31XTile(startPoint)
		val y = getPoint31YTile(startPoint)
		var nx = startPoint
		var px = x
		var py = y
		var total = 0.0
		do {
			if (plus) {
				nx++
				if (nx >= getPointsLength()) {
					break
				}
			} else {
				nx--
				if (nx < 0) {
					break
				}
			}
			px = getPoint31XTile(nx)
			py = getPoint31YTile(nx)
			// translate into meters
			total += KMapUtils.squareRootDist31(x, y, px, py)
		} while (total < dist)
		return -atan2((x - px).toDouble(), (y - py).toDouble())
	}

	fun coordinates(): String {
		val b = StringBuilder()
		b.append(" lat/lon : ")
		for (i in 0 until getPointsLength()) {
			val x = KMapUtils.get31LongitudeX(getPoint31XTile(i)).toFloat()
			val y = KMapUtils.get31LatitudeY(getPoint31YTile(i)).toFloat()
			b.append(y).append(" / ").append(x).append(" , ")
		}
		return b.toString()
	}

	override fun toString(): String {
		var str = "Road (${id / 64})"
		val rf = getRef("", false, true)
		if (!KAlgorithms.isEmpty(rf)) {
			str += ", ref ('$rf')"
		}
		val name = getName()
		if (!KAlgorithms.isEmpty(name)) {
			str += ", name ('$name')"
		}
		return str
	}

	fun hasNameTagStartsWith(tagStartsWith: String): Boolean {
		val nameIds = this.nameIds ?: return false
		for (nameId in nameIds) {
			val rtr = region.quickGetEncodingRule(nameId)
			if (rtr != null && rtr.getTag().startsWith(tagStartsWith)) {
				return true
			}
		}
		return false
	}

	class RestrictionInfo {
		@JvmField
		var type: Int = 0

		@JvmField
		var toWay: Long = 0

		@JvmField
		var viaWay: Long = 0

		/** Optional, to simulate a linked list. */
		@JvmField
		var next: RestrictionInfo? = null

		fun length(): Int {
			val next = this.next ?: return 1
			return next.length() + 1
		}
	}

	fun setRestriction(k: Int, to: Long, type: Int, viaWay: Long) {
		var restrictions = this.restrictions
		if (restrictions == null) {
			restrictions = LongArray(k + 1)
			this.restrictions = restrictions
		}
		if (restrictions.size <= k) {
			restrictions = restrictions.copyOf(k + 1)
			this.restrictions = restrictions
		}
		restrictions[k] = (to shl RESTRICTION_SHIFT) or (type.toLong() and RESTRICTION_MASK.toLong())
		if (viaWay != 0L) {
			setRestrictionVia(k, viaWay)
		}
	}

	fun setRestrictionVia(k: Int, viaWay: Long) {
		// note: the copy below reads `restrictions`, not `restrictionsVia`. That looks wrong, but it
		// is what the java original did and changing it would change which turns get restricted.
		val via = restrictionsVia
		val target: LongArray
		if (via != null) {
			val restrictions = this.restrictions!!
			target = LongArray(maxOf(k + 1, restrictions.size))
			restrictions.copyInto(target, 0, 0, restrictions.size)
		} else {
			target = LongArray(k + 1)
		}
		restrictionsVia = target
		target[k] = viaWay
	}

	fun setPointNames(pntInd: Int, array: IntArray, nms: Array<String>) {
		val pointNameTypes = this.pointNameTypes
		if (pointNameTypes == null || pointNameTypes.size <= pntInd) {
			val npointTypes = arrayOfNulls<IntArray>(pntInd + 1)
			val npointNames = arrayOfNulls<Array<String>>(pntInd + 1)
			val pointNames = this.pointNames
			var k = 0
			while (pointNameTypes != null && k < pointNameTypes.size) {
				npointTypes[k] = pointNameTypes[k]
				npointNames[k] = pointNames!![k]
				k++
			}
			this.pointNameTypes = npointTypes
			this.pointNames = npointNames
		}
		this.pointNameTypes!![pntInd] = array
		this.pointNames!![pntInd] = nms
	}

	fun setPointTypes(pntInd: Int, array: IntArray) {
		val pointTypes = this.pointTypes
		if (pointTypes == null || pointTypes.size <= pntInd) {
			val npointTypes = arrayOfNulls<IntArray>(pntInd + 1)
			var k = 0
			while (pointTypes != null && k < pointTypes.size) {
				npointTypes[k] = pointTypes[k]
				k++
			}
			this.pointTypes = npointTypes
		}
		this.pointTypes!![pntInd] = array
	}

	fun hasPointType(pntId: Int, type: Int): Boolean {
		val point = (pointTypes ?: return false)[pntId] ?: return false
		for (t in point) {
			if (t == type) {
				return true
			}
		}
		return false
	}

	fun containsType(cachedType: Int): Boolean {
		if (cachedType != -1) {
			for (type in types!!) {
				if (type == cachedType) {
					return true
				}
			}
		}
		return false
	}

	private fun getPointNameByTypeRule(typeRule: Int): String? {
		val pointNames = this.pointNames
		val pointNameTypes = this.pointNameTypes
		if (typeRule != -1 && pointNames != null && pointNameTypes != null) {
			for (i in pointNames.indices) {
				val point = pointNames[i] ?: continue
				for (j in point.indices) {
					if (pointNameTypes[i]!![j] == typeRule) {
						return point[j]
					}
				}
			}
		}
		return null
	}

	companion object {
		// Turn restriction types, as getRestrictionType answers them. MapRenderingTypes in
		// OsmAnd-java declares the same seven for the rendering and the map creator; the two sets
		// merge when that class moves, and until then these are the ones routing uses.
		const val RESTRICTION_NO_RIGHT_TURN = 1
		const val RESTRICTION_NO_LEFT_TURN = 2
		const val RESTRICTION_NO_U_TURN = 3
		const val RESTRICTION_NO_STRAIGHT_ON = 4
		const val RESTRICTION_ONLY_RIGHT_TURN = 5
		const val RESTRICTION_ONLY_LEFT_TURN = 6
		const val RESTRICTION_ONLY_STRAIGHT_ON = 7

		private const val RESTRICTION_SHIFT = 3
		private const val RESTRICTION_MASK = 7

		const val HEIGHT_UNDEFINED = -80000

		const val NONE_MAX_SPEED = RouteDataUtils.NONE_MAX_SPEED

		const val JUNCTION_REF = "junction:ref"
		const val JUNCTION_NAME = "junction:name"

		@JvmStatic
		fun parseSpeed(v: String, def: Float): Float = RouteDataUtils.parseSpeed(v, def)

		/** Parses an osm length, which may be metric, or feet and inches like `14'10"`. */
		@JvmStatic
		fun parseLength(v: String, def: Float): Float {
			var f = 0f
			// 14'10" 14 - inches, 10 feet
			val i = KAlgorithms.findFirstNumberEndIndex(v)
			if (i > 0) {
				f += v.substring(0, i).toFloat()
				var pref = v.substring(i).trim()
				var add = 0f
				for (ik in pref.indices) {
					if (KAlgorithms.isDigit(pref[ik]) || pref[ik] == '.' || pref[ik] == '-') {
						val first = KAlgorithms.findFirstNumberEndIndex(pref.substring(ik))
						if (first != -1) {
							add = parseLength(pref.substring(ik), 0f)
							pref = pref.substring(0, ik)
						}
						break
					}
				}
				if (pref.contains("km")) {
					f *= 1000f
				}
				if (pref.contains("\"") || pref.contains("in")) {
					f = (f * 0.0254).toFloat()
				} else if (pref.contains("'") || pref.contains("ft") || pref.contains("feet")) {
					// foot to meters
					f = (f * 0.3048).toFloat()
				} else if (pref.contains("cm")) {
					f = (f * 0.01).toFloat()
				} else if (pref.contains("mile")) {
					f *= 1609.34f
				}
				return f + add
			}
			return def
		}

		@JvmStatic
		fun parseWeightInTon(v: String, def: Float): Float {
			val i = KAlgorithms.findFirstNumberEndIndex(v)
			if (i > 0) {
				var f = v.substring(0, i).toFloat()
				if (v.contains("\"") || v.contains("lbs")) {
					// lbs -> kg -> ton
					f = (f * 0.4535f) / 1000f
				}
				return f
			}
			return def
		}

		@JvmStatic
		fun getHighway(types: IntArray, region: RouteRegion): String? {
			for (type in types) {
				val highway = region.quickGetEncodingRule(type)!!.highwayRoad()
				if (highway != null) {
					return highway
				}
			}
			return null
		}
	}
}
