package net.osmand.shared.routing

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.OpeningHoursParser
import net.osmand.shared.util.internString

/**
 * One tag/value pair of the routing section of an obf file.
 *
 * Rules are read once per region and then looked up by index for every road of that region,
 * so everything the router asks for later is precomputed in [analyze].
 */
class RouteTypeRule(t: String, v: String?) {

	private val t: String
	private val v: String?
	private var intValue = 0
	private var floatValue = 0f
	private var type = 0
	private var conditions: MutableList<RouteTypeCondition>? = null
	private var forward = 0

	init {
		this.t = internString(t)
		var value = v
		if ("true" == value) {
			value = "yes"
		}
		if ("false" == value) {
			value = "no"
		}
		this.v = if (value == null) null else internString(value)
		try {
			analyze()
		} catch (e: RuntimeException) {
			LOG.error("Error analyzing tag/value = ${this.t}/${this.v}")
			throw e
		}
	}

	/** A single `value @ (opening hours)` alternative of a `*:conditional` tag. */
	class RouteTypeCondition internal constructor(val value: String?, val condition: String) {

		internal val hours: OpeningHoursParser.OpeningHours? =
			OpeningHoursParser.parseOpenedHours(condition)

		/** Index of the rule this condition resolves to, assigned by the owning route region. */
		var ruleId: Int = 0
	}

	override fun hashCode(): Int {
		val prime = 31
		var result = 1
		result = prime * result + (t.hashCode())
		result = prime * result + (v?.hashCode() ?: 0)
		return result
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null || other !is RouteTypeRule) {
			return false
		}
		return KAlgorithms.stringsEqual(other.t, t) && KAlgorithms.stringsEqual(other.v, v)
	}

	override fun toString(): String = "$t=$v"

	fun isForward(): Int = forward

	fun getTag(): String = t

	fun getValue(): String? = v

	fun roundabout(): Boolean = type == ROUNDABOUT

	fun getType(): Int = type

	fun conditional(): Boolean = conditions != null

	fun getConditions(): List<RouteTypeCondition>? = conditions

	fun getNonConditionalTag(): String {
		var tag = getTag()
		if (tag.endsWith(":conditional")) {
			tag = tag.substring(0, tag.length - ":conditional".length)
		}
		return tag
	}

	fun onewayDirection(): Int {
		if (type == ONEWAY) {
			return intValue
		}
		return 0
	}

	fun conditionalValue(time: Long): Int {
		conditions?.let {
			for (c in it) {
				if (c.hours != null && c.hours.isOpenedForTime(time)) {
					return c.ruleId
				}
			}
		}
		return 0
	}

	fun getMaxIntegerConditionalValue(): Int? {
		val conditions = this.conditions ?: return null
		var maxValue = Int.MIN_VALUE
		for (c in conditions) {
			val value = c.value?.toIntOrNull() ?: continue
			if (value > maxValue) {
				maxValue = value
			}
		}
		return if (maxValue > Int.MIN_VALUE) maxValue else null
	}

	fun maxSpeed(profile: Int): Float {
		if (type == MAXSPEED + profile) {
			return floatValue
		}
		return -1f
	}

	fun lanes(): Int {
		if (type == LANES) {
			return intValue
		}
		return -1
	}

	fun highwayRoad(): String? {
		if (type == HIGHWAY_TYPE) {
			return v
		}
		return null
	}

	private fun analyze() {
		if (t.equals("oneway", ignoreCase = true)) {
			type = ONEWAY
			intValue = if ("-1" == v || "reverse" == v) {
				-1
			} else if ("1" == v || "yes" == v) {
				1
			} else {
				0
			}
		} else if (t.equals("highway", ignoreCase = true) && "traffic_signals" == v) {
			type = TRAFFIC_SIGNALS
		} else if (t.equals("railway", ignoreCase = true) && ("crossing" == v || "level_crossing" == v)) {
			type = RAILWAY_CROSSING
		} else if (t.equals("roundabout", ignoreCase = true) && v != null) {
			type = ROUNDABOUT
		} else if (t.equals("junction", ignoreCase = true) && "roundabout".equals(v, ignoreCase = true)) {
			type = ROUNDABOUT
		} else if (t.equals("highway", ignoreCase = true) && v != null) {
			type = HIGHWAY_TYPE
		} else if (t.endsWith(":conditional") && v != null) {
			val parsed = ArrayList<RouteTypeCondition>()
			for (c in v.split(");")) {
				val ch = c.indexOf('@')
				if (ch > 0) {
					var condition = c.substring(ch + 1).trim()
					if (condition.startsWith("(")) {
						condition = condition.substring(1).trim()
					}
					if (condition.endsWith(")")) {
						condition = condition.substring(0, condition.length - 1).trim()
					}
					parsed.add(RouteTypeCondition(c.substring(0, ch).trim(), condition))
				}
			}
			conditions = parsed
			// we don't set type for conditional so they are not used directly
		} else if (t.startsWith("access") && v != null) {
			type = ACCESS
		} else if (t.startsWith("maxspeed") && v != null) {
			var tg = t
			if (t.endsWith(":forward")) {
				tg = t.substring(0, t.length - ":forward".length)
				forward = 1
			} else if (t.endsWith(":backward")) {
				tg = t.substring(0, t.length - ":backward".length)
				forward = -1
			} else {
				forward = 0
			}
			floatValue = RouteDataUtils.parseSpeed(v, 0f)
			if (tg.equals("maxspeed", ignoreCase = true)) {
				type = MAXSPEED
			} else if (tg.equals("maxspeed:hgv", ignoreCase = true)) {
				type = MAXSPEED + PROFILE_TRUCK
			} else if (tg.equals("maxspeed:motorcar", ignoreCase = true)) {
				type = MAXSPEED + PROFILE_CAR
			}
		} else if (t.equals("lanes", ignoreCase = true) && v != null) {
			intValue = -1
			var i = 0
			type = LANES
			while (i < v.length && KAlgorithms.isDigit(v[i])) {
				i++
			}
			if (i > 0) {
				intValue = v.substring(0, i).toInt()
			}
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("RouteTypeRule")

		private const val ACCESS = 1
		private const val ONEWAY = 2
		private const val HIGHWAY_TYPE = 3
		private const val MAXSPEED = 4
		private const val ROUNDABOUT = 5
		const val TRAFFIC_SIGNALS = 6
		const val RAILWAY_CROSSING = 7
		private const val LANES = 8

		const val PROFILE_NONE = 0
		const val PROFILE_TRUCK = 1000
		const val PROFILE_CAR = 1001
	}
}
