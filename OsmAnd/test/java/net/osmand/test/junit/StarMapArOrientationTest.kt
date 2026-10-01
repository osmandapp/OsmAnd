package net.osmand.test.junit

import android.hardware.SensorManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import net.osmand.plus.plugins.astronomy.utils.StarMapArOrientation
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Checks the AR-mode orientation math against SensorManager and against hand-computed poses.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class StarMapArOrientationTest {

	private val eps = 1e-3

	/** Quaternion for a rotation of [deg] degrees about the unit axis (x, y, z). */
	private fun axisAngle(x: Float, y: Float, z: Float, deg: Double): FloatArray {
		val half = Math.toRadians(deg) / 2.0
		val s = sin(half).toFloat()
		return floatArrayOf(x * s, y * s, z * s, cos(half).toFloat())
	}

	private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
		val out = FloatArray(9)
		for (r in 0 until 3) for (c in 0 until 3) {
			out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c]
		}
		return out
	}

	@Test
	fun quaternionToMatrixMatchesSensorManager() {
		val q = axisAngle(1f / sqrt(3f), 1f / sqrt(3f), 1f / sqrt(3f), 70.0)
		val expected = FloatArray(9)
		SensorManager.getRotationMatrixFromVector(expected, q)
		val actual = FloatArray(9)
		StarMapArOrientation.quaternionToMatrix(q, actual)
		for (i in 0 until 9) assertEquals("m[$i]", expected[i].toDouble(), actual[i].toDouble(), eps)
	}

	@Test
	fun matrixToQuaternionRoundTrip() {
		for (deg in listOf(10.0, 95.0, 170.0, 250.0)) {
			val q = axisAngle(0.6f, 0f, 0.8f, deg)
			val m = FloatArray(9)
			StarMapArOrientation.quaternionToMatrix(q, m)
			val back = FloatArray(4)
			StarMapArOrientation.matrixToQuaternion(m, back)
			assertEquals("angle $deg", 0.0, StarMapArOrientation.angleBetween(q, back), eps)
		}
	}

	@Test
	fun slerpEndpointsAndMidpoint() {
		val a = axisAngle(0f, 0f, 1f, 0.0)
		val b = axisAngle(0f, 0f, 1f, 90.0)
		val out = FloatArray(4)
		StarMapArOrientation.slerp(a, b, 0f, out)
		assertEquals(0.0, StarMapArOrientation.angleBetween(a, out), eps)
		StarMapArOrientation.slerp(a, b, 1f, out)
		assertEquals(0.0, StarMapArOrientation.angleBetween(b, out), eps)
		StarMapArOrientation.slerp(a, b, 0.5f, out)
		assertEquals(45.0, StarMapArOrientation.angleBetween(a, out), eps)
		// Negated quaternion is the same rotation and must not produce a long-way interpolation.
		val minusB = floatArrayOf(-b[0], -b[1], -b[2], -b[3])
		StarMapArOrientation.slerp(a, minusB, 0.5f, out)
		assertEquals(45.0, StarMapArOrientation.angleBetween(a, out), eps)
	}

	@Test
	fun anglesOfCanonicalPoses() {
		// Identity: screen up, back camera looks straight down.
		val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
		val down = StarMapArOrientation.anglesFromMatrix(identity)
		assertEquals(-90.0, down.altitude, eps)

		// Phone held upright facing north: rotate the device 90 degrees about X.
		val upright = FloatArray(9)
		StarMapArOrientation.quaternionToMatrix(axisAngle(1f, 0f, 0f, 90.0), upright)
		val north = StarMapArOrientation.anglesFromMatrix(upright)
		assertEquals(0.0, north.azimuth, eps)
		assertEquals(0.0, north.altitude, eps)
		assertEquals(0.0, north.roll, eps)

		// Same pose turned to face east: additional rotation about the world Z axis by -90 degrees.
		val rz = FloatArray(9)
		StarMapArOrientation.quaternionToMatrix(axisAngle(0f, 0f, 1f, -90.0), rz)
		val east = StarMapArOrientation.anglesFromMatrix(multiply(rz, upright))
		assertEquals(90.0, east.azimuth, eps)
		assertEquals(0.0, east.altitude, eps)
	}

	@Test
	fun rotateAzimuthShiftsAzimuthOnly() {
		val upright = FloatArray(9)
		StarMapArOrientation.quaternionToMatrix(axisAngle(1f, 0f, 0f, 90.0), upright)
		val tilted = FloatArray(9)
		StarMapArOrientation.quaternionToMatrix(axisAngle(1f, 0f, 0f, 60.0), tilted)
		for ((matrix, offset) in listOf(upright to 37.0, tilted to -120.0, upright to 350.0)) {
			val before = StarMapArOrientation.anglesFromMatrix(matrix)
			val rotated = FloatArray(9)
			StarMapArOrientation.rotateAzimuth(matrix, offset, rotated)
			val after = StarMapArOrientation.anglesFromMatrix(rotated)
			assertEquals(StarMapArOrientation.normalizeAzimuth(before.azimuth + offset), after.azimuth, eps)
			assertEquals(before.altitude, after.altitude, eps)
			assertEquals(before.roll, after.roll, eps)
		}
		// In-place rotation gives the same result.
		val inPlace = upright.copyOf()
		StarMapArOrientation.rotateAzimuth(inPlace, 37.0, inPlace)
		val separate = FloatArray(9)
		StarMapArOrientation.rotateAzimuth(upright, 37.0, separate)
		for (i in 0 until 9) assertEquals(separate[i].toDouble(), inPlace[i].toDouble(), eps)
	}

	@Test
	fun headingCorrectionRecoversYawOffset() {
		// A pose seen in the relative (game rotation vector) frame and the same pose in the absolute
		// frame that is yawed by a known amount. The correction must bring relative azimuths onto
		// absolute ones regardless of the pitch of the device.
		for (pitch in listOf(90.0, 45.0, 10.0)) {
			val relative = FloatArray(9)
			StarMapArOrientation.quaternionToMatrix(axisAngle(1f, 0f, 0f, pitch), relative)
			for (yaw in listOf(30.0, -75.0, 170.0)) {
				val rz = FloatArray(9)
				StarMapArOrientation.quaternionToMatrix(axisAngle(0f, 0f, 1f, -yaw), rz)
				val absolute = multiply(rz, relative)
				val correction = StarMapArOrientation.headingCorrectionDeg(absolute, relative)
				val corrected = FloatArray(9)
				StarMapArOrientation.rotateAzimuth(relative, correction, corrected)
				val expected = StarMapArOrientation.anglesFromMatrix(absolute)
				val actual = StarMapArOrientation.anglesFromMatrix(corrected)
				assertEquals("pitch $pitch yaw $yaw", expected.azimuth, actual.azimuth, eps)
				assertEquals("pitch $pitch yaw $yaw", expected.altitude, actual.altitude, eps)
			}
		}
	}

	@Test
	fun angleDeltaTakesShortestWay() {
		assertEquals(-20.0, StarMapArOrientation.angleDelta(10.0, 350.0), eps)
		assertEquals(20.0, StarMapArOrientation.angleDelta(350.0, 10.0), eps)
		assertEquals(180.0, StarMapArOrientation.angleDelta(0.0, 180.0), eps)
		assertEquals(5.0, StarMapArOrientation.normalizeAzimuth(365.0), eps)
		assertEquals(355.0, StarMapArOrientation.normalizeAzimuth(-5.0), eps)
	}
}
