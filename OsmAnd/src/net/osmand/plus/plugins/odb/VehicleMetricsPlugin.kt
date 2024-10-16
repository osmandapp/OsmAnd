package net.osmand.plus.plugins.odb

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_VEHICLE_METRICS_ID
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.odb.dialogs.OBDDevicesListFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.settings.fragments.SettingsScreenType
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BLEUtils
import net.osmand.plus.utils.BLEUtils.getAliasName
import net.osmand.plus.views.mapwidgets.MapWidgetInfo
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem
import net.osmand.shared.data.BTDeviceInfo
import net.osmand.shared.data.KLatLon
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataFieldType.*
import net.osmand.shared.obd.OBDDispatcher
import net.osmand.util.Algorithms
import okio.IOException
import okio.sink
import okio.source
import java.util.UUID

class VehicleMetricsPlugin(app: OsmandApplication) : OsmandPlugin(app),
	OBDDispatcher.OBDReadStatusListener {
	private val settings: OsmandSettings = app.settings
	val USED_OBD_DEVICES = registerStringPreference(
		"used_obd_devices",
		"").makeGlobal().cache();
	val LAST_CONNECTED_OBD_DEVICE = registerStringPreference(
		"last_connected_obd_device",
		"").makeGlobal().cache();

	private val uuid =
		UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Standard UUID for SPP
	private var connectedDeviceInfo: BTDeviceInfo? = null
	var socket: BluetoothSocket? = null
	private var scanDevicesListener: ScanOBDDevicesListener? = null
	private var connectionStateListener: ConnectionStateListener? = null

	enum class OBDConnectionState {
		CONNECTED, CONNECTING, DISCONNECTED
	}


	interface ScanOBDDevicesListener {
		fun onDeviceFound(foundDevice: BTDeviceInfo)
	}

	interface ConnectionStateListener {
		fun onStateChanged(state: OBDConnectionState)
	}

	init {
		OBDDispatcher.setReadStatusListener(this)
	}

	override fun createWidgets(
		mapActivity: MapActivity, widgetsInfos: MutableList<MapWidgetInfo?>,
		appMode: ApplicationMode) {
		val creator = WidgetInfoCreator(app, appMode)
		val speedWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_SPEED)
		widgetsInfos.add(creator.createWidgetInfo(speedWidget))
		val rpmWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_RPM)
		widgetsInfos.add(creator.createWidgetInfo(rpmWidget))
		val airIntakeTempWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_AIR_INTAKE_TEMP)
		widgetsInfos.add(creator.createWidgetInfo(airIntakeTempWidget))
		val ambientAirTempWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_AMBIENT_AIR_TEMP)
		widgetsInfos.add(creator.createWidgetInfo(ambientAirTempWidget))
		val batteryVoltageWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_BATTERY_VOLTAGE)
		widgetsInfos.add(creator.createWidgetInfo(batteryVoltageWidget))
		val fuelLevelWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_LEVEL_PERCENT)
		widgetsInfos.add(creator.createWidgetInfo(fuelLevelWidget))
		val fuelLeftDistanceWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_LEFT_DISTANCE)
		widgetsInfos.add(creator.createWidgetInfo(fuelLeftDistanceWidget))
		val fuelConsumptionRateLiterHourWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_CONSUMPTION_RATE_LITER_HOUR)
		widgetsInfos.add(creator.createWidgetInfo(fuelConsumptionRateLiterHourWidget))
		val fuelConsumptionRatePercentHourWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_CONSUMPTION_RATE_PERCENT_HOUR)
		widgetsInfos.add(creator.createWidgetInfo(fuelConsumptionRatePercentHourWidget))
		val fuelConsumptionRateSensorWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_CONSUMPTION_RATE_SENSOR)
		widgetsInfos.add(creator.createWidgetInfo(fuelConsumptionRateSensorWidget))
		val fuelTypeWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_TYPE)
		widgetsInfos.add(creator.createWidgetInfo(fuelTypeWidget))
		val engineCoolantTempWidget: MapWidget =
			createMapWidgetForParams(mapActivity, WidgetType.OBD_ENGINE_COOLANT_TEMP)
		widgetsInfos.add(creator.createWidgetInfo(engineCoolantTempWidget))
	}

	override fun createMapWidgetForParams(
		mapActivity: MapActivity,
		widgetType: WidgetType,
		customId: String?,
		widgetsPanel: WidgetsPanel?): OBDTextWidget? {
		return when (widgetType) {
			WidgetType.OBD_SPEED -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.SPEED)

			WidgetType.OBD_RPM -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.RPM)

			WidgetType.OBD_AIR_INTAKE_TEMP -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.AIR_INTAKE_TEMP)

			WidgetType.OBD_AMBIENT_AIR_TEMP -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.AMBIENT_AIR_TEMP)

			WidgetType.OBD_BATTERY_VOLTAGE -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.BATTERY_VOLTAGE)

			WidgetType.OBD_FUEL_LEVEL_PERCENT -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_LEFT_PERCENT)

			WidgetType.OBD_FUEL_LEFT_DISTANCE -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_LEFT_DISTANCE)

			WidgetType.OBD_FUEL_CONSUMPTION_RATE_PERCENT_HOUR -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_CONSUMPTION_RATE_PERCENT_HOUR)

			WidgetType.OBD_FUEL_CONSUMPTION_RATE_SENSOR -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_CONSUMPTION_RATE_SENSOR)

			WidgetType.OBD_FUEL_CONSUMPTION_RATE_LITER_HOUR -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_CONSUMPTION_RATE_LITER_HOUR)

			WidgetType.OBD_FUEL_TYPE -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_TYPE)

			WidgetType.OBD_ENGINE_COOLANT_TEMP -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.COOLANT_TEMP)

			else -> null
		}
	}

	override fun getId(): String {
		return OsmAndCustomizationConstants.PLUGIN_VEHICLE_METRICS
	}

	override fun getName(): String {
		return app.getString(R.string.obd_plugin_name)
	}

	override fun getDescription(linksEnabled: Boolean): CharSequence {
		return app.getString(R.string.obd_plugin_description)
	}

	override fun getLogoResourceId(): Int {
		return R.drawable.ic_action_car_info
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
			helper.addItem(ContextMenuItem(DRAWER_VEHICLE_METRICS_ID)
				.setTitleId(R.string.obd_plugin_name, mapActivity)
				.setIcon(R.drawable.ic_action_sensor)
				.setListener { _: OnDataChangeUiAdapter?, _: View?, _: ContextMenuItem?, _: Boolean ->
					app.logEvent("obdOpen")
					OBDDevicesListFragment.showInstance(mapActivity.supportFragmentManager)
					true
				})
		}
	}

	@SuppressLint("MissingPermission")
	fun getPairedOBDDevicesList(activity: Activity): List<BTDeviceInfo> {
		var deviceList = listOf<BTDeviceInfo>()
		if (BLEUtils.isBLEEnabled(activity) && AndroidUtils.hasBLEPermission(activity)) {
			val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
			bluetoothAdapter?.let { adapter ->
				adapter.cancelDiscovery()
				val pairedDevices = adapter.bondedDevices.toList()
				deviceList = pairedDevices.filter { device ->
					device.uuids?.any { parcelUuid -> parcelUuid.uuid == uuid } == true
				}.map {
					if (it != null) BTDeviceInfo(
						it.getAliasName(activity),
						it.address) else BTDeviceInfo.UNKNOWN_DEVICE
				}
			}

		} else {
			Toast.makeText(activity, "Please, grant BLUETOOTH_SCAN permission", Toast.LENGTH_LONG)
				.show()
		}
		return deviceList
	}

	fun disconnect() {
		socket?.apply {
			if (isConnected) {
				OBDDispatcher.stopReading()
				close()
			}
		}
		socket = null
		connectedDeviceInfo = null
		connectionStateListener?.onStateChanged(OBDConnectionState.DISCONNECTED)
	}

	@SuppressLint("MissingPermission")
	fun connectToObd(activity: Activity, deviceInfo: BTDeviceInfo): Boolean {
		if (connectedDeviceInfo == null) {
			if (BLEUtils.isBLEEnabled(activity)) {
				if (AndroidUtils.hasBLEPermission(activity)) {
					connectionStateListener?.onStateChanged(OBDConnectionState.CONNECTING)
					if (socket != null && socket?.isConnected == true) {
						socket?.close()
						socket = null
					}
					val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
					bluetoothAdapter?.let { adapter ->
						LOG.debug("adapter.isDiscovering ${adapter.isDiscovering}")
						adapter.cancelDiscovery()
						val pairedDevices = adapter.bondedDevices.toList()
						val obdDevice: BluetoothDevice? =
							pairedDevices.find { it.getAliasName(activity) == deviceInfo.name && it.address == deviceInfo.address }
						if (obdDevice != null) {
							connectToDevice(activity, obdDevice)
						} else {
							connectionStateListener?.onStateChanged(OBDConnectionState.DISCONNECTED)
						}
					}
				} else {
					AndroidUtils.requestBLEPermissions(activity)
				}
			}
		} else {
			socket?.close()
			connectedDeviceInfo = null
			connectToObd(activity, deviceInfo)
		}
		return socket?.isConnected == true
	}

	@SuppressLint("MissingPermission")
	private fun connectToDevice(activity: Activity, connectedDevice: BluetoothDevice) {
		try {
			socket = connectedDevice.createRfcommSocketToServiceRecord(uuid)
			socket?.apply {
				connect()
				if (isConnected) {
					onDeviceConnected(
						BTDeviceInfo(
							connectedDevice.getAliasName(activity),
							connectedDevice.address))
					val input = inputStream.source()
					val output = outputStream.sink()
					OBDDispatcher.setReadWriteStreams(input, output)
					app.runInUIThread {
						Toast.makeText(
							activity,
							"Connected to ${connectedDevice.getAliasName(activity)}",
							Toast.LENGTH_LONG).show()
					}
				}
			}
		} catch (error: IOException) {
			LOG.error("Can't connect to device. $error")
			app.runInUIThread {
				Toast.makeText(
					activity,
					"Can\'t connect to ${connectedDevice.getAliasName(activity)}",
					Toast.LENGTH_LONG).show()
			}
		}
	}

	@SuppressLint("MissingPermission")
	private fun onDeviceConnected(btDeviceInfo: BTDeviceInfo) {
		connectedDeviceInfo = btDeviceInfo
		connectedDeviceInfo?.let {
			OBDDataComputer.fuelTank = 52f //todo implement setting correct fuel tank
			saveDeviceToUsedOBDDevicesList(it)
			setLastConnectedDevice(it)
		}
		connectionStateListener?.onStateChanged(OBDConnectionState.CONNECTED)
	}

	override fun getSettingsScreenType(): SettingsScreenType {
		return SettingsScreenType.VEHICLE_METRICS_SETTINGS
	}


	fun getConnectedDeviceName(): String? {
		return connectedDeviceInfo?.name
	}

	fun getConnectedDeviceInfo(): BTDeviceInfo? {
		return connectedDeviceInfo
	}

	fun isConnected(): Boolean {
		return socket?.isConnected == true
	}

	companion object {
		private val LOG = PlatformUtil.getLog(VehicleMetricsPlugin::class.java)
	}

	override fun onIOError() {
//		socket?.apply {
//			if(!isConnected) {
//				connectedDevice?.let { device ->
//					connectToDevice()
//				}
//			}
//		}
	}

	override fun onInitConnectionFailed() {
		disconnect()
	}

	override fun updateLocation(location: Location) {
		OBDDataComputer.registerLocation(
			OBDDataComputer.OBDLocation(
				location.time,
				KLatLon(location.latitude, location.longitude)))
	}

	fun setScanDevicesListener(listener: ScanOBDDevicesListener?) {
		scanDevicesListener = listener
	}

	fun setConnectionStateListener(listener: ConnectionStateListener?) {
		connectionStateListener = listener
	}

	fun getUsedOBDDevicesList(): List<BTDeviceInfo> {
		val savedDevicesList = USED_OBD_DEVICES.get()
		val gson = GsonBuilder().create()
		val t = object : TypeToken<List<BTDeviceInfo>?>() {}.type
		val arr: List<BTDeviceInfo>? = gson.fromJson(savedDevicesList, t)
		return arr?.toList() ?: emptyList()
	}

	private fun saveDeviceToUsedOBDDevicesList(deviceInfo: BTDeviceInfo) {
		val currentList = getUsedOBDDevicesList().toMutableList()
		val savedDevice = currentList.find { it.address == deviceInfo.address }
		if (savedDevice == null) {
			currentList.add(deviceInfo)
			writeUsedOBDDevicesList(currentList)
		} else {
			if (savedDevice.name != deviceInfo.name) {
				currentList.remove(savedDevice)
				currentList.add(deviceInfo)
				writeUsedOBDDevicesList(currentList)
			}
		}
	}

	fun setDeviceName(address: String, newName: String) {
		removeDeviceToUsedOBDDevicesList(address)
		saveDeviceToUsedOBDDevicesList(BTDeviceInfo(newName, address))
	}

	fun removeDeviceToUsedOBDDevicesList(address: String) {
		val device = getUsedOBDDevicesList().find { info -> info.address == address }
		if (device != null) {
			removeDeviceToUsedOBDDevicesList(device)
		}
	}

	private fun removeDeviceToUsedOBDDevicesList(deviceInfo: BTDeviceInfo) {
		val currentList = getUsedOBDDevicesList().toMutableList()
		currentList.remove(deviceInfo)
		writeUsedOBDDevicesList(currentList)
	}

	private fun writeUsedOBDDevicesList(list: List<BTDeviceInfo>) {
		val gson = GsonBuilder().create()
		USED_OBD_DEVICES.set(gson.toJson(list))
	}

	private fun setLastConnectedDevice(deviceInfo: BTDeviceInfo?) {
		val gson = GsonBuilder().create()
		LAST_CONNECTED_OBD_DEVICE.set(if (deviceInfo != null) gson.toJson(deviceInfo) else "")
	}

	private fun getLastConnectedDevice(): BTDeviceInfo? {
		val savedDevice = LAST_CONNECTED_OBD_DEVICE.get()
		return if (Algorithms.isEmpty(savedDevice)) {
			null
		} else {
			val gson = GsonBuilder().create()
			gson.fromJson(savedDevice, BTDeviceInfo::class.java)
		}
	}

	private val bluetoothReceiver = object : BroadcastReceiver() {
		@SuppressLint("MissingPermission")
		override fun onReceive(context: Context, intent: Intent) {
			if (AndroidUtils.hasBLEPermission(context)) {
				when (intent.action) {
					BluetoothDevice.ACTION_FOUND -> {
						val device: BluetoothDevice? =
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
								intent.getParcelableExtra(
									BluetoothDevice.EXTRA_DEVICE,
									BluetoothDevice::class.java)
							} else {
								@Suppress("DEPRECATION")
								intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
							}
						device?.let {
							if (it.bondState != BluetoothDevice.BOND_BONDED) {
								scanDevicesListener?.onDeviceFound(
									BTDeviceInfo(
										it.getAliasName(
											context), it.address))
							}
						}
					}
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun searchUnboundDevices(activity: Activity) {
		if (BLEUtils.isBLEEnabled(activity) && AndroidUtils.hasBLEPermission(activity)) {
			val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
			bluetoothAdapter?.startDiscovery()
		}
	}

	override fun mapActivityPause(activity: MapActivity) {
		super.mapActivityPause(activity)
		activity.unregisterReceiver(bluetoothReceiver)
	}

	override fun mapActivityResume(activity: MapActivity) {
		super.mapActivityResume(activity)
		val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
		activity.registerReceiver(bluetoothReceiver, filter)
	}

	override fun mapActivityCreate(activity: MapActivity) {
		super.mapActivityCreate(activity)
		val lastConnectedDevice = getLastConnectedDevice()
		if (connectedDeviceInfo == null && lastConnectedDevice != null) {
			connectToObd(activity, lastConnectedDevice)
		}
	}
}