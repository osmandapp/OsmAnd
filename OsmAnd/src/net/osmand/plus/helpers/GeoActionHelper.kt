package net.osmand.plus.helpers

import android.net.Uri
import androidx.annotation.StringRes
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.auto.NavigationSession

object GeoActionHelper {

	private val LOG = PlatformUtil.getLog(GeoActionHelper::class.java)

	const val SCHEME_GEO_ACTION = "geo.action"
	const val SCHEME_GEO_ACTION_OFFLINE = "geo.action.offline"

	const val ACTION_EXIT_NAVIGATION = "exit_navigation"
	const val ACTION_MUTE = "mute"
	const val ACTION_UNMUTE = "unmute"

	@JvmStatic
	fun isGeoActionUri(uri: Uri?): Boolean {
		val scheme = uri?.scheme ?: return false
		return scheme.equals(SCHEME_GEO_ACTION, ignoreCase = true) ||
				scheme.equals(SCHEME_GEO_ACTION_OFFLINE, ignoreCase = true)
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
					val mapActions = mapActivity?.mapActions
					if (mapActions != null) {
						mapActions.stopNavigationWithoutConfirm()
					} else {
						app.stopNavigation()
					}
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
			else -> {
				LOG.warn("Unsupported geo action: $action")
				false
			}
		}
	}

	private fun showFeedback(
		app: OsmandApplication,
		text: String
	) {
		app.toastHelper.showToast(text, true)
	}

	private fun showFeedback(
		app: OsmandApplication,
		@StringRes textId: Int,
		vararg args: Any
	) {
		app.toastHelper.showToast(textId, true, *args)
	}
}
