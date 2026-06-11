package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.model.MediaHolder

class GalleryRepository(
	private val loadStateRegistry: MediaLoadStateRegistry
) {
	private val cache = mutableMapOf<GalleryKey, MediaHolder>()

	fun put(key: GalleryKey, holder: MediaHolder) {
		cache[key] = holder
		loadStateRegistry.clear()
	}

	fun invalidate(key: GalleryKey) {
		cache.remove(key)
		loadStateRegistry.clear()
	}

	fun clear() {
		cache.clear()
		loadStateRegistry.clear()
	}

	fun get(key: GalleryKey): MediaHolder? = cache[key]
}