package net.osmand.plus.plugins.astronomy.utils

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure math for the star map AR mode: quaternion smoothing, heading correction of a gyro-based
 * (game rotation vector) orientation against an absolute (magnetometer-fused) one, and extraction
 * of azimuth/altitude/roll from a device-to-world rotation matrix.
 *
 * Conventions follow android.hardware.SensorManager: rotation matrices are 3x3 row-major and map
 * device coordinates to world coordinates (X east, Y north, Z up). Quaternions are stored as
 * (x, y, z, w). Azimuth is measured clockwise from north, in degrees.
 */
object StarMapArOrientation {

	data class Angles(val azimuth: Double, val altitude: Double, val roll: Double)

	/** Copies a sensor rotation vector (3 or 4 values) into a normalized (x, y, z, w) quaternion. */
	fun quaternionFromRotationVector(values: FloatArray, out: FloatArray) {
		out[0] = values[0]
		out[1] = values[1]
		out[2] = values[2]
		out[3] = if (values.size >= 4) {
			values[3]
		} else {
			sqrt(max(0f, 1f - values[0] * values[0] - values[1] * values[1] - values[2] * values[2]))
		}
		normalize(out)
	}

	fun normalize(q: FloatArray) {
		val len = sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3])
		if (len > 0f) {
			q[0] /= len; q[1] /= len; q[2] /= len; q[3] /= len
		}
	}

	/** Angle between two unit quaternions in degrees, ignoring the sign ambiguity. */
	fun angleBetween(a: FloatArray, b: FloatArray): Double {
		val dot = min(1.0, abs((a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3]).toDouble()))
		return Math.toDegrees(2.0 * acos(dot))
	}

	/** Spherical interpolation from [from] towards [to] by [t] in 0..1. Result is written to [out]. */
	fun slerp(from: FloatArray, to: FloatArray, t: Float, out: FloatArray) {
		var dot = from[0] * to[0] + from[1] * to[1] + from[2] * to[2] + from[3] * to[3]
		var sign = 1f
		if (dot < 0f) {
			dot = -dot
			sign = -1f
		}
		val wFrom: Float
		val wTo: Float
		if (dot > 0.9995f) {
			wFrom = 1f - t
			wTo = t
		} else {
			val theta = acos(dot.toDouble())
			val sinTheta = sin(theta)
			wFrom = (sin((1.0 - t) * theta) / sinTheta).toFloat()
			wTo = (sin(t * theta) / sinTheta).toFloat()
		}
		out[0] = wFrom * from[0] + sign * wTo * to[0]
		out[1] = wFrom * from[1] + sign * wTo * to[1]
		out[2] = wFrom * from[2] + sign * wTo * to[2]
		out[3] = wFrom * from[3] + sign * wTo * to[3]
		normalize(out)
	}

	/** Converts an (x, y, z, w) unit quaternion to a 3x3 row-major rotation matrix. */
	fun quaternionToMatrix(q: FloatArray, out: FloatArray) {
		val x = q[0]; val y = q[1]; val z = q[2]; val w = q[3]
		val xx = x * x; val yy = y * y; val zz = z * z
		val xy = x * y; val xz = x * z; val yz = y * z
		val wx = w * x; val wy = w * y; val wz = w * z
		out[0] = 1f - 2f * (yy + zz); out[1] = 2f * (xy - wz); out[2] = 2f * (xz + wy)
		out[3] = 2f * (xy + wz); out[4] = 1f - 2f * (xx + zz); out[5] = 2f * (yz - wx)
		out[6] = 2f * (xz - wy); out[7] = 2f * (yz + wx); out[8] = 1f - 2f * (xx + yy)
	}

	/** Converts a 3x3 row-major rotation matrix to an (x, y, z, w) unit quaternion. */
	fun matrixToQuaternion(m: FloatArray, out: FloatArray) {
		val trace = m[0] + m[4] + m[8]
		if (trace > 0f) {
			val s = sqrt(trace + 1f) * 2f
			out[3] = 0.25f * s
			out[0] = (m[7] - m[5]) / s
			out[1] = (m[2] - m[6]) / s
			out[2] = (m[3] - m[1]) / s
		} else if (m[0] > m[4] && m[0] > m[8]) {
			val s = sqrt(1f + m[0] - m[4] - m[8]) * 2f
			out[3] = (m[7] - m[5]) / s
			out[0] = 0.25f * s
			out[1] = (m[1] + m[3]) / s
			out[2] = (m[2] + m[6]) / s
		} else if (m[4] > m[8]) {
			val s = sqrt(1f + m[4] - m[0] - m[8]) * 2f
			out[3] = (m[2] - m[6]) / s
			out[0] = (m[1] + m[3]) / s
			out[1] = 0.25f * s
			out[2] = (m[5] + m[7]) / s
		} else {
			val s = sqrt(1f + m[8] - m[0] - m[4]) * 2f
			out[3] = (m[3] - m[1]) / s
			out[0] = (m[2] + m[6]) / s
			out[1] = (m[5] + m[7]) / s
			out[2] = 0.25f * s
		}
		normalize(out)
	}

	/**
	 * Azimuth offset (degrees) that has to be added to azimuths derived from [relative] so that they
	 * match azimuths derived from [absolute]. Both matrices describe the same physical device pose:
	 * [relative] in the arbitrary-yaw frame of the game rotation vector, [absolute] in the
	 * magnetometer-referenced frame. Only the rotation about the vertical axis is considered.
	 */
	fun headingCorrectionDeg(absolute: FloatArray, relative: FloatArray): Double {
		// C = absolute * relative^T maps the relative world frame onto the absolute one.
		val c00 = absolute[0] * relative[0] + absolute[1] * relative[1] + absolute[2] * relative[2]
		val c10 = absolute[3] * relative[0] + absolute[4] * relative[1] + absolute[5] * relative[2]
		// For a pure rotation about Z, C = Rz(theta) with c00 = cos(theta), c10 = sin(theta).
		// Rotating the world frame by theta decreases the compass azimuth by theta.
		return -Math.toDegrees(atan2(c10.toDouble(), c00.toDouble()))
	}

	/**
	 * Rotates the world frame of [matrix] about the vertical axis so that azimuths derived from the
	 * result are larger by [azimuthOffsetDeg]. [out] may be the same array as [matrix].
	 */
	fun rotateAzimuth(matrix: FloatArray, azimuthOffsetDeg: Double, out: FloatArray) {
		val rad = Math.toRadians(azimuthOffsetDeg)
		val c = cos(rad).toFloat()
		val s = sin(rad).toFloat()
		for (i in 0 until 3) {
			val rx = matrix[i]
			val ry = matrix[3 + i]
			out[i] = c * rx + s * ry
			out[3 + i] = -s * rx + c * ry
		}
		if (out !== matrix) {
			out[6] = matrix[6]; out[7] = matrix[7]; out[8] = matrix[8]
		}
	}

	/**
	 * Azimuth and altitude of the direction the back camera looks at (device -Z axis) and the roll
	 * of the screen relative to the zenith, all in degrees.
	 */
	fun anglesFromMatrix(m: FloatArray): Angles {
		val vX = -m[2].toDouble()
		val vY = -m[5].toDouble()
		val vZ = -m[8].toDouble()
		var azimuth = Math.toDegrees(atan2(vX, vY))
		if (azimuth < 0) azimuth += 360.0
		val altitude = Math.toDegrees(asin(vZ.coerceIn(-1.0, 1.0)))
		val roll = Math.toDegrees(atan2(m[6].toDouble(), m[7].toDouble()))
		return Angles(azimuth, altitude, roll)
	}

	/** Shortest signed difference [target] - [current] for angles in degrees, in -180..180. */
	fun angleDelta(current: Double, target: Double): Double {
		var delta = (target - current) % 360.0
		if (delta > 180.0) delta -= 360.0
		if (delta < -180.0) delta += 360.0
		return delta
	}

	fun normalizeAzimuth(azimuth: Double): Double {
		var result = azimuth % 360.0
		if (result < 0) result += 360.0
		return result
	}
}
