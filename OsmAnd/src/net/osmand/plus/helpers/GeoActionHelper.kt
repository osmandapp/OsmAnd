package net.osmand.plus.helpers

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.car.app.ScreenManager
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.auto.NavigationSession
import net.osmand.plus.auto.TripUtils
import net.osmand.plus.base.ContextMenuFragment.MenuState
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment
import net.osmand.plus.routing.NextDirectionInfo
import net.osmand.plus.utils.OsmAndFormatter
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

	const val ACTION_ETA = "eta"
	const val ACTION_TIME_TO_DESTINATION = "time_to_destination"
	const val ACTION_DISTANCE_TO_DESTINATION = "distance_to_destination"
	const val ACTION_TIME_TO_NEXT_TURN = "time_to_next_turn"
	const val ACTION_DISTANCE_TO_NEXT_TURN = "distance_to_next_turn"
	const val ACTION_QUERY_NEXT_TURN = "query_next_turn"
	const val ACTION_QUERY_DESTINATION = "query_destination"
	const val ACTION_QUERY_CURRENT_ROAD = "query_current_road"

	const val ACTION_REPORT_CRASH = "report_crash"
	const val ACTION_REPORT_HAZARD = "report_hazard"
	const val ACTION_REPORT_POLICE = "report_police"
	const val ACTION_REPORT_TRAFFIC = "report_traffic"
	const val ACTION_REPORT_ROAD_CLOSURE = "report_road_closure"
	const val ACTION_SHOW_TRAFFIC = "show_traffic"
	const val ACTION_HIDE_TRAFFIC = "hide_traffic"
	const val ACTION_SHOW_SATELLITE = "show_satellite"
	const val ACTION_HIDE_SATELLITE = "hide_satellite"

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
		return executeActionInternal(app, action, mapActivity, session)
	}

	private fun executeActionInternal(
		app: OsmandApplication,
		action: String,
		mapActivity: MapActivity?,
		session: NavigationSession?
	): Boolean {
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
					app.osmandMap?.fitCurrentRouteToMap(false, 0)
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
			ACTION_ETA,
			ACTION_TIME_TO_DESTINATION -> {
				if (!app.routingHelper.isRouteCalculated) {
					showFeedback(app, mapActivity, session, R.string.animate_routing_route_not_calculated)
				} else {
					val eta = OsmAndFormatter.getFormattedTimeShort(app.routingHelper.leftTime.toLong(), true)
					showFeedback(app, mapActivity, session, "${app.getString(R.string.shared_string_eta)}: $eta")
				}
				true
			}
			ACTION_DISTANCE_TO_DESTINATION -> {
				if (!app.routingHelper.isRouteCalculated) {
					showFeedback(app, mapActivity, session, R.string.animate_routing_route_not_calculated)
				} else {
					val distance = OsmAndFormatter.getFormattedDistance(app.routingHelper.leftDistance.toFloat(), app)
					showFeedback(app, mapActivity, session, "${app.getString(R.string.distance)}: $distance")
				}
				true
			}
			ACTION_TIME_TO_NEXT_TURN -> {
				if (!app.routingHelper.isRouteCalculated) {
					showFeedback(app, mapActivity, session, R.string.animate_routing_route_not_calculated)
				} else {
					val time = app.routingHelper.leftTimeNextTurn
					if (time > 0) {
						showFeedback(app, mapActivity, session, OsmAndFormatter.getFormattedDuration(time.toLong(), app))
					} else {
						showFeedback(app, mapActivity, session, R.string.shared_string_none)
					}
				}
				true
			}
			ACTION_DISTANCE_TO_NEXT_TURN -> {
				if (!app.routingHelper.isRouteCalculated) {
					showFeedback(app, mapActivity, session, R.string.animate_routing_route_not_calculated)
				} else {
					val nextInfo = app.routingHelper.getNextRouteDirectionInfo(NextDirectionInfo(), false)
					val dist = nextInfo?.distanceTo ?: 0
					if (dist > 0) {
						showFeedback(app, mapActivity, session, OsmAndFormatter.getFormattedDistance(dist.toFloat(), app))
					} else {
						showFeedback(app, mapActivity, session, R.string.shared_string_none)
					}
				}
				true
			}
			ACTION_QUERY_NEXT_TURN -> {
				if (!app.routingHelper.isRouteCalculated) {
					showFeedback(app, mapActivity, session, R.string.animate_routing_route_not_calculated)
				} else {
					val nextInfo = app.routingHelper.getNextRouteDirectionInfo(NextDirectionInfo(), false)
					val desc = TripUtils.getNextTurnDescription(app, nextInfo, nextInfo?.directionInfo?.turnType, null)
					if (desc.isNotEmpty()) {
						showFeedback(app, mapActivity, session, desc)
					} else {
						showFeedback(app, mapActivity, session, R.string.shared_string_none)
					}
					app.routingHelper.voiceRouter.announceCurrentDirection(app.locationProvider.lastKnownLocation)
				}
				true
			}
			ACTION_QUERY_DESTINATION -> {
				val point = app.targetPointsHelper.pointToNavigate
				if (point != null) {
					val name = point.onlyName
					val dest = name.ifEmpty { app.getString(R.string.route_descr_destination) }
					showFeedback(app, mapActivity, session, "${app.getString(R.string.route_descr_destination)}: $dest")
				} else {
					showFeedback(app, mapActivity, session, R.string.animate_routing_route_not_calculated)
				}
				true
			}
			ACTION_QUERY_CURRENT_ROAD -> {
				val streetName = if (app.routingHelper.isRouteCalculated) {
					app.routingHelper.getCurrentName(NextDirectionInfo(), false).text
				} else {
					null
				}
				val road = if (!streetName.isNullOrEmpty()) {
					streetName
				} else {
					val locale = app.settings.MAP_PREFERRED_LOCALE.get()
					val transliterate = app.settings.MAP_TRANSLITERATE_NAMES.get()
					app.locationProvider.lastKnownRouteSegment?.getName(locale, transliterate)
				}
				if (!road.isNullOrEmpty()) {
					showFeedback(app, mapActivity, session, road)
				} else {
					showFeedback(app, mapActivity, session, R.string.shared_string_none)
				}
				true
			}
			ACTION_REPORT_CRASH,
			ACTION_REPORT_HAZARD,
			ACTION_REPORT_POLICE,
			ACTION_REPORT_TRAFFIC,
			ACTION_REPORT_ROAD_CLOSURE,
			ACTION_SHOW_TRAFFIC,
			ACTION_HIDE_TRAFFIC,
			ACTION_SHOW_SATELLITE,
			ACTION_HIDE_SATELLITE -> {
				showFeedback(app, mapActivity, session, R.string.download_unsupported_action, action)
				false
			}
			else -> {
				LOG.warn("Unsupported geo action: $action")
				showFeedback(app, mapActivity, session, R.string.download_unsupported_action, action)
				false
			}
		}
	}

	private fun showFeedback(
		app: OsmandApplication,
		mapActivity: MapActivity?,
		session: NavigationSession?,
		text: String
	) {
		app.runInUIThread {
			val context: Context = mapActivity ?: app
			Toast.makeText(context, text, Toast.LENGTH_LONG).show()
		}
		val navSession = session ?: app.carNavigationSession
		if (navSession != null) {
			app.toastHelper.showCarToast(text, true)
		}
	}

	private fun showFeedback(
		app: OsmandApplication,
		mapActivity: MapActivity?,
		session: NavigationSession?,
		@StringRes textId: Int,
		vararg args: Any
	) {
		showFeedback(app, mapActivity, session, app.getString(textId, *args))
	}

	private fun setAvoidRoutingParameter(app: OsmandApplication, parameterId: String, avoid: Boolean): Boolean {
		val appMode = app.routingHelper.appMode
		app.settings.getCustomRoutingBooleanProperty(parameterId, false).setModeValue(appMode, avoid)
		app.routingHelper.onSettingsChanged(appMode, true)
		return true
	}
}
