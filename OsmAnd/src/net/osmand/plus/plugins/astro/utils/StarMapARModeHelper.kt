package net.osmand.plus.plugins.astro.utils

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import net.osmand.Location
import net.osmand.plus.plugins.astro.views.StarView
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

class StarMapARModeHelper(
	private val context: Context,
	private val starView: StarView,
	private val onArModeChanged: (Boolean) -> Unit
) : SensorEventListener {

	var isArModeEnabled = false
		private set

	private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private var sensorRotation: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
	private var sensorAccelerometer: Sensor? = null
	private var sensorMagnetic: Sensor? = null

	private val rotationMatrix = FloatArray(9)
	private val remappedRotationMatrix = FloatArray(9)
	private val accelerometerReading = FloatArray(3)
	private val magnetometerReading = FloatArray(3)

	private var hasAccelerometer = false
	private var hasMagnetometer = false
	private var geomagneticField: GeomagneticField? = null

	// Low pass filter for smoothing
	private var smoothedAzimuth = 0.0
	private var smoothedAltitude = 45.0

	// Adaptive Smoothing
	private val minAlpha = 0.03
	private val maxAlpha = 0.3
	private val jitterThresh = 0.5
	private val moveThresh = 2.0

	private var lastAccuracyWarningTime = 0L

	init {
		if (sensorRotation == null) {
			sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
			sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
		}
	}

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
			Toast.makeText(context, "AR Mode Enabled", Toast.LENGTH_SHORT).show()
		} else {
			unregisterSensors()
			// Reset roll when exiting AR mode
			starView.roll = 0.0
			Toast.makeText(context, "AR Mode Disabled", Toast.LENGTH_SHORT).show()
		}
		onArModeChanged(isArModeEnabled)
	}

	private fun registerSensors() {
		if (sensorRotation != null) {
			sensorManager.registerListener(this, sensorRotation, SensorManager.SENSOR_DELAY_GAME)
		} else if (sensorAccelerometer != null && sensorMagnetic != null) {
			sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME)
			sensorManager.registerListener(this, sensorMagnetic, SensorManager.SENSOR_DELAY_GAME)
		} else {
			Toast.makeText(context, "Sensors not available for AR", Toast.LENGTH_SHORT).show()
			isArModeEnabled = false
			onArModeChanged(false)
		}
	}

	private fun unregisterSensors() {
		sensorManager.unregisterListener(this)
	}

	override fun onSensorChanged(event: SensorEvent) {
		if (!isArModeEnabled) return

		var success = false
		when (event.sensor.type) {
			Sensor.TYPE_ROTATION_VECTOR -> {
				SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
				success = true
			}
			Sensor.TYPE_ACCELEROMETER -> {
				System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
				hasAccelerometer = true
			}
			Sensor.TYPE_MAGNETIC_FIELD -> {
				System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
				hasMagnetometer = true
			}
		}

		if (hasAccelerometer && hasMagnetometer && event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
			success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
		}

		if (success) {
			val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
			val rotation = windowManager.defaultDisplay.rotation

			var axisX = SensorManager.AXIS_X
			var axisY = SensorManager.AXIS_Y

			when (rotation) {
				Surface.ROTATION_0 -> {
					axisX = SensorManager.AXIS_X
					axisY = SensorManager.AXIS_Y
				}
				Surface.ROTATION_90 -> {
					axisX = SensorManager.AXIS_Y
					axisY = SensorManager.AXIS_MINUS_X
				}
				Surface.ROTATION_180 -> {
					axisX = SensorManager.AXIS_MINUS_X
					axisY = SensorManager.AXIS_MINUS_Y
				}
				Surface.ROTATION_270 -> {
					axisX = SensorManager.AXIS_MINUS_Y
					axisY = SensorManager.AXIS_X
				}
			}

			SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedRotationMatrix)

			val vX = -remappedRotationMatrix[2]
			val vY = -remappedRotationMatrix[5]
			val vZ = -remappedRotationMatrix[8]

			val azimuthRad = atan2(vX.toDouble(), vY.toDouble())
			val altitudeRad = asin(vZ.toDouble())

			var azimuthDeg = Math.toDegrees(azimuthRad)
			val altitudeDeg = Math.toDegrees(altitudeRad)

			if (azimuthDeg < 0) azimuthDeg += 360

			if (geomagneticField != null) {
				azimuthDeg += geomagneticField!!.declination
			}
			if (azimuthDeg >= 360) azimuthDeg -= 360
			if (azimuthDeg < 0) azimuthDeg += 360

			val azDiff = azimuthDeg - smoothedAzimuth
			var azDelta = azDiff
			if (azDiff > 180) azDelta = azDiff - 360
			else if (azDiff < -180) azDelta = azDiff + 360

			// Adaptive Smoothing for Azimuth
			val alphaAz = calculateAdaptiveAlpha(azDelta)
			smoothedAzimuth += azDelta * alphaAz
			if (smoothedAzimuth >= 360) smoothedAzimuth -= 360
			if (smoothedAzimuth < 0) smoothedAzimuth += 360

			// Adaptive Smoothing for Altitude
			val altDelta = altitudeDeg - smoothedAltitude
			val alphaAlt = calculateAdaptiveAlpha(altDelta)
			smoothedAltitude += altDelta * alphaAlt

			// Calculate Roll (Projected Zenith angle on Screen)
			val zenithX = remappedRotationMatrix[6]
			val zenithY = remappedRotationMatrix[7]
			val rollDeg = Math.toDegrees(atan2(zenithX.toDouble(), zenithY.toDouble()))

			starView.setCenter(smoothedAzimuth, smoothedAltitude)
			starView.roll = rollDeg

			// Compass Calibration Check
			if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
				if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
					val currentTime = System.currentTimeMillis()
					if (currentTime - lastAccuracyWarningTime > 10000) { // Warn every 10s max
						Toast.makeText(context, "Compass calibration needed", Toast.LENGTH_SHORT).show()
						lastAccuracyWarningTime = currentTime
					}
				}
			}
		}
	}

	private fun calculateAdaptiveAlpha(delta: Double): Double {
		val absDelta = abs(delta)
		return when {
			absDelta < jitterThresh -> minAlpha // Very stable for jitter
			absDelta > moveThresh -> maxAlpha   // Fast response
			else -> minAlpha + (absDelta - jitterThresh) * (maxAlpha - minAlpha) / (moveThresh - jitterThresh)
		}
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
		// No-op
	}
}