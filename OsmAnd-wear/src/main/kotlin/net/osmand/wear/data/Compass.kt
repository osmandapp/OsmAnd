package net.osmand.wear.data

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import net.osmand.wear.api.LocationState

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the watch is pointing, in degrees from true north, or null when it has no compass.
 *
 * The sensor reports magnetic north; the phone's heading is corrected for declination before it
 * reaches OsmAnd's own widgets, so the same correction is applied here or the two would disagree
 * by up to a dozen degrees depending on where in the world the user is standing.
 *
 * Registers while the calling screen is composed and drops the listener when it leaves, because
 * a rotation vector sensor left running is one of the more expensive things on a watch.
 */
@Composable
fun rememberWatchHeading(location: LocationState?): State<Float?> {
	val context = LocalContext.current
	val heading = remember { mutableStateOf<Float?>(null) }

	// Keyed on a coarse position, not the live one: declination moves by a degree over something
	// like a hundred kilometres, while the phone republishes its position constantly, and every
	// key change would tear the sensor down and restart the smoothing from scratch.
	val region = location?.let { RegionKey(round(it.latitude), round(it.longitude)) }
	DisposableEffect(context, region) {
		val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
		val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
		if (manager == null || sensor == null) {
			heading.value = null
			return@DisposableEffect onDispose { }
		}

		val declination = region?.let {
			GeomagneticField(
				it.latitude.toFloat(), it.longitude.toFloat(), 0f, System.currentTimeMillis()
			).declination
		} ?: 0f

		val listener = HeadingListener(declination) { heading.value = it }
		manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
		onDispose { manager.unregisterListener(listener) }
	}
	return heading
}

private data class RegionKey(val latitude: Double, val longitude: Double)

private fun round(degrees: Double): Double = kotlin.math.round(degrees * 2) / 2

private class HeadingListener(
	private val declination: Float,
	private val onHeading: (Float) -> Unit
) : SensorEventListener {

	private val rotation = FloatArray(9)
	private val orientation = FloatArray(3)
	private var smoothedSin = 0f
	private var smoothedCos = 0f
	private var started = false
	private var published = 0f

	override fun onSensorChanged(event: SensorEvent) {
		SensorManager.getRotationMatrixFromVector(rotation, event.values)
		SensorManager.getOrientation(rotation, orientation)
		val radians = orientation[0] + Math.toRadians(declination.toDouble()).toFloat()

		// Averaged as a vector rather than as an angle: a heading wobbling around north would
		// otherwise average 0 and 359 into 180 and swing the arrow to the opposite side.
		val nextSin = sin(radians)
		val nextCos = cos(radians)
		if (started) {
			smoothedSin += (nextSin - smoothedSin) * SMOOTHING
			smoothedCos += (nextCos - smoothedCos) * SMOOTHING
		} else {
			smoothedSin = nextSin
			smoothedCos = nextCos
			started = true
		}

		val degrees = normalize(Math.toDegrees(atan2(smoothedSin, smoothedCos).toDouble()).toFloat())
		if (abs(shortestTurn(degrees - published)) >= MIN_CHANGE_DEGREES) {
			published = degrees
			onHeading(degrees)
		}
	}

	override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
	}

	private fun normalize(degrees: Float): Float = (degrees % 360f + 360f) % 360f

	private fun shortestTurn(delta: Float): Float {
		val wrapped = normalize(delta)
		return if (wrapped > 180f) wrapped - 360f else wrapped
	}

	private companion object {
		const val SMOOTHING = 0.2f
		const val MIN_CHANGE_DEGREES = 1.5f
	}
}
