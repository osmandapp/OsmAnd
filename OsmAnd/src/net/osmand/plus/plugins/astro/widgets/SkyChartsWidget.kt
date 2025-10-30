package net.osmand.plus.plugins.astro.widgets

import android.view.View
import androidx.appcompat.widget.AppCompatButton
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.astro.views.CelestialPathView
import net.osmand.plus.plugins.astro.views.PlanetsAltitudeChartView
import net.osmand.plus.plugins.astro.views.PlanetsVisiblityChartView
import net.osmand.plus.plugins.astro.widgets.SkyChartWidgetState.SkyChartType
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget

class SkyChartsWidget(mapActivity: MapActivity, val skyChartWidgetState: SkyChartWidgetState, customId: String?, panel: WidgetsPanel?) :
	MapWidget(mapActivity, WidgetType.SKY_CHART_WIDGET, customId, panel) {

	private var planetsVisiblityView: PlanetsVisiblityChartView = view.findViewById(R.id.planets_visiblity_view)
	private var planetsAltitudeView: PlanetsAltitudeChartView = view.findViewById(R.id.planets_altitude_view)
	private var celestialPathView: CelestialPathView = view.findViewById(R.id.celestial_path_view)

	init {
		view.findViewById<AppCompatButton>(R.id.enter_3d_button).apply {
			setOnClickListener {

			}
		}
		view.findViewById<AppCompatButton>(R.id.switch_chart_button).apply {
			setOnClickListener {
				widgetState.changeToNextState();
				updateInfo(null)
			}
		}
	}

	override fun getLayoutId() = R.layout.sky_charts_widget

	override fun getWidgetState() = skyChartWidgetState

	override fun copySettingsFromMode(
		sourceAppMode: ApplicationMode,
		appMode: ApplicationMode,
		customId: String?
	) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId)
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId)
	}

	override fun updateInfo(drawSettings: DrawSettings?) {
		val chartType = widgetState.getSkyChartType()
		planetsVisiblityView.visibility = if (chartType == SkyChartType.PLANETS_VISIBLITY) View.VISIBLE else View.GONE
		planetsAltitudeView.visibility = if (chartType == SkyChartType.PLANETS_ALTITUDE) View.VISIBLE else View.GONE
		celestialPathView.visibility = if (chartType == SkyChartType.CELESTIAL_PATH) View.VISIBLE else View.GONE

		when (chartType) {
			SkyChartType.PLANETS_VISIBLITY -> planetsVisiblityView.invalidate()
			SkyChartType.PLANETS_ALTITUDE -> planetsAltitudeView.invalidate()
			else -> celestialPathView.invalidate()
		}
	}
}