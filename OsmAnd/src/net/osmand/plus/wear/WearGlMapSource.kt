package net.osmand.plus.wear

import android.graphics.Bitmap

import net.osmand.core.android.AtlasMapRendererView
import net.osmand.core.android.MapRendererContext
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.MapStylesCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.ZoomLevel
import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.corenative.NativeCoreContext

import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

/**
 * The OpenGL renderer, drawing the watch's map in a second core context.
 *
 * What the watch sees is then exactly what the phone draws, which the legacy renderer cannot
 * promise. The price is memory: measured on a Galaxy S23, about 87 MB of pss, most of it the
 * second set of tile and symbol providers rather than the frame buffers. The phone's own map
 * keeps rendering throughout - the core is happy to run two, unlike Android Auto, which takes
 * the one renderer over.
 */
class WearGlMapSource(private val app: OsmandApplication) : WearMapSource {

	private var context: MapRendererContext? = null
	private var view: AtlasMapRendererView? = null

	@Volatile
	override var drawn = 0
		private set

	/** The GPU may already be drawing when a gesture lands; that frame cannot contain it. */
	override val framesInFlight = 2

	override fun open(width: Int, height: Int, density: Float): Boolean {
		if (!NativeCoreContext.isInit()) {
			LOG.warn("Watch asked for the map, but the OpenGL core is not initialised")
			return false
		}
		val collections = NativeCoreContext.getObfsCollections() ?: return false

		val rendererContext = MapRendererContext(app, density)
		rendererContext.setupObfMap(MapStylesCollection(), collections)

		val mapView = app.osmandMap.mapView
		val rendererView = AtlasMapRendererView(app)
		rendererContext.presetMapRendererOptions(rendererView, false)
		rendererView.setupRenderer(app, width, height, null)
		rendererView.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.minZoom))
		rendererView.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.maxZoom))
		rendererView.setAzimuth(0f)
		rendererView.setElevationAngle(90f)
		rendererView.setZoom(mapView.zoom.toFloat())
		val box = mapView.currentRotatedTileBox
		rendererView.setTarget(PointI(box.center31X, box.center31Y))
		// Drawn far more often than it is sent: redrawing is GPU work that the measurements
		// found cheap, and it is what decides how soon a gesture is answered. Encoding, which
		// is the expensive half, still happens only as often as frames are sent.
		rendererView.setMaximumFrameRate(RENDER_FRAMES_PER_SECOND)
		rendererView.addListener(object : MapRendererView.MapRendererViewListener {
			override fun onUpdateFrame(view: MapRendererView) {}
			override fun onFrameReady(view: MapRendererView) {
				drawn++
			}
		})
		rendererContext.setMapRendererView(rendererView)

		context = rendererContext
		view = rendererView
		return true
	}

	override fun frame(): Bitmap? = view?.bitmap

	/**
	 * The core's float zoom is a level plus visualZoom - 1, so the ground it shows is linear in
	 * visualZoom rather than in powers of two: adding a tenth to the zoom enlarges by 1.1, not
	 * by 1.072. Doing the arithmetic in log2 of the scale and encoding the result back is what
	 * makes the watch's own preview of the gesture land on the frame that follows it.
	 */
	override fun zoom(factor: Float) {
		val renderer = view ?: return
		if (factor <= 0f) {
			return
		}
		val wanted = log2(scaleOf(renderer.zoom) * factor)
		val level = floor(wanted)
		val visual = 2.0.pow(wanted - level).toFloat()
		renderer.setZoom((level.toFloat() + visual - 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
	}

	/** How much ground a float zoom shows, in the renderer's own level-plus-visualZoom terms. */
	private fun scaleOf(zoom: Float): Double {
		val level = floor(zoom.toDouble())
		return 2.0.pow(level) * (1.0 + (zoom - level))
	}

	/**
	 * The screen point the finger started from is asked for its location, and the target moves
	 * by the difference. Doing the arithmetic in 31-coordinates would need the zoom and the
	 * projection, which the renderer already holds.
	 */
	override fun pan(dx: Float, dy: Float) {
		val renderer = view ?: return
		val centre = renderer.targetScreenPosition ?: return
		val from = PointI()
		val to = PointI()
		if (renderer.getLocationFromScreenPoint(centre, from)
				&& renderer.getLocationFromScreenPoint(
					PointI(centre.x - dx.toInt(), centre.y - dy.toInt()), to)) {
			val target = renderer.target ?: return
			renderer.setTarget(PointI(
				target.x + (to.x - from.x),
				target.y + (to.y - from.y)))
		}
	}

	override fun followPhone() {
		val renderer = view ?: return
		val box = app.osmandMap.mapView.currentRotatedTileBox
		renderer.setTarget(PointI(box.center31X, box.center31Y))
	}

	override fun close() {
		runCatching { context?.releaseMapRendererView(view) }
		runCatching { view?.stopRenderer() }
		view = null
		context = null
	}

	private companion object {
		private val LOG = net.osmand.PlatformUtil.getLog(WearGlMapSource::class.java)
		const val RENDER_FRAMES_PER_SECOND = 15
		const val MIN_ZOOM = 3f
		const val MAX_ZOOM = 21f
	}
}
