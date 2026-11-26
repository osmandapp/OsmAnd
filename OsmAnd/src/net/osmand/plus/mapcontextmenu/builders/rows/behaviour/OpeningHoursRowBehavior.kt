package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.data.Amenity
import net.osmand.plus.R
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.data.AmenityTagEntry
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
		    val entries = mutableListOf(AmenityTagEntry.Builder(key).setText(formattedValue).build())
		    val checkDate = additionalInfo.getFilteredInfo()[Amenity.CHECK_DATE_OPENING_HOURS]
		    if (key == Amenity.OPENING_HOURS && !checkDate.isNullOrEmpty()) {
			    val checkDateText = app.getString(R.string.ltr_or_rtl_combine_via_colon,
				    app.getString(R.string.poi_check_date), OsmAndFormatter.getFormattedIsoDate(app, checkDate))
			    entries.add(AmenityTagEntry.Builder(Amenity.CHECK_DATE_OPENING_HOURS)
				    .setValue(checkDate).setText(checkDateText).build())
		    }
		    builder.setCollapsableEntries(entries)
		    builder.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.OPENING_HOURS)

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