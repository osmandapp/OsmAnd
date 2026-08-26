package net.osmand.plus.gallery

import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.attached.AttachedMediaDelegate
import net.osmand.plus.gallery.attached.AttachedMediaRegistry
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.GalleryRepository
import net.osmand.plus.gallery.data.LocalMediaMetadataRepository
import net.osmand.plus.gallery.data.MediaLoadStateRegistry
import net.osmand.plus.gallery.data.MediaLoader
import net.osmand.plus.gallery.data.MediaMetadataRepository
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.data.MediaSourceResolver
import net.osmand.plus.gallery.online.OnlinePhotosDelegate
import net.osmand.plus.plugins.astronomy.AstronomyDelegate

class GalleryHelper(
	private val app: OsmandApplication
) {
	val loadStateRegistry = MediaLoadStateRegistry()
	val repository = GalleryRepository(loadStateRegistry)
	val mediaLoader = MediaLoader(repository)
	val attachedMediaRegistry = AttachedMediaRegistry()

	// Temporary local implementation behind the metadata/poster/source contracts;
	// Will be replaced by a backend-backed one when the media backend is ready.
	private val localMediaRepository = LocalMediaMetadataRepository(app)
	val metadataRepository: MediaMetadataRepository = localMediaRepository
	val posterLoader: MediaPosterLoader = localMediaRepository
	val mediaSourceResolver: MediaSourceResolver = localMediaRepository
	private val attachedMediaChangeListeners = mutableSetOf<(Set<GalleryKey>) -> Unit>()

	init {
		registerDelegates()
	}

	private fun registerDelegates() {
		mediaLoader.registerDelegate(
			GalleryKey.Location::class.java,
			OnlinePhotosDelegate(app)
		)
		mediaLoader.registerDelegate(
			GalleryKey.Astronomy::class.java,
			AstronomyDelegate(app)
		)

		val attachedDelegate = AttachedMediaDelegate(attachedMediaRegistry)
		mediaLoader.registerDelegate(GalleryKey.Favorite::class.java, attachedDelegate)
		mediaLoader.registerDelegate(GalleryKey.Waypoint::class.java, attachedDelegate)
	}

	fun addAttachedMediaChangeListener(listener: (Set<GalleryKey>) -> Unit) {
		attachedMediaChangeListeners.add(listener)
	}

	fun removeAttachedMediaChangeListener(listener: (Set<GalleryKey>) -> Unit) {
		attachedMediaChangeListeners.remove(listener)
	}

	fun notifyAttachedMediaChanged(keys: Set<GalleryKey>) {
		if (keys.isEmpty()) return
		keys.forEach { repository.invalidate(it) }
		attachedMediaChangeListeners.toList().forEach { it(keys) }
	}
}