package net.osmand.plus.gallery.attached

import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.shared.gpx.primitives.Linkable

// TODO: Temporary solution
class AttachedMediaRegistry {

	private val cache = mutableMapOf<GalleryKey, Linkable>()

	fun register(key: GalleryKey, linkable: Linkable) {
		cache[key] = linkable
	}

	fun unregister(key: GalleryKey) {
		cache.remove(key)
	}

	fun get(key: GalleryKey): Linkable? {
		return cache[key]
	}
}