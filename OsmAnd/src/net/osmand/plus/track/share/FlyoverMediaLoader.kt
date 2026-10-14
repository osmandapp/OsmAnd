package net.osmand.plus.track.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.gpx.flyover.FlyoverPlaceSelector
import net.osmand.gpx.flyover.FlyoverPlaceSelector.Candidate
import net.osmand.gpx.flyover.FlyoverPlaceSelector.Selection
import net.osmand.gpx.flyover.FlyoverTrackGeometry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.library.MediaAttachment
import net.osmand.plus.gallery.library.MediaLibraryEntry
import net.osmand.plus.gallery.library.MediaLibraryScanner
import net.osmand.plus.media.MediaMetadataUtils
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import java.io.File

/**
 * Finds the photos of OsmAnd's media library (photo notes, photos attached to favorites and to track
 * points: what My Places shows under Media) taken along a track, within 100 m of it, and loads them:
 * for the flyover video they come before the popular places, and they need no internet. Blocking;
 * call from a background thread.
 */
class FlyoverMediaLoader(private val app: OsmandApplication) {

	class LoadedPhoto(val selection: Selection, val photo: Bitmap)

	private class Photo(val file: File, val rotation: Int)

	/**
	 * @param startTime the track's first time, milliseconds, 0 when the track has no times
	 * @param endTime the track's last time, milliseconds
	 */
	fun load(geometry: FlyoverTrackGeometry, startTime: Long, endTime: Long, maxPhotos: Int, isCancelled: () -> Boolean): List<LoadedPhoto> {
		if (maxPhotos <= 0) {
			return emptyList()
		}
		val entries = MediaLibraryScanner(app, PluginsHelper.getPlugin(AudioVideoNotesPlugin::class.java)).scan()
		val photos = HashMap<String, Photo>()
		val candidates = ArrayList<Candidate>()
		val titles = HashMap<String, Int>()
		for (entry in entries) {
			if (entry.type != MediaType.PHOTO) {
				continue
			}
			val file = fileOf(entry) ?: continue
			val location = locationOf(entry, file) ?: continue
			if (geometry.project(location.latitude, location.longitude)[1] > FlyoverPlaceSelector.NEAR_CORRIDOR_METERS) {
				continue
			}
			// the card's title: the favorite the photo belongs to, or the note's name (its time unless named)
			var title = titleOf(entry, file)
			val same = titles[title] ?: 0
			titles[title] = same + 1
			if (same > 0) {
				title += " (${same + 1})"
			}
			// photos taken during the track come first
			val taken = file.lastModified()
			val rank = if (startTime > 0 && taken in startTime..endTime) 2 else 1
			photos[file.absolutePath] = Photo(file, entry.recording?.bitmapRotation ?: 0)
			candidates.add(Candidate(location.latitude, location.longitude, rank, title, file.absolutePath, "photo"))
		}
		if (isCancelled()) {
			return emptyList()
		}
		val selected = FlyoverPlaceSelector.selectSpread(geometry, candidates, maxPhotos)
		LOG.info("Flyover photos: ${entries.size} media, ${candidates.size} photos within ${FlyoverPlaceSelector.NEAR_CORRIDOR_METERS.toInt()} m of the track, ${selected.size} taken")
		return selected.mapNotNull { selection ->
			if (isCancelled()) {
				return@mapNotNull null
			}
			val photo = photos[selection.candidate.imageUrl] ?: return@mapNotNull null
			loadPhoto(photo)?.let { LoadedPhoto(selection, it) }
		}
	}

	/** The photo's file on the phone: an internal one in OsmAnd's media folders, or a file link. */
	private fun fileOf(entry: MediaLibraryEntry): File? {
		val item = entry.mediaItem
		val file = when {
			item is MediaItem.Internal -> MediaProvider.resolveInternalMediaFile(app.getAppPath(null).absolutePath, item.relativePath)
			item.mediaUri.startsWith("file:") -> runCatching { File(Uri.parse(item.mediaUri).path ?: "") }.getOrNull()
			item.mediaUri.startsWith("/") -> File(item.mediaUri)
			else -> null
		}
		return file?.takeIf { it.isFile && FlyoverPlaceSelector.isPhotograph(it.name) }
	}

	/** Where the photo was taken: the note's location, the photo's own, or the point it is attached to. */
	private fun locationOf(entry: MediaLibraryEntry, file: File): LatLon? {
		entry.recording?.let { return LatLon(it.latitude, it.longitude) }
		MediaMetadataUtils.getLocation(file)?.let { return LatLon(it.latitude, it.longitude) }
		return entry.attachments.firstOrNull()?.latLon
	}

	private fun titleOf(entry: MediaLibraryEntry, file: File): String {
		entry.attachments.firstOrNull { it.kind == MediaAttachment.Kind.FAVORITE }?.let { return it.name }
		entry.recording?.let { return it.getName(app, false) }
		return file.nameWithoutExtension
	}

	/** The photo scaled to fit the card, the right way up. */
	private fun loadPhoto(photo: Photo): Bitmap? = try {
		val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		BitmapFactory.decodeFile(photo.file.path, bounds)
		var sample = 1
		while (bounds.outWidth / sample > PHOTO_SIZE * 2 || bounds.outHeight / sample > PHOTO_SIZE * 2) {
			sample *= 2
		}
		val bitmap = BitmapFactory.decodeFile(photo.file.path, BitmapFactory.Options().apply { inSampleSize = sample })
		val rotation = if (photo.rotation != 0) photo.rotation else exifRotation(photo.file)
		if (bitmap == null || rotation == 0) bitmap
		else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
	} catch (e: Exception) {
		LOG.warn("Could not load the photo ${photo.file}: ${e.message}")
		null
	}

	private fun exifRotation(file: File): Int = when (MediaMetadataUtils.getExifOrientation(file)) {
		3 -> 180
		6 -> 90
		8 -> 270
		else -> 0
	}

	companion object {
		private val LOG = PlatformUtil.getLog(FlyoverMediaLoader::class.java)
		private const val PHOTO_SIZE = 900
	}
}
