package net.osmand.plus.simulation

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import net.osmand.Location
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER
import net.osmand.util.MapUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Manually driven location simulation: instead of replaying a recorded track it moves
 * a virtual vehicle over the map according to throttle / brake / steering input.
 *
 * The vehicle is driven with a hardware keyboard: the arrow keys drive it and
 * Q / E (rotate), W / S (tilt), A / D (zoom) control the camera. The R key switches the gear.
 *
 * Locations are pushed to [net.osmand.plus.OsmAndLocationProvider.setLocationFromSimulation],
 * so all the regular consumers (widgets, auto zoom, track recording, routing) work as usual.
 */
class DriveSimulationEngine(private val app: OsmandApplication) : KeyEvent.Callback {

	private val handler = Handler(Looper.getMainLooper())

	private var latitude = 0.0
	private var longitude = 0.0

	/** Direction the vehicle is facing, degrees clockwise from north. */
	var heading = 0f
		private set

	/** Signed speed in m/s, negative while driving in reverse gear. */
	var speed = 0f
		private set

	var running = false
		private set

	private var savedMapRotationMode: Int? = null

	private val throttleInput = ControlInput()
	private val brakeInput = ControlInput()
	private val steeringInput = ControlInput()

	/** Throttle pedal, 0..1. */
	val throttle: Float get() = throttleInput.value

	/** Brake pedal, 0..1. */
	val brake: Float get() = brakeInput.value

	/** Steering wheel, -1 (full left) .. 1 (full right). */
	val steering: Float get() = steeringInput.value

	fun setThrottlePressed(pressed: Boolean) = throttleInput.set(if (pressed) 1f else 0f)

	fun setBrakePressed(pressed: Boolean) = brakeInput.set(if (pressed) 1f else 0f)

	/** -1 to steer left, 1 to steer right, 0 to release the wheel. */
	fun setSteering(value: Float) = steeringInput.set(value.coerceIn(-1f, 1f))

	/** Reverse gear: throttle moves the vehicle backwards. */
	var reverseGear = false
		set(value) {
			// Do not allow to change the gear while the vehicle is rolling
			if (abs(speed) < GEAR_SWITCH_MAX_SPEED) {
				field = value
			}
		}

	private val tickRunnable = object : Runnable {
		override fun run() {
			if (!running) {
				return
			}
			update()
			handler.postDelayed(this, TICK_INTERVAL_MS)
		}
	}

	/**
	 * Starts driving from [startLocation], from the last known location or, if there is none,
	 * from the center of the map.
	 */
	fun start(startLocation: Location?) {
		if (running) {
			return
		}
		val mapView = app.osmandMap.mapView
		val location = startLocation ?: app.locationProvider.lastKnownLocation
		if (location != null) {
			latitude = location.latitude
			longitude = location.longitude
			heading = if (location.hasBearing()) location.bearing else 0f
		} else {
			latitude = mapView.latitude
			longitude = mapView.longitude
			heading = 0f
		}
		speed = 0f
		resetControls()
		running = true
		publishLocation()
		// Keep the map following the vehicle right after the start
		val trackingUtilities = app.mapViewTrackingUtilities
		trackingUtilities.setMapLinkedToLocation(true)
		trackingUtilities.backToLocationImpl(mapView.zoom, false)
		attachKeyListener()
		handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
	}

	/**
	 * Starts to listen to the hardware keyboard. Called on every map activity resume,
	 * because the callback may be taken over by other screens.
	 */
	fun attachKeyListener() {
		app.keyEventHelper.setExternalCallback(this)
	}

	/**
	 * Switches the map to the manual rotation, otherwise the map rotation set from the camera
	 * controls is immediately reset back by the map tracking on the next simulated location.
	 * The previous mode is restored when the simulation is stopped.
	 */
	fun enableManualMapRotation() {
		val settings = app.settings
		val rotationMode = settings.ROTATE_MAP.get()
		if (rotationMode != OsmandSettings.ROTATE_MAP_MANUAL) {
			savedMapRotationMode = rotationMode
			settings.ROTATE_MAP.set(OsmandSettings.ROTATE_MAP_MANUAL)
		}
	}

	private fun restoreMapRotationMode() {
		savedMapRotationMode?.let {
			app.settings.ROTATE_MAP.set(it)
			savedMapRotationMode = null
		}
	}

	fun stop() {
		if (!running) {
			return
		}
		restoreMapRotationMode()
		app.keyEventHelper.setExternalCallback(null)
		running = false
		handler.removeCallbacks(tickRunnable)
		resetControls()
		speed = 0f
	}

	fun resetControls() {
		throttleInput.reset()
		brakeInput.reset()
		steeringInput.reset()
	}

	private fun update() {
		updateSpeed()
		updateHeading()
		updatePosition()
		// A short tap is shorter than the tick interval, so every press is applied at least once
		throttleInput.onTickApplied()
		brakeInput.onTickApplied()
		steeringInput.onTickApplied()
		publishLocation()
	}

	private fun updateSpeed() {
		val gearDirection = if (reverseGear) -1f else 1f
		if (throttle > 0) {
			speed += gearDirection * throttle * THROTTLE_ACCELERATION * TICK_SECONDS
		}
		val deceleration = if (brake > 0) {
			brake * BRAKE_DECELERATION
		} else if (throttle == 0f) {
			ROLLING_DECELERATION
		} else {
			0f
		}
		if (deceleration > 0) {
			val delta = deceleration * TICK_SECONDS
			speed = if (speed > 0) max(0f, speed - delta) else min(0f, speed + delta)
		}
		speed = speed.coerceIn(-MAX_REVERSE_SPEED, MAX_FORWARD_SPEED)
	}

	private fun updateHeading() {
		if (steering == 0f) {
			return
		}
		val absSpeed = abs(speed)
		// A standing vehicle still can be turned slowly, otherwise it would be impossible
		// to choose the direction before starting to drive
		val speedFactor = MIN_TURN_FACTOR +
				(1 - MIN_TURN_FACTOR) * min(1f, absSpeed / FULL_TURN_RATE_SPEED)
		// Above that speed the turn rate is damped, otherwise the vehicle spins on the spot
		val dampingFactor = if (absSpeed > FULL_TURN_RATE_SPEED) {
			sqrt(FULL_TURN_RATE_SPEED / absSpeed)
		} else {
			1f
		}
		// Steering a reversing vehicle turns it to the opposite side
		val gearFactor = if (speed < 0) -1f else 1f
		heading = MapUtils.normalizeDegrees360(heading +
				steering * MAX_TURN_RATE * speedFactor * dampingFactor * gearFactor * TICK_SECONDS)
	}

	private fun updatePosition() {
		if (speed == 0f) {
			return
		}
		val distance = abs(speed) * TICK_SECONDS
		val course = if (speed > 0) heading else MapUtils.normalizeDegrees360(heading + 180)
		val latLon = MapUtils.rhumbDestinationPoint(latitude, longitude, distance.toDouble(), course.toDouble())
		latitude = latLon.latitude
		longitude = latLon.longitude
	}

	private fun publishLocation() {
		val location = SimulatedLocation(Location(SIMULATED_PROVIDER, latitude, longitude), SIMULATED_PROVIDER)
		location.time = System.currentTimeMillis()
		location.speed = abs(speed)
		location.accuracy = LOCATION_ACCURACY
		// Report the direction the vehicle is facing, so that the map is not flipped while reversing
		location.bearing = heading
		app.locationProvider.setLocationFromSimulation(location)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		when (keyCode) {
			// Vehicle
			KeyEvent.KEYCODE_DPAD_UP -> setThrottlePressed(true)
			KeyEvent.KEYCODE_DPAD_DOWN -> setBrakePressed(true)
			KeyEvent.KEYCODE_DPAD_LEFT -> setSteering(-1f)
			KeyEvent.KEYCODE_DPAD_RIGHT -> setSteering(1f)
			KeyEvent.KEYCODE_R -> if (isFirstKeyPress(event)) {
				reverseGear = !reverseGear
			}

			// Camera. Rotation and tilt are repeatable, so that a held key moves the camera
			KeyEvent.KEYCODE_Q -> rotateCamera(-CAMERA_ROTATION_STEP)
			KeyEvent.KEYCODE_E -> rotateCamera(CAMERA_ROTATION_STEP)
			KeyEvent.KEYCODE_W -> tiltCamera(CAMERA_TILT_STEP)
			KeyEvent.KEYCODE_S -> tiltCamera(-CAMERA_TILT_STEP)
			KeyEvent.KEYCODE_A -> if (isFirstKeyPress(event)) {
				app.osmandMap.mapView.changeZoomManually(1)
			}

			KeyEvent.KEYCODE_D -> if (isFirstKeyPress(event)) {
				app.osmandMap.mapView.changeZoomManually(-1)
			}

			else -> return false
		}
		return true
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		when (keyCode) {
			KeyEvent.KEYCODE_DPAD_UP -> setThrottlePressed(false)
			KeyEvent.KEYCODE_DPAD_DOWN -> setBrakePressed(false)
			KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> setSteering(0f)
			KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_S,
			KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_R -> Unit

			else -> return false
		}
		return true
	}

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

	override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false

	private fun isFirstKeyPress(event: KeyEvent?) = event == null || event.repeatCount == 0

	private fun rotateCamera(degrees: Float) {
		enableManualMapRotation()
		val mapView = app.osmandMap.mapView
		mapView.setRotate(mapView.rotate + degrees, true)
	}

	private fun tiltCamera(degrees: Float) {
		val mapView = app.osmandMap.mapView
		val angle = mapView.normalizeElevationAngle(mapView.elevationAngle + degrees)
		app.settings.setLastKnownMapElevation(angle)
		mapView.animatedDraggingThread.startTilting(angle, 0f)
		mapView.refreshMap()
	}

	/**
	 * Control input that is guaranteed to be applied by at least one simulation tick,
	 * so that short taps and key presses are not lost between the ticks.
	 */
	private class ControlInput {

		var value = 0f
			private set

		private var applied = true
		private var releasePending = false

		fun set(newValue: Float) {
			if (newValue == 0f) {
				if (applied) {
					value = 0f
				} else {
					releasePending = true
				}
			} else {
				value = newValue
				applied = false
				releasePending = false
			}
		}

		fun onTickApplied() {
			applied = true
			if (releasePending) {
				value = 0f
				releasePending = false
			}
		}

		fun reset() {
			value = 0f
			applied = true
			releasePending = false
		}
	}

	companion object {
		private const val TICK_INTERVAL_MS = 100L
		private const val TICK_SECONDS = TICK_INTERVAL_MS / 1000f

		private const val MAX_FORWARD_SPEED = 55f // m/s, ~200 km/h
		private const val MAX_REVERSE_SPEED = 12f // m/s, ~43 km/h

		private const val THROTTLE_ACCELERATION = 8f // m/s^2
		private const val BRAKE_DECELERATION = 14f // m/s^2
		private const val ROLLING_DECELERATION = 1.5f // m/s^2
		private const val GEAR_SWITCH_MAX_SPEED = 0.5f // m/s

		private const val MAX_TURN_RATE = 110f // degrees per second
		private const val FULL_TURN_RATE_SPEED = 5f // m/s, full steering response above this speed
		private const val MIN_TURN_FACTOR = 0.5f

		private const val LOCATION_ACCURACY = 5f

		private const val CAMERA_ROTATION_STEP = 10f // degrees per key event
		private const val CAMERA_TILT_STEP = 3f // degrees per key event
	}
}
