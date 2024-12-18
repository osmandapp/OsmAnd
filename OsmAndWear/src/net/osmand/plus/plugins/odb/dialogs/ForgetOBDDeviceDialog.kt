package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.dialogs.ForgetDeviceBaseDialog

class ForgetOBDDeviceDialog : ForgetDeviceBaseDialog() {
	private lateinit var deviceId: String
	override val layoutId: Int
		get() = super.layoutId

	companion object {
		fun showInstance(
			manager: FragmentManager,
			targetFragment: Fragment,
			deviceId: String) {
			if (targetFragment !is ForgetDeviceListener) {
				throw IllegalArgumentException("target fragment should implement ForgetDeviceListener")
			}
			val fragment = ForgetOBDDeviceDialog()
			val args = Bundle()
			args.putString(DEVICE_ID_KEY, deviceId)
			fragment.arguments = args
			fragment.setTargetFragment(targetFragment, 0)
			fragment.show(manager, TAG)
		}
	}

	interface ForgetDeviceListener {
		fun onForgetSensorConfirmed(deviceId: String)
	}

	override fun initDevice(deviceId: String) {
		this.deviceId = deviceId
	}

	override fun onForgetSensorConfirmed() {
		(targetFragment as ForgetDeviceListener).onForgetSensorConfirmed(deviceId)
	}
}