package net.osmand.plus.plugins.odb.dialogs

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.plus.widgets.dialogbutton.DialogButtonType
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget

class OBDMainFragment : OBDDevicesBaseFragment(), VehicleMetricsPlugin.ConnectionStateListener {

	private val handler = Handler(Looper.getMainLooper())
	private val widgets = mutableListOf<OBDComputerWidget>()
	private val dataRows = mutableListOf<View>()

	private lateinit var device: BTDeviceInfo

	private var updateEnable = false

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
		setupConnectionState(view)
		setupConnectionButton(view)
		setupVehicleInfo(view)
		setupReceivedData(view)
	}

	private fun setupConnectionState(view: View) {
		val connected = vehicleMetricsPlugin!!.isConnected()

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

	private fun setupConnectionButton(view: View) {
		val container = view.findViewById<ViewGroup>(R.id.pair_btn)
		container.removeAllViews()

		val button = DialogButton(view.context)
		if (vehicleMetricsPlugin?.isConnected() == true) {
			button.setButtonType(DialogButtonType.SECONDARY)
			button.setTitle(getString(R.string.external_device_details_disconnect))
		} else {
			button.setButtonType(DialogButtonType.PRIMARY)
			button.setTitle(getString(R.string.external_device_details_connect))
		}
		button.setOnClickListener { toggleConnection() }
		container.addView(button)
	}

	private fun toggleConnection() {
		vehicleMetricsPlugin?.let {
			if (it.getConnectedDeviceName() != null) {
				it.disconnect()
			} else {
				Thread { it.connectToObd(requireActivity(), device) }.start()
			}
		}
	}

	override fun onStateChanged(state: VehicleMetricsPlugin.OBDConnectionState) {
		app.runInUIThread {
			view?.let {
				setupConnectionState(it)
				setupConnectionButton(it)
			}
		}
	}

	private fun setupVehicleInfo(view: View) {
		view.findViewById<ViewGroup>(R.id.info_container)?.apply {
			removeAllViews()
			createWidgetView(OBDTypeWidget.VIN, this, true)
		}
	}

	private fun setupReceivedData(view: View) {
		view.findViewById<ViewGroup>(R.id.data_container)?.apply {
			removeAllViews()
			OBDTypeWidget.entries.forEach {
				if (it != OBDTypeWidget.VIN) {
					createWidgetView(it, this, it == OBDTypeWidget.entries.last())
				}
			}
		}
	}

	private fun createWidgetView(
		widgetType: OBDTypeWidget,
		container: ViewGroup,
		lastItem: Boolean
	) {
		val widget = OBDDataComputer.registerWidget(widgetType, 0)
		widgets.add(widget)

		val itemView = themedInflater.inflate(R.layout.device_characteristic_item, container, false)
		itemView.findViewById<TextView>(R.id.title).text = widget.type.getTitle()
		itemView.tag = widget
		container.addView(itemView)
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), !lastItem)
		dataRows.add(itemView)
	}

	override fun onStart() {
		super.onStart()
		updateWidgets()
	}

	private fun updateWidgetsData(view: View, widget: OBDComputerWidget) {
		val value = if (widget.computeValue() == null) " - " else widget.computeValue().toString()
		view.findViewById<TextView>(R.id.value).apply {
			if (text.toString() != value) {
				text = value
			}
		}
	}

	private fun updateWidgets() {
		dataRows.forEach {
			if (it.tag is OBDComputerWidget) {
				app.runInUIThread { updateWidgetsData(it, it.tag as OBDComputerWidget) }
			}
		}
		handler.postDelayed({ updateWidgets() }, 100)
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

	override fun onDestroy() {
		super.onDestroy()
		widgets.forEach { OBDDataComputer.removeWidget(it) }
	}

	companion object {
		const val TAG: String = "VehicleMetricsFragment"
		const val UPDATE_INTERVAL_MILLIS = 100L

		fun showInstance(manager: FragmentManager, device: BTDeviceInfo) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = OBDMainFragment()
				fragment.device = device
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}
}