package net.osmand.plus.track.share

import android.graphics.Bitmap
import android.os.SystemClock
import com.squareup.picasso.Picasso
import net.osmand.PlatformUtil
import net.osmand.data.QuadRect
import net.osmand.gpx.flyover.FlyoverPlaceSelector
import net.osmand.gpx.flyover.FlyoverPlaceSelector.Candidate
import net.osmand.gpx.flyover.FlyoverPlaceSelector.Selection
import net.osmand.gpx.flyover.FlyoverTrackGeometry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.PicassoUtils
import net.osmand.util.Algorithms
import kotlin.math.cos

/**
 * Finds popular places along a track through the online Explore Places provider and downloads
 * one photo for each of them. Blocking; call from a background thread.
 */
class FlyoverPlacesLoader(private val app: OsmandApplication) {

	class LoadedPlace(val selection: Selection, val photo: Bitmap)

	fun load(
		geometry: FlyoverTrackGeometry,
		maxPlaces: Int,
		isCancelled: () -> Boolean,
	): List<LoadedPlace> {
		if (!app.settings.isInternetConnectionAvailable) {
			return emptyList()
		}
		val candidates = loadCandidates(geometry, FlyoverPlaceSelector.FAR_CORRIDOR_METERS, isCancelled)
		if (isCancelled()) {
			return emptyList()
		}
		val selected = FlyoverPlaceSelector.selectSpread(geometry, candidates, maxPlaces)
		LOG.info("Flyover places: ${candidates.size} candidates with photos around the track, ${selected.size} selected")
		selected.forEach { LOG.info("Flyover place: ${it.candidate.name} [${it.candidate.kind}] ${it.candidate.imageUrl}") }
		PicassoUtils.getPicasso(app)
		return selected.mapNotNull { selection ->
			if (isCancelled()) {
				return@mapNotNull null
			}
			val photo = try {
				Picasso.get()
					.load(selection.candidate.imageUrl)
					.resize(PHOTO_SIZE, PHOTO_SIZE)
					.centerInside()
					.onlyScaleDown()
					.get()
			} catch (e: Exception) {
				LOG.warn("Could not load photo for ${selection.candidate.name}: ${e.message}")
				null
			}
			photo?.let { LoadedPlace(selection, it) }
		}
	}

	private fun loadCandidates(
		geometry: FlyoverTrackGeometry,
		corridorMeters: Double,
		isCancelled: () -> Boolean,
	): List<Candidate> {
		val provider = app.explorePlacesProvider
		val latPadding = corridorMeters / METERS_PER_DEGREE
		val lonPadding = corridorMeters / (METERS_PER_DEGREE * cos(Math.toRadians(geometry.centerLat)))
		val rect = QuadRect(
			geometry.minLon - lonPadding, geometry.maxLat + latPadding,
			geometry.maxLon + lonPadding, geometry.minLat - latPadding,
		)
		var amenities = provider.getDataCollection(rect, 0)
		val start = SystemClock.uptimeMillis()
		while (provider.isLoading && !isCancelled() && SystemClock.uptimeMillis() - start < LOAD_TIMEOUT_MS) {
			Thread.sleep(100)
		}
		if (provider.isLoading && !isCancelled()) {
			LOG.warn("Explore places did not finish loading in time, using what has arrived")
		}
		amenities = provider.getDataCollection(rect, 0)
		LOG.info("Flyover places: ${amenities.size} explore places loaded for the track area")

		val lang = app.settings.MAP_PREFERRED_LOCALE.get()
		val transliterate = app.settings.MAP_TRANSLITERATE_NAMES.get()
		return amenities.mapNotNull { amenity ->
			val location = amenity.location ?: return@mapNotNull null
			val imageUrl = amenity.wikiPhoto?.takeIf { !Algorithms.isEmpty(it) } ?: amenity.wikiImageStubUrl
			if (Algorithms.isEmpty(imageUrl)) {
				return@mapNotNull null
			}
			Candidate(location.latitude, location.longitude, amenity.travelEloNumber,
				amenity.getName(lang, transliterate), imageUrl, "${amenity.type?.keyName}/${amenity.subType}")
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(FlyoverPlacesLoader::class.java)
		private const val METERS_PER_DEGREE = 111_320.0
		private const val LOAD_TIMEOUT_MS = 15_000L
		private const val PHOTO_SIZE = 900
	}
}
