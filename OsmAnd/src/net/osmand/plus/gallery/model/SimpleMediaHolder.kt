package net.osmand.plus.gallery.model

import net.osmand.shared.media.domain.MediaItem

class SimpleMediaHolder(
	items: Collection<MediaItem> = emptyList()
) : MediaHolder {

	private val items = items.toMutableList()

	override fun getItems(): List<MediaItem> = items

	fun addItem(item: MediaItem) {
		items.add(item)
	}

	fun addItems(items: Collection<MediaItem>) {
		this.items.addAll(items)
	}

	fun clear() {
		this.items.clear()
	}
}