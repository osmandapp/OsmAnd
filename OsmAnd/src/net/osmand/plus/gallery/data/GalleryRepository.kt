package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.online.OnlinePhotosHolder

object GalleryRepository {

	private val cache = mutableMapOf<GalleryKey, OnlinePhotosHolder>()

	fun get(key: GalleryKey): OnlinePhotosHolder? = cache[key]

	fun put(key: GalleryKey, holder: OnlinePhotosHolder) {
		cache[key] = holder
	}

	fun isCurrentKey(key: GalleryKey): Boolean = cache.containsKey(key)

	fun invalidate(key: GalleryKey) {
		cache.remove(key)
	}

	fun clear() {
		cache.clear()
	}
}