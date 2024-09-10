package net.osmand.plus.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import net.osmand.OnCompleteCallback
import net.osmand.PlatformUtil
import net.osmand.SharedUtil.loadGpxFile
import net.osmand.plus.OsmandApplication
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper
import net.osmand.plus.track.helpers.save.SaveGpxHelper
import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxParameter.ACTIVITY_TYPE
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.primitives.RouteActivity
import net.osmand.shared.gpx.primitives.RouteActivityGroup
import net.osmand.util.Algorithms
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

private const val ROUTE_ACTIVITIES_FILE = "activities.json"

class RouteActivityHelper(
	val app: OsmandApplication
) {
	private val LOG = PlatformUtil.getLog(RouteActivitySelectionHelper::class.java)

	private var cachedGroups = mutableListOf<RouteActivityGroup>()
	private var cachedActivities = mutableListOf<RouteActivity>()

	fun findRouteActivity(id: String?) = id?.let { getActivities().firstOrNull { it.id == id } }

	fun getActivityGroups(): List<RouteActivityGroup> {
		if (cachedGroups.isEmpty()) {
			collectRouteActivities()
		}
		return cachedGroups
	}

	fun getActivities(): List<RouteActivity> {
		if (cachedActivities.isEmpty()) {
			collectRouteActivities()
		}
		return cachedActivities
	}

	fun saveRouteActivity(trackItems: Collection<TrackItem>, routeActivity: RouteActivity?) {
		runAsync {
			trackItems.forEach { trackItem ->
				trackItem.getFile()?.let { file ->
					val selectedGpxFile = app.selectedGpxHelper.getSelectedFileByPath(file.absolutePath())
					val gpxFile = selectedGpxFile?.gpxFile ?: loadGpxFile(file)
					if (gpxFile.error == null) {
						saveRouteActivityAsync(gpxFile, routeActivity)
					}
				}
			}
		}
	}

	private suspend fun saveRouteActivityAsync(gpxFile: GpxFile, routeActivity: RouteActivity?): Unit =
		suspendCancellableCoroutine { continuation ->
			saveRouteActivity(gpxFile, routeActivity) {
				if (continuation.isActive) {
					continuation.resume(Unit)
				}
			}
		}

	fun saveRouteActivity(gpxFile: GpxFile, routeActivity: RouteActivity?) {
		saveRouteActivity(gpxFile, routeActivity, callback = null)
	}

	private fun saveRouteActivity(gpxFile: GpxFile, routeActivity: RouteActivity?, callback: OnCompleteCallback? = null) {
		gpxFile.metadata.setRouteActivity(routeActivity, getActivities())

		SaveGpxHelper.saveGpx(gpxFile) {
			val updateRouteActivity: (dataItem: GpxDataItem) -> Unit = { dataItem ->
				val activityId = routeActivity?.id.orEmpty()
				app.gpxDbHelper.updateDataItemParameter(dataItem, ACTIVITY_TYPE, activityId)
				callback?.onComplete()
			}
			val dataItem = app.gpxDbHelper.getItem(File(gpxFile.path)) {
				updateRouteActivity(it)
			}
			dataItem?.let {
				updateRouteActivity(it)
			}
		}
	}

	private fun collectRouteActivities() {
		try {
			readActivitiesFromJson()
		} catch (e: JSONException) {
			LOG.error("Failed to read activities from JSON", e)
		}
	}

	@Throws(JSONException::class)
	private fun readActivitiesFromJson() {
		var activitiesJsonStr: String? = null
		try {
			val `is` = app.assets.open(ROUTE_ACTIVITIES_FILE)
			activitiesJsonStr = Algorithms.readFromInputStream(`is`).toString()
		} catch (e: IOException) {
			LOG.error("Failed to read activities source file", e)
		}
		if (Algorithms.isEmpty(activitiesJsonStr)) return

		val json = JSONObject(activitiesJsonStr!!)
		val groupsArray = json.getJSONArray("groups")
		for (i in 0 until groupsArray.length()) {
			val groupJson = groupsArray.getJSONObject(i)
			val id = groupJson.getString("id")
			val label = groupJson.getString("label")
			val activitiesJson = groupJson.getJSONArray("activities")
			val activities: MutableList<RouteActivity> = ArrayList()
			val activitiesGroup = RouteActivityGroup(id, label, activities)
			for (j in 0 until activitiesJson.length()) {
				val activityJson = activitiesJson.getJSONObject(j)
				val activityId = activityJson.getString("id")
				val activityLabel = activityJson.getString("label")
				val iconName = activityJson.getString("icon_name")
				val activity = RouteActivity(activityId, activityLabel, iconName, activitiesGroup)
				cachedActivities.add(activity)
				activities.add(activity)
			}
			cachedGroups.add(activitiesGroup)
		}
	}

	private fun runAsync(block: suspend () -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
			block()
		}
	}
}