package net.osmand.plus.gallery.data

import kotlin.reflect.KClass

class MediaLoader(
	private val repository: GalleryRepository
) {

	private val listeners = mutableMapOf<GalleryKey, MutableList<MediaLoadListener>>()
	private val delegates = mutableMapOf<KClass<out GalleryKey>, MediaLoadDelegate>()

	fun registerDelegate(
		keyClass: Class<out GalleryKey>,
		delegate: MediaLoadDelegate
	) {
		registerDelegate(keyClass.kotlin, delegate)
	}

	fun registerDelegate(
		keyType: KClass<out GalleryKey>,
		delegate: MediaLoadDelegate
	) {
		delegates[keyType] = delegate
	}

	fun load(
		key: GalleryKey,
		listener: MediaLoadListener,
		forceReload: Boolean = false
	) {
		if (!forceReload) {
			val cached = repository.get(key)
			if (cached != null) {
				listener.onLoaded(key, cached)
				return
			}
		}

		val listenersForKey = listeners.getOrPut(key) { mutableListOf() }
		val isFirstListener = listenersForKey.isEmpty()

		val added = if (!listenersForKey.contains(listener)) {
			listenersForKey.add(listener)
			true
		} else {
			false
		}

		if (!isFirstListener) {
			if (added) {
				listener.onLoadingStarted(key)
			}
			return
		}

		val delegate = delegates[key::class]
		if (delegate == null) {
			listeners.remove(key)?.forEach { it.onLoadFailed(key) }
			return
		}

		delegate.load(
			key = key,
			onStarted = {
				listeners[key]?.forEach { it.onLoadingStarted(key) }
			},
			onResult = { holder ->
				repository.put(key, holder)
				listeners.remove(key)?.forEach { it.onLoaded(key, holder) }
			},
			onError = {
				listeners.remove(key)?.forEach { it.onLoadFailed(key) }
			}
		)
	}

	fun reload(key: GalleryKey, listener: MediaLoadListener) {
		repository.invalidate(key)
		load(key, listener, forceReload = true)
	}

	fun cancel(key: GalleryKey, listener: MediaLoadListener) {
		listeners[key]?.remove(listener)

		if (listeners[key].isNullOrEmpty()) {
			listeners.remove(key)
			delegates[key::class]?.cancel(key)
		}
	}
}