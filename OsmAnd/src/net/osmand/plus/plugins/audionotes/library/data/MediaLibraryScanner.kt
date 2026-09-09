package net.osmand.plus.plugins.audionotes.library.data

import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.settings.mediastorage.MediaStorageHelper
import net.osmand.plus.settings.mediastorage.MediaStorageLocation
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.domain.MediaItem

/** Storage enumeration and attachment collection run only on the repository executor. */
class MediaLibraryScanner(private val app: OsmandApplication, private val plugin: AudioVideoNotesPlugin?) {
	fun scan(): List<MediaLibraryEntry> {
		val storage = MediaStorageHelper(app)
		val entries = linkedMapOf<String, MediaLibraryEntry>()
		fun add(link: Link, attachment: MediaAttachment? = null) {
			val key = MediaLibraryKey.of(link, app.getAppPath().absolutePath) ?: return
			val item = LinkMediaFactory.fromLinks(listOf(link)).firstOrNull() ?: return
			val existing = entries[key]
			if (existing == null) {
				entries[key] = MediaLibraryEntry(item, key, attachments = listOfNotNull(attachment))
			} else if (attachment != null) {
				entries[key] = existing.copy(attachments = existing.attachments + attachment)
			}
		}
		for (source in storage.listMedia(MediaStorageLocation.fromSettings(app))) {
			add(Link(source.href).apply { text = source.fileName })
		}
		for (recording in plugin?.allRecordings.orEmpty().toList()) {
			val link = Link(storage.createMediaFileHref(recording.file)).apply { text = recording.file.name }
			add(link)
			MediaLibraryKey.of(link, app.getAppPath().absolutePath)?.let { key -> entries[key]?.let { entries[key] = it.copy(recording = recording) } }
		}
		fun addTarget(target: Linkable, kind: MediaAttachment.Kind, name: String, latLon: LatLon, path: String? = null) {
			for (original in target.links.orEmpty().toList()) {
				val link = Link(original)
				if (link.text.isNullOrBlank()) {
					link.text = link.href?.substringBefore('?')?.substringAfterLast('/')
				}
				add(link, MediaAttachment(target, original, kind, name, latLon, path))
			}
		}
		for (point in app.favoritesHelper.favouritePoints.toList()) {
			addTarget(point, MediaAttachment.Kind.FAVORITE, point.name, LatLon(point.latitude, point.longitude))
		}
		for (selected in app.selectedGpxHelper.selectedGPXFiles.toList()) {
			val gpx = selected.gpxFile
			val path = gpx.path
			val name = path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
			for (point in gpx.getPointsList() + gpx.getRoutePoints()) {
				addTarget(point, MediaAttachment.Kind.TRACK_POINT, name, LatLon(point.lat, point.lon), path)
			}
		}
		return entries.values.toList()
	}
}
