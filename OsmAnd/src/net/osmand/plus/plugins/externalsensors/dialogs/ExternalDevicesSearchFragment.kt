package net.osmand.plus.plugins.externalsensors.dialogs

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
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin.ScanDevicesListener
import net.osmand.plus.plugins.externalsensors.adapters.FoundDevicesAdapter
import net.osmand.plus.plugins.externalsensors.adapters.FoundDevicesAdapter.DeviceClickListener
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY
import net.osmand.plus.widgets.dialogbutton.DialogButton

class ExternalDevicesSearchFragment : ExternalDevicesBaseFragment(), ScanDevicesListener,
    DeviceClickListener {

    private var currentState = SearchStates.NOTHING_FOUND
    private var stateNoBluetoothView: View? = null
    private var stateSearchingView: View? = null
    private var stateNothingFoundView: View? = null
    private var stateDevicesListView: View? = null
    private var foundDevicesCountView: TextView? = null
    private lateinit var adapter: FoundDevicesAdapter
    private var bleSearch: Boolean = false
    private var antSearch: Boolean = false

    companion object {
        val TAG: String = Companion::class.java.simpleName
        private const val BLE_SEARCH_KEY: String = "BLE_SEARCH"
        private const val ANT_SEARCH_KEY: String = "ANT_SEARCH"
        fun showInstance(manager: FragmentManager, bleSearch: Boolean, antSearch: Boolean) {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = ExternalDevicesSearchFragment()
                val args = Bundle()
                if (bleSearch) {
                    args.putBoolean(BLE_SEARCH_KEY, true)
                }
                if (antSearch) {
                    args.putBoolean(ANT_SEARCH_KEY, true)
                }
                fragment.arguments = args
                fragment.retainInstance = true
                manager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment, TAG)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_ant_plus_search
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        setupNoBluetoothView(view)
        setupSearchingView(view)
        setupNothingFoundView(view)
        setupDevicesListView(view)
    }

    private fun setupNoBluetoothView(parentView: View) {
        stateNoBluetoothView = parentView.findViewById(R.id.state_no_bluetooth)
        val openSettingButton = stateNoBluetoothView?.findViewById<DialogButton>(R.id.dismiss_button)
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
            setCurrentState(SearchStates.SEARCHING)
            startSearch()
        }
        AndroidUiHelper.updateVisibility(searchAgain, true)
    }

    private fun setupDevicesListView(parentView: View) {
        stateDevicesListView = parentView.findViewById(R.id.state_found_devices_list)
        foundDevicesCountView = stateDevicesListView?.findViewById(R.id.found_devices_count)
        val recyclerView: RecyclerView? =
            stateDevicesListView?.findViewById(R.id.found_devices_list)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        adapter = FoundDevicesAdapter(app, nightMode, this)
        recyclerView?.adapter = adapter
    }

    private fun bindFoundDevices() {
        val devices = plugin.unpairedDevices
        if (devices.isEmpty()) {
            setCurrentState(SearchStates.NOTHING_FOUND)
        } else {
            setCurrentState(SearchStates.DEVICES_LIST)
            val formatString = activity?.resources?.getString(R.string.bluetooth_found_title)
            formatString?.let {
                foundDevicesCountView?.text =
                    String.format(formatString, devices.size)
            }
            adapter.setItems(devices as List<Any>)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val newView = super.onCreateView(inflater, container, savedInstanceState)
        val args = savedInstanceState ?: arguments
        if (args != null) {
            bleSearch = args.getBoolean(BLE_SEARCH_KEY, false)
            antSearch = args.getBoolean(ANT_SEARCH_KEY, false)
        }
        updateCurrentStateView()
        return newView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentState = if (!AndroidUtils.isBluetoothEnabled(requireActivity())) {
            SearchStates.NO_BLUETOOTH
        } else {
            SearchStates.SEARCHING
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentState == SearchStates.NO_BLUETOOTH && AndroidUtils.isBluetoothEnabled(
                requireActivity())) {
            setCurrentState(SearchStates.SEARCHING)
        }
        if (currentState == SearchStates.SEARCHING) {
            startSearch()
        } else if (currentState == SearchStates.DEVICES_LIST) {
            bindFoundDevices()
        }
    }

    private fun startSearch() {
        plugin.setScanDevicesListener(this)
        if (bleSearch) {
            plugin.searchBLEDevices()
        } else if (antSearch) {
            plugin.searchAntDevices();
        }
    }

    override fun onPause() {
        super.onPause()
        plugin.setScanDevicesListener(null)
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

    override fun onDestroy() {
        super.onDestroy()
        plugin.dropUnpairedDevices()
    }

    private fun updateCurrentStateView() {
        AndroidUiHelper.updateVisibility(
            stateNoBluetoothView,
            currentState == SearchStates.NO_BLUETOOTH
        )
        AndroidUiHelper.updateVisibility(stateSearchingView, currentState == SearchStates.SEARCHING)
        AndroidUiHelper.updateVisibility(
            stateNothingFoundView,
            currentState == SearchStates.NOTHING_FOUND
        )
        AndroidUiHelper.updateVisibility(
            stateDevicesListView,
            currentState == SearchStates.DEVICES_LIST
        )
    }

    override fun onScanFinished(foundDevices: List<AbstractDevice<out AbstractSensor>>) {
        bindFoundDevices()
    }

    internal enum class SearchStates {
        NO_BLUETOOTH, SEARCHING, NOTHING_FOUND, DEVICES_LIST
    }

    override fun onDeviceClicked(device: AbstractDevice<out AbstractSensor>) {
        ExternalDeviceDetailsFragment.showInstance(requireActivity().supportFragmentManager, device)
    }
}