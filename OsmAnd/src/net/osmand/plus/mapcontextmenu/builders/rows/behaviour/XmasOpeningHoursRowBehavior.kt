package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.data.Amenity
import net.osmand.shared.data.AdditionalInfoBundle
import net.osmand.util.OpeningHoursParser

object XmasOpeningHoursRowBehavior : DefaultPoiAdditionalRowBehaviour() {

	override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
			builder.setText(getText(value, additionalInfo))
		}
	}

	private fun getText(value: String, additionalInfo: AdditionalInfoBundle): String {
		val openingHours = if (isEveryRuleParsed(value)) OpeningHoursParser.parseOpenedHours(value) else null
		val hours = (openingHours?.toLocalString() ?: value).replace("; ", "\n")
		val season = additionalInfo.getFilteredInfo()[Amenity.XMAS_DAY_DATE]
		return if (season.isNullOrEmpty()) hours else hours + "\n" + season
	}

	private fun isEveryRuleParsed(value: String): Boolean =
		value.split(Amenity.SEPARATOR).filter { it.isNotBlank() }
			.all { OpeningHoursParser.parseOpenedHours(it) != null }
}
