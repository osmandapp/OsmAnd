package net.osmand.plus.gallery

import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.attached.AttachedMediaDelegate
import net.osmand.plus.gallery.attached.AttachedMediaRegistry
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.GalleryRepository
import net.osmand.plus.gallery.data.MediaLoadStateRegistry
import net.osmand.plus.gallery.data.MediaLoader
import net.osmand.plus.gallery.online.OnlinePhotosDelegate
import net.osmand.plus.plugins.astronomy.AstronomyDelegate

class GalleryHelper(
	private val app: OsmandApplication
) {
	val loadStateRegistry = MediaLoadStateRegistry()
	val repository = GalleryRepository(loadStateRegistry)
	val mediaLoader = MediaLoader(repository)
	val attachedMediaRegistry = AttachedMediaRegistry()

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
}