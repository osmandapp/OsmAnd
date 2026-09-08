package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.data.AmenityTagEntry
import net.osmand.plus.R
import net.osmand.shared.util.OpeningHoursParser
import java.util.Calendar

object OpeningHoursRowBehavior : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(
	    params: PoiRowParams
    ) {
		super.applyCustomRules(params)
		with(params) {
		    var vl = value
		    val formattedValue = vl.replace("; ", "\n").replace(",", ", ")
		    builder.setCollapsableEntries(listOf(AmenityTagEntry.Builder(key).setText(formattedValue).build()))
		    builder.collapsableEntryType = AmenityTagEntry.CollapsableEntryType.OPENING_HOURS

		    val openingHours = OpeningHoursParser.parseOpenedHours(vl)
		    if (openingHours != null) {
			    vl = openingHours.toLocalString()
			    val opened = openingHours.isOpenedForTime(System.currentTimeMillis())
			    builder.setTextColor(if (opened) R.color.color_ok else R.color.color_invalid)
		    }
		    builder.setText(vl.replace("; ", "\n"))
	    }
    }
}