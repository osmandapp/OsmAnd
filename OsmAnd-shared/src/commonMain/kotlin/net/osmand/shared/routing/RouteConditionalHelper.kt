package net.osmand.shared.routing

import net.osmand.shared.util.LoggerFactory

/**
 * Applies `:conditional` tags to a road before the router sees it.
 *
 * Two things happen here. [processConditionalTags] resolves a condition against a moment in time -
 * `maxspeed:conditional` that only holds at night, an `access:conditional` that only holds in
 * summer - and writes the value it produced onto the road as if the map had carried it plainly.
 * [resolveAmbiguousConditionalTags] does the same for the conditions the caller decided to treat as
 * always true, which is how HHRoutingShortcutCreator builds a graph that no clock can invalidate.
 */
class RouteConditionalHelper {

	fun resolveAmbiguousConditionalTags(rdo: RouteDataObject, ambiguousConditionalTags: Map<String, String>) {
		val existingIntValues = HashMap<String, Int>()

		// Find corresponding non-conditional tags and save their existing int-values.
		// Example: maxspeed:conditional (RULE_INT_MAX) will save the value of maxspeed.
		for (type in rdo.types!!) {
			val r = rdo.region!!.quickGetEncodingRule(type)
			if (r != null && !r.conditional()) {
				val key = r.getTag() + ":conditional"
				val rule = ambiguousConditionalTags[key]
				if (rule != null && RULE_INT_MAX == rule) {
					val newValue = r.getValue()?.toIntOrNull() ?: continue
					val oldValue = existingIntValues[key]
					if (oldValue == null || newValue > oldValue) {
						existingIntValues[key] = newValue
					}
				}
			}
		}

		// Find conditionals and update their non-conditionals by the rules.
		// Example: access:conditional ("yes") will always set "access" = "yes"
		// Example: maxspeed:conditional (RULE_INT_MAX) might set maxspeed = max(existing, conditional)
		for (type in rdo.types!!) {
			val r = rdo.region!!.quickGetEncodingRule(type)
			if (r != null && r.conditional()) {
				val key = r.getTag()
				val rule = ambiguousConditionalTags[key]
				if (rule != null && RULE_INT_MAX == rule) {
					val existingValue = existingIntValues[key]
					val newValue = r.getMaxIntegerConditionalValue()
					if (newValue != null && (existingValue == null || newValue > existingValue)) {
						updateTypesByTagValue(rdo, r.getNonConditionalTag(), newValue.toString()) // max value
					}
				} else if (rule != null) {
					updateTypesByTagValue(rdo, r.getNonConditionalTag(), rule) // Default rule: set the string value
				}
			}
		}
	}

	fun processConditionalTags(rdo: RouteDataObject, conditionalTime: Long) {
		val sz = rdo.types!!.size
		for (i in 0 until sz) {
			val r = rdo.region!!.quickGetEncodingRule(rdo.types!![i])
			if (r != null && r.conditional()) {
				val vl = r.conditionalValue(conditionalTime)
				if (vl != 0) {
					val nonCondTag = rdo.region!!.quickGetEncodingRule(vl)!!.getTag()
					updateTypesByTagRuleId(rdo, nonCondTag, vl)
				}
			}
		}

		val pointTypes = rdo.pointTypes
		if (pointTypes != null) {
			for (i in pointTypes.indices) {
				if (pointTypes[i] != null) {
					var pTypes = pointTypes[i]!!
					val pSz = pTypes.size
					if (pSz > 0) {
						for (j in 0 until pSz) {
							val r = rdo.region!!.quickGetEncodingRule(pTypes[j])
							if (r != null && r.conditional()) {
								val vl = r.conditionalValue(conditionalTime)
								if (vl != 0) {
									val rtr = rdo.region!!.quickGetEncodingRule(vl)!!
									val nonCondTag = rtr.getTag()
									var ks = 0
									while (ks < pointTypes[i]!!.size) {
										val toReplace = rdo.region!!.quickGetEncodingRule(pointTypes[i]!![ks])
										if (toReplace != null && toReplace.getTag() == nonCondTag) {
											break
										}
										ks++
									}
									if (ks == pTypes.size) {
										val ntypes = IntArray(pTypes.size + 1)
										pTypes.copyInto(ntypes, 0, 0, pTypes.size)
										pTypes = ntypes
									}
									pTypes[ks] = vl
								}
							}
						}
					}
					pointTypes[i] = pTypes
				}
			}
		}
	}

	fun updateTypesByTagValue(rdo: RouteDataObject, tag: String, value: String) {
		val ruleId = rdo.region!!.searchRouteEncodingRule(tag, value)
		if (ruleId > 0) {
			updateTypesByTagRuleId(rdo, tag, ruleId)
		} else {
			LOG.error("updateTypesByTagValue($tag,$value): searchRouteEncodingRule failed")
		}
	}

	fun updateTypesByTagRuleId(rdo: RouteDataObject, tag: String, ruleId: Int) {
		if (ruleId > 0) {
			var types = rdo.types!!
			var ks = 0
			while (ks < types.size) {
				val toReplace = rdo.region!!.quickGetEncodingRule(types[ks])
				if (toReplace != null && toReplace.getTag() == tag) {
					break
				}
				ks++
			}
			if (ks == types.size) {
				val ntypes = IntArray(types.size + 1)
				types.copyInto(ntypes, 0, 0, types.size)
				rdo.types = ntypes
				types = ntypes
			}
			types[ks] = ruleId
		}
	}

	companion object {
		const val RULE_INT_MAX = "RULE_INT_MAX"

		private val LOG = LoggerFactory.getLogger("RouteConditionalHelper")
	}
}
