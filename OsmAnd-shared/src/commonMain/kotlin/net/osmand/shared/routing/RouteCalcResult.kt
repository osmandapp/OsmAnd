package net.osmand.shared.routing

import kotlin.jvm.JvmField

/**
 * What a route calculation came back with: the segments, or the reason there are none.
 *
 * A copy of `RouteResultPreparation.RouteCalcResult` in OsmAnd-java, which stays there for
 * android and tools; this copy is for iOS.
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

	/** routes with the same start / end, empty unless they were requested and found */
	open fun getAlternatives(): List<List<RouteSegmentResult>> = emptyList()

	fun getError(): String? = error

	fun isCorrect(): Boolean = error == null && detailed.isNotEmpty()
}
