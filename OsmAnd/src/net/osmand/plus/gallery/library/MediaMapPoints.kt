package net.osmand.plus.gallery.library

import net.osmand.plus.OsmandApplication

/**
 * Media of the Media tab that has coordinates (short link in the file name, photo EXIF, video
 * location) and is not an A/V note: notes are drawn by the layer from their recordings.
 * Includes Gallery photos attached to favorites and files of the Media storage folder.
 */
class MediaMapPoints(private val app: OsmandApplication, private val onChanged: Runnable) {

	var entries: List<MediaLibraryEntry> = emptyList()
		private set
	private var subscribed = false
	private val listener: (List<MediaLibraryEntry>) -> Unit = ::update

	fun start() {
		if (!subscribed) {
			subscribed = true
			app.galleryHelper.mediaLibraryRepository.subscribe(listener)
		}
	}

	fun stop() {
		if (subscribed) {
			subscribed = false
			app.galleryHelper.mediaLibraryRepository.unsubscribe(listener)
			entries = emptyList()
		}
	}

	fun getOrderedIds(): List<String> = entries.map { it.id }

	private fun update(all: List<MediaLibraryEntry>) {
		val located = all.filter { entry ->
			val location = entry.location
			entry.recording == null && location != null
					&& location.latitude.isFinite() && location.longitude.isFinite()
					&& (location.latitude != 0.0 || location.longitude != 0.0)
		}
		// The repository publishes on every metadata batch; redraw only when the points change
		if (located.map(::pointKey) != entries.map(::pointKey)) {
			entries = located
			onChanged.run()
		}
	}

	private fun pointKey(entry: MediaLibraryEntry) = Triple(entry.id, entry.type, entry.location)
}
