package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import net.osmand.plus.auto.SearchHelper
import net.osmand.plus.auto.SearchHelper.SearchHelperListener
import net.osmand.search.core.SearchCoreFactory.MAX_DEFAULT_SEARCH_RADIUS

abstract class BaseSearchScreen(carContext: CarContext) : BaseAndroidAutoScreen(carContext),
	SearchHelperListener {

	protected var loading = false
	protected val searchHelper: SearchHelper by lazy(::createSearchHelper)

	override fun getConstraintLimitType(): Int {
		return ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
	}

	private fun createSearchHelper(): SearchHelper {
		val searchHelper = SearchHelper(
			app, true, contentLimit, 2,
			MAX_DEFAULT_SEARCH_RADIUS, false
		)
		searchHelper.listener = this
		searchHelper.setupSearchSettings(true)
		return searchHelper
	}
}