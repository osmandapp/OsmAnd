package net.osmand.plus.download.freeregion

import android.content.Context
import android.content.SharedPreferences
import net.osmand.IndexConstants
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.map.WorldRegion
import net.osmand.plus.OsmandApplication
import net.osmand.plus.Version
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.IndexItem
import net.osmand.plus.download.local.LocalItem
import net.osmand.plus.download.local.LocalItemType
import net.osmand.plus.download.local.LocalItemUtils
import net.osmand.plus.helpers.FileNameTranslationHelper
import net.osmand.plus.inapp.InstallReferrer
import net.osmand.plus.utils.AndroidNetworkUtils
import net.osmand.util.Algorithms
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Map downloads of the free version: all maps of one region - the free region - and their updates are
 * free.
 *
 * The free region is a country - the region right under a continent, so Germany with all its Länder
 * is one region. In the big federations it is a first-level region (a US state), otherwise one free
 * region would be tens of GB.
 *
 * Only new downloads of standard and roads-only maps are counted; updates of maps on the device are
 * always free. The world overview map, seamarks and the maps of plugins - contour lines, terrain,
 * depth, Wikipedia, travel guides - are never counted and never deleted when the free region is
 * replaced. A map of another region needs a replace, which the server allows by the number of
 * replaces already made and whether the user has left an email, see [requestReplace].
 *
 * The free region and the replaces are kept in their own preferences file, not in OsmandSettings:
 * the Android backup and the OsmAnd Cloud export must not bring them back after a reinstall.
 */
class FreeVersionMapDownloads(private val app: OsmandApplication) {

	private val preferences: SharedPreferences =
		app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

	val isFreeVersion: Boolean
		get() = !Version.isPaidVersion(app)

	/**
	 * The region tree is loaded in the background on app start. Before that no map has a region and
	 * the stored free region can't be read, so nothing is decided and the free region is never
	 * overwritten.
	 */
	private val isRegionsLoaded: Boolean
		get() = app.regions.isInitialized

	/** Replaces this device has already made; the first map is not a replace. */
	val replacesCount: Int
		get() = preferences.getInt(REPLACES, 0)

	@get:Synchronized
	val freeRegion: WorldRegion?
		get() {
			migrateIfNeeded()
			val id = preferences.getString(REGION, null)
			return if (id.isNullOrEmpty()) null else app.regions.getRegionData(id)
		}

	/** The start screen never asks: the region of its map becomes the free region. */
	@Synchronized
	fun chooseFreeRegion(item: IndexItem) {
		val region = getItemRegion(item)
		if (isFreeVersion && region != null) {
			storeFreeRegion(region)
		}
	}

	/** The server allowed a replace: the region becomes free and the replace is counted. */
	@Synchronized
	fun replaceFreeRegion(region: WorldRegion) {
		preferences.edit()
			.putString(REGION, region.regionId)
			.putInt(REPLACES, replacesCount + 1)
			.apply()
	}

	/**
	 * Splits a download request into the maps free now and the regions that need a replace. Before the
	 * first map the region of the first counted item is the free one - it is stored only when its
	 * download starts, see [canDownload].
	 */
	fun check(items: Array<IndexItem>): FreeRegionCheck {
		if (!isFreeVersion || !isRegionsLoaded) {
			return FreeRegionCheck(null, items.toList(), emptySet())
		}
		var freeRegion = freeRegion
		val freeItems = mutableListOf<IndexItem>()
		val otherRegions = linkedSetOf<WorldRegion>()
		for (item in items) {
			val region = getItemRegion(item)
			if (freeRegion == null) {
				freeRegion = region
			}
			if (region == null || region == freeRegion || item.isDownloaded) {
				freeItems.add(item)
			} else {
				otherRegions.add(region)
			}
		}
		return FreeRegionCheck(freeRegion, freeItems, otherRegions)
	}

	/**
	 * The last check, right before the download. The first counted map makes its region the free
	 * one, without a dialog.
	 */
	@Synchronized
	fun canDownload(item: IndexItem): Boolean {
		if (!isFreeVersion || !isRegionsLoaded) {
			return true
		}
		val region = getItemRegion(item)
		if (region == null || item.isDownloaded) {
			return true
		}
		val freeRegion = freeRegion
		if (freeRegion == null) {
			storeFreeRegion(region)
			return true
		}
		return region == freeRegion
	}

	/**
	 * Asks the server whether the free region may be replaced. A GET request, so that the parameters -
	 * package, version, nd, ns, aid, utm* - land in the server access log.
	 *
	 * @param onAnswer gets null when the server could not be reached
	 */
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
		params["replaces"] = replacesCount.toString()
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

	/** Maps deleted when the free region is replaced by [newRegion]: maps of every other region. */
	fun collectMapsToDelete(newRegion: WorldRegion): Map<WorldRegion, List<LocalItem>> {
		return collectLocalMaps() - newRegion
	}

	/**
	 * @return the region the item counts against, or null when the item is free for everyone (world
	 * maps, plugin maps, voice, fonts...)
	 */
	private fun getItemRegion(item: IndexItem): WorldRegion? {
		return if (DownloadActivityType.isCountedInDownloads(item)) getRegionByDownloadName(item.basename) else null
	}

	private fun getRegionByDownloadName(downloadName: String): WorldRegion? {
		val name = downloadName.lowercase()
		if (FileNameTranslationHelper.getStandardMapName(app, name) != null) {
			return null
		}
		return app.regions.getRegionDataByDownloadName(name)?.let { getFreeRegionOf(it) }
	}

	/** The country of the region, or its first-level region in a federation. */
	private fun getFreeRegionOf(region: WorldRegion): WorldRegion? {
		val country = region.countryRegion
		if (country != null && country.regionId in FEDERATIONS && country != region) {
			var state = region
			while (state.superregion != null && state.superregion != country) {
				state = state.superregion
			}
			return state
		}
		return country
	}

	/**
	 * Standard and roads-only map files on the device, by region. Files of no region (world maps,
	 * custom maps) are left out.
	 */
	private fun collectLocalMaps(): Map<WorldRegion, List<LocalItem>> {
		val result = linkedMapOf<WorldRegion, MutableList<LocalItem>>()
		for (dirName in arrayOf(IndexConstants.MAPS_PATH, IndexConstants.ROADS_INDEX_DIR)) {
			val files = app.getAppPath(dirName).listFiles() ?: continue
			for (file in files) {
				if (!file.isFile) {
					continue
				}
				val type = LocalItemUtils.getItemType(app, file)
				if (type != LocalItemType.MAP_DATA && type != LocalItemType.ROAD_DATA) {
					continue
				}
				val region = getRegionByDownloadName(getDownloadName(file.name)) ?: continue
				result.getOrPut(region) { mutableListOf() }.add(LocalItem(file, type))
			}
		}
		return result
	}

	private fun storeFreeRegion(region: WorldRegion) {
		preferences.edit().putString(REGION, region.regionId).apply()
	}

	/**
	 * The old free version counted 7 downloads of any region. An existing user gets the region of
	 * their current location if they have its maps, otherwise the region with the most map data.
	 * Everything already downloaded stays and keeps free updates; a wrong choice is fixed by a replace.
	 */
	private fun migrateIfNeeded() {
		if (preferences.getBoolean(MIGRATED, false) || !isFreeVersion || !isRegionsLoaded) {
			return
		}
		preferences.edit().putBoolean(MIGRATED, true).apply()
		if (!preferences.getString(REGION, null).isNullOrEmpty()) {
			return
		}
		val maps = collectLocalMaps()
		val region = getLocationRegion(maps.keys)
			?: maps.maxByOrNull { (_, items) -> items.sumOf { it.size } }?.key
		if (region != null) {
			storeFreeRegion(region)
			LOG.info("Free region migrated: ${region.regionId} of ${maps.size} regions")
		}
	}

	private fun getLocationRegion(candidates: Set<WorldRegion>): WorldRegion? {
		val location = app.locationProvider.lastKnownLocation
		if (location == null || candidates.isEmpty()) {
			return null
		}
		try {
			return app.regions.getWorldRegionsAt(LatLon(location.latitude, location.longitude))
				.firstNotNullOfOrNull { region -> getFreeRegionOf(region)?.takeIf { it in candidates } }
		} catch (e: IOException) {
			LOG.error(e)
		}
		return null
	}

	companion object {
		private val LOG = PlatformUtil.getLog(FreeVersionMapDownloads::class.java)

		private val FEDERATIONS = setOf(
			"northamerica_us", "northamerica_canada", "russia", "southamerica_brazil",
			"asia_india", "asia_china", "australia-oceania-all_australia-oceania")

		private const val REPLACE_URL = "https://osmand.net/api/replace-download-map"
		private const val REGISTER_EMAIL_URL = "https://osmand.net/subscription/register_email"

		private const val PREFERENCES_NAME = "net.osmand.free_region"
		private const val REGION = "region"
		private const val MIGRATED = "migrated"
		private const val REPLACES = "replaces"

		/** "Ukraine_kyiv-city_europe_2.obf" -> "ukraine_kyiv-city_europe", the download name of its region. */
		private fun getDownloadName(fileName: String): String {
			val extension = fileName.indexOf('.')
			val basename = if (extension > 0) fileName.substring(0, extension) else fileName
			return Algorithms.removeFileVersionSuffix(basename).lowercase()
		}
	}
}

/** What a download request may get now, see [FreeVersionMapDownloads.check]. */
class FreeRegionCheck(
	val freeRegion: WorldRegion?,
	val freeItems: List<IndexItem>,
	/** Regions of the maps that need a replace of the free region, in the order of the request. */
	val otherRegions: Set<WorldRegion>
) {
	val isAllFree: Boolean
		get() = otherRegions.isEmpty()
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
