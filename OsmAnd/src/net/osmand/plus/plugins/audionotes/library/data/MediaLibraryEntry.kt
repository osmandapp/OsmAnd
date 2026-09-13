package net.osmand.plus.plugins.audionotes.library.data

import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.plugins.audionotes.Recording
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.library.SortableMedia

data class MediaLibraryEntry(
	val mediaItem: MediaItem,
	val key: String,
	val recording: Recording? = null,
	val attachments: List<MediaAttachment> = emptyList(),
	val metadata: GalleryMediaMetadata? = null
) : SortableMedia {
	val href: String get() = (mediaItem as? MediaItem.Internal)?.let { LinkMediaFactory.createInternalUri(it.relativePath) }
		?: mediaItem.mediaUri
	override val id get() = mediaItem.id
	override val title get() = mediaItem.title
	override val type get() = mediaItem.type
	override val dateMs get() = metadata?.creationTimeMs
	override val lastModifiedMs get() = metadata?.lastModifiedTimeMs
	override val sizeBytes get() = metadata?.sizeBytes
	override val durationMs get() = metadata?.durationMs
	override val lat get() = metadata?.latLon?.latitude ?: recording?.latitude
	override val lon get() = metadata?.latLon?.longitude ?: recording?.longitude
}
