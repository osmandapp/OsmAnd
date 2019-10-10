package net.osmand.telegram.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import net.osmand.aidlapi.IOsmAndAidlCallback
import net.osmand.aidlapi.IOsmAndAidlInterface
import net.osmand.aidlapi.contextmenu.AContextMenuButton
import net.osmand.aidlapi.contextmenu.ContextMenuButtonsParams
import net.osmand.aidlapi.contextmenu.RemoveContextMenuButtonsParams
import net.osmand.aidlapi.favorite.AFavorite
import net.osmand.aidlapi.favorite.AddFavoriteParams
import net.osmand.aidlapi.favorite.RemoveFavoriteParams
import net.osmand.aidlapi.favorite.UpdateFavoriteParams
import net.osmand.aidlapi.favorite.group.AFavoriteGroup
import net.osmand.aidlapi.favorite.group.AddFavoriteGroupParams
import net.osmand.aidlapi.favorite.group.RemoveFavoriteGroupParams
import net.osmand.aidlapi.favorite.group.UpdateFavoriteGroupParams
import net.osmand.aidlapi.gpx.*
import net.osmand.aidlapi.map.ALatLon
import net.osmand.aidlapi.map.SetMapLocationParams
import net.osmand.aidlapi.maplayer.AMapLayer
import net.osmand.aidlapi.maplayer.AddMapLayerParams
import net.osmand.aidlapi.maplayer.RemoveMapLayerParams
import net.osmand.aidlapi.maplayer.UpdateMapLayerParams
import net.osmand.aidlapi.maplayer.point.*
import net.osmand.aidlapi.mapmarker.AMapMarker
import net.osmand.aidlapi.mapmarker.AddMapMarkerParams
import net.osmand.aidlapi.mapmarker.RemoveMapMarkerParams
import net.osmand.aidlapi.mapmarker.UpdateMapMarkerParams
import net.osmand.aidlapi.mapwidget.AMapWidget
import net.osmand.aidlapi.mapwidget.AddMapWidgetParams
import net.osmand.aidlapi.mapwidget.RemoveMapWidgetParams
import net.osmand.aidlapi.mapwidget.UpdateMapWidgetParams
import net.osmand.aidlapi.navdrawer.NavDrawerItem
import net.osmand.aidlapi.navdrawer.SetNavDrawerItemsParams
import net.osmand.aidlapi.navigation.*
import net.osmand.aidlapi.note.StartAudioRecordingParams
import net.osmand.aidlapi.note.StartVideoRecordingParams
import net.osmand.aidlapi.note.StopRecordingParams
import net.osmand.aidlapi.note.TakePhotoNoteParams
import net.osmand.aidlapi.search.SearchParams
import net.osmand.aidlapi.search.SearchResult
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.ShowLocationHelper.Companion.MAP_LAYER_ID
import java.io.File
import java.util.*

class OsmandAidlHelper(private val app: TelegramApplication) {

	companion object {
		const val OSMAND_FREE_PACKAGE_NAME = "net.osmand"
		const val OSMAND_PLUS_PACKAGE_NAME = "net.osmand.plus"
		const val OSMAND_NIGHTLY_PACKAGE_NAME = "net.osmand.dev"

		const val UPDATE_TIME_MS = 5000L
	}

	private var mIOsmAndAidlInterface: IOsmAndAidlInterface? = null

	private var initialized: Boolean = false
	private var bound: Boolean = false

	private var osmandUpdatesCallbackId: Long = -1
	private var osmandContextMenuCallbackId: Long = -1

	var listener: OsmandHelperListener? = null

	interface OsmandHelperListener {
		fun onOsmandConnectionStateChanged(connected: Boolean)
	}

	private var mSearchCompleteListener: SearchCompleteListener? = null

	interface SearchCompleteListener {
		fun onSearchComplete(resultSet: List<SearchResult>)
	}

	private var gpxBitmapCreatedListener: GpxBitmapCreatedListener? = null

	interface GpxBitmapCreatedListener {
		fun onGpxBitmapCreated(bitmap: AGpxBitmap)
	}

	private var contextMenuButtonsListener: ContextMenuButtonsListener? = null

	interface ContextMenuButtonsListener {
		fun onContextMenuButtonClicked(buttonId:Int, pointId: String, layerId: String)
	}

	private val mIOsmAndAidlCallback = object : IOsmAndAidlCallback.Stub() {

		@Throws(RemoteException::class)
		override fun onSearchComplete(resultSet: List<SearchResult>) {
			if (mSearchCompleteListener != null) {
				mSearchCompleteListener!!.onSearchComplete(resultSet)
			}
		}

		@Throws(RemoteException::class)
		override fun onUpdate() {
			if (mUpdatesListener != null) {
				mUpdatesListener!!.update()
			}
		}

		override fun onAppInitialized() {

		}

		override fun onGpxBitmapCreated(bitmap: AGpxBitmap) {
			if (gpxBitmapCreatedListener != null) {
				gpxBitmapCreatedListener!!.onGpxBitmapCreated(bitmap)
			}
		}

		override fun updateNavigationInfo(directionInfo: ADirectionInfo?) {

		}

		override fun onContextMenuButtonClicked(buttonId:Int, pointId: String, layerId: String) {
			if (contextMenuButtonsListener != null) {
				contextMenuButtonsListener!!.onContextMenuButtonClicked(buttonId, pointId, layerId)
			}
		}

		override fun onVoiceRouterNotify(params: OnVoiceNavigationParams?) {

		}
	}

	fun setSearchCompleteListener(mSearchCompleteListener: SearchCompleteListener) {
		this.mSearchCompleteListener = mSearchCompleteListener
	}

	fun setGpxBitmapCreatedListener(gpxBitmapCreatedListener: GpxBitmapCreatedListener) {
		this.gpxBitmapCreatedListener = gpxBitmapCreatedListener
	}

	fun setContextMenuButtonsListener(contextMenuButtonsListener: ContextMenuButtonsListener) {
		this.contextMenuButtonsListener = contextMenuButtonsListener
	}

	private var mUpdatesListener: UpdatesListener? = null

	interface UpdatesListener {
		fun update()
	}

	fun setUpdatesListener(mUpdatesListener: UpdatesListener) {
		this.mUpdatesListener = mUpdatesListener
	}

	fun updatesCallbackRegistered() = osmandUpdatesCallbackId > 0
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private val mConnection = object : ServiceConnection {
		override fun onServiceConnected(className: ComponentName,
										service: IBinder) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service)
			initialized = true
			//Toast.makeText(app, "OsmAnd connected", Toast.LENGTH_SHORT).show()
			listener?.onOsmandConnectionStateChanged(true)
		}

		override fun onServiceDisconnected(className: ComponentName) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mIOsmAndAidlInterface = null
			//Toast.makeText(app, "OsmAnd disconnected", Toast.LENGTH_SHORT).show()
			listener?.onOsmandConnectionStateChanged(false)
		}
	}

	fun isOsmandBound(): Boolean {
		return initialized && bound
	}

	fun isOsmandNotInstalled(): Boolean {
		return initialized && !bound
	}

	fun isOsmandConnected(): Boolean {
		return mIOsmAndAidlInterface != null
	}
	
	/**
	 * Get list of active GPX files.
	 *
	 * @return list of active gpx files.
	 */
	val activeGpxFiles: List<ASelectedGpxFile>?
		get() {
			if (mIOsmAndAidlInterface != null) {
				try {
					val res = ArrayList<ASelectedGpxFile>()
					if (mIOsmAndAidlInterface!!.getActiveGpx(res)) {
						return res
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}

			}
			return null
		}

	/**
	 * Get list of all imported GPX files.
	 *
	 * @return list of imported gpx files.
	 */
	val importedGpxFiles: List<AGpxFile>?
		get() {
			if (mIOsmAndAidlInterface != null) {
				try {
					val files = mutableListOf<AGpxFile>()
					mIOsmAndAidlInterface!!.getImportedGpx(files)
					return files
				} catch (e: RemoteException) {
					e.printStackTrace()
				}
			}
			return null
		}

	init {
		connectOsmand()
	}

	fun reconnectOsmand() {
		cleanupResources()
		connectOsmand()
	}

	fun connectOsmand() {
		if (bindService(app.settings.appToConnectPackage)) {
			bound = true
		} else {
			bound = false
			initialized = true
		}
	}

	fun execOsmandApi(action: (() -> Unit)) {
		if (!isOsmandConnected() && isOsmandBound()) {
			connectOsmand()
		}
		if (isOsmandConnected()) {
			action.invoke()
		}
	}

	private fun bindService(packageName: String): Boolean {
		return if (mIOsmAndAidlInterface == null) {
			val intent = Intent("net.osmand.aidl.OsmandAidlServiceV2")
			intent.`package` = packageName
			app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
		} else {
			true
		}
	}

	fun cleanupResources() {
		try {
			if (mIOsmAndAidlInterface != null) {
				unregisterFromUpdates()
				mIOsmAndAidlInterface = null
				app.unbindService(mConnection)
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
	}

	fun refreshMap(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.refreshMap()
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Add favorite group with given params.
	 *
	 * @param name    - group name.
	 * @param color   - group color. Can be one of: "red", "orange", "yellow",
	 * "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
	 * @param visible - group visibility.
	 */
	fun addFavoriteGroup(name: String, color: String, visible: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val favoriteGroup = AFavoriteGroup(name, color, visible)
				return mIOsmAndAidlInterface!!.addFavoriteGroup(AddFavoriteGroupParams(favoriteGroup))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Update favorite group with given params.
	 *
	 * @param namePrev    - group name (current).
	 * @param colorPrev   - group color (current).
	 * @param visiblePrev - group visibility (current).
	 * @param nameNew     - group name (new).
	 * @param colorNew    - group color (new).
	 * @param visibleNew  - group visibility (new).
	 */
	fun updateFavoriteGroup(namePrev: String, colorPrev: String, visiblePrev: Boolean,
							nameNew: String, colorNew: String, visibleNew: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val favoriteGroupPrev = AFavoriteGroup(namePrev, colorPrev, visiblePrev)
				val favoriteGroupNew = AFavoriteGroup(nameNew, colorNew, visibleNew)
				return mIOsmAndAidlInterface!!.updateFavoriteGroup(UpdateFavoriteGroupParams(favoriteGroupPrev, favoriteGroupNew))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove favorite group with given name.
	 *
	 * @param name - name of favorite group.
	 */
	fun removeFavoriteGroup(name: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val favoriteGroup = AFavoriteGroup(name, "", false)
				return mIOsmAndAidlInterface!!.removeFavoriteGroup(RemoveFavoriteGroupParams(favoriteGroup))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Add favorite at given location with given params.
	 *
	 * @param lat         - latitude.
	 * @param lon         - longitude.
	 * @param name        - name of favorite item.
	 * @param description - description of favorite item.
	 * @param category    - category of favorite item.
	 * @param color       - color of favorite item. Can be one of: "red", "orange", "yellow",
	 * "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
	 * @param visible     - should favorite item be visible after creation.
	 */
	fun addFavorite(lat: Double, lon: Double, name: String, description: String,
					category: String, color: String, visible: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val favorite = AFavorite(lat, lon, name, description, category, color, visible)
				return mIOsmAndAidlInterface!!.addFavorite(AddFavoriteParams(favorite))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Update favorite at given location with given params.
	 *
	 * @param latPrev        - latitude (current favorite).
	 * @param lonPrev        - longitude (current favorite).
	 * @param namePrev       - name of favorite item (current favorite).
	 * @param categoryPrev   - category of favorite item (current favorite).
	 * @param latNew         - latitude (new favorite).
	 * @param lonNew         - longitude (new favorite).
	 * @param nameNew        - name of favorite item (new favorite).
	 * @param descriptionNew - description of favorite item (new favorite).
	 * @param categoryNew    - category of favorite item (new favorite). Use only to create a new category,
	 * not to update an existing one. If you want to  update an existing category,
	 * use the [.updateFavoriteGroup] method.
	 * @param colorNew       - color of new category. Can be one of: "red", "orange", "yellow",
	 * "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
	 * @param visibleNew     - should new category be visible after creation.
	 */
	fun updateFavorite(latPrev: Double, lonPrev: Double, namePrev: String, categoryPrev: String,
					   latNew: Double, lonNew: Double, nameNew: String, descriptionNew: String,
					   categoryNew: String, colorNew: String, visibleNew: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val favoritePrev = AFavorite(latPrev, lonPrev, namePrev, "", categoryPrev, "", false)
				val favoriteNew = AFavorite(latNew, lonNew, nameNew, descriptionNew, categoryNew, colorNew, visibleNew)
				return mIOsmAndAidlInterface!!.updateFavorite(UpdateFavoriteParams(favoritePrev, favoriteNew))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove favorite at given location with given params.
	 *
	 * @param lat      - latitude.
	 * @param lon      - longitude.
	 * @param name     - name of favorite item.
	 * @param category - category of favorite item.
	 */
	fun removeFavorite(lat: Double, lon: Double, name: String, category: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val favorite = AFavorite(lat, lon, name, "", category, "", false)
				return mIOsmAndAidlInterface!!.removeFavorite(RemoveFavoriteParams(favorite))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Add map marker at given location.
	 *
	 * @param lat  - latitude.
	 * @param lon  - longitude.
	 * @param name - name.
	 */
	fun addMapMarker(lat: Double, lon: Double, name: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val marker = AMapMarker(ALatLon(lat, lon), name)
				return mIOsmAndAidlInterface!!.addMapMarker(AddMapMarkerParams(marker))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	/**
	 * Add map marker at given location.
	 *
	 * @param marker  - AMapMarker.
	 */
	fun addMapMarker(marker: AMapMarker): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.addMapMarker(AddMapMarkerParams(marker))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Update map marker at given location with name.
	 *
	 * @param latPrev  - latitude (current marker).
	 * @param lonPrev  - longitude (current marker).
	 * @param namePrev - name (current marker).
	 * @param latNew  - latitude (new marker).
	 * @param lonNew  - longitude (new marker).
	 * @param nameNew - name (new marker).
	 */
	fun updateMapMarker(latPrev: Double, lonPrev: Double, namePrev: String,
						latNew: Double, lonNew: Double, nameNew: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val markerPrev = AMapMarker(ALatLon(latPrev, lonPrev), namePrev)
				val markerNew = AMapMarker(ALatLon(latNew, lonNew), nameNew)
				return mIOsmAndAidlInterface!!.updateMapMarker(UpdateMapMarkerParams(markerPrev, markerNew))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	/**
	 * Update map marker at given location with name.
	 *
	 * @param markerPrev  - AMapMarker (current marker).
	 * @param markerNew  - AMapMarker (new marker).
	 */
	fun updateMapMarker(markerPrev: AMapMarker, markerNew: AMapMarker): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.updateMapMarker(UpdateMapMarkerParams(markerPrev, markerNew))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove map marker at given location with name.
	 *
	 * @param lat  - latitude.
	 * @param lon  - longitude.
	 * @param name - name.
	 */
	fun removeMapMarker(lat: Double, lon: Double, name: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val marker = AMapMarker(ALatLon(lat, lon), name)
				return mIOsmAndAidlInterface!!.removeMapMarker(RemoveMapMarkerParams(marker))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Add map widget to the right side of the main screen.
	 * Note: any specified icon should exist in OsmAnd app resources.
	 *
	 * @param id - widget id.
	 * @param menuIconName - icon name (configure map menu).
	 * @param menuTitle - widget name (configure map menu).
	 * @param lightIconName - icon name for the light theme (widget).
	 * @param darkIconName - icon name for the dark theme (widget).
	 * @param text - main widget text.
	 * @param description - sub text, like "km/h".
	 * @param order - order position in the widgets list.
	 * @param intentOnClick - onClick intent. Called after click on widget as startActivity(Intent intent).
	 */
	fun addMapWidget(id: String, menuIconName: String, menuTitle: String,
					 lightIconName: String, darkIconName: String, text: String, description: String,
					 order: Int, intentOnClick: Intent): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val widget = AMapWidget(id, menuIconName, menuTitle, lightIconName,
						darkIconName, text, description, order, intentOnClick)
				return mIOsmAndAidlInterface!!.addMapWidget(AddMapWidgetParams(widget))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Update map widget.
	 * Note: any specified icon should exist in OsmAnd app resources.
	 *
	 * @param id - widget id.
	 * @param menuIconName - icon name (configure map menu).
	 * @param menuTitle - widget name (configure map menu).
	 * @param lightIconName - icon name for the light theme (widget).
	 * @param darkIconName - icon name for the dark theme (widget).
	 * @param text - main widget text.
	 * @param description - sub text, like "km/h".
	 * @param order - order position in the widgets list.
	 * @param intentOnClick - onClick intent. Called after click on widget as startActivity(Intent intent).
	 */
	fun updateMapWidget(id: String, menuIconName: String, menuTitle: String,
						lightIconName: String, darkIconName: String, text: String, description: String,
						order: Int, intentOnClick: Intent): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val widget = AMapWidget(id, menuIconName, menuTitle, lightIconName,
						darkIconName, text, description, order, intentOnClick)
				return mIOsmAndAidlInterface!!.updateMapWidget(UpdateMapWidgetParams(widget))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove map widget.
	 *
	 * @param id - widget id.
	 */
	fun removeMapWidget(id: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.removeMapWidget(RemoveMapWidgetParams(id))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Add user layer on the map.
	 *
	 * @param id - layer id.
	 * @param name - layer name.
	 * @param zOrder - z-order position of layer. Default value is 5.5f
	 * @param points - initial list of points. Nullable.
	 */
	fun addMapLayer(id: String, name: String, zOrder: Float, points: List<AMapPoint>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val layer = AMapLayer(id, name, zOrder, points)
				layer.isImagePoints = true
				return mIOsmAndAidlInterface!!.addMapLayer(AddMapLayerParams(layer))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Update user layer.
	 *
	 * @param id - layer id.
	 * @param name - layer name.
	 * @param zOrder - z-order position of layer. Default value is 5.5f
	 * @param points - list of points. Nullable.
	 */
	fun updateMapLayer(id: String, name: String, zOrder: Float, points: List<AMapPoint>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val layer = AMapLayer(id, name, zOrder, points)
				layer.isImagePoints = true
				return mIOsmAndAidlInterface!!.updateMapLayer(UpdateMapLayerParams(layer))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove user layer.
	 *
	 * @param id - layer id.
	 */
	fun removeMapLayer(id: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.removeMapLayer(RemoveMapLayerParams(id))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Show AMapPoint on map in OsmAnd.
	 *
	 * @param layerId - layer id. Note: layer should be added first.
	 * @param pointId - point id.
	 * @param shortName - short name (single char). Displayed on the map.
	 * @param fullName - full name. Displayed in the context menu on first row.
	 * @param typeName - type name. Displayed in context menu on second row.
	 * @param color - color of circle's background.
	 * @param location - location of the point.
	 * @param details - list of details. Displayed under context menu.
	 */
	fun showMapPoint(layerId: String, pointId: String, shortName: String, fullName: String,
							typeName: String, color: Int, location: ALatLon, details: List<String>?, params: Map<String, String>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val point = AMapPoint(pointId, shortName, fullName, typeName, layerId, color, location, details, params)
				return mIOsmAndAidlInterface!!.showMapPoint(ShowMapPointParams(layerId, point))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	/**
	 * Add point to user layer.
	 *
	 * @param layerId - layer id. Note: layer should be added first.
	 * @param pointId - point id.
	 * @param shortName - short name (single char). Displayed on the map.
	 * @param fullName - full name. Displayed in the context menu on first row.
	 * @param typeName - type name. Displayed in context menu on second row.
	 * @param color - color of circle's background.
	 * @param location - location of the point.
	 * @param details - list of details. Displayed under context menu.
	 */
	fun addMapPoint(layerId: String, pointId: String, shortName: String, fullName: String,
					typeName: String, color: Int, location: ALatLon, details: List<String>?, params: Map<String, String>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val point = AMapPoint(pointId, shortName, fullName, typeName, layerId, color, location, details, params)
				return mIOsmAndAidlInterface!!.addMapPoint(AddMapPointParams(layerId, point))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Update point.
	 *
	 * @param layerId - layer id.
	 * @param pointId - point id.
	 * @param shortName - short name (single char). Displayed on the map.
	 * @param fullName - full name. Displayed in the context menu on first row.
	 * @param typeName - type name. Displayed in context menu on second row.
	 * @param color - color of circle's background.
	 * @param location - location of the point.
	 * @param details - list of details. Displayed under context menu.
	 */
	fun updateMapPoint(layerId: String, pointId: String, shortName: String, fullName: String,
					   typeName: String, color: Int, location: ALatLon, details: List<String>?, params: Map<String, String>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val point = AMapPoint(pointId, shortName, fullName, typeName, layerId, color, location, details, params)
				return mIOsmAndAidlInterface!!.updateMapPoint(UpdateMapPointParams(layerId, point, true))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove point.
	 *
	 * @param layerId - layer id.
	 * @param pointId - point id.
	 */
	fun removeMapPoint(layerId: String, pointId: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.removeMapPoint(RemoveMapPointParams(layerId, pointId))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Import GPX file to OsmAnd.
	 * OsmAnd must have rights to access location. Not recommended.
	 *
	 * @param file      - File which represents GPX track.
	 * @param fileName  - Destination file name. May contain dirs.
	 * @param color     - color of gpx. Can be one of: "red", "orange", "lightblue", "blue", "purple",
	 * "translucent_red", "translucent_orange", "translucent_lightblue",
	 * "translucent_blue", "translucent_purple"
	 * @param show      - show track on the map after import
	 */
	fun importGpxFromFile(file: File, fileName: String, color: String, show: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.importGpx(ImportGpxParams(file, fileName, color, show))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Import GPX file to OsmAnd.
	 *
	 * @param gpxUri    - URI created by FileProvider.
	 * @param fileName  - Destination file name. May contain dirs.
	 * @param color     - color of gpx. Can be one of: "", "red", "orange", "lightblue", "blue", "purple",
	 * "translucent_red", "translucent_orange", "translucent_lightblue",
	 * "translucent_blue", "translucent_purple"
	 * @param show      - show track on the map after import
	 */
	fun importGpxFromUri(gpxUri: Uri, fileName: String, color: String, show: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				app.grantUriPermission(app.settings.appToConnectPackage, gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				return mIOsmAndAidlInterface!!.importGpx(ImportGpxParams(gpxUri, fileName, color, show))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun navigateGpxFromUri(gpxUri: Uri, force: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				app.grantUriPermission(app.settings.appToConnectPackage, gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				return mIOsmAndAidlInterface!!.navigateGpx(NavigateGpxParams(gpxUri, force))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Import GPX file to OsmAnd.
	 *
	 * @param data      - Raw contents of GPX file. Sent as intent's extra string parameter.
	 * @param fileName  - Destination file name. May contain dirs.
	 * @param color     - color of gpx. Can be one of: "red", "orange", "lightblue", "blue", "purple",
	 * "translucent_red", "translucent_orange", "translucent_lightblue",
	 * "translucent_blue", "translucent_purple"
	 * @param show      - show track on the map after import
	 */
	fun importGpxFromData(data: String, fileName: String, color: String, show: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.importGpx(ImportGpxParams(data, fileName, color, show))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun navigateGpxFromData(data: String, force: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.navigateGpx(NavigateGpxParams(data, force))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Show GPX file on map.
	 *
	 * @param fileName - file name to show. Must be imported first.
	 */
	fun showGpx(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.showGpx(ShowGpxParams(fileName))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Hide GPX file.
	 *
	 * @param fileName - file name to hide.
	 */
	fun hideGpx(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.hideGpx(HideGpxParams(fileName))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Remove GPX file.
	 *
	 * @param fileName - file name to remove;
	 */
	fun removeGpx(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.removeGpx(RemoveGpxParams(fileName))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Get list of active GPX files.
	 *
	 * @param latitude - latitude of new map center.
	 * @param longitude - longitude of new map center.
	 * @param zoom - map zoom level. Set 0 to keep zoom unchanged.
	 * @param animated - set true to animate changes.
	 */
	fun setMapLocation(latitude: Double, longitude: Double, zoom: Int, animated: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.setMapLocation(
						SetMapLocationParams(latitude, longitude, zoom, animated))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun startGpxRecording(params: StartGpxRecordingParams): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.startGpxRecording(params)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun stopGpxRecording(params: StopGpxRecordingParams): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.stopGpxRecording(params)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun takePhotoNote(lat: Double, lon: Double): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.takePhotoNote(TakePhotoNoteParams(lat, lon))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun startVideoRecording(lat: Double, lon: Double): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.startVideoRecording(StartVideoRecordingParams(lat, lon))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun startAudioRecording(lat: Double, lon: Double): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.startAudioRecording(StartAudioRecordingParams(lat, lon))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun stopRecording(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.stopRecording(StopRecordingParams())
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun navigate(startName: String, startLat: Double, startLon: Double, destName: String, destLat: Double, destLon: Double, profile: String, force: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.navigate(NavigateParams(startName, startLat, startLon, destName, destLat, destLon, profile, force))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun navigateSearch(startName: String, startLat: Double, startLon: Double, searchQuery: String, searchLat: Double, searchLon: Double, profile: String, force: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.navigateSearch(NavigateSearchParams(startName, startLat, startLon, searchQuery, searchLat, searchLon, profile, force))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	fun setNavDrawerItems(appPackage: String, names: List<String>, uris: List<String>, iconNames: List<String>, flags: List<Int>): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val items = mutableListOf<NavDrawerItem>()
				for (i in names.indices) {
					items.add(NavDrawerItem(names[i], uris[i], iconNames[i], flags[i]))
				}
				return mIOsmAndAidlInterface!!.setNavDrawerItems(SetNavDrawerItemsParams(appPackage, items))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	fun clearNavDrawerItems(appPackage: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.setNavDrawerItems(SetNavDrawerItemsParams(appPackage, emptyList()))
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	/**
	 * Put navigation on pause.
	 */
	fun pauseNavigation(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.pauseNavigation(PauseNavigationParams())
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Resume navigation if it was paused before.
	 */
	fun resumeNavigation(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.resumeNavigation(ResumeNavigationParams())
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Stop navigation. Removes target / intermediate points and route path from the map.
	 */
	fun stopNavigation(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.stopNavigation(StopNavigationParams())
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Mute voice guidance. Stays muted until unmute manually or via the api.
	 */
	fun muteNavigation(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.muteNavigation(MuteNavigationParams())
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Unmute voice guidance.
	 */
	fun unmuteNavigation(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.unmuteNavigation(UnmuteNavigationParams())
			} catch (e: RemoteException) {
				e.printStackTrace()
			}

		}
		return false
	}

	/**
	 * Run search for POI / Address.
	 *
	 * @param searchQuery - search query string.
	 * @param searchType - type of search. Values:
	 * SearchParams.SEARCH_TYPE_ALL - all kind of search
	 * SearchParams.SEARCH_TYPE_POI - POIs only
	 * SearchParams.SEARCH_TYPE_ADDRESS - addresses only
	 *
	 * @param latitude - latitude of original search location.
	 * @param longitude - longitude of original search location.
	 * @param radiusLevel - value from 1 to 7. Default value = 1.
	 * @param totalLimit - limit of returned search result rows. Default value = -1 (unlimited).
	 */
	fun search(searchQuery: String, searchType: Int, latitude: Double, longitude: Double, radiusLevel: Int, totalLimit: Int): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.search(SearchParams(searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit), mIOsmAndAidlCallback)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	fun registerForUpdates(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				osmandUpdatesCallbackId = mIOsmAndAidlInterface!!.registerForUpdates(UPDATE_TIME_MS, mIOsmAndAidlCallback)
				return osmandUpdatesCallbackId > 0
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}
	
	fun unregisterFromUpdates(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val unregistered = mIOsmAndAidlInterface!!.unregisterFromUpdates(osmandUpdatesCallbackId)
				if (unregistered) {
					osmandUpdatesCallbackId = 0
				}
				return unregistered
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	fun getBitmapForGpx(gpxUri: Uri, density: Float, widthPixels: Int, heightPixels: Int, color: Int): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				app.grantUriPermission(app.settings.appToConnectPackage, gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				return mIOsmAndAidlInterface!!.getBitmapForGpx(CreateGpxBitmapParams(gpxUri, density, widthPixels, heightPixels, color), mIOsmAndAidlCallback)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	fun addContextMenuButtons(
		appPackage: String,paramsId:String,
		leftTextCaption: String, rightTextCaption: String,
		leftIconName: String, rightIconName: String,
		needColorizeIcon: Boolean, enabled: Boolean, buttonId: Int
	): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val leftButton = AContextMenuButton(buttonId, leftTextCaption, rightTextCaption, leftIconName, rightIconName, needColorizeIcon, enabled)
				val params = ContextMenuButtonsParams(leftButton, null, paramsId, appPackage, MAP_LAYER_ID, osmandContextMenuCallbackId, mutableListOf())
				osmandContextMenuCallbackId = mIOsmAndAidlInterface!!.addContextMenuButtons(params, mIOsmAndAidlCallback)
				return osmandContextMenuCallbackId >= 0
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	fun removeContextMenuButtons(paramsId: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val params = RemoveContextMenuButtonsParams(paramsId, osmandContextMenuCallbackId)
				val removed = mIOsmAndAidlInterface!!.removeContextMenuButtons(params)
				if (removed) {
					osmandContextMenuCallbackId = -1
				}
				return removed
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}
}