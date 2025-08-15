package net.osmand.plus.plugins.externalsensors.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.osmand.plus.R
import net.osmand.plus.base.BottomSheetDialogFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities


abstract class ForgetDeviceBaseDialog : BottomSheetDialogFragment() {
	open val layoutId = R.layout.forget_obd_device_dialog

	companion object {
		const val TAG = "ForgetDeviceDialog"
		const val DEVICE_ID_KEY = "DEVICE_ID"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.let { initDevice(it) }
	}

	override fun isUsedOnMap() = true

	abstract fun initDevice(arguments: Bundle)

	abstract fun onForgetSensorConfirmed()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {

		updateNightMode()
		val view: View = inflate(layoutId, container, false)

		val forgetButton = view.findViewById<View>(R.id.forget_btn)
		val forgetButtonText = forgetButton.findViewById<TextView>(R.id.button_text)
		val forgetButtonContainer = forgetButton.findViewById<View>(R.id.button_container)

		val cancelButton = view.findViewById<View>(R.id.cancel_btn)
		val cancelButtonText = cancelButton.findViewById<TextView>(R.id.button_text)
		val cancelButtonContainer = cancelButton.findViewById<View>(R.id.button_container)

		forgetButton.setOnClickListener {
			onForgetSensorConfirmed()
			dismiss()
		}
		cancelButton.setOnClickListener { dismiss() }

		AndroidUtils.setBackground(
			app,
			forgetButtonContainer,
			nightMode,
			R.drawable.ripple_solid_light,
			R.drawable.ripple_solid_dark
		)

		AndroidUtils.setBackground(
			app,
			forgetButton,
			nightMode,
			R.drawable.dlg_btn_secondary_light,
			R.drawable.dlg_btn_secondary_dark
		)
		val forgetBtnTextColor =
			if (nightMode) R.color.color_osm_edit_delete else R.color.color_osm_edit_delete

		forgetButtonText.setTextColor(ContextCompat.getColorStateList(app, forgetBtnTextColor))

		AndroidUtils.setBackground(
			app,
			cancelButtonContainer,
			nightMode,
			R.drawable.ripple_solid_light,
			R.drawable.ripple_solid_dark
		)

		AndroidUtils.setBackground(
			app,
			cancelButton,
			nightMode,
			R.drawable.dlg_btn_secondary_light,
			R.drawable.dlg_btn_secondary_dark
		)
		cancelButtonText.setTextColor(ColorUtilities.getButtonSecondaryTextColor(app, nightMode))
		return view
	}
}