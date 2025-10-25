package net.osmand.plus.plugins.astro

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.astro.views.CelestialPathView
import net.osmand.plus.plugins.astro.widgets.CelestialPathWidget
import net.osmand.plus.plugins.astro.widgets.PlanetsAltitudeWidget
import net.osmand.plus.plugins.astro.widgets.PlanetsVisibilityWidget
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper
import net.osmand.plus.views.mapwidgets.MapWidgetInfo
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem

class AstroPlugin(app: OsmandApplication) : OsmandPlugin(app) {

	init {
		val noAppMode = arrayOf<ApplicationMode?>()
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.PLANETS_VISIBILITY_WIDGET, *noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.PLANETS_ALTITUDE_WIDGET, *noAppMode)
		WidgetsAvailabilityHelper.regWidgetVisibility(WidgetType.CELESTIAL_PATH_WIDGET, *noAppMode)
	}

	override fun getId(): String {
		return OsmAndCustomizationConstants.PLUGIN_SKYMAP
	}

	override fun getName(): String {
		return app.getString(R.string.astro_plugin_name)
	}

	override fun getDescription(linksEnabled: Boolean): CharSequence {
		return app.getString(R.string.astro_plugin_description)
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

		val planetsVisibilityWidget = createMapWidgetForParams(mapActivity, WidgetType.PLANETS_VISIBILITY_WIDGET)
		if (planetsVisibilityWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(planetsVisibilityWidget))
		}
		val planetsAltitudeWidget = createMapWidgetForParams(mapActivity, WidgetType.PLANETS_ALTITUDE_WIDGET)
		if (planetsAltitudeWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(planetsAltitudeWidget))
		}
		val celestialPathWidget = createMapWidgetForParams(mapActivity, WidgetType.CELESTIAL_PATH_WIDGET)
		if (celestialPathWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(celestialPathWidget))
		}
	}

	override fun createMapWidgetForParams(mapActivity: MapActivity, widgetType: WidgetType): MapWidget? {
		return createMapWidgetForParams(mapActivity, widgetType, null, null)
	}

	override fun createMapWidgetForParams(mapActivity: MapActivity, widgetType: WidgetType,
										  customId: String?, widgetsPanel: WidgetsPanel?): MapWidget? {
		return when (widgetType) {
			WidgetType.PLANETS_VISIBILITY_WIDGET ->
				PlanetsVisibilityWidget(mapActivity, customId, widgetsPanel)

			WidgetType.PLANETS_ALTITUDE_WIDGET ->
				PlanetsAltitudeWidget(mapActivity, customId, widgetsPanel)

			WidgetType.CELESTIAL_PATH_WIDGET ->
				CelestialPathWidget(mapActivity, customId, widgetsPanel)

			else -> null
		}
	}

	override fun registerOptionsMenuItems(mapActivity: MapActivity, helper: ContextMenuAdapter) {
		if (isActive) {
			helper.addItem(
				ContextMenuItem(OsmAndCustomizationConstants.DRAWER_SKY_MAP_ID)
					.setTitleId(R.string.show_skymap, mapActivity)
					.setIcon(R.drawable.ic_action_favorite)
					.setOrder(18)
					.setListener { uiAdapter: OnDataChangeUiAdapter?, view: View?, item: ContextMenuItem?, isChecked: Boolean ->
						app.logEvent("skymapOpen")
						showSkymap(mapActivity)
						true
					}
			)
		}
	}

	private fun showSkymap(mapActivity: MapActivity) {
		SkymapFragment.showInstance(mapActivity)
	}
}
