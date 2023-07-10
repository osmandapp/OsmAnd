package net.osmand.plus.auto

import android.os.AsyncTask
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
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
import kotlin.math.max
import kotlin.math.min

class TracksScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val surfaceRenderer: SurfaceRenderer,
    private val trackTab: TrackTab
) : BaseOsmAndAndroidAutoScreen(carContext) {
    val gpxDbHelper: GpxDbHelper = app.gpxDbHelper
    private var loadedGpxFiles = HashMap<TrackItem, SelectedGpxFile>()
    private lateinit var loadTracksTask: LoadTracksTask

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                super.onCreate(owner)
                loadTracksTask = LoadTracksTask()
                loadTracksTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                app.osmandMap.mapLayers.gpxLayer.setCustomMapObjects(null)
                app.osmandMap.mapView.backToLocation()
            }
        })
    }

    private inner class LoadTracksTask : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            prepareTrackItems()
        }

        override fun onPostExecute(result: Unit?) {
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = PlaceListNavigationTemplate.Builder()
        val title = if (trackTab.type == TrackTabType.ALL) {
            app.getString(R.string.sort_last_modified)
        } else {
            trackTab.getName(app, false)
        }
        val isLoading = loadTracksTask.status != AsyncTask.Status.FINISHED
        templateBuilder.setLoading(isLoading)
        if (!isLoading) {
            setupTracks(templateBuilder)
        }

        return templateBuilder
            .setTitle(title)
            .setActionStrip(ActionStrip.Builder().addAction(createSearchAction()).build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun prepareTrackItems() {
        val newMap = HashMap<TrackItem, SelectedGpxFile>()
        for (track in trackTab.trackItems) {
            track.file?.let { file ->
                val item = gpxDbHelper.getItem(file) { updateTrack(track, it) }
                if (item != null) {
                    track.dataItem = item
                }
                val gpxFile = GPXUtilities.loadGPXFile(file)
                val selectedGpxFile = SelectedGpxFile()
                selectedGpxFile.setGpxFile(gpxFile, app)
                newMap[track] = selectedGpxFile
            }
        }
        loadedGpxFiles = newMap
    }

    private fun updateTrack(trackItem: TrackItem, dataItem: GpxDataItem?) {
        trackItem.dataItem = dataItem
    }

    private fun setupTracks(templateBuilder: PlaceListNavigationTemplate.Builder) {
        val latLon = app.mapViewTrackingUtilities.defaultLocation
        val listBuilder = ItemList.Builder()
        val tracksSize = trackTab.trackItems.size
        val selectedGpxFiles = ArrayList<SelectedGpxFile>()
        val tracks =
            trackTab.trackItems.subList(0, tracksSize.coerceAtMost(contentLimit - 1))
        val mapRect = QuadRect()
        val currentLocationQuadRect =
            QuadRect(latLon.longitude, latLon.latitude, latLon.longitude, latLon.latitude)
        extendRectToContainPoint(mapRect, currentLocationQuadRect)
        for (track in tracks) {
            val gpxFile = loadedGpxFiles[track]
            gpxFile?.let {
                selectedGpxFiles.add(it)
                val gpxRect: QuadRect = it.gpxFile.rect
                extendRectToContainPoint(mapRect, gpxRect)
            }
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
        if (mapRect.left != 0.0 && mapRect.right != 0.0 && mapRect.top != 0.0 && mapRect.bottom != 0.0) {
            val tb: RotatedTileBox = app.osmandMap.mapView.currentRotatedTileBox.copy()
            app.osmandMap.mapView.fitRectToMap(mapRect.left, mapRect.right, mapRect.top, mapRect.bottom, tb.pixWidth, tb.pixHeight, 0)
        }
        app.osmandMap.mapLayers.gpxLayer.setCustomMapObjects(selectedGpxFiles)
        templateBuilder.setItemList(listBuilder.build())
    }

    private fun extendRectToContainPoint(mapRect: QuadRect, gpxRect: QuadRect) {
        mapRect.left = if (mapRect.left == 0.0) gpxRect.left else min(mapRect.left, gpxRect.left)
        mapRect.right = max(mapRect.right, gpxRect.right)
        mapRect.top = max(mapRect.top, gpxRect.top)
        mapRect.bottom =
            if (mapRect.bottom == 0.0) gpxRect.bottom else min(mapRect.bottom, gpxRect.bottom)
    }

    private fun onClickTrack(trackItem: TrackItem) {
        val result = SearchResult()
        result.objectType = ObjectType.GPX_TRACK
        result.`object` = trackItem
        result.relatedObject = GPXInfo(trackItem.name, trackItem.file)
        openRoutePreview(settingsAction, surfaceRenderer, result)
    }
}