package net.osmand.plus.auto.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Item
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.R
import net.osmand.plus.auto.NavigationSession

class LandingScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseAndroidAutoScreen(carContext) {
    @DrawableRes
    private var compassResId = R.drawable.ic_compass_niu

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                app.carNavigationSession?.updateCarNavigation(app.locationProvider.lastKnownLocation)
            }
        })
    }

    override fun getTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val app = app
        for (category in PlaceCategory.entries) {
            if (category == PlaceCategory.FREE_MODE) {
                if (app.routingHelper.isRouteCalculated) {
                    listBuilder.addItem(createContinueNavigationItem())
                    continue
                }
            }
            val title = app.getString(category.titleId)
            val icon = CarIcon.Builder(IconCompat.createWithResource(app, category.iconId)).build()
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setImage(icon)
                    .setBrowsable(true)
                    .setOnClickListener { onCategoryClick(category) }
                    .build())
        }
        val actionStripBuilder = ActionStrip.Builder()
        updateCompass()
        actionStripBuilder.addAction(settingsAction)
        actionStripBuilder.addAction(createSearchAction())
        val mapActionStripBuilder = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_my_location)
                        ).build()
                    )
                    .setOnClickListener {
                        session?.navigationCarSurface?.handleRecenter()
                    }
                    .build())
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_zoom_in)
                        ).build()
                    )
                    .setOnClickListener {
                        app.carNavigationSession?.navigationCarSurface?.handleScale(
	                        NavigationSession.INVALID_FOCAL_POINT_VAL,
	                        NavigationSession.INVALID_FOCAL_POINT_VAL,
	                        NavigationSession.ZOOM_IN_BUTTON_SCALE_FACTOR
                        )
                    }
                    .build())
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                R.drawable.ic_zoom_out))
                            .build())
                    .setOnClickListener {
                        app.carNavigationSession?.navigationCarSurface?.handleScale(
	                        NavigationSession.INVALID_FOCAL_POINT_VAL,
	                        NavigationSession.INVALID_FOCAL_POINT_VAL,
	                        NavigationSession.ZOOM_OUT_BUTTON_SCALE_FACTOR
                        )
                    }
                    .build())
            .build()
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .setMapActionStrip(mapActionStripBuilder)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }

    private fun createContinueNavigationItem(): Item {
        val title = app.getString(R.string.continue_navigation)
        val icon = CarIcon.Builder(
            IconCompat.createWithResource(
                app,
                R.drawable.ic_action_gdirections_dark)).build()
        return Row.Builder()
            .setTitle(title)
            .setImage(icon)
            .setBrowsable(true)
            .setOnClickListener {
                app.carNavigationSession?.let { carNavigationSession ->
                    carNavigationSession.startNavigationScreen()
                }
            }
            .build()
    }

    private fun updateCompass() {
        val settings = app.settings
        val nightMode = carContext.isDarkMode
        val compassMode = settings.compassMode
        compassResId = compassMode.getIconId(nightMode)
    }


    private fun onCategoryClick(category: PlaceCategory) {
        when (category) {
            PlaceCategory.FREE_MODE ->
                app.carNavigationSession?.let {
                    screenManager.push(
                        NavigationScreen(
                            carContext,
                            settingsAction,
                            it
                        )
                    )
                }

            PlaceCategory.FAVORITES -> screenManager.push(
                FavoriteGroupsScreen(
                    carContext,
                    settingsAction
                )
            )

            PlaceCategory.HISTORY -> screenManager.push(
                HistoryScreen(
                    carContext,
                    settingsAction)
            )

            PlaceCategory.MAP_MARKERS -> screenManager.push(
                MapMarkersScreen(
                    carContext,
                    settingsAction)
            )

            PlaceCategory.POI -> screenManager.push(
                POICategoriesScreen(
                    carContext,
                    settingsAction)
            )

            PlaceCategory.TRACKS -> screenManager.push(
                TracksFoldersScreen(
                    carContext,
                    settingsAction)
            )
        }
    }

    internal enum class PlaceCategory(
        @DrawableRes val iconId: Int,
        @StringRes val titleId: Int) {
        FREE_MODE(R.drawable.ic_action_start_navigation, R.string.free_ride),
        HISTORY(R.drawable.ic_action_history, R.string.shared_string_history),
        POI(R.drawable.ic_action_info_dark, R.string.poi_categories),
        FAVORITES(R.drawable.ic_action_favorite, R.string.shared_string_favorites),
        MAP_MARKERS(R.drawable.ic_action_flag_stroke, R.string.map_markers_item),
        TRACKS(R.drawable.ic_action_polygom_dark, R.string.shared_string_gpx_tracks);
    }
}