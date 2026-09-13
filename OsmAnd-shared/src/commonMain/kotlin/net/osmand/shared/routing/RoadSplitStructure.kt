package net.osmand.shared.routing

import kotlin.jvm.JvmField
import kotlin.math.abs

/**
 * What the roads leaving one intersection look like from the road being driven: how many of them go
 * off to each side, how many lanes they take with them, and how loudly each deserves to be spoken.
 *
 * This is what a "keep left" or "keep right" is decided from - a fork is only worth an instruction
 * when there is something to keep away from.
 */
class RoadSplitStructure {

	@JvmField var keepLeft: Boolean = false
	@JvmField var keepRight: Boolean = false
	@JvmField var speak: Boolean = false

	@JvmField var leftLanesInfo: MutableList<AttachedRoadInfo> = ArrayList()
	@JvmField var leftLanes: Int = 0
	@JvmField var leftMaxPrio: Int = 0
	@JvmField var roadsOnLeft: Int = 0

	@JvmField var rightLanesInfo: MutableList<AttachedRoadInfo> = ArrayList()
	@JvmField var rightLanes: Int = 0
	@JvmField var rightMaxPrio: Int = 0
	@JvmField var roadsOnRight: Int = 0

	/** True when nothing here actually turns: every road leaves within the slight-turn margin. */
	fun allAreStraight(): Boolean {
		for (angle in leftLanesInfo) {
			if (abs(angle.attachedAngle) > TURN_SLIGHT_DEGREE) {
				return false
			}
		}
		for (angle in rightLanesInfo) {
			if (abs(angle.attachedAngle) > TURN_SLIGHT_DEGREE) {
				return false
			}
		}
		return true
	}

	/** One road leaving the intersection, as the turn logic needs to see it. */
	class AttachedRoadInfo {
		@JvmField var parsedLanes: IntArray? = null
		@JvmField var attachedAngle: Double = 0.0
		@JvmField var lanes: Int = 0
		@JvmField var speakPriority: Int = 0
		@JvmField var attachedOnTheRight: Boolean = false
		@JvmField var turnType: Int = 0
	}

	companion object {
		/** java had this as a float 5, widened to a double at every comparison below. */
		private const val TURN_SLIGHT_DEGREE = 5.0
	}
}
