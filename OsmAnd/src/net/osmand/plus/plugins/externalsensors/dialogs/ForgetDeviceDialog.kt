package net.osmand.plus.plugins.externalsensors.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.BottomSheetDialogFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class ForgetDeviceDialog : BottomSheetDialogFragment() {
    private var nightMode = false
    private lateinit var app: OsmandApplication
    private lateinit var device: AbstractDevice<out AbstractSensor>

    companion object {
        const val TAG = "ForgetDeviceDialog"
        const val DEVICE_ID_KEY = "DEVICE_ID"

        fun showInstance(manager: FragmentManager, targetFragment: Fragment, device: AbstractDevice<*>) {
            if(targetFragment !is ForgetDeviceListener){
                throw IllegalArgumentException("target fragment should implement ForgetDeviceListener")
            }
            val fragment = ForgetDeviceDialog()
            val args = Bundle()
            args.putString(DEVICE_ID_KEY, device.deviceId)
            fragment.arguments = args
            fragment.setTargetFragment(targetFragment, 0)
            fragment.show(manager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = requireContext()
        app = context.applicationContext as OsmandApplication
        nightMode = app.daynightHelper.isNightModeForMapControls
        val plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin::class.java)
        val deviceId = arguments?.getString(DEVICE_ID_KEY)
        device = plugin.getDevice(deviceId!!)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.forget_device_dialog, container, false)

        val forgetButton = view.findViewById<View>(R.id.forget_btn)
        val forgetButtonText = forgetButton.findViewById<TextView>(R.id.button_text)
        val forgetButtonContainer = forgetButton.findViewById<View>(R.id.button_container)

        val cancelButton = view.findViewById<View>(R.id.cancel_btn)
        val cancelButtonText = cancelButton.findViewById<TextView>(R.id.button_text)
        val cancelButtonContainer = cancelButton.findViewById<View>(R.id.button_container)

        forgetButton.setOnClickListener {
            (targetFragment as ForgetDeviceListener).onForgetSensorConfirmed(device)
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

    interface ForgetDeviceListener {
        fun onForgetSensorConfirmed(device: AbstractDevice<out AbstractSensor>)
    }
}