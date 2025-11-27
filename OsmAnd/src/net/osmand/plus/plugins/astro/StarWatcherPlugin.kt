package net.osmand.plus.plugins.astro

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.astro.widgets.StarChartWidgetState
import net.osmand.plus.plugins.astro.widgets.SkyChartsWidget
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.views.mapwidgets.MapWidgetInfo
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem

class StarWatcherPlugin(app: OsmandApplication) : OsmandPlugin(app) {

	companion object {
		private const val SETTINGS_PREFERENCE_ID = "star_watcher_plugin_settings"
	}

	private val _swSettings by lazy { StarWatcherSettings(getSettingsPref()) }
	val swSettings: StarWatcherSettings get() = _swSettings

	init {
		val noAppMode = arrayOf<ApplicationMode?>()
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.STAR_CHART_WIDGET, *noAppMode)
	}

	override fun getId(): String {
		return OsmAndCustomizationConstants.PLUGIN_STAR_WATCHER
	}

	override fun getName(): String {
		return app.getString(R.string.star_watcher_plugin_name)
	}

	override fun getDescription(linksEnabled: Boolean): CharSequence {
		return app.getString(R.string.star_watcher_plugin_description)
	}

	override fun getLogoResourceId(): Int {
		return R.drawable.ic_action_favorite
	}

	override fun getAssetResourceImage(): Drawable? {
		return app.uiUtilities.getIcon(R.drawable.osmand_development)
	}

	override fun init(app: OsmandApplication, activity: Activity?): Boolean {
		return true
	}

	override fun createWidgets(mapActivity: MapActivity, widgetInfos: MutableList<MapWidgetInfo?>, appMode: ApplicationMode) {
		val creator = WidgetInfoCreator(app, appMode)

		val planetsVisibilityWidget = createMapWidgetForParams(mapActivity, WidgetType.STAR_CHART_WIDGET)
		if (planetsVisibilityWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(planetsVisibilityWidget))
		}
	}

	override fun createMapWidgetForParams(mapActivity: MapActivity, widgetType: WidgetType): MapWidget? {
		return createMapWidgetForParams(mapActivity, widgetType, null, null)
	}

	override fun createMapWidgetForParams(mapActivity: MapActivity, widgetType: WidgetType,
										  customId: String?, widgetsPanel: WidgetsPanel?): MapWidget? {
		return when (widgetType) {
			WidgetType.STAR_CHART_WIDGET ->
				SkyChartsWidget(mapActivity, StarChartWidgetState(app, customId), customId, widgetsPanel)

			else -> null
		}
	}

	private fun getSettingsPref(): CommonPreference<String> {
		val pref = registerStringPreference(SETTINGS_PREFERENCE_ID, "")
		pref.makeProfile().makeShared()
		return pref
	}

	fun showSkymap(mapActivity: MapActivity) {
		StarMapFragment.showInstance(mapActivity)
	}
}
