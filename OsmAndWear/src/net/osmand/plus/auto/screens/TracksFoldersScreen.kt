package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.IndexConstants.GPX_INDEX_DIR
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackTab
import net.osmand.plus.configmap.tracks.TrackTabType
import net.osmand.plus.configmap.tracks.TrackTabsHelper
import net.osmand.plus.settings.enums.TracksSortMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.FileUtils
import net.osmand.shared.extensions.kFile
import net.osmand.shared.gpx.TrackFolderLoaderTask
import net.osmand.shared.gpx.TrackFolderLoaderTask.LoadTracksListener
import net.osmand.shared.gpx.data.TrackFolder

class TracksFoldersScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseAndroidAutoScreen(carContext),
    LoadTracksListener {
    private var asyncLoader: TrackFolderLoaderTask? = null
    private val trackTabsHelper: TrackTabsHelper = TrackTabsHelper(app)

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                recenterMap()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = PlaceListNavigationTemplate.Builder()
        setupTrackFolders(templateBuilder)
        val actionStripBuilder = ActionStrip.Builder()
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext, R.drawable.ic_action_search_dark)).build())
                .setOnClickListener { openSearch() }
                .build())

        return templateBuilder
            .setTitle(app.getString(R.string.shared_string_tracks))
            .setActionStrip(actionStripBuilder.build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun reloadTracks() {
        val folder = TrackFolder(FileUtils.getExistingDir(app, GPX_INDEX_DIR).kFile(), null)
        asyncLoader = TrackFolderLoaderTask(folder, this).apply { execute() }
    }


    private fun setupTrackFolders(templateBuilder: PlaceListNavigationTemplate.Builder) {
        val listBuilder = ItemList.Builder()
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
                .setOnClickListener { onClickTabFolder(trackTabsHelper.trackTabs[TrackTabType.ALL.name]!!) }
                .build())

        if (trackTabsHelper.trackTabs.isEmpty()) {
            if (asyncLoader == null) {
                reloadTracks()
                templateBuilder.setLoading(true)
            }
            return
        }
        templateBuilder.setLoading(false)
        var itemsCount = 1
        for (trackTab in trackTabsHelper.trackTabs.values) {
            if (trackTab.type != TrackTabType.FOLDER) {
                continue
            }
            if (itemsCount == contentLimit) {
                break
            }
            val title = trackTab.getName(app)
            val iconColorId = ColorUtilities.getDefaultIconColorId(app.daynightHelper.isNightMode)
            val iconDrawable = app.uiUtilities.getIcon(trackTab.type.iconId, iconColorId)
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(iconDrawable))).build()
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setImage(icon)
                    .setBrowsable(true)
                    .setOnClickListener { onClickTabFolder(trackTab) }
                    .build())
            itemsCount++
        }
        templateBuilder.setItemList(listBuilder.build())
    }

    private fun onClickTabFolder(trackTab: TrackTab) {
        if (trackTab.type == TrackTabType.ALL) {
            trackTab.sortMode = TracksSortMode.LAST_MODIFIED
            trackTabsHelper.sortTrackTab(trackTab)
        }
        screenManager.pushForResult(
            TracksScreen(
                carContext,
                settingsAction,
                trackTab)
        ) { }
    }

    override fun loadTracksFinished(folder: TrackFolder) {
        trackTabsHelper.updateTrackItems(folder.getFlattenedTrackItems())
        invalidate()
    }

    private fun openSearch() {
        screenManager.pushForResult(
	        SearchScreen(
		        carContext,
		        settingsAction
	        )
        ) { }
    }

}