package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.wikipedia.WikiAlgorithms

object WikipediaRowBehavior : DefaultPoiAdditionalRowBehaviour() {

	override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
			val wikiParams = WikiAlgorithms.getWikiParams(key, value)
			builder.setText(wikiParams.first).setHiddenUrl(wikiParams.second)
		}
	}
}