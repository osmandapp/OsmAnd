package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.R
import net.osmand.util.Algorithms

object CapacityRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCommonRules(params: PoiRowParams) {
	    super.applyCommonRules(params)
	    with(params) {
		    if (Algorithms.isInt(value)) {
			    val prefix = builder.getTextPrefix() ?: ""
			    builder.setTextPrefix(formatPrefix(prefix, app.getString(R.string.shared_string_capacity)))
		    }
	    }
    }
}