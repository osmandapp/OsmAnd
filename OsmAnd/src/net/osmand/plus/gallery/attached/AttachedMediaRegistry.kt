package net.osmand.plus.gallery.attached

import net.osmand.plus.gallery.data.GalleryKey

// TODO: Temporary solution
class AttachedMediaRegistry {

	private val cache = mutableMapOf<GalleryKey, Any>()

	fun register(key: GalleryKey, obj: Any) {
		cache[key] = obj
	}

	fun unregister(key: GalleryKey) {
		cache.remove(key)
	}

	fun getObject(key: GalleryKey): Any? {
		return cache[key]
	}
}