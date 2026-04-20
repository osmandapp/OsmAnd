package net.osmand.shared.vehicle.specification.domain.util

import net.osmand.shared.util.Localization

data class Assets(
	val iconDayName: String,
	val iconNightName: String,
	val summaryName: String
) {

	fun getIconName(nightMode: Boolean) =
		if (nightMode) iconNightName else iconDayName

	fun getSummary() = Localization.getString(summaryName)
}