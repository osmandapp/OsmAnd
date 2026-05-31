package net.osmand.plus.plugins.astronomy

import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.domain.MediaItem

class AstronomyMediaHolder : MediaHolder {

	private val items = mutableListOf<MediaItem>()

	override fun getItems(): List<MediaItem> {
		return items
	}

	fun addItem(item: MediaItem) {
		items.add(item)
	}

	fun clear() { items.clear() }
}