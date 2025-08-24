package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.R
import net.osmand.util.Algorithms

object MaxWeightRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
		    if (Algorithms.isInt(value)) {
			    builder.setText(value + " " + app.getString(R.string.metric_ton))
		    }
	    }
    }
}