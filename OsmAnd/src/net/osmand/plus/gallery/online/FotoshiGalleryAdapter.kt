package net.osmand.plus.gallery.online

import net.osmand.PlatformUtil
import net.osmand.shared.media.FotoshiMediaFactory
import java.net.HttpURLConnection
import java.net.URL

/** Exact-object lookup using a public, server-restricted OsmAnd integration credential. */
object FotoshiGalleryAdapter {
	const val OSM_ID_PARAM = "fotoshi_osm_id"
	// Public mobile-client identifier, not a secret. Restricted to exact OSM photo lookups.
	// Quotas, permissions and revocation are enforced by Fotoshi on the server.
	private const val INTEGRATION_KEY = "fshi_DpDaC-GZxK7UlVJ6Lv3w5kdVmHoHLg5NFCcLz45YPFE"
	private val log = PlatformUtil.getLog(FotoshiGalleryAdapter::class.java)

	@JvmStatic
	fun isEnabled(osmId: String?): Boolean = FotoshiMediaFactory.requestUrl(osmId) != null

	@JvmStatic
	fun load(osmId: String?, holder: OnlinePhotosHolder) {
		val url = FotoshiMediaFactory.requestUrl(osmId) ?: return
		var connection: HttpURLConnection? = null
		try {
			connection = URL(url).openConnection() as HttpURLConnection
			connection.connectTimeout = 5000
			connection.readTimeout = 5000
			connection.instanceFollowRedirects = false
			connection.setRequestProperty("Authorization", "Bearer $INTEGRATION_KEY")
			connection.setRequestProperty("User-Agent", "OsmAnd-Fotoshi/1.0")
			connection.setRequestProperty("Accept", "application/geo+json, application/json")
			if (connection.responseCode != 200) {
				holder.markIncomplete()
				return
			}
			val response = connection.inputStream.bufferedReader().use { it.readText() }
			for (item in FotoshiMediaFactory.fromResponse(response)) {
				holder.addItem(OnlinePhotosGroup.OTHER, item)
			}
		} catch (_: Exception) {
			// An optional provider must not prevent other gallery sources from loading.
			// Do not log HTTP bodies, request headers or exceptions containing credentials.
			holder.markIncomplete()
			log.warn("Fotoshi photos unavailable")
		} finally {
			connection?.disconnect()
		}
	}
}
