package net.osmand.plus.plugins.odb

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.drawable.Drawable
import android.util.ArraySet
import android.view.View
//import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.externalsensors.dialogs.ExternalDevicesListFragment
import net.osmand.plus.plugins.odb.dialogs.OBDMainFragment
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BLEUtils
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem
import net.osmand.shared.obd.OBDCommand
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OBDPlugin(app: OsmandApplication) : OsmandPlugin(app) {
	private val settings: OsmandSettings = app.settings

	//	private ScanDevicesListener scanDevicesListener;
	interface OBDResponseListener {
		fun onCommandResponse(command: OBDCommand, rawResponse: String, result: String)
	}
	
	private val responseListeners = ArraySet<OBDResponseListener>()
	val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	val deviceName = "OBDII"  // This is the name of the OBD-II device
	val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Standard UUID for SPP
	var connectedDevice: BluetoothDevice? = null

	override fun getId(): String {
		return OsmAndCustomizationConstants.PLUGIN_OBD
	}

	override fun getName(): String {
		return app.getString(R.string.obd_plugin_name)
	}

	override fun getDescription(linksEnabled: Boolean): CharSequence {
		return app.getString(R.string.obd_plugin_description)
	}

	override fun getLogoResourceId(): Int {
		return R.drawable.ic_action_external_sensor
	}

	override fun getAssetResourceImage(): Drawable? {
		return app.uiUtilities.getIcon(R.drawable.osmand_development)
	}

//	fun attachAdditionalInfoToRecordedTrack(location: Location, json: JSONObject) {
//
//	}

	override fun init(app: OsmandApplication, activity: Activity?): Boolean {
		return true
	}

	override fun disable(app: OsmandApplication) {
		super.disable(app)
	}

	fun registerBooleanPref(prefId: String, defValue: Boolean): CommonPreference<Boolean> {
		return registerBooleanPreference(prefId, defValue).makeGlobal().makeShared()
	}

	fun registerIntPref(prefId: String, defValue: Int): CommonPreference<Int> {
		return registerIntPreference(prefId, defValue).makeGlobal().makeShared()
	}

	fun registerStringPref(prefId: String, defValue: String?): CommonPreference<String> {
		return registerStringPreference(prefId, defValue).makeGlobal().makeShared()
	}

	public override fun registerOptionsMenuItems(
		mapActivity: MapActivity,
		helper: ContextMenuAdapter) {
		if (isActive) {
			helper.addItem(ContextMenuItem(OsmAndCustomizationConstants.DRAWER_ANT_PLUS_ID)
				.setTitleId(R.string.obd_plugin_name, mapActivity)
				.setIcon(R.drawable.ic_action_sensor)
				.setListener { _: OnDataChangeUiAdapter?, _: View?, _: ContextMenuItem?, _: Boolean ->
					app.logEvent("obdOpen")
					OBDMainFragment.showInstance(mapActivity.supportFragmentManager)

//					if(connectedDevice != null) {
//						for (command in OBDCommand.entries) {
//							sendCommand(command)
//						}
//					} else {
//						connectToObd(mapActivity)
//					}
					true
				})
		}
	}

	@SuppressLint("MissingPermission")
	fun connectToObd(activity: Activity, name: String) {
		if (connectedDevice == null) {
			if (BLEUtils.isBLEEnabled(activity)) {
				if (AndroidUtils.hasBLEPermission(activity)) {
					val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
					bluetoothAdapter?.let { adapter ->
						val pairedDevices = adapter.bondedDevices.toList()
						pairedDevices.indices
						val obdDevice: BluetoothDevice? =
							pairedDevices.find { it.name == name }
						connectedDevice = obdDevice
					}
				} else {
					AndroidUtils.requestBLEPermissions(activity)
				}
			}
		}
	}




	fun getRPM() {
		sendCommand(OBDCommand.OBD_RPM_COMMAND)
	}

	@SuppressLint("MissingPermission")
	fun sendCommand(command: OBDCommand) {
		connectedDevice?.let {
			// Establish a Bluetooth socket connection
			val socket: BluetoothSocket = it.createRfcommSocketToServiceRecord(uuid)
			socket.connect()
			val inputStream: InputStream = socket.inputStream
			val outputStream: OutputStream = socket.outputStream
			// Send commands to the OBD-II adapter
			sendObdCommand(
				outputStream,
				inputStream,
				command)

			socket.close()
		}
	}

	private fun sendObdCommand(outputStream: OutputStream, inputStream: InputStream, command: OBDCommand) {
		val fullCommand = "01${command.command}\r"
		outputStream.write(fullCommand.toByteArray())
		outputStream.flush()

		// Read the response from the OBD-II adapter
		val buffer = ByteArray(1024)
		val bytesRead = inputStream.read(buffer)

		// Convert the response to a string
		val response = String(buffer, 0, bytesRead)
		val result = command.responseParser(response)
		onCommandResponse(command, response, result)
	}


	@SuppressLint("MissingPermission")
	fun getConnectedDeviceName(): String? {
		return connectedDevice?.name
	}
//	010C
//	41 0C 15 CE
//	41 0C 16 40
//
//	>

//	7E906410098188001
//	7E8064100BE3FE813
//

	fun addResponseListener(responseListener: OBDResponseListener) {
		responseListeners.add(responseListener)
	}

	fun removeResponseListener(responseListener: OBDResponseListener) {
		responseListeners.remove(responseListener)
	}

	private fun onCommandResponse(command: OBDCommand, rawResponse: String, result: String) {
		for (listener in responseListeners) {
			listener.onCommandResponse(command, rawResponse, result)
		}
		LOG.debug("OBD-II Response: $rawResponse")
		LOG.debug("OBD-II command ${command.name} result : $result")
	}

	companion object {
		private val LOG = PlatformUtil.getLog(OBDPlugin::class.java)
	}
}