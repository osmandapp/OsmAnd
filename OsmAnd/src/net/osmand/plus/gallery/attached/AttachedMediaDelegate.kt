package net.osmand.plus.gallery.attached

import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.MediaLoadDelegate
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.model.SimpleMediaHolder
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
		val links = registry.get(key)?.links
		if (links.isNullOrEmpty()) {
			onError()
		} else {
			onResult(SimpleMediaHolder(LinkMediaFactory.fromLinks(links)))
		}
	}
}