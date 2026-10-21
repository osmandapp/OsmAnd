package net.osmand.plus.gallery.library

import net.osmand.data.LatLon
import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.plugins.audionotes.Recording
import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.library.SortableMedia

data class MediaLibraryEntry(
	val mediaItem: MediaItem,
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
	override val location get() = metadata?.latLon?.let { KLatLon(it.latitude, it.longitude) }
		?: recording?.let { KLatLon(it.latitude, it.longitude) }
}

data class MediaAttachment(
	val target: Linkable,
	val link: Link,
	val kind: Kind,
	val name: String,
	val latLon: LatLon,
	val trackFile: String? = null
) {
	enum class Kind { FAVORITE, TRACK_POINT }
}
