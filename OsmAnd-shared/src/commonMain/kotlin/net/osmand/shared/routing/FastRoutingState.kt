package net.osmand.shared.routing

import kotlin.jvm.JvmStatic
import kotlin.math.max

/**
 * How far the fast (hierarchical) routing attempt got, and why it stopped.
 *
 * The state is a single ordinal, as in java, where the C++ core reads and raises it through one int field on
 * [RouteCalculationProgress]; the transitions live here rather than on the progress itself because
 * they only ever move forward, and [raise] is what enforces that.
 */
object FastRoutingState {

	enum class Status {
		READY,

		// MissingMapsCalculator
		MIXED_MAPS_INTERMEDIATES,
		MIXED_MAPS_AT_START_OR_END,
		MISSING_MAPS_INTERMEDIATES,
		MISSING_MAPS_AT_START_OR_END,

		// HHRoutePlanner
		FAILED_WITH_MIXED_MAPS,
		FAILED_WITH_MISSING_MAPS,
		FAILED_NO_HH_ROUTING_DATA, // pedestrian profile, ancient maps, etc
		FAILED_NEED_MORE_LAND_MAPS, // unusual geometry, e.g. Istanbul to Chișinău without the Bulgaria map
		FAILED_UNSUPPORTED_PARAMETERS, // highly likely unsupported routing parameters (too many recalculations)

		CANCELLED,
		SUCCESS
	}

	@JvmStatic
	fun isSuccessStatus(status: Status): Boolean {
		return status == Status.SUCCESS
	}

	@JvmStatic
	fun isCancelledStatus(status: Status): Boolean {
		return status == Status.CANCELLED
	}

	@JvmStatic
	fun isFailedStatus(status: Status): Boolean {
		return status == Status.FAILED_WITH_MIXED_MAPS
				|| status == Status.FAILED_WITH_MISSING_MAPS
				|| status == Status.FAILED_NO_HH_ROUTING_DATA
				|| status == Status.FAILED_NEED_MORE_LAND_MAPS
				|| status == Status.FAILED_UNSUPPORTED_PARAMETERS
	}

	internal fun get(ordinal: Int): Status {
		return Status.entries[ordinal]
	}

	internal fun reset(): Int {
		return Status.READY.ordinal
	}

	internal fun raise(old: Int, status: Status): Int {
		return max(status.ordinal, old)
	}

	internal fun fail(old: Int, hasUnsupportedParameters: Boolean): Int {
		return if (isMixedMaps(old)) {
			raise(old, Status.FAILED_WITH_MIXED_MAPS)
		} else if (isMissingMaps(old)) {
			raise(old, Status.FAILED_WITH_MISSING_MAPS)
		} else {
			raise(
				old, if (hasUnsupportedParameters) Status.FAILED_UNSUPPORTED_PARAMETERS
				else Status.FAILED_NEED_MORE_LAND_MAPS
			)
		}
	}

	internal fun isMixedOrMissingMaps(ordinal: Int): Boolean {
		return isMixedMaps(ordinal) || isMissingMaps(ordinal)
	}

	internal fun isSlowRoutingActive(ordinal: Int): Boolean {
		return isFailedStatus(get(ordinal))
	}

	internal fun isMixedMaps(ordinal: Int): Boolean {
		return ordinal == Status.FAILED_WITH_MIXED_MAPS.ordinal
				|| ordinal == Status.MIXED_MAPS_INTERMEDIATES.ordinal
				|| ordinal == Status.MIXED_MAPS_AT_START_OR_END.ordinal
	}

	internal fun isMissingMaps(ordinal: Int): Boolean {
		return ordinal == Status.FAILED_WITH_MISSING_MAPS.ordinal
				|| ordinal == Status.MISSING_MAPS_INTERMEDIATES.ordinal
				|| ordinal == Status.MISSING_MAPS_AT_START_OR_END.ordinal
	}
}
