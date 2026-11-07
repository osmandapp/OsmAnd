package net.osmand.plus.deeplink

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.osmand.PlatformUtil
import net.osmand.osm.PoiCategory
import net.osmand.plus.AppInitializeListener
import net.osmand.plus.AppInitializer
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.backup.ui.BackupAuthorizationFragment
import net.osmand.plus.backup.ui.BackupCloudFragment
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.chooseplan.OsmAndFeature
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment
import net.osmand.plus.dialogs.WhatsNewDialogFragment
import net.osmand.plus.download.DownloadActivity
import net.osmand.plus.help.HelpActivity
import net.osmand.plus.mapmarkers.DirectionIndicationDialogFragment
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment
import net.osmand.plus.measurementtool.MeasurementToolFragment
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.PluginInfoFragment
import net.osmand.plus.plugins.PluginsFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.plugins.srtm.SRTMPlugin
import net.osmand.plus.plugins.weather.WeatherPlugin
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenuFragment
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.settings.fragments.BaseSettingsFragment
import net.osmand.plus.settings.fragments.SettingsScreenType
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.configure.buttons.CustomMapButtonsFragment
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment
import net.osmand.plus.views.mapwidgets.configure.dialogs.SpeedometerSettingsFragment
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment

/**
 * Opens the screen an [AppDeepLink] points to. Only navigates: it does not enable plugins,
 * map layers or purchases on the user's behalf.
 */
class AppDeepLinkRouter(private val mapActivity: MapActivity) {

	private val app = mapActivity.getApp()
	private val settings = app.settings
	private val fragmentManager get() = mapActivity.supportFragmentManager

	fun open(link: AppDeepLink) {
		if (app.isApplicationInitializing) {
			app.appInitializer.addListener(object : AppInitializeListener {
				override fun onFinish(init: AppInitializer) {
					init.removeListener(this)
					if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
						openRoute(link)
					}
				}
			})
		} else {
			openRoute(link)
		}
	}

	private fun openRoute(link: AppDeepLink) {
		closeOpenedScreens(keepRouteMenu = link.route == AppDeepLinkRoute.NAVIGATION_SCREEN)
		when (link.route) {
			AppDeepLinkRoute.MAIN -> Unit
			AppDeepLinkRoute.HELP -> startActivity(HelpActivity::class.java)
			AppDeepLinkRoute.HELP_WHATS_NEW -> openWhatsNewArticle()
			AppDeepLinkRoute.HELP_LAST_RELEASE_NOTES -> WhatsNewDialogFragment.showInstance(fragmentManager, app)
			AppDeepLinkRoute.SETTINGS -> BaseSettingsFragment.showInstance(mapActivity, SettingsScreenType.MAIN_SETTINGS)
			AppDeepLinkRoute.CLOUD -> openCloud()
			AppDeepLinkRoute.PURCHASES -> ChoosePlanFragment.showDefaultInstance(mapActivity)
			AppDeepLinkRoute.PURCHASES_OSMAND_PRO -> ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.ADVANCED_WIDGETS)
			AppDeepLinkRoute.PURCHASES_MAPS_PLUS -> ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.MONTHLY_MAP_UPDATES)
			AppDeepLinkRoute.PLUGINS -> PluginsFragment.showInstance(fragmentManager)
			AppDeepLinkRoute.PLUGINS_TRIP_RECORDING -> openPlugin(OsmandMonitoringPlugin::class.java)
			AppDeepLinkRoute.PLUGINS_TOPOGRAPHY -> openPlugin(SRTMPlugin::class.java)
			AppDeepLinkRoute.PLUGINS_WEATHER -> openPlugin(WeatherPlugin::class.java)
			AppDeepLinkRoute.PLUGINS_EXTERNAL_SENSORS -> openPlugin(ExternalSensorsPlugin::class.java)
			AppDeepLinkRoute.PLUGINS_VEHICLE_METRICS -> openPlugin(VehicleMetricsPlugin::class.java)
			AppDeepLinkRoute.PLAN_ROUTE -> MeasurementToolFragment.showInstance(fragmentManager)
			AppDeepLinkRoute.CONFIGURE_SCREEN_WIDGETS_LEFT -> openWidgetsPanel(WidgetsPanel.LEFT)
			AppDeepLinkRoute.CONFIGURE_SCREEN_WIDGETS_RIGHT -> openWidgetsPanel(WidgetsPanel.RIGHT)
			AppDeepLinkRoute.CONFIGURE_SCREEN_WIDGETS_TOP -> openWidgetsPanel(WidgetsPanel.TOP)
			AppDeepLinkRoute.CONFIGURE_SCREEN_WIDGETS_BOTTOM -> openWidgetsPanel(WidgetsPanel.BOTTOM)
			AppDeepLinkRoute.MAP_SETTINGS -> openDashboard(DashboardType.CONFIGURE_MAP)
			AppDeepLinkRoute.MAP_SETTINGS_MAP_TYPE -> SelectMapStyleBottomSheetDialogFragment.showInstance(fragmentManager)
			AppDeepLinkRoute.MAP_SETTINGS_WIKIPEDIA,
			AppDeepLinkRoute.MAP_SETTINGS_POPULAR_PLACES -> openDashboard(DashboardType.WIKIPEDIA)
			AppDeepLinkRoute.MAP_SETTINGS_OVERLAY -> openDashboard(DashboardType.OVERLAY_MAP)
			AppDeepLinkRoute.MAP_SETTINGS_UNDERLAY -> openDashboard(DashboardType.UNDERLAY_MAP)
			AppDeepLinkRoute.MAP_SETTINGS_TOPOGRAPHY -> openDashboard(DashboardType.TERRAIN)
			AppDeepLinkRoute.MAP_SETTINGS_CONTOUR_LINES -> openDashboard(DashboardType.CONTOUR_LINES)
			AppDeepLinkRoute.MAP_SETTINGS_3D_BUILDINGS -> openDashboard(DashboardType.BUILDINGS_3D)
			AppDeepLinkRoute.MAP_SETTINGS_ROUTES -> openDashboard(DashboardType.MAP_ROUTES)
			AppDeepLinkRoute.DESTINATIONS -> MapMarkersDialogFragment.showInstance(mapActivity)
			AppDeepLinkRoute.DESTINATIONS_DIRECTION_APPEARANCE -> DirectionIndicationDialogFragment.showInstance(fragmentManager)
			AppDeepLinkRoute.MY_PLACES_FAVORITES -> openMyPlaces(MyPlacesActivity.FAV_TAB)
			AppDeepLinkRoute.MY_PLACES_TRACKS -> openMyPlaces(MyPlacesActivity.GPX_TAB)
			AppDeepLinkRoute.MY_PLACES_OSM_EDITS -> openMyPlaces(R.string.osm_edits)
			AppDeepLinkRoute.MAPS_AND_RESOURCES -> openDownloads(DownloadActivity.DOWNLOAD_TAB)
			AppDeepLinkRoute.MAPS_AND_RESOURCES_LOCAL -> openDownloads(DownloadActivity.LOCAL_TAB)
			AppDeepLinkRoute.MAPS_AND_RESOURCES_UPDATES -> openDownloads(DownloadActivity.UPDATES_TAB)
			AppDeepLinkRoute.QUICK_ACTIONS_LOCK_SCREEN_ADD -> CustomMapButtonsFragment.showInstance(fragmentManager, null)
			AppDeepLinkRoute.TRIP_RECORDING_EXTERNAL_SENSORS -> BaseSettingsFragment.showInstance(mapActivity,
					SettingsScreenType.EXTERNAL_SETTINGS_WRITE_TO_TRACK_SETTINGS)
			AppDeepLinkRoute.SEARCH -> openSearch(link)
			AppDeepLinkRoute.TRIP_RECORDING_SETTINGS -> BaseSettingsFragment.showInstance(mapActivity,
					SettingsScreenType.MONITORING_SETTINGS, enabledAppMode(link))
			AppDeepLinkRoute.DISTANCE_BY_TAP -> {
				selectAppMode(link)
				DistanceByTapFragment.showInstance(mapActivity)
			}
			AppDeepLinkRoute.SPEEDOMETER -> {
				selectAppMode(link)
				SpeedometerSettingsFragment.showInstance(mapActivity)
			}
			AppDeepLinkRoute.NAVIGATION_SCREEN -> openNavigationScreen(enabledAppMode(link))
		}
	}

	private fun closeOpenedScreens(keepRouteMenu: Boolean) {
		if (keepRouteMenu && isRouteMenuShown()) {
			// Popping the route menu cancels route planning, so close only what is on top of it
			for (fragment in fragmentManager.fragments) {
				if (fragment is DialogFragment) {
					fragment.dismiss()
				}
			}
			fragmentManager.popBackStack(MapRouteInfoMenuFragment.TAG, 0)
		} else {
			mapActivity.fragmentsHelper.closeAllFragments()
			mapActivity.mapRouteInfoMenu.hide()
		}
		mapActivity.dashboard.hideDashboard(false)
		// Plan a route is not on the back stack; leave it unless it has unsaved changes (then it asks the user)
		mapActivity.fragmentsHelper.measurementToolFragment?.quit(false)
	}

	// MapRouteInfoMenu.isVisible() is false while the activity is paused, which is the case in onNewIntent()
	private fun isRouteMenuShown(): Boolean {
		return fragmentManager.findFragmentByTag(MapRouteInfoMenuFragment.TAG) != null
	}

	private fun openWhatsNewArticle() {
		val nightMode = app.daynightHelper.isNightMode(ThemeUsageContext.APP)
		AndroidUtils.openUrl(mapActivity, R.string.docs_latest_version, nightMode)
	}

	private fun openCloud() {
		if (app.backupHelper.isRegistered) {
			BackupCloudFragment.showInstance(fragmentManager)
		} else {
			BackupAuthorizationFragment.showInstance(fragmentManager)
		}
	}

	private fun <T : OsmandPlugin> openPlugin(pluginClass: Class<T>) {
		val plugin = PluginsHelper.getPlugin(pluginClass)
		if (plugin == null) {
			LOG.error("Deep link plugin is not available: ${pluginClass.simpleName}")
			return
		}
		PluginsFragment.showInstance(fragmentManager)
		fragmentManager.executePendingTransactions()
		val pluginsFragment = fragmentManager.findFragmentByTag(PluginsFragment.TAG)
		if (pluginsFragment != null) {
			PluginInfoFragment.showInstance(fragmentManager, pluginsFragment, plugin)
		}
	}

	private fun openWidgetsPanel(panel: WidgetsPanel) {
		ConfigureWidgetsFragment.showInstance(mapActivity, panel, settings.applicationMode, null)
	}

	private fun openDashboard(type: DashboardType) {
		mapActivity.dashboard.setDashboardVisibility(true, type)
	}

	private fun openMyPlaces(tabId: Int) {
		val intent = Intent(mapActivity, app.appCustomization.myPlacesActivity)
		intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		intent.putExtra(MapActivity.INTENT_PARAMS, Bundle().apply { putInt(MyPlacesActivity.TAB_ID, tabId) })
		mapActivity.startActivity(intent)
	}

	private fun openDownloads(tab: String) {
		val intent = Intent(mapActivity, app.appCustomization.downloadIndexActivity)
		intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		intent.putExtra(DownloadActivity.TAB_TO_OPEN, tab)
		mapActivity.startActivity(intent)
	}

	private fun startActivity(activityClass: Class<*>) {
		val intent = Intent(mapActivity, activityClass)
		intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		mapActivity.startActivity(intent)
	}

	private fun openSearch(link: AppDeepLink) {
		// A category key (e.g. "tourism") searches that category; a POI type key (e.g. "museum") searches by its name
		val poiType = link.searchCategory?.let { app.poiTypes.getAnyPoiTypeByKey(it) }
		val query = poiType?.translation ?: link.searchQuery ?: link.searchCategory ?: ""
		QuickSearchDialogFragment.showInstance(mapActivity, query, poiType as? PoiCategory,
				QuickSearchType.REGULAR, QuickSearchTab.CATEGORIES, null)
	}

	private fun openNavigationScreen(appMode: ApplicationMode) {
		mapActivity.mapActions.enterRoutePlanningModeGivenGpx(null, appMode, null, null,
				true, true, MapRouteInfoMenu.DEFAULT_MENU_STATE)
		// setShowMenu() above only takes effect on the next resume; show or refresh the menu now
		if (isRouteMenuShown()) {
			mapActivity.mapRouteInfoMenu.updateMenu()
		} else {
			mapActivity.mapRouteInfoMenu.show()
		}
	}

	private fun selectAppMode(link: AppDeepLink) {
		val appMode = enabledAppMode(link)
		if (settings.applicationMode != appMode) {
			settings.setApplicationMode(appMode)
		}
	}

	// A link may point to a profile the user has hidden; show it instead of opening a screen for an unavailable profile
	private fun enabledAppMode(link: AppDeepLink): ApplicationMode {
		val appMode = link.appMode ?: settings.applicationMode
		if (!ApplicationMode.values(app).contains(appMode)) {
			ApplicationMode.changeProfileAvailability(appMode, true, app)
		}
		return appMode
	}

	companion object {
		private val LOG = PlatformUtil.getLog(AppDeepLinkRouter::class.java)
	}
}
