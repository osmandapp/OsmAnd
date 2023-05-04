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
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.MapUtils

class MapMarkersScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val app = app
        val markers = app.mapMarkersHelper.mapMarkers
        val location = app.settings.lastKnownMapLocation
        val iconsCache = app.uiUtilities
        for (marker in markers) {
            val title = marker.getName(app)
            val markerColor = MapMarker.getColorId(marker.colorIndex)
            val iconDrawable = iconsCache.getIcon(R.drawable.ic_action_flag, markerColor)
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(iconDrawable))).build()
            val rowBuilder = Row.Builder()
                .setTitle(title)
                .setImage(icon)
                .setOnClickListener { onClickMarkerItem(marker) }
            marker.point?.let { markerLocation ->
                val dist = MapUtils.getDistance(
                    markerLocation.latitude, markerLocation.longitude,
                    location.latitude, location.longitude)
                val address = SpannableString(" ")
                val distanceSpan = DistanceSpan.create(TripHelper.getDistance(app, dist))
                address.setSpan(distanceSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                rowBuilder.addText(address)
                rowBuilder.setMetadata(
                    Metadata.Builder().setPlace(
                        Place.Builder(
                            CarLocation.create(
                                location.latitude,
                                location.longitude)).build()).build())

            }
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
            .setTitle(app.getString(R.string.map_markers))
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }

    private fun onClickMarkerItem(mapMarker: MapMarker) {
        val result = SearchResult()
        result.location = LatLon(
            mapMarker.point.latitude,
            mapMarker.point.longitude)
        result.objectType = ObjectType.MAP_MARKER
        result.`object` = mapMarker
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
        session?.let{
            if(it.hasStarted()){
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