package net.osmand.plus.auto

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import net.osmand.data.LatLon
import net.osmand.plus.R
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchCoreFactory
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult

class POIScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer,
    private val group: PoiUIFilter
) : BaseOsmAndAndroidAutoSearchScreen(carContext) {
    private var itemList: ItemList = withNoResults(ItemList.Builder()).build()

    init {
        loadPOI()
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = PlaceListNavigationTemplate.Builder()
        if (loading) {
            templateBuilder.setLoading(true)
        } else {
            templateBuilder.setLoading(false)
            templateBuilder.setItemList(itemList)
        }
        return templateBuilder
            .setTitle(group.name)
            .setActionStrip(ActionStrip.Builder().addAction(settingsAction).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun withNoResults(builder: ItemList.Builder): ItemList.Builder {
        return builder.setNoItemsMessage(carContext.getString(R.string.no_poi_for_category))
    }

    override fun onClickSearchMore() {
        invalidate()
    }

    override fun onSearchDone(
        phrase: SearchPhrase,
        searchResults: List<SearchResult>?,
        itemList: ItemList?,
        resultsCount: Int) {

        loading = false
        if (resultsCount == 0) {
            this.itemList = withNoResults(ItemList.Builder()).build()
        } else {
            this.itemList = itemList!!
        }
        invalidate()
    }

    private fun loadPOI() {
        val objectLocalizedName = group.name;
        val sr = SearchResult()
        sr.localeName = objectLocalizedName
        sr.`object` = group
        sr.priority = SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY.toDouble()
        sr.priorityDistance = 0.0
        sr.objectType = ObjectType.POI_TYPE
        searchHelper.completeQueryWithObject(sr)
        loading = true
        invalidate()
    }

    override fun onClickSearchResult(point: SearchResult) {
        val result = SearchResult()
        result.location = LatLon(point.location.latitude, point.location.longitude)
        result.objectType = ObjectType.POI
        result.`object` = point.`object`
        openRoutePreview(settingsAction, surfaceRenderer, result)
    }
}