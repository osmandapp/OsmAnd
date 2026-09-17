package net.osmand.plus.gallery.library

import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.plugins.audionotes.Recording
import net.osmand.plus.settings.mediastorage.MediaStorageHelper
import net.osmand.plus.settings.mediastorage.MediaStorageLocation
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.MediaProvider
import java.net.URI

class MediaLibraryScanner(private val app: OsmandApplication, private val plugin: AudioVideoNotesPlugin?) {
	fun scan(): List<MediaLibraryEntry> {
		val storage = MediaStorageHelper(app)
		val appBasePath = app.getAppPath().absolutePath
		val entries = linkedMapOf<String, MediaLibraryEntry>()
		fun add(link: Link, attachment: MediaAttachment? = null, recording: Recording? = null) {
			val key = keyOf(link, appBasePath) ?: return
			val item = LinkMediaFactory.fromLinks(listOf(link)).firstOrNull() ?: return
			val existing = entries[key]
			entries[key] = when {
				existing == null -> MediaLibraryEntry(item, recording, listOfNotNull(attachment))
				else -> existing.copy(recording = recording ?: existing.recording, attachments = existing.attachments + listOfNotNull(attachment))
			}
		}
		for (source in storage.listMedia(MediaStorageLocation.fromSettings(app))) {
			add(Link(source.href).apply { text = source.fileName })
		}
		for (recording in plugin?.allRecordings.orEmpty().toList()) {
			add(Link(storage.createMediaFileHref(recording.file)).apply { text = recording.file.name }, recording = recording)
		}
		fun addTarget(target: Linkable, kind: MediaAttachment.Kind, name: String, latLon: LatLon, path: String? = null) {
			for (link in target.links.orEmpty().toList()) {
				val titled = if (link.text.isNullOrBlank()) Link(link).apply { text = link.href?.substringBefore('?')?.substringAfterLast('/') } else link
				add(titled, MediaAttachment(target, link, kind, name, latLon, path))
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

	companion object {
		@JvmStatic
		fun keyOf(link: Link, appBasePath: String): String? {
			val href = link.href?.trim()?.takeIf { it.isNotEmpty() } ?: return null
			LinkMediaFactory.getInternalMediaFileName(LinkMediaFactory.getInternalPath(href))?.let { return "internal:$it" }
			val uri = runCatching { URI(href).normalize() }.getOrNull()
			if (uri?.scheme.equals("file", ignoreCase = true)) {
				val path = uri?.path.orEmpty()
				val base = appBasePath.trimEnd('/') + "/"
				if (path.startsWith(base)) {
					MediaProvider.getInternalMediaAliasFileName(path.removePrefix(base))?.let { return "internal:$it" }
				}
			}
			return LinkMediaFactory.getMediaId(link)
		}
	}
}
