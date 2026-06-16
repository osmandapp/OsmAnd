package net.osmand.plus.gallery.data

import android.graphics.Bitmap
import net.osmand.data.LatLon
import net.osmand.shared.media.domain.MediaItem

/**
 * Decoded metadata of a media item used by the gallery UI.
 *
 * All fields are optional: the current temporary implementation fills what it
 * can read locally (size, date, duration), while [latLon], [heading] and
 * [posterUri] are reserved for the future backend (Nearest sorting, heading
 * metadata, backend-provided poster thumbnails).
 */
data class GalleryMediaMetadata(
	val sizeBytes: Long? = null,
	val dateMillis: Long? = null,
	val durationMs: Long? = null,
	val latLon: LatLon? = null,
	val heading: Float? = null,
	val posterUri: String? = null
)

interface MediaMetadataRepository {

	fun getCached(item: MediaItem): GalleryMediaMetadata?
	fun request(items: List<MediaItem>, listener: MediaMetadataListener): Cancellable
}

interface MediaMetadataListener {
	fun onMetadataLoaded(item: MediaItem, metadata: GalleryMediaMetadata)
	fun onBatchFinished()
}

interface MediaPosterLoader {
	fun loadPoster(item: MediaItem, callback: (Bitmap?) -> Unit)
}


interface MediaSourceResolver {

	fun getPlaybackUri(item: MediaItem): android.net.Uri?

	fun getShareableUri(item: MediaItem): android.net.Uri?
}

fun interface Cancellable {
	fun cancel()
}
