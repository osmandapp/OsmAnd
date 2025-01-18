package net.osmand.plus.nearbyplaces

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.search.GetNearbyImagesTask
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.layers.NearbyPlacesLayer
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import java.util.Collections
import kotlin.math.min

object NearbyPlacesHelper {
	private lateinit var app: OsmandApplication
	private var lastModifiedTime: Long = 0
	private const val PLACES_LIMIT = 50;

	fun init(app: OsmandApplication) {
		this.app = app
	}

	private var listeners: List<NearbyPlacesListener> = Collections.emptyList()
	private var dataCollection: List<OsmandApiFeatureData>? = null

	private val loadNearbyPlacesListener: GetNearbyImagesTask.GetImageCardsListener =
		object : GetNearbyImagesTask.GetImageCardsListener {
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
		GetNearbyImagesTask(
			mapRect, mapView.zoom,
			preferredLang!!, loadNearbyPlacesListener).execute()
	}

	private fun updateLastModifiedTime() {
		lastModifiedTime = System.currentTimeMillis()
	}

	fun getLastModifiedTime(): Long {
		return lastModifiedTime
	}

	fun createSmallPointBitmap(layer: NearbyPlacesLayer): Bitmap {
		val bitmapPaint = layer.bitmapPaint
		val circle = layer.circle
		val smallIconSize = layer.smallIconSize
		val pointerOuterColor = layer.pointOuterColor
		var bitmapResult =
			Bitmap.createBitmap(smallIconSize, smallIconSize, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmapResult)
		bitmapPaint.setColorFilter(PorterDuffColorFilter(pointerOuterColor, PorterDuff.Mode.SRC_IN))
		val srcRect = Rect(0, 0, circle.getWidth(), circle.getHeight())
		var dstRect = RectF(0f, 0f, smallIconSize.toFloat(), smallIconSize.toFloat())
		canvas.drawBitmap(circle, srcRect, dstRect!!, bitmapPaint)
		bitmapPaint.setColorFilter(
			PorterDuffColorFilter(
				ColorUtilities.getColor(
					app,
					R.color.poi_background), PorterDuff.Mode.SRC_IN))
		dstRect = RectF(2f, 2f, (smallIconSize - 4).toFloat(), (smallIconSize - 4).toFloat())
		canvas.drawBitmap(circle, srcRect, dstRect, bitmapPaint)
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, smallIconSize, smallIconSize, false)
		return bitmapResult
	}

	fun createBigBitmap(layer: NearbyPlacesLayer, loadedBitmap: Bitmap): Bitmap {
		val bitmapPaint = layer.bitmapPaint
		val circle = layer.circle
		val bigIconSize = layer.bigIconSize
		val pointerOuterColor = layer.pointOuterColor
		val bg: Bitmap = circle
		var bitmapResult = Bitmap.createBitmap(bigIconSize, bigIconSize, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmapResult)
		bitmapPaint.setColorFilter(PorterDuffColorFilter(pointerOuterColor, PorterDuff.Mode.SRC_IN))
		canvas.drawBitmap(bg, 0f, 0f, bitmapPaint)
		val cx = bg.width / 2
		val cy = bg.height / 2
		val radius = (min(cx.toDouble(), cy.toDouble()) - 8).toInt()
		canvas.save()
		canvas.clipRect(0, 0, bg.width, bg.height)
		val circularPath = Path()
		circularPath.addCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), Path.Direction.CW)
		canvas.clipPath(circularPath)
		val srcRect = Rect(0, 0, loadedBitmap.width, loadedBitmap.height)
		val dstRect = RectF(0f, 0f, bg.width.toFloat(), bg.height.toFloat())
		bitmapPaint.setColorFilter(null)
		canvas.drawBitmap(loadedBitmap, srcRect, dstRect, bitmapPaint)
		canvas.restore()
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, bigIconSize, bigIconSize, false)
		return bitmapResult
	}
}