package net.osmand.plus.helpers

import android.net.Uri
import androidx.car.app.ScreenManager
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.auto.NavigationSession
import net.osmand.plus.base.ContextMenuFragment.MenuState
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment
import net.osmand.router.GeneralRouter
import java.util.Locale

object GeoActionHelper {

	private val LOG = PlatformUtil.getLog(GeoActionHelper::class.java)

	const val SCHEME_GEO_ACTION = "geo.action"
	const val SCHEME_GEO_ACTION_OFFLINE = "geo.action.offline"

	const val ACTION_EXIT_NAVIGATION = "exit_navigation"
	const val ACTION_MUTE = "mute"
	const val ACTION_UNMUTE = "unmute"

	const val ACTION_AVOID_TOLLS = "avoid_tolls"
	const val ACTION_ALLOW_TOLLS = "allow_tolls"
	const val ACTION_AVOID_HIGHWAYS = "avoid_highways"
	const val ACTION_ALLOW_HIGHWAYS = "allow_highways"
	const val ACTION_AVOID_FERRIES = "avoid_ferries"
	const val ACTION_ALLOW_FERRIES = "allow_ferries"

	const val ACTION_SHOW_ALTERNATES = "show_alternates"
	const val ACTION_ROUTE_OVERVIEW = "route_overview"
	const val ACTION_SHOW_DIRECTIONS_LIST = "show_directions_list"
	const val ACTION_FOLLOW_MODE = "follow_mode"
	const val ACTION_GO_BACK = "go_back"

	@JvmStatic
	fun isGeoActionUri(uri: Uri?): Boolean {
		val scheme = uri?.scheme ?: return false
		return scheme.equals(SCHEME_GEO_ACTION, ignoreCase = true) ||
				scheme.equals(SCHEME_GEO_ACTION_OFFLINE, ignoreCase = true)
	}

	@JvmStatic
	fun parseAction(uri: Uri?): String {
		if (!isGeoActionUri(uri)) return ""

		val ssp = uri?.schemeSpecificPart?.removePrefix("//?")?.removePrefix("?") ?: return ""
		for (param in ssp.split('&')) {
			if (param.startsWith("act=")) {
				return param.removePrefix("act=").substringBefore('#').trim().lowercase(Locale.US)
			}
		}
		return ""
	}

	@JvmStatic
	@JvmOverloads
	fun executeAction(
		app: OsmandApplication,
		action: String?,
		mapActivity: MapActivity? = null,
		session: NavigationSession? = null
	): Boolean {
		if (action.isNullOrEmpty()) return false
		return when (action) {
			ACTION_EXIT_NAVIGATION -> {
				val navSession = session ?: app.carNavigationSession
				if (navSession != null) {
					navSession.stopNavigation()
				} else {
					mapActivity?.mapActions?.stopNavigationWithoutConfirm() ?: app.stopNavigation()
				}
				true
			}
			ACTION_MUTE -> {
				app.settings.VOICE_MUTE.set(true)
				true
			}
			ACTION_UNMUTE -> {
				app.settings.VOICE_MUTE.set(false)
				true
			}
			ACTION_AVOID_TOLLS -> setAvoidRoutingParameter(app, GeneralRouter.AVOID_TOLL, true)
			ACTION_ALLOW_TOLLS -> setAvoidRoutingParameter(app, GeneralRouter.AVOID_TOLL, false)
			ACTION_AVOID_HIGHWAYS -> setAvoidRoutingParameter(app, GeneralRouter.AVOID_MOTORWAY, true)
			ACTION_ALLOW_HIGHWAYS -> setAvoidRoutingParameter(app, GeneralRouter.AVOID_MOTORWAY, false)
			ACTION_AVOID_FERRIES -> setAvoidRoutingParameter(app, GeneralRouter.AVOID_FERRIES, true)
			ACTION_ALLOW_FERRIES -> setAvoidRoutingParameter(app, GeneralRouter.AVOID_FERRIES, false)
			ACTION_SHOW_ALTERNATES -> {
				// TODO: Integrate with alternative routes UI once implemented (#1294).
				// The core routing engine supports calculating alternatives (HHAlternativeRoutes),
				// but app-level UI and navigation session integration are still pending.
				// Fallback to route details/menu for now.
				val activity = mapActivity ?: app.osmandMap?.mapView?.mapActivity
				if (activity != null) {
					app.runInUIThread {
						ChooseRouteFragment.showInstance(activity.supportFragmentManager, 0, MenuState.FULL_SCREEN)
					}
				}
				true
			}
			ACTION_SHOW_DIRECTIONS_LIST -> {
				val activity = mapActivity ?: app.osmandMap?.mapView?.mapActivity
				if (activity != null) {
					app.runInUIThread {
						ChooseRouteFragment.showInstance(activity.supportFragmentManager, 0, MenuState.FULL_SCREEN)
					}
				}
				true
			}
			ACTION_ROUTE_OVERVIEW -> {
				app.runInUIThread {
					app.osmandMap.fitCurrentRouteToMap(false, 0)
				}
				true
			}
			ACTION_FOLLOW_MODE -> {
				val navSession = session ?: app.carNavigationSession
				app.runInUIThread {
					if (navSession != null) {
						navSession.navigationCarSurface?.handleRecenter()
					} else {
						mapActivity?.mapView?.backToLocation() ?: app.mapViewTrackingUtilities.backToLocationImpl()
					}
				}
				true
			}
			ACTION_GO_BACK -> {
				val navSession = session ?: app.carNavigationSession
				app.runInUIThread {
					if (navSession != null) {
						val screenManager = navSession.carContext.getCarService(ScreenManager::class.java)
						if (screenManager.screenStack.size > 1) {
							screenManager.pop()
						}
					} else {
						mapActivity?.onBackPressedDispatcher?.onBackPressed()
					}
				}
				true
			}
			else -> {
				LOG.warn("Unsupported geo action: $action")
				false
			}
		}
	}

	private fun setAvoidRoutingParameter(app: OsmandApplication, parameterId: String, avoid: Boolean): Boolean {
		val appMode = app.routingHelper.appMode
		app.settings.getCustomRoutingBooleanProperty(parameterId, false).setModeValue(appMode, avoid)
		app.routingHelper.onSettingsChanged(appMode, true)
		return true
	}
}
