package net.osmand.plus.auto.screens

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
import net.osmand.plus.R
import net.osmand.plus.auto.TripHelper
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.settings.enums.CompassMode
import net.osmand.plus.views.layers.base.OsmandMapLayer.CustomMapObjects
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class MapMarkersScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseAndroidAutoScreen(carContext) {
    private var initialCompassMode: CompassMode? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                app.osmandMap.mapLayers.mapMarkersLayer.setCustomMapObjects(null)
                app.osmandMap.mapLayers.mapMarkersLayer.customObjectsDelegate = null
                app.osmandMap.mapView.backToLocation()
                initialCompassMode?.let {
                    app.mapViewTrackingUtilities.switchCompassModeTo(it)
                }
            }
            override fun onStart(owner: LifecycleOwner) {
                recenterMap()
                app.osmandMap.mapLayers.mapMarkersLayer.customObjectsDelegate = CustomMapObjects()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val markersSize = app.mapMarkersHelper.mapMarkers.size
        val markers =
            app.mapMarkersHelper.mapMarkers.subList(0, markersSize.coerceAtMost(contentLimit - 1))
        val location = app.mapViewTrackingUtilities.defaultLocation
        app.osmandMap.mapLayers.mapMarkersLayer.setCustomMapObjects(markers)
        val mapRect = QuadRect()
        if (!Algorithms.isEmpty(markers)) {
            initialCompassMode = app.settings.compassMode
            app.mapViewTrackingUtilities.switchCompassModeTo(CompassMode.NORTH_IS_UP)
        }
        for (marker in markers) {
            val longitude = marker.longitude
            val latitude = marker.latitude
            Algorithms.extendRectToContainPoint(mapRect, longitude, latitude)
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
        adjustMapToRect(location, mapRect)
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.map_markers))
            .setActionStrip(ActionStrip.Builder().addAction(createSearchAction()).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun onClickMarkerItem(mapMarker: MapMarker) {
        val result = SearchResult()
        result.location = LatLon(
            mapMarker.point.latitude,
            mapMarker.point.longitude)
        result.objectType = ObjectType.MAP_MARKER
        result.`object` = mapMarker
        openRoutePreview(settingsAction, result)
    }

}