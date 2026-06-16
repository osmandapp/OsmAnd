package net.osmand.plus.gallery.controller

import android.text.format.DateFormat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.data.MediaMetadataRepository
import net.osmand.plus.gallery.model.GalleryMediaPresentation
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.util.Algorithms
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

	fun presentation(item: MediaItem): GalleryMediaPresentation {
		val metadata = repository.getCached(item)
		val date = metadata?.dateMillis?.let {
			DateFormat.getMediumDateFormat(app).format(Date(it))
		}
		val size = metadata?.sizeBytes?.takeIf { it > 0 }?.let {
			AndroidUtils.formatSize(app, it)
		}
		val duration = metadata?.durationMs?.let { formatDuration(it) }
		val description = listOfNotNull(date, size, duration)
			.joinToString(separator = " • ")
		return GalleryMediaPresentation(
			description = description.takeIf { it.isNotEmpty() },
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
		val counts = app.getString(R.string.gallery_stats_counts, photos, videos, audios)
		val size = app.getString(
			R.string.gallery_stats_size, AndroidUtils.formatSize(app, sizeBytes)
		)
		return "$counts\n$size"
	}

	private fun formatDuration(durationMs: Long): String =
		Algorithms.formatDuration((durationMs / 1000).toInt(), app.accessibilityEnabled())
}
