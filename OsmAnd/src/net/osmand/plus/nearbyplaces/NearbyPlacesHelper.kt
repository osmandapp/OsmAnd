package net.osmand.plus.nearbyplaces

import com.squareup.picasso.Picasso
import net.osmand.ResultMatcher
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.binary.ObfConstants
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.NearbyPlacePoint
import net.osmand.data.PointDescription
import net.osmand.data.QuadRect
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.search.GetNearbyPlacesImagesTask
import net.osmand.plus.views.layers.ContextMenuLayer
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils
import net.osmand.util.MapUtils
import net.osmand.wiki.WikiCoreHelper
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import java.util.Collections
import kotlin.math.min

object NearbyPlacesHelper {
	private lateinit var app: OsmandApplication
	private var lastModifiedTime: Long = 0
	private const val PLACES_LIMIT = 50
	private const val NEARBY_MIN_RADIUS: Int = 250

	private var prevMapRect: QuadRect = QuadRect()
	private var prevZoom = 0
	private var prevLang = ""

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
				dataCollection = result.filter { !Algorithms.isEmpty(it.properties.photoTitle) }
					.subList(0, newListSize)
				dataCollection?.let {
					for (image in it) {
						val wikiImage = WikiCoreHelper.getImageData(image.properties.photoTitle)
						Picasso.get()
							.load(wikiImage.imageIconUrl)
							.fetch()
					}
				}
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
				app,
				prevMapRect, prevZoom,
				prevLang, loadNearbyPlacesListener).execute()
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

	fun showPointInContextMenu(mapActivity: MapActivity, data: OsmandApiFeatureData) {
		val point = NearbyPlacePoint(data)
		val latitude: Double = point.latitude
		val longitude: Double = point.longitude
		val latLon = LatLon(latitude, longitude)
		val contextObject: IContextMenuProvider = mapActivity.mapLayers.nearbyPlacesLayer
		val contextMenuLayer: ContextMenuLayer = mapActivity.mapLayers.contextMenuLayer
		val pointDescription = PointDescription(
			PointDescription.POINT_TYPE_NEARBY_PLACE,
			point.wikiTitle)
		val mapView = app.osmandMap.mapView
		mapView.currentRotatedTileBox.setLatLonCenter(latLon.latitude, latLon.longitude)
		contextMenuLayer.showContextMenu(
			latLon,
			pointDescription,
			point,
			contextObject)
	}

	fun getAmenity(latLon: LatLon, osmId: Long): Amenity? {
		var foundAmenity: Amenity? = null
		val radius = NEARBY_MIN_RADIUS
		val rect = MapUtils.calculateLatLonBbox(latLon.latitude, latLon.longitude, radius)
		app.resourceManager.searchAmenities(
			BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
			rect.top, rect.left, rect.bottom, rect.right,
			-1, true,
			object : ResultMatcher<Amenity?> {
				override fun publish(amenity: Amenity?): Boolean {
					var idFound = false
					val id = ObfConstants.getOsmObjectId(amenity)
					if (osmId == id) {
						foundAmenity = amenity
						idFound = true
					}
					return idFound
				}

				override fun isCancelled(): Boolean {
					return false
				}
			})
		return foundAmenity
	}
}