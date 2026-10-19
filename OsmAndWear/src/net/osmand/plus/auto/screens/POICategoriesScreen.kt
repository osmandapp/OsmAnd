package net.osmand.plus.auto.screens

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.R
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.AndroidUtils

class POICategoriesScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseAndroidAutoScreen(carContext) {

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                recenterMap()
            }
        })
    }

    private var selectedPOIGroup: PoiUIFilter? = null

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        setupPOICategories(listBuilder)
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.poi_categories))
            .setActionStrip(ActionStrip.Builder().addAction(createSearchAction()).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun setupPOICategories(listBuilder: ItemList.Builder) {
        val poiFilters = app.poiFilters.getSortedPoiFilters(false)
        val poiFiltersSize = poiFilters.size
        val limitedPOIFilters = app.poiFilters.getSortedPoiFilters(false)
            .subList(0, poiFiltersSize.coerceAtMost(contentLimit - 1))
        for (poiFilter in limitedPOIFilters) {
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
        selectedPOIGroup = group
        Handler(Looper.getMainLooper()).post {
            screenManager.push(
                POIScreen(
                    carContext,
                    settingsAction,
                    group)
            )
        }
    }

}