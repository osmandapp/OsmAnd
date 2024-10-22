package net.osmand.plus.plugins.odb

import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.util.Algorithms

class OBDTextWidget(
	mapActivity: MapActivity,
	widgetType: WidgetType,
	fieldType: OBDTypeWidget,
	customId: String?,
	widgetsPanel: WidgetsPanel?) :
	SimpleWidget(mapActivity, widgetType, customId, widgetsPanel) {
	private val plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin::class.java)
	private val widgetComputer: OBDComputerWidget
	private var cacheTextData: String? = null
	private var cacheSubTextData: String? = null

	init {
		var averageTimeSeconds = 0

		if (fieldType == OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR ||
			fieldType == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR ||
			fieldType == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM) {
			averageTimeSeconds = 5 * 60
		}

		//todo implement setting correct time for widget (0 for instant)
		widgetComputer =
			OBDDataComputer.registerWidget(fieldType, averageTimeSeconds)
	}

	override fun updateSimpleWidgetInfo(drawSettings: DrawSettings?) {
		val subtext: String? = plugin?.getWidgetUnit(widgetComputer)
		val textData: String = plugin?.getWidgetValue(widgetComputer) ?: NO_VALUE
		if (!Algorithms.objectEquals(textData, cacheTextData) ||
			!Algorithms.objectEquals(subtext, cacheSubTextData)) {
			setText(textData, subtext)
			cacheTextData = textData
			cacheSubTextData = subtext
		}
	}

	override fun isMetricSystemDepended(): Boolean {
		return true
	}

	init {
		updateInfo(null)
		setIcons(widgetType)
	}
}