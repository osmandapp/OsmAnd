package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.data.Amenity
import net.osmand.shared.data.AdditionalInfoBundle
import net.osmand.util.OpeningHoursParser

/**
 * xmas:opening_hours as plain text: the hours and, on the next line, the xmas:day_date season.
 * No open/closed color - a Christmas market stays on the map all year.
 */
object XmasOpeningHoursRowBehavior : DefaultPoiAdditionalRowBehaviour() {

	override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
			builder.setText(getText(value, additionalInfo))
		}
	}

	private fun getText(value: String, additionalInfo: AdditionalInfoBundle): String {
		val openingHours = OpeningHoursParser.parseOpenedHours(value)
		val hours = (openingHours?.toLocalString() ?: value).replace("; ", "\n")
		val season = additionalInfo.getFilteredInfo()[Amenity.XMAS_DAY_DATE]
		return if (season.isNullOrEmpty()) hours else hours + "\n" + season
	}
}
