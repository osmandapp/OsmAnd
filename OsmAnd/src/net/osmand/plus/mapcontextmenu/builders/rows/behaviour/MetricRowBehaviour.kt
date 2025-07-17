package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.plus.R
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.util.OsmAndFormatter.FEET_IN_ONE_METER
import net.osmand.shared.util.OsmAndFormatter.YARDS_IN_ONE_METER
import net.osmand.util.Algorithms
import java.math.RoundingMode
import java.text.DecimalFormat

object MetricRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    private val DISTANCE_FORMAT = DecimalFormat("#.##").apply { roundingMode = RoundingMode.CEILING }

    override fun applyCustomRules(
	    params: PoiRowParams
	) {
		super.applyCustomRules(params)
		with(params) {
		    val metricSystem = app.settings.METRIC_SYSTEM.get()
		    val valueAsDouble = Algorithms.parseDoubleSilently(value, 0.0)
		    if (valueAsDouble > 0) {
			    val formattedValue = when (metricSystem) {
				    MetricsConstants.MILES_AND_FEET, MetricsConstants.NAUTICAL_MILES_AND_FEET ->
					    "${DISTANCE_FORMAT.format(valueAsDouble * FEET_IN_ONE_METER)} ${app.getString(R.string.foot)}"

				    MetricsConstants.MILES_AND_YARDS ->
					    "${DISTANCE_FORMAT.format(valueAsDouble * YARDS_IN_ONE_METER)} ${app.getString(R.string.yard)}"

				    else ->
					    "$value ${app.getString(R.string.m)}"
			    }
			    builder.setText(formattedValue)
		    } else {
			    builder.setText(value)
		    }
	    }
    }
}