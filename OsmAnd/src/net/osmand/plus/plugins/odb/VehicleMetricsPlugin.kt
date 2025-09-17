package net.osmand.plus.plugins.odb

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.MainThread
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.StateChangedListener
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_VEHICLE_METRICS_ID
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin
import net.osmand.plus.plugins.externalsensors.DevicesHelper
import net.osmand.plus.plugins.externalsensors.VehicleMetricsBLEDeviceHelper
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEOBDDevice
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData
import net.osmand.plus.plugins.odb.dialogs.OBDDevicesListFragment
import net.osmand.plus.plugins.weather.units.TemperatureUnit
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.settings.backend.preferences.CommonPreferenceProvider
import net.osmand.plus.settings.backend.preferences.ListStringPreference
import net.osmand.plus.settings.enums.VolumeUnit
import net.osmand.plus.settings.fragments.SettingsScreenType
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BLEUtils
import net.osmand.plus.utils.BLEUtils.getAliasName
import net.osmand.plus.utils.BLEUtils.getAliasNameOrNull
import net.osmand.plus.utils.OsmAndFormatter
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
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDConnector
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDispatcher
import net.osmand.shared.obd.OBDDispatcher.OBDReadStatusListener
import net.osmand.shared.obd.OBDSimulationSource
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.util.Algorithms
import okio.IOException
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import org.json.JSONObject
import java.util.UUID

class VehicleMetricsPlugin(app: OsmandApplication) : OsmandPlugin(app), OBDReadStatusListener {

	private val DEVICES_SEARCH_TIMEOUT: Int = 20000

	private var mapActivity: MapActivity? = null

	private val handler = Handler(Looper.getMainLooper())
	private val RECONNECT_DELAY = 5000L
	private val RECONNECT_ATTEMPTS_COUNT = 3
	private val RECALCULATE_RECONNECT_ATTEMPTS_COUNT = 1
	private var currentReconnectAttempt = 0

	private var connectionState = OBDConnectionState.DISCONNECTED
	val OBD_DEVICES_SETTINGS_PREF_ID: String = "obd_devices_settings"

	val USED_OBD_DEVICES =
		registerStringPreference("used_obd_devices", "").makeGlobal().cache();
	val LAST_CONNECTED_OBD_DEVICE =
		registerStringPreference("last_connected_obd_device", "").makeGlobal().cache()

	val TRIP_RECORDING_VEHICLE_METRICS: ListStringPreference =
		registerListStringPreference("trip_recording_vehicle_metrics", null, ";").makeProfile()
			.makeShared() as ListStringPreference

	private val uuid =
		UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Standard UUID for SPP
	private var connectedDeviceInfo: BTDeviceInfo? = null
	private var scanDevicesListener: ScanOBDDevicesListener? = null
	private var scanBLEDevicesListener: ScanBLEDevicesListener? = null

	private var connectionStateListener: ConnectionStateListener? = null
	private var pairingDevice: BTDeviceInfo? = null

	private var obdDispatcher: OBDDispatcher? = null

	enum class OBDConnectionState {
		CONNECTED, CONNECTING, DISCONNECTED
	}

	interface ScanOBDDevicesListener {
		fun onDeviceFound(foundDevice: BTDeviceInfo)
		fun onDevicePaired(pairedDevice: BTDeviceInfo)
		fun onDevicePairingFailed()
	}

	interface ConnectionStateListener {
		fun onStateChanged(state: OBDConnectionState, deviceInfo: BTDeviceInfo)
	}

	private val deviceSettingsPreferenceProvider: CommonPreferenceProvider<String> =
		object : CommonPreferenceProvider<String> {
			override fun getPreference(): CommonPreference<String> {
				return registerStringPref(OBD_DEVICES_SETTINGS_PREF_ID, "")
			}
		}

	fun registerStringPref(prefId: String, defValue: String?): CommonPreference<String> {
		return registerStringPreference(prefId, defValue).makeGlobal().makeShared()
	}

	private val devicesHelper: DevicesHelper =
		VehicleMetricsBLEDeviceHelper(this, app, deviceSettingsPreferenceProvider)

	private fun BluetoothSocket?.safeClose() {
		try {
			this?.close()
		} catch (e: IOException) {
			LOG.error(e.message, e)
		}
	}

	override fun createWidgets(
		mapActivity: MapActivity, widgetsInfos: MutableList<MapWidgetInfo?>,
		appMode: ApplicationMode) {
		val creator = WidgetInfoCreator(app, appMode)
		for (widgetType in WidgetType.getObdTypes()) {
			val obdWidget: MapWidget =
				createMapWidgetForParams(mapActivity, widgetType)
			widgetsInfos.add(creator.createWidgetInfo(obdWidget))
		}
	}

	override fun createMapWidgetForParams(
		mapActivity: MapActivity,
		widgetType: WidgetType,
		customId: String?,
		widgetsPanel: WidgetsPanel?): OBDTextWidget? {

		return when (widgetType) {
			WidgetType.OBD_SPEED -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_SPEED,
				OBDDataComputer.OBDTypeWidget.SPEED,
				customId,
				widgetsPanel)

			WidgetType.OBD_RPM -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_RPM,
				OBDDataComputer.OBDTypeWidget.RPM,
				customId,
				widgetsPanel)

			WidgetType.OBD_ENGINE_RUNTIME -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_ENGINE_RUNTIME,
				OBDDataComputer.OBDTypeWidget.ENGINE_RUNTIME,
				customId,
				widgetsPanel)

			WidgetType.OBD_FUEL_PRESSURE -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_FUEL_PRESSURE,
				OBDDataComputer.OBDTypeWidget.FUEL_PRESSURE,
				customId,
				widgetsPanel)

			WidgetType.OBD_AIR_INTAKE_TEMP -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_AIR_INTAKE_TEMP,
				OBDDataComputer.OBDTypeWidget.TEMPERATURE_INTAKE,
				customId,
				widgetsPanel)

			WidgetType.ENGINE_OIL_TEMPERATURE -> return OBDTextWidget(
				mapActivity,
				WidgetType.ENGINE_OIL_TEMPERATURE,
				OBDDataComputer.OBDTypeWidget.ENGINE_OIL_TEMPERATURE,
				customId,
				widgetsPanel)

			WidgetType.OBD_AMBIENT_AIR_TEMP -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_AMBIENT_AIR_TEMP,
				OBDDataComputer.OBDTypeWidget.TEMPERATURE_AMBIENT,
				customId,
				widgetsPanel)

			// TODO: OBD Battery widget
//			WidgetType.OBD_ALT_BATTERY_VOLTAGE -> return OBDTextWidget(
//				mapActivity,
//				WidgetType.OBD_ALT_BATTERY_VOLTAGE,
//				OBDDataComputer.OBDTypeWidget.ADAPTER_BATTERY_VOLTAGE,
//				customId,
//				widgetsPanel)

			WidgetType.OBD_BATTERY_VOLTAGE -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_BATTERY_VOLTAGE,
				OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE,
				customId,
				widgetsPanel)

			WidgetType.OBD_CALCULATED_ENGINE_LOAD -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_CALCULATED_ENGINE_LOAD,
				OBDDataComputer.OBDTypeWidget.CALCULATED_ENGINE_LOAD,
				customId,
				widgetsPanel)

			WidgetType.OBD_THROTTLE_POSITION -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_THROTTLE_POSITION,
				OBDDataComputer.OBDTypeWidget.THROTTLE_POSITION,
				customId,
				widgetsPanel)

			WidgetType.OBD_FUEL_CONSUMPTION -> return OBDFuelConsumptionWidget(
				mapActivity,
				WidgetType.OBD_FUEL_CONSUMPTION,
				OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR,
				customId,
				widgetsPanel)

			WidgetType.OBD_REMAINING_FUEL -> return OBDRemainingFuelWidget(
				mapActivity,
				WidgetType.OBD_REMAINING_FUEL,
				OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT,
				customId,
				widgetsPanel)

//			WidgetType.OBD_FUEL_TYPE -> return OBDTextWidget(
//				mapActivity,
//				WidgetType.OBD_FUEL_TYPE,
//				OBDDataComputer.OBDTypeWidget.FUEL_TYPE,
//				customId,
//				widgetsPanel)

			WidgetType.OBD_ENGINE_COOLANT_TEMP -> return OBDTextWidget(
				mapActivity,
				WidgetType.OBD_ENGINE_COOLANT_TEMP,
				OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT,
				customId,
				widgetsPanel)

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
		settings.SIMULATE_OBD_DATA.addListener(simulateOBDListener)
		devicesHelper.setActivity(activity)
		return true
	}

	override fun disable(app: OsmandApplication) {
		super.disable(app)
		devicesHelper.disconnectDevices()
		devicesHelper.deinitBLE()
	}

	private val simulateOBDListener = StateChangedListener<Boolean> { enabled ->
		if (!enabled) {
			disconnect(true)
		}
	}

	public override fun registerOptionsMenuItems(
		mapActivity: MapActivity,
		helper: ContextMenuAdapter) {
		if (isActive) {
			helper.addItem(
				ContextMenuItem(DRAWER_VEHICLE_METRICS_ID)
					.setTitleId(R.string.obd_plugin_name, mapActivity)
					.setIcon(R.drawable.ic_action_car_info)
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
			bluetoothAdapter?.apply {
				val pairedDevices = bondedDevices.toList()
				deviceList = pairedDevices.filter { device ->
					device.uuids?.any { parcelUuid -> parcelUuid.uuid == uuid } == true
				}.map {
					if (it != null) BTDeviceInfo(
						it.getAliasName(activity),
						it.address) else BTDeviceInfo.UNKNOWN_DEVICE
				}
			}

		} else {
			AndroidUtils.requestBLEPermissions(activity)
		}
		return deviceList
	}

	@MainThread
	fun disconnect(forgetCurrentDeviceConnected: Boolean) {
		LOG.info("VMPlugin disconnect")
		obdDispatcher?.stopReading()
		val lastConnectedDeviceInfo = connectedDeviceInfo
		connectedDeviceInfo = null
		if (forgetCurrentDeviceConnected) {
			setLastConnectedDevice(null)
		}
		onDisconnected(lastConnectedDeviceInfo)
	}

	@SuppressLint("MissingPermission")
	fun isPaired(activity: Activity, deviceInfo: BTDeviceInfo): Boolean =
		getRemoteDevice(activity, deviceInfo.address)?.bondState == BluetoothDevice.BOND_BONDED

	@SuppressLint("MissingPermission")
	private fun getRemoteDevice(activity: Activity, address: String): BluetoothDevice? {
		if (BLEUtils.isBLEEnabled(activity) && AndroidUtils.hasBLEPermission(activity)) {
			val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
			return bluetoothAdapter?.getRemoteDevice(address)
		}
		return null
	}

	@SuppressLint("MissingPermission")
	fun pairDevice(activity: Activity, device: BTDeviceInfo) {
		if (BLEUtils.isBLEEnabled(activity) && AndroidUtils.hasBLEPermission(activity)) {
			val btDevice = getRemoteDevice(activity, device.address)
			btDevice?.apply {
				val isBondingStarted = createBond()
				if (!isBondingStarted) {
					app.showShortToastMessage(R.string.bt_start_pair_failed)
				} else {
					pairingDevice = BTDeviceInfo(getAliasName(activity), device.address)
				}
			}
		}
	}

	@MainThread
	fun connectToObd(activity: Activity, deviceInfo: BTDeviceInfo) {
		currentReconnectAttempt = RECONNECT_ATTEMPTS_COUNT
		connectToObdInternal(activity, deviceInfo)
	}

	@SuppressLint("MissingPermission")
	@MainThread
	private fun connectToObdInternal(activity: Activity, deviceInfo: BTDeviceInfo) {
		currentReconnectAttempt--
		if (currentReconnectAttempt < 0) {
			return
		}
		saveDeviceToUsedOBDDevicesList(deviceInfo)
		LOG.debug("connectToObd $deviceInfo reconnectCount $currentReconnectAttempt")
		if (deviceInfo.isBLE) {
			val bleDevice = getBLEOBDDeviceById(deviceInfo.address)
			if (bleDevice != null) {
				if (!devicesHelper.isDevicePaired(bleDevice)) {
					devicesHelper.setDevicePaired(bleDevice, true)
				}
				connectDevice(activity, bleDevice, deviceInfo)
			}
			return
		}
		if (connectionState != OBDConnectionState.DISCONNECTED) {
			disconnect(true)
		}
		if (BLEUtils.isBLEEnabled(activity)) {
			if (AndroidUtils.hasBLEPermission(activity)) {
				onConnecting(deviceInfo)
				if (settings.SIMULATE_OBD_DATA.get() && deviceInfo.address.isEmpty()) {
					connectToSimulator(deviceInfo)
				} else {
					val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
					bluetoothAdapter?.apply {
						LOG.debug("adapter.isDiscovering $isDiscovering")
						val pairedDevices = bondedDevices.toList()
						val obdDevice: BluetoothDevice? =
							pairedDevices.find { it.address == deviceInfo.address }
						if (obdDevice != null) {
							connectToDevice(activity, obdDevice)
						} else {
							LOG.debug("bt device ${deviceInfo.name} - ${deviceInfo.address}")
							onDisconnected(deviceInfo)
						}
					}
				}
			} else {
				AndroidUtils.requestBLEPermissions(activity)
			}
		}
	}

	private fun createOBDDispatcher(): OBDDispatcher {
		LOG.info("createOBDDispatcher")
		obdDispatcher?.apply {
			setReadStatusListener(null)
			stopReading()
		}
		val debug = PluginsHelper.getPlugin(OsmandDevelopmentPlugin::class.java)?.isEnabled == true
		val obdDispatcher = OBDDispatcher(debug)
		obdDispatcher.setReadStatusListener(this)
		this.obdDispatcher = obdDispatcher
		OBDDataComputer.obdDispatcher = obdDispatcher
		return obdDispatcher
	}

	private fun connectToSimulator(deviceInfo: BTDeviceInfo) {
		createOBDDispatcher().connect(object : OBDConnector {
			val deviceToConnect = deviceInfo
			val simulator = OBDSimulationSource()
			override fun connect(): Pair<Source, Sink> {
				return Pair(simulator.reader, simulator.writer)
			}

			override fun onConnectionSuccess() {
				handler.post { onDeviceConnected(deviceToConnect) }
			}

			override fun onConnectionFailed() {
				handler.post { onDisconnected(deviceToConnect) }
			}

			override fun disconnect() {
			}
		})
	}

	fun connectToDevice(device: BLEOBDDevice) {
		createOBDDispatcher().connect(object : OBDConnector {

			override fun connect(): Pair<Source, Sink> {
				return Pair(device, device)
			}

			override fun disconnect() {
				LOG.debug("VMPlugin OBDConnector disconnect")
				app.runInUIThread {
					disconnect(true)
				}
			}

			override fun onConnectionSuccess() {
				LOG.debug("VMPlugin OBDConnector success")
				app.runInUIThread {
					val deviceToConnect = BTDeviceInfo(
						device.name,
						device.deviceId,
						true)
					onDeviceConnected(deviceToConnect)
				}
			}

			override fun onConnectionFailed() {
				LOG.debug("VMPlugin OBDConnector failed")
			}
		})
	}

	@SuppressLint("MissingPermission")
	private fun connectToDevice(activity: Activity, connectedDevice: BluetoothDevice) {
		createOBDDispatcher().connect(object : OBDConnector {
			private var socket: BluetoothSocket? = null
			val deviceToConnect = BTDeviceInfo(
				connectedDevice.getAliasName(activity),
				connectedDevice.address)

			init {
				socket = connectedDevice.createRfcommSocketToServiceRecord(uuid)
			}

			@Throws(IOException::class)
			override fun connect(): Pair<Source, Sink>? {
				socket?.apply {
					connect()
					if (isConnected) {
						return Pair(inputStream.source(), outputStream.sink())
					}
				}
				return null
			}

			override fun disconnect() {
				socket?.apply {
					safeClose()
					socket = null
				}
			}

			override fun onConnectionSuccess() {
				handler.post { onDeviceConnected(deviceToConnect) }
			}

			override fun onConnectionFailed() {
				handler.post { onDisconnected(deviceToConnect) }
			}
		})
	}

	private fun onDisconnected(deviceInfo: BTDeviceInfo?) {
		connectionState = OBDConnectionState.DISCONNECTED
		deviceInfo?.let {
			app.runInUIThread({connectionStateListener?.onStateChanged(OBDConnectionState.DISCONNECTED, it)})
			if (it.isBLE) {
				val device = getBLEOBDDeviceById(it.address);
				device?.disconnect()
			}
		}
	}

	private fun onConnecting(deviceInfo: BTDeviceInfo) {
		connectionState = OBDConnectionState.CONNECTING
		connectionStateListener?.onStateChanged(OBDConnectionState.CONNECTING, deviceInfo)
	}

	@SuppressLint("MissingPermission")
	private fun onDeviceConnected(btDeviceInfo: BTDeviceInfo) {
		LOG.debug("Device connected ${btDeviceInfo.name}")
		connectionState = OBDConnectionState.CONNECTED
		connectedDeviceInfo = btDeviceInfo
		connectedDeviceInfo?.let {
			saveDeviceToUsedOBDDevicesList(it)
			setLastConnectedDevice(it)
		}
		connectionStateListener?.onStateChanged(OBDConnectionState.CONNECTED, btDeviceInfo)
	}

	override fun getSettingsScreenType(): SettingsScreenType {
		return if (isConnected()) {
			SettingsScreenType.VEHICLE_CONNECTED_METRICS_SETTINGS
		} else {
			SettingsScreenType.VEHICLE_METRICS_SETTINGS
		}
	}


	fun getConnectedDeviceName(): String? {
		return connectedDeviceInfo?.name
	}

	fun getConnectedDeviceInfo(): BTDeviceInfo? {
		return connectedDeviceInfo
	}

	fun isConnected(): Boolean {
		return getConnectedDeviceInfo() != null
	}

	companion object {
		private val LOG = PlatformUtil.getLog(VehicleMetricsPlugin::class.java)
		val REQUEST_BT_PERMISSION_CODE = 50

		fun getFormatDistancePerVolume(
			metersPerLiter: Float,
			mc: MetricsConstants,
			volumeUnit: VolumeUnit): Float {
			val litersInVolume = OsmAndFormatter.convertLiterToVolumeUnit(volumeUnit, 1f)

			val distanceInMeters = when (mc) {
				MetricsConstants.MILES_AND_YARDS,
				MetricsConstants.MILES_AND_FEET,
				MetricsConstants.MILES_AND_METERS -> {
					OsmAndFormatter.METERS_IN_ONE_MILE
				}

				MetricsConstants.NAUTICAL_MILES_AND_FEET,
				MetricsConstants.NAUTICAL_MILES_AND_METERS -> {
					OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE
				}

				else -> {
					OsmAndFormatter.METERS_IN_KILOMETER
				}
			}

			return metersPerLiter / litersInVolume / distanceInMeters
		}
	}

	override fun onIOError() {
		handler.removeCallbacksAndMessages(null)
		handler.post { disconnect(false) }
		handler.postDelayed({ reconnectObd() }, RECONNECT_DELAY)
	}

	private fun reconnectObd() {
		mapActivity?.let {
			val lastConnectedDevice = getLastConnectedDevice()
			if (connectedDeviceInfo == null && lastConnectedDevice != null) {
				connectToObdInternal(it, lastConnectedDevice)
			}
		}
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
		if (deviceInfo.address.isNotEmpty()) {
			val currentList = getUsedOBDDevicesList().toMutableList()
			val savedDevice = currentList.find { it.address == deviceInfo.address && it.isBLE == deviceInfo.isBLE}
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
	}

	fun setDeviceName(address: String, newName: String, isBLE: Boolean) {
		removeDeviceToUsedOBDDevicesList(address, isBLE)
		saveDeviceToUsedOBDDevicesList(BTDeviceInfo(newName, address, isBLE))
	}

	fun removeDeviceToUsedOBDDevicesList(address: String, isBLE: Boolean) {
		val device =
			getUsedOBDDevicesList().find { info -> info.address == address && info.isBLE == isBLE }
		if (device != null) {
			if (isBLE) {
				val bleDevice = getBLEOBDDeviceById(address)
				bleDevice?.let {
					devicesHelper.setDevicePaired(it, false)
				}
			}
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
				val device: BluetoothDevice? =
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						intent.getParcelableExtra(
							BluetoothDevice.EXTRA_DEVICE,
							BluetoothDevice::class.java)
					} else {
						@Suppress("DEPRECATION")
						intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
					}

				device?.apply {
					when (intent.action) {
						BluetoothDevice.ACTION_FOUND -> {
							val deviceName = getAliasNameOrNull()
							deviceName?.let { name ->
								if (bondState != BluetoothDevice.BOND_BONDED) {
									scanDevicesListener?.onDeviceFound(
										BTDeviceInfo(name, address))
								}
							}
						}

						BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
							val bondState =
								intent.getIntExtra(
									BluetoothDevice.EXTRA_BOND_STATE,
									BluetoothDevice.BOND_NONE)
							when (bondState) {
								BluetoothDevice.BOND_BONDED -> {
									if (address == pairingDevice?.address) {
										scanDevicesListener?.onDevicePaired(
											BTDeviceInfo(getAliasName(context), address))
										pairingDevice = null
									}
								}

								BluetoothDevice.BOND_NONE -> {
									pairingDevice = null
									scanDevicesListener?.onDevicePairingFailed()
								}

								else -> {}
							}
						}

						else -> {}
					}
				}
			}
		}
	}

	override fun handleRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray) {
		super.handleRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_BT_PERMISSION_CODE) {
			for (grantResult in grantResults) {
				if (grantResult != PackageManager.PERMISSION_GRANTED) {
					return
				}
			}
			mapActivity?.let {
				searchUnboundDevices(it)
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun searchUnboundDevices(activity: Activity) {
		if (BLEUtils.isBLEEnabled(activity) && AndroidUtils.hasBLEPermission(activity)) {
			val bluetoothAdapter = BLEUtils.getBluetoothAdapter(activity)
			bluetoothAdapter?.startDiscovery()
		} else {
			AndroidUtils.requestBLEPermissions(activity)
		}
	}

	override fun mapActivityPause(activity: MapActivity) {
		super.mapActivityPause(activity)
		mapActivity = null
		try {
			activity.unregisterReceiver(bluetoothReceiver)
		} catch (_: IllegalArgumentException) {
		}
	}

	override fun mapActivityResume(activity: MapActivity) {
		super.mapActivityResume(activity)
		mapActivity = activity
		val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
		activity.registerReceiver(bluetoothReceiver, filter)
	}

	override fun mapActivityCreate(activity: MapActivity) {
		super.mapActivityCreate(activity)
		connectToLastConnectedDevice(activity, RECONNECT_ATTEMPTS_COUNT)
		devicesHelper.setActivity(activity)
	}

	override fun mapActivityDestroy(activity: MapActivity) {
		super.mapActivityDestroy(activity)
		devicesHelper.setActivity(null)
	}

	private fun connectToLastConnectedDevice(activity: MapActivity, attemptsCount: Int) {
		val lastConnectedDevice = getLastConnectedDevice()
		val registry = app.osmandMap.mapLayers.mapWidgetRegistry
		if (connectedDeviceInfo == null && lastConnectedDevice != null && registry.allWidgets.any {
				it.widget is OBDTextWidget && it.isEnabledForAppMode(app.settings.applicationMode)
			}) {
			currentReconnectAttempt = attemptsCount
			connectToObdInternal(activity, lastConnectedDevice)
		}
	}

	fun getWidgetValue(
		computerWidget: OBDDataComputer.OBDComputerWidget): String {
		val data = computerWidget.computeValue()
		if (data == "N/A") {
			return "N/A"
		} else if (data == null) {
			return "-"
		}
		if (computerWidget.type.requiredCommand == OBDCommand.OBD_FUEL_LEVEL_COMMAND) {
			var isNan = (data is Float) && data.isNaN()
			isNan = isNan || (data is Double) && data.isNaN()
			if (isNan) {
				return "-"
			}
		}
		val convertedData = when (computerWidget.type) {
			OBDDataComputer.OBDTypeWidget.SPEED -> getConvertedSpeed(data as Number)
			OBDDataComputer.OBDTypeWidget.FUEL_LEFT_KM -> getConvertedDistance(data as Double)
			OBDDataComputer.OBDTypeWidget.TEMPERATURE_INTAKE,
			OBDDataComputer.OBDTypeWidget.ENGINE_OIL_TEMPERATURE,
			OBDDataComputer.OBDTypeWidget.TEMPERATURE_AMBIENT,
			OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT -> getConvertedTemperature(
				data as Number)

			OBDDataComputer.OBDTypeWidget.FUEL_LEFT_LITER -> getFormattedVolume(data as Number)
			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR -> getFormatVolumePerHour(
				data as Number)

			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM -> getFormatVolumePerDistance(
				data as Number)

			OBDDataComputer.OBDTypeWidget.ENGINE_RUNTIME -> getFormattedTime(data as Int)
			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_SENSOR,
			OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE,
			OBDDataComputer.OBDTypeWidget.ADAPTER_BATTERY_VOLTAGE,
			OBDDataComputer.OBDTypeWidget.FUEL_TYPE,
			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR,
			OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT,
			OBDDataComputer.OBDTypeWidget.CALCULATED_ENGINE_LOAD,
			OBDDataComputer.OBDTypeWidget.THROTTLE_POSITION,
			OBDDataComputer.OBDTypeWidget.VIN,
			OBDDataComputer.OBDTypeWidget.FUEL_PRESSURE,
			OBDDataComputer.OBDTypeWidget.RPM -> data

			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_M_PER_LITER -> getFormatDistancePerVolume(
				data as Number)
		}

		return computerWidget.type.formatter.format(convertedData)
	}

	fun getWidgetUnit(computerWidget: OBDDataComputer.OBDComputerWidget): String? {
		return when (computerWidget.type) {
			OBDDataComputer.OBDTypeWidget.SPEED -> getSpeedUnit()
			OBDDataComputer.OBDTypeWidget.RPM -> app.getString(R.string.rpm_unit)
			OBDDataComputer.OBDTypeWidget.FUEL_PRESSURE -> app.getString(R.string.kpa_unit)
			OBDDataComputer.OBDTypeWidget.FUEL_LEFT_KM -> getDistanceUnit()
			OBDDataComputer.OBDTypeWidget.CALCULATED_ENGINE_LOAD,
			OBDDataComputer.OBDTypeWidget.THROTTLE_POSITION,
			OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT -> app.getString(R.string.percent_unit)

			OBDDataComputer.OBDTypeWidget.FUEL_LEFT_LITER -> settings.UNIT_OF_VOLUME.get()
				.getUnitSymbol(app)

			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR -> app.getString(R.string.percent_hour)
			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR -> getFormatVolumePerHourUnit()
			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_SENSOR -> app.getString(R.string.liter_per_hour)

			OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT,
			OBDDataComputer.OBDTypeWidget.TEMPERATURE_INTAKE,
			OBDDataComputer.OBDTypeWidget.ENGINE_OIL_TEMPERATURE,
			OBDDataComputer.OBDTypeWidget.TEMPERATURE_AMBIENT -> getTemperatureUnit().symbol

			OBDDataComputer.OBDTypeWidget.ADAPTER_BATTERY_VOLTAGE,
			OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE -> app.getString(R.string.unit_volt)

			OBDDataComputer.OBDTypeWidget.FUEL_TYPE,
			OBDDataComputer.OBDTypeWidget.ENGINE_RUNTIME,
			OBDDataComputer.OBDTypeWidget.VIN -> null

			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM -> getFormatVolumePerDistanceUnit()
			OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_M_PER_LITER -> getFormatDistancePerVolumeUnit()
		}
	}

	private fun getFormattedVolume(data: Number): Float {
		return OsmAndFormatter.convertLiterToVolumeUnit(
			settings.UNIT_OF_VOLUME.get(),
			data.toFloat())
	}

	private fun getConvertedTemperature(data: Number): Float {
		val temperature = data.toFloat()
		return if (getTemperatureUnit() == TemperatureUnit.CELSIUS) {
			temperature
		} else {
			temperature * 1.8f + 32
		}
	}

	private fun getFormatVolumePerHourUnit(): String {
		val volumeUnit = settings.UNIT_OF_VOLUME.get().getUnitSymbol(app)
		val hour = app.getString(R.string.int_hour)
		return app.getString(R.string.ltr_or_rtl_combine_via_slash, volumeUnit, hour)
	}

	private fun getFormatVolumePerDistanceUnit(): String {
		val mc: MetricsConstants = settings.METRIC_SYSTEM.get()
		val distanceUnit: String = when (mc) {
			MetricsConstants.MILES_AND_YARDS, MetricsConstants.MILES_AND_FEET, MetricsConstants.MILES_AND_METERS -> {
				app.getString(R.string.mile)
			}

			MetricsConstants.NAUTICAL_MILES_AND_FEET, MetricsConstants.NAUTICAL_MILES_AND_METERS -> {
				app.getString(R.string.nm)
			}

			else -> {
				app.getString(R.string.km)
			}
		}
		val volumeUnit = settings.UNIT_OF_VOLUME.get().getUnitSymbol(app)
		return app.getString(R.string.ltr_or_rtl_combine_via_slash, volumeUnit, "100$distanceUnit")
	}

	private fun getFormatDistancePerVolumeUnit(): String {
		val mc: MetricsConstants = settings.METRIC_SYSTEM.get()
		val distanceUnit: String = when (mc) {
			MetricsConstants.MILES_AND_YARDS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_METERS -> {
				app.getString(R.string.mile)
			}

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> {
				app.getString(R.string.nm)
			}

			else -> {
				app.getString(R.string.km)
			}
		}
		val volume = settings.UNIT_OF_VOLUME.get()
		val volumeUnit = volume.getUnitSymbol(app)
		return if ((mc == MetricsConstants.MILES_AND_FEET ||
					mc == MetricsConstants.MILES_AND_YARDS ||
					mc == MetricsConstants.MILES_AND_METERS) &&
			(volume == VolumeUnit.US_GALLONS || volume == VolumeUnit.IMPERIAL_GALLONS))
			app.getString(R.string.mpg)
		else
			app.getString(R.string.ltr_or_rtl_combine_via_slash, distanceUnit, volumeUnit)
	}

	private fun getFormatDistancePerVolume(metersPerLiter: Number): Float {
		val mc: MetricsConstants = settings.METRIC_SYSTEM.get()
		val volumeUnit = settings.UNIT_OF_VOLUME.get()
		val metersPerLiterFloat = metersPerLiter.toFloat()
		return getFormatDistancePerVolume(metersPerLiterFloat, mc, volumeUnit)
	}

	private fun getFormatVolumePerDistance(litersPer100km: Number): Float {
		val volumeResult: Float
		val volumeUnit = settings.UNIT_OF_VOLUME.get()
		volumeResult =
			OsmAndFormatter.convertLiterToVolumeUnit(volumeUnit, litersPer100km.toFloat())

		val mc: MetricsConstants = settings.METRIC_SYSTEM.get()
		return when (mc) {
			MetricsConstants.MILES_AND_YARDS, MetricsConstants.MILES_AND_FEET, MetricsConstants.MILES_AND_METERS -> {
				volumeResult * OsmAndFormatter.METERS_IN_ONE_MILE / 1000
			}

			MetricsConstants.NAUTICAL_MILES_AND_FEET, MetricsConstants.NAUTICAL_MILES_AND_METERS -> {
				volumeResult * OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE / 1000
			}

			else -> {
				volumeResult
			}
		}
	}

	private fun getFormatVolumePerHour(literPerHour: Number): Float {
		val volumeUnit = settings.UNIT_OF_VOLUME.get()
		return OsmAndFormatter.convertLiterToVolumeUnit(volumeUnit, literPerHour.toFloat())
	}

	private fun getFormattedTime(time: Int): String {
		return OsmAndFormatter.getFormattedTimeRuntime(time)
	}

	private fun getConvertedSpeed(speed: Number): Float {
		val formattedValue =
			OsmAndFormatter.getFormattedSpeedValue(speed.toFloat() * 1000 / 3600, app)
		return formattedValue.valueSrc
	}

	private fun getConvertedDistance(distance: Double): Float {
		val formattedValue = OsmAndFormatter.getFormattedDistanceValue(distance.toFloat(), app)
		return formattedValue.valueSrc
	}

	private fun getSpeedUnit(): String {
		val mode = app.settings.applicationMode
		val speedMode = app.settings.SPEED_SYSTEM.getModeValue(mode)
		return speedMode.toShortString()
	}

	private fun getDistanceUnit(): String {
		val mc = app.settings.METRIC_SYSTEM.get()
		return app.getString(
			when (mc) {
				MetricsConstants.KILOMETERS_AND_METERS -> R.string.km
				MetricsConstants.NAUTICAL_MILES_AND_METERS,
				MetricsConstants.NAUTICAL_MILES_AND_FEET -> R.string.nm

				else -> R.string.mile
			})
	}

	private fun getTemperatureUnit(): TemperatureUnit {
		return app.settings.temperatureUnit
	}

	override fun attachAdditionalInfoToRecordedTrack(location: Location, json: JSONObject) {
		super.attachAdditionalInfoToRecordedTrack(location, json)
		val mode = app.settings.applicationMode
		val commandNames: List<String>? =
			TRIP_RECORDING_VEHICLE_METRICS.getStringsListForProfile(mode)
		val selectedCommands: List<OBDCommand> = commandNames?.mapNotNull {
			OBDCommand.getCommand(it)
		} ?: emptyList()
		if (!Algorithms.isEmpty(selectedCommands) && InAppPurchaseUtils.isVehicleMetricsAvailable(
				app)) {
			val rawData = obdDispatcher?.getRawData()
			rawData?.let { data ->
				for (command in data.keys) {
					if (!selectedCommands.contains(command)) {
						continue
					}
					val dataField = rawData[command]
					if (command.gpxTag != null) {
						json.put(
							GpxUtilities.OSMAND_EXTENSIONS_PREFIX + command.gpxTag,
							dataField?.value)
					}
				}
			}
		}
	}

	override fun onCarNavigationSessionCreated() {
		super.onCarNavigationSessionCreated()
		val activity = mapActivity
		val lastConnectedDevice = getLastConnectedDevice()
		LOG.debug("onCarNavigationSessionCreated $connectionState $lastConnectedDevice ${activity != null}")
		if (connectionState == OBDConnectionState.DISCONNECTED && lastConnectedDevice != null && activity != null) {
			connectToObd(activity, lastConnectedDevice)
		}
	}

	override fun newRouteIsCalculated(newRoute: Boolean) {
		mapActivity?.let {
			connectToLastConnectedDevice(it, RECALCULATE_RECONNECT_ATTEMPTS_COUNT)
		}
	}

	fun getBLEOBDDeviceById(deviceId: String): BLEOBDDevice? {
		val device = devicesHelper.getAnyDevice(deviceId)
		return if (device is BLEOBDDevice) device else null
	}

	fun searchBLEDevices() {
		devicesHelper.scanBLEDevices(true)
		Handler(Looper.myLooper()!!).postDelayed(
			{ this.finishBLEDevicesSearch() },
			DEVICES_SEARCH_TIMEOUT.toLong())
	}

	fun onBLEDeviceFound(device: BLEOBDDevice) {
		scanBLEDevicesListener?.onDeviceFound(device)
	}

	fun finishBLEDevicesSearch() {
		devicesHelper.scanBLEDevices(false)
		scanBLEDevicesListener?.onScanFinished(
			devicesHelper.unpairedDevices
				.filterIsInstance<BLEOBDDevice>())
	}

	fun setScanBLEDevicesListener(listener: ScanBLEDevicesListener?) {
		scanBLEDevicesListener = listener
	}

	interface ScanBLEDevicesListener {
		fun onScanFinished(foundDevices: List<BLEOBDDevice>)
		fun onDeviceFound(foundDevice: BLEOBDDevice)
	}

	private fun isDevicePaired(device: AbstractDevice<*>): Boolean {
		return devicesHelper.isDevicePaired(device)
	}

	private fun connectDevice(activity: Activity?, device: BLEOBDDevice, deviceInfo: BTDeviceInfo) {
		if (isDevicePaired(device)) {
			devicesHelper.setDeviceEnabled(device, true)
		}
		device.addListener(object : DeviceListener {
			override fun onDeviceConnecting(device: AbstractDevice<*>) {
				onConnecting(deviceInfo)
			}

			override fun onDeviceConnect(
				device: AbstractDevice<*>,
				result: DeviceConnectionResult,
				error: String?) {
				onDeviceConnected(deviceInfo)
			}

			override fun onDeviceDisconnect(device: AbstractDevice<*>) {
				onDisconnected(deviceInfo)
			}

			override fun onSensorData(sensor: AbstractSensor, data: SensorData) {
			}

		})
		devicesHelper.connectDevice(activity, device)
	}
}
