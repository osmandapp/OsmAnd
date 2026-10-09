package net.osmand.plus.wear

import android.graphics.Bitmap

import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.plus.OsmandApplication
import net.osmand.plus.render.MapRenderRepositories
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType

import kotlin.math.floor
import kotlin.math.log2

/**
 * The legacy renderer, drawing the watch's map into a bitmap of whatever size is asked for.
 *
 * It keeps its own repositories and its own readers rather than sharing the phone's: that
 * renderer holds one bitmap and one tile box, and the phone is using them.
 */
class WearLegacyMapSource(private val app: OsmandApplication) : WearMapSource {

	private var repositories: MapRenderRepositories? = null
	private var box: RotatedTileBox? = null

	override var drawn = 0
		private set

	/** Drawing happens on the thread that asks for the frame, so a gesture is already in it. */
	override val framesInFlight = 0

	override fun open(width: Int, height: Int, density: Float): Boolean {
		val own = repositories ?: MapRenderRepositories(app).also { repositories = it }
		for (resource in app.resourceManager.fileReaders) {
			val reader = resource.getReader(BinaryMapReaderResourceType.WEAR_RENDERING) ?: continue
			own.initializeNewResource(reader.file, reader)
		}
		val phone = app.osmandMap.mapView.currentRotatedTileBox
		box = RotatedTileBox.RotatedTileBoxBuilder()
			.setLocation(phone.latitude, phone.longitude)
			.setZoom(phone.zoom)
			.setZoomFloatPart(phone.zoomFloatPart)
			.density(density)
			.setMapDensity(phone.mapDensity)
			.setRotate(phone.rotate)
			.setPixelDimensions(width, height, 0.5f, 0.5f)
			.build()
		return true
	}

	override fun overlayBox(): RotatedTileBox? = box?.let { RotatedTileBox(it) }

	override fun frame(): Bitmap? {
		val own = repositories ?: return null
		val current = box ?: return null
		own.loadMap(RotatedTileBox(current), null)
		drawn++
		return own.bitmap
	}

	/** Unlike the core renderer, this float zoom really is a power of two, so log2 converts. */
	override fun zoom(factor: Float) {
		val current = box ?: return
		if (factor <= 0f) {
			return
		}
		val wanted = current.zoom + current.zoomFloatPart + log2(factor.toDouble())
		val level = floor(wanted).toInt().coerceIn(MIN_ZOOM, MAX_ZOOM)
		current.setZoomAndAnimation(level, 0.0, wanted - level)
	}

	override fun pan(dx: Float, dy: Float) {
		val current = box ?: return
		val moved = current.getLatLonFromPixel(
			current.centerPixelX - dx, current.centerPixelY - dy)
		current.setLatLonCenter(moved.latitude, moved.longitude)
	}


	override fun fit(bounds: QuadRect) {
		val current = box ?: return
		current.setRotate(app.osmandMap.mapView.currentRotatedTileBox.rotate)
		current.setLatLonCenter(
			(bounds.top + bounds.bottom) / 2, (bounds.left + bounds.right) / 2)
		for (zoom in MAX_ZOOM downTo MIN_ZOOM) {
			current.setZoomAndAnimation(zoom, 0.0, 0.0)
			if (current.holds(bounds)) {
				return
			}
		}
	}

	/**
	 * Asked in screen pixels, not degrees: a turned box reports degree bounds out to its
	 * diagonal, and a route half as wide again as the screen would appear to fit.
	 */
	private fun RotatedTileBox.holds(bounds: QuadRect): Boolean {
		val corners = arrayOf(
			bounds.top to bounds.left, bounds.top to bounds.right,
			bounds.bottom to bounds.left, bounds.bottom to bounds.right)
		return corners.all { (latitude, longitude) ->
			val x = getPixXFromLatLon(latitude, longitude)
			val y = getPixYFromLatLon(latitude, longitude)
			x >= 0 && y >= 0 && x <= pixWidth && y <= pixHeight
		}
	}

	override fun followPhone() {
		val current = box ?: return
		val phone = app.osmandMap.mapView.currentRotatedTileBox
		current.setLatLonCenter(phone.latitude, phone.longitude)
		current.setRotate(phone.rotate)
	}

	override fun close() {
		box = null
		// The repositories are kept: they hold the parsed rendering rules and the cached objects,
		// and building them again is most of what opening the map costs. The readers belong to
		// the resource manager, which closes them with the map files themselves.
	}

	private companion object {
		const val MIN_ZOOM = 3
		const val MAX_ZOOM = 21
	}
}
