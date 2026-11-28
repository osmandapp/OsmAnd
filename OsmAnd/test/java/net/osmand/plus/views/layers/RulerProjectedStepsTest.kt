package net.osmand.plus.views.layers

import android.graphics.PointF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class RulerProjectedStepsTest {

	@Test
	fun circleEnlargedByReliefKeepsAllSteps() {
		// 400 m circle drawn near Mestia: camera 300-500 m above the slope, flat radius 300 px
		val points = closedCircle(MESTIA_400M_CIRCLE)
		val limit = maxStep(points, flatRadius = 300f)

		assertTrue("longest step must stay in the line", longestStep(points) <= limit)
	}

	@Test
	fun globeSeamStillBreaksTheLine() {
		val points = closedCircle(regularCircle(radius = 300f, center = PointF(540f, 1200f)))
		points[30] = PointF(points[30]!!.x + 2000f, points[30]!!.y)

		val limit = maxStep(points, flatRadius = 300f)

		val brokenSteps = (1 until points.size).filter { stepLength(points, it) > limit }
		assertEquals(listOf(30, 31), brokenSteps)
	}

	@Test
	fun flatLimitStaysWhenCircleIsSmallerThanExpected() {
		val points = closedCircle(regularCircle(radius = 50f, center = PointF(540f, 1200f)))

		assertEquals(MIN_STEP, maxStep(points, flatRadius = 50f), 0.001f)
		assertEquals(flatStep(600f) * SLACK, maxStep(points, flatRadius = 600f), 0.001f)
	}

	@Test
	fun missingPointsGiveFlatLimit() {
		val points = arrayOfNulls<PointF>(CIRCLE_SAMPLES + 1)

		assertEquals(flatStep(300f) * SLACK, maxStep(points, flatRadius = 300f), 0.001f)
	}

	private fun maxStep(points: Array<PointF?>, flatRadius: Float): Float =
		RulerProjectedSteps(CIRCLE_SAMPLES + 1).maxStep(points, points.size, MIN_STEP, flatStep(flatRadius), SLACK)

	private fun flatStep(radius: Float): Float = (2 * radius * sin(Math.toRadians(STEP_DEGREES / 2))).toFloat()

	private fun closedCircle(points: List<PointF>): Array<PointF?> =
		arrayOfNulls<PointF>(points.size + 1).also { array ->
			points.forEachIndexed { i, point -> array[i] = point }
			array[points.size] = points[0]
		}

	private fun regularCircle(radius: Float, center: PointF): List<PointF> = (0 until CIRCLE_SAMPLES).map { i ->
		val angle = Math.toRadians(i * STEP_DEGREES)
		PointF(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
	}

	private fun longestStep(points: Array<PointF?>): Float = (1 until points.size).maxOf { stepLength(points, it) }

	private fun stepLength(points: Array<PointF?>, index: Int): Float {
		val previous = points[index - 1]!!
		val current = points[index]!!
		return hypot(current.x - previous.x, current.y - previous.y)
	}

	private companion object {
		const val CIRCLE_SAMPLES = 72
		const val STEP_DEGREES = 5.0
		const val MIN_STEP = 63f
		const val SLACK = 4f

		val MESTIA_400M_CIRCLE = listOf(
			570 to 1336, 459 to 1363, 349 to 1374, 242 to 1372, 140 to 1366, 44 to 1384, -40 to 1437,
			-117 to 1488, -184 to 1529, -243 to 1564, -293 to 1580, -338 to 1547, -380 to 1465, -415 to 1359,
			-442 to 1255, -460 to 1155, -469 to 1054, -470 to 961, -462 to 875, -446 to 799, -423 to 731,
			-394 to 658, -359 to 584, -319 to 519, -273 to 460, -221 to 422, -164 to 404, -103 to 402,
			-39 to 410, 27 to 422, 94 to 431, 163 to 434, 233 to 420, 303 to 394, 374 to 358, 446 to 313,
			519 to 267, 593 to 219, 667 to 182, 741 to 159, 815 to 132, 889 to 102, 962 to 73, 1034 to 45,
			1105 to 18, 1174 to -5, 1240 to -16, 1302 to -7, 1360 to 11, 1413 to 44, 1460 to 96, 1500 to 159,
			1534 to 237, 1560 to 324, 1578 to 433, 1588 to 545, 1592 to 632, 1591 to 702, 1583 to 763,
			1564 to 829, 1535 to 911, 1498 to 984, 1453 to 1043, 1399 to 1090, 1337 to 1120, 1265 to 1148,
			1185 to 1179, 1097 to 1194, 1002 to 1199, 901 to 1202, 793 to 1237, 682 to 1292
		).map { (x, y) -> PointF(x.toFloat(), y.toFloat()) }
	}
}