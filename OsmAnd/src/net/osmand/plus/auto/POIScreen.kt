package net.osmand.plus.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class POIScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer,
    private val group: PoiUIFilter,
    private val searchResults: ArrayList<SearchResult>
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        setupPOI(listBuilder)
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(group.name)
            .setActionStrip(ActionStrip.Builder().addAction(settingsAction).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun setupPOI(listBuilder: ItemList.Builder) {
        val location = app.settings.lastKnownMapLocation
        for (point in searchResults) {
            val title = point.localeName
            var groupIcon = RenderingIcons.getBigIcon(app, group.iconId)
            if (groupIcon == null) {
                groupIcon = app.getDrawable(R.drawable.mx_special_custom_category)
            }
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(groupIcon))).build()
            val description =
                if (point.alternateName != null) point.alternateName else ""
            val dist = MapUtils.getDistance(
                point.location.latitude, point.location.longitude,
                location.latitude, location.longitude)
            val address =
                SpannableString(if (Algorithms.isEmpty(description)) " " else "  • $description")
            val distanceSpan = DistanceSpan.create(TripHelper.getDistance(app, dist))
            address.setSpan(distanceSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            listBuilder.addItem(Row.Builder()
                .setTitle(title)
                .setImage(icon)
                .addText(address)
                .setOnClickListener { onClickPOI(point) }
                .setMetadata(
                    Metadata.Builder().setPlace(
                        Place.Builder(
                            CarLocation.create(
                                point.location.latitude,
                                point.location.longitude)).build()).build())
                .build())
        }
    }

    private fun onClickPOI(point: SearchResult) {
        val result = SearchResult()
        result.location = LatLon(point.location.latitude, point.location.longitude)
        result.objectType = ObjectType.POI
        result.`object` = point.`object`
        screenManager.pushForResult(
            RoutePreviewScreen(carContext, settingsAction, surfaceRenderer, result)
        ) { obj: Any? ->
            if (obj != null) {
                onRouteSelected(result)
            }
        }
        finish()
    }

    private fun onRouteSelected(sr: SearchResult) {
        app.osmandMap.mapLayers.mapControlsLayer.startNavigation()
        val session = app.carNavigationSession
        if (session != null && session.hasStarted()) {
            session.startNavigation()
        }
    }

    private val app: OsmandApplication
        private get() = carContext.applicationContext as OsmandApplication
}