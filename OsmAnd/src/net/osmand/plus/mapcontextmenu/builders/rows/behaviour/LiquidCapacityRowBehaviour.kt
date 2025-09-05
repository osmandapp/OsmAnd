package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.R

object LiquidCapacityRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
		    if (subtype == "water_tower" || subtype == "storage_tank") {
			    builder.setText(value + " " + app.getString(R.string.cubic_m))
		    }
	    }
    }
}