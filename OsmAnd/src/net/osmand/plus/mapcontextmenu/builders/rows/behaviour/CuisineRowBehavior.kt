package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import java.lang.StringBuilder

object CuisineRowBehavior : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(
	    params: PoiRowParams
	) {
		super.applyCustomRules(params)
		with(params) {
		    val sb = StringBuilder()
		    val cuisines = value.split(";")
		    for (name in cuisines) {
			    val translation = app.poiTypes.getPoiTranslation("cuisine_$name")
			    if (sb.isNotEmpty()) {
				    sb.append(", ")
				    sb.append(translation.lowercase())
			    } else {
				    sb.append(translation)
			    }
		    }
		    builder.setText(sb.toString())
	    }
    }
}