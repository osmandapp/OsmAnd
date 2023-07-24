package net.osmand.plus.auto

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
import net.osmand.search.core.SearchResult
import kotlin.math.abs

abstract class BaseOsmAndAndroidAutoScreen(
	carContext: CarContext) : Screen(carContext) {

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
			ConstraintManager::class.java)
		contentLimit = DEFAULT_CONTENT_LIMIT.coerceAtMost(
			manager.getContentLimit(getConstraintLimitType()))
	}

	protected open fun getConstraintLimitType(): Int {
		return ConstraintManager.CONTENT_LIMIT_TYPE_LIST
	}

	protected fun openRoutePreview(
		settingsAction: Action,
		result: SearchResult) {
		screenManager.pushForResult(
			RoutePreviewScreen(carContext, settingsAction, result)
		) { obj: Any? ->
			obj?.let {
				onSearchResultSelected(result)
				startNavigation()
				finish()
			}
		}
	}

	protected open fun onSearchResultSelected(result: SearchResult) {
	}

	private fun startNavigation() {
		app.osmandMap.mapLayers.mapControlsLayer.startNavigation()
		val session = app.carNavigationSession
		if (session != null && session.hasStarted()) {
			session.startNavigation()
		}
	}

	protected fun createSearchAction() = Action.Builder()
		.setIcon(
			CarIcon.Builder(
				IconCompat.createWithResource(
					carContext, R.drawable.ic_action_search_dark)).build())
		.setOnClickListener { openSearch() }
		.build()

	private fun openSearch() {
		app.carNavigationSession?.let { navigationSession ->
			screenManager.pushForResult(
				SearchScreen(
					carContext,
					navigationSession.settingsAction)) { _: Any? -> }
		}
	}

	protected fun adjustMapToRect(location: LatLon, mapRect: QuadRect) {
		app.carNavigationSession?.navigationCarSurface?.let { surfaceRenderer ->
			if (!mapRect.hasInitialState()) {
				val mapView = app.osmandMap.mapView
				val tileBox = mapView.rotatedTileBox
				val leftRightDelta = abs(location.longitude - mapRect.left)
					.coerceAtLeast(abs(location.longitude - mapRect.right))
				val topBottomDelta = abs(location.latitude - mapRect.top)
					.coerceAtLeast(abs(location.latitude - mapRect.bottom))
				val rectLeft = location.longitude - leftRightDelta
				val rectRight = location.longitude + leftRightDelta
				val rectWidth = rectRight - rectLeft
				val coef: Double = surfaceRenderer.visibleAreaWidth / tileBox.pixWidth
				val left = rectLeft - rectWidth * coef
				val right = rectRight + rectWidth * coef
				val rectInVisibleArea = QuadRect()
				rectInVisibleArea.left = left
				rectInVisibleArea.right = right
				rectInVisibleArea.top = location.latitude - topBottomDelta
				rectInVisibleArea.bottom = location.latitude + topBottomDelta
				mapView.fitRectToMap(
					rectInVisibleArea.left,
					rectInVisibleArea.right,
					rectInVisibleArea.top,
					rectInVisibleArea.bottom,
					tileBox.pixWidth,
					tileBox.pixHeight,
					0)
			}
		}
	}

	companion object {
		private const val DEFAULT_CONTENT_LIMIT = 12
	}
}