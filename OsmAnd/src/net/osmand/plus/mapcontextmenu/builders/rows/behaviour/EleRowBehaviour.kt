package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.PlatformUtil
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.settings.enums.MetricsConstants.*

object EleRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    val LOG = PlatformUtil.getLog(EleRowBehaviour::class.java)!!

    override fun applyCustomRules(
	    params: PoiRowParams
    ) {
        super.applyCustomRules(params)
	    with(params) {
		    var vl = value
		    val metricSystem = app.settings.METRIC_SYSTEM.get()
		    try {
			    val distance: Float = vl.toFloat()
			    vl = OsmAndFormatter.getFormattedAlt(distance.toDouble(), app, metricSystem)
			    val collapsibleVal = if (metricSystem == MILES_AND_FEET
				    || metricSystem == MILES_AND_YARDS
				    || metricSystem == NAUTICAL_MILES_AND_FEET
			    ) {
				    OsmAndFormatter.getFormattedAlt(distance.toDouble(), app, KILOMETERS_AND_METERS)
			    } else {
				    OsmAndFormatter.getFormattedAlt(distance.toDouble(), app, MILES_AND_FEET)
			    }
			    val elevationData: MutableSet<String> = HashSet()
			    elevationData.add(collapsibleVal)
			    builder.setCollapsableView(menuBuilder.getDistanceCollapsableView(elevationData))
		    } catch (ex: NumberFormatException) {
			    LOG.error(ex)
		    }
		    builder.setText(vl)
	    }
    }
}