package net.osmand.plus.gallery.attached

import net.osmand.data.FavouritePoint
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.MediaLoadDelegate
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt
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
		val links = getLinks(key)
		if (links != null) {
			val holder = AttachedMediaHolder()
			LinkMediaFactory.fromLinks(links).forEach { holder.addItem(it) }
			onResult(holder)
		} else {
			onError()
		}
	}

	private fun getLinks(key: GalleryKey): List<Link>? {
		val obj = registry.getObject(key)
		if (obj is FavouritePoint) {
			return obj.links
		}
		if (obj is WptPt) {
			return obj.links
		}
		return null
	}
}