package net.osmand.plus.nearbyplaces

import net.osmand.data.QuadRect
import net.osmand.plus.OsmandApplication
import net.osmand.plus.search.GetNearbyPlacesImagesTask
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import java.util.Collections
import kotlin.math.min

object NearbyPlacesHelper {
	private lateinit var app: OsmandApplication
	private var lastModifiedTime: Long = 0
	private const val PLACES_LIMIT = 50
	private var prevMapRect: QuadRect? = null
	private var prevZoom: Int? = null
	private var prevLang: String? = null

	fun init(app: OsmandApplication) {
		this.app = app
	}

	private var listeners: List<NearbyPlacesListener> = Collections.emptyList()
	private var dataCollection: List<OsmandApiFeatureData>? = null

	private val loadNearbyPlacesListener: GetNearbyPlacesImagesTask.GetImageCardsListener =
		object : GetNearbyPlacesImagesTask.GetImageCardsListener {
			override fun onTaskStarted() {
			}

			override fun onFinish(result: List<OsmandApiFeatureData>) {
				val newListSize = min(result.size, PLACES_LIMIT)
				dataCollection = result.subList(0, newListSize)
				updateLastModifiedTime()
				notifyListeners()
			}
		}

	fun addListener(listener: NearbyPlacesListener) {
		listeners = CollectionUtils.addToList(listeners, listener)
	}

	fun removeListener(listener: NearbyPlacesListener) {
		listeners = CollectionUtils.removeFromList(listeners, listener)
	}

	fun notifyListeners() {
		app.runInUIThread {
			for (listener in listeners) {
				listener.onNearbyPlacesUpdated()
			}
		}
	}

	fun getDataCollection(): List<OsmandApiFeatureData> {
		return this.dataCollection ?: Collections.emptyList()
	}

	fun startLoadingNearestPhotos() {
		val mapView = app.osmandMap.mapView
		val mapRect = mapView.currentRotatedTileBox.latLonBounds
		var preferredLang = app.settings.MAP_PREFERRED_LOCALE.get()
		if (Algorithms.isEmpty(preferredLang)) {
			preferredLang = app.language
		}
		if (prevMapRect != mapRect || prevZoom != mapView.zoom || prevLang != preferredLang) {
			prevMapRect = mapRect
			prevZoom = mapView.zoom
			prevLang = preferredLang
			GetNearbyPlacesImagesTask(
				prevMapRect!!, prevZoom!!,
				prevLang!!, loadNearbyPlacesListener).execute()
		} else {
			notifyListeners()
		}
	}

	private fun updateLastModifiedTime() {
		lastModifiedTime = System.currentTimeMillis()
	}

	fun getLastModifiedTime(): Long {
		return lastModifiedTime
	}
}