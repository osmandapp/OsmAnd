package net.osmand.plus.auto.cluster

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import net.osmand.Location
import net.osmand.core.android.AtlasMapRendererView
import net.osmand.core.android.MapRendererContext
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.AnimatedValue
import net.osmand.core.jni.MapStylesCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.SwigUtilities
import net.osmand.core.jni.TimingFunction
import net.osmand.core.jni.ZoomLevel
import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.util.MapUtils
import kotlin.math.log2

/**
 * A map renderer of its own for the instrument cluster, next to the one of the main car screen:
 * the cluster keeps its own size, zoom and rotation and follows my location while the main screen
 * is browsed. Both renderers read the same loaded map files.
 */
class ClusterMap private constructor(
	app: OsmandApplication,
	private val rendererContext: MapRendererContext,
	private val view: AtlasMapRendererView,
	density: Float
) {

	private val overlays = ClusterMapOverlays(app, view, density)
	private val handler = Handler(Looper.getMainLooper())
	private val animationKey = SwigUtilities.getOnSurfaceIconKey(1)
	private var lastFollowTime = 0L
	private var userZoomDelta = 0f

	private val renderAnimationFrame = object : Runnable {
		override fun run() {
			view.requestRender()
			if (!view.isMapAnimationFinished || !view.isMapMarkersAnimationFinished) {
				handler.postDelayed(this, FRAME_INTERVAL_MS)
			}
		}
	}

	val bitmap: Bitmap?
		get() = view.bitmap

	fun setNightMode(nightMode: Boolean) {
		rendererContext.setNightMode(nightMode)
		overlays.setNightMode(nightMode)
	}

	fun updateMapSettings(forceUpdateProviders: Boolean) {
		rendererContext.updateMapSettings(forceUpdateProviders)
	}

	fun updateProfile() {
		overlays.updateProfile()
	}

	/**
	 * Tilts the map, 90 degrees shows it flat.
	 */
	fun setElevationAngle(elevationAngle: Float) {
		if (elevationAngle == view.elevationAngle) {
			return
		}
		val animator = view.mapAnimator
		animator.cancelCurrentAnimation(animationKey, AnimatedValue.ElevationAngle)
		animator.animateElevationAngleTo(elevationAngle, TILT_ANIMATION_S, TimingFunction.Linear, animationKey)
		view.resumeMapAnimation()
		renderAnimationFrames()
	}

	fun setFrameListener(onFrameReady: () -> Unit) {
		view.addListener(object : MapRendererView.MapRendererViewListener {
			override fun onUpdateFrame(mapRenderer: MapRendererView) {}

			override fun onFrameReady(mapRenderer: MapRendererView) {
				onFrameReady()
			}
		})
	}

	/**
	 * Keeps my location at [positionRatioY] of the visible area height, heading up while moving.
	 * The camera glides to each new fix over the time since the previous one, so it arrives
	 * as the next fix comes in.
	 */
	fun follow(location: Location, visibleArea: Rect, positionRatioY: Float) {
		val location31 = NativeUtilities.getPoint31FromLatLon(location.latitude, location.longitude)
		val point = PointI(
			visibleArea.centerX(),
			visibleArea.top + (visibleArea.height() * positionRatioY).toInt()
		)
		val now = SystemClock.uptimeMillis()
		val sinceLastFollow = now - lastFollowTime
		lastFollowTime = now

		val zoom = getZoomForSpeed(location) + userZoomDelta
		cancelCameraAnimations()
		if (!isAt(point) || sinceLastFollow > MAX_ANIMATION_MS) {
			if (isMoving(location)) {
				view.setAzimuth(location.bearing)
			}
			view.setZoom(zoom)
			view.setMapTarget(point, location31)
			overlays.updateMyLocation(location, location31, 0f)
			view.requestRender()
			return
		}
		val duration = sinceLastFollow / 1000f
		val animator = view.mapAnimator
		animator.animateTargetTo(location31, duration, TimingFunction.Linear, animationKey)
		if (isMoving(location)) {
			val azimuth = view.azimuth
			val toAzimuth = azimuth + MapUtils.unifyRotationDiff(azimuth, location.bearing)
			animator.animateAzimuthTo(toAzimuth, duration, TimingFunction.Linear, animationKey)
		}
		if (zoom != view.zoom) {
			animator.animateZoomTo(zoom, ZOOM_ANIMATION_S, TimingFunction.EaseOutQuadratic, animationKey)
		}
		view.resumeMapAnimation()
		overlays.updateMyLocation(location, location31, duration)
		renderAnimationFrames()
	}

	/**
	 * Whether the camera keeps my location at [point]. A renderer that hasn't drawn its first frame
	 * yet drops the pixel, so this is checked against the renderer and not remembered.
	 */
	private fun isAt(point: PointI): Boolean {
		val fixedPixel = view.targetScreenPosition
		return fixedPixel.x == point.x && fixedPixel.y == point.y
	}

	private fun renderAnimationFrames() {
		handler.removeCallbacks(renderAnimationFrame)
		handler.post(renderAnimationFrame)
	}

	/**
	 * Zooms the cluster in or out on top of the speed based zoom, for scale gestures sent by the car.
	 */
	fun scaleBy(scaleFactor: Float) {
		if (scaleFactor > 0) {
			val delta = userZoomDelta + log2(scaleFactor)
			userZoomDelta = delta.coerceIn(MIN_USER_ZOOM_DELTA, MAX_USER_ZOOM_DELTA)
		}
	}

	fun updateRoute() {
		overlays.updateRoute()
	}

	fun release() {
		handler.removeCallbacks(renderAnimationFrame)
		view.stopMapAnimation()
		overlays.release()
		rendererContext.releaseMapRendererView(view)
	}

	private fun cancelCameraAnimations() {
		val animator = view.mapAnimator
		for (value in arrayOf(AnimatedValue.Target, AnimatedValue.Azimuth, AnimatedValue.Zoom)) {
			animator.cancelCurrentAnimation(animationKey, value)
		}
	}

	private fun isMoving(location: Location): Boolean {
		return location.hasBearing() && location.hasSpeed() && location.speed >= MIN_HEADING_SPEED
	}

	private fun getZoomForSpeed(location: Location): Float {
		val speed = if (location.hasSpeed()) location.speed else 0f
		return when {
			speed < CITY_SPEED -> CITY_ZOOM
			speed < ROAD_SPEED -> ROAD_ZOOM
			else -> HIGHWAY_ZOOM
		}
	}

	companion object {

		private const val MAX_FRAME_RATE = 20
		private const val FRAME_INTERVAL_MS = 1000L / MAX_FRAME_RATE
		private const val MAX_ANIMATION_MS = 2000L
		private const val ZOOM_ANIMATION_S = 0.8f
		private const val TILT_ANIMATION_S = 0.5f
		private const val MIN_HEADING_SPEED = 1f
		private const val CITY_SPEED = 50f / 3.6f
		private const val ROAD_SPEED = 90f / 3.6f
		private const val CITY_ZOOM = 17f
		private const val ROAD_ZOOM = 16f
		private const val HIGHWAY_ZOOM = 15f
		private const val MIN_USER_ZOOM_DELTA = -5f
		private const val MAX_USER_ZOOM_DELTA = 3f

		fun create(app: OsmandApplication, width: Int, height: Int, density: Float): ClusterMap? {
			val collections = NativeCoreContext.getObfsCollections()
			if (!NativeCoreContext.isInit() || collections == null) {
				return null
			}
			val rendererContext = MapRendererContext(app, density)
			rendererContext.setupObfMap(MapStylesCollection(), collections)

			val mapView = app.osmandMap.mapView
			val view = AtlasMapRendererView(app)
			rendererContext.presetMapRendererOptions(view, false)
			view.setupRenderer(app, width, height, null)
			view.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.minZoom))
			view.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.maxZoom))
			view.setElevationAngle(app.settings.CLUSTER_MAP_ELEVATION.get())
			view.setFlatEarth(!app.settings.SPHERICAL_MAP.get())
			view.setMaximumFrameRate(MAX_FRAME_RATE)
			rendererContext.setMapRendererView(view)
			return ClusterMap(app, rendererContext, view, density)
		}
	}
}