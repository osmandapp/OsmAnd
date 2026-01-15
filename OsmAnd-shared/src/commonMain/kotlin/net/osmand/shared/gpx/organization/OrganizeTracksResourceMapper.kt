package net.osmand.shared.gpx.organization

import co.touchlab.stately.collections.ConcurrentMutableMap
import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.RouteActivityHelper
import net.osmand.shared.gpx.organization.enums.OrganizeByCategory
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import net.osmand.shared.util.SharedDateFormatter
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization

object OrganizeTracksResourceMapper {

	private val nameCache = ConcurrentMutableMap<String, String>()
	private val iconNameCache = ConcurrentMutableMap<String, String>()

	fun getName(type: OrganizeByType, value: Any): String {
		val key = buildNameKey(type, value)
		return nameCache.getOrPut(key) { resolveName(type, value) }
	}

	fun getIconName(type: OrganizeByType, value: Any): String {
		val key = buildIconKey(type, value)
		return iconNameCache.getOrPut(key) { resolveIconName(type, value) }
	}

	fun clearCache() {
		nameCache.clear()
		iconNameCache.clear()
	}

	private fun buildNameKey(type: OrganizeByType, value: Any) = buildFullKey(type, value)

	private fun buildIconKey(type: OrganizeByType, value: Any): String {
		if (type == OrganizeByType.ACTIVITY) {
			return buildFullKey(type, value)
		}
		return type.name
	}

	private fun buildFullKey(type: OrganizeByType, value: Any) = "${type.name}__${value}"

	private fun resolveName(type: OrganizeByType, value: Any): String {
		if (value is Limits) {
			val displayUnits = type.getDisplayUnits()
			val min = value.min.toDouble()
			val max = value.max.toDouble()

			val from = displayUnits.fromBase(min).toInt()
			val to = displayUnits.fromBase(max).toInt()
			val formattedRange = Localization.getString(
				"ltr_or_rtl_combine_via_dash",
				from.toString(),
				to.toString()
			)

			return Localization.getString(
				"ltr_or_rtl_combine_via_space",
				formattedRange,
				displayUnits.getSymbol()
			)

		} else if (type.category == OrganizeByCategory.DATE_TIME) {
			val date = (value as? Number)?.toLong() ?: 0L
			return if (date == 0L) {
				Localization.getString("no_date")
			} else {
				when (type) {
					OrganizeByType.YEAR_OF_CREATION -> SharedDateFormatter.formatYear(date)

					OrganizeByType.MONTH_AND_YEAR -> SharedDateFormatter.formatMonthAndYear(date)

					else -> throw IllegalArgumentException("Unknown OrganizeByType $type of category ${type.category}")
				}
			}
		} else if (type == OrganizeByType.ACTIVITY && value is String) {
			val activity = RouteActivityHelper.findRouteActivity(value)
			return activity?.label ?: Localization.getString("shared_string_none")
		} else if (KAlgorithms.isEmpty(value.toString())) {
			return Localization.getString("shared_string_none")
		}
		return value.toString()
	}

	private fun resolveIconName(type: OrganizeByType, value: Any): String {
		if (value is String && type == OrganizeByType.ACTIVITY) {
			val activity = RouteActivityHelper.findRouteActivity(value)
			return activity?.iconName ?: "ic_action_activity"
		}
		return type.iconResId
	}
}