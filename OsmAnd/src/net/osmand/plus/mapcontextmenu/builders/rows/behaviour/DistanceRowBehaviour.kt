package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.PlatformUtil
import net.osmand.plus.R
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.settings.enums.MetricsConstants

object DistanceRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    val LOG = PlatformUtil.getLog(DistanceRowBehaviour::class.java)!!

    override fun applyCommonRules(
	    params: PoiRowParams
    ) {
		super.applyCommonRules(params)
		with(params) {
		    val metricSystem = app.settings.METRIC_SYSTEM.get()
		    try {
			    val valueAsFloatInMeters = value.toFloat() * 1000
			    val formattedValue = if (metricSystem == MetricsConstants.KILOMETERS_AND_METERS) {
				    "$value ${app.getString(R.string.km)}"
			    } else {
				    OsmAndFormatter.getFormattedDistance(valueAsFloatInMeters, app)
			    }
			    builder.setText(formattedValue)
			    val prefix = builder.textPrefix
			    builder.setTextPrefix(formatPrefix(prefix, app.getString(R.string.distance)))
		    } catch (e: RuntimeException) {
			    LOG.error(e.message, e)
		    }
	    }
    }
}