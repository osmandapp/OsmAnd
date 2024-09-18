package net.osmand.plus.plugins.odb

import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.OBDWidgetDataFieldType.*
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDFuelTypeFormatter

class OBDTextWidget @JvmOverloads constructor(
	mapActivity: MapActivity,
	private val fieldType: OBDWidgetDataFieldType, customId: String? = null,
	widgetsPanel: WidgetsPanel? = null) :
	SimpleWidget(mapActivity, fieldType.widgetType, customId, widgetsPanel) {
	private val plugin: VehicleMetricsPlugin = PluginsHelper.getPlugin(VehicleMetricsPlugin::class.java)
	private val widgetComputer: OBDDataComputer.OBDComputerWidget

	init {
		val obdDataWidgetType = when(fieldType) {
			RPM -> OBDDataComputer.OBDTypeWidget.RPM
			SPEED -> OBDDataComputer.OBDTypeWidget.SPEED
			FUEL_LVL -> OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT
			AMBIENT_AIR_TEMP -> OBDDataComputer.OBDTypeWidget.TEMPERATURE_AMBIENT
			BATTERY_VOLTAGE -> OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE
			AIR_INTAKE_TEMP -> OBDDataComputer.OBDTypeWidget.TEMPERATURE_INTAKE
			COOLANT_TEMP -> OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT
			FUEL_TYPE -> OBDDataComputer.OBDTypeWidget.FUEL_TYPE
		}
		val formatter = if(obdDataWidgetType == OBDDataComputer.OBDTypeWidget.FUEL_TYPE) {
			OBDFuelTypeFormatter()
		} else {
			OBDDataComputer.OBDComputerWidgetFormatter()
		}
		widgetComputer = OBDDataComputer.registerWidget(obdDataWidgetType, 15, formatter)
	}
	
	override fun updateSimpleWidgetInfo(drawSettings: DrawSettings?) {
		val sensorData = plugin.getSensorData(fieldType)
		val data = widgetComputer.computeValue()
		if (data == null) {
			setText(NO_VALUE, null)
		} else {
			setText(widgetComputer.formatter.format(data), fieldType.dataType.getDisplayUnit())
		}
	}

	override fun isMetricSystemDepended(): Boolean {
		return true
	}

	init {
		updateInfo(null)
		setIcons(fieldType.widgetType)
	}
}