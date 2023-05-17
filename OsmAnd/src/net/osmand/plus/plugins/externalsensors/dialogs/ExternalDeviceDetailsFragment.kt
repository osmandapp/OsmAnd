package net.osmand.plus.plugins.externalsensors.dialogs

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.externalsensors.adapters.DeviceCharacteristicsAdapter
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

class ExternalDeviceDetailsFragment : ExternalDevicesBaseFragment(), DeviceListener {
    companion object {
        const val TAG: String = "ExternalSensorDetailsFragment"
        const val DEVICE_ID_KEY = "DEVICE_ID"

        fun showInstance(manager: FragmentManager, device: AbstractDevice<out AbstractSensor>) {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = ExternalDeviceDetailsFragment()
                val arguments = Bundle()
                arguments.putString(DEVICE_ID_KEY, device.deviceId)
                fragment.arguments = arguments
                fragment.show(manager, TAG)
            }
        }
    }

    lateinit var device: AbstractDevice<out AbstractSensor>
    private var connectionState: TextView? = null
    private var batteryLevel: TextView? = null
    private var receivedDataView: RecyclerView? = null
    private lateinit var receivedDataAdapter: DeviceCharacteristicsAdapter

    override fun getLayoutId(): Int {
        return R.layout.fragment_external_device_details
    }

    override fun getElevation(): Float {
        return 0f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceId = arguments?.getString(DEVICE_ID_KEY)
            ?: throw IllegalArgumentException("DEVICE_ID in not defined")
        device = plugin.getDevice(deviceId)
            ?: throw IllegalArgumentException("Device in not found")
    }

    override fun setupToolbar(view: View) {
        val closeButton = view.findViewById<View>(R.id.close_button)
        if (closeButton != null) {
            closeButton.setOnClickListener {
                dismiss()
            }
            if (closeButton is ImageView) {
                UiUtilities.rotateImageByLayoutDirection(closeButton)
            }
        }
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        val deviceName: TextView = view.findViewById(R.id.device_name)
        deviceName.text = plugin.getDeviceName(device)
        val widgetIcon: ImageView = view.findViewById(R.id.widget_icon)
        val deviceType = device.deviceType
        deviceType.let {
            widgetIcon.setImageResource(if (nightMode) it.nightIconId else it.dayIconId)
        }
        connectionState = view.findViewById(R.id.connection_state)
        batteryLevel = view.findViewById(R.id.battery_level)
        updateConnectedState(view)
        updateButtonState(view)
        val connectionTypeTextView: TextView = view.findViewById(R.id.connection_type)
        connectionTypeTextView.text =
            getConnectionTypeName()
        receivedDataAdapter = DeviceCharacteristicsAdapter(app, nightMode)
        receivedDataView = view.findViewById(R.id.received_data)
        receivedDataView!!.adapter = receivedDataAdapter
    }

    private fun getAntText() = app.getString(R.string.external_device_ant)

    private fun getBleText() = app.getString(R.string.external_device_ble)

    private fun isBle(): Boolean {
        return device is BLEAbstractDevice
    }

    private fun updateConnectedState(view: View) {
        val isConnected = device.isConnected
        val rssi = device.rssi
        val signalLevelIcon: Int = if (rssi > -50) {
            R.drawable.ic_action_signal_high
        } else if (rssi > -70) {
            R.drawable.ic_action_signal_middle
        } else {
            R.drawable.ic_action_signal_low
        }
        val connectedTextId =
            if (isConnected) R.string.bluetooth_connected else R.string.bluetooth_disconnected
        connectionState?.text = String.format(
            app.getString(connectedTextId),
            getConnectionTypeName()
        )
        val connectionStateIcon: ImageView = view.findViewById(R.id.connection_state_icon)
        connectionStateIcon.setImageResource(signalLevelIcon)
        batteryLevel?.text = device.batteryLevel.toString()
    }

    private fun getConnectionTypeName() = if (isBle()) getBleText() else getAntText()

    private fun updateConnectedState() {
        view?.let { updateConnectedState(it) }
    }

    private fun updateButtonState() {
        view?.let { updateButtonState(it) }
    }

    private fun updateButtonState(view: View) {
        val pairButtonText = view.findViewById<TextView>(R.id.button_text)
        val pairButton = view.findViewById<View>(R.id.pair_btn)
        val pairButtonContainer = view.findViewById<View>(R.id.button_container)
        view.post {
            AndroidUtils.setBackground(
                app,
                pairButtonContainer,
                nightMode,
                R.drawable.ripple_solid_light,
                R.drawable.ripple_solid_dark
            )
        }
        val connectedStateBtnTextColor = ColorUtilities.getButtonSecondaryTextColorId(nightMode)
        val disconnectedStateBtnTextColor =
            if (nightMode) R.color.dlg_btn_primary_text_dark else R.color.dlg_btn_primary_text_light
        val unpairedStateBtnTextColor =
            if (nightMode) R.color.dlg_btn_primary_text_dark else R.color.dlg_btn_primary_text_light

        val connectedStateBtnBgColorLight = R.drawable.dlg_btn_secondary_light
        val connectedStateBtnBgColorDark = R.drawable.dlg_btn_secondary_dark

        val disconnectedStateBtnBgColorLight = R.drawable.dlg_btn_primary_light
        val disconnectedStateBtnBgColorDark = R.drawable.dlg_btn_primary_dark

        val unpairedStateBtnBgColorLight = R.color.ble_unpaired_device_btn_bg
        val unpairedStateBtnBgColorDark = R.color.ble_unpaired_device_btn_bg

        val lightResId: Int
        val darkResId: Int
        val pairBtnTextColorId: Int
        val pairBtnTextId: Int
        if (!plugin.isDevicePaired(device)) {
            lightResId = unpairedStateBtnBgColorLight
            darkResId = unpairedStateBtnBgColorDark
            pairBtnTextColorId = unpairedStateBtnTextColor
            pairBtnTextId = R.string.external_device_details_pair
            pairButton.setOnClickListener { pairDevice() }
        } else if (device.isConnected) {
            lightResId = connectedStateBtnBgColorLight
            darkResId = connectedStateBtnBgColorDark
            pairBtnTextColorId = connectedStateBtnTextColor
            pairBtnTextId = R.string.external_device_details_disconnect
            pairButton.setOnClickListener { disconnectDevice() }
        } else {
            lightResId = disconnectedStateBtnBgColorLight
            darkResId = disconnectedStateBtnBgColorDark
            pairBtnTextColorId = disconnectedStateBtnTextColor
            pairBtnTextId = R.string.external_device_details_connect
            pairButton.setOnClickListener { connectDevice() }
        }

        AndroidUtils.setBackground(
            app,
            pairButton,
            nightMode,
            lightResId,
            darkResId
        )

        pairButtonText.text = getString(pairBtnTextId)
        val colorStateList = ContextCompat.getColorStateList(app, pairBtnTextColorId)
        pairButtonText.setTextColor(colorStateList)
    }

    override fun onResume() {
        super.onResume()
        device.addListener(this)
        updateConnectedState()
        updateButtonState()
    }

    override fun onPause() {
        super.onPause()
        device.removeListener(this)
    }

    private fun pairDevice() {
        plugin.pairDevice(device)
        updateButtonState()
    }

    private fun connectDevice() {
        plugin.connectDevice(activity, device)
    }

    private fun disconnectDevice() {
        plugin.disconnectDevice(device)
    }

    override fun onDeviceConnect(
        device: AbstractDevice<out AbstractSensor>,
        result: DeviceConnectionResult,
        error: String?) {
        app.runInUIThread {
            updateConnectedState()
            updateButtonState()
        }
    }

    override fun onDeviceDisconnect(device: AbstractDevice<out AbstractSensor>) {
        app.runInUIThread {
            updateConnectedState()
            updateButtonState()
        }
    }

    override fun onSensorData(sensor: AbstractSensor, data: SensorData) {
        app.runInUIThread {
            updateConnectedState()
            val dataFields = ArrayList<SensorDataField>()
            for (sensor in device.sensors) {
                val sensorDataList = sensor.lastSensorDataList
                sensorDataList?.let {
                    for (sensorData in sensorDataList) {
                        sensorData?.let {
                            dataFields.addAll(sensorData.dataFields)
                        }
                    }
                }
            }
            receivedDataAdapter.setItems(dataFields)
            if (device.isBatteryLow) {
                app.showShortToastMessage(R.string.external_device_low_battery)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectionState = null
        batteryLevel = null
        receivedDataView = null
    }

    @ColorRes
    override fun getStatusBarColorId(): Int {
        AndroidUiHelper.setStatusBarContentColor(view, nightMode)
        return if (nightMode) R.color.status_bar_color_dark else R.color.activity_background_color_light
    }
}