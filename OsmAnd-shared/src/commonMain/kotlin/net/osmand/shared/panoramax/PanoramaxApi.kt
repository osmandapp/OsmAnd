package net.osmand.shared.panoramax

import kotlin.jvm.JvmStatic

/** Panoramax URLs shared by the gallery, tile source, and plugin. */
object PanoramaxApi {
	const val INSTANCE_URL = "https://api.panoramax.xyz/"
	const val API_URL = INSTANCE_URL + "api/"

	@JvmStatic
	fun getSearchUrl(ids: List<String>): String =
		API_URL + "search?ids=" + ids.joinToString(",") + "&limit=" + ids.size

	/** The caller URL-encodes the query before passing it here. */
	@JvmStatic
	fun getUserSearchUrl(encodedQuery: String): String = "${API_URL}users/search?q=$encodedQuery"

	@JvmStatic
	fun getViewerUrl(id: String): String = "$INSTANCE_URL#focus=pic&pic=$id"
}
