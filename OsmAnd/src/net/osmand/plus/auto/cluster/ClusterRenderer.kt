package net.osmand.plus.auto.cluster

import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.HostException
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.StateChangedListener
import net.osmand.core.android.MapRendererContext.MapSettingsListener
import net.osmand.data.ValueHolder
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.profiles.ProfileIconColors
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.MapPosition
import net.osmand.plus.views.corenative.NativeCoreContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shows [ClusterMap] on the instrument cluster surface.
 */
class ClusterRenderer(
	private val carContext: CarContext,
	lifecycle: Lifecycle
) : DefaultLifecycleObserver {

	private val app = carContext.applicationContext as OsmandApplication
	private val handler = Handler(Looper.getMainLooper())
	private val framePending = AtomicBoolean()

	private var surface: Surface? = null
	private var surfaceContainer: SurfaceContainer? = null
	private var visibleArea: Rect? = null
	private var map: ClusterMap? = null

	private var firstFrameDrawn = false

	private val drawFrame = Runnable {
		framePending.set(false)
		drawFrame()
	}

	private val locationListener = OsmAndLocationListener { location ->
		handler.post { follow(location) }
	}

	private val mapSettingsListener = MapSettingsListener { forceUpdateProviders ->
		handler.post { map?.updateMapSettings(forceUpdateProviders) }
	}

	// Preferences keep weak references to their listeners, so these live in fields
	private val appModeListener = StateChangedListener<ApplicationMode> { onProfileChanged() }
	private val navigationIconListener = StateChangedListener<String> { onProfileChanged() }
	private val iconColorListener = StateChangedListener<ProfileIconColors> { onProfileChanged() }
	private val customIconColorListener = StateChangedListener<String> { onProfileChanged() }
	private val positionPlacementListener = StateChangedListener<Int> { handler.post { followLastLocation() } }
	private val elevationListener = StateChangedListener<Float> { handler.post { updateElevationAngle() } }

	private val routeListener = object : IRouteInformationListener {
		override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>?) {
			handler.post { map?.updateRoute() }
		}

		override fun routeWasCancelled() {
			handler.post { map?.updateRoute() }
		}

		override fun routeWasFinished() {
			handler.post { map?.updateRoute() }
		}
	}

	private val surfaceCallback = object : SurfaceCallback {
		override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
			LOG.info("Cluster surface available ${surfaceContainer.width}x${surfaceContainer.height} dpi=${surfaceContainer.dpi}")
			releaseMap()
			this@ClusterRenderer.surfaceContainer = surfaceContainer
			surface = surfaceContainer.surface
			createMap(surfaceContainer)
		}

		override fun onVisibleAreaChanged(visibleArea: Rect) {
			LOG.info("Cluster visible area $visibleArea")
			this@ClusterRenderer.visibleArea = visibleArea
			followLastLocation()
		}

		override fun onStableAreaChanged(stableArea: Rect) {
			LOG.info("Cluster stable area $stableArea")
		}

		override fun onScroll(distanceX: Float, distanceY: Float) {
			LOG.info("Cluster scroll $distanceX, $distanceY")
		}

		override fun onFling(velocityX: Float, velocityY: Float) {
			LOG.info("Cluster fling $velocityX, $velocityY")
		}

		override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
			LOG.info("Cluster scale $scaleFactor at $focusX, $focusY")
			map?.scaleBy(scaleFactor)
			followLastLocation()
		}

		override fun onClick(x: Float, y: Float) {
			LOG.info("Cluster click $x, $y")
		}

		override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
			LOG.info("Cluster surface destroyed")
			releaseMap()
			surface = null
			this@ClusterRenderer.surfaceContainer = null
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

	override fun onDestroy(owner: LifecycleOwner) {
		releaseMap()
	}

	private fun createMap(container: SurfaceContainer) {
		val density = container.dpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
		val map = ClusterMap.create(app, container.width, container.height, density)
		if (map == null) {
			if (app.appInitializer.isAppInitializing) {
				LOG.info("Cluster map waits for the app to initialize")
				app.appInitializer.addOnFinishListener {
					if (surfaceContainer === container && this.map == null) {
						createMap(container)
					}
				}
			} else {
				LOG.warn("Cluster map is not available: the OpenGL core is not initialized")
			}
			return
		}
		firstFrameDrawn = false
		map.setNightMode(carContext.isDarkMode)
		map.setFrameListener { requestFrame() }
		map.updateRoute()
		this.map = map
		app.locationProvider.addLocationListener(locationListener)
		app.routingHelper.addListener(routeListener)
		addSettingsListeners()
		followLastLocation()
	}

	private fun releaseMap() {
		val map = map ?: return
		this.map = null
		app.locationProvider.removeLocationListener(locationListener)
		app.routingHelper.removeListener(routeListener)
		removeSettingsListeners()
		handler.removeCallbacks(drawFrame)
		framePending.set(false)
		map.release()
	}

	private fun addSettingsListeners() {
		val settings = app.settings
		NativeCoreContext.getMapRendererContext()?.addMapSettingsListener(mapSettingsListener)
		settings.APPLICATION_MODE.addListener(appModeListener)
		settings.NAVIGATION_ICON.addListener(navigationIconListener)
		settings.ICON_COLOR.addListener(iconColorListener)
		settings.CUSTOM_ICON_COLOR.addListener(customIconColorListener)
		settings.POSITION_PLACEMENT_ON_MAP.addListener(positionPlacementListener)
		settings.CLUSTER_POSITION_PLACEMENT_ON_MAP.addListener(positionPlacementListener)
		settings.LAST_KNOWN_AA_MAP_ELEVATION.addListener(elevationListener)
		settings.CLUSTER_MAP_ELEVATION.addListener(elevationListener)
	}

	private fun removeSettingsListeners() {
		val settings = app.settings
		NativeCoreContext.getMapRendererContext()?.removeMapSettingsListener(mapSettingsListener)
		settings.APPLICATION_MODE.removeListener(appModeListener)
		settings.NAVIGATION_ICON.removeListener(navigationIconListener)
		settings.ICON_COLOR.removeListener(iconColorListener)
		settings.CUSTOM_ICON_COLOR.removeListener(customIconColorListener)
		settings.POSITION_PLACEMENT_ON_MAP.removeListener(positionPlacementListener)
		settings.CLUSTER_POSITION_PLACEMENT_ON_MAP.removeListener(positionPlacementListener)
		settings.LAST_KNOWN_AA_MAP_ELEVATION.removeListener(elevationListener)
		settings.CLUSTER_MAP_ELEVATION.removeListener(elevationListener)
	}

	private fun updateElevationAngle() {
		map?.setElevationAngle(app.settings.CLUSTER_MAP_ELEVATION.get())
	}

	private fun onProfileChanged() {
		handler.post {
			map?.updateProfile()
			followLastLocation()
		}
	}

	private fun followLastLocation() {
		app.locationProvider.lastKnownLocation?.let { follow(it) }
	}

	private fun follow(location: Location) {
		val map = map ?: return
		map.setNightMode(carContext.isDarkMode)
		map.follow(location, getMapArea(), getMyLocationRatioY())
	}

	/**
	 * The cluster map always turns with the heading, so the automatic placement keeps my location at the bottom.
	 */
	private fun getMyLocationRatioY(): Float {
		val placement = app.settings.CLUSTER_POSITION_PLACEMENT_ON_MAP.get()
		val position = if (placement == OsmandSettings.POSITION_PLACEMENT_CENTER) MapPosition.CENTER else MapPosition.BOTTOM
		return position.ratioY
	}

	private fun getMapArea(): Rect {
		val area = visibleArea
		if (area != null && !area.isEmpty) {
			return area
		}
		val container = surfaceContainer
		return if (container != null) Rect(0, 0, container.width, container.height) else Rect()
	}

	/**
	 * Called on the renderer's thread for every frame; frames that come faster than they are drawn are skipped.
	 */
	private fun requestFrame() {
		if (framePending.compareAndSet(false, true)) {
			handler.post(drawFrame)
		}
	}

	private fun drawFrame() {
		val surface = surface?.takeIf { it.isValid } ?: return
		val bitmap = map?.bitmap ?: return
		val canvas = try {
			surface.lockCanvas(null)
		} catch (e: RuntimeException) {
			// The host may still hold the surface for a process that died, e.g. after a reinstall
			LOG.error("Cluster surface can't be drawn", e)
			return
		}
		try {
			canvas.drawColor(Color.BLACK)
			canvas.drawBitmap(bitmap, 0f, 0f, null)
		} catch (e: Exception) {
			LOG.error("Cluster frame failed", e)
		} finally {
			surface.unlockCanvasAndPost(canvas)
		}
		if (!firstFrameDrawn) {
			// Placing my location needs the renderer to have drawn once
			firstFrameDrawn = true
			followLastLocation()
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(ClusterRenderer::class.java)
	}
}