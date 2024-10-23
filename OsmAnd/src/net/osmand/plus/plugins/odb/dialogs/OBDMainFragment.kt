package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.plugins.odb.adapters.OBDMainFragmentAdapter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.util.Algorithms

class OBDMainFragment : OBDDevicesBaseFragment(), VehicleMetricsPlugin.ConnectionStateListener {

	private val handler = Handler(Looper.getMainLooper())
	private val items = mutableListOf<Any>()

	private lateinit var adapter: OBDMainFragmentAdapter
	private var progress: View? = null

	private lateinit var device: BTDeviceInfo

	private var updateEnable = false
	private var currentConnectedState: VehicleMetricsPlugin.OBDConnectionState =
		VehicleMetricsPlugin.OBDConnectionState.DISCONNECTED

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.let {
			val connectedDevice = vehicleMetricsPlugin?.getConnectedDeviceInfo()
			val deviceName = it.getString(DEVICE_NAME_KEY) ?: ""
			val deviceAddress = it.getString(DEVICE_ADDRESS_KEY) ?: ""
			device = if (connectedDevice != null &&
				(deviceAddress == connectedDevice.address || Algorithms.isEmpty(deviceAddress))) {
				currentConnectedState = VehicleMetricsPlugin.OBDConnectionState.CONNECTED
				connectedDevice
			} else {
				currentConnectedState = VehicleMetricsPlugin.OBDConnectionState.DISCONNECTED
				BTDeviceInfo(deviceName, deviceAddress)
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
		setupConnectionState(view)
		updateButtonState(view)
		setupVehicleInfo()
		setupReceivedData()
		setupList(view)
	}

	private fun setupList(view: View) {
		adapter = OBDMainFragmentAdapter(app, nightMode, requireMapActivity())
		view.findViewById<RecyclerView>(R.id.recycler_view)?.adapter = adapter
		adapter.items = ArrayList(items)
	}

	private fun setupConnectionState(view: View) {
		val connected = currentConnectedState == VehicleMetricsPlugin.OBDConnectionState.CONNECTED

		val connectedText = app.getString(
			if (connected) R.string.external_device_connected else R.string.external_device_disconnected
		)
		view.findViewById<TextView>(R.id.device_name).text = device.name
		view.findViewById<TextView>(R.id.connection_state).text = app.getString(
			R.string.ltr_or_rtl_combine_via_comma,
			connectedText,
			app.getString(R.string.shared_string_bluetooth)
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
		when (currentConnectedState) {
			VehicleMetricsPlugin.OBDConnectionState.CONNECTED -> {
				lightResId = connectedStateBtnBgColorLight
				darkResId = connectedStateBtnBgColorDark
				pairBtnTextColorId = connectedStateBtnTextColor
				pairBtnTextId = R.string.external_device_details_disconnect
				pairButton.setOnClickListener {
					Thread {
						vehicleMetricsPlugin?.disconnect()
					}.start()
				}
			}

			VehicleMetricsPlugin.OBDConnectionState.CONNECTING -> {
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
					Thread {
						vehicleMetricsPlugin?.connectToObd(requireActivity(), device)
					}.start()
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
		state: VehicleMetricsPlugin.OBDConnectionState,
		deviceInfo: BTDeviceInfo?) {
		if (device.address == deviceInfo?.address) {
			currentConnectedState = state
		}
		app.runInUIThread {
			view?.let {
				setupConnectionState(it)
				updateButtonState(it)
			}
		}
	}

	private fun setupVehicleInfo() {
		items.add(OBDMainFragmentAdapter.ITEM_DIVIDER)
		items.add(OBDMainFragmentAdapter.TITLE_VEHICLE_TYPE)
		val widget = OBDDataComputer.registerWidget(OBDTypeWidget.VIN, 0)
		items.add(widget)
	}

	private fun setupReceivedData() {
		items.add(OBDMainFragmentAdapter.ITEM_DIVIDER)
		items.add(OBDMainFragmentAdapter.TITLE_RECEIVED_TYPE)
		OBDTypeWidget.entries.forEach {
			if (it != OBDTypeWidget.VIN) {
				val widget = OBDDataComputer.registerWidget(it, 0)
				items.add(widget)
			}
		}
	}

	override fun onStart() {
		super.onStart()
		updateWidgets()
	}

	private fun updateWidgets() {
		items.forEach {
			if (it is OBDComputerWidget) {
				val value = vehicleMetricsPlugin?.getWidgetValue(it)
				val unit = vehicleMetricsPlugin?.getWidgetUnit(it)
				val widget = adapter.lastSavedValueMap[it]
				if (!widget?.first.equals(value) or !widget?.second.equals(unit)) {
					adapter.notifyItemChanged(
						items.indexOf(it),
						OBDMainFragmentAdapter.UPDATE_VALUE_PAYLOAD_TYPE
					)
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		updateEnable = true
		startHandler()
		vehicleMetricsPlugin?.setConnectionStateListener(this)
	}

	private fun startHandler() {
		handler.postDelayed({
			if (view != null && updateEnable) {
				updateWidgets()
				startHandler()
			}
		}, UPDATE_INTERVAL_MILLIS)
	}

	override fun onPause() {
		super.onPause()
		updateEnable = false
		vehicleMetricsPlugin?.setConnectionStateListener(null)
	}

	companion object {
		const val TAG: String = "VehicleMetricsFragment"
		const val UPDATE_INTERVAL_MILLIS = 100L
		const val DEVICE_NAME_KEY = "DEVICE_NAME_KEY"
		const val DEVICE_ADDRESS_KEY = "DEVICE_ADDRESS_KEY"

		fun showInstance(manager: FragmentManager, device: BTDeviceInfo) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = OBDMainFragment()
				val args = Bundle()
				args.putString(DEVICE_NAME_KEY, device.name)
				args.putString(DEVICE_ADDRESS_KEY, device.address)
				fragment.arguments = args
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}
}