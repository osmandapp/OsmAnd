package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.HostException
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.views.Zoom
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import kotlin.math.max
import kotlin.math.min
import net.osmand.data.RotatedTileBox

abstract class BaseAndroidAutoScreen(carContext: CarContext) : Screen(carContext),
	DefaultLifecycleObserver {

	private val ANIMATION_RETURN_FROM_PREVIEW_TIME = 1500

	private var prevElevationAngle = 90f
	private var prevRotationAngle = 0f
	private var prevZoom: Zoom? = null
	private var prevMapLinkedToLocation = false
	private var prevStateSaved = false
	protected var firstTimeGetTemplate = true

	protected val app: OsmandApplication
		get() {
			return carContext.applicationContext as OsmandApplication
		}
	protected var contentLimit: Int = 0
		private set
	var session = app.carNavigationSession

	private fun initContentLimit() {
		val manager = carContext.getCarService(
			ConstraintManager::class.java
		)
		try {
			contentLimit = DEFAULT_CONTENT_LIMIT.coerceAtMost(
				manager.getContentLimit(getConstraintLimitType())
			)
		} catch (e: HostException) {
			contentLimit = DEFAULT_CONTENT_LIMIT
		}
		contentLimit = max(2, contentLimit)
	}

	final override fun onGetTemplate(): Template {
		if(firstTimeGetTemplate) {
			onFirstGetTemplate()
			firstTimeGetTemplate = false
		}
		return getTemplate()
	}

	abstract fun getTemplate(): Template

	protected open fun onFirstGetTemplate() {
		initContentLimit()
	}

	protected open fun shouldRestoreMapState(): Boolean = false

	protected open fun getConstraintLimitType(): Int {
		return ConstraintManager.CONTENT_LIMIT_TYPE_LIST
	}

	protected fun openRoutePreview(
		settingsAction: Action,
		result: SearchResult
	) {
		screenManager.pushForResult(
			RoutePreviewScreen(carContext, settingsAction, result, true)
		) { obj: Any? ->
			obj?.let {
				screenManager.popToRoot()
				startNavigation()
				finish()
			}
		}
	}

	protected open fun onSearchResultSelected(result: SearchResult) {
	}

	private fun startNavigation() {
		app.osmandMap.mapActions.startNavigation()
		val session = app.carNavigationSession
		session?.startNavigationScreen()
	}

	protected fun createSearchAction() = Action.Builder()
		.setIcon(
			CarIcon.Builder(
				IconCompat.createWithResource(
					carContext, R.drawable.ic_action_search_dark
				)
			).build()
		)
		.setOnClickListener { openSearch() }
		.build()

	private fun openSearch() {
		app.carNavigationSession?.let { navigationSession ->
			screenManager.pushForResult(
				SearchScreen(
					carContext,
					navigationSession.settingsAction
				)
			) { _: Any? -> }
		}
	}

	protected open fun adjustMapToRect(location: LatLon, mapRect: QuadRect) {
		app.mapViewTrackingUtilities.isMapLinkedToLocation = false
		Algorithms.extendRectToContainPoint(mapRect, location.longitude, location.latitude)
		app.carNavigationSession?.navigationCarSurface?.let { surfaceRenderer ->
			if (!mapRect.hasInitialState()) {
				val mapView = app.osmandMap.mapView
				val tb = RotatedTileBox(mapView.rotatedTileBox)
				val surfaceAdditionalWidth = surfaceRenderer.surfaceAdditionalWidth
				val adjustedPixWidth = tb.pixWidth - surfaceAdditionalWidth
				tb.setPixelDimensions(adjustedPixWidth, tb.pixHeight, surfaceRenderer.cachedRatioX, surfaceRenderer.cachedRatioY)
				tb.rotate = 0f;
				tb.setZoomAndAnimation(tb.zoom, 0.0, 0.0);
//				tb.mapDensity = surfaceRenderer.density.toDouble() * app.settings.MAP_DENSITY.get();
				tb.mapDensity = surfaceRenderer.density.toDouble() * app.osmandMap.mapDensity;
				val rtl = false; // panel is always on the left
				val leftPanel = tb.pixWidth / 2; // assume panel takes half screen
				val tileBoxWidthPx = tb.pixWidth - leftPanel;
				mapView.fitRectToMap(tb, mapRect.left, mapRect.right, mapRect.top, mapRect.bottom,
					tileBoxWidthPx, 0, 0, 0, rtl, 0.75f,true)
				mapView.refreshMap()
			}
		}
	}

	protected fun recenterMap() {
		session?.navigationCarSurface?.handleRecenter()
	}

	override fun onCreate(owner: LifecycleOwner) {
		if (shouldSaveMapState()) {
			saveMapState()
		}
	}

	override fun onDestroy(owner: LifecycleOwner) {
		if (prevMapLinkedToLocation != app.mapViewTrackingUtilities.isMapLinkedToLocation) {
			app.mapViewTrackingUtilities.isMapLinkedToLocation = prevMapLinkedToLocation
		}
		if (prevStateSaved) {
			restoreMapState()
		}
	}

	private fun shouldSaveMapState(): Boolean {
		return shouldRestoreMapState() && screenManager.screenStack
			.filterIsInstance<BaseAndroidAutoScreen>()
			.none { it != this && it.shouldRestoreMapState() }
	}

	private fun saveMapState() {
		val mapView = app.osmandMap.mapView
		prevMapLinkedToLocation = app.mapViewTrackingUtilities.isMapLinkedToLocation
		prevZoom = mapView.currentZoom
		prevRotationAngle = mapView.rotate
		prevElevationAngle = mapView.normalizeElevationAngle(mapView.elevationAngle)
		prevStateSaved = true
	}

	private fun restoreMapState() {
		val mapView = app.osmandMap.mapView
		val locationProvider = app.locationProvider
		val lastKnownLocation = locationProvider.lastKnownLocation
		mapView.animateToState(
			lastKnownLocation?.latitude ?: mapView.latitude,
			lastKnownLocation?.longitude ?: mapView.longitude,
			prevZoom ?: mapView.currentZoom,
			prevRotationAngle,
			prevElevationAngle,
			ANIMATION_RETURN_FROM_PREVIEW_TIME.toLong(),
			false)
	}

	companion object {
		private const val DEFAULT_CONTENT_LIMIT = 12
	}
}