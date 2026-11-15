package net.osmand.plus.auto.cluster

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.HostException
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import net.osmand.PlatformUtil
import net.osmand.data.RotatedTileBox
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.SurfaceRenderer
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import kotlin.math.max

/**
 * Draws the map of the main Android Auto screen on the instrument cluster.
 * The map engine has a single OpenGL renderer, so the cluster shows the main screen's frame,
 * cropped around the my location point, instead of rendering a map of its own.
 */
class ClusterRenderer(
	private val carContext: CarContext,
	lifecycle: Lifecycle
) : DefaultLifecycleObserver {

	private var surface: Surface? = null
	private var surfaceContainer: SurfaceContainer? = null
	private var visibleArea: Rect? = null

	private val surfaceCallback = object : SurfaceCallback {
		override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
			synchronized(this@ClusterRenderer) {
				LOG.info("Cluster surface available ${surfaceContainer.width}x${surfaceContainer.height} dpi=${surfaceContainer.dpi}")
				surface?.release()
				this@ClusterRenderer.surfaceContainer = surfaceContainer
				surface = surfaceContainer.surface
			}
			requestMainFrame()
		}

		override fun onVisibleAreaChanged(visibleArea: Rect) {
			synchronized(this@ClusterRenderer) {
				LOG.info("Cluster visible area $visibleArea")
				this@ClusterRenderer.visibleArea = visibleArea
			}
			requestMainFrame()
		}

		override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
			synchronized(this@ClusterRenderer) {
				LOG.info("Cluster surface destroyed")
				surface?.release()
				surface = null
				this@ClusterRenderer.surfaceContainer = null
			}
		}
	}

	init {
		lifecycle.addObserver(this)
	}

	override fun onCreate(owner: LifecycleOwner) {
		try {
			carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
		} catch (e: SecurityException) {
			LOG.error("Cluster setSurfaceCallback failed", e)
		} catch (e: HostException) {
			LOG.error("Cluster setSurfaceCallback failed", e)
		}
	}

	@Synchronized
	fun renderFrame(mainRenderer: SurfaceRenderer, tileBox: RotatedTileBox, drawSettings: DrawSettings) {
		val surface = surface?.takeIf { it.isValid } ?: return
		val container = surfaceContainer ?: return
		val surfaceArea = mainRenderer.surfaceArea ?: return
		if (surfaceArea.isEmpty) {
			return
		}
		val target = visibleArea?.takeUnless { it.isEmpty } ?: Rect(0, 0, container.width, container.height)
		val source = getSourceRect(surfaceArea, mainRenderer.myLocationX, mainRenderer.myLocationY, target)
		val scale = target.width() / source.width()

		val canvas = surface.lockCanvas(null)
		try {
			canvas.drawColor(Color.LTGRAY)
			canvas.save()
			canvas.clipRect(target)
			canvas.translate(target.left.toFloat(), target.top.toFloat())
			canvas.scale(scale, scale)
			canvas.translate(-source.left, -source.top)
			mainRenderer.drawMap(canvas, tileBox, drawSettings)
			canvas.restore()
		} catch (e: Exception) {
			LOG.error("Cluster frame failed", e)
		} finally {
			surface.unlockCanvasAndPost(canvas)
		}
	}

	/**
	 * The largest part of the main surface with the cluster's aspect ratio, centered on my location horizontally
	 * and keeping it at the same relative height as on the main screen.
	 * The whole surface is used, as the map is drawn under the host UI too:
	 * the visible area alone would be stretched and blurred.
	 */
	private fun getSourceRect(surfaceArea: Rect, myLocationX: Float, myLocationY: Float, target: Rect): RectF {
		val scale = max(
			target.width().toFloat() / surfaceArea.width(),
			target.height().toFloat() / surfaceArea.height()
		)
		val width = target.width() / scale
		val height = target.height() / scale
		val locationRatioY = ((myLocationY - surfaceArea.top) / surfaceArea.height()).coerceIn(0f, 1f)
		val left = clamp(myLocationX - width / 2, surfaceArea.left.toFloat(), surfaceArea.right - width)
		val top = clamp(myLocationY - height * locationRatioY, surfaceArea.top.toFloat(), surfaceArea.bottom - height)
		return RectF(left, top, left + width, top + height)
	}

	private fun clamp(value: Float, min: Float, max: Float): Float {
		return if (max < min) min else value.coerceIn(min, max)
	}

	private fun requestMainFrame() {
		val app = carContext.applicationContext as OsmandApplication
		app.carNavigationSession?.mapView?.refreshMap()
	}

	companion object {
		private val LOG = PlatformUtil.getLog(ClusterRenderer::class.java)
	}
}