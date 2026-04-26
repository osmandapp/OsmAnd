package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.os.AsyncTask
import net.osmand.data.LatLon
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.GalleryItem
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.mapcontextmenu.gallery.ImageCardType
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder
import net.osmand.plus.mapcontextmenu.gallery.PhotoCacheManager
import net.osmand.plus.mapcontextmenu.gallery.tasks.CacheReadTask
import net.osmand.plus.mapcontextmenu.gallery.tasks.CacheWriteTask
import net.osmand.plus.wikipedia.WikiImageCard
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiImage
import net.osmand.util.Algorithms

class AstroGalleryLoader(
	private val app: OsmandApplication,
	private val galleryController: GalleryController,
	private val mapActivityProvider: () -> MapActivity?,
	private val onStateChanged: (String, AstroGalleryState) -> Unit
) {

	private var getAstroImagesTask: GetAstroImagesTask? = null
	private var requestWid: String? = null

	private val imageCardListener = object : GetAstroImagesTask.GetImageCardsListener {
		override fun onTaskStarted() = Unit

		override fun onFinish(wikidataId: String, images: List<WikiImage>?) {
			getAstroImagesTask = null
			if (requestWid != wikidataId) {
				return
			}
			if (images == null) {
				publishReadyState(wikidataId, emptyList())
				return
			}
			val cardsHolder = buildCardsHolder(wikidataId, images)
			val galleryItems = cardsHolder?.astronomyGalleryItems.orEmpty()
			galleryController.currentCardsHolder = cardsHolder?.takeIf { galleryItems.isNotEmpty() }
			publishReadyState(wikidataId, galleryItems)
		}
	}

	fun startLoading(wikidataId: String) {
		requestWid = wikidataId

		val latLon = LatLon(0.0, 0.0)
		val params = hashMapOf("wikidataId" to wikidataId)
		val cacheManager = PhotoCacheManager(app)
		val rawKey = "wikidataId=$wikidataId"

		val hasMatchingHolder = galleryController.isCurrentHolderEquals(latLon, params)
		val existingGalleryItems = galleryController.currentCardsHolder?.astronomyGalleryItems.orEmpty()
		if (hasMatchingHolder && existingGalleryItems.isNotEmpty()) {
			publishReadyState(wikidataId, existingGalleryItems)
			return
		}
		if (hasMatchingHolder) {
			galleryController.clearHolder()
		}

		if (!app.settings.isInternetConnectionAvailable) {
			loadFromCache(cacheManager, rawKey, wikidataId)
			return
		}

		cancel()
		requestWid = wikidataId
		galleryController.clearHolder()

		getAstroImagesTask = GetAstroImagesTask(
			app = app,
			holder = ImageCardsHolder(latLon, params),
			wikidataId = wikidataId,
			getImageCardsListener = imageCardListener,
			networkResponseListener = { response ->
				savePhotoListToCache(cacheManager, rawKey, response)
			}
		)
		OsmAndTaskManager.executeTask<Void?, GetAstroImagesTask?>(getAstroImagesTask as GetAstroImagesTask)
	}

	fun cancel() {
		requestWid = null
		if (getAstroImagesTask?.status == AsyncTask.Status.RUNNING) {
			getAstroImagesTask?.cancel(false)
		}
		getAstroImagesTask = null
	}

	private fun loadFromCache(
		cacheManager: PhotoCacheManager,
		rawKey: String,
		wikidataId: String
	) {
		if (!cacheManager.exists(rawKey)) {
			publishReadyState(wikidataId, emptyList())
			return
		}
		val cacheReadTask = CacheReadTask(cacheManager, rawKey) { json ->
			if (requestWid != wikidataId) {
				return@CacheReadTask true
			}
			if (!Algorithms.isEmpty(json)) {
				imageCardListener.onFinish(wikidataId, WikiCoreHelper.getAstroImagesFromJson(json!!))
			} else {
				imageCardListener.onFinish(wikidataId, null)
			}
			true
		}
		OsmAndTaskManager.executeTask<Void?, CacheReadTask?>(cacheReadTask)
	}

	private fun savePhotoListToCache(
		cacheManager: PhotoCacheManager,
		rawKey: String,
		response: String?
	) {
		if (!Algorithms.isEmpty(response)) {
			OsmAndTaskManager.executeTask<Void?, CacheWriteTask?>(
				CacheWriteTask(cacheManager, rawKey, response!!)
			)
		}
	}

	private fun buildCardsHolder(
		wikidataId: String,
		images: List<WikiImage>
	): ImageCardsHolder? {
		val mapActivity = mapActivityProvider() ?: return null
		val latLon = LatLon(0.0, 0.0)
		val params = hashMapOf("wikidataId" to wikidataId)
		return ImageCardsHolder(latLon, params).apply {
			for (wikiImage in images) {
				addCard(ImageCardType.ASTRONOMY, WikiImageCard(mapActivity, wikiImage))
			}
		}
	}

	private fun publishReadyState(
		wikidataId: String,
		galleryItems: List<GalleryItem>
	) {
		val readyItems = galleryItems.ifEmpty {
			listOf(GalleryItem.NoImages)
		}
		onStateChanged(wikidataId, AstroGalleryState.Ready(readyItems))
	}
}
