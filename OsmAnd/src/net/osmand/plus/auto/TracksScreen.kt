package net.osmand.plus.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.gpx.GPXUtilities
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.configmap.tracks.TrackTab
import net.osmand.plus.configmap.tracks.TrackTabType
import net.osmand.plus.track.data.GPXInfo
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem
import net.osmand.plus.track.helpers.GpxDbHelper
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class TracksScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer,
    private val trackTab: TrackTab
) : BaseOsmAndAndroidAutoScreen(carContext) {
    val gpxDbHelper: GpxDbHelper = app.gpxDbHelper
    var isLoading = true
    var loadGpxFilesThread: Thread? = null
    private val selectedGpxFiles = ArrayList<SelectedGpxFile>()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                super.onCreate(owner)
                isLoading = true
                loadGpxFilesThread = Thread {
                    try {
                        prepareTrackItems()
                        isLoading = false
                        invalidate()
                    } catch (_: Throwable) {
                    }
                }
                loadGpxFilesThread?.start()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                loadGpxFilesThread?.interrupt()
                app.osmandMap.mapLayers.gpxLayer.setAndroidAutoDisplayTracks(null)
                app.osmandMap.refreshMap()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = PlaceListNavigationTemplate.Builder()
        val title = if (trackTab.type == TrackTabType.ALL) {
            app.getString(R.string.sort_last_modified)
        } else {
            trackTab.getName(app, false)
        }
        templateBuilder.setLoading(isLoading)
        if (!isLoading) {
            setupTracks(templateBuilder)
        }

        return templateBuilder
            .setTitle(title)
            .setActionStrip(ActionStrip.Builder().addAction(settingsAction).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun prepareTrackItems() {
        selectedGpxFiles.clear()
        for (track in trackTab.trackItems) {
            track.file?.let { file ->
                val item = gpxDbHelper.getItem(file) { updateTrack(track, it) }
                if (item != null) {
                    track.dataItem = item
                }
                val gpxFile = GPXUtilities.loadGPXFile(file)
                val selectedGpxFile = SelectedGpxFile()
                selectedGpxFile.setGpxFile(gpxFile, app)
                selectedGpxFiles.add(selectedGpxFile)
            }
        }
        isLoading = false
        invalidate()
    }

    private fun updateTrack(trackItem: TrackItem, dataItem: GpxDataItem?) {
        trackItem.dataItem = dataItem
        invalidate()
    }

    private fun setupTracks(templateBuilder: PlaceListNavigationTemplate.Builder) {
        val latLon = app.mapViewTrackingUtilities.defaultLocation
        val listBuilder = ItemList.Builder()
        val tracksSize = trackTab.trackItems.size
        val tracks =
            trackTab.trackItems.subList(0, tracksSize.coerceAtMost(contentLimit - 1))
        for (track in tracks) {
            val title = track.name
            val icon = CarIcon.Builder(
                IconCompat.createWithResource(app, R.drawable.ic_action_polygom_dark))
                .setTint(
                    CarColor.createCustom(
                        app.getColor(R.color.icon_color_default_light),
                        app.getColor(R.color.icon_color_default_dark)))
                .build()
            var description = ""
            var dist = 0f
            track.dataItem?.let { dataItem ->
                description = dataItem.nearestCityName ?: ""
                dist = if (dataItem.analysis == null) {
                    0f
                } else {
                    MapUtils.getDistance(latLon, dataItem.analysis?.latLonStart).toFloat()
                }
            }
            val address =
                SpannableString(if (Algorithms.isEmpty(description)) " " else "  • $description")
            val distanceSpan = DistanceSpan.create(TripHelper.getDistance(app, dist.toDouble()))
            address.setSpan(distanceSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            listBuilder.addItem(Row.Builder()
                .setTitle(title)
                .setImage(icon)
                .addText(address)
                .setOnClickListener { onClickTrack(track) }
                .build())
        }
        val selectedTracksSize = selectedGpxFiles.size
        val selectedTracks =
            selectedGpxFiles.subList(0, selectedTracksSize.coerceAtMost(contentLimit - 1))
        app.osmandMap.mapLayers.gpxLayer.setAndroidAutoDisplayTracks(selectedTracks)
        app.osmandMap.refreshMap()
        templateBuilder.setItemList(listBuilder.build())
    }

    private fun onClickTrack(trackItem: TrackItem) {
        val result = SearchResult()
        result.objectType = ObjectType.GPX_TRACK
        result.`object` = trackItem
        result.relatedObject = GPXInfo(trackItem.name, trackItem.file)
        openRoutePreview(settingsAction, surfaceRenderer, result)
    }
}