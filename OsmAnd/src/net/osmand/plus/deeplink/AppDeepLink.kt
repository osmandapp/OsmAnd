package net.osmand.plus.deeplink

import android.net.Uri
import net.osmand.plus.settings.backend.ApplicationMode

/**
 * Marketing deep links `https://osmand.net/app/<route>`, shared with the iOS app
 * (see OsmAnd-Issues #2987 / #3088). The route strings must stay identical on both platforms.
 */
enum class AppDeepLinkRoute(val path: String, val hasAppMode: Boolean = false) {
	MAIN(""),
	HELP("help"),
	HELP_WHATS_NEW("help/whats-new"),
	HELP_LAST_RELEASE_NOTES("help/last-release-notes"),
	SETTINGS("settings"),
	CLOUD("cloud"),
	PURCHASES("purchases"),
	PURCHASES_OSMAND_PRO("purchases/osmand-pro"),
	PURCHASES_MAPS_PLUS("purchases/maps-plus"),
	PLUGINS("plugins"),
	PLUGINS_TRIP_RECORDING("plugins/trip-recording"),
	PLUGINS_TOPOGRAPHY("plugins/topography"),
	PLUGINS_WEATHER("plugins/weather"),
	PLUGINS_EXTERNAL_SENSORS("plugins/external-sensors"),
	PLUGINS_VEHICLE_METRICS("plugins/vehicle-metrics"),
	PLAN_ROUTE("plan-a-route"),
	CONFIGURE_SCREEN_WIDGETS_LEFT("configure-screen/widgets/left"),
	CONFIGURE_SCREEN_WIDGETS_RIGHT("configure-screen/widgets/right"),
	CONFIGURE_SCREEN_WIDGETS_TOP("configure-screen/widgets/top"),
	CONFIGURE_SCREEN_WIDGETS_BOTTOM("configure-screen/widgets/bottom"),
	MAP_SETTINGS("map-settings"),
	MAP_SETTINGS_MAP_TYPE("map-settings/map-type"),
	MAP_SETTINGS_WIKIPEDIA("map-settings/wikipedia"),
	MAP_SETTINGS_POPULAR_PLACES("map-settings/popular-places"),
	MAP_SETTINGS_OVERLAY("map-settings/overlay"),
	MAP_SETTINGS_UNDERLAY("map-settings/underlay"),
	MAP_SETTINGS_TOPOGRAPHY("map-settings/topography"),
	MAP_SETTINGS_CONTOUR_LINES("map-settings/contour-lines"),
	MAP_SETTINGS_3D_BUILDINGS("map-settings/3d-buildings"),
	MAP_SETTINGS_ROUTES("map-settings/routes"),
	DESTINATIONS("destinations"),
	DESTINATIONS_DIRECTION_APPEARANCE("destinations/direction-appearance"),
	MY_PLACES_FAVORITES("my-places/favorites"),
	MY_PLACES_TRACKS("my-places/tracks"),
	MY_PLACES_OSM_EDITS("my-places/osm-edits"),
	MAPS_AND_RESOURCES("maps-and-resources"),
	MAPS_AND_RESOURCES_LOCAL("maps-and-resources/local"),
	MAPS_AND_RESOURCES_UPDATES("maps-and-resources/updates"),
	QUICK_ACTIONS_LOCK_SCREEN_ADD("quick-actions/lock-screen/add"),
	TRIP_RECORDING_EXTERNAL_SENSORS("trip-recording/external-sensors"),
	SEARCH("search"),
	TRIP_RECORDING_SETTINGS("settings/profile-settings/trip-recording", hasAppMode = true),
	DISTANCE_BY_TAP("settings/profile-settings/configure-screen/distance-by-tap", hasAppMode = true),
	SPEEDOMETER("settings/profile-settings/configure-screen/speedometer", hasAppMode = true),
	NAVIGATION_SCREEN("navigation-screen", hasAppMode = true);
}

class AppDeepLink(
	val route: AppDeepLinkRoute,
	val appMode: ApplicationMode? = null,
	val searchQuery: String? = null,
	val searchCategory: String? = null
)

object AppDeepLinkParser {

	private const val APP_PATH_SEGMENT = "app"
	private const val SEARCH_QUERY_PARAMETER = "q"

	private val appModeKeys = mapOf(
		"browse" to ApplicationMode.DEFAULT,
		"car" to ApplicationMode.CAR,
		"bicycle" to ApplicationMode.BICYCLE,
		"walking" to ApplicationMode.PEDESTRIAN
	)

	fun isAppDeepLink(uri: Uri): Boolean {
		val path = uri.path ?: return false
		return path.equals("/$APP_PATH_SEGMENT", ignoreCase = true)
				|| path.startsWith("/$APP_PATH_SEGMENT/", ignoreCase = true)
	}

	fun parse(uri: Uri): AppDeepLink? {
		if (!isAppDeepLink(uri)) {
			return null
		}
		val segments = uri.pathSegments.filter { it.isNotEmpty() }.map { it.lowercase() }
		val routePath = segments.drop(1).joinToString("/")

		AppDeepLinkRoute.entries.firstOrNull { !it.hasAppMode && it.path == routePath }?.let { route ->
			return if (route == AppDeepLinkRoute.SEARCH) {
				AppDeepLink(route, searchQuery = uri.getQueryParameter(SEARCH_QUERY_PARAMETER))
			} else {
				AppDeepLink(route)
			}
		}
		if (segments.size == 3 && segments[1] == AppDeepLinkRoute.SEARCH.path) {
			return AppDeepLink(AppDeepLinkRoute.SEARCH, searchCategory = segments[2])
		}
		AppDeepLinkRoute.entries.firstOrNull { it.hasAppMode && routePath.startsWith(it.path + "/") }?.let { route ->
			val appMode = appModeKeys[routePath.removePrefix(route.path + "/")] ?: return null
			return AppDeepLink(route, appMode = appMode)
		}
		return null
	}
}
