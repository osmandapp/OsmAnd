package net.osmand.plus.track

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

object AndroidOrganizeTracksResourceMapper: OrganizeTracksResourceMapper() {

	lateinit var app: OsmandApplication

	override fun resolveName(type: OrganizeByType, value: Any): String {
		if (value is Limits) {
			val displayUnits = type.getDisplayUnits()
			val min = value.min.toDouble()
			val max = value.max.toDouble()

			val from = displayUnits.fromBase(min).toInt()
			val to = displayUnits.fromBase(max).toInt()
			val formattedRange = app.getString(R.string.ltr_or_rtl_combine_via_dash, from.toString(), to.toString())

			val unitsLabel = displayUnits.getSymbol()
			return app.getString(R.string.ltr_or_rtl_combine_via_space, formattedRange, unitsLabel)
		} else if (value is String && type == OrganizeByType.ACTIVITY) {
			val activity = app.routeActivityHelper.findRouteActivity(value)
			return activity?.label ?: app.getString(R.string.shared_string_none)
		}
		return value.toString()
	}

	override fun resolveIconName(type: OrganizeByType, value: Any): String {
		if (value is String && type == OrganizeByType.ACTIVITY) {
			val activity = app.routeActivityHelper.findRouteActivity(value)
			return activity?.iconName ?: "ic_action_activity"
		}
		return super.resolveIconName(type, value)
	}
}