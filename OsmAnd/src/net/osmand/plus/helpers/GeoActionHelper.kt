package net.osmand.plus.helpers

import android.net.Uri
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.auto.NavigationSession
import java.util.Locale

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
			else -> {
				LOG.warn("Unsupported geo action: $action")
				false
			}
		}
	}
}
