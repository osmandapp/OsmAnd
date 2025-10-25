package net.osmand.plus.plugins.astro.widgets

import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.astro.views.PlanetsAltitudeChartView
import net.osmand.plus.plugins.astro.views.PlanetsVisiblityChartView
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget

class PlanetsAltitudeWidget(mapActivity: MapActivity, customId: String?, panel: WidgetsPanel?) :
	MapWidget(mapActivity, WidgetType.PLANETS_ALTITUDE_WIDGET, customId, panel) {

	private var chartView: PlanetsAltitudeChartView = view.findViewById(R.id.chart_view)

	override fun getLayoutId() = R.layout.planets_altitude_chart_widget

	override fun updateInfo(drawSettings: DrawSettings?) {
		chartView.invalidate()
	}
}