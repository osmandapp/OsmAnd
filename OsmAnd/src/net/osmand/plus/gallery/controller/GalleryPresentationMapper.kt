package net.osmand.plus.gallery.controller

import android.text.format.DateFormat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.data.MediaMetadataRepository
import net.osmand.plus.gallery.model.GalleryMediaPresentation
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.shared.media.library.MediaFormatting
import net.osmand.shared.media.library.MediaLibrarySortMode
import net.osmand.shared.media.library.MediaLibraryStats
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import java.util.Date

/**
 * Builds display-ready strings for media items from the metadata available in
 * [MediaMetadataRepository]. All gallery text formatting (date, size,
 * duration, stats) lives here, on the controller side; view holders only
 * render the prepared [GalleryMediaPresentation].
 */
class GalleryPresentationMapper(
	private val app: OsmandApplication,
	private val repository: MediaMetadataRepository
) {

	@JvmOverloads
	fun presentation(item: MediaItem, sortMode: GallerySortMode = GallerySortMode.NAME_A_Z, distance: String? = null): GalleryMediaPresentation {
		val metadata = repository.getCached(item)
		val date = (metadata?.creationTimeMs ?: metadata?.lastModifiedTimeMs)?.let {
			DateFormat.getMediumDateFormat(app).format(Date(it))
		}
		val size = metadata?.sizeBytes?.takeIf { it > 0 }?.let {
			AndroidUtils.formatSize(app, it)
		}
		val duration = metadata?.durationMs?.takeIf { item.type == MediaType.AUDIO || item.type == MediaType.VIDEO }?.let { MediaFormatting.duration(it) }
		val description = MediaFormatting.secondLine(MediaLibrarySortMode.valueOf(sortMode.name), date, size, duration, distance)
		return GalleryMediaPresentation(
			description = description,
			durationLabel = duration
		)
	}

	fun statsText(media: List<MediaItem>): String {
		var photos = 0
		var videos = 0
		var audios = 0
		var sizeBytes = 0L
		media.forEach { item ->
			when (item.type) {
				MediaType.PHOTO -> photos++
				MediaType.VIDEO -> videos++
				MediaType.AUDIO -> audios++
				else -> {}
			}
			sizeBytes += repository.getCached(item)?.sizeBytes ?: 0L
		}
		return statsText(MediaLibraryStats(photos, videos, audios, sizeBytes))
	}

	fun statsText(stats: MediaLibraryStats): String {
		val counts = app.getString(R.string.gallery_stats_counts, stats.photos, stats.videos, stats.audios)
		val size = app.getString(
			R.string.gallery_stats_size, AndroidUtils.formatSize(app, stats.bytes)
		)
		return "$counts\n$size"
	}

}
