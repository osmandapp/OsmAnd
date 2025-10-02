package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.plugins.externalsensors.dialogs.ForgetDeviceBaseDialog
import net.osmand.plus.utils.AndroidUtils

class ForgetOBDDeviceDialog : ForgetDeviceBaseDialog() {
	private lateinit var deviceId: String
	private var isBLE: Boolean = false

	override val layoutId: Int
		get() = super.layoutId

	companion object {
		const val BLE_KEY = "BLE_KEY"
		fun showInstance(
			manager: FragmentManager,
			targetFragment: Fragment,
			deviceId: String, isBLE: Boolean) {
			if (targetFragment !is ForgetDeviceListener) {
				throw IllegalArgumentException("target fragment should implement ForgetDeviceListener")
			}
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = ForgetOBDDeviceDialog()
				val args = Bundle()
				args.putString(DEVICE_ID_KEY, deviceId)
				args.putBoolean(BLE_KEY, isBLE)
				fragment.arguments = args
				fragment.setTargetFragment(targetFragment, 0)
				fragment.show(manager, TAG)
			}
		}
	}

	interface ForgetDeviceListener {
		fun onForgetSensorConfirmed(deviceId: String, isBLE: Boolean)
	}

	override fun initDevice(arguments: Bundle) {
		val deviceId = arguments.getString(DEVICE_ID_KEY)
		isBLE = arguments.getBoolean(BLE_KEY)
		if (deviceId == null) {
			dismiss()
		} else {
			this.deviceId = deviceId
		}
	}

	override fun onForgetSensorConfirmed() {
		(targetFragment as ForgetDeviceListener).onForgetSensorConfirmed(deviceId, isBLE)
	}
}