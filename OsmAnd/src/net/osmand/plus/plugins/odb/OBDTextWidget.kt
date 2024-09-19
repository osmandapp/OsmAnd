package net.osmand.plus.plugins.odb

import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.odb.OBDWidgetDataFieldType.*
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidgetFormatter
import net.osmand.shared.obd.OBDFuelTypeFormatter

class OBDTextWidget @JvmOverloads constructor(
	mapActivity: MapActivity,
	private val fieldType: OBDWidgetDataFieldType, customId: String? = null,
	widgetsPanel: WidgetsPanel? = null) :
	SimpleWidget(mapActivity, fieldType.widgetType, customId, widgetsPanel) {
	private val plugin: VehicleMetricsPlugin = PluginsHelper.getPlugin(VehicleMetricsPlugin::class.java)
	private val widgetComputer: OBDComputerWidget

	init {
		val obdDataWidgetType: OBDTypeWidget
		var formatter = OBDComputerWidgetFormatter()
		when(fieldType) {
			RPM -> {
				obdDataWidgetType = OBDTypeWidget.RPM
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			FUEL_CONSUMPTION_RATE -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_CONSUMPTION_RATE
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			FUEL_LEFT_DISTANCE -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_LEFT_DISTANCE
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			SPEED -> {
				obdDataWidgetType = OBDTypeWidget.SPEED
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			FUEL_LVL -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_LEFT_PERCENT
				formatter = OBDComputerWidgetFormatter("%.2f")
			}
			AMBIENT_AIR_TEMP -> {
				obdDataWidgetType = OBDTypeWidget.TEMPERATURE_AMBIENT
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			BATTERY_VOLTAGE -> {
				obdDataWidgetType = OBDTypeWidget.BATTERY_VOLTAGE
				formatter = OBDComputerWidgetFormatter("%.2f")
			}
			AIR_INTAKE_TEMP -> {
				obdDataWidgetType = OBDTypeWidget.TEMPERATURE_INTAKE
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			COOLANT_TEMP -> {
				obdDataWidgetType = OBDTypeWidget.TEMPERATURE_COOLANT
				formatter = OBDComputerWidgetFormatter("%.0f")
			}
			FUEL_TYPE -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_TYPE
				formatter = OBDFuelTypeFormatter()
			}
		}
		widgetComputer = OBDDataComputer.registerWidget(obdDataWidgetType, 15, formatter)
	}
	
	override fun updateSimpleWidgetInfo(drawSettings: DrawSettings?) {
		val sensorData = plugin.getSensorData(fieldType)
		val data = widgetComputer.computeValue()
		if (data == null) {
			setText(NO_VALUE, null)
		} else {
			setText(data.toString(), fieldType.dataType.getDisplayUnit())
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