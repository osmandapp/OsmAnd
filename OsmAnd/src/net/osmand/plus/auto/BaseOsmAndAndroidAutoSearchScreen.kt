package net.osmand.plus.auto

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import net.osmand.plus.auto.SearchHelper.SearchHelperListener
import net.osmand.search.core.SearchCoreFactory

abstract class BaseOsmAndAndroidAutoSearchScreen(
	carContext: CarContext) :
	BaseOsmAndAndroidAutoScreen(carContext), SearchHelperListener {

    protected var loading = false
    protected val searchHelper: SearchHelper by lazy(::createSearchHelper)

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
    }

    private fun createSearchHelper(): SearchHelper {
        val searchHelper = SearchHelper(
            app,
            true,
            contentLimit,
            2,
            SearchCoreFactory.MAX_DEFAULT_SEARCH_RADIUS,
            false)
        searchHelper.listener = this
        searchHelper.setupSearchSettings(true)
        return searchHelper
    }
}