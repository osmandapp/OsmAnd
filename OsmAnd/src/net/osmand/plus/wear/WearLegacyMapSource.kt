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
			.setRotate(phone.rotate)
			.setPixelDimensions(width, height, 0.5f, 0.5f)
			.build()
		return true
	}

	// A copy, because the box this returns is read while gestures keep editing the live one.
	override fun overlayBox(): RotatedTileBox? = box?.let { RotatedTileBox(it) }

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

	/**
	 * Zoom is stepped down from the closest until the whole thing is in view rather than
	 * computed: the projection that decides what fits is the tile box's own, so asking it is
	 * both shorter and exactly right, and there are only eighteen steps to try.
	 */
	override fun fit(bounds: QuadRect) {
		val current = box ?: return
		// The phone's heading, read now rather than left at whatever it was when the stream
		// opened: OsmAnd has four map orientations and keeps the profile's through a route
		// preview, so this is how the two screens end up showing the route the same way up.
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
	 * Whether all of [bounds] lands on the screen, asked in screen pixels rather than in degrees.
	 *
	 * The map keeps whatever heading the phone is on - OsmAnd has four of them and does not force
	 * north up for a route preview either - and a turned box reports its bounds in degrees as
	 * the extent of the turned rectangle, which reaches out to its diagonal. Judged by that, a
	 * route half as wide again as the screen appears to fit.
	 *
	 * The corners are enough: the projection is affine, so the whole rectangle lands inside the
	 * shape its corners make, and the route lands inside the rectangle.
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

	/**
	 * Takes the phone's heading as well as its position. The two screens show the same ground,
	 * and showing it turned differently is harder to read than either on its own - the watch is
	 * glanced at while the phone is in view.
	 */
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
