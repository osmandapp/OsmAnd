package net.osmand.plus.myplaces.tracks

import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.FormattedValue
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.settings.enums.AltitudeMetrics

object MeasureUnitsFormatter {

	fun getFormattedValue(
		app: OsmandApplication,
		measureUnitType: MeasureUnitType,
		value: String,
		params: OsmAndFormatterParams? = null
	): FormattedValue {
		val am: AltitudeMetrics = app.settings.ALTITUDE_METRIC.get()

		var formatterParams = params
		if (formatterParams == null) {
			formatterParams = OsmAndFormatterParams()
			formatterParams.setExtraDecimalPrecision(3)
			formatterParams.setForcePreciseValue(true)
		}

		return when (measureUnitType) {
			MeasureUnitType.SPEED -> OsmAndFormatter.getFormattedSpeedValue(value.toFloat(), app)
			MeasureUnitType.ALTITUDE -> OsmAndFormatter.getFormattedAltitudeValue(value.toDouble(), app, am)
			MeasureUnitType.DISTANCE -> OsmAndFormatter.getFormattedDistanceValue(value.toFloat(), app, formatterParams)
			MeasureUnitType.TIME_DURATION -> FormattedValue(
				value.toFloat() / 1000 / 60,
				value,
				""
			)
			else -> FormattedValue(value.toFloat(), value, "")
		}
	}

	fun getUnitsLabel(
		app: OsmandApplication,
		unitType: MeasureUnitType,
	): String {
		// TODO: improve - it's not safe using direct call of the settings without application mode
		val mc = app.settings.METRIC_SYSTEM.get()
		val am = app.settings.ALTITUDE_METRIC.get()
		return unitType.getFilterUnitText(mc, am)
	}
}