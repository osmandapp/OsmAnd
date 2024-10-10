package net.osmand.plus.auto.screens

import android.os.AsyncTask
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.shared.SharedUtil
import net.osmand.plus.R
import net.osmand.plus.auto.TripHelper
import net.osmand.plus.configmap.tracks.TrackTab
import net.osmand.plus.configmap.tracks.TrackTabType
import net.osmand.plus.settings.enums.CompassMode
import net.osmand.plus.track.data.GPXInfo
import net.osmand.shared.gpx.GpxDbHelper
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.plus.views.layers.base.OsmandMapLayer.CustomMapObjects
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.jFile
import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxParameter.NEAREST_CITY_NAME
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.util.Algorithms

class TracksScreen(
	carContext: CarContext,
	private val settingsAction: Action,
	private val trackTab: TrackTab
) : BaseAndroidAutoScreen(carContext) {
	val gpxDbHelper: GpxDbHelper = app.gpxDbHelper
	private var loadedGpxFiles = HashMap<TrackItem, SelectedGpxFile>()
	private lateinit var loadTracksTask: LoadTracksTask
	private var initialCompassMode: CompassMode? = null

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
				app.osmandMap.mapLayers.gpxLayer.customObjectsDelegate = null
				app.osmandMap.mapView.backToLocation()
				initialCompassMode?.let {
					app.mapViewTrackingUtilities.switchCompassModeTo(it)
				}
			}

	        override fun onStart(owner: LifecycleOwner) {
		        recenterMap()
		        app.osmandMap.mapLayers.gpxLayer.customObjectsDelegate = CustomMapObjects()
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
            trackTab.getName(app)
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
            track.getFile()?.let { file ->
                val item = gpxDbHelper.getItem(file) { updateTrack(track, it) }
                if (item != null) {
                    track.dataItem = item
                }
                val gpxFile = GpxUtilities.loadGpxFile(file)
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
		val tracks = trackTab.trackItems.subList(0, tracksSize.coerceAtMost(contentLimit - 1))
		val mapRect = KQuadRect()
		if (!Algorithms.isEmpty(tracks)) {
			initialCompassMode = app.settings.compassMode
			app.mapViewTrackingUtilities.switchCompassModeTo(CompassMode.NORTH_IS_UP)
		}
		for (track in tracks) {
			val gpxFile = loadedGpxFiles[track]
			gpxFile?.let {
				selectedGpxFiles.add(it)
				val gpxRect: KQuadRect = it.gpxFile.getRect()
				KAlgorithms.extendRectToContainRect(mapRect, gpxRect)
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
				description = dataItem.getParameter(NEAREST_CITY_NAME) ?: ""
				val latLonStart = dataItem.getAnalysis()?.getLatLonStart()
				dist = if (latLonStart == null) {
					0f
				} else {
					KMapUtils.getDistance(SharedUtil.kLatLon(latLon), latLonStart).toFloat()
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
		adjustMapToRect(latLon, SharedUtil.jQuadRect(mapRect))
		app.osmandMap.mapLayers.gpxLayer.setCustomMapObjects(selectedGpxFiles)
		templateBuilder.setItemList(listBuilder.build())
	}

    private fun onClickTrack(trackItem: TrackItem) {
        val result = SearchResult()
        result.objectType = ObjectType.GPX_TRACK
        result.`object` = trackItem
	    val file = trackItem.getFile()
	    result.relatedObject = GPXInfo(trackItem.name, file?.jFile())
        openRoutePreview(settingsAction, result)
    }
}