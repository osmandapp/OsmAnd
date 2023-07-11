package net.osmand.plus.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.search.core.SearchResult

abstract class BaseOsmAndAndroidAutoScreen(carContext: CarContext) : Screen(carContext) {

    protected val app: OsmandApplication
        get() {
            return carContext.applicationContext as OsmandApplication
        }
    protected var contentLimit: Int = 0
        private set

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
        surfaceRenderer: SurfaceRenderer,
        result: SearchResult) {
        screenManager.pushForResult(
            RoutePreviewScreen(carContext, settingsAction, surfaceRenderer, result)
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
                    navigationSession.settingsAction,
                    navigationSession.navigationCarSurface)) { _: Any? -> }
        }
    }

    companion object {
        private const val DEFAULT_CONTENT_LIMIT = 12
    }
}