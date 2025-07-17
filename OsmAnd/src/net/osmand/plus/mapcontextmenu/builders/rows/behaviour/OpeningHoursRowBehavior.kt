package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.R
import net.osmand.util.OpeningHoursParser
import java.util.Calendar

object OpeningHoursRowBehavior : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(
	    params: PoiRowParams
    ) {
		super.applyCustomRules(params)
		with(params) {
		    var vl = value
		    val formattedValue = vl.replace("; ", "\n").replace(",", ", ")
		    builder.setCollapsableView(
			    menuBuilder.getCollapsableTextView(app, true, formattedValue)
		    )

		    val openingHours = OpeningHoursParser.parseOpenedHours(vl)
		    if (openingHours != null) {
			    vl = openingHours.toLocalString()
			    val inst = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }

			    val opened = openingHours.isOpenedForTime(inst)
			    builder.setTextColor(if (opened) R.color.color_ok else R.color.color_invalid)
		    }
		    builder.setText(vl.replace("; ", "\n"))
	    }
    }
}