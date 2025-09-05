package net.osmand.plus.plugins.odb

import android.view.View
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.settings.enums.VolumeUnit.LITRES
import net.osmand.plus.utils.next
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.util.Algorithms

class OBDFuelConsumptionWidget(
	mapActivity: MapActivity,
	widgetType: WidgetType,
	fieldType: OBDTypeWidget,
	customId: String?,
	widgetsPanel: WidgetsPanel?
) :
	OBDTextWidget(mapActivity, widgetType, fieldType, customId, widgetsPanel) {

	var fuelConsumptionMode: OsmandPreference<FuelConsumptionMode> =
		registerFuelConsumptionPref(customId)

	companion object {
		private const val OBD_FUEL_CONSUMPTION_MODE = "obd_fuel_consumption_mode"
	}

	init {
		val typeWidget = getFieldType()
		widgetComputer = OBDDataComputer.registerWidget(typeWidget, getAverageTime(typeWidget))
	}

	private fun getFieldType(): OBDTypeWidget {
		return fuelConsumptionMode.get().fieldType
	}

	override fun updatePrefs(prefsChanged: Boolean) {
		super.updatePrefs(prefsChanged)
		val typeWidget = getFieldType()

		if (prefsChanged) {
			if (widgetComputer.type != typeWidget
				&& widgetComputer.averageTimeSeconds != 0
				&& (widgetComputer.averageTimeSeconds != typeWidget.defaultAverageTime
						&& (typeWidget != OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR
						&& typeWidget != OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR
						&& typeWidget != OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM))
			) {
				OBDDataComputer.removeWidget(widgetComputer)
			}
			widgetComputer = OBDDataComputer.registerWidget(typeWidget, getAverageTime(typeWidget))
		}

		updateSimpleWidgetInfo(null)
	}

	override fun getOnClickListener(): View.OnClickListener {
		return View.OnClickListener { _: View? ->
			nextMode()
		}
	}

	private fun nextMode() {
		fuelConsumptionMode.set(fuelConsumptionMode.get().next())
		updatePrefs(true)
	}

	private fun getAverageTime(typeWidget: OBDTypeWidget): Int {
		var averageTimeSeconds = 0
		if (typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR ||
			typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR ||
			typeWidget == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM
		) {
			averageTimeSeconds = 5 * 60
		}
		return averageTimeSeconds
	}

	private fun registerFuelConsumptionPref(customId: String?): OsmandPreference<FuelConsumptionMode> {
		val prefId = if (Algorithms.isEmpty(customId))
			OBD_FUEL_CONSUMPTION_MODE
		else OBD_FUEL_CONSUMPTION_MODE + customId

		val defaultMode =
			if (app.settings.UNIT_OF_VOLUME.get() == LITRES)
				FuelConsumptionMode.VOLUME_PER_100_UNITS
			else
				FuelConsumptionMode.UNITS_PER_VOLUME

		return settings.registerEnumStringPreference(
			prefId, defaultMode,
			FuelConsumptionMode.entries.toTypedArray(), FuelConsumptionMode::class.java
		)
			.makeProfile()
			.cache()
	}

	enum class FuelConsumptionMode(
		val fieldType: OBDTypeWidget
	) {
		UNITS_PER_VOLUME(
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_M_PER_LITER
		),
		VOLUME_PER_100_UNITS(
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM
		),
		VOLUME_PER_HOUR(
			OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR
		);

		fun getTitle(app: OsmandApplication): String {
			val volumeUnit = app.settings.UNIT_OF_VOLUME.get()
			val mc = app.settings.METRIC_SYSTEM.get()
			val leftText: String
			val rightText: String

			if (this == UNITS_PER_VOLUME) {
				val unitRes = when (mc) {
					MetricsConstants.KILOMETERS_AND_METERS -> R.string.kilometers
					MetricsConstants.NAUTICAL_MILES_AND_METERS, MetricsConstants.NAUTICAL_MILES_AND_FEET -> R.string.si_nm
					else -> R.string.miles
				}
				leftText = app.getString(unitRes)
				rightText = volumeUnit.toSingleHumanString(app)
			} else if (this == VOLUME_PER_100_UNITS) {
				val unitRes = when (mc) {
					MetricsConstants.KILOMETERS_AND_METERS -> R.string.kilometers
					MetricsConstants.NAUTICAL_MILES_AND_METERS, MetricsConstants.NAUTICAL_MILES_AND_FEET -> R.string.si_nm
					else -> R.string.miles
				}
				leftText = volumeUnit.toHumanString(app)
				rightText = app.getString(
					R.string.ltr_or_rtl_combine_via_space,
					"100",
					app.getString(unitRes)
				)
			} else {
				leftText = volumeUnit.toHumanString(app)
				rightText = app.getString(R.string.shared_string_hour).lowercase()
			}
			return app.getString(R.string.ltr_or_rtl_combine_via_per, leftText, rightText)
		}
	}
}