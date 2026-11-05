package net.osmand.plus.download.freeregion

import net.osmand.PlatformUtil
import net.osmand.map.WorldRegion
import net.osmand.plus.OsmandApplication
import net.osmand.plus.Version
import net.osmand.plus.inapp.InstallReferrer
import net.osmand.plus.utils.AndroidNetworkUtils
import org.json.JSONException
import org.json.JSONObject

/**
 * The server decides whether the free region may be replaced: the app sends how many replaces it has
 * already made and whether the user has left an email. A GET request, so that the parameters -
 * package, version, nd, ns, aid, utm* - land in the server access log.
 */
class FreeRegionApi(private val app: OsmandApplication) {

	/** @param answer null when the server could not be reached */
	fun requestReplace(from: WorldRegion, to: WorldRegion, onAnswer: (answer: ReplaceAnswer?) -> Unit) {
		val initializer = app.appInitializer
		val params = linkedMapOf(
			"package" to app.packageName,
			"version" to Version.getFullVersion(app),
			"lang" to app.language,
			"nd" to initializer.firstInstalledDays.toString(),
			"ns" to initializer.numberOfStarts.toString())
		if (app.isUserAndroidIdAllowed) {
			params["aid"] = app.userAndroidId
		}
		InstallReferrer.addParams(app, params)
		params["from"] = from.regionId
		params["to"] = to.regionId
		params["replaces"] = app.downloadThread.freeVersionMaps.replacesCount.toString()
		params["email"] = app.settings.EMAIL_SUBSCRIBED.get().toString()

		AndroidNetworkUtils.sendRequestAsync(app, REPLACE_URL, params, "Requesting free region replace...",
			false, false) { result, _, _ -> onAnswer(ReplaceAnswer.parse(result)) }
	}

	/** Subscribes the email to OsmAnd news; the server answers with the registered address. */
	fun registerEmail(email: String, onRegistered: (registered: Boolean) -> Unit) {
		val params = mutableMapOf("email" to email)
		if (app.isUserAndroidIdAllowed) {
			params["aid"] = app.userAndroidId
		}
		AndroidNetworkUtils.sendRequestAsync(app, REGISTER_EMAIL_URL, params, "Subscribing email...",
			false, true) { result, _, _ ->
			val registered = isEmailRegistered(email, result)
			if (registered) {
				app.settings.EMAIL_SUBSCRIBED.set(true)
			}
			onRegistered(registered)
		}
	}

	private fun isEmailRegistered(email: String, response: String?): Boolean {
		if (response == null) {
			return false
		}
		return try {
			email.equals(JSONObject(response).optString("email"), ignoreCase = true)
		} catch (e: JSONException) {
			LOG.error("Register email response: $response", e)
			false
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(FreeRegionApi::class.java)

		private const val REPLACE_URL = "https://osmand.net/api/replace-download-map"
		private const val REGISTER_EMAIL_URL = "https://osmand.net/subscription/register_email"
	}
}

enum class ReplaceAnswer(private val status: String) {
	/** Replace now. */
	OK("ok"),

	/** One more replace for an email. */
	REQUEST_EMAIL("request_email"),

	/** No more replaces - only Maps+. */
	NO("no");

	companion object {
		private val LOG = PlatformUtil.getLog(ReplaceAnswer::class.java)

		/** @return null when the response can't be read, [NO] for a status unknown to this version */
		fun parse(response: String?): ReplaceAnswer? {
			if (response == null) {
				return null
			}
			return try {
				val status = JSONObject(response).optString("status")
				entries.firstOrNull { it.status.equals(status, ignoreCase = true) } ?: NO
			} catch (e: JSONException) {
				LOG.error("Replace response: $response", e)
				null
			}
		}
	}
}
