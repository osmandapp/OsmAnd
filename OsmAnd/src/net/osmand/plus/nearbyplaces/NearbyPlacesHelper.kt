package net.osmand.plus.nearbyplaces

import net.osmand.NativeLibrary
import net.osmand.ResultMatcher
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.binary.ObfConstants
import net.osmand.core.jni.ZoomLevel
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.MapObject
import net.osmand.data.PointDescription
import net.osmand.data.QuadRect
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.other.MenuObject
import net.osmand.plus.mapcontextmenu.other.MenuObjectUtils
import net.osmand.plus.search.GetNearbyPlacesImagesTask
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.plus.views.layers.ContextMenuLayer
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import java.util.Collections
import kotlin.math.min

object NearbyPlacesHelper {
	private lateinit var app: OsmandApplication
	private var lastModifiedTime: Long = 0
	private const val PLACES_LIMIT = 50
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

	fun showPointInContextMenu(mapActivity: MapActivity, point: OsmandApiFeatureData) {
		val rendererView = app.osmandMap.mapView.mapRenderer
		val mapContext = NativeCoreContext.getMapRendererContext()
		var menuObject: MenuObject? = null
		val latitude: Double = point.geometry.coordinates[1]
		val longitude: Double = point.geometry.coordinates[0]
		val latLon = LatLon(latitude, longitude)
		if (rendererView != null && mapContext != null) {
			val pointI = NativeUtilities.getPoint31FromLatLon(latLon)
			val zoom: ZoomLevel = rendererView.zoomLevel
			val polygons: List<NativeLibrary.RenderedObject?> =
				mapContext.retrievePolygonsAroundPoint(pointI, zoom, false)
			if (!Algorithms.isEmpty(polygons)) {
				var menuObjects =
					MenuObjectUtils.createMenuObjectsList(mapActivity, polygons, latLon)
				menuObjects = menuObjects.filter { it.`object` is MapObject }
				for (mObj in menuObjects) {
					val mapObject = mObj.`object` as MapObject
					val osmId = ObfConstants.getOsmObjectId(mapObject)
					if (osmId == point.properties.osmid) {
						menuObject = mObj
						break
					}
				}
			}
		}
		val contextObject: IContextMenuProvider = mapActivity.mapLayers.poiMapLayer
		val contextMenuLayer: ContextMenuLayer = mapActivity.mapLayers.contextMenuLayer
		val pointDescription: PointDescription
		val contextMenuObject: Any?
		if (menuObject == null) {
			val amenity: Amenity? = getAmenity(latLon, point)
			pointDescription = PointDescription(
				PointDescription.POINT_TYPE_NEARBY_PLACE,
				point.properties.wikiTitle)
			contextMenuObject = amenity
		} else {
			pointDescription = menuObject.pointDescription
			contextMenuObject = menuObject.getObject()
		}
		val mapView = app.osmandMap.mapView
		mapView.currentRotatedTileBox.setLatLonCenter(latLon.latitude, latLon.longitude)
		contextMenuLayer.showContextMenu(
			latLon,
			pointDescription,
			contextMenuObject,
			contextObject)
	}

	fun getAmenity(latLon: LatLon, point: OsmandApiFeatureData): Amenity? {
		val mapView = app.osmandMap.mapView
		val tb = mapView.currentRotatedTileBox.copy()
		tb.setLatLonCenter(latLon.latitude, latLon.longitude)
		tb.zoom = 15
		val mapRect = tb.latLonBounds
		var foundAmenity: Amenity? = null
		app.resourceManager.searchAmenities(
			BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
			mapRect.top, mapRect.left, mapRect.bottom, mapRect.right,
			-1, true,
			object : ResultMatcher<Amenity?> {
				override fun publish(amenity: Amenity?): Boolean {
					var idFound = false
					val id = ObfConstants.getOsmObjectId(amenity)
					if (point.properties.osmid == id) {
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