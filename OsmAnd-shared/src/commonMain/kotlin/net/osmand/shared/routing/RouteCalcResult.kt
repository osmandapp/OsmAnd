package net.osmand.shared.routing

import kotlin.jvm.JvmField

/**
 * What a route calculation came back with: the segments, or the reason there are none.
 *
 * Based on `RouteResultPreparation.RouteCalcResult` from OsmAnd-java.
 */
open class RouteCalcResult {

	@JvmField
	var detailed: MutableList<RouteSegmentResult> = ArrayList()

	@JvmField
	var error: String? = null

	constructor(list: MutableList<RouteSegmentResult>?) {
		if (list == null) {
			error = "Result is empty"
		} else {
			this.detailed = list
		}
	}

	constructor(error: String) {
		this.error = error
	}

	fun getList(): MutableList<RouteSegmentResult> = detailed

	fun getError(): String? = error

	fun isCorrect(): Boolean = error == null && detailed.isNotEmpty()
}
