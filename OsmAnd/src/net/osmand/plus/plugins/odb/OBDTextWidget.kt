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
import net.osmand.util.Algorithms

class OBDTextWidget @JvmOverloads constructor(
	mapActivity: MapActivity,
	private val fieldType: OBDWidgetDataFieldType, customId: String? = null,
	widgetsPanel: WidgetsPanel? = null) :
	SimpleWidget(mapActivity, fieldType.widgetType, customId, widgetsPanel) {
	private val plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin::class.java)
	private val widgetComputer: OBDComputerWidget
	private var cacheTextData: String? = null

	init {
		val obdDataWidgetType: OBDTypeWidget
		var formatter = OBDComputerWidgetFormatter()
		var averageTimeSeconds = 0

		when (fieldType) {
			RPM -> {
				obdDataWidgetType = OBDTypeWidget.RPM
				formatter = OBDComputerWidgetFormatter("%.0f")
			}

			FUEL_CONSUMPTION_RATE_PERCENT_HOUR -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR
				formatter = OBDComputerWidgetFormatter("%.0f")
				averageTimeSeconds = 5 * 60
			}

			FUEL_CONSUMPTION_RATE_LITER_HOUR -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR
				formatter = OBDComputerWidgetFormatter("%.0f")
				averageTimeSeconds = 5 * 60
			}

			FUEL_CONSUMPTION_RATE_SENSOR -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_CONSUMPTION_RATE_SENSOR
				formatter = OBDComputerWidgetFormatter("%.2f")
			}

			FUEL_LEFT_DISTANCE -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_LEFT_KM
				formatter = OBDComputerWidgetFormatter("%.0f")
			}

			SPEED -> {
				obdDataWidgetType = OBDTypeWidget.SPEED
				formatter = OBDComputerWidgetFormatter("%.0f")
			}

			FUEL_LEFT_PERCENT -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_LEFT_PERCENT
				formatter = OBDComputerWidgetFormatter("%.2f")
			}

			FUEL_LEFT_LITER -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_LEFT_LITER
				formatter = OBDComputerWidgetFormatter("%.2f")
			}

			FUEL_LVL_PERCENT -> {
				obdDataWidgetType = OBDTypeWidget.FUEL_PERCENT
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

			VIN -> {
				obdDataWidgetType = OBDTypeWidget.VIN
				formatter = OBDComputerWidgetFormatter("%s")
			}
		}
		//todo implement setting correct time for widget (0 for instant)
		widgetComputer =
			OBDDataComputer.registerWidget(obdDataWidgetType, averageTimeSeconds, formatter)
	}

	override fun updateSimpleWidgetInfo(drawSettings: DrawSettings?) {
		val data = widgetComputer.computeValue()
		val textData: String
		val subtext: String?
		if (data == null) {
			textData = NO_VALUE
			subtext = null
		} else {
			textData = data.toString()
			subtext = fieldType.dataType.getDisplayUnit()
		}
		if (!Algorithms.objectEquals(textData, cacheTextData)) {
			setText(textData, subtext)
			cacheTextData = textData
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