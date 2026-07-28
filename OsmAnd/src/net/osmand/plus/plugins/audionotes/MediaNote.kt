package net.osmand.plus.plugins.audionotes

import android.content.Context
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.data.MediaMetadataRepository
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.util.Algorithms

class MediaNote private constructor(
	val recording: Recording?,
	val mediaItem: MediaItem?,
	val target: Linkable?,
	val link: Link?,
	val latitude: Double,
	val longitude: Double
) {

	fun isRecording(): Boolean = recording != null

	fun isAttachedMedia(): Boolean = mediaItem != null && target != null && link != null

	fun isPhoto(): Boolean = recording?.isPhoto ?: mediaItem?.type == MediaType.PHOTO

	fun isVideo(): Boolean = recording?.isVideo ?: mediaItem?.type == MediaType.VIDEO

	fun isAudio(): Boolean = recording?.isAudio ?: mediaItem?.type == MediaType.AUDIO

	fun getLastModified(repository: MediaMetadataRepository): Long {
		return recording?.lastModified ?: mediaItem?.let { repository.getCached(it)?.dateMillis } ?: 0L
	}

	fun getName(context: Context, repository: MediaMetadataRepository, includingType: Boolean): String {
		recording?.let { return it.getName(context, includingType) }

		val type = getType(context)
		val title = mediaItem?.title?.trim().orEmpty()
		val date = getLastModified(repository).takeIf { it > 0 }
		val genericTitle = title.equals(type, ignoreCase = true) || title.equals(mediaItem?.type?.typeName, ignoreCase = true)
		if (title.isNotEmpty() && !genericTitle) {
			return title
		}
		if (date != null) {
			val formattedDate = Recording.formatDateTime(context, date)
			return if (includingType) "$type $formattedDate" else formattedDate
		}
		return title.ifEmpty { type }
	}

	fun getType(context: Context): String {
		recording?.let { return it.getType(context) }
		return when (mediaItem?.type) {
			MediaType.PHOTO -> context.getString(R.string.shared_string_photo)
			MediaType.VIDEO -> context.getString(R.string.shared_string_video)
			MediaType.AUDIO -> context.getString(R.string.shared_string_audio)
			else -> context.getString(R.string.shared_string_media)
		}
	}

	fun getTypeWithDuration(app: OsmandApplication, repository: MediaMetadataRepository): String {
		recording?.let { return it.getTypeWithDuration(app) }
		val type = getType(app)
		val duration = mediaItem?.let { repository.getCached(it)?.durationMs }?.takeIf { it > 0 } ?: return type
		return "$type, ${Algorithms.formatDuration((duration / 1000).toInt(), app.accessibilityEnabled())}"
	}

	fun getSearchHistoryType(): String {
		recording?.let { return it.searchHistoryType }
		return when (mediaItem?.type) {
			MediaType.VIDEO -> PointDescription.POINT_TYPE_VIDEO_NOTE
			else -> PointDescription.POINT_TYPE_PHOTO_NOTE
		}
	}

	fun getMimeType(): String {
		return when {
			isPhoto() -> "image/*"
			isVideo() -> "video/*"
			isAudio() -> "audio/*"
			else -> "*/*"
		}
	}

	companion object {

		@JvmStatic
		fun fromRecording(recording: Recording): MediaNote = MediaNote(
			recording = recording,
			mediaItem = null,
			target = null,
			link = null,
			latitude = recording.latitude,
			longitude = recording.longitude
		)

		@JvmStatic
		fun fromAttachedMedia(
			mediaItem: MediaItem,
			target: Linkable,
			link: Link,
			latitude: Double,
			longitude: Double
		): MediaNote = MediaNote(
			recording = null,
			mediaItem = mediaItem,
			target = target,
			link = link,
			latitude = latitude,
			longitude = longitude
		)

		@JvmStatic
		fun createShareLocationItem(): MediaNote = MediaNote(
			recording = null,
			mediaItem = null,
			target = null,
			link = null,
			latitude = Double.NaN,
			longitude = Double.NaN
		)
	}
}