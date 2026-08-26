package net.osmand.plus.plugins.astronomy

import android.os.AsyncTask
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.MediaLoadDelegate
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.model.SimpleMediaHolder
import net.osmand.plus.gallery.online.cache.PhotoCacheManager
import net.osmand.plus.gallery.online.tasks.CacheReadTask
import net.osmand.plus.gallery.online.tasks.CacheWriteTask
import net.osmand.plus.plugins.astronomy.views.contextmenu.GetAstroImagesTask
import net.osmand.shared.media.RemoteMediaFactory
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiImage
import net.osmand.util.Algorithms

class AstronomyDelegate(
	private val app: OsmandApplication
) : MediaLoadDelegate {

	private val activeTasks = mutableMapOf<GalleryKey.Astronomy, GetAstroImagesTask>()

	override fun load(
		key: GalleryKey,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit,
		onError: () -> Unit
	) {
		if (key !is GalleryKey.Astronomy) {
			onError()
			return
		}

		val wikidataId = key.wikidataId
		val cacheManager = PhotoCacheManager(app)
		val rawKey = "wikidataId=$wikidataId"

		if (!app.settings.isInternetConnectionAvailable) {
			loadFromCache(cacheManager, rawKey, onStarted, onResult)
			return
		}

		cancel(key)

		val task = GetAstroImagesTask(
			app = app,
			wikidataId = wikidataId,
			listener = object : GetAstroImagesTask.GetImagesListener {
				override fun onTaskStarted() = onStarted()

				override fun onFinish(wikidataId: String, images: List<WikiImage>?) {
					activeTasks.remove(key)
					onResult(buildHolder(images.orEmpty()))
				}
			},
			networkResponseListener = { response ->
				saveToCache(cacheManager, rawKey, response)
			}
		)
		activeTasks[key] = task
		OsmAndTaskManager.executeTask(task)
	}

	override fun cancel(key: GalleryKey) {
		if (key !is GalleryKey.Astronomy) return
		activeTasks.remove(key)?.let { task ->
			if (task.status == AsyncTask.Status.RUNNING) {
				task.cancel(false)
			}
		}
	}

	private fun loadFromCache(
		cacheManager: PhotoCacheManager,
		rawKey: String,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit
	) {
		if (!cacheManager.exists(rawKey)) {
			onResult(SimpleMediaHolder())
			return
		}

		onStarted()

		val cacheReadTask = CacheReadTask(cacheManager, rawKey) { json ->
			val images = if (!Algorithms.isEmpty(json)) {
				WikiCoreHelper.getAstroImagesFromJson(json!!)
			} else {
				emptyList()
			}
			onResult(buildHolder(images))
			true
		}
		OsmAndTaskManager.executeTask(cacheReadTask)
	}

	private fun buildHolder(images: List<WikiImage>): MediaHolder {
		return SimpleMediaHolder().apply {
			images.forEach { addItem(RemoteMediaFactory.fromWikiImage(it)) }
		}
	}

	private fun saveToCache(cacheManager: PhotoCacheManager, rawKey: String, response: String?) {
		if (!response.isNullOrEmpty()) {
			OsmAndTaskManager.executeTask(CacheWriteTask(cacheManager, rawKey, response))
		}
	}
}