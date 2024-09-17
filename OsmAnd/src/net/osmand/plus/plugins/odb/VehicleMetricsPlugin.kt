package net.osmand.plus.plugins.odb

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
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper
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
import net.osmand.shared.obd.OBDAirIntakeTempDataField
import net.osmand.shared.obd.OBDAmbientAirTempDataField
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDataField
import net.osmand.shared.obd.OBDDispatcher
import net.osmand.shared.obd.OBDEngineCoolantDataField
import net.osmand.shared.obd.OBDFuelLvlDataField
import net.osmand.shared.obd.OBDFuelTypeDataField
import net.osmand.shared.obd.OBDResponseListener
import net.osmand.shared.obd.OBDRpmDataField
import net.osmand.shared.obd.OBDSpeedDataField
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
		val noAppMode = arrayOf<ApplicationMode>()
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.OBD_SPEED, *noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.OBD_RPM, *noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.OBD_AIR_INTAKE_TEMP, *noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(
			WidgetType.OBD_ENGINE_COOLANT_TEMP,
			*noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.OBD_FUEL_TYPE, *noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.OBD_FUEL_LEVEL, *noAppMode)
		OBDDispatcher.addResponseListener(this)
		OBDDispatcher.setReadStatusListener(this)
	}

	override fun createWidgets(
		mapActivity: MapActivity, widgetsInfos: MutableList<MapWidgetInfo?>,
		appMode: ApplicationMode) {
		val creator = WidgetInfoCreator(app, appMode)
		val fuelTypeWidget: MapWidget =
			OBDTextWidget(mapActivity, OBDWidgetDataFieldType.FUEL_TYPE)
		widgetsInfos.add(creator.createWidgetInfo(fuelTypeWidget))
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

			WidgetType.OBD_FUEL_LEVEL -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_LVL)

			WidgetType.OBD_FUEL_TYPE -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.FUEL_TYPE)

			WidgetType.OBD_ENGINE_COOLANT_TEMP -> return OBDTextWidget(
				mapActivity,
				OBDWidgetDataFieldType.COOLANT_TEMP)

			else -> null
		}
	}

	override fun createMapWidgetForParams(
		mapActivity: MapActivity,
		widgetType: WidgetType): OBDTextWidget? {
		return createMapWidgetForParams(mapActivity, widgetType, null, null)
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
			OBDCommand.OBD_RPM_COMMAND -> OBDRpmDataField(result)
			OBDCommand.OBD_SPEED_COMMAND -> OBDSpeedDataField(result)
			OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND -> OBDAirIntakeTempDataField(result)
			OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND -> OBDEngineCoolantDataField(result)
			OBDCommand.OBD_FUEL_TYPE_COMMAND -> OBDFuelTypeDataField(result)
			OBDCommand.OBD_FUEL_LEVEL_COMMAND -> OBDFuelLvlDataField(result)
			OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND -> OBDAmbientAirTempDataField(result)
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
}