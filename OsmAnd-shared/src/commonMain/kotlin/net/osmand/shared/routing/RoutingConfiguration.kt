package net.osmand.shared.routing

import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.KQuadTree
import net.osmand.shared.io.KFile
import net.osmand.shared.routing.GeneralRouter.RouteAttributeContext
import net.osmand.shared.routing.GeneralRouter.RouteDataObjectAttribute
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.xml.XmlPullParser
import okio.Source
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Everything one route calculation is configured with: which profile to route on, how much memory
 * the tile cache may take, and the tweaks that come out of `routing.xml`.
 *
 * A configuration is built, not edited: [Builder] holds what the XML declared and hands out a fresh
 * configuration per calculation. The C++ core reads twelve fields off this class and calls
 * [getNativeDirectionPoints]; `RoutingContext` holds it in a field the core resolves by descriptor,
 * so the package name is part of that contract too.
 */
class RoutingConfiguration {

	@JvmField
	var attributes: MutableMap<String, String> = LinkedHashMap()

	// 1. parameters of routing and different tweaks
	// Influence on A* : f(x) + heuristicCoefficient*g(X)
	@JvmField
	var heuristicCoefficient: Float = 1f

	// 1.1 tile load parameters (should not affect routing)
	@JvmField
	var ZOOM_TO_LOAD_TILES: Int = 16

	@JvmField
	var memoryLimitation: Long = 0

	@JvmField
	var memoryMaxHits: Long = -1

	@JvmField
	var nativeMemoryLimitation: Long = 0

	// 1.2 Build A* graph in backward/forward direction (can affect results)
	// 0 - 2 ways, 1 - direct way, -1 - reverse way
	@JvmField
	var planRoadDirection: Int = 0

	// 1.3 Router specific coefficients and restrictions
	// use GeneralRouter and not interface to simplify native access !
	@JvmField
	var router: GeneralRouter = GeneralRouter(GeneralRouterProfile.CAR, LinkedHashMap())

	@JvmField
	var routerName: String = ""

	// 1.4 Used to calculate route in movement
	@JvmField
	var initialDirection: Double? = null

	@JvmField
	var targetDirection: Double? = null

	// -1 means reverse is forbidden
	@JvmField
	var penaltyForReverseDirection: Double = DEFAULT_PENALTY_FOR_REVERSE_DIRECTION

	// 1.5 Recalculate distance help
	@JvmField
	var recalculateDistance: Float = 20000f

	// 1.6 Time to calculate all access restrictions based on conditions
	@JvmField
	var routeCalculationTime: Long = 0

	// 1.6.1. Apply "unlimited" :conditional tags (used by HHRoutingShortcutCreator)
	@JvmField
	var ambiguousConditionalTags: MutableMap<String, String>? = null

	// 1.7 Maximum visited segments
	@JvmField
	var MAX_VISITED: Int = -1

	/**
	 * Set only while alternative routes are being searched (see HHAlternativeRoutes): the bidirectional
	 * search then does not stop at the first meeting point but keeps settling until both queues leave
	 * the (1 + this) * optimum band, so that the two trees overlap enough to compare routes through them.
	 */
	@JvmField
	var altHorizon: Double = 0.0

	// extra points to be inserted in ways (quad tree is based on 31 coords)
	private var directionPoints: KQuadTree<DirectionPoint>? = null

	@JvmField
	var directionPointsRadius: Int = 30 // 30 m

	// ! MAIN parameter to approximate (35m good for custom recorded tracks)
	@JvmField
	var minPointApproximation: Float = 50f

	// don't search subsegments shorter than specified distance (also used to step back for car turns)
	@JvmField
	var minStepApproximation: Float = 100f

	// This parameter could speed up or slow down evaluation (better to make bigger for long routes and smaller for short)
	@JvmField
	var maxStepApproximation: Float = 3000f

	// Parameter to smoother the track itself (could be 0 if it's not recorded track)
	@JvmField
	var smoothenPointsNoRoute: Float = 5f

	@JvmField
	var showMinorTurns: Boolean = false

	fun getDirectionPoints(): KQuadTree<DirectionPoint>? = directionPoints

	fun getNativeDirectionPoints(): Array<NativeDirectionPoint> {
		val points = directionPoints ?: return emptyArray()
		val rect = KQuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble())
		val list = points.queryInBox(rect, ArrayList())
		return Array(list.size) {
			val point = list[it]
			NativeDirectionPoint(point.getLatitude(), point.getLongitude(), point.getTags())
		}
	}

	class RoutingMemoryLimits(
		@JvmField var memoryLimitMb: Int,
		@JvmField var nativeMemoryLimitMb: Int
	)

	class Builder {
		// Design time storage
		private var defaultRouter: String? = ""
		private val routers: MutableMap<String, GeneralRouter> = LinkedHashMap()
		private val attributes: MutableMap<String, String> = LinkedHashMap()
		private val impassableRoadLocations: MutableSet<Long> = HashSet()
		private var directionPointsBuilder: KQuadTree<DirectionPoint>? = null

		constructor()

		constructor(defaultAttributes: Map<String, String>) {
			attributes.putAll(defaultAttributes)
		}

		@JvmOverloads
		fun build(
			router: String,
			memoryLimits: RoutingMemoryLimits,
			params: MutableMap<String, String> = LinkedHashMap()
		): RoutingConfiguration = build(router, null, memoryLimits, params)

		fun build(
			router: String,
			direction: Double?,
			memoryLimits: RoutingMemoryLimits,
			params: MutableMap<String, String>
		): RoutingConfiguration {
			var selectedRouter: String? = router
			var derivedProfile: String? = null
			if (!routers.containsKey(router)) {
				for (r in routers.entries) {
					val derivedProfiles = r.value.getAttribute("derivedProfiles")
					if (derivedProfiles != null && derivedProfiles.contains(router)) {
						derivedProfile = router
						selectedRouter = r.key
						break
					}
				}
				if (derivedProfile == null) {
					selectedRouter = defaultRouter
				}
			}
			if (derivedProfile != null) {
				params["profile_$derivedProfile"] = true.toString()
			}
			val i = RoutingConfiguration()
			val profileRouter = if (selectedRouter != null) routers[selectedRouter] else null
			if (profileRouter != null) {
				i.router = profileRouter.build(params)
				i.routerName = selectedRouter!!
			}
			// a missing defaultProfile leaves nothing to name; java put a null here
			attributes["routerName"] = selectedRouter ?: ""
			i.attributes.putAll(attributes)
			i.initialDirection = direction
			i.recalculateDistance = parseSilentFloat(getAttribute(i.router, "recalculateDistanceHelp"), i.recalculateDistance)
			i.heuristicCoefficient = parseSilentFloat(getAttribute(i.router, "heuristicCoefficient"), i.heuristicCoefficient)
			i.minPointApproximation = parseSilentFloat(getAttribute(i.router, "minPointApproximation"), i.minPointApproximation)
			i.minStepApproximation = parseSilentFloat(getAttribute(i.router, "minStepApproximation"), i.minStepApproximation)
			i.maxStepApproximation = parseSilentFloat(getAttribute(i.router, "maxStepApproximation"), i.maxStepApproximation)
			i.smoothenPointsNoRoute = parseSilentFloat(getAttribute(i.router, "smoothenPointsNoRoute"), i.smoothenPointsNoRoute)
			i.penaltyForReverseDirection = parseSilentFloat(
				getAttribute(i.router, "penaltyForReverseDirection"), i.penaltyForReverseDirection.toFloat()
			).toDouble()

			i.router.setImpassableRoads(HashSet(impassableRoadLocations))
			i.ZOOM_TO_LOAD_TILES = parseSilentInt(getAttribute(i.router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES)
			var memoryLimitMB = memoryLimits.memoryLimitMb
			val desirable = parseSilentInt(getAttribute(i.router, "memoryLimitInMB"), 0)
			if (desirable != 0) {
				i.memoryLimitation = desirable * (1L shl 20)
			} else {
				if (memoryLimitMB == 0) {
					memoryLimitMB = DEFAULT_MEMORY_LIMIT
				}
				i.memoryLimitation = memoryLimitMB * (1L shl 20)
			}
			val desirableNativeLimit = parseSilentInt(getAttribute(i.router, "nativeMemoryLimitInMB"), 0)
			if (desirableNativeLimit != 0) {
				i.nativeMemoryLimitation = desirableNativeLimit * (1L shl 20)
			} else {
				i.nativeMemoryLimitation = memoryLimits.nativeMemoryLimitMb * (1L shl 20)
			}
			i.planRoadDirection = parseSilentInt(getAttribute(i.router, "planRoadDirection"), i.planRoadDirection)
			val builderPoints = directionPointsBuilder
			if (builderPoints != null) {
				val rect = KQuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble())
				val lst = builderPoints.queryInBox(rect, ArrayList())
				val points = KQuadTree<DirectionPoint>(rect, 14, 0.5f)
				for (p in lst) {
					// a copy per calculation: the router writes onto the point as it attaches it
					val dp = DirectionPoint(p)
					val x = KMapUtils.get31TileNumberX(dp.getLongitude())
					val y = KMapUtils.get31TileNumberY(dp.getLatitude())
					points.insert(dp, KQuadRect(x.toDouble(), y.toDouble(), x.toDouble(), y.toDouble()))
				}
				i.directionPoints = points
			}
			return i
		}

		fun setDirectionPoints(directionPoints: KQuadTree<DirectionPoint>?): Builder {
			this.directionPointsBuilder = directionPoints
			return this
		}

		fun clearImpassableRoadLocations() {
			impassableRoadLocations.clear()
		}

		fun getImpassableRoadLocations(): MutableSet<Long> = impassableRoadLocations

		fun addImpassableRoad(routeId: Long): Builder {
			impassableRoadLocations.add(routeId)
			return this
		}

		fun getAttributes(): MutableMap<String, String> = attributes

		private fun getAttribute(router: VehicleRouter, propertyName: String): String? {
			if (router.containsAttribute(propertyName)) {
				return router.getAttribute(propertyName)
			}
			return attributes[propertyName]
		}

		fun getDefaultRouter(): String? = defaultRouter

		fun getRouter(routingProfileName: String): GeneralRouter? = routers[routingProfileName]

		fun getRoutingProfileKeyByFileName(fileName: String?): String? {
			if (fileName != null) {
				for (router in routers.entries) {
					if (fileName == router.value.getFilename()) {
						return router.key
					}
				}
			}
			return null
		}

		fun getAllRouters(): MutableMap<String, GeneralRouter> = routers

		fun removeImpassableRoad(routeId: Long) {
			impassableRoadLocations.remove(routeId)
		}

		internal fun setDefaultRouter(name: String?) {
			defaultRouter = name
		}

		internal fun putAttribute(name: String, value: String) {
			attributes[name] = value
		}

		internal fun putRouter(name: String, router: GeneralRouter) {
			routers[name] = router
		}
	}

	private class RoutingRule {
		var tagName: String? = null
		var t: String? = null
		var v: String? = null
		var param: String? = null
		var value1: String? = null
		var value2: String? = null
		var type: String? = null
	}

	companion object {

		const val DEFAULT_MEMORY_LIMIT = 30
		const val DEFAULT_NATIVE_MEMORY_LIMIT = 256
		const val DEVIATION_RADIUS = 3000f

		// if no penaltyForReverseDirection in xml
		const val DEFAULT_PENALTY_FOR_REVERSE_DIRECTION = 60.0

		@JvmStatic
		fun parseSilentInt(t: String?, v: Int): Int {
			if (t == null || t.length == 0) {
				return v
			}
			return t.toInt()
		}

		@JvmStatic
		fun parseSilentFloat(t: String?, v: Float): Float {
			if (t == null || t.length == 0) {
				return v
			}
			return t.toFloat()
		}

		private var DEFAULT: Builder? = null

		@JvmStatic
		fun getDefault(): Builder {
			var d = DEFAULT
			if (d == null) {
				d = parseDefault()
				DEFAULT = d
			}
			return d
		}

		@JvmStatic
		fun parseDefault(): Builder {
			try {
				val source = openBundledRoutingXml()
					?: throw IllegalStateException("no routing.xml is bundled on this platform")
				return parseFromSource(source, null, Builder())
			} catch (e: Exception) {
				throw IllegalStateException(e)
			}
		}

		@JvmStatic
		@JvmOverloads
		fun parseFromFile(file: KFile, filename: String? = null, config: Builder = Builder()): Builder =
			parseFromSource(file.source(), filename, config)

		/**
		 * Takes a path rather than a [KFile] so that java callers do not have to name one: okio is
		 * an implementation dependency of this module and its `Path` is not on their classpath.
		 */
		@JvmStatic
		@JvmOverloads
		fun parseFromFile(filePath: String, filename: String? = null, config: Builder = Builder()): Builder =
			parseFromFile(KFile(filePath), filename, config)

		@JvmStatic
		@JvmOverloads
		fun parseFromSource(source: Source, filename: String? = null, config: Builder = Builder()): Builder {
			val parser = XmlPullParser()
			var currentRouter: GeneralRouter? = null
			var currentAttribute: RouteDataObjectAttribute? = null
			var preType: String? = null
			val rulesStck = ArrayList<RoutingRule>()
			try {
				parser.setInput(source, "UTF-8")
				while (true) {
					val tok = parser.next()
					if (tok == XmlPullParser.END_DOCUMENT) {
						break
					}
					if (tok == XmlPullParser.START_TAG) {
						val name = parser.getName()
						if ("osmand_routing_config" == name) {
							config.setDefaultRouter(parser.getAttributeValue("", "defaultProfile"))
						} else if ("routingProfile" == name) {
							currentRouter = parseRoutingProfile(parser, config, filename)
						} else if ("attribute" == name) {
							parseAttribute(parser, config, currentRouter)
						} else if ("parameter" == name) {
							parseRoutingParameter(parser, currentRouter!!)
						} else if ("point" == name || "way" == name) {
							val attribute = parser.getAttributeValue("", "attribute")
							currentAttribute =
								if (attribute != null) RouteDataObjectAttribute.getValueOf(attribute) else null
							preType = parser.getAttributeValue("", "type")
						} else {
							parseRoutingRule(parser, currentRouter!!, currentAttribute, preType, rulesStck)
						}
					} else if (tok == XmlPullParser.END_TAG) {
						val pname = parser.getName()
						if (checkTag(pname)) {
							rulesStck.removeAt(rulesStck.size - 1)
						}
					}
				}
			} finally {
				// java closed the stream only on the way out; a failed parse used to leak it
				source.close()
			}
			return config
		}

		private fun parseRoutingParameter(parser: XmlPullParser, currentRouter: GeneralRouter) {
			val description = parser.getAttributeValue("", "description")
			val group = parser.getAttributeValue("", "group")
			val name = parser.getAttributeValue("", "name")
			val id = parser.getAttributeValue("", "id")
			val type = parser.getAttributeValue("", "type")
			val profilesList = parser.getAttributeValue("", "profiles")
			val profiles = if (KAlgorithms.isEmpty(profilesList)) null else profilesList!!.split(",").toTypedArray()
			if ("boolean".equals(type, ignoreCase = true)) {
				val defaultBoolean = parser.getAttributeValue("", "default").toBoolean()
				currentRouter.registerBooleanParameter(
					id!!, if (KAlgorithms.isEmpty(group)) null else group,
					name, description, profiles, defaultBoolean
				)
			} else if ("numeric".equals(type, ignoreCase = true)) {
				val defaultNumeric = KAlgorithms.parseDoubleSilently(parser.getAttributeValue("", "default"), 0.0)
				val values = parser.getAttributeValue("", "values")
				val valueDescriptions = parser.getAttributeValue("", "valueDescriptions")
				val vlsDesc = valueDescriptions!!.split(",").toTypedArray()
				val strValues = values!!.split(",").toTypedArray()
				val vls = Array<Any>(strValues.size) { strValues[it].trim().toDouble() }
				currentRouter.registerNumericParameter(id!!, name, description, profiles, vls, vlsDesc, defaultNumeric)
			} else {
				throw UnsupportedOperationException("Unsupported routing parameter type - $type")
			}
		}

		private fun parseRoutingRule(
			parser: XmlPullParser, currentRouter: GeneralRouter, attr: RouteDataObjectAttribute?,
			parentType: String?, stack: MutableList<RoutingRule>
		) {
			val pname = parser.getName()
			if (checkTag(pname)) {
				if (attr == null) {
					throw NullPointerException("Select tag filter outside road attribute < $pname > : " + parser.getLineNumber())
				}
				val rr = RoutingRule()
				rr.tagName = pname
				rr.t = parser.getAttributeValue("", "t")
				rr.v = parser.getAttributeValue("", "v")
				rr.param = parser.getAttributeValue("", "param")
				rr.value1 = parser.getAttributeValue("", "value1")
				rr.value2 = parser.getAttributeValue("", "value2")
				rr.type = parser.getAttributeValue("", "type")
				if ((rr.type == null || rr.type!!.length == 0) && parentType != null && parentType.length > 0) {
					rr.type = parentType
				}

				val ctx = currentRouter.getObjContext(attr)
				if ("select" == rr.tagName) {
					// the grammar requires these, and a rule built without one is broken anyway
					val v = parser.getAttributeValue("", "value")
					val type = rr.type
					ctx.registerNewRule(v!!, type)
					addSubclause(rr, ctx)
					for (i in stack.indices) {
						addSubclause(stack[i], ctx)
					}
				} else if ("min" == rr.tagName || "max" == rr.tagName) {
					val initVal = parser.getAttributeValue("", "value1")
					val type = rr.type
					ctx.registerNewRule(initVal!!, type)
					addSubclause(rr, ctx)
				} else if (stack.size > 0 && "select" == stack[stack.size - 1].tagName) {
					addSubclause(rr, ctx)
				}
				stack.add(rr)
			}
		}

		private fun checkTag(pname: String?): Boolean {
			return "select" == pname || "if" == pname || "ifnot" == pname
					|| "gt" == pname || "ge" == pname || "lt" == pname || "le" == pname
					|| "eq" == pname || "min" == pname || "max" == pname
		}

		private fun addSubclause(rr: RoutingRule, ctx: RouteAttributeContext) {
			val not = "ifnot" == rr.tagName
			if (!KAlgorithms.isEmpty(rr.param)) {
				if (rr.param!!.contains(",")) {
					val params = rr.param!!.split(",")
					for (p in params) {
						val trimmed = p.trim()
						if (!KAlgorithms.isEmpty(trimmed)) {
							ctx.getLastRule().registerAndParamCondition(trimmed, not)
						}
					}
				} else {
					ctx.getLastRule().registerAndParamCondition(rr.param!!, not)
				}
			}
			if (!KAlgorithms.isEmpty(rr.t)) {
				ctx.getLastRule().registerAndTagValueCondition(rr.t!!, if (KAlgorithms.isEmpty(rr.v)) null else rr.v, not)
			}
			when (rr.tagName) {
				"gt" -> ctx.getLastRule().registerGreatCondition(rr.value1!!, rr.value2!!, rr.type)
				"ge" -> ctx.getLastRule().registerGreatOrEqualCondition(rr.value1!!, rr.value2!!, rr.type)
				"lt" -> ctx.getLastRule().registerLessCondition(rr.value1!!, rr.value2!!, rr.type)
				"le" -> ctx.getLastRule().registerLessOrEqualCondition(rr.value1!!, rr.value2!!, rr.type)
				"eq" -> ctx.getLastRule().registerEqualCondition(rr.value1!!, rr.value2!!, rr.type)
				"min" -> ctx.getLastRule().registerMinExpression(rr.value1!!, rr.value2!!, rr.type)
				"max" -> ctx.getLastRule().registerMaxExpression(rr.value1!!, rr.value2!!, rr.type)
			}
		}

		private fun parseRoutingProfile(parser: XmlPullParser, config: Builder, filename: String?): GeneralRouter {
			var currentSelectedRouterName = parser.getAttributeValue("", "name")
			val attrs = LinkedHashMap<String, String>()
			for (i in 0 until parser.getAttributeCount()) {
				attrs[parser.getAttributeName(i)!!] = parser.getAttributeValue(i)!!
			}
			val baseProfile = parser.getAttributeValue("", "baseProfile")
			val c = GeneralRouterProfile.entries.firstOrNull { it.name.equals(baseProfile, ignoreCase = true) }
				?: GeneralRouterProfile.CAR
			val currentRouter = GeneralRouter(c, attrs)
			currentRouter.setProfileName(currentSelectedRouterName!!)
			if (filename != null) {
				currentRouter.setFilename(filename)
				currentSelectedRouterName = "$filename/$currentSelectedRouterName"
			}

			config.putRouter(currentSelectedRouterName!!, currentRouter)
			return currentRouter
		}

		private fun parseAttribute(parser: XmlPullParser, config: Builder, currentRouter: GeneralRouter?) {
			if (currentRouter != null) {
				currentRouter.addAttribute(
					parser.getAttributeValue("", "name")!!,
					parser.getAttributeValue("", "value")!!
				)
			} else {
				config.putAttribute(
					parser.getAttributeValue("", "name")!!,
					parser.getAttributeValue("", "value")!!
				)
			}
		}
	}
}
