package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryIndexPart
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.jvm.JvmField
import kotlin.math.max
import kotlin.math.min

/**
 * The routing section of one obf file: its encoding rules and the r-tree over its roads.
 *
 * Roads are stored with their tags replaced by indices into [routeEncodingRules], so a road can
 * only be read through the region it came from.
 *
 * A copy of `BinaryMapRouteReaderAdapter.RouteRegion`, which stays in OsmAnd-java: android, tools and the C++ core keep using
 * the original, and this copy is for iOS. Keep the two identical.
 */
class RouteRegion : BinaryIndexPart() {

	@JvmField
	var regionsRead: Int = 0

	@JvmField
	var routeEncodingRules: MutableList<RouteTypeRule?> = ArrayList()

	@JvmField
	var routeEncodingRulesBytes: Int = 0

	private var decodingRules: MutableMap<String, Int>? = null

	@JvmField
	var subregions: MutableList<RouteSubregion> = ArrayList()

	@JvmField
	var basesubregions: MutableList<RouteSubregion> = ArrayList()

	@JvmField
	var directionForward: Int = -1

	@JvmField
	var directionBackward: Int = -1

	@JvmField
	var maxheightForward: Int = -1

	@JvmField
	var maxheightBackward: Int = -1

	@JvmField
	var directionTrafficSignalsForward: Int = -1

	@JvmField
	var directionTrafficSignalsBackward: Int = -1

	@JvmField
	var trafficSignals: Int = -1

	@JvmField
	var stopSign: Int = -1

	@JvmField
	var stopMinor: Int = -1

	@JvmField
	var giveWaySign: Int = -1

	@JvmField
	var nameTypeRule: Int = -1

	@JvmField
	var refTypeRule: Int = -1

	@JvmField
	var destinationTypeRule: Int = -1

	@JvmField
	var destinationRefTypeRule: Int = -1

	/** The region whose rules this one borrowed in [adopt], so its roads need no re-encoding. */
	private var referenceRouteRegion: RouteRegion? = null

	override fun getPartName(): String = "Routing"

	override fun getFieldNumber(): Int = ROUTING_INDEX_FIELD_NUMBER

	/** Index of the rule for [tag]/[value], or -1. */
	fun searchRouteEncodingRule(tag: String, value: String?): Int {
		var rules = decodingRules
		if (rules == null) {
			rules = LinkedHashMap()
			for (i in 1 until routeEncodingRules.size) {
				val rt = routeEncodingRules[i] ?: continue
				rules[rt.getTag() + "#" + (rt.getValue() ?: "")] = i
			}
			decodingRules = rules
		}
		return rules[tag + "#" + (value ?: "")] ?: -1
	}

	fun getNameTypeRule(): Int = nameTypeRule

	fun getRefTypeRule(): Int = refTypeRule

	/** The rule at [id]. Null only where the rule table has a gap. */
	fun quickGetEncodingRule(id: Int): RouteTypeRule? = routeEncodingRules[id]

	fun quickGetEncodingRulesSize(): Int = routeEncodingRules.size

	fun initRouteEncodingRule(id: Int, tags: String, `val`: String?) {
		val append = id >= routeEncodingRules.size
		if (!append) {
			decodingRules = null
		}
		while (routeEncodingRules.size <= id) {
			routeEncodingRules.add(null)
		}
		val rt = RouteTypeRule(tags, `val`)
		routeEncodingRules[id] = rt
		if (append && id > 0) {
			decodingRules?.put(rt.getTag() + "#" + (rt.getValue() ?: ""), id)
		}
		if (tags == "name") {
			nameTypeRule = id
		} else if (tags == "ref") {
			refTypeRule = id
		} else if (tags == "destination" || tags == "destination:forward" || tags == "destination:backward" || tags.startsWith("destination:lang:")) {
			destinationTypeRule = id
		} else if (tags == "destination:ref" || tags == "destination:ref:forward" || tags == "destination:ref:backward") {
			destinationRefTypeRule = id
		} else if (tags == "highway" && `val` == "traffic_signals") {
			trafficSignals = id
		} else if (tags == "stop" && `val` == "minor") {
			stopMinor = id
		} else if (tags == "highway" && `val` == "stop") {
			stopSign = id
		} else if (tags == "highway" && `val` == "give_way") {
			giveWaySign = id
		} else if (tags == "traffic_signals:direction" && `val` != null) {
			if (`val` == "forward") {
				directionTrafficSignalsForward = id
			} else if (`val` == "backward") {
				directionTrafficSignalsBackward = id
			}
		} else if (tags == "direction" && `val` != null) {
			if (`val` == "forward") {
				directionForward = id
			} else if (`val` == "backward") {
				directionBackward = id
			}
			// could be generic
		} else if (tags == "maxheight:forward" && `val` != null) {
			maxheightForward = id
		} else if (tags == "maxheight:backward" && `val` != null) {
			maxheightBackward = id
		}
	}

	/** Resolves every conditional rule to the plain rule it stands for while the condition holds. */
	fun completeRouteEncodingRules() {
		for (i in routeEncodingRules.indices) {
			val rtr = routeEncodingRules[i]
			if (rtr != null && rtr.conditional()) {
				val tag = rtr.getNonConditionalTag()
				for (c in rtr.getConditions()!!) {
					if (c.value != null) {
						c.ruleId = findOrCreateRouteType(tag, c.value)
					}
				}
			}
		}
	}

	fun getSubregions(): MutableList<RouteSubregion> = subregions

	fun getBaseSubregions(): MutableList<RouteSubregion> = basesubregions

	fun getLeftLongitude(): Double {
		var l = 180.0
		for (s in subregions) {
			l = min(l, KMapUtils.get31LongitudeX(s.left))
		}
		return l
	}

	fun getRightLongitude(): Double {
		var l = -180.0
		for (s in subregions) {
			l = max(l, KMapUtils.get31LongitudeX(s.right))
		}
		return l
	}

	fun getBottomLatitude(): Double {
		var l = 90.0
		for (s in subregions) {
			l = min(l, KMapUtils.get31LatitudeY(s.bottom))
		}
		return l
	}

	fun getTopLatitude(): Double {
		var l = -90.0
		for (s in subregions) {
			l = max(l, KMapUtils.get31LatitudeY(s.top))
		}
		return l
	}

	fun contains(x31: Int, y31: Int): Boolean {
		for (s in subregions) {
			if (s.left <= x31 && s.right >= x31 && s.top <= y31 && s.bottom >= y31) {
				return true
			}
		}
		return false
	}

	/**
	 * Returns [o] re-encoded against this region's rules, or [o] itself when it needs no change.
	 *
	 * Used when roads from several files are merged into one: their type indices point into
	 * different rule tables and have to be made to agree.
	 */
	fun adopt(o: RouteDataObject): RouteDataObject {
		if (o.region === this || o.region === referenceRouteRegion) {
			return o
		}

		if (routeEncodingRules.isEmpty()) {
			routeEncodingRules.addAll(o.region!!.routeEncodingRules)
			referenceRouteRegion = o.region
			return o
		}
		val rdo = RouteDataObject(this)
		rdo.pointsX = o.pointsX
		rdo.pointsY = o.pointsY
		rdo.id = o.id
		rdo.restrictions = o.restrictions
		rdo.restrictionsVia = o.restrictionsVia

		val types = o.types
		if (types != null) {
			rdo.types = IntArray(types.size) { i ->
				val tp = o.region!!.routeEncodingRules[types[i]]!!
				findOrCreateRouteType(tp.getTag(), tp.getValue())
			}
		}
		val pointTypes = o.pointTypes
		if (pointTypes != null) {
			val adopted = arrayOfNulls<IntArray>(pointTypes.size)
			for (i in pointTypes.indices) {
				val point = pointTypes[i] ?: continue
				adopted[i] = IntArray(point.size) { j ->
					val tp = o.region!!.routeEncodingRules[point[j]]!!
					var ruleId = searchRouteEncodingRule(tp.getTag(), tp.getValue())
					if (ruleId == -1) {
						ruleId = routeEncodingRules.size
						initRouteEncodingRule(ruleId, tp.getTag(), tp.getValue())
					}
					ruleId
				}
			}
			rdo.pointTypes = adopted
		}
		val nameIds = o.nameIds
		if (nameIds != null) {
			val adopted = IntArray(nameIds.size)
			val names = KTIntObjectMap<String>()
			for (i in nameIds.indices) {
				val tp = o.region!!.routeEncodingRules[nameIds[i]]!!
				var ruleId = searchRouteEncodingRule(tp.getTag(), null)
				if (ruleId == -1) {
					ruleId = routeEncodingRules.size
					initRouteEncodingRule(ruleId, tp.getTag(), null)
				}
				adopted[i] = ruleId
				o.names?.get(nameIds[i])?.let { names.put(ruleId, it) }
			}
			rdo.nameIds = adopted
			rdo.names = names
		}
		rdo.pointNames = o.pointNames
		val pointNameTypes = o.pointNameTypes
		if (pointNameTypes != null) {
			val adopted = arrayOfNulls<IntArray>(pointNameTypes.size)
			for (i in pointNameTypes.indices) {
				val point = pointNameTypes[i] ?: continue
				adopted[i] = IntArray(point.size) { j ->
					val tp = o.region!!.routeEncodingRules[point[j]]!!
					var ruleId = searchRouteEncodingRule(tp.getTag(), null)
					if (ruleId == -1) {
						ruleId = routeEncodingRules.size
						initRouteEncodingRule(ruleId, tp.getTag(), tp.getValue())
					}
					ruleId
				}
			}
			rdo.pointNameTypes = adopted
		}
		return rdo
	}

	fun findOrCreateRouteType(tag: String, value: String?): Int {
		var ruleId = searchRouteEncodingRule(tag, value)
		if (ruleId == -1) {
			ruleId = routeEncodingRules.size
			initRouteEncodingRule(ruleId, tag, value)
		}
		return ruleId
	}

	companion object {
		/** `OsmAndStructure.routingIndex` in osmand_odb.proto, frozen by the obf format. */
		private const val ROUTING_INDEX_FIELD_NUMBER = 9
	}
}
