package net.osmand.plus.plugins.odb

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.drawable.Drawable
import android.view.View
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.odb.dialogs.OBDMainFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.BLEUtils
import net.osmand.plus.views.mapwidgets.MapWidgetInfo
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem
import net.osmand.shared.data.KLatLon
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataField
import net.osmand.shared.obd.OBDDataFieldType.*
import net.osmand.shared.obd.OBDDispatcher
import net.osmand.shared.obd.OBDResponseListener
import okio.IOException
import okio.sink
import okio.source
import java.util.UUID

class VehicleMetricsPlugin(app: OsmandApplication) : OsmandPlugin(app), OBDResponseListener,
	OBDDispatcher.OBDReadStatusListener {
	private val settings: OsmandSettings = app.settings

	val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	private val uuid =
		UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Standard UUID for SPP
	private var connectedDevice: BluetoothDevice? = null
	var socket: BluetoothSocket? = null
	private val sensorDataCache = HashMap<OBDCommand, OBDDataField?>()

	init {
		OBDDispatcher.addResponseListener(this)
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
		val airIntakeTempWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_AIR_INTAKE_TEMP)
		widgetsInfos.add(creator.createWidgetInfo(airIntakeTempWidget))
		val ambientAirTempWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_AMBIENT_AIR_TEMP)
		widgetsInfos.add(creator.createWidgetInfo(ambientAirTempWidget))
		val batteryVoltageWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_BATTERY_VOLTAGE)
		widgetsInfos.add(creator.createWidgetInfo(batteryVoltageWidget))
		val fuelLevelWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_LEVEL)
		widgetsInfos.add(creator.createWidgetInfo(fuelLevelWidget))
		val fuelLeftDistanceWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_LEFT_DISTANCE)
		widgetsInfos.add(creator.createWidgetInfo(fuelLeftDistanceWidget))
		val fuelConsumptionRateWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_CONSUMPTION_RATE)
		widgetsInfos.add(creator.createWidgetInfo(fuelConsumptionRateWidget))
		val fuelTypeWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_FUEL_TYPE)
		widgetsInfos.add(creator.createWidgetInfo(fuelTypeWidget))
		val engineCoolantTempWidget: MapWidget = createMapWidgetForParams(mapActivity, WidgetType.OBD_ENGINE_COOLANT_TEMP)
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

			WidgetType.OBD_FUEL_LEVEL -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_LVL)

			WidgetType.OBD_FUEL_LEFT_DISTANCE -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_LEFT_DISTANCE)

			WidgetType.OBD_FUEL_CONSUMPTION_RATE -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_CONSUMPTION_RATE)

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
		return R.drawable.ic_action_external_sensor
//		return R.drawable.ic_action_car_info
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
						connectToDevice()
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
	private fun connectToDevice() {
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
		} catch (error: IOException) {
			LOG.error("Can't connect to device. $error")
		}
	}

	fun addCommandToRead(command: OBDCommand) {
		if (isCommandListening(command)) {
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

	override fun onCommandResponse(command: OBDCommand, result: String) {
		LOG.debug("OBD-II command ${command.name} result : $result")
		sensorDataCache[command] = when (command) {
			OBDCommand.OBD_RPM_COMMAND -> OBDDataField(RPM,  result)
			OBDCommand.OBD_SPEED_COMMAND -> OBDDataField(SPEED, result)
			OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND -> OBDDataField(AIR_INTAKE_TEMP, result)
			OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND -> OBDDataField(COOLANT_TEMP, result)
			OBDCommand.OBD_FUEL_TYPE_COMMAND -> OBDDataField(FUEL_TYPE, result)
			OBDCommand.OBD_FUEL_LEVEL_COMMAND -> OBDDataField(FUEL_LVL, result)
			OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND -> OBDDataField(AMBIENT_AIR_TEMP, result)
			OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND -> OBDDataField(BATTERY_VOLTAGE, result)
			else -> null
		}
	}

	fun isCommandListening(command: OBDCommand): Boolean {
		return OBDDispatcher.getCommandQueue().contains(command)
	}

	fun getSensorData(dataField: OBDWidgetDataFieldType): OBDDataField? {
		if (!isCommandListening(dataField.command)) {
			OBDDispatcher.addCommand(dataField.command)
		}
		return sensorDataCache[dataField.command]
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

	override fun updateLocation(location: Location) {
		OBDDataComputer.registerLocation(OBDDataComputer.OBDLocation(location.time, KLatLon(location.latitude, location.longitude)))
	}

	override fun onBatchReadCompleted() {
		OBDDataComputer.acceptValue(sensorDataCache)
	}
}