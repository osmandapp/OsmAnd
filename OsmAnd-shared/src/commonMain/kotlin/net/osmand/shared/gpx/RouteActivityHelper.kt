package net.osmand.shared.gpx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.osmand.shared.gpx.GpxParameter.ACTIVITY_TYPE
import net.osmand.shared.gpx.primitives.RouteActivity
import net.osmand.shared.gpx.primitives.RouteActivityGroup
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import kotlin.coroutines.resume

private typealias OnCompleteCallback = () -> Unit

object RouteActivityHelper {
	private val log = LoggerFactory.getLogger("RouteActivityHelper")

	private const val ROUTE_ACTIVITIES_FILE = "activities.json"

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
					val gpxFile = PlatformUtil.getOsmAndContext().getSelectedFileByPath(file.absolutePath())
					if (gpxFile != null && gpxFile.error == null) {
						saveRouteActivityAsync(gpxFile, routeActivity)
					}
				}
			}
		}
	}

	private suspend fun saveRouteActivityAsync(gpxFile: GpxFile, routeActivity: RouteActivity?) =
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
		val file = KFile(gpxFile.path)
		CoroutineScope(Dispatchers.Main).launch {
			withContext(Dispatchers.IO) {
				GpxUtilities.writeGpxFile(file, gpxFile)
			}
			val updateRouteActivity: (dataItem: GpxDataItem) -> Unit = { dataItem ->
				val activityId = routeActivity?.id.orEmpty()
				GpxDbHelper.updateDataItemParameter(dataItem, ACTIVITY_TYPE, activityId)
				callback?.invoke()
			}
			val dataItem = GpxDbHelper.getItem(file) {
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
		} catch (e: Exception) {
			log.error("Failed to read activities from JSON", e)
		}
	}

	private fun readActivitiesFromJson() {
		val activitiesJsonStr: String? = PlatformUtil.getOsmAndContext().getAssetAsString(ROUTE_ACTIVITIES_FILE)
		if (activitiesJsonStr.isNullOrEmpty()) return

		val json = Json.parseToJsonElement(activitiesJsonStr)
		val groupsArray = json.jsonObject["groups"] as JsonArray

		for (groupElement in groupsArray) {
			val groupJson = groupElement.jsonObject
			val id = groupJson["id"]!!.jsonPrimitive.content
			val label = groupJson["label"]!!.jsonPrimitive.content
			val activitiesJson = groupJson["activities"]!!.jsonArray

			val activities = mutableListOf<RouteActivity>()
			val activitiesGroup = RouteActivityGroup(id, label, activities)

			for (activityElement in activitiesJson) {
				val activityJson = activityElement.jsonObject
				val activityId = activityJson["id"]!!.jsonPrimitive.content
				val activityLabel = activityJson["label"]!!.jsonPrimitive.content
				val iconName = activityJson["icon_name"]!!.jsonPrimitive.content
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