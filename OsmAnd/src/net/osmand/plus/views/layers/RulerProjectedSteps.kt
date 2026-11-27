package net.osmand.plus.views.layers

import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max

/**
 * Limit for the screen step between neighbour ruler points that still belong to one line.
 * Relief close to the camera and the map tilt enlarge a projected circle far beyond its flat
 * radius, so the limit also follows the typical step of the projected circle itself.
 * A globe projection discontinuity is one or two steps much longer than the rest.
 */
internal class RulerProjectedSteps(capacity: Int) {

	private val steps = FloatArray(capacity)

	fun maxStep(points: Array<PointF?>, count: Int, minStep: Float, flatStep: Float, slack: Float): Float {
		var stepsCount = 0
		for (i in 1 until count) {
			val previous = points[i - 1]
			val current = points[i]
			if (previous != null && current != null) {
				steps[stepsCount++] = hypot(current.x - previous.x, current.y - previous.y)
			}
		}
		var limit = max(minStep, flatStep * slack)
		if (stepsCount > 0) {
			steps.sort(0, stepsCount)
			limit = max(limit, steps[stepsCount / 2] * slack)
		}
		return limit
	}
}