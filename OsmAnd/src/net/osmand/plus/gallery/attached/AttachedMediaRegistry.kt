package net.osmand.plus.gallery.attached

import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.shared.gpx.primitives.Linkable
import java.util.concurrent.ConcurrentHashMap

/**
 * TODO: Temporary solution — registry is populated lazily from context menu only.
 *
 * For the future "All Attached Media" screen, this registry must be populated globally:
 * - From FavouritesHelper via FavoritesListener.onFavoritesLoaded / onFavoriteDataUpdated
 * - From GpxSelectionHelper when a GPX file is selected / deselected
 *
 * Until then, registry.get(key) only works for objects whose context menu has been opened.
 */
class AttachedMediaRegistry {

	private val cache = ConcurrentHashMap<GalleryKey, Linkable>()

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