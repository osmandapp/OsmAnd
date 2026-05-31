package net.osmand.plus.gallery.attached

import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.domain.MediaItem

class AttachedMediaHolder : MediaHolder {
	private val items = mutableListOf<MediaItem>()

	override fun getItems(): List<MediaItem> = items

	fun addItem(item: MediaItem) { items.add(item) }
	fun clear() { items.clear() }
}