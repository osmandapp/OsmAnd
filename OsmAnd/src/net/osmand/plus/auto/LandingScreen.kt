package net.osmand.plus.auto

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R

class LandingScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer) : Screen(carContext) {
    @DrawableRes
    private var compassResId = R.drawable.ic_compass_niu

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val app = app
        for (category in PlaceCategory.values()) {
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
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext, R.drawable.ic_action_search_dark)).build())
                .setOnClickListener { openSearch() }
                .build())
        val mapActionStripBuilder = ActionStrip.Builder()
            .addAction(
                Action.Builder(Action.PAN)
                    .build())
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                R.drawable.ic_zoom_in))
                            .build())
                    .setOnClickListener {
                        surfaceRenderer.handleScale(
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.ZOOM_IN_BUTTON_SCALE_FACTOR)
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
                        surfaceRenderer.handleScale(
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.ZOOM_OUT_BUTTON_SCALE_FACTOR)
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

    private fun updateCompass() {
        val settings = app.settings
        val nightMode = carContext.isDarkMode
        val compassMode = settings.compassMode
        compassResId = compassMode.getIconId(nightMode)
    }

    private fun openSearch() {
        screenManager.pushForResult(
            SearchScreen(
                carContext,
                settingsAction,
                surfaceRenderer)) { obj: Any? -> }
    }

    private fun onCategoryClick(category: PlaceCategory) {
        when (category) {
            PlaceCategory.FAVORITES -> screenManager.push(
                FavoriteGroupsScreen(
                    carContext,
                    settingsAction,
                    surfaceRenderer))
            PlaceCategory.HISTORY -> screenManager.push(
                HistoryScreen(
                    carContext,
                    settingsAction,
                    surfaceRenderer))
            PlaceCategory.MAP_MARKERS -> screenManager.push(
                MapMarkersScreen(
                    carContext,
                    settingsAction,
                    surfaceRenderer))
            PlaceCategory.POI -> screenManager.push(
                POICategoriesScreen(
                    carContext,
                    settingsAction,
                    surfaceRenderer))
            PlaceCategory.TRACKS -> screenManager.push(
                TracksFoldersScreen(
                    carContext,
                    settingsAction,
                    surfaceRenderer))
        }
    }


    private val app: OsmandApplication
        private get() = carContext.applicationContext as OsmandApplication

    internal enum class PlaceCategory(
        @DrawableRes val iconId: Int,
        @StringRes val titleId: Int) {
        HISTORY(R.drawable.ic_action_history, R.string.shared_string_history),
        POI(R.drawable.ic_action_info_dark, R.string.poi_categories),
        FAVORITES(R.drawable.ic_action_favorite, R.string.shared_string_favorites),
        MAP_MARKERS(R.drawable.ic_action_flag_stroke, R.string.map_markers_item),
        TRACKS(R.drawable.ic_action_polygom_dark, R.string.shared_string_gpx_tracks);
    }
}