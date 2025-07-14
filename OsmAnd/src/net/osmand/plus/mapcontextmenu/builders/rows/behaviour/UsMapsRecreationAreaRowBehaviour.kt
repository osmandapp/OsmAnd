package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.util.Algorithms

object UsMapsRecreationAreaRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(
	    params: PoiRowParams
	) {
		super.applyCustomRules(params)
		with(params) {
		    val translatedUsMapsKey: String = app.poiTypes.poiTranslator.getTranslation(key)
		    builder.setTextPrefix(
			    if (!Algorithms.isEmpty(translatedUsMapsKey)) {
				    translatedUsMapsKey
			    } else {
				    Algorithms.capitalizeFirstLetterAndLowercase(key)
			    }
		    )
	    }
    }
}