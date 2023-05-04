package net.osmand.plus.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.CorwinLogger
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.SearchHistoryHelper
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.MapUtils

class HistoryScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val app = app
        val historyHelper = SearchHistoryHelper.getInstance(app)
        val results = historyHelper.getSearchHistoryResults(true)
        val location = app.settings.lastKnownMapLocation

        for (result in results) {
            val listItem = QuickSearchListItem(app, result)
            val title = listItem.name
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(listItem.icon))).build()
            val rowBuilder = Row.Builder()
                .setTitle(title)
                .setImage(icon)
                .setOnClickListener { onClickHistoryItem(listItem) }
            if (result.location == null) {
                CorwinLogger.log("")
            }
            val dist = if (result.location == null) {
                0.0
            } else {
                rowBuilder.setMetadata(
                    Metadata.Builder().setPlace(
                        Place.Builder(
                            CarLocation.create(
                                result.location.latitude,
                                result.location.longitude)).build()).build())
                MapUtils.getDistance(
                    result.location.latitude, result.location.longitude,
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
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.shared_string_history))
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }

    private fun onClickHistoryItem(historyItem: QuickSearchListItem) {
        val result = SearchResult()
        result.location = LatLon(
            historyItem.searchResult.location.latitude,
            historyItem.searchResult.location.longitude)
        result.objectType = ObjectType.RECENT_OBJ
        result.`object` = historyItem.searchResult.`object`
        screenManager.pushForResult(
            RoutePreviewScreen(carContext, settingsAction, surfaceRenderer, result)
        ) { obj: Any? ->
            if (obj != null) {
                onRouteSelected()
            }
        }
        finish()
    }

    private fun onRouteSelected() {
        app.osmandMap.mapLayers.mapControlsLayer.startNavigation()
        val session = app.carNavigationSession
        session?.let {
            if (it.hasStarted()) {
                session.startNavigation()
            }
        }
    }

    private fun openSearch() {
        screenManager.pushForResult(
            SearchScreen(
                carContext,
                settingsAction,
                surfaceRenderer)) { }
    }

    private val app: OsmandApplication
        private get() = carContext.applicationContext as OsmandApplication
}