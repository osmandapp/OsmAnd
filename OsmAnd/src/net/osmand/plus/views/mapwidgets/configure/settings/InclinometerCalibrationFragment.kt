package net.osmand.plus.views.mapwidgets.configure.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.utils.InclinometerSensor
import net.osmand.plus.views.mapwidgets.utils.InclinometerSensor.InclinometerListener
import net.osmand.plus.views.mapwidgets.widgets.InclinometerWidget
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.plus.views.mapwidgets.configure.settings.InclinometerWidgetInfoFragment.formatOffset
import kotlin.math.abs

/**
 * Calibration screen of the inclinometer widgets: a live bubble level, the stored offset and
 * the Calibrate / Reset buttons. The calibration is shared by all inclinometer widgets.
 */
class InclinometerCalibrationFragment : BaseFullScreenFragment(), InclinometerListener {

	companion object {
		val TAG: String = InclinometerCalibrationFragment::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.add(R.id.fragmentContainer, InclinometerCalibrationFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}

	private lateinit var sensor: InclinometerSensor

	private lateinit var levelView: InclinometerLevelView
	private lateinit var pitchValue: TextView
	private lateinit var rollValue: TextView
	private lateinit var statusTitle: TextView
	private lateinit var statusDescription: TextView
	private lateinit var calibrateButton: DialogButton
	private lateinit var resetButton: DialogButton

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		sensor = app.inclinometerSensor
	}

	override fun isUsedOnMap(): Boolean = true

	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return ColorUtilities.getListBgColorId(nightMode)
	}

	override fun getContentStatusBarNightMode(): Boolean = nightMode

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		val view = inflate(R.layout.fragment_inclinometer_calibration, container, false)
		setupToolbar(view)

		levelView = view.findViewById(R.id.level_view)
		levelView.setColors(
			ColorUtilities.getSecondaryIconColor(app, nightMode),
			ColorUtilities.getActiveColor(app, nightMode),
			ContextCompat.getColor(app, R.color.inclinometer_level_safe),
			ContextCompat.getColor(app, R.color.inclinometer_level_warning)
		)
		pitchValue = view.findViewById(R.id.pitch_value)
		rollValue = view.findViewById(R.id.roll_value)
		statusTitle = view.findViewById(R.id.status_title)
		statusDescription = view.findViewById(R.id.status_description)

		calibrateButton = view.findViewById(R.id.calibrate_button)
		calibrateButton.setOnClickListener {
			if (sensor.calibrate()) {
				app.showShortToastMessage(R.string.inclinometer_calibration_saved)
			} else {
				app.showShortToastMessage(R.string.inclinometer_sensor_unavailable)
			}
			updateStatus()
		}
		resetButton = view.findViewById(R.id.reset_calibration_button)
		resetButton.setOnClickListener {
			sensor.resetCalibration()
			updateStatus()
		}
		updateStatus()
		updateValues()

		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)
		return view
	}

	private fun setupToolbar(view: View) {
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.navigationIcon = getContentIcon(AndroidUtils.getNavigationIconResId(app))
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
		toolbar.setTitleTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode))
	}

	override fun onResume() {
		super.onResume()
		if (sensor.isSensorAvailable) {
			sensor.requestUpdates(this)
		}
	}

	override fun onPause() {
		super.onPause()
		sensor.removeListener(this)
	}

	override fun onInclinationChanged(pitchDegrees: Float, rollDegrees: Float) {
		updateValues()
	}

	override fun isInclinometerActive(): Boolean = isResumed

	private fun updateValues() {
		val pitch = if (sensor.hasValue()) sensor.pitch else 0f
		val roll = if (sensor.hasValue()) sensor.roll else 0f
		levelView.setInclination(pitch, roll)
		pitchValue.text = formatTilt(WidgetType.INCLINOMETER_PITCH, pitch)
		rollValue.text = formatTilt(WidgetType.INCLINOMETER_ROLL, roll)
	}

	private fun formatTilt(widgetType: WidgetType, degrees: Float): String {
		val value = formatOffset(abs(degrees))
		val arrow = InclinometerWidget.getDirectionArrow(widgetType, degrees)
		return if (arrow != null) "$value $arrow" else value
	}

	private fun updateStatus() {
		val offset = sensor.calibrationOffset
		when {
			!sensor.isSensorAvailable -> {
				statusTitle.setText(R.string.inclinometer_sensor_unavailable)
				statusDescription.visibility = View.GONE
			}
			offset != null -> {
				statusTitle.setText(R.string.inclinometer_calibrated)
				statusDescription.text = getString(R.string.inclinometer_calibration_offset,
					formatOffset(offset[0]), formatOffset(offset[1]))
				statusDescription.visibility = View.VISIBLE
			}
			else -> {
				statusTitle.setText(R.string.inclinometer_not_calibrated)
				statusDescription.visibility = View.GONE
			}
		}
		calibrateButton.setTitleId(if (offset != null) R.string.inclinometer_recalibrate else R.string.inclinometer_calibrate)
		calibrateButton.isEnabled = sensor.isSensorAvailable
		AndroidUiHelper.updateVisibility(resetButton, offset != null)
	}
}
