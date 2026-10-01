package net.osmand.plus.plugins.astronomy.utils

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import net.osmand.Location
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.views.StarView
import kotlin.math.abs

/**
 * Drives the star map from the device sensors.
 *
 * Orientation comes from the game rotation vector (gyroscope + accelerometer), which is smooth and
 * immune to magnetic disturbances but has an arbitrary yaw. The absolute rotation vector
 * (magnetometer-fused) is only used to slowly correct that yaw, so magnetic noise never reaches the
 * picture directly. The user can additionally shift the azimuth by dragging in AR mode; that offset
 * is kept in [userHeadingOffset] and persisted by the owner.
 *
 * Devices without a game rotation vector fall back to the rotation vector alone, and devices without
 * any fused sensor fall back to accelerometer + magnetometer.
 */
class StarMapARModeHelper(
	private val context: Context,
	private val starView: StarView,
	private val onArModeChanged: (Boolean) -> Unit
) : SensorEventListener {

	var isArModeEnabled = false
		private set

	/** Manual azimuth correction in degrees, added on top of the sensor heading. */
	var userHeadingOffset = 0.0
		set(value) {
			field = StarMapArOrientation.angleDelta(0.0, value)
		}

	private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val gameRotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
	private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
	private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
	private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
	private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

	private val hasGameRotation = gameRotationSensor != null
	private val hasRotationVector = rotationSensor != null

	private val sampleQuaternion = FloatArray(4)
	private val smoothedQuaternion = FloatArray(4)
	private var hasSmoothedOrientation = false

	private val orientationMatrix = FloatArray(9)
	private val relativeMatrix = FloatArray(9)
	private val absoluteMatrix = FloatArray(9)
	private val remappedMatrix = FloatArray(9)
	private val accelerometerReading = FloatArray(3)
	private val magnetometerReading = FloatArray(3)
	private var hasAccelerometer = false
	private var hasMagnetometer = false
	private var relativeMatrixTime = 0L

	/** Yaw correction of the game rotation vector frame towards magnetic north, degrees. */
	private var headingCorrection = 0.0
	private var hasHeadingCorrection = false
	private var headingCorrectionStartTime = 0L

	@Volatile
	private var geomagneticField: GeomagneticField? = null

	private var lastAccuracyWarningTime = 0L

	fun onResume() {
		if (isArModeEnabled) {
			registerSensors()
		}
	}

	fun onPause() {
		unregisterSensors()
	}

	fun updateGeomagneticField(location: Location) {
		geomagneticField = GeomagneticField(
			location.latitude.toFloat(),
			location.longitude.toFloat(),
			location.altitude.toFloat(),
			System.currentTimeMillis()
		)
	}

	fun toggleArMode(enable: Boolean? = null) {
		val newState = enable ?: !isArModeEnabled
		if (isArModeEnabled == newState) return

		isArModeEnabled = newState
		if (isArModeEnabled) {
			registerSensors()
			Toast.makeText(context, context.getString(R.string.ar_mode_enabled), Toast.LENGTH_SHORT).show()
		} else {
			unregisterSensors()
			// Reset roll when exiting AR mode
			starView.roll = 0.0
			Toast.makeText(context, context.getString(R.string.ar_mode_disabled), Toast.LENGTH_SHORT).show()
		}
		onArModeChanged(isArModeEnabled)
	}

	/** Shifts the manual azimuth correction by [deltaDeg]. */
	fun adjustUserHeadingOffset(deltaDeg: Double) {
		userHeadingOffset += deltaDeg
	}

	fun resetUserHeadingOffset() {
		userHeadingOffset = 0.0
	}

	private fun registerSensors() {
		resetFilters()
		val rate = SensorManager.SENSOR_DELAY_GAME
		when {
			hasGameRotation -> {
				sensorManager.registerListener(this, gameRotationSensor, rate)
				if (hasRotationVector) {
					sensorManager.registerListener(this, rotationSensor, rate)
				}
			}
			hasRotationVector -> sensorManager.registerListener(this, rotationSensor, rate)
			accelerometerSensor != null && magneticSensor != null -> {
				sensorManager.registerListener(this, accelerometerSensor, rate)
				sensorManager.registerListener(this, magneticSensor, rate)
			}
			else -> {
				Toast.makeText(context, context.getString(R.string.sensors_not_available_for_ar), Toast.LENGTH_SHORT).show()
				isArModeEnabled = false
				onArModeChanged(false)
			}
		}
	}

	private fun unregisterSensors() {
		sensorManager.unregisterListener(this)
	}

	private fun resetFilters() {
		hasSmoothedOrientation = false
		hasHeadingCorrection = false
		hasAccelerometer = false
		hasMagnetometer = false
		relativeMatrixTime = 0L
	}

	override fun onSensorChanged(event: SensorEvent) {
		if (!isArModeEnabled) return

		when (event.sensor.type) {
			Sensor.TYPE_GAME_ROTATION_VECTOR -> {
				StarMapArOrientation.quaternionFromRotationVector(event.values, sampleQuaternion)
				StarMapArOrientation.quaternionToMatrix(sampleQuaternion, relativeMatrix)
				relativeMatrixTime = SystemClock.elapsedRealtime()
				smoothOrientation()
				render()
			}
			Sensor.TYPE_ROTATION_VECTOR -> {
				checkCompassAccuracy(event.accuracy)
				if (hasGameRotation) {
					updateHeadingCorrection(event)
				} else {
					StarMapArOrientation.quaternionFromRotationVector(event.values, sampleQuaternion)
					smoothOrientation()
					render()
				}
			}
			Sensor.TYPE_ACCELEROMETER -> {
				System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
				hasAccelerometer = true
				updateFromAccelerometerAndMagnetometer()
			}
			Sensor.TYPE_MAGNETIC_FIELD -> {
				checkCompassAccuracy(event.accuracy)
				System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
				hasMagnetometer = true
				updateFromAccelerometerAndMagnetometer()
			}
		}
	}

	private fun updateFromAccelerometerAndMagnetometer() {
		if (!hasAccelerometer || !hasMagnetometer) return
		if (SensorManager.getRotationMatrix(absoluteMatrix, null, accelerometerReading, magnetometerReading)) {
			StarMapArOrientation.matrixToQuaternion(absoluteMatrix, sampleQuaternion)
			smoothOrientation()
			render()
		}
	}

	private fun updateHeadingCorrection(event: SensorEvent) {
		// The correction compares the two fused sensors for the same pose, so both samples must be fresh.
		if (relativeMatrixTime == 0L || SystemClock.elapsedRealtime() - relativeMatrixTime > MAX_SAMPLE_AGE_MS) return
		val reliable = event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
				&& event.accuracy != SensorManager.SENSOR_STATUS_ACCURACY_LOW
		if (hasHeadingCorrection && !reliable) return

		SensorManager.getRotationMatrixFromVector(absoluteMatrix, event.values)
		val target = StarMapArOrientation.headingCorrectionDeg(absoluteMatrix, relativeMatrix)
		val now = SystemClock.elapsedRealtime()
		if (!hasHeadingCorrection) {
			headingCorrection = target
			hasHeadingCorrection = true
			headingCorrectionStartTime = now
			return
		}
		val delta = StarMapArOrientation.angleDelta(headingCorrection, target)
		val warmingUp = now - headingCorrectionStartTime < HEADING_WARMUP_MS
		val alpha = if (warmingUp || abs(delta) > HEADING_SNAP_THRESHOLD_DEG) HEADING_FAST_ALPHA else HEADING_SLOW_ALPHA
		headingCorrection = StarMapArOrientation.angleDelta(0.0, headingCorrection + delta * alpha)
	}

	private fun smoothOrientation() {
		if (!hasSmoothedOrientation) {
			System.arraycopy(sampleQuaternion, 0, smoothedQuaternion, 0, 4)
			hasSmoothedOrientation = true
			return
		}
		val angle = StarMapArOrientation.angleBetween(smoothedQuaternion, sampleQuaternion)
		val alpha = adaptiveAlpha(angle)
		StarMapArOrientation.slerp(smoothedQuaternion, sampleQuaternion, alpha, smoothedQuaternion)
	}

	private fun adaptiveAlpha(angleDeg: Double): Float {
		return when {
			angleDeg < JITTER_THRESHOLD_DEG -> MIN_ALPHA
			angleDeg > MOVE_THRESHOLD_DEG -> MAX_ALPHA
			else -> (MIN_ALPHA + (angleDeg - JITTER_THRESHOLD_DEG) * (MAX_ALPHA - MIN_ALPHA)
					/ (MOVE_THRESHOLD_DEG - JITTER_THRESHOLD_DEG)).toFloat()
		}
	}

	private fun render() {
		if (!hasSmoothedOrientation) return
		StarMapArOrientation.quaternionToMatrix(smoothedQuaternion, orientationMatrix)

		val axisX: Int
		val axisY: Int
		@Suppress("DEPRECATION")
		when (windowManager.defaultDisplay.rotation) {
			Surface.ROTATION_90 -> { axisX = SensorManager.AXIS_Y; axisY = SensorManager.AXIS_MINUS_X }
			Surface.ROTATION_180 -> { axisX = SensorManager.AXIS_MINUS_X; axisY = SensorManager.AXIS_MINUS_Y }
			Surface.ROTATION_270 -> { axisX = SensorManager.AXIS_MINUS_Y; axisY = SensorManager.AXIS_X }
			else -> { axisX = SensorManager.AXIS_X; axisY = SensorManager.AXIS_Y }
		}
		SensorManager.remapCoordinateSystem(orientationMatrix, axisX, axisY, remappedMatrix)

		var azimuthOffset = userHeadingOffset + (geomagneticField?.declination?.toDouble() ?: 0.0)
		if (hasGameRotation && hasHeadingCorrection) {
			azimuthOffset += headingCorrection
		}
		StarMapArOrientation.rotateAzimuth(remappedMatrix, azimuthOffset, remappedMatrix)

		val angles = StarMapArOrientation.anglesFromMatrix(remappedMatrix)
		starView.setCenter(angles.azimuth, angles.altitude)
		starView.roll = angles.roll
	}

	private fun checkCompassAccuracy(accuracy: Int) {
		if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
			val currentTime = System.currentTimeMillis()
			if (currentTime - lastAccuracyWarningTime > ACCURACY_WARNING_INTERVAL_MS) {
				Toast.makeText(context, context.getString(R.string.compass_calibration_needed), Toast.LENGTH_SHORT).show()
				lastAccuracyWarningTime = currentTime
			}
		}
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
		// No-op
	}

	companion object {
		// Orientation smoothing: slerp weight grows with the angular step so small jitter is damped
		// while real movement is followed closely.
		private const val MIN_ALPHA = 0.08f
		private const val MAX_ALPHA = 0.5f
		private const val JITTER_THRESHOLD_DEG = 0.3
		private const val MOVE_THRESHOLD_DEG = 2.0

		// Heading correction: snap quickly right after start or after a large disagreement,
		// otherwise follow the compass slowly so magnetic disturbances do not shake the picture.
		private const val HEADING_WARMUP_MS = 3000L
		private const val HEADING_SNAP_THRESHOLD_DEG = 25.0
		private const val HEADING_FAST_ALPHA = 0.3
		private const val HEADING_SLOW_ALPHA = 0.01
		private const val MAX_SAMPLE_AGE_MS = 250L

		private const val ACCURACY_WARNING_INTERVAL_MS = 10000L
	}
}
