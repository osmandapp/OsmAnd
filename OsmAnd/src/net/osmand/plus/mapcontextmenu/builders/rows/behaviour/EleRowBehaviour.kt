package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import net.osmand.PlatformUtil
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.settings.enums.AltitudeMetrics

object EleRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

	val LOG = PlatformUtil.getLog(EleRowBehaviour::class.java)!!

	override fun applyCustomRules(params: PoiRowParams) {
		super.applyCustomRules(params)
		with(params) {
			var vl = value
			val altitudeMetrics = app.settings.ALTITUDE_METRIC.get()
			try {
				val distance: Float = vl.toFloat()
				vl = OsmAndFormatter.getFormattedAlt(distance.toDouble(), app, altitudeMetrics)

				val collapsibleVal = if (altitudeMetrics == AltitudeMetrics.FEET) {
					OsmAndFormatter.getFormattedAlt(distance.toDouble(), app, AltitudeMetrics.METERS)
				} else {
					OsmAndFormatter.getFormattedAlt(distance.toDouble(), app, AltitudeMetrics.FEET)
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