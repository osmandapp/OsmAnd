package net.osmand.plus.plugins.odb.dialogs

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.plugins.odb.adapters.OBDDevicesAdapter
import net.osmand.plus.plugins.odb.adapters.OBDDevicesAdapter.OBDDeviceItemListener
import net.osmand.plus.plugins.odb.adapters.PairedDevicesAdapter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.shared.data.BTDeviceInfo

class OBDDevicesSearchFragment : OBDDevicesBaseFragment(), VehicleMetricsPlugin.ScanDevicesListener,
	PairedDevicesAdapter.PairedDevicesMenuListener {

	private var currentState = SearchStates.NOTHING_FOUND
	private var stateNoBluetoothView: View? = null
	private var stateSearchingView: View? = null
	private var stateNothingFoundView: View? = null
	private var stateDevicesListView: View? = null
	private var foundDevicesCountView: TextView? = null
	private lateinit var pairedDevicesAdapter: PairedDevicesAdapter
	private lateinit var foundDevicesAdapter: OBDDevicesAdapter
	private var bleSearch: Boolean = false
	private var antSearch: Boolean = false
	private var devicesList: List<BTDeviceInfo>? = null

	companion object {
		val TAG: String = OBDDevicesSearchFragment::class.java.simpleName
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = OBDDevicesSearchFragment()
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}

	override val layoutId: Int
		get() = R.layout.fragment_obd_search


	override fun setupUI(view: View) {
		super.setupUI(view)
		setupNoBluetoothView(view)
		setupSearchingView(view)
		setupNothingFoundView(view)
		setupDevicesListView(view)
	}

	private fun setupNoBluetoothView(parentView: View) {
		stateNoBluetoothView = parentView.findViewById(R.id.state_no_bluetooth)
		val openSettingButton =
			stateNoBluetoothView?.findViewById<DialogButton>(R.id.dismiss_button)
		openSettingButton?.setButtonType(SECONDARY)
		openSettingButton?.setTitleId(R.string.ant_plus_open_settings)
		openSettingButton?.setOnClickListener {
			val intentOpenBluetoothSettings = Intent()
			intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
			startActivity(intentOpenBluetoothSettings)
		}
		AndroidUiHelper.updateVisibility(openSettingButton, true)
	}

	private fun setupSearchingView(parentView: View) {
		stateSearchingView = parentView.findViewById(R.id.state_searching)
		val progressBar = parentView.findViewById<MaterialProgressBar>(R.id.progressBar)
		progressBar.showProgressBackground = true
	}

	private fun setupNothingFoundView(parentView: View) {
		stateNothingFoundView = parentView.findViewById(R.id.state_nothing_found)
		val searchAgain = stateNothingFoundView?.findViewById<DialogButton>(R.id.dismiss_button)
		searchAgain?.setButtonType(SECONDARY)
		searchAgain?.setTitleId(R.string.ble_search_again)
		searchAgain?.setOnClickListener {
			startSearch()
		}
		AndroidUiHelper.updateVisibility(searchAgain, true)
	}

	private fun setupDevicesListView(parentView: View) {
		stateDevicesListView = parentView.findViewById(R.id.state_found_devices_list)
		foundDevicesCountView = stateDevicesListView?.findViewById(R.id.found_devices_count)
		val recyclerView: RecyclerView? =
			stateDevicesListView?.findViewById(R.id.paired_devices_list)
		recyclerView?.layoutManager = LinearLayoutManager(context)
		pairedDevicesAdapter = PairedDevicesAdapter(app, nightMode, this)
		recyclerView?.adapter = pairedDevicesAdapter
		val foundDevicesRecyclerView: RecyclerView? =
			stateDevicesListView?.findViewById(R.id.found_devices_list)
		foundDevicesRecyclerView?.layoutManager = LinearLayoutManager(context)
		foundDevicesAdapter = PairedDevicesAdapter(app, nightMode, this)
		foundDevicesRecyclerView?.adapter = foundDevicesAdapter
	}

	private fun bindFoundDevices(devices: List<BTDeviceInfo>) {
		if (devices.isEmpty()) {
			setCurrentState(SearchStates.NOTHING_FOUND)
		} else {
			setCurrentState(SearchStates.DEVICES_LIST)
			val formatString = activity?.resources?.getString(R.string.bluetooth_found_title)
			formatString?.let {
				foundDevicesCountView?.text =
					String.format(formatString, devices.size)
			}
			pairedDevicesAdapter.items = devices
		}
	}

	override fun onStart() {
		super.onStart()
		pairedDevicesAdapter.items =
			vehicleMetricsPlugin?.getPairedOBDDevicesList(requireActivity()) ?: emptyList()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val newView = super.onCreateView(inflater, container, savedInstanceState)
		updateCurrentStateView()
		return newView
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		currentState = if (!AndroidUtils.isBluetoothEnabled(requireActivity())) {
			SearchStates.NO_BLUETOOTH
		} else {
			SearchStates.DEVICES_LIST
		}
	}

	override fun onResume() {
		super.onResume()
		if (currentState == SearchStates.NO_BLUETOOTH && AndroidUtils.isBluetoothEnabled(
				requireActivity())) {
			setCurrentState(SearchStates.DEVICES_LIST)
		}
		if (currentState == SearchStates.DEVICES_LIST) {
			startSearch()
		}
	}

	private fun startSearch() {
		vehicleMetricsPlugin?.setScanDevicesListener(this)
	}

	override fun onPause() {
		super.onPause()
		vehicleMetricsPlugin?.setScanDevicesListener(null)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		stateNoBluetoothView = null
		stateSearchingView = null
		stateNothingFoundView = null
		stateDevicesListView = null
	}

	private fun setCurrentState(newState: SearchStates) {
		if (currentState != newState) {
			currentState = newState
			updateCurrentStateView()
		}
	}

	private fun updateCurrentStateView() {
		AndroidUiHelper.updateVisibility(
			stateNoBluetoothView,
			currentState == SearchStates.NO_BLUETOOTH
		)
		AndroidUiHelper.updateVisibility(stateSearchingView, false)
		AndroidUiHelper.updateVisibility(
			stateNothingFoundView,
			currentState == SearchStates.NOTHING_FOUND
		)
		AndroidUiHelper.updateVisibility(
			stateDevicesListView,
			currentState == SearchStates.DEVICES_LIST
		)
	}

	override fun onScanFinished(foundDevices: List<BTDeviceInfo>) {
		bindFoundDevices(foundDevices)
	}

	internal enum class SearchStates {
		NO_BLUETOOTH, NOTHING_FOUND, DEVICES_LIST
	}

	override fun onConnect(device: BTDeviceInfo) {
		vehicleMetricsPlugin?.connectToObd(requireActivity(), device)
		activity?.onBackPressed()
	}

	override fun onSave(device: BTDeviceInfo) {
		vehicleMetricsPlugin?.saveDeviceToUsedOBDDevicesList(device)
	}

	override fun onForget(device: BTDeviceInfo) {
	}

	fun onDeviceClicked(device: BTDeviceInfo) {
        OBDMainFragment.showInstance(requireActivity().supportFragmentManager, device)
    }
}