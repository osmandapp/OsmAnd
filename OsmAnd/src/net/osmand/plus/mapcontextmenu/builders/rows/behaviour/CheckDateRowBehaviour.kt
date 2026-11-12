package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.utils.OsmAndFormatter

object CheckDateRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

	override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
			builder.setText(OsmAndFormatter.getFormattedIsoDate(context, value))
		}
	}
}
