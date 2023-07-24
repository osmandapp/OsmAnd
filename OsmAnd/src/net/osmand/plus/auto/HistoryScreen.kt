package net.osmand.plus.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.data.LatLon
import net.osmand.plus.R
import net.osmand.plus.helpers.SearchHistoryHelper
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import net.osmand.util.MapUtils

class HistoryScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseOsmAndAndroidAutoScreen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val app = app
        val historyHelper = SearchHistoryHelper.getInstance(app)
        val results = historyHelper.getHistoryEntries(true)
        val location = app.settings.lastKnownMapLocation
        val resultsSize = results.size
        val limitedResults = results.subList(0, resultsSize.coerceAtMost(contentLimit - 1))
        for (result in limitedResults) {
            val searchResult =
                SearchHistoryAPI.createSearchResult(app, result, SearchPhrase.emptyPhrase())
            val listItem = QuickSearchListItem(app, searchResult)
            val pointDescription = result.name

            val title = listItem.name
            val icon = CarIcon.Builder(
                IconCompat.createWithResource(app, pointDescription.itemIcon)).build()
            val rowBuilder = Row.Builder()
                .setTitle(title)
                .setImage(icon)
                .setOnClickListener { onClickHistoryItem(listItem) }
            val dist = if (searchResult.location == null) {
                0.0
            } else {
                rowBuilder.setMetadata(
                    Metadata.Builder().setPlace(
                        Place.Builder(
                            CarLocation.create(
                                result.lat,
                                result.lon)).build()).build())
                MapUtils.getDistance(
                    result.lat, result.lon,
                    location.latitude, location.longitude)
            }
            val address = SpannableString(" ")
            val distanceSpan = DistanceSpan.create(TripHelper.getDistance(app, dist))
            address.setSpan(distanceSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            rowBuilder.addText(address)
            listBuilder.addItem(rowBuilder.build())
        }
        val actionStripBuilder = ActionStrip.Builder()
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext, R.drawable.ic_action_search_dark)).build())
                .setOnClickListener { openSearch() }
                .build())
        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle(app.getString(R.string.shared_string_history))
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
    }

    private fun onClickHistoryItem(historyItem: QuickSearchListItem) {
        val result = SearchResult()
        result.location = LatLon(
            historyItem.searchResult.location.latitude,
            historyItem.searchResult.location.longitude)
        result.objectType = ObjectType.RECENT_OBJ
        result.`object` = historyItem.searchResult.`object`
        openRoutePreview(settingsAction, result)
    }

    private fun openSearch() {
        screenManager.pushForResult(
            SearchScreen(
                carContext,
                settingsAction)) { }
    }
}