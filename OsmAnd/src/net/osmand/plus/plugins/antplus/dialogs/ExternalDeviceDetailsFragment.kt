package net.osmand.plus.plugins.antplus.dialogs

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.plugins.antplus.BleConnectionStateListener
import net.osmand.plus.plugins.antplus.BleDataListener
import net.osmand.plus.plugins.antplus.ExternalDevice
import net.osmand.plus.plugins.antplus.adapters.DeviceCharacteristicsAdapter
import net.osmand.plus.plugins.antplus.models.BatteryData
import net.osmand.plus.plugins.antplus.models.BleDeviceData
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

class ExternalDeviceDetailsFragment : AntPlusBaseFragment(), BleConnectionStateListener,
    BleDataListener {
    companion object {
        val TAG: String = ExternalDeviceDetailsFragment.javaClass.simpleName
        fun showInstance(manager: FragmentManager, device: ExternalDevice) {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = ExternalDeviceDetailsFragment()
                val arguments = Bundle()
                arguments.putSerializable("DEVICE", device)
                fragment.arguments = arguments
                fragment.show(manager, TAG)
            }
        }
    }

    lateinit var device: ExternalDevice
    private var connectionState: TextView? = null
    private var receivedDataView: RecyclerView? = null
    lateinit var receivedDataAdapter: DeviceCharacteristicsAdapter

    override fun getLayoutId(): Int {
        return R.layout.fragment_external_device_details;
    }

    override fun getElevation(): Float {
        return 0f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("DEVICE", ExternalDevice::class.java)!!
        } else {
            arguments?.getSerializable("DEVICE") as ExternalDevice
        }

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
        var deviceName: TextView = view.findViewById(R.id.device_name)
        deviceName.text = device.name
        var widgetIcon: ImageView = view.findViewById(R.id.widget_icon)
        val widgetType = device.deviceType?.widgetType
        widgetType?.let {
            widgetIcon.setImageResource(if (nightMode) it.nightIconId else it.dayIconId)
        }
        connectionState = view.findViewById(R.id.connection_state)
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
        return device.connectionType === ExternalDevice.DeviceConnectionType.BLE
    }

    private fun updateConnectedState(
        view: View
    ) {
        val isConnected = plugin.isDeviceConnected(device)
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

        var lightResId = 0;
        var darkResId = 0;
        var pairBtnTextColorId = 0;
        var pairBtnTextId = 0;
        if (!plugin.isDevicePaired(device)) {
            lightResId = unpairedStateBtnBgColorLight
            darkResId = unpairedStateBtnBgColorDark
            pairBtnTextColorId = unpairedStateBtnTextColor
            pairBtnTextId = R.string.external_device_details_pair
            pairButton.setOnClickListener { pairDevice() }
        } else if (plugin.isDeviceConnected(device)) {
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

        val textAndIconColorResId = pairBtnTextColorId
        pairButtonText.text = getString(pairBtnTextId)
        val colorStateList = ContextCompat.getColorStateList(app, textAndIconColorResId)
        pairButtonText.setTextColor(colorStateList)
    }

    override fun onResume() {
        super.onResume()
        plugin.addBleDeviceConnectionStateListener(this)
        plugin.addBleDeviceDataListener(this)
        updateConnectedState()
        updateButtonState()
    }

    override fun onPause() {
        super.onPause()
        plugin.removeBleDeviceConnectionStateListener(this)
        plugin.removeBleDeviceDataListener(this)
    }

    private fun pairDevice() {
        plugin.pairDevice(device)
        updateButtonState()
    }

    private fun connectDevice() {
        plugin.connectDevice(device)
    }

    private fun disconnectDevice() {
        plugin.disconnectDevice(device)
    }

    override fun onStateChanged(address: String?, newState: Int) {
        if (device.address.equals(address)) {
            updateConnectedState()
            updateButtonState()
        }
    }

    override fun onDataReceived(address: String?, data: BleDeviceData) {
        if (device.address.equals(address)) {
            if (data is BatteryData) {

            } else {
                receivedDataAdapter.setItems(data.getDataFields())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectionState = null
        receivedDataView = null
    }
}