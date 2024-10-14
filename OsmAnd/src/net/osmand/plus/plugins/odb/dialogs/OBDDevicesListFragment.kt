package net.osmand.plus.plugins.odb.dialogs

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.plugins.odb.adapters.OBDDevicesAdapter
import net.osmand.plus.plugins.odb.dialogs.RenameOBDDialog.OnSensorNameChangedCallback
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.plus.widgets.dialogbutton.DialogButtonType
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.util.Algorithms

class OBDDevicesListFragment : OBDDevicesBaseFragment(),
	OBDDevicesAdapter.OBDDeviceItemListener,
	OnSensorNameChangedCallback, VehicleMetricsPlugin.ConnectionStateListener

{
	protected var dividerBeforeButton: View? = null
	protected var dividerBetweenDeviceGroups: View? = null
	protected var emptyView: View? = null
	protected var contentView: View? = null
	protected var connectedPrompt: View? = null
	protected var disconnectedPrompt: View? = null
	protected var connectedList: RecyclerView? = null
	protected var disconnectedList: RecyclerView? = null
	private var connectedListAdapter: OBDDevicesAdapter? = null
	private var disconnectedListAdapter: OBDDevicesAdapter? = null
	private var appBar: AppBarLayout? = null
	private var noBluetoothCard: View? = null
	override val layoutId: Int
		get() = R.layout.fragment_obd_devices_list

	override fun setupUI(view: View) {
		super.setupUI(view)
		emptyView = view.findViewById(R.id.empty_view)
		contentView = view.findViewById(R.id.devices_content)
		connectedList = view.findViewById(R.id.connected_devices_list)
		disconnectedList = view.findViewById(R.id.disconnected_devices_list)
		connectedPrompt = view.findViewById(R.id.connected_prompt)
		disconnectedPrompt = view.findViewById(R.id.disconnected_prompt)
		appBar = view.findViewById(R.id.appbar)
		noBluetoothCard = view.findViewById(R.id.no_bluetooth_card)
		dividerBeforeButton = view.findViewById(R.id.pair_btn_additional_divider)
		dividerBetweenDeviceGroups = view.findViewById(R.id.divider_between_device_groups)
		val sensorIcon = view.findViewById<ImageView>(R.id.sensor_icon)
		sensorIcon.setBackgroundResource(if (nightMode) R.drawable.bg_empty_external_device_list_icon_night else R.drawable.bg_empty_external_device_list_icon_day)
		sensorIcon.setImageResource(if (nightMode) R.drawable.img_help_vehicle_metrics_night else R.drawable.img_help_vehicle_metrics_day)
		val docsLinkText = app.getString(R.string.learn_more_about_obd_sensors)
		val spannable =
			UiUtilities.createClickableSpannable(docsLinkText, docsLinkText) { unused: Void? ->
				val activity = activity
				if (activity != null) {
					AndroidUtils.openUrl(
						activity,
						Uri.parse(getString(R.string.docs_obd_sensors)),
						nightMode)
				}
				false
			}
		val learnMore = view.findViewById<TextView>(R.id.learn_more_button)
		UiUtilities.setupClickableText(learnMore, spannable, nightMode)
		setupPairSensorButton(view.findViewById(R.id.pair_btn_empty))
		setupPairSensorButton(view.findViewById(R.id.pair_btn_additional))
		setupOpenBtSettingsButton(view.findViewById(R.id.bt_settings_button_container))
		connectedListAdapter = OBDDevicesAdapter(app, nightMode, this)
		disconnectedListAdapter = OBDDevicesAdapter(app, nightMode, this)
		connectedList?.adapter = connectedListAdapter
		disconnectedList?.adapter = disconnectedListAdapter
	}

	private fun setupPairSensorButton(view: View) {
		val dismissButton = view.findViewById<DialogButton>(R.id.dismiss_button)
		dismissButton.setButtonType(DialogButtonType.SECONDARY)
		dismissButton.setTitleId(R.string.ant_plus_pair_new_sensor)
		val layoutParams = dismissButton.layoutParams
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
		dismissButton.layoutParams = layoutParams
		view.requestLayout()
		dismissButton.setOnClickListener { v: View? -> showPairNewSensorBottomSheet() }
		AndroidUiHelper.updateVisibility(dismissButton, true)
	}

	private fun setupOpenBtSettingsButton(view: View) {
		val dismissButton = view.findViewById<DialogButton>(R.id.dismiss_button)
		dismissButton.setButtonType(DialogButtonType.SECONDARY)
		dismissButton.setTitleId(R.string.ant_plus_open_settings)
		val layoutParams = dismissButton.layoutParams
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
		dismissButton.layoutParams = layoutParams
		view.requestLayout()
		dismissButton.setOnClickListener { v: View? ->
			val intentOpenBluetoothSettings = Intent()
			intentOpenBluetoothSettings.setAction(Settings.ACTION_BLUETOOTH_SETTINGS)
			startActivity(intentOpenBluetoothSettings)
		}
		AndroidUiHelper.updateVisibility(dismissButton, true)
	}

	private fun showPairNewSensorBottomSheet() {
		OBDDevicesSearchFragment.showInstance(requireActivity().supportFragmentManager)
	}

	override fun setupToolbar(view: View) {
		super.setupToolbar(view)
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setOnMenuItemClickListener { item: MenuItem ->
			if (item.itemId == R.id.action_add) {
				showPairNewSensorBottomSheet()
			}
			false
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		contentView = null
		emptyView = null
		connectedList = null
		disconnectedList = null
		connectedPrompt = null
		disconnectedPrompt = null
		appBar = null
		noBluetoothCard = null
	}

	override fun onResume() {
		super.onResume()
		vehicleMetricsPlugin?.setConnectionStateListener(this)
		noBluetoothCard!!.visibility =
			if (AndroidUtils.isBluetoothEnabled(requireActivity())) View.GONE else View.VISIBLE
		updatePairedSensorsList()
	}

	override fun onPause() {
		super.onPause()
		vehicleMetricsPlugin?.setConnectionStateListener(null)
	}
	private fun updatePairedSensorsList() {
		vehicleMetricsPlugin?.let { plugin ->
			val connectedDevice = plugin.getConnectedDeviceInfo()
			val connectedDevices: List<BTDeviceInfo> =
				if (connectedDevice == null) emptyList() else arrayListOf(connectedDevice)
			val disconnectedDevices =
				plugin.getUsedOBDDevicesList().filter { it.address != connectedDevice?.address }
			if (Algorithms.isEmpty(disconnectedDevices) && Algorithms.isEmpty(connectedDevices)) {
				emptyView!!.visibility = View.VISIBLE
				contentView!!.visibility = View.GONE
				app.runInUIThread { appBar?.setExpanded(true, false) }
			} else {
				app.runInUIThread {
					appBar?.setExpanded(false, false)
					connectedListAdapter!!.items = ArrayList<Any>(connectedDevices)
					disconnectedListAdapter!!.items = ArrayList<Any>(disconnectedDevices)
					contentView!!.visibility = View.VISIBLE
					emptyView!!.visibility = View.GONE
					val hasConnectedDevices = connectedDevices.isNotEmpty()
					val hasDisConnectedDevices = disconnectedDevices.isNotEmpty()
					connectedPrompt!!.visibility =
						if (hasConnectedDevices) View.VISIBLE else View.GONE
					disconnectedPrompt!!.visibility =
						if (hasDisConnectedDevices) View.VISIBLE else View.GONE
					dividerBetweenDeviceGroups!!.visibility =
						if (hasConnectedDevices && hasDisConnectedDevices) View.VISIBLE else View.GONE
				}
			}
		}
	}

	override fun onDisconnect(device: BTDeviceInfo) {
		vehicleMetricsPlugin?.disconnect()
	}

	override fun onConnect(device: BTDeviceInfo) {
		vehicleMetricsPlugin?.connectToObd(requireActivity(), device)
	}

	override fun onSettings(device: BTDeviceInfo) {
		OBDMainFragment.showInstance(requireActivity().supportFragmentManager, device)
	}

	override fun onRename(device: BTDeviceInfo) {
		RenameOBDDialog.showInstance(
			requireActivity(),
			this,
			device)
	}

	override fun onForget(device: BTDeviceInfo) {
	}

//	override fun onForgetSensorConfirmed(device:BTDeviceInfo) {
////		plugin.unpairDevice(device)
//		updatePairedSensorsList()
//	}

	override fun onSensorNameChanged() {
		updatePairedSensorsList()
	}

	override fun onDeviceClicked(device: BTDeviceInfo) {
		OBDMainFragment.showInstance(requireActivity().supportFragmentManager, device)
	}

	companion object {
		val TAG = OBDDevicesListFragment::class.java.simpleName
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = OBDDevicesListFragment()
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun onStateChanged(state: VehicleMetricsPlugin.OBDConnectionState) {
		app.runInUIThread { updatePairedSensorsList() }
	}
}