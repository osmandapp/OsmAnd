package net.osmand.plus.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.plus.R
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.MapUtils
import kotlin.math.max
import kotlin.math.min

class MapMarkersScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer) : BaseOsmAndAndroidAutoScreen(carContext) {

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                app.osmandMap.mapLayers.mapMarkersLayer.setCustomMapObjects(null)
                app.osmandMap.mapView.backToLocation()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val markersSize = app.mapMarkersHelper.mapMarkers.size
        val markers =
            app.mapMarkersHelper.mapMarkers.subList(0, markersSize.coerceAtMost(contentLimit - 1))
        val location = app.settings.lastKnownMapLocation
        app.osmandMap.mapLayers.mapMarkersLayer.setCustomMapObjects(markers)
        val mapRect = QuadRect()
        extendRectToContainPoint(mapRect, location.longitude, location.latitude)
        for (marker in markers) {
            val longitude = marker.longitude
            val latitude = marker.latitude
            extendRectToContainPoint(mapRect, longitude, latitude)
            val title = marker.getName(app)
            val markerColor = MapMarker.getColorId(marker.colorIndex)
            val icon = CarIcon.Builder(
                IconCompat.createWithResource(carContext, R.drawable.ic_action_flag))
                .setTint(
                    CarColor.createCustom(
                        markerColor,
                        markerColor))
                .build()
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
        if (mapRect.left != 0.0 && mapRect.right != 0.0 && mapRect.top != 0.0 && mapRect.bottom != 0.0) {
            val tb: RotatedTileBox = app.osmandMap.mapView.currentRotatedTileBox.copy()
            app.osmandMap.mapView.fitRectToMap(mapRect.left, mapRect.right, mapRect.top, mapRect.bottom, tb.pixWidth, tb.pixHeight, 0)
        }
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.map_markers))
            .setActionStrip(ActionStrip.Builder().addAction(createSearchAction()).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun extendRectToContainPoint(
        mapRect: QuadRect,
        longitude: Double,
        latitude: Double) {
        mapRect.left = if (mapRect.left == 0.0) longitude else min(mapRect.left, longitude)
        mapRect.right = max(mapRect.right, longitude)
        mapRect.bottom = if (mapRect.bottom == 0.0) latitude else min(mapRect.bottom, latitude)
        mapRect.top = max(mapRect.top, latitude)
    }

    private fun onClickMarkerItem(mapMarker: MapMarker) {
        val result = SearchResult()
        result.location = LatLon(
            mapMarker.point.latitude,
            mapMarker.point.longitude)
        result.objectType = ObjectType.MAP_MARKER
        result.`object` = mapMarker
        openRoutePreview(settingsAction, surfaceRenderer, result)
    }

}