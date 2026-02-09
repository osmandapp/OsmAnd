package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.R
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.utils.AndroidUtils

class FavoriteGroupsScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseAndroidAutoScreen(carContext) {

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                recenterMap()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        setupFavoriteGroups(listBuilder)
        return PlaceListNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(app.getString(R.string.shared_string_favorites))
            .setActionStrip(ActionStrip.Builder().addAction(createSearchAction()).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
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
        if (contentLimit < 2) {
            return
        }
        val favoriteGroupsSize = favoriteGroups.size
        val limitedFavoriteGroups =
            favoriteGroups.subList(0, favoriteGroupsSize.coerceAtMost(contentLimit - 2))
        for (group in limitedFavoriteGroups) {
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
                group)
        )
    }

    private val favoriteGroups: List<FavoriteGroup>
        private get() = app.favoritesHelper.favoriteGroups

}