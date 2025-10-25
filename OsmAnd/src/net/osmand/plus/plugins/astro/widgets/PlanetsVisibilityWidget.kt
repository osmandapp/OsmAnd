package net.osmand.plus.plugins.astro.widgets

import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.astro.views.PlanetsVisiblityChartView
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget

class PlanetsVisibilityWidget(mapActivity: MapActivity, customId: String?, panel: WidgetsPanel?) :
	MapWidget(mapActivity, WidgetType.PLANETS_VISIBILITY_WIDGET, customId, panel) {

	private var chartView: PlanetsVisiblityChartView = view.findViewById(R.id.chart_view)

	override fun getLayoutId() = R.layout.planets_visibility_chart_widget

	override fun updateInfo(drawSettings: DrawSettings?) {
		chartView.invalidate()
	}
}