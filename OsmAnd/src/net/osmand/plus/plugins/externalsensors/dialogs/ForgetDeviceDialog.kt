package net.osmand.plus.plugins.externalsensors.dialogs

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor
import net.osmand.plus.utils.AndroidUtils

class ForgetDeviceDialog : ForgetDeviceBaseDialog() {
    private lateinit var device: AbstractDevice<out AbstractSensor>

    companion object {
	    fun showInstance(manager: FragmentManager, targetFragment: Fragment, deviceId: String) {
		    if (targetFragment !is ForgetDeviceListener) {
                throw IllegalArgumentException("target fragment should implement ForgetDeviceListener")
            }
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = ForgetDeviceDialog()
                val args = Bundle()
                args.putString(DEVICE_ID_KEY, deviceId)
                fragment.arguments = args
                fragment.setTargetFragment(targetFragment, 0)
                fragment.show(manager, TAG)
            }
        }
    }

    interface ForgetDeviceListener {
        fun onForgetSensorConfirmed(device: AbstractDevice<out AbstractSensor>)
    }

	override fun initDevice(arguments: Bundle) {
		val deviceId = arguments.getString(DEVICE_ID_KEY)
		val plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin::class.java)
		val device = if (deviceId != null) plugin?.getDevice(deviceId) else null
		if (device == null) {
			dismiss()
		} else {
			this.device = device
		}
	}

	override fun onForgetSensorConfirmed() {
		(targetFragment as ForgetDeviceListener).onForgetSensorConfirmed(device)
	}
}