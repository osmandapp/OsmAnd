package net.osmand.plus.plugins.odb

//import net.osmand.Location
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.drawable.Drawable
import android.view.View
import net.osmand.PlatformUtil
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.odb.dialogs.OBDMainFragment
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BLEUtils
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDispatcher
import net.osmand.shared.obd.OBDResponseListener
import net.osmand.util.Algorithms
import okio.IOException
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OBDPlugin(app: OsmandApplication) : OsmandPlugin(app) {
	private val settings: OsmandSettings = app.settings

	val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	private val uuid =
		UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Standard UUID for SPP
	private var connectedDevice: BluetoothDevice? = null
	var socket: BluetoothSocket? = null

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

	override fun init(app: OsmandApplication, activity: Activity?): Boolean {
		for (command in OBDCommand.entries) {
			OBDDispatcher.addCommand(command)
		}
		return true
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
					true
				})
		}
	}

	@SuppressLint("MissingPermission")
	fun connectToObd(activity: Activity, name: String): Boolean {
		if (connectedDevice == null) {
			if (BLEUtils.isBLEEnabled(activity)) {
				if (AndroidUtils.hasBLEPermission(activity)) {
					if (socket != null && socket?.isConnected == true) {
						socket?.close()
						socket = null
					}
					val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
					bluetoothAdapter?.let { adapter ->
						LOG.debug("adapter.isDiscovering ${adapter.isDiscovering}")
						adapter.cancelDiscovery()
						val pairedDevices = adapter.bondedDevices.toList()
						pairedDevices.indices
						pairedDevices.forEach {
							LOG.debug(it.name)
						}
						val obdDevice: BluetoothDevice? =
							pairedDevices.find { it.name == name }
						connectedDevice = obdDevice
						try {
							socket = connectedDevice?.createRfcommSocketToServiceRecord(uuid)
							socket?.apply {
								connect()
								if (isConnected) {
									val input = inputStream.source()
									val output = outputStream.sink()
									OBDDispatcher.setReadWriteStreams(input, output)
								}
							}
						} catch (error: Throwable) {
							LOG.error("Can't connect to device. $error")
						}
					}
				} else {
					AndroidUtils.requestBLEPermissions(activity)
				}
			}
		} else {
			socket?.close()
			connectedDevice = null
			connectToObd(activity, name)
		}
		return socket != null && socket?.isConnected == true
	}

	@SuppressLint("MissingPermission")
	fun sendCommand(command: OBDCommand) {
		if (isCommandListening(command)) {
			OBDDispatcher.removeCommand(command)
		} else {
			OBDDispatcher.addCommand(command)
		}
	}

	@SuppressLint("MissingPermission")
	fun getConnectedDeviceName(): String? {
		return connectedDevice?.name
	}

	fun addResponseListener(responseListener: OBDResponseListener) {
		OBDDispatcher.addResponseListener(responseListener)
	}

	fun removeResponseListener(responseListener: OBDResponseListener) {
		OBDDispatcher.removeResponseListener(responseListener)
	}

	private fun onCommandResponse(command: OBDCommand, rawResponse: String, result: String) {
		LOG.debug("OBD-II Response: $rawResponse")
		LOG.debug("OBD-II command ${command.name} result : $result")
	}

	fun isCommandListening(command: OBDCommand): Boolean {
		return OBDDispatcher.getCommandQueue().contains(command)
	}

	companion object {
		private val LOG = PlatformUtil.getLog(OBDPlugin::class.java)
	}
}