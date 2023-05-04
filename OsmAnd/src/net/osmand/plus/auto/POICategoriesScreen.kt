package net.osmand.plus.auto

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.ResultMatcher
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchCoreFactory
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms

class POICategoriesScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        setupPOICategories(listBuilder)
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.poi_categories))
            .setActionStrip(ActionStrip.Builder().addAction(settingsAction).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun setupPOICategories(listBuilder: ItemList.Builder) {
        for (poiFilter in app.poiFilters.getSortedPoiFilters(false)) {
            val title = poiFilter.name
            var groupIcon = RenderingIcons.getBigIcon(app, poiFilter.iconId)
            if (groupIcon == null) {
                groupIcon = app.getDrawable(R.drawable.mx_special_custom_category)
            }
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(groupIcon))).build()
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setImage(icon)
                    .setBrowsable(true)
                    .setOnClickListener { onClickPOICategory(poiFilter) }
                    .build())
        }
    }

    private fun onClickPOICategory(group: PoiUIFilter) {
        val locale = app.settings.MAP_PREFERRED_LOCALE.get()
        val transliterate = app.settings.MAP_TRANSLITERATE_NAMES.get()
        val searchHelper = app.searchUICore
        val searchUICore = searchHelper.core
        val location = app.locationProvider.lastKnownLocation
        val result = searchUICore.shallowSearch(
            SearchCoreFactory.SearchAmenityTypesAPI::class.java,
            group.name,
            null)
        for (poiCategory in result.currentSearchResults) {
            if (poiCategory.localeName.equals(group.name)) {
                searchUICore.resetPhrase()
                searchUICore.selectSearchResult(poiCategory)
            }
        }
        var searchSettings = searchUICore.searchSettings.setSearchTypes(ObjectType.POI)
        searchSettings = searchSettings.setLang(locale, transliterate)
        location?.let {
            searchSettings = searchSettings.setOriginalLocation(LatLon(it.latitude, it.longitude))
        }
        searchUICore.updateSettings(searchSettings)
        val searchResults = ArrayList<SearchResult>()
        searchUICore.search(
            searchUICore.phrase.getText(true),
            false,
            object : ResultMatcher<SearchResult> {
                override fun publish(result: SearchResult): Boolean {
                    when (result.objectType) {
                        ObjectType.SEARCH_FINISHED -> {
                            if (!Algorithms.isEmpty(searchResults)) {
                                Handler(Looper.getMainLooper()).post {
                                    screenManager.push(
                                        POIScreen(
                                            carContext,
                                            settingsAction,
                                            surfaceRenderer,
                                            group,
                                            searchResults
                                        ))
                                }
                            }
                        }
                        ObjectType.POI -> {
                            searchResults.add(result)
                        }
                    }
                    return true
                }

                override fun isCancelled(): Boolean {
                    return false
                }

            })
    }

    private val app: OsmandApplication
        private get() = carContext.applicationContext as OsmandApplication
}