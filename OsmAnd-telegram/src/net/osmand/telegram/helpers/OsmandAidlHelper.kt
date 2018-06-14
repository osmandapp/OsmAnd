package net.osmand.telegram.helpers

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import android.widget.Toast
import net.osmand.aidl.IOsmAndAidlInterface
import net.osmand.aidl.favorite.AFavorite
import net.osmand.aidl.favorite.AddFavoriteParams
import net.osmand.aidl.favorite.RemoveFavoriteParams
import net.osmand.aidl.favorite.UpdateFavoriteParams
import net.osmand.aidl.favorite.group.AFavoriteGroup
import net.osmand.aidl.favorite.group.AddFavoriteGroupParams
import net.osmand.aidl.favorite.group.RemoveFavoriteGroupParams
import net.osmand.aidl.favorite.group.UpdateFavoriteGroupParams
import net.osmand.aidl.gpx.*
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.map.SetMapLocationParams
import net.osmand.aidl.maplayer.AMapLayer
import net.osmand.aidl.maplayer.AddMapLayerParams
import net.osmand.aidl.maplayer.RemoveMapLayerParams
import net.osmand.aidl.maplayer.UpdateMapLayerParams
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.aidl.maplayer.point.AddMapPointParams
import net.osmand.aidl.maplayer.point.RemoveMapPointParams
import net.osmand.aidl.maplayer.point.UpdateMapPointParams
import net.osmand.aidl.mapmarker.AMapMarker
import net.osmand.aidl.mapmarker.AddMapMarkerParams
import net.osmand.aidl.mapmarker.RemoveMapMarkerParams
import net.osmand.aidl.mapmarker.UpdateMapMarkerParams
import net.osmand.aidl.mapwidget.AMapWidget
import net.osmand.aidl.mapwidget.AddMapWidgetParams
import net.osmand.aidl.mapwidget.RemoveMapWidgetParams
import net.osmand.aidl.mapwidget.UpdateMapWidgetParams
import net.osmand.aidl.navigation.NavigateGpxParams
import net.osmand.aidl.navigation.NavigateParams
import net.osmand.aidl.note.StartAudioRecordingParams
import net.osmand.aidl.note.StartVideoRecordingParams
import net.osmand.aidl.note.StopRecordingParams
import net.osmand.aidl.note.TakePhotoNoteParams
import java.io.File
import java.util.*

class OsmandAidlHelper(private val app: Application) {

	companion object {
		private const val OSMAND_FREE_PACKAGE_NAME = "net.osmand"
		private const val OSMAND_PLUS_PACKAGE_NAME = "net.osmand.plus"
		private var OSMAND_PACKAGE_NAME = OSMAND_PLUS_PACKAGE_NAME
	}

	private var mIOsmAndAidlInterface: IOsmAndAidlInterface? = null

	private var initialized: Boolean = false
	private var bound: Boolean = false

	var listener: OsmandHelperListener? = null

	interface OsmandHelperListener {
		fun onOsmandConnectionStateChanged(connected: Boolean)
	}

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

	init {
		connectOsmand()
	}

	fun connectOsmand() {
		when {
			bindService(OSMAND_PLUS_PACKAGE_NAME) -> {
				OSMAND_PACKAGE_NAME = OSMAND_PLUS_PACKAGE_NAME
				bound = true
			}
			bindService(OSMAND_FREE_PACKAGE_NAME) -> {
				OSMAND_PACKAGE_NAME = OSMAND_FREE_PACKAGE_NAME
				bound = true
			}
			else -> {
				bound = false
				initialized = true
			}
		}
	}

	private fun bindService(packageName: String): Boolean {
		return if (mIOsmAndAidlInterface == null) {
			val intent = Intent("net.osmand.aidl.OsmandAidlService")
			intent.`package` = packageName
			app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
		} else {
			true
		}
	}

	fun cleanupResources() {
		try {
			if (mIOsmAndAidlInterface != null) {
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
					typeName: String, color: Int, location: ALatLon, details: List<String>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val point = AMapPoint(pointId, shortName, fullName, typeName, color, location, details)
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
					   typeName: String, color: Int, location: ALatLon, details: List<String>?): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				val point = AMapPoint(pointId, shortName, fullName, typeName, color, location, details)
				return mIOsmAndAidlInterface!!.updateMapPoint(UpdateMapPointParams(layerId, point))
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
				app.grantUriPermission(OSMAND_PACKAGE_NAME, gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
				app.grantUriPermission(OSMAND_PACKAGE_NAME, gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
}
