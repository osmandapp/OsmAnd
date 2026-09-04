package net.osmand.plus.simulation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.simulation.DriveSimulationEngine.DriveSimulationListener
import net.osmand.plus.simulation.OsmAndLocationSimulation.LocationSimulationListener
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

/**
 * On-map controls of the manually driven location simulation: steering, throttle, brake and gear.
 * A hardware keyboard drives the vehicle with the arrow keys and controls the camera with
 * Q / E (rotate), W / S (tilt) and A / D (zoom).
 */
class DriveSimulationFragment : BaseOsmAndFragment(), DriveSimulationListener,
	LocationSimulationListener, KeyEvent.Callback {

	private val simulation: OsmAndLocationSimulation
		get() = app.locationProvider.locationSimulation

	private val engine: DriveSimulationEngine
		get() = simulation.driveSimulation

	private var panel: View? = null
	private var expandButton: ImageButton? = null
	private var gearButton: TextView? = null

	private var collapsed = false

	override fun isUsedOnMap() = true

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		updateNightMode()
		val view = themedInflater.inflate(R.layout.fragment_drive_simulation, container, false)

		collapsed = savedInstanceState?.getBoolean(COLLAPSED_KEY, false) ?: false
		panel = view.findViewById(R.id.drive_simulation_panel)
		gearButton = view.findViewById(R.id.gear_button)

		setupPedal(view.findViewById(R.id.throttle_button), R.drawable.ic_action_arrow_up) { pressed ->
			engine.setThrottlePressed(pressed)
		}
		setupPedal(view.findViewById(R.id.brake_button), R.drawable.ic_action_arrow_down) { pressed ->
			engine.setBrakePressed(pressed)
		}
		setupPedal(view.findViewById(R.id.steer_left_button), R.drawable.ic_arrow_back) { pressed ->
			engine.setSteering(if (pressed) -1f else 0f)
		}
		setupPedal(view.findViewById(R.id.steer_right_button), R.drawable.ic_arrow_forward) { pressed ->
			engine.setSteering(if (pressed) 1f else 0f)
		}

		gearButton?.let {
			AndroidUtils.setBackground(it.context, it, nightMode, R.drawable.btn_circle, R.drawable.btn_circle_night)
			it.setOnClickListener { _ ->
				engine.reverseGear = !engine.reverseGear
				updateControls()
			}
		}
		val stopButton = view.findViewById<ImageButton>(R.id.stop_simulation_button)
		setupButtonAppearance(stopButton, R.drawable.ic_action_close)
		stopButton.setOnClickListener {
			val activity = mapActivity
			if (activity != null) {
				simulation.stopDriveSimulation()
				hideInstance(activity)
			}
		}
		val collapseButton = view.findViewById<ImageButton>(R.id.collapse_button)
		setupButtonAppearance(collapseButton, R.drawable.ic_action_arrow_down)
		collapseButton.setOnClickListener { setCollapsed(true) }

		expandButton = view.findViewById<ImageButton>(R.id.expand_button).also {
			setupButtonAppearance(it, R.drawable.ic_action_car)
			it.setOnClickListener { setCollapsed(false) }
		}
		updateVisibility()
		updateControls()
		return view
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(COLLAPSED_KEY, collapsed)
	}

	/** Hides the pedals, leaving a single button on the map. The keyboard keeps working. */
	private fun setCollapsed(collapsed: Boolean) {
		this.collapsed = collapsed
		if (collapsed) {
			engine.resetControls()
		}
		updateVisibility()
	}

	private fun updateVisibility() {
		AndroidUiHelper.updateVisibility(panel, !collapsed)
		AndroidUiHelper.updateVisibility(expandButton, collapsed)
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun setupPedal(button: ImageButton, iconId: Int, onPressed: (Boolean) -> Unit) {
		setupButtonAppearance(button, iconId)
		button.setOnTouchListener { view, event ->
			when (event.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					view.isPressed = true
					onPressed(true)
					true
				}

				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					view.isPressed = false
					onPressed(false)
					true
				}

				else -> false
			}
		}
	}

	private fun setupButtonAppearance(button: ImageButton, iconId: Int) {
		AndroidUtils.setBackground(button.context, button, nightMode,
			R.drawable.btn_circle, R.drawable.btn_circle_night)
		button.setImageDrawable(uiUtilities.getIcon(iconId, ColorUtilities.getActiveIconColorId(nightMode)))
	}

	override fun onResume() {
		super.onResume()
		engine.addListener(this)
		simulation.addSimulationListener(this)
		app.keyEventHelper.setExternalCallback(this)
		updateControls()
	}

	override fun onPause() {
		super.onPause()
		engine.removeListener(this)
		engine.resetControls()
		simulation.removeSimulationListener(this)
		app.keyEventHelper.setExternalCallback(null)
	}

	override fun onDriveSimulationUpdate(engine: DriveSimulationEngine) {
		updateControls()
	}

	private fun isFirstKeyPress(event: KeyEvent?) = event == null || event.repeatCount == 0

	override fun onSimulationStateChanged(simulating: Boolean) {
		if (!simulation.isDriveSimulationActive) {
			mapActivity?.let { hideInstance(it) }
		}
	}

	private fun updateControls() {
		val gearNameId = if (engine.reverseGear) {
			R.string.drive_simulation_gear_reverse
		} else {
			R.string.drive_simulation_gear_drive
		}
		gearButton?.setText(gearNameId)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		when (keyCode) {
			// Vehicle
			KeyEvent.KEYCODE_DPAD_UP -> engine.setThrottlePressed(true)
			KeyEvent.KEYCODE_DPAD_DOWN -> engine.setBrakePressed(true)
			KeyEvent.KEYCODE_DPAD_LEFT -> engine.setSteering(-1f)
			KeyEvent.KEYCODE_DPAD_RIGHT -> engine.setSteering(1f)
			KeyEvent.KEYCODE_R -> if (isFirstKeyPress(event)) {
				engine.reverseGear = !engine.reverseGear
				updateControls()
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

	private fun rotateCamera(degrees: Float) {
		engine.enableManualMapRotation()
		val mapView = app.osmandMap.mapView
		mapView.setRotate(mapView.rotate + degrees, true)
	}

	private fun tiltCamera(degrees: Float) {
		val mapView = app.osmandMap.mapView
		val angle = mapView.normalizeElevationAngle(mapView.elevationAngle + degrees)
		settings.setLastKnownMapElevation(angle)
		mapView.animatedDraggingThread.startTilting(angle, 0f)
		mapView.refreshMap()
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		when (keyCode) {
			KeyEvent.KEYCODE_DPAD_UP -> engine.setThrottlePressed(false)
			KeyEvent.KEYCODE_DPAD_DOWN -> engine.setBrakePressed(false)
			KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> engine.setSteering(0f)
			KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_S,
			KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_R -> Unit

			else -> return false
		}
		return true
	}

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

	override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false

	companion object {
		val TAG: String = DriveSimulationFragment::class.java.simpleName

		private const val COLLAPSED_KEY = "collapsed"
		private const val CAMERA_ROTATION_STEP = 10f // degrees per key event
		private const val CAMERA_TILT_STEP = 3f // degrees per key event

		@JvmStatic
		fun showInstance(activity: MapActivity) {
			val manager: FragmentManager = activity.supportFragmentManager
			if (!manager.isStateSaved && manager.findFragmentByTag(TAG) == null) {
				manager.beginTransaction()
					.add(R.id.bottomFragmentContainer, DriveSimulationFragment(), TAG)
					.commitAllowingStateLoss()
			}
		}

		@JvmStatic
		fun hideInstance(activity: MapActivity) {
			val manager: FragmentManager = activity.supportFragmentManager
			val fragment = manager.findFragmentByTag(TAG)
			if (fragment != null && !manager.isStateSaved) {
				manager.beginTransaction().remove(fragment).commitAllowingStateLoss()
			}
		}
	}
}
