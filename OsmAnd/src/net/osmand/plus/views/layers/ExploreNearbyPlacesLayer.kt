package net.osmand.plus.views.layers

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import net.osmand.PlatformUtil
import net.osmand.core.jni.PointI
import net.osmand.core.jni.TextRasterizer
import net.osmand.data.LatLon
import net.osmand.data.NearbyPlacePoint
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.R
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.core.NearbyPlacesTileProvider
import net.osmand.util.Algorithms
import net.osmand.wiki.WikiCoreHelper
import org.apache.commons.logging.Log

class ExploreNearbyPlacesLayer(ctx: Context) : OsmandMapLayer(ctx), IContextMenuProvider,
	IMoveObjectProvider, MapTextProvider<NearbyPlacePoint?> {
	protected var cache: MutableList<NearbyPlacePoint?> = ArrayList()

	@ColorInt
	private var grayColor = 0
	private var settings: OsmandSettings? = null
	private var contextMenuLayer: ContextMenuLayer? = null

	private var textScale = 1f
	private var nightMode = false

	private var nearbyPlacesChangedTime = 0L;
	var customObjectsDelegate: CustomMapObjects<NearbyPlacePoint?> = CustomMapObjects()

	//OpenGl
	private var nearbyPlacesMapLayerProvider: NearbyPlacesTileProvider? = null

	override fun initLayer(view: OsmandMapTileView) {
		super.initLayer(view)

		settings = view.application.settings
		grayColor = ContextCompat.getColor(context, R.color.color_favorite_gray)
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer::class.java)
	}

	override fun cleanupResources() {
		super.cleanupResources()
		clearNearbyPlaces()
	}

	override fun drawInScreenPixels(): Boolean {
		return true
	}

	override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {
//		if (contextMenuLayer.getMoveableObject() instanceof NearbyPlacePoint) {
//			NearbyPlacePoint objectInMotion = (NearbyPlacePoint) contextMenuLayer.getMoveableObject();
//			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
//			MapMarker mapMarker = mapMarkersHelper.getMapMarker(objectInMotion);
//			float textScale = getTextScale();
//			drawBigPoint(canvas, objectInMotion, pf.x, pf.y, mapMarker, textScale);
//			if (!changeMarkerPositionMode) {
//				changeMarkerPositionMode = true;
//				showNearbyPlcaes();
//			}
//		} else if (changeMarkerPositionMode) {
//			changeMarkerPositionMode = false;
//		showNearbyPlaces()
		//		}
	}

	override fun onPrepareBufferImage(
		canvas: Canvas,
		tileBox: RotatedTileBox,
		settings: DrawSettings) {
		super.onPrepareBufferImage(canvas, tileBox, settings)
		val nightMode = settings != null && settings.isNightMode
		val nightModeChanged = this.nightMode != nightMode
		this.nightMode = nightMode
		val textScale = getTextScale()
		val textScaleChanged = this.textScale != textScale
		this.textScale = textScale
		val nearbyPlacesChangedTime = NearbyPlacesHelper.getLastModifiedTime();
		val nearbyPlacesChanged = this.nearbyPlacesChangedTime != nearbyPlacesChangedTime;
		this.nearbyPlacesChangedTime = nearbyPlacesChangedTime;
		if (hasMapRenderer()) {
			if (mapActivityInvalidated || mapRendererChanged || nightModeChanged
				|| nearbyPlacesChanged
				|| textScaleChanged
				|| customObjectsDelegate.isChanged) {
				showNearbyPlaces()
				customObjectsDelegate.acceptChanges()
				mapRendererChanged = false
			}
		}
		mapActivityInvalidated = false
	}

	@Synchronized
	fun showNearbyPlaces() {
		val mapRenderer = mapRenderer ?: return
		clearNearbyPlaces()
		val textScale = getTextScale()
		nearbyPlacesMapLayerProvider = NearbyPlacesTileProvider(
			application, getPointsOrder(), isTextVisible,
			getTextStyle(textScale), view.density)

		val points = customObjectsDelegate.mapObjects
		showNearbyPlacePoints(textScale, points)
		nearbyPlacesMapLayerProvider!!.drawSymbols(mapRenderer)
	}

	private fun showNearbyPlacePoints(textScale: Float, points: List<NearbyPlacePoint?>) {
		val checkedPoints = points.filterNotNull()
		LOG.debug("showNearbyPlacePoints start")
		for (nearbyPlacePoint in checkedPoints) {
			val color = grayColor
			nearbyPlacesMapLayerProvider!!.addToData(
				nearbyPlacePoint,
				color,
				true,
				textScale)
		}
		LOG.debug("showNearbyPlacePoints finish")
	}

	private fun getTextStyle(textScale: Float): TextRasterizer.Style {
		return MapTextLayer.getTextStyle(context, nightMode, textScale, view.density)
	}

	private fun clearNearbyPlaces() {
		val mapRenderer = mapRenderer
		if (mapRenderer == null || nearbyPlacesMapLayerProvider == null) {
			return
		}
		nearbyPlacesMapLayerProvider!!.clearSymbols(mapRenderer)
		nearbyPlacesMapLayerProvider = null
	}

	override fun onLongPressEvent(point: PointF, tileBox: RotatedTileBox): Boolean {
		return false
	}

	private fun getNearbyPlaceFromPoint(
		tb: RotatedTileBox,
		point: PointF,
		res: MutableList<in NearbyPlacePoint>) {
		val NearbyPlacePoints = nearbyPlacePoints
		if (Algorithms.isEmpty(NearbyPlacePoints)) {
			return
		}

		val mapRenderer = mapRenderer
		val radius =
			getScaledTouchRadius(application, tb.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER
		var touchPolygon31: List<PointI?>? = null
		if (mapRenderer != null) {
			touchPolygon31 =
				NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius)
			if (touchPolygon31 == null) {
				return
			}
		}

		for (NearbyPlacePoint in NearbyPlacePoints) {
			if (!NearbyPlacePoint!!.isVisible) {
				continue
			}

			val lat = NearbyPlacePoint.latitude
			val lon = NearbyPlacePoint.longitude

			val add = if (mapRenderer != null
			) NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31!!)
			else tb.isLatLonNearPixel(lat, lon, point.x, point.y, radius)
			if (add) {
				res.add(NearbyPlacePoint)
			}
		}
	}

	override fun getObjectName(o: Any): PointDescription? {
		if (o is NearbyPlacePoint) {
			return o.getPointDescription(context)
		}
		return null
	}

	override fun collectObjectsFromPoint(
		point: PointF, tileBox: RotatedTileBox, res: MutableList<Any>,
		unknownLocation: Boolean, excludeUntouchableObjects: Boolean) {
		getNearbyPlaceFromPoint(tileBox, point, res)
	}

	override fun getObjectLocation(o: Any): LatLon? {
		if (o is NearbyPlacePoint) {
			return LatLon(o.latitude, o.longitude)
		}
		return null
	}

	override fun getTextLocation(o: NearbyPlacePoint?): LatLon? {
		return if (o != null) LatLon(o.latitude, o.longitude) else null
	}

	override fun getTextShift(o: NearbyPlacePoint?, rb: RotatedTileBox): Int {
		return (16 * rb.density * getTextScale()).toInt()
	}

	override fun getText(o: NearbyPlacePoint?): String {
		return if (o != null) PointDescription.getSimpleName(o, context) else ""
	}

	override fun isTextVisible(): Boolean {
		return settings!!.SHOW_POI_LABEL.get()
	}

	override fun isFakeBoldText(): Boolean {
		return false
	}

	override fun isObjectMovable(o: Any): Boolean {
		return o is NearbyPlacePoint
	}

	override fun applyNewObjectPosition(
		o: Any,
		position: LatLon,
		callback: ApplyMovedObjectCallback?) {
	}

	fun setCustomMapObjects(nearbyPlacePoints: List<WikiCoreHelper.OsmandApiFeatureData>?) {
		val nearbyPlacePointsList = nearbyPlacePoints?.map { o -> NearbyPlacePoint(o) }
		customObjectsDelegate.setCustomMapObjects(nearbyPlacePointsList)
		application.osmandMap.refreshMap()
	}


	private val nearbyPlacePoints: List<NearbyPlacePoint?>
		get() = customObjectsDelegate.mapObjects


	companion object {
		private val LOG: Log = PlatformUtil.getLog(
			ExploreNearbyPlacesLayer::class.java)
	}
}
