package net.osmand.plus.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
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
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.plus.R
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchCoreFactory
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils
import kotlin.math.max
import kotlin.math.min

class POIScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer,
    private val group: PoiUIFilter
) : BaseOsmAndAndroidAutoSearchScreen(carContext), LifecycleObserver {
    private lateinit var itemList: ItemList
    private var searchRadius = 0.0

    init {
        loadPOI()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                app.osmandMap.mapLayers.poiMapLayer.setCustomMapObjects(null)
                app.osmandMap.mapView.backToLocation()
            }
        })
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
            .setActionStrip(ActionStrip.Builder()
                .addAction(createSearchAction())
                .build())
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
        if(resultsCount < contentLimit && searchRadius < SearchCoreFactory.MAX_DEFAULT_SEARCH_RADIUS) {
            searchRadius++
            loadPOI()
        } else {
            loading = false
            if (resultsCount == 0) {
                this.itemList = withNoResults(ItemList.Builder()).build()
            } else {
                var builder = ItemList.Builder();
                setupPOI(builder, searchResults)
                this.itemList = builder.build()
            }
            invalidate()
        }
    }

    private fun setupPOI(listBuilder: ItemList.Builder, searchResults: List<SearchResult>?) {
        val location = app.settings.lastKnownMapLocation
        val mapPoint = ArrayList<Amenity>()
        val mapRect = QuadRect()
        extendRectToContainPoint(mapRect, location)
        searchResults?.let {
            val searchResultsSize = searchResults.size
            val limitedSearchResults =
                searchResults.subList(0, searchResultsSize.coerceAtMost(contentLimit - 1))
            for (point in limitedSearchResults) {
                if (point.`object` is Amenity) {
                    val amenity = point.`object` as Amenity
                    mapPoint.add(amenity)
                    val amenityLocation = amenity.location
                    extendRectToContainPoint(mapRect, amenityLocation)
                }
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
                    .setOnClickListener { onClickSearchResult(point) }
                    .setMetadata(
                        Metadata.Builder().setPlace(
                            Place.Builder(
                                CarLocation.create(
                                    point.location.latitude,
                                    point.location.longitude)).build()).build())
                    .build())
            }
        }
        if (mapRect.left != 0.0 && mapRect.right != 0.0 && mapRect.top != 0.0 && mapRect.bottom != 0.0) {
            val tb: RotatedTileBox = app.osmandMap.mapView.currentRotatedTileBox.copy()
            app.osmandMap.mapView.fitRectToMap(mapRect.left, mapRect.right, mapRect.top, mapRect.bottom, tb.pixWidth, tb.pixHeight, 0)
        }
        app.osmandMap.mapLayers.poiMapLayer.setCustomMapObjects(mapPoint)
    }

	private fun extendRectToContainPoint(mapRect: QuadRect, amenityLocation: LatLon) {
		mapRect.left = if (mapRect.left == 0.0) amenityLocation.longitude else min(
			mapRect.left,
			amenityLocation.longitude)
		mapRect.right = max(mapRect.right, amenityLocation.longitude)
		mapRect.bottom = if (mapRect.bottom == 0.0) amenityLocation.latitude else min(
			mapRect.bottom,
			amenityLocation.latitude)
		mapRect.top = max(mapRect.top, amenityLocation.latitude)
	}

    private fun loadPOI() {
        val objectLocalizedName = group.name;
        val sr = SearchResult()
        sr.localeName = objectLocalizedName
        sr.`object` = group
        sr.priority = SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY.toDouble()
	    sr.priorityDistance = searchRadius
        sr.objectType = ObjectType.POI_TYPE
        searchHelper.completeQueryWithObject(sr)
        loading = true
    }

    override fun onClickSearchResult(point: SearchResult) {
        val result = SearchResult()
        result.location = LatLon(point.location.latitude, point.location.longitude)
        result.objectType = ObjectType.POI
        result.`object` = point.`object`
        openRoutePreview(settingsAction, surfaceRenderer, result)
    }
}