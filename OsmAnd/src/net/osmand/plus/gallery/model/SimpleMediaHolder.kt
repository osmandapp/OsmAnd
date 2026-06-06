package net.osmand.plus.gallery.model

import net.osmand.shared.media.domain.MediaItem

class SimpleMediaHolder(
	private val items: MutableList<MediaItem> = mutableListOf()
) : MediaHolder {

	override fun getItems(): List<MediaItem> = items

	fun addItem(item: MediaItem) {
		items.add(item)
	}

	fun clear() {
		items.clear()
	}
}