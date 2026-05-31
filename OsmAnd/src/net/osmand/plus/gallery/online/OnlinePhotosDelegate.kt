package net.osmand.plus.gallery.online

import android.os.AsyncTask
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.MediaLoadDelegate
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.online.OnlinePhotosGroup.WIKIMEDIA
import net.osmand.plus.gallery.online.cache.PhotoCacheManager
import net.osmand.plus.gallery.online.tasks.CacheReadTask
import net.osmand.plus.gallery.online.tasks.CacheWriteTask
import net.osmand.plus.gallery.online.tasks.GetOnlineImagesTask
import net.osmand.shared.media.RemoteMediaFactory
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiHelper
import net.osmand.shared.wiki.WikiImage
import net.osmand.util.Algorithms

class OnlinePhotosDelegate(
	private val app: OsmandApplication
) : MediaLoadDelegate {

	private val activeTasks = mutableMapOf<GalleryKey.Location, GetOnlineImagesTask>()

	override fun load(
		key: GalleryKey,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit,
		onError: () -> Unit
	) {
		if (key !is GalleryKey.Location) {
			onError()
			return
		}

		val latLon = key.latLon
		val params = HashMap(key.params)

		val cacheManager = PhotoCacheManager(app)

		val wikiTagData = WikiHelper.extractWikiTagData(params)
		val wikidataId = wikiTagData.wikidataId
		val wikiCategory = wikiTagData.wikiCategory
		val wikiTitle = wikiTagData.wikiTitle
		val rawKey = PhotoCacheManager.buildRawKey(wikidataId, wikiCategory, wikiTitle)

		if (!app.settings.isInternetConnectionAvailable) {
			loadFromCache(
				cacheManager = cacheManager,
				rawKey = rawKey,
				key = key,
				wikiTagData = wikiTagData,
				onStarted = onStarted,
				onResult = onResult
			)
		} else {
			cancel(key)

			val loadImagesListener = object : GetOnlineImagesTask.GetImageCardsListener {

				override fun onTaskStarted() = onStarted()

				override fun onFinish(holder: OnlinePhotosHolder) {
					activeTasks.remove(key)
					onResult(holder)
				}
			}

			val task = GetOnlineImagesTask(app, latLon, params, loadImagesListener) { response ->
				savePhotoListToCache(cacheManager, rawKey, response)
			}

			activeTasks[key] = task
			OsmAndTaskManager.executeTask(task)
		}
	}

	private fun loadFromCache(
		cacheManager: PhotoCacheManager,
		rawKey: String,
		key: GalleryKey.Location,
		wikiTagData: WikiHelper.WikiTagData,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit
	) {
		val holder = OnlinePhotosHolder(key.latLon, key.params)

		if (!cacheManager.exists(rawKey)) {
			onResult(holder)
			return
		}

		onStarted()

		val cacheReadTask = CacheReadTask(cacheManager, rawKey) { json ->
			if (!Algorithms.isEmpty(json)) {
				val wikiImages = WikiCoreHelper.getImagesFromJson(
					json,
					wikiTagData.wikiImages
				)
				for (wikiImage: WikiImage in wikiImages) {
					holder.addItem(
						WIKIMEDIA,
						RemoteMediaFactory.fromWikiImage(wikiImage)
					)
				}
			}
			onResult(holder)
			true
		}
		OsmAndTaskManager.executeTask(cacheReadTask)
	}

	private fun savePhotoListToCache(
		cacheManager: PhotoCacheManager,
		rawKey: String,
		response: String?
	) {
		if (!response.isNullOrEmpty()) {
			val cacheWriteTask = CacheWriteTask(cacheManager, rawKey, response)
			OsmAndTaskManager.executeTask(cacheWriteTask)
		}
	}

	override fun cancel(key: GalleryKey) {
		if (key !is GalleryKey.Location) {
			return
		}

		activeTasks.remove(key)?.let { task ->
			if (task.status == AsyncTask.Status.RUNNING) {
				task.cancel(false)
			}
		}
	}
}