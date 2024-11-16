package net.osmand.plus.auto.screens

import android.graphics.Rect
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.enums.CompassMode
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms

abstract class BaseAndroidAutoScreen(carContext: CarContext) : Screen(carContext) {

	protected val app: OsmandApplication
		get() {
			return carContext.applicationContext as OsmandApplication
		}
	protected var contentLimit: Int = 0
		private set
	var session = app.carNavigationSession

	init {
		initContentLimit()
	}

	private fun initContentLimit() {
		val manager = carContext.getCarService(
			ConstraintManager::class.java
		)
		contentLimit = DEFAULT_CONTENT_LIMIT.coerceAtMost(
			manager.getContentLimit(getConstraintLimitType())
		)
	}

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
				startNavigation()
				finish()
			}
		}
	}

	protected open fun onSearchResultSelected(result: SearchResult) {
	}

	private fun startNavigation() {
		app.osmandMap.mapLayers.mapActionsHelper.startNavigation()
		val session = app.carNavigationSession
		session?.startNavigation()
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
		app.getSettings().setCompassMode(CompassMode.NORTH_IS_UP);
		Algorithms.extendRectToContainPoint(mapRect, location.longitude, location.latitude)
		app.carNavigationSession?.navigationCarSurface?.let { surfaceRenderer ->
			if (!mapRect.hasInitialState()) {
				val mapView = app.osmandMap.mapView
				val tb = mapView.rotatedTileBox
				tb.setCenterLocation(tb.centerPixelX.toFloat() / tb.pixWidth, 0.5f)
				tb.rotate = 0f;
				mapView.fitRectToMap(
					tb,
					mapRect.left, mapRect.right, mapRect.top, mapRect.bottom,
					0, 0, true, true
				)
				mapView.refreshMap()
			}
		}
	}

	protected fun recenterMap() {
		session?.navigationCarSurface?.handleRecenter()
	}

	companion object {
		private const val DEFAULT_CONTENT_LIMIT = 12
	}
}