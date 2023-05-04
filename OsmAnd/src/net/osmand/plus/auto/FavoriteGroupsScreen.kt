package net.osmand.plus.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.utils.AndroidUtils

class FavoriteGroupsScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        setupFavoriteGroups(listBuilder)
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.shared_string_favorites))
            .setActionStrip(ActionStrip.Builder().addAction(settingsAction).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun setupFavoriteGroups(listBuilder: ItemList.Builder) {
        val iconLastModified =
            CarIcon.Builder(IconCompat.createWithResource(app, R.drawable.ic_action_history))
                .setTint(
                    CarColor.createCustom(
                        app.getColor(R.color.icon_color_osmand_light),
                        app.getColor(R.color.icon_color_osmand_dark)))
                .build()
        listBuilder.addItem(
            Row.Builder()
                .setTitle(app.getString(R.string.sort_last_modified))
                .setImage(iconLastModified)
                .setBrowsable(true)
                .setOnClickListener { onClickFavoriteGroup(null) }
                .build())
        for (group in favoriteGroups) {
            val title = group.getDisplayName(app)
            val groupIcon = app.favoritesHelper.getColoredIconForGroup(group.name);
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(groupIcon))).build()
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setImage(icon)
                    .setBrowsable(true)
                    .setOnClickListener { onClickFavoriteGroup(group) }
                    .build())
        }
    }

    private fun onClickFavoriteGroup(group: FavoriteGroup?) {
        screenManager.push(
            FavoritesScreen(
                carContext,
                settingsAction,
                surfaceRenderer,
                group))
    }

    private val favoriteGroups: List<FavoriteGroup>
        private get() = app.favoritesHelper.favoriteGroups


    private val app: OsmandApplication
        private get() = carContext.applicationContext as OsmandApplication
}