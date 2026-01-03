package net.osmand.plus.track

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.myplaces.tracks.MeasureUnitsFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.organization.OrganizeByResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

object AndroidOrganizeByResourceMapper: OrganizeByResourceMapper() {

	lateinit var app: OsmandApplication

	override fun resolveName(type: OrganizeByType, value: Any): String {
		if (value is Limits) {
			val params = OsmAndFormatterParams()
			params.setExtraDecimalPrecision(0)
			params.setForcePreciseValue(true)

			val unitType = type.filterType.measureUnitType
			val min = value.min.toDouble()
			val max = value.max.toDouble()

			val from = MeasureUnitsFormatter.getFormattedValue(app, unitType, min.toString(), params)
			val to = MeasureUnitsFormatter.getFormattedValue(app, unitType, max.toString(), params)
			val formattedRange = app.getString(R.string.ltr_or_rtl_combine_via_dash, from.value, to.value)

			val unitsLabel = MeasureUnitsFormatter.getUnitsLabel(app, unitType)
			return app.getString(R.string.ltr_or_rtl_combine_via_space, formattedRange, unitsLabel)
		}
		return value.toString()
	}

	override fun resolveIconName(type: OrganizeByType, value: Any): String {
		return super.resolveIconName(type, value)
	}
}