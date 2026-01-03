package net.osmand.plus.track

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.organization.OrganizeByResourcesMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

object AndroidOrganizeByResourcesMapper: OrganizeByResourcesMapper() {

	lateinit var app: OsmandApplication

	override fun resolveName(type: OrganizeByType, value: Any): String {
		if (value is Limits) {
			val params = OsmAndFormatterParams()
			params.setExtraDecimalPrecision(0)
			params.setForcePreciseValue(true)

			val from = OsmAndFormatter.getFormattedDistanceValue(value.min.toFloat(), app, params)
			val to = OsmAndFormatter.getFormattedDistanceValue(value.max.toFloat(), app, params)
			val formattedRange = app.getString(R.string.ltr_or_rtl_combine_via_dash, from.value, to.value)

			val filterType = type.filterType
			val unitType = filterType.measureUnitType

			val settings = app.settings
			val metricsConstants = settings.METRIC_SYSTEM.get()
			val altitudeMetrics = settings.ALTITUDE_METRIC.get()
			val unitsLabel = unitType.getFilterUnitText(metricsConstants, altitudeMetrics)
			return app.getString(R.string.ltr_or_rtl_combine_via_space, formattedRange, unitsLabel)
		}
		return value.toString()
	}

	override fun resolveIconName(type: OrganizeByType, value: Any): String {
		return super.resolveIconName(type, value)
	}
}