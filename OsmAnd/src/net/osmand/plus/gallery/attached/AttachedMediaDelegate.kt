package net.osmand.plus.gallery.attached

import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.MediaLoadDelegate
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.LinkMediaFactory

class AttachedMediaDelegate(
	private val registry: AttachedMediaRegistry
) : MediaLoadDelegate {

	override fun load(
		key: GalleryKey,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit,
		onError: () -> Unit
	) {
		when (key) {
			is GalleryKey.Favorite,
			is GalleryKey.Waypoint -> {
				val holder = AttachedMediaHolder()
				registry.getLinks(key)
					?.let { LinkMediaFactory.fromLinks(it) }
					?.forEach { holder.addItem(it) }
				onResult(holder)
			}
			else -> onError()
		}
	}
}