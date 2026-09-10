package net.osmand.shared.routing

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KLock
import net.osmand.shared.util.synchronized
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KBitSet
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.floor

/**
 * One routing profile as `routing.xml` describes it: the attributes of the vehicle and the rules
 * that decide, for every road, whether it may be used, how fast it is taken and what its obstacles
 * and turns cost.
 *
 * A profile is parsed once into a root router, and [build] makes a cheap copy of it for one set of
 * parameters. The copy shares the parsed rule tables with the root and only re-filters the rules
 * the parameters select, which is why so many fields are handed over rather than copied.
 *
 * The C++ core reads the cached attribute fields and walks the rules of this class through JNI, so
 * the field names, and the names of the four nested classes, are part of its contract.
 */
class GeneralRouter : VehicleRouter {

	private val objectAttributes: Array<RouteAttributeContext>

	@JvmField
	val attributes: MutableMap<String, String>

	private val parameters: MutableMap<String, RoutingParameter>
	private val parameterValues: MutableMap<String, String>
	private val universalRules: MutableMap<String, Int>
	private val universalRulesById: MutableList<String>
	private val tagRuleMask: MutableMap<String, KBitSet>
	private val ruleToValue: MutableList<Any?>
	private var shortestRoute: Boolean = false
	private var heightObstacles: Boolean = false
	private var allowPrivate: Boolean = false
	private var filename: String? = null
	private var profileName: String = ""

	private val regionConvert: MutableMap<RouteRegion, MutableMap<Int, Int>> = LinkedHashMap()

	// cached values
	private var restrictionsAware = true
	private var sharpTurn = 0f
	private var shortWaySharpTurn = 0f
	private var slightTurn = 0f
	private var shortWaySlightTurn = 0f
	private var roundaboutTurn = 0f
	private var shortWayRoundaboutTurn = 0f

	/** speed in m/s */
	private var minSpeed = 0.28f

	/** speed in m/s */
	private var defaultSpeed = 1f

	/** speed in m/s */
	private var maxSpeed = 10f

	/** speed in m/s, used for the shortest route */
	private var maxVehicleSpeed = 0f

	private var impassableRoads: KTLongHashSet? = null

	private var profile: GeneralRouterProfile

	private var evalCache: Array<MutableMap<RouteRegion, MutableMap<IntHolder, Float>>>

	/** getFilteredTags() as flat Array (JNI) */
	@JvmField
	var hhNativeFilter: Array<String> = emptyArray()

	/** parameterValues as flat Array (JNI) */
	@JvmField
	var hhNativeParameterValues: Array<String> = emptyArray()

	private val root: GeneralRouter

	enum class RouteDataObjectAttribute(@JvmField val nm: String) {
		ROAD_SPEED("speed"),
		ROAD_PRIORITIES("priority"),
		DESTINATION_PRIORITIES("destination_priority"),
		ACCESS("access"),
		OBSTACLES("obstacle_time"),
		ROUTING_OBSTACLES("obstacle"),
		ONEWAY("oneway"),
		PENALTY_TRANSITION("penalty_transition"),
		OBSTACLE_SRTM_ALT_SPEED("obstacle_srtm_alt_speed"),
		AREA("area");

		companion object {
			@JvmStatic
			fun getValueOf(s: String): RouteDataObjectAttribute? {
				for (a in entries) {
					if (a.nm == s) {
						return a
					}
				}
				return null
			}
		}
	}

	enum class RoutingParameterType {
		NUMERIC,
		BOOLEAN,
		SYMBOLIC
	}

	/** A copy of [copy]'s root with [params] applied, sharing the parsed rule tables with it. */
	constructor(copy: GeneralRouter, params: MutableMap<String, String>) {
		this.root = copy.root
		val parent = root
		this.profile = parent.profile
		this.attributes = LinkedHashMap()

		for (next in parent.attributes.entries) {
			addAttribute(next.key, next.value)
		}
		// do not copy, keep linked
		universalRules = parent.universalRules
		universalRulesById = parent.universalRulesById
		parameterValues = params
		tagRuleMask = parent.tagRuleMask
		ruleToValue = parent.ruleToValue
		parameters = parent.parameters
		profileName = parent.profileName

		objectAttributes = Array(RouteDataObjectAttribute.entries.size) { i ->
			RouteAttributeContext(parent.objectAttributes[i], params)
		}
		shortestRoute = params.containsKey(USE_SHORTEST_WAY) && parseSilentBoolean(params[USE_SHORTEST_WAY], false)
		heightObstacles = params.containsKey(USE_HEIGHT_OBSTACLES) && parseSilentBoolean(params[USE_HEIGHT_OBSTACLES], false)

		allowPrivate = if (params.containsKey("profile_truck")) {
			params.containsKey(ALLOW_PRIVATE_FOR_TRUCK) && parseSilentBoolean(params[ALLOW_PRIVATE_FOR_TRUCK], false)
		} else {
			params.containsKey(ALLOW_PRIVATE) && parseSilentBoolean(params[ALLOW_PRIVATE], false)
		}
		if (params.containsKey(DEFAULT_SPEED)) {
			defaultSpeed = parseSilentFloat(params[DEFAULT_SPEED], defaultSpeed)
		}
		if (params.containsKey(MIN_SPEED)) {
			minSpeed = parseSilentFloat(params[MIN_SPEED], minSpeed)
		}
		if (params.containsKey(MAX_SPEED)) {
			maxSpeed = parseSilentFloat(params[MAX_SPEED], maxSpeed)
		}
		maxVehicleSpeed = maxSpeed
		if (shortestRoute) {
			if (profile == GeneralRouterProfile.BICYCLE) {
				maxSpeed = min(BICYCLE_SHORTEST_DEFAULT_SPEED, maxSpeed)
			} else if (profile == GeneralRouterProfile.CAR) {
				maxSpeed = min(CAR_SHORTEST_DEFAULT_SPEED, maxSpeed)
				heightObstacles = true
			}
		}
		evalCache = newCaches()
	}

	constructor(profile: GeneralRouterProfile, attributes: Map<String, String>) {
		this.root = this
		this.profile = profile
		this.attributes = LinkedHashMap()
		this.parameterValues = LinkedHashMap()
		for (next in attributes.entries) {
			addAttribute(next.key, next.value)
		}
		objectAttributes = Array(RouteDataObjectAttribute.entries.size) { RouteAttributeContext() }
		universalRules = LinkedHashMap()
		universalRulesById = ArrayList()
		tagRuleMask = LinkedHashMap()
		ruleToValue = ArrayList()
		parameters = LinkedHashMap()

		evalCache = newCaches()
	}

	private fun newCaches(): Array<MutableMap<RouteRegion, MutableMap<IntHolder, Float>>> =
		Array(RouteDataObjectAttribute.entries.size) { HashMap() }

	fun getFilename(): String? = filename

	fun setFilename(filename: String?) {
		this.filename = filename
	}

	fun getProfileName(): String = profileName

	fun setProfileName(profileName: String) {
		this.profileName = profileName
	}

	override fun getProfile(): GeneralRouterProfile = profile

	fun getHeightObstacles(): Boolean = heightObstacles

	fun getParameters(): MutableMap<String, RoutingParameter> = parameters

	fun getParameterValues(): MutableMap<String, String> = parameterValues

	fun serializeParameterValues(vls: Map<String, String>): List<String> {
		val ls = ArrayList<String>()
		for (e in vls.entries) {
			val value = e.value
			if (value.isEmpty() || "true" == value || "false" == value) {
				ls.add(e.key)
			} else {
				ls.add(e.key + "=" + value)
			}
		}
		return ls
	}

	fun addAttribute(k: String, v: String) {
		attributes[k] = v
		if (k == "restrictionsAware") {
			restrictionsAware = parseSilentBoolean(v, restrictionsAware)
		} else if (k == "sharpTurn" || k == "leftTurn") {
			sharpTurn = parseSilentFloat(v, sharpTurn)
		} else if (k == "slightTurn" || k == "rightTurn") {
			slightTurn = parseSilentFloat(v, slightTurn)
		} else if (k == "roundaboutTurn") {
			roundaboutTurn = parseSilentFloat(v, roundaboutTurn)
		} else if (k == "minDefaultSpeed" || k == "defaultSpeed") {
			defaultSpeed = parseSilentFloat(v, defaultSpeed * 3.6f) / 3.6f
		} else if (k == "minSpeed") {
			minSpeed = parseSilentFloat(v, minSpeed * 3.6f) / 3.6f
		} else if (k == "maxDefaultSpeed" || k == "maxSpeed") {
			maxSpeed = parseSilentFloat(v, maxSpeed * 3.6f) / 3.6f
		} else if (k == "shortWaySharpTurn") {
			shortWaySharpTurn = parseSilentFloat(v, shortWaySharpTurn)
		} else if (k == "shortWaySlightTurn") {
			shortWaySlightTurn = parseSilentFloat(v, shortWaySlightTurn)
		} else if (k == "shortWayRoundaboutTurn") {
			shortWayRoundaboutTurn = parseSilentFloat(v, shortWayRoundaboutTurn)
		}
	}

	fun getObjContext(a: RouteDataObjectAttribute): RouteAttributeContext = objectAttributes[a.ordinal]

	fun registerBooleanParameter(
		id: String, group: String?, name: String?, description: String?, profiles: Array<String>?,
		defaultBoolean: Boolean
	) {
		val rp = RoutingParameter()
		rp.id = id
		rp.group = group
		rp.name = name
		rp.description = description
		rp.profiles = profiles
		rp.type = RoutingParameterType.BOOLEAN
		rp.defaultBoolean = defaultBoolean
		parameters[rp.id!!] = rp
	}

	fun registerNumericParameter(
		id: String, name: String?, description: String?, profiles: Array<String>?, vls: Array<Any>?,
		vlsDescriptions: Array<String>?, defaultNumeric: Double
	) {
		val rp = RoutingParameter()
		rp.name = name
		rp.description = description
		rp.id = id
		rp.profiles = profiles
		rp.possibleValues = vls
		rp.possibleValueDescriptions = vlsDescriptions
		rp.type = RoutingParameterType.NUMERIC
		rp.defaultNumeric = defaultNumeric
		parameters[rp.id!!] = rp
	}

	override fun acceptLine(way: RouteDataObject): Boolean {
		var res = getCache(RouteDataObjectAttribute.ACCESS, way)
		if (res == null) {
			res = getObjContext(RouteDataObjectAttribute.ACCESS).evaluateInt(way, 0).toFloat()
			putCache(RouteDataObjectAttribute.ACCESS, way, res)
		}
		val impassable = impassableRoads
		if (impassable != null && impassable.contains(way.id shr IMPASSABLE_ROAD_SHIFT)) {
			return false
		}
		return res >= 0
	}

	fun isAllowPrivate(): Boolean = allowPrivate

	fun getImpassableRoadIds(): LongArray = impassableRoads?.toArray() ?: LongArray(0)

	fun registerTagValueAttribute(tag: String, value: String?): Int {
		val key = "$tag$$value"
		universalRules[key]?.let { return it }
		return registerSyncTagValue(this, tag, key)
	}

	private fun parseValue(value: String, type: String?): Any? {
		var vl = -1f
		val trimmed = value.trim()
		if ("speed" == type) {
			vl = RouteDataUtils.parseSpeed(trimmed, vl)
		} else if ("weight" == type) {
			vl = RouteDataObject.parseWeightInTon(trimmed, vl)
		} else if ("length" == type) {
			vl = RouteDataObject.parseLength(trimmed, vl)
		} else {
			val i = KAlgorithms.findFirstNumberEndIndex(trimmed)
			if (i > 0) {
				// could be negative
				return trimmed.substring(0, i).toFloat()
			}
		}
		if (vl == -1f) {
			return null
		}
		return vl
	}

	private fun parseValueFromTag(id: Int, type: String?): Any? {
		while (ruleToValue.size <= id) {
			ruleToValue.add(null)
		}
		var res = ruleToValue[id]
		if (res == null) {
			val v = universalRulesById[id]
			val value = v.substring(v.indexOf('$') + 1)
			res = parseValue(value, type)
			if (res == null) {
				res = ""
			}
			ruleToValue[id] = res
		}
		if ("" == res) {
			return null
		}
		return res
	}

	override fun build(params: Map<String, String>): GeneralRouter =
		GeneralRouter(this, if (params is MutableMap<String, String>) params else LinkedHashMap(params))

	override fun restrictionsAware(): Boolean = restrictionsAware

	override fun defineObstacle(road: RouteDataObject, point: Int, isBackwardDir: Boolean): Float {
		val pointTypes = road.getPointTypes(point) ?: return 0f
		var obst = getCache(RouteDataObjectAttribute.OBSTACLES, road.region!!, pointTypes, isBackwardDir)
		if (obst == null) {
			val filteredPointTypes = filterDirectionTags(road, pointTypes, isBackwardDir)
			obst = getObjContext(RouteDataObjectAttribute.OBSTACLES)
				.evaluateFloat(road.region!!, filteredPointTypes, 0f)
			putCache(RouteDataObjectAttribute.OBSTACLES, road.region!!, pointTypes, obst, isBackwardDir)
		}
		return obst
	}

	override fun defineRoutingObstacle(road: RouteDataObject, point: Int, isBackwardDir: Boolean): Float {
		val pointTypes = road.getPointTypes(point) ?: return 0f
		var obst = getCache(RouteDataObjectAttribute.ROUTING_OBSTACLES, road.region!!, pointTypes, isBackwardDir)
		if (obst == null) {
			val filteredPointTypes = filterDirectionTags(road, pointTypes, isBackwardDir)
			obst = getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES)
				.evaluateFloat(road.region!!, filteredPointTypes, 0f)
			putCache(RouteDataObjectAttribute.ROUTING_OBSTACLES, road.region!!, pointTypes, obst, isBackwardDir)
		}
		return obst
	}

	/** Drops the stop signs, traffic lights and height limits that only apply the other way. */
	private fun filterDirectionTags(road: RouteDataObject, pointTypes: IntArray, isBackwardDir: Boolean): IntArray {
		val wayDirection = if (isBackwardDir) 1 else -1
		var direction = 0
		var tdirection = 0
		var hdirection = 0
		val region = road.region!!
		for (type in pointTypes) {
			if (type == region.directionBackward) {
				direction = -1
			} else if (type == region.directionForward) {
				direction = 1
			} else if (type == region.directionTrafficSignalsBackward) {
				tdirection = -1
			} else if (type == region.directionTrafficSignalsForward) {
				tdirection = 1
			} else if (type == region.maxheightBackward) {
				hdirection = -1
			} else if (type == region.maxheightForward) {
				hdirection = 1
			}
		}
		if (direction != 0 || tdirection != 0 || hdirection != 0) {
			val filteredPointTypes = KTIntArrayList()
			for (type in pointTypes) {
				if (((type == region.stopSign || type == region.giveWaySign) && direction == wayDirection) ||
					(type == region.trafficSignals && tdirection == wayDirection) ||
					(hdirection == wayDirection)
				) {
					continue
				}
				filteredPointTypes.add(type)
			}
			return filteredPointTypes.toArray()
		}
		return pointTypes
	}

	override fun defineHeightObstacle(road: RouteDataObject, startIndex: Short, endIndex: Short): Double {
		if (!heightObstacles) {
			return 0.0
		}
		val heightArray = road.calculateHeightArray()
		if (heightArray.isEmpty()) {
			return 0.0
		}

		var sum = 0.0
		var knext: Int
		val objContext = getObjContext(RouteDataObjectAttribute.OBSTACLE_SRTM_ALT_SPEED)
		var k = startIndex.toInt()
		while (k != endIndex.toInt()) {
			knext = if (startIndex < endIndex) k + 1 else k - 1
			val dist = if (startIndex < endIndex) heightArray[2 * knext].toDouble() else heightArray[2 * k].toDouble()
			val diff = (heightArray[2 * knext + 1] - heightArray[2 * k + 1]).toDouble()
			if (diff != 0.0 && dist > 0) {
				val incl = abs(diff / max(dist, MIN_DISTANCE_SLOPE_ROUND))
				var percentIncl = (incl * 100).toInt()
				percentIncl = (percentIncl + 2) / 3 * 3 - 2 // 1, 4, 7, 10, .
				if (percentIncl >= 1) {
					objContext.paramContext?.incline = if (diff > 0) percentIncl.toDouble() else -percentIncl.toDouble()
					sum += objContext.evaluateFloat(road, 0f) * (if (diff > 0) diff else -diff)
				}
			}
			k = knext
		}
		return sum
	}

	override fun isOneWay(road: RouteDataObject): Int {
		var res = getCache(RouteDataObjectAttribute.ONEWAY, road)
		if (res == null) {
			res = getObjContext(RouteDataObjectAttribute.ONEWAY).evaluateInt(road, 0).toFloat()
			putCache(RouteDataObjectAttribute.ONEWAY, road, res)
		}
		return res.toInt()
	}

	override fun isArea(obj: RouteDataObject): Boolean =
		getObjContext(RouteDataObjectAttribute.AREA).evaluateInt(obj, 0) == 1

	override fun getPenaltyTransition(road: RouteDataObject): Float {
		var vl = getCache(RouteDataObjectAttribute.PENALTY_TRANSITION, road)
		if (vl == null) {
			vl = getObjContext(RouteDataObjectAttribute.PENALTY_TRANSITION).evaluateInt(road, 0).toFloat()
			putCache(RouteDataObjectAttribute.PENALTY_TRANSITION, road, vl)
		}
		return vl
	}

	override fun defineRoutingSpeed(road: RouteDataObject, dir: Boolean): Float {
		var definedSpd = getCache(RouteDataObjectAttribute.ROAD_SPEED, road, dir)
		if (definedSpd == null) {
			// not implemented direction usage
			val spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed)
			definedSpd = max(min(spd, maxSpeed), minSpeed)
			putCache(RouteDataObjectAttribute.ROAD_SPEED, road, definedSpd, dir)
		}
		return definedSpd
	}

	override fun defineVehicleSpeed(road: RouteDataObject, dir: Boolean): Float {
		// don't use cache cause max/min is different for routing speed
		if (maxVehicleSpeed != maxSpeed) {
			// not implemented direction usage
			val spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed)
			return max(min(spd, maxVehicleSpeed), minSpeed)
		}
		var sp = getCache(RouteDataObjectAttribute.ROAD_SPEED, road, dir)
		if (sp == null) {
			// not implemented direction usage
			val spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed)
			sp = max(min(spd, maxVehicleSpeed), minSpeed)
			putCache(RouteDataObjectAttribute.ROAD_SPEED, road, sp, dir)
		}
		return sp
	}

	override fun defineSpeedPriority(road: RouteDataObject, dir: Boolean): Float {
		var sp = getCache(RouteDataObjectAttribute.ROAD_PRIORITIES, road, dir)
		if (sp == null) {
			// not implemented direction usage
			sp = getObjContext(RouteDataObjectAttribute.ROAD_PRIORITIES).evaluateFloat(road, 1f)
			putCache(RouteDataObjectAttribute.ROAD_PRIORITIES, road, sp, dir)
		}
		return sp
	}

	override fun defineDestinationPriority(road: RouteDataObject): Float {
		var sp = getCache(RouteDataObjectAttribute.DESTINATION_PRIORITIES, road)
		if (sp == null) {
			sp = getObjContext(RouteDataObjectAttribute.DESTINATION_PRIORITIES).evaluateFloat(road, 1f)
			putCache(RouteDataObjectAttribute.DESTINATION_PRIORITIES, road, sp, false)
		}
		return sp
	}

	private fun putCache(attr: RouteDataObjectAttribute, road: RouteDataObject, value: Float) {
		putCache(attr, road.region!!, road.types!!, value, false)
	}

	private fun putCache(attr: RouteDataObjectAttribute, road: RouteDataObject, value: Float, extra: Boolean) {
		putCache(attr, road.region!!, road.types!!, value, extra)
	}

	private fun putCache(
		attr: RouteDataObjectAttribute, reg: RouteRegion, types: IntArray, value: Float, extra: Boolean
	) {
		val ch = evalCache[attr.ordinal]
		if (USE_CACHE) {
			var rM = ch[reg]
			if (rM == null) {
				rM = HashMap()
				ch[reg] = rM
			}
			rM[IntHolder(types, extra)] = value
		}
	}

	/** The tag set of a road, as the key of the evaluation cache. */
	class IntHolder(private val array: IntArray, private val extra: Boolean) {

		override fun hashCode(): Int = array.contentHashCode() + (if (extra) 1 else 0)

		override fun equals(other: Any?): Boolean {
			if (this === other) {
				return true
			}
			if (other !is IntHolder) {
				return false
			}
			if (other.extra != this.extra) {
				return false
			}
			return array.contentEquals(other.array)
		}
	}

	private fun getCache(attr: RouteDataObjectAttribute, road: RouteDataObject): Float? =
		getCache(attr, road.region!!, road.types!!, false)

	private fun getCache(attr: RouteDataObjectAttribute, road: RouteDataObject, extra: Boolean): Float? =
		getCache(attr, road.region!!, road.types!!, extra)

	private fun getCache(
		attr: RouteDataObjectAttribute, reg: RouteRegion, types: IntArray, extra: Boolean
	): Float? {
		val ch = evalCache[attr.ordinal]
		if (USE_CACHE) {
			val rM = ch[reg] ?: return null
			return rM[IntHolder(types, extra)]
		}
		return null
	}

	override fun getDefaultSpeed(): Float = defaultSpeed

	override fun getMinSpeed(): Float = minSpeed

	override fun getMaxSpeed(): Float = maxSpeed

	private fun getSharpTurnPenalty(): Float = if (shortestRoute) shortWaySharpTurn else sharpTurn

	private fun getSlightTurnPenalty(): Float = if (shortestRoute) shortWaySlightTurn else slightTurn

	private fun getRoundaboutTurnPenalty(): Float = if (shortestRoute) shortWayRoundaboutTurn else roundaboutTurn

	override fun calculateTurnTime(segment: RoadTraversal, prev: RoadTraversal): Double {
		val ts = getPenaltyTransition(segment.getRoad())
		val prevTs = getPenaltyTransition(prev.getRoad())
		var totalPenalty = 0f
		if (prevTs != ts) {
			totalPenalty += abs(ts - prevTs) / 2
		}

		val currentRoundAbout = segment.getRoad().roundabout()
		val previousRoundAbout = prev.getRoad().roundabout()

		if ((currentRoundAbout && !previousRoundAbout) || (!currentRoundAbout && previousRoundAbout)) {
			val rt = getRoundaboutTurnPenalty() / 2
			if (rt > 0) {
				totalPenalty += rt
			}
		} else if (getSharpTurnPenalty() > 0 || getSlightTurnPenalty() > 0) {
			val a1 = segment.getRoad().directionRoute(segment.getSegmentStart().toInt(), segment.isPositive())
			val a2 = prev.getRoad().directionRoute(prev.getSegmentEnd().toInt(), !prev.isPositive())
			val diff = abs(KMapUtils.alignAngleDifference(a1 - a2 - PI))
			if (diff > PI / 1.5) {
				totalPenalty += getSharpTurnPenalty() // >120 degree (U-turn)
			} else if (diff > PI / 3) {
				totalPenalty += getSlightTurnPenalty() // >60 degree (standard)
			} else if (diff > PI / 6) {
				totalPenalty += getSlightTurnPenalty() / 2 // >30 degree (light)
			}
		}

		return totalPenalty.toDouble()
	}

	override fun containsAttribute(attribute: String): Boolean = attributes.containsKey(attribute)

	override fun getAttribute(attribute: String): String? = attributes[attribute]

	fun getFloatAttribute(attribute: String, v: Float): Float = parseSilentFloat(getAttribute(attribute), v)

	fun getIntAttribute(attribute: String, v: Int): Int = parseSilentFloat(getAttribute(attribute), v.toFloat()).toInt()

	/** One parameter a profile exposes, as `routing.xml` declared it. */
	class RoutingParameter {
		internal var id: String? = null
		internal var group: String? = null
		internal var name: String? = null
		internal var description: String? = null
		internal var type: RoutingParameterType? = null
		internal var possibleValues: Array<Any>? = null
		internal var possibleValueDescriptions: Array<String>? = null
		internal var profiles: Array<String>? = null
		internal var defaultBoolean: Boolean = false
		internal var defaultNumeric: Double = 0.0

		fun getId(): String? = id

		fun getGroup(): String? = group

		fun getName(): String? = name

		fun getDescription(): String? = description

		fun getType(): RoutingParameterType? = type

		fun getPossibleValueDescriptions(): Array<String>? = possibleValueDescriptions

		fun getPossibleValues(): Array<Any>? = possibleValues

		fun getDefaultBoolean(): Boolean = defaultBoolean

		fun getDefaultNumeric(): Double = defaultNumeric

		/** Also the default of the preference this parameter is stored in, so the text matters. */
		fun getDefaultString(): String =
			if (type == RoutingParameterType.NUMERIC) formatOneDecimal(defaultNumeric) else "-"

		fun getProfiles(): Array<String>? = profiles
	}

	internal class ParameterContext {
		var vars: Map<String, String>? = null
		var incline: Double = 0.0
	}

	/** The rules of one attribute, already filtered down to the parameters in force. */
	inner class RouteAttributeContext {

		internal val rules: MutableList<RouteAttributeEvalRule> = ArrayList()
		internal var paramContext: ParameterContext? = null
		private val evalLock = KLock()

		constructor()

		constructor(original: RouteAttributeContext, params: Map<String, String>?) {
			if (params != null) {
				val context = ParameterContext()
				context.vars = params
				paramContext = context
			}
			for (rt in original.rules) {
				if (checkParameter(rt)) {
					rules.add(rt)
				}
			}
		}

		fun getRules(): Array<RouteAttributeEvalRule> = rules.toTypedArray()

		fun getParamKeys(): Array<String> = paramContext?.vars?.keys?.toTypedArray() ?: emptyArray()

		fun getParamValues(): Array<String> = paramContext?.vars?.values?.toTypedArray() ?: emptyArray()

		private fun evaluate(ro: RouteDataObject): Any? = evaluate(convert(ro.region!!, ro.types!!))

		fun printRules(out: StringBuilder) {
			for (r in rules) {
				r.printRule(out)
			}
		}

		fun registerNewRule(selectValue: String, selectType: String?): RouteAttributeEvalRule {
			val ev = RouteAttributeEvalRule()
			ev.registerSelectValue(selectValue, selectType)
			rules.add(ev)
			return ev
		}

		fun getLastRule(): RouteAttributeEvalRule = rules[rules.size - 1]

		/**
		 * The rules are shared with the root router and hold scratch state while they evaluate,
		 * so only one thread may be inside them at a time. This was `synchronized` in Java.
		 */
		private fun evaluate(types: KBitSet): Any? = synchronized(evalLock) {
			for (k in rules.indices) {
				val r = rules[k]
				val o = r.eval(types, paramContext)
				if (o != null) {
					return@synchronized o
				}
			}
			null
		}

		private fun checkParameter(r: RouteAttributeEvalRule): Boolean {
			val context = paramContext
			if (context != null && r.parameters.size > 0) {
				for (parameter in r.parameters) {
					var p = parameter
					var not = false
					if (p.startsWith("-")) {
						not = true
						p = p.substring(1)
					}
					val value = context.vars?.containsKey(p) == true
					if (not && value) {
						return false
					} else if (!not && !value) {
						return false
					}
				}
			}
			return true
		}

		fun evaluateInt(ro: RouteDataObject, defValue: Int): Int {
			val o = evaluate(ro)
			if (o !is Number) {
				return defValue
			}
			return o.toInt()
		}

		fun evaluateInt(region: RouteRegion, types: IntArray, defValue: Int): Int {
			val o = evaluate(convert(region, types))
			if (o !is Number) {
				return defValue
			}
			return o.toInt()
		}

		fun evaluateInt(rawTypes: KBitSet, defValue: Int): Int {
			val o = evaluate(rawTypes)
			if (o !is Number) {
				return defValue
			}
			return o.toInt()
		}

		fun evaluateFloat(ro: RouteDataObject, defValue: Float): Float {
			val o = evaluate(ro)
			if (o !is Number) {
				return defValue
			}
			return o.toFloat()
		}

		fun evaluateFloat(region: RouteRegion, types: IntArray, defValue: Float): Float {
			val o = evaluate(convert(region, types))
			if (o !is Number) {
				return defValue
			}
			return o.toFloat()
		}

		fun evaluateFloat(rawTypes: KBitSet, defValue: Float): Float {
			val o = evaluate(rawTypes)
			if (o !is Number) {
				return defValue
			}
			return o.toFloat()
		}

		/** Translates the tag ids of one region into the router's own universal rule ids. */
		private fun convert(reg: RouteRegion, types: IntArray): KBitSet {
			val b = KBitSet(universalRules.size)
			var map = regionConvert[reg]
			if (map == null) {
				map = HashMap()
				regionConvert[reg] = map
			}
			for (k in types.indices) {
				var nid = map[types[k]]
				if (nid == null) {
					val r = reg.quickGetEncodingRule(types[k])!!
					nid = registerTagValueAttribute(r.getTag(), r.getValue())
					map[types[k]] = nid
				}
				b.set(nid)
			}
			return b
		}
	}

	/** A comparison or a min/max over two values of a rule, as `routing.xml` wrote it. */
	class RouteAttributeExpression(
		private val router: GeneralRouter,
		vs: Array<String>,
		valueType: String?,
		expressionId: Int
	) {

		// definition
		private val values: Array<String> = vs
		private val expressionType: Int = expressionId
		private val valueType: String? = valueType

		// numbers
		private val cacheValues: Array<Number?>

		init {
			if (vs.size < 2) {
				throw IllegalStateException("Expression should have at least 2 arguments")
			}
			cacheValues = arrayOfNulls(vs.size)
			for (i in vs.indices) {
				if (!vs[i].startsWith("$") && !vs[i].startsWith(":")) {
					val o = router.parseValue(vs[i], valueType)
					if (o is Number) {
						cacheValues[i] = o
					}
				}
			}
		}

		internal fun matches(types: KBitSet, paramContext: ParameterContext?): Boolean {
			val f1 = calculateExprValue(0, types, paramContext)
			val f2 = calculateExprValue(1, types, paramContext)
			if (f1.isNaN() || f2.isNaN()) {
				return false
			}
			if (expressionType == GREAT_EXPRESSION) {
				return f1 > f2
			} else if (expressionType == GREAT_OR_EQUAL_EXPRESSION) {
				return f1 >= f2
			} else if (expressionType == LESS_EXPRESSION) {
				return f1 < f2
			} else if (expressionType == LESS_OR_EQUAL_EXPRESSION) {
				return f1 <= f2
			} else if (expressionType == EQUAL_EXPRESSION) {
				return f1 == f2
			}
			return false
		}

		internal fun calculateExprValue(types: KBitSet, paramContext: ParameterContext?): Double? {
			val f1 = calculateExprValue(0, types, paramContext)
			val f2 = calculateExprValue(1, types, paramContext)
			if (!f1.isNaN() && !f2.isNaN()) {
				when (expressionType) {
					MIN_EXPRESSION -> return min(f1, f2)
					MAX_EXPRESSION -> return max(f1, f2)
				}
			}
			return null
		}

		private fun calculateExprValue(id: Int, types: KBitSet, paramContext: ParameterContext?): Double {
			val value = values[id]
			val cacheValue = cacheValues[id]
			if (cacheValue != null) {
				return cacheValue.toDouble()
			}
			var o: Any? = null
			if (value.startsWith("$")) {
				val mask = router.tagRuleMask[value.substring(1)]
				if (mask != null && mask.intersects(types)) {
					val findBit = KBitSet(mask.length())
					findBit.or(mask)
					findBit.and(types)
					val v = findBit.nextSetBit(0)
					o = router.parseValueFromTag(v, valueType)
				}
			} else if (value == ":incline") {
				return paramContext!!.incline
			} else if (value.startsWith(":")) {
				val p = value.substring(1)
				val vars = paramContext?.vars
				if (vars != null && vars.containsKey(p)) {
					o = router.parseValue(vars[p]!!, valueType)
				}
			}

			if (o is Number) {
				return o.toDouble()
			}
			return Double.NaN
		}

		companion object {
			const val LESS_EXPRESSION = 1
			const val GREAT_EXPRESSION = 2
			const val EQUAL_EXPRESSION = 3
			const val MIN_EXPRESSION = 4
			const val MAX_EXPRESSION = 5
			const val GREAT_OR_EQUAL_EXPRESSION = 6
			const val LESS_OR_EQUAL_EXPRESSION = 7
		}
	}

	/** One `select` of a routing attribute: what it yields, and the conditions it yields under. */
	inner class RouteAttributeEvalRule {

		internal val parameters: MutableList<String> = ArrayList()
		internal val tagValueCondDefTag: MutableList<String> = ArrayList()
		internal val tagValueCondDefValue: MutableList<String?> = ArrayList()
		internal val tagValueCondDefNot: MutableList<Boolean> = ArrayList()

		internal var selectValueDef: String? = null
		internal var selectValue: Any? = null
		internal var selectType: String? = null
		internal var selectExpression: RouteAttributeExpression? = null
		internal val filterTypes = KBitSet()
		internal val filterNotTypes = KBitSet()
		internal val evalFilterTypes = KBitSet()

		internal val onlyTags: MutableSet<String> = LinkedHashSet()
		internal val onlyNotTags: MutableSet<String> = LinkedHashSet()
		internal val conditionExpressions: MutableList<RouteAttributeExpression> = ArrayList()

		fun getExpressions(): Array<RouteAttributeExpression> = conditionExpressions.toTypedArray()

		fun getParameters(): Array<String> = parameters.toTypedArray()

		fun getTagValueCondDefTag(): Array<String> = tagValueCondDefTag.toTypedArray()

		fun getTagValueCondDefValue(): Array<String?> = tagValueCondDefValue.toTypedArray()

		fun getTagValueCondDefNot(): BooleanArray = BooleanArray(tagValueCondDefNot.size) { tagValueCondDefNot[it] }

		fun registerSelectValue(value: String, type: String?) {
			selectType = type
			selectValueDef = value
			if (value.startsWith(":") || value.startsWith("$")) {
				selectValue = value
			} else {
				selectValue = parseValue(value, type)
				if (selectValue == null) {
					LOG.error("Routing.xml select value '$value' was not registered")
				}
			}
		}

		fun printRule(out: StringBuilder) {
			out.append(" Select ").append(selectValue).append(" if ")
			for (k in 0 until filterTypes.length()) {
				if (filterTypes.get(k)) {
					out.append(universalRulesById[k]).append(" ")
				}
			}
			if (filterNotTypes.length() > 0) {
				out.append(" ifnot ")
			}
			for (k in 0 until filterNotTypes.length()) {
				if (filterNotTypes.get(k)) {
					out.append(universalRulesById[k]).append(" ")
				}
			}
			for (k in parameters.indices) {
				out.append(" param=").append(parameters[k])
			}
			if (onlyTags.size > 0) {
				out.append(" match tag = ").append(onlyTags)
			}
			if (onlyNotTags.size > 0) {
				out.append(" not match tag = ").append(onlyNotTags)
			}
			if (conditionExpressions.size > 0) {
				out.append(" subexpressions ").append(conditionExpressions.size).append('\n')
			}
			selectExpression?.let { out.append("  selectexpression ").append(it).append('\n') }
			out.append('\n')
		}

		fun registerAndTagValueCondition(tag: String, value: String?, not: Boolean) {
			tagValueCondDefTag.add(tag)
			tagValueCondDefValue.add(value)
			tagValueCondDefNot.add(not)
			if (value == null) {
				if (not) {
					onlyNotTags.add(tag)
				} else {
					onlyTags.add(tag)
				}
			} else {
				val vtype = registerTagValueAttribute(tag, value)
				if (not) {
					filterNotTypes.set(vtype)
				} else {
					filterTypes.set(vtype)
				}
			}
		}

		fun registerLessCondition(value1: String, value2: String, valueType: String?) {
			conditionExpressions.add(
				RouteAttributeExpression(
					this@GeneralRouter, arrayOf(value1, value2), valueType,
					RouteAttributeExpression.LESS_EXPRESSION
				)
			)
		}

		fun registerGreatCondition(value1: String, value2: String, valueType: String?) {
			conditionExpressions.add(
				RouteAttributeExpression(
					this@GeneralRouter, arrayOf(value1, value2), valueType,
					RouteAttributeExpression.GREAT_EXPRESSION
				)
			)
		}

		fun registerGreatOrEqualCondition(value1: String, value2: String, valueType: String?) {
			conditionExpressions.add(
				RouteAttributeExpression(
					this@GeneralRouter, arrayOf(value1, value2), valueType,
					RouteAttributeExpression.GREAT_OR_EQUAL_EXPRESSION
				)
			)
		}

		fun registerLessOrEqualCondition(value1: String, value2: String, valueType: String?) {
			conditionExpressions.add(
				RouteAttributeExpression(
					this@GeneralRouter, arrayOf(value1, value2), valueType,
					RouteAttributeExpression.LESS_OR_EQUAL_EXPRESSION
				)
			)
		}

		fun registerEqualCondition(value1: String, value2: String, valueType: String?) {
			conditionExpressions.add(
				RouteAttributeExpression(
					this@GeneralRouter, arrayOf(value1, value2), valueType,
					RouteAttributeExpression.EQUAL_EXPRESSION
				)
			)
		}

		fun registerMinExpression(value1: String, value2: String, valueType: String?) {
			selectExpression = RouteAttributeExpression(
				this@GeneralRouter, arrayOf(value1, value2), valueType,
				RouteAttributeExpression.MIN_EXPRESSION
			)
		}

		fun registerMaxExpression(value1: String, value2: String, valueType: String?) {
			selectExpression = RouteAttributeExpression(
				this@GeneralRouter, arrayOf(value1, value2), valueType,
				RouteAttributeExpression.MAX_EXPRESSION
			)
		}

		fun registerAndParamCondition(param: String, not: Boolean) {
			parameters.add(if (not) "-$param" else param)
		}

		internal fun eval(types: KBitSet, paramContext: ParameterContext?): Any? {
			if (matches(types, paramContext)) {
				return calcSelectValue(types, paramContext)
			}
			return null
		}

		internal fun calcSelectValue(types: KBitSet, paramContext: ParameterContext?): Any? {
			val expression = selectExpression
			val value = selectValue
			if (expression != null) {
				selectValue = expression.calculateExprValue(types, paramContext)
			} else if (value is String && value.startsWith("$")) {
				val mask = tagRuleMask[value.substring(1)]
				if (mask != null && mask.intersects(types)) {
					val findBit = KBitSet(mask.length())
					findBit.or(mask)
					findBit.and(types)
					return parseValueFromTag(findBit.nextSetBit(0), selectType)
				}
			} else if (value is String && value.startsWith(":")) {
				val p = value.substring(1)
				val vars = paramContext?.vars
				selectValue = if (vars != null && vars.containsKey(p)) {
					parseValue(vars[p]!!, selectType)
				} else {
					return null
				}
			}
			return selectValue
		}

		internal fun matches(types: KBitSet, paramContext: ParameterContext?): Boolean {
			if (!checkAllTypesShouldBePresent(types)) {
				return false
			}
			if (!checkAllTypesShouldNotBePresent(types)) {
				return false
			}
			if (!checkFreeTags(types)) {
				return false
			}
			if (!checkNotFreeTags(types)) {
				return false
			}
			if (!checkExpressions(types, paramContext)) {
				return false
			}
			return true
		}

		private fun checkExpressions(types: KBitSet, paramContext: ParameterContext?): Boolean {
			for (e in conditionExpressions) {
				if (!e.matches(types, paramContext)) {
					return false
				}
			}
			return true
		}

		private fun checkFreeTags(types: KBitSet): Boolean {
			for (ts in onlyTags) {
				val b = tagRuleMask[ts]
				if (b == null || !b.intersects(types)) {
					return false
				}
			}
			return true
		}

		private fun checkNotFreeTags(types: KBitSet): Boolean {
			for (ts in onlyNotTags) {
				val b = tagRuleMask[ts]
				if (b != null && b.intersects(types)) {
					return false
				}
			}
			return true
		}

		private fun checkAllTypesShouldNotBePresent(types: KBitSet): Boolean = !filterNotTypes.intersects(types)

		private fun checkAllTypesShouldBePresent(types: KBitSet): Boolean {
			// Bitset method subset is missing "filterTypes.isSubset(types)"
			// reset previous evaluation
			// evalFilterTypes.clear(); // not needed same as or()
			evalFilterTypes.or(filterTypes)
			// evaluate bit intersection and check if filterTypes contained as set in types
			evalFilterTypes.and(types)
			return evalFilterTypes == filterTypes
		}
	}

	fun clearCaches() {
		for (cache in evalCache) {
			cache.clear()
		}
	}

	fun printRules(out: StringBuilder) {
		for (i in RouteDataObjectAttribute.entries.indices) {
			out.append(RouteDataObjectAttribute.entries[i]).append('\n')
			objectAttributes[i].printRules(out)
		}
	}

	fun setImpassableRoads(impassableRoads: Set<Long>?) {
		if (impassableRoads != null && impassableRoads.isNotEmpty()) {
			val set = KTLongHashSet(impassableRoads.size)
			for (id in impassableRoads) {
				set.add(id)
			}
			this.impassableRoads = set
		} else {
			this.impassableRoads?.clear()
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("GeneralRouter")

		private const val CAR_SHORTEST_DEFAULT_SPEED = 55 / 3.6f
		private const val BICYCLE_SHORTEST_DEFAULT_SPEED = 15 / 3.6f

		@JvmField
		var IMPASSABLE_ROAD_SHIFT = 0 // 6 is better

		const val USE_SHORTEST_WAY = "short_way"
		const val USE_HEIGHT_OBSTACLES = "height_obstacles"
		const val GROUP_RELIEF_SMOOTHNESS_FACTOR = "relief_smoothness_factor"
		const val AVOID_FERRIES = "avoid_ferries"
		const val AVOID_TOLL = "avoid_toll"
		const val AVOID_MOTORWAY = "avoid_motorway"
		const val AVOID_UNPAVED = "avoid_unpaved"
		const val PREFER_MOTORWAYS = "prefer_motorway"
		const val ALLOW_PRIVATE = "allow_private"
		const val ALLOW_PRIVATE_FOR_TRUCK = "allow_private_for_truck"
		const val HAZMAT_CATEGORY = "hazmat_category"
		const val GOODS_RESTRICTIONS = "goods_restrictions"
		const val ALLOW_MOTORWAYS = "allow_motorway"
		const val DEFAULT_SPEED = "default_speed"
		const val MIN_SPEED = "min_speed"
		const val MAX_SPEED = "max_speed"
		const val VEHICLE_HEIGHT = "height"
		const val VEHICLE_WEIGHT = "weight"
		const val VEHICLE_WIDTH = "width"
		const val VEHICLE_LENGTH = "length"
		const val MOTOR_TYPE = "motor_type"
		const val MAX_AXLE_LOAD = "maxaxleload"
		const val WEIGHT_RATING = "weightrating"
		const val ALLOW_VIA_FERRATA = "allow_via_ferrata"
		const val CHECK_ALLOW_PRIVATE_NEEDED = "check_allow_private_needed"

		private const val MIN_DISTANCE_SLOPE_ROUND = 10.0

		private const val USE_CACHE = true

		@JvmField
		var TIMER: Long = 0

		/** The parent copy shares the rule tables, so this has to lock across all instances. */
		private val registerLock = KLock()

		private fun registerSyncTagValue(r: GeneralRouter, tag: String, key: String): Int =
			synchronized(registerLock) {
				val id = r.universalRules.size
				r.universalRulesById.add(key)
				r.universalRules[key] = id
				if (!r.tagRuleMask.containsKey(tag)) {
					r.tagRuleMask[tag] = KBitSet()
				}
				r.tagRuleMask[tag]!!.set(id)
				id
			}

		private fun parseSilentBoolean(t: String?, v: Boolean): Boolean {
			if (t.isNullOrEmpty()) {
				return v
			}
			return t.equals("true", ignoreCase = true)
		}

		private fun parseSilentFloat(t: String?, v: Float): Float {
			if (t.isNullOrEmpty()) {
				return v
			}
			return t.toFloat()
		}

		/**
		 * The text `String.format(Locale.US, "%.1f", value)` produced: exactly one fraction digit,
		 * a dot, half up rounding. It is stored as a preference default, so it has to stay the same.
		 */
		internal fun formatOneDecimal(value: Double): String {
			if (value.isNaN()) {
				return "NaN"
			}
			if (value.isInfinite()) {
				return if (value > 0) "Infinity" else "-Infinity"
			}
			if (abs(value) >= 1e15) {
				// far outside anything routing.xml carries, and past what the scaling below holds
				return value.toString()
			}
			val negative = value < 0 || (value == 0.0 && 1.0 / value < 0)
			// String.format rounds half up, kotlin.math.round rounds half to even
			val units = floor(abs(value) * 10.0 + 0.5).toLong()
			return (if (negative) "-" else "") + (units / 10) + "." + (units % 10)
		}
	}
}
