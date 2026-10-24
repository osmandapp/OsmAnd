package net.osmand.plus.auto.screens

import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.data.FavouritePoint
import net.osmand.data.LatLon
import net.osmand.data.SpecialPointType
import net.osmand.plus.R
import net.osmand.plus.auto.TripUtils
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.PointImageUtils
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

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

    @Suppress("DEPRECATION") // PlaceListNavigationTemplate, needs migration to MapWithContentTemplate
    override fun getTemplate(): Template {
        val listBuilder = ItemList.Builder()
        setupFavoriteGroups(listBuilder)
        return androidx.car.app.navigation.model.PlaceListNavigationTemplate.Builder()
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
        val homeAdded = addHomeRow(listBuilder)
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
        val groupsLimit = contentLimit - 2 - (if (homeAdded) 1 else 0)
        if (groupsLimit <= 0) {
            return
        }
        val favoriteGroupsSize = favoriteGroups.size
        val limitedFavoriteGroups =
            favoriteGroups.subList(0, favoriteGroupsSize.coerceAtMost(groupsLimit))
        for (group in limitedFavoriteGroups) {
            val title = group.getDisplayName(app)
            var groupIcon = app.favoritesHelper.getColoredIconForGroup(group.name)
            if (groupIcon == null) {
                val colorId =
                    if (!carContext.isDarkMode) R.color.icon_color_default_light else R.color.icon_color_secondary_dark
                val uiUtilities = app.uiUtilities
                groupIcon = uiUtilities.getIcon(R.drawable.ic_action_group_name_16, colorId)
            }
            val icon = if (groupIcon != null) CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(groupIcon)))
                .build() else null
            val rowBuilder = Row.Builder()
                .setTitle(title)
                .setBrowsable(true)
                .setOnClickListener { onClickFavoriteGroup(group) }
            icon?.let { rowBuilder.setImage(it) }
            listBuilder.addItem(
                rowBuilder.build())
        }
    }

    private fun addHomeRow(listBuilder: ItemList.Builder): Boolean {
        val home = app.favoritesHelper.getSpecialPoint(SpecialPointType.HOME) ?: return false
        val location = app.mapViewTrackingUtilities.defaultLocation
        val color = app.favoritesHelper.getColorWithCategory(
            home, ContextCompat.getColor(app, R.color.color_favorite))
        val icon = CarIcon.Builder(IconCompat.createWithBitmap(
            AndroidUtils.drawableToBitmap(PointImageUtils.getFromPoint(app, color, false, home)))).build()
        val description = home.description
        val dist = MapUtils.getDistance(home.latitude, home.longitude,
            location.latitude, location.longitude)
        val address = SpannableString(if (Algorithms.isEmpty(description)) " " else "  • $description")
        address.setSpan(DistanceSpan.create(TripUtils.getDistance(app, dist)), 0, 1, SPAN_INCLUSIVE_INCLUSIVE)
        listBuilder.addItem(
            Row.Builder()
                .setTitle(home.getDisplayName(app))
                .setImage(icon)
                .addText(address)
                .setOnClickListener { onClickHome(home) }
                .build())
        return true
    }

    private fun onClickHome(home: FavouritePoint) {
        val result = SearchResult()
        result.location = LatLon(home.latitude, home.longitude)
        result.objectType = ObjectType.FAVORITE
        result.`object` = home
        result.localeName = home.address
        openRoutePreview(settingsAction, result)
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