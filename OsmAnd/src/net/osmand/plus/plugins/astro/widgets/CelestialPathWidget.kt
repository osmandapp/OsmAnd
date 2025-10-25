package net.osmand.plus.plugins.astro.widgets

import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.astro.views.CelestialPathView
import net.osmand.plus.plugins.astro.views.PlanetsAltitudeChartView
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget

class CelestialPathWidget(mapActivity: MapActivity, customId: String?, panel: WidgetsPanel?) :
	MapWidget(mapActivity, WidgetType.CELESTIAL_PATH_WIDGET, customId, panel) {

	private var celestialPathView: CelestialPathView = view.findViewById(R.id.chart_view)

	override fun getLayoutId() = R.layout.celestial_path_widget

	override fun updateInfo(drawSettings: DrawSettings?) {
		celestialPathView.invalidate()
	}
}