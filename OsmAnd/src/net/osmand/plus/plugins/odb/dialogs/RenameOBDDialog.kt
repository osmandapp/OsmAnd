package net.osmand.plus.plugins.odb.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColor
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.util.Algorithms
import studio.carbonylgroup.textfieldboxes.ExtendedEditText

class RenameOBDDialog : BaseFullScreenDialogFragment() {
	private var textInput: ExtendedEditText? = null
	private var propertyOldValueValue: String? = null
	private var deviceAddress: String? = null
	private var isBLE: Boolean = false
	private var device: BTDeviceInfo? = null
	val plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin::class.java)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		updateNightMode()
		val view = themedInflater.inflate(R.layout.dialog_edit_device_name, container, false)
		val propertyName = view.findViewById<TextView>(R.id.property_name)
		textInput = view.findViewById(R.id.description)
		textInput?.requestFocus()
		val propertyValueView = view.findViewById<OsmandTextFieldBoxes>(R.id.property_value)
		propertyValueView.setClearButton(
			app.uiUtilities.getIcon(
				R.drawable.ic_action_cancel,
				nightMode))
		view.findViewById<View>(R.id.btn_close).setOnClickListener { v: View? ->
			if (shouldClose()) {
				dismiss()
			} else {
				showDismissDialog()
			}
		}
		setupSaveButton(view)
		val args = arguments
		if (args != null) {
			deviceAddress = args.getString(DEVICE_ADDRESS_KEY)
			isBLE = args.getBoolean(BLE_KEY)
			textInput?.inputType = EditorInfo.TYPE_CLASS_TEXT
			propertyName.setText(R.string.shared_string_name)
			propertyValueView.hasClearButton = true
			plugin?.let {
				val pairedDevices = plugin.getUsedOBDDevicesList()
				device = pairedDevices.find { it.address == deviceAddress }
				propertyOldValueValue = device?.name ?: ""
				textInput?.setText(propertyOldValueValue);
			}
		}
		return view
	}

	override fun isUsedOnMap(): Boolean {
		return true
	}

	override fun getThemeId(): Int {
		return if (nightMode) R.style.OsmandDarkTheme_DarkActionbar else R.style.OsmandLightTheme_DarkActionbar_LightStatusBar
	}

	override fun getStatusBarColorId(): Int {
		return ColorUtilities.getActivityBgColorId(nightMode)
	}

	private fun shouldClose(): Boolean {
		val editable = textInput!!.text
		return if (propertyOldValueValue == null || editable == null) {
			true
		} else {
			propertyOldValueValue == editable.toString()
		}
	}

	private fun setupSaveButton(view: View) {
		val btnSaveContainer: View? = view.findViewById(R.id.btn_save_container)
		btnSaveContainer?.setOnClickListener { _: View? ->
			val editable = textInput?.text
			editable?.let {
				if (!Algorithms.isEmpty(editable)) {
					onSaveEditedText(editable.toString())
					dismiss()
				}
			}
		}
		val ctx = btnSaveContainer?.context
		AndroidUtils.setBackground(
			ctx,
			btnSaveContainer,
			nightMode,
			R.drawable.ripple_light,
			R.drawable.ripple_dark)
		val btnSave: View? = view.findViewById(R.id.btn_save)
		val drawableRes =
			if (nightMode) R.drawable.btn_solid_border_dark else R.drawable.btn_solid_border_light
		AndroidUtils.setBackground(btnSave, getIcon(drawableRes))
	}

	private fun showDismissDialog() {
		val dialogData = AlertDialogData(requireContext(), nightMode)
			.setTitle(R.string.shared_string_dismiss)
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_exit) { _: DialogInterface?, _: Int -> dismiss() }
		CustomAlert.showSimpleMessage(dialogData, R.string.exit_without_saving)
	}

	private fun onSaveEditedText(newName: String) {
		val target = targetFragment
		if (target is OnDeviceNameChangedCallback) {
			plugin?.let {
				deviceAddress?.let { address ->
					it.setDeviceName(address, newName, isBLE)
					target.onNameChanged()
				}
			}
		}
	}

	interface OnDeviceNameChangedCallback {
		fun onNameChanged()
	}

	companion object {
		const val TAG = "RenameOBDDialog"
		private const val DEVICE_ADDRESS_KEY = "device_address"
		private const val BLE_KEY = "BLE_KEY"
		fun showInstance(
			activity: FragmentActivity,
			target: Fragment?,
			device: BTDeviceInfo) {
			require(target is OnDeviceNameChangedCallback) { "target fragment should implement OnSaveSensorNameCallback" }
			val fragmentManager = activity.supportFragmentManager
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
				val fragment = RenameOBDDialog()
				val args = Bundle()
				args.putString(DEVICE_ADDRESS_KEY, device.address)
				args.putBoolean(BLE_KEY, device.isBLE)
				fragment.arguments = args
				fragment.setTargetFragment(target, 0)
				fragment.show(fragmentManager, TAG)
			}
		}
	}
}