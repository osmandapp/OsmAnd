package net.osmand.plus.wear

import android.graphics.Bitmap

import net.osmand.data.RotatedTileBox
import net.osmand.plus.OsmandApplication
import net.osmand.plus.render.MapRenderRepositories
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType

import kotlin.math.floor
import kotlin.math.log2

/**
 * The legacy renderer, drawing the watch's map into a bitmap.
 *
 * It wants no GL context and no surface: a tile box says where to look and how large, and a
 * bitmap of that size comes back on the calling thread. That is the whole of what the watch
 * needs, where the OpenGL path has to stand a second core context up to get it.
 *
 * It renders alongside the phone's own map rather than sharing with it. Sharing is not possible:
 * the renderer holds one bitmap and one tile box, and the phone is already using them. So this
 * keeps its own, with its own file handles - a reader per purpose is how OsmAnd already lets
 * search, routing and rendering read the same map at once - over the indexes the phone has
 * already parsed.
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
			// North up, and left that way: a rotated box costs the renderer more and the watch
			// has no heading of the phone's to follow yet.
			.setRotate(0f)
			.setPixelDimensions(width, height, 0.5f, 0.5f)
			.build()
		return true
	}

	override fun frame(): Bitmap? {
		val own = repositories ?: return null
		val current = box ?: return null
		// Copied, because loadMap keeps the box it is given and this one keeps being edited by
		// gestures arriving on another thread.
		own.loadMap(RotatedTileBox(current), null)
		drawn++
		return own.bitmap
	}

	/**
	 * Unlike the core renderer, this one's float zoom really is a power of two: the tile box
	 * scales by 2^(zoom + zoomFloatPart). So a scale factor converts by its logarithm, and the
	 * watch's own preview of the gesture lands exactly where the next frame puts the map.
	 */
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
		// The ground that is to end up in the middle is the ground the drag came from.
		val moved = current.getLatLonFromPixel(
			current.centerPixelX - dx, current.centerPixelY - dy)
		current.setLatLonCenter(moved.latitude, moved.longitude)
	}

	override fun followPhone() {
		val current = box ?: return
		val phone = app.osmandMap.mapView.currentRotatedTileBox
		current.setLatLonCenter(phone.latitude, phone.longitude)
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
