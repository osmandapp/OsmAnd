package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin.OBDConnectionState
import net.osmand.plus.plugins.odb.adapters.OBDMainFragmentAdapter
import net.osmand.plus.settings.enums.VolumeUnit
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.util.Algorithms

class OBDMainFragment : OBDDevicesBaseFragment(), VehicleMetricsPlugin.ConnectionStateListener,
	RenameOBDDialog.OnDeviceNameChangedCallback, ForgetOBDDeviceDialog.ForgetDeviceListener {
	private var handlerThread: HandlerThread? = null

	enum class OBDDataType(val widgetType: (OsmandApplication) -> OBDTypeWidget, val icon: Int?) {
		VIN({ OBDTypeWidget.VIN }, null),
		FUEL_TYPE({ OBDTypeWidget.FUEL_TYPE }, R.drawable.ic_action_fuel_tank),
		TEMPERATURE_INTAKE({ OBDTypeWidget.TEMPERATURE_INTAKE }, R.drawable.ic_action_obd_temperature_intake),
		TEMPERATURE_AMBIENT({ OBDTypeWidget.TEMPERATURE_AMBIENT }, R.drawable.ic_action_obd_temperature_outside),
		TEMPERATURE_COOLANT({ OBDTypeWidget.TEMPERATURE_COOLANT }, R.drawable.ic_action_obd_temperature_coolant),
		ENGINE_OIL_TEMPERATURE({ OBDTypeWidget.ENGINE_OIL_TEMPERATURE }, R.drawable.ic_action_obd_temperature_engine_oil),
		RPM({ OBDTypeWidget.RPM }, R.drawable.ic_action_obd_engine_speed),
		SPEED({ OBDTypeWidget.SPEED }, R.drawable.ic_action_obd_speed),
		FUEL_CONSUMPTION_RATE(
			{ app ->
				if (app.settings.UNIT_OF_VOLUME.get() == VolumeUnit.LITRES)
					OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM
				else
					OBDTypeWidget.FUEL_CONSUMPTION_RATE_M_PER_LITER
			}, R.drawable.ic_action_obd_fuel_consumption
		),
		FUEL_LEFT_LITER({ OBDTypeWidget.FUEL_LEFT_LITER }, R.drawable.ic_action_obd_fuel_remaining),
		CALCULATED_ENGINE_LOAD({ OBDTypeWidget.CALCULATED_ENGINE_LOAD }, R.drawable.ic_action_car_info),
		FUEL_PRESSURE({ OBDTypeWidget.FUEL_PRESSURE }, R.drawable.ic_action_obd_fuel_pressure),
		THROTTLE_POSITION({ OBDTypeWidget.THROTTLE_POSITION }, R.drawable.ic_action_obd_throttle_position),
		BATTERY_VOLTAGE({ OBDTypeWidget.BATTERY_VOLTAGE }, R.drawable.ic_action_obd_battery_voltage),
		ADAPTER_BATTERY_VOLTAGE({ OBDTypeWidget.ADAPTER_BATTERY_VOLTAGE}, R.drawable.ic_action_obd2_connector_voltage)
	}

	data class OBDDataItem(val dataType: OBDDataType, val widget: OBDComputerWidget)

	private val uiHandler = Handler(Looper.getMainLooper())
	private var updateWidgetsHandler: Handler? = null
	private val items = mutableListOf<Any>()

	private lateinit var adapter: OBDMainFragmentAdapter
	private var progress: View? = null

	private lateinit var device: BTDeviceInfo

	private var updateEnable = false
	private var deviceConnectionState: OBDConnectionState = OBDConnectionState.DISCONNECTED

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.apply {
			val connectedDevice = vehicleMetricsPlugin.getConnectedDeviceInfo()
			val deviceName = getString(DEVICE_NAME_KEY) ?: ""
			val deviceAddress = getString(DEVICE_ADDRESS_KEY) ?: ""
			val isBLE = getBoolean(DEVICE_IS_BLE_KEY)
			device = if (connectedDevice != null &&
				(deviceAddress == connectedDevice.address && isBLE == connectedDevice.isBLE||
						(Algorithms.isEmpty(deviceAddress) && Algorithms.isEmpty(deviceName)))) {
				deviceConnectionState = OBDConnectionState.CONNECTED
				connectedDevice
			} else {

				deviceConnectionState = OBDConnectionState.DISCONNECTED
				BTDeviceInfo(deviceName, deviceAddress, isBLE)
			}
			if(isBLE) {
				val bleDevice = vehicleMetricsPlugin.getBLEOBDDeviceById(deviceAddress)
				if(bleDevice?.isConnecting == true) {
					deviceConnectionState = OBDConnectionState.CONNECTING
				}
			}
		}
	}

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.activity_background_color_light
	}

	override val layoutId: Int
		get() = R.layout.fragment_obd_main

	override fun setupToolbar(view: View) {
		view.findViewById<ImageButton>(R.id.close_button).apply {
			setOnClickListener {
				requireActivity().onBackPressed()
			}
			setImageResource(AndroidUtils.getNavigationIconResId(context))
		}
	}

	override fun setupUI(view: View) {
		progress = view.findViewById(R.id.progress_bar)
		items.clear()
		setupConnectionState(view)
		updateButtonState(view)
		setupVehicleInfo()
		setupReceivedData()
		setupSettingsCard()
		setupList(view)
	}

	private fun setupList(view: View) {
		adapter = OBDMainFragmentAdapter(app, nightMode, requireMapActivity(), device, this)
		val recycler = view.findViewById<RecyclerView>(R.id.recycler_view)
		recycler?.adapter = adapter
		recycler?.itemAnimator = null
		adapter.items = ArrayList(items)
	}

	private fun setupConnectionState(view: View) {
		val connected = deviceConnectionState == OBDConnectionState.CONNECTED

		val connectedText = app.getString(
			if (connected) R.string.external_device_connected else R.string.external_device_disconnected
		)
		view.findViewById<TextView>(R.id.device_name).text = device.name
		val protocolStringId = if(device.isBLE)  R.string.external_device_ble else R.string.shared_string_bluetooth
		view.findViewById<TextView>(R.id.connection_state).text = app.getString(
			R.string.ltr_or_rtl_combine_via_comma,
			connectedText,
			app.getString(protocolStringId)
		)

		view.findViewById<ImageView?>(R.id.widget_icon).apply {
			background = uiUtilities.getIcon(
				if (connected) {
					if (nightMode) R.drawable.bg_widget_type_icon_dark else R.drawable.bg_widget_type_icon_light
				} else {
					if (nightMode) R.drawable.bg_widget_type_disconnected_icon_dark else R.drawable.bg_widget_type_disconnected_icon_light
				}
			)
			if (connected) {
				setImageDrawable(uiUtilities.getIcon(if (nightMode) R.drawable.widget_obd_car_day else R.drawable.widget_obd_car_night))
			} else {
				setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_car_obd2))
			}
		}
	}

	private fun updateButtonState(view: View) {
		val pairButtonText = view.findViewById<TextView>(R.id.button_text)
		val pairButton = view.findViewById<View>(R.id.pair_btn)
		val pairButtonContainer = view.findViewById<View>(R.id.button_container)
		val connectedStateBtnTextColor = ColorUtilities.getButtonSecondaryTextColorId(nightMode)
		val disconnectedStateBtnTextColor =
			if (nightMode) R.color.dlg_btn_primary_text_dark else R.color.dlg_btn_primary_text_light

		val connectedStateBtnBgColorLight = R.drawable.dlg_btn_secondary_light
		val connectedStateBtnBgColorDark = R.drawable.dlg_btn_secondary_dark

		val disconnectedStateBtnBgColorLight = R.drawable.dlg_btn_primary_light
		val disconnectedStateBtnBgColorDark = R.drawable.dlg_btn_primary_dark

		val connectingStateBtnBgColorLight = R.color.active_color_secondary_light
		val connectingStateBtnBgColorDark = R.color.active_color_secondary_dark

		val lightResId: Int
		val darkResId: Int
		var pairBtnTextColorId = 0
		var pairBtnTextId = 0
		var isConnecting = false
		when (deviceConnectionState) {
			OBDConnectionState.CONNECTED -> {
				lightResId = connectedStateBtnBgColorLight
				darkResId = connectedStateBtnBgColorDark
				pairBtnTextColorId = connectedStateBtnTextColor
				pairBtnTextId = R.string.external_device_details_disconnect
				pairButton.setOnClickListener {
					vehicleMetricsPlugin.disconnect(true)
				}
			}

			OBDConnectionState.CONNECTING -> {
				lightResId = connectingStateBtnBgColorLight
				darkResId = connectingStateBtnBgColorDark
				pairButton.setOnClickListener(null)
				isConnecting = true
			}

			else -> {
				lightResId = disconnectedStateBtnBgColorLight
				darkResId = disconnectedStateBtnBgColorDark
				pairBtnTextColorId = disconnectedStateBtnTextColor
				pairBtnTextId = R.string.external_device_details_connect
				pairButton.setOnClickListener {
					vehicleMetricsPlugin.connectToObd(requireActivity(), device)
				}
			}
		}
		view.post {
			AndroidUtils.setBackground(
				app,
				pairButtonContainer,
				nightMode,
				R.drawable.ripple_solid_light,
				R.drawable.ripple_solid_dark
			)
			AndroidUtils.setBackground(
				app,
				pairButton,
				nightMode,
				lightResId,
				darkResId
			)
			if (pairBtnTextId != 0) {
				pairButtonText.text = getString(pairBtnTextId)
			}
			if (pairBtnTextColorId != 0) {
				val colorStateList = ContextCompat.getColorStateList(app, pairBtnTextColorId)
				pairButtonText.setTextColor(colorStateList)
			}
			progress?.visibility = if (isConnecting) View.VISIBLE else View.GONE
			pairButtonText.visibility = if (isConnecting) View.GONE else View.VISIBLE
		}
	}

	override fun onStateChanged(
		state: OBDConnectionState,
		deviceInfo: BTDeviceInfo) {
		if (device.address == deviceInfo.address && device.isBLE == deviceInfo.isBLE) {
			deviceConnectionState = state
		}
		view?.let {
			setupConnectionState(it)
			updateButtonState(it)
		}
	}

	private fun setupVehicleInfo() {
		items.add(OBDMainFragmentAdapter.ITEM_DIVIDER)
		items.add(OBDMainFragmentAdapter.TITLE_VEHICLE_TYPE)
		items.add(OBDDataItem(OBDDataType.VIN, OBDDataComputer.registerWidget(OBDTypeWidget.VIN, 0)))
	}

	private fun setupReceivedData() {
		items.add(OBDMainFragmentAdapter.ITEM_DIVIDER)
		items.add(OBDMainFragmentAdapter.TITLE_RECEIVED_TYPE)
		OBDDataType.entries.forEach {
			if (it.widgetType(app) != OBDTypeWidget.VIN) {
				items.add(OBDDataItem(it, OBDDataComputer.registerWidget(it.widgetType(app), it.widgetType(app).defaultAverageTime)))
			}
		}
	}

	private fun setupSettingsCard() {
		items.add(OBDMainFragmentAdapter.ITEM_DIVIDER)
		items.add(OBDMainFragmentAdapter.TITLE_SETTINGS_TYPE)
		items.add(OBDMainFragmentAdapter.NAME_ITEM_TYPE)
		items.add(OBDMainFragmentAdapter.FORGET_SENSOR_TYPE)
	}

	override fun onStart() {
		super.onStart()
		handlerThread = HandlerThread("Update OBD Widgets")
		handlerThread?.start()
		handlerThread?.apply { updateWidgetsHandler = Handler(looper) }
		updateWidgets()
	}

	override fun onStop() {
		super.onStop()
		updateWidgetsHandler = null
		handlerThread?.quitSafely()
		handlerThread = null
	}

	private fun updateWidgets() {
		items.forEach {
			if (it is OBDDataItem) {
				val widget = it.widget
				val value = vehicleMetricsPlugin.getWidgetValue(widget)
				val unit = vehicleMetricsPlugin.getWidgetUnit(widget)
				adapter.let { obdAdapter ->
					val savedValue = obdAdapter.lastSavedValueMap[widget]
					if (!savedValue?.first.equals(value) or !savedValue?.second.equals(unit)) {
						uiHandler.post {
							adapter.notifyItemChanged(items.indexOf(it))
						}
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		updateEnable = true
		startHandler()
		vehicleMetricsPlugin.setConnectionStateListener(this)
	}

	private fun startHandler() {
		updateWidgetsHandler?.postDelayed({
			updateWidgetsHandler?.removeCallbacksAndMessages(null)
			if (view != null && updateEnable) {
				updateWidgets()
				startHandler()
			}
		}, UPDATE_INTERVAL_MILLIS)
	}

	override fun onPause() {
		super.onPause()
		updateEnable = false
		vehicleMetricsPlugin.setConnectionStateListener(null)
	}

	companion object {
		const val TAG: String = "VehicleMetricsFragment"
		const val UPDATE_INTERVAL_MILLIS = 100L
		const val DEVICE_NAME_KEY = "DEVICE_NAME_KEY"
		const val DEVICE_ADDRESS_KEY = "DEVICE_ADDRESS_KEY"
		const val DEVICE_IS_BLE_KEY = "DEVICE_IS_BLE_KEY"

		fun showInstance(manager: FragmentManager, device: BTDeviceInfo) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = OBDMainFragment()
				val args = Bundle()
				args.putString(DEVICE_NAME_KEY, device.name)
				args.putString(DEVICE_ADDRESS_KEY, device.address)
				args.putBoolean(DEVICE_IS_BLE_KEY, device.isBLE)
				fragment.arguments = args
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun onNameChanged() {
		view?.findViewById<TextView>(R.id.device_name)?.text = device.name
		adapter.notifyDataSetChanged()
	}

	override fun onForgetSensorConfirmed(deviceId: String, isBLE: Boolean) {
		vehicleMetricsPlugin.removeDeviceToUsedOBDDevicesList(deviceId, isBLE)
		view?.let { setupUI(it) }
	}
}
