package net.osmand.plus.track

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByCategory
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AndroidOrganizeTracksResourceMapper : OrganizeTracksResourceMapper() {

	lateinit var app: OsmandApplication

	override fun resolveName(type: OrganizeByType, value: Any): String {
		if (type == OrganizeByType.ACTIVITY) {
			val activity = app.routeActivityHelper.findRouteActivity(value as String)
			return activity?.label ?: app.getString(R.string.shared_string_none)
		} else if (type.category == OrganizeByCategory.DATE_TIME) {
			val date = value as Long
			return if (date == 0L) {
				app.getString(R.string.no_date)
			} else {
				val formatter = when (type) {
					OrganizeByType.YEAR_OF_CREATION -> DateTimeFormatter.ofPattern(
						"yyyy",
						Locale.getDefault())

					OrganizeByType.MONTH_AND_YEAR -> DateTimeFormatter.ofPattern(
						"MMMM yyyy",
						Locale.getDefault())

					else -> throw IllegalArgumentException("Unknown OrganizeByType $type of category ${type.category}")
				}
				Instant.ofEpochMilli(date)
					.atZone(ZoneId.systemDefault())
					.format(formatter)
			}
		}

		if (value is Limits) {
			val displayUnits = type.getDisplayUnits()
			val min = value.min.toDouble()
			val max = value.max.toDouble()

			val from = displayUnits.fromBase(min).toInt()
			val to = displayUnits.fromBase(max).toInt()
			val formattedRange =
				app.getString(R.string.ltr_or_rtl_combine_via_dash, from.toString(), to.toString())

			val unitsLabel = displayUnits.getSymbol()
			return app.getString(R.string.ltr_or_rtl_combine_via_space, formattedRange, unitsLabel)
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