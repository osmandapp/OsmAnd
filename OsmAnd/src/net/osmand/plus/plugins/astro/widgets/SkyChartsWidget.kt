package net.osmand.plus.plugins.astro.widgets

import android.view.View
import androidx.appcompat.widget.AppCompatButton
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astro.StarWatcherPlugin
import net.osmand.plus.plugins.astro.views.CelestialPathView
import net.osmand.plus.plugins.astro.views.StarAltitudeChartView
import net.osmand.plus.plugins.astro.views.StarVisiblityChartView
import net.osmand.plus.plugins.astro.widgets.StarChartWidgetState.StarChartType
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget
import java.time.LocalDate

class SkyChartsWidget(mapActivity: MapActivity, val starChartWidgetState: StarChartWidgetState, customId: String?, panel: WidgetsPanel?) :
	MapWidget(mapActivity, WidgetType.STAR_CHART_WIDGET, customId, panel) {

	private var starVisiblityView: StarVisiblityChartView = view.findViewById(R.id.star_visiblity_view)
	private var starAltitudeView: StarAltitudeChartView = view.findViewById(R.id.star_altitude_view)
	private var celestialPathView: CelestialPathView = view.findViewById(R.id.celestial_path_view)

	init {
		view.findViewById<AppCompatButton>(R.id.enter_3d_button).apply {
			setOnClickListener {
				val plugin = PluginsHelper.getActivePlugin(StarWatcherPlugin::class.java)
				plugin?.showSkymap(super.mapActivity)
			}
		}
		view.findViewById<AppCompatButton>(R.id.switch_chart_button).apply {
			setOnClickListener {
				widgetState.changeToNextState()
				updateInfo(null)
			}
		}
	}

	override fun getLayoutId() = R.layout.star_charts_widget

	override fun getWidgetState() = starChartWidgetState

	override fun copySettingsFromMode(
		sourceAppMode: ApplicationMode,
		appMode: ApplicationMode,
		customId: String?
	) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId)
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId)
	}

	override fun updateInfo(drawSettings: DrawSettings?) {
		val chartType = widgetState.getStarChartType()

		// Get current map center
		val mapLocation = mapActivity.mapView.currentRotatedTileBox.centerLatLon

		// Update visibility and push data to the active view
		when (chartType) {
			StarChartType.STAR_VISIBLITY -> {
				starVisiblityView.visibility = View.VISIBLE
				starAltitudeView.visibility = View.GONE
				celestialPathView.visibility = View.GONE
				starVisiblityView.updateData(mapLocation.latitude, mapLocation.longitude, LocalDate.now())
			}
			StarChartType.STAR_ALTITUDE -> {
				starVisiblityView.visibility = View.GONE
				starAltitudeView.visibility = View.VISIBLE
				celestialPathView.visibility = View.GONE
				starAltitudeView.updateData(mapLocation.latitude, mapLocation.longitude, LocalDate.now())
			}
			StarChartType.CELESTIAL_PATH -> {
				starVisiblityView.visibility = View.GONE
				starAltitudeView.visibility = View.GONE
				celestialPathView.visibility = View.VISIBLE
				celestialPathView.updateData(mapLocation.latitude, mapLocation.longitude, LocalDate.now())
			}
		}
	}
}