package net.osmand.plus.plugins.odb

import android.view.View
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.util.Algorithms

class OBDFuelConsumptionWidget(
	mapActivity: MapActivity,
	widgetType: WidgetType,
	fieldType: OBDTypeWidget,
	customId: String?,
	widgetsPanel: WidgetsPanel?
) :
	OBDTextWidget(mapActivity, widgetType, fieldType, customId, widgetsPanel) {

	var fuelConsumptionMode: OsmandPreference<FuelConsumptionMode> = registerFuelConsumptionPref(customId)
	companion object {
		private const val OBD_FUEL_CONSUMPTION_MODE = "obd_fuel_consumption_mode"
	}

	init {
		var averageTimeSeconds = 0
		val typeWidget = getFieldType()
		if (typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR ||
			typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR ||
			typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM
		) {
			averageTimeSeconds = 5 * 60
		}

		OBDDataComputer.removeWidget(widgetComputer)
		widgetComputer = OBDDataComputer.registerWidget(typeWidget, averageTimeSeconds)
	}

	private fun getFieldType() : OBDTypeWidget{
		return fuelConsumptionMode.get().fieldType
	}

	private fun registerFuelConsumptionPref(customId: String?): OsmandPreference<FuelConsumptionMode> {
		val prefId = if (Algorithms.isEmpty(customId))
			OBD_FUEL_CONSUMPTION_MODE
		else OBD_FUEL_CONSUMPTION_MODE + customId

		return settings.registerEnumStringPreference(
			prefId, FuelConsumptionMode.DISTANCE_PER_VOLUME,
			FuelConsumptionMode.entries.toTypedArray(), FuelConsumptionMode::class.java)
			.makeProfile()
			.cache()
	}

	override fun updatePrefs() {
		super.updatePrefs()
		var averageTimeSeconds = 0
		val typeWidget = getFieldType()
		if (typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR ||
			typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR ||
			typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM
		) {
			averageTimeSeconds = 5 * 60
		}
		
		OBDDataComputer.removeWidget(widgetComputer)
		widgetComputer = OBDDataComputer.registerWidget(typeWidget, averageTimeSeconds)

		updateSimpleWidgetInfo(null)
	}

	override fun getWidgetName(): String? {
		val widgetName = if (widgetType != null) getString(widgetType.titleId) else null

		if (supportsAverageMode() && !Algorithms.isEmpty(widgetName) && averageModePref?.get() == true) {
			val formattedInterval = formatIntervals(app, measuredIntervalPref!!.get())
			return app.getString(
				R.string.ltr_or_rtl_combine_via_colon,
				widgetName,
				formattedInterval
			)
		}
		return widgetName
	}

	enum class FuelConsumptionMode(
		val resId: Int,
		val fieldType: OBDTypeWidget
	) {
		DISTANCE_PER_VOLUME(
			R.string.obd_fuel_consumption_rate_distance_per_volume,
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM,
		),
		VOLUME_PER_100_UNITS(
			R.string.obd_fuel_consumption_rate_volume_per_100units,
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR
		),
		VOLUME_PER_HOUR(
			R.string.obd_fuel_consumption_rate_volume_per_hour,
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR
		),
		SENSOR(
			R.string.obd_fuel_consumption_rate_sensor_type,
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_SENSOR
		);

		fun getTitle(app: OsmandApplication): String {
			return app.getString(resId)
		}
	}
}