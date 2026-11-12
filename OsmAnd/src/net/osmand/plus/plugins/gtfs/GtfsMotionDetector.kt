package net.osmand.plus.plugins.gtfs

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.sqrt

/**
 * A lightweight, untuned guess whether the phone is in a vehicle, from the accelerometer and the step detector.
 * Every [WINDOW_MS] it reports a likelihood 0..1: recent steps mean walking (low), smooth low-amplitude vibration
 * with no steps means a vehicle (high). Supporting evidence only; GPS speed and the timetable decide. Runs only
 * between [start] and [stop].
 */
class GtfsMotionDetector(context: Context, private val onLikelihood: (Double) -> Unit) : SensorEventListener {

	private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private var sumSq = 0.0
	private var samples = 0
	private var windowStart = 0L
	private var lastStepMs = Long.MIN_VALUE
	private var stepsInWindow = 0
	private var running = false

	fun start() {
		if (running) {
			return
		}
		val accel = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
			?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
		val steps = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
		try {
			accel?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
			steps?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
		} catch (e: SecurityException) {
			Log.w(TAG, "motion sensors not permitted", e)
		}
		running = accel != null
		windowStart = SystemClock.elapsedRealtime()
		Log.i(TAG, "motion detector started accel=${accel?.name} steps=${steps?.name}")
	}

	fun stop() {
		if (running) {
			manager.unregisterListener(this)
			running = false
		}
	}

	override fun onSensorChanged(event: SensorEvent) {
		val now = SystemClock.elapsedRealtime()
		if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
			lastStepMs = now
			stepsInWindow++
			return
		}
		var mag = sqrt((event.values[0] * event.values[0] + event.values[1] * event.values[1] +
			event.values[2] * event.values[2]).toDouble())
		if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
			mag -= SensorManager.GRAVITY_EARTH
		}
		sumSq += mag * mag
		samples++
		if (now - windowStart >= WINDOW_MS) {
			val rms = if (samples > 0) sqrt(sumSq / samples) else 0.0
			val walking = stepsInWindow >= 3 || now - lastStepMs < 3000
			val likelihood = when {
				walking -> 0.1
				rms < 0.03 -> 0.3 // lying still
				rms < 1.0 -> 0.75 // smooth vibration, no steps
				else -> 0.4
			}
			Log.i(TAG, "motion rms=$rms steps=$stepsInWindow likelihood=$likelihood")
			sumSq = 0.0
			samples = 0
			stepsInWindow = 0
			windowStart = now
			onLikelihood(likelihood)
		}
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	private companion object {
		const val TAG = "GtfsAuto"
		const val WINDOW_MS = 4000L
	}
}
