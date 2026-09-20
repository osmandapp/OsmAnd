package net.osmand.shared.travel

import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.data.KLatLon

/**
 * What [TravelObfHelper] needs from the platform: which obf files to search, and where the user is.
 *
 * In android this is `ResourceManager` plus a few settings; deciding which files are open, which
 * ones the user has switched off and in what order they are consulted stays there, because it
 * depends on downloads, live updates and map settings that shared knows nothing about.
 */
interface TravelObfContext {

	/** Wikivoyage files - `.travel.obf` - newest first, as `getWikivoyageRepositories` returns them. */
	fun getWikivoyageRepositories(): List<AmenityIndexRepository>

	/**
	 * Every file that may hold gpx tracks, in Z-A order. Android returns the same list it uses for
	 * amenities: OSM routes and gpx collections live in ordinary map files, not only travel ones.
	 */
	fun getTravelGpxRepositories(): List<AmenityIndexRepository>

	/** The language articles are preferred in, e.g. "en", "de". */
	fun getLanguage(): String

	/** Where the map is looking, which is where the offered articles are searched around. */
	fun getMapLocation(): KLatLon?

	/** Written as the author of a built gpx file. */
	fun getAppVersion(): String
}
