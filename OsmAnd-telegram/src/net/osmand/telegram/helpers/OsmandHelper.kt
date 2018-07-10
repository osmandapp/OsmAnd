package net.osmand.telegram.helpers

import android.content.Intent
import android.net.Uri
import android.support.v4.app.FragmentActivity
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.ui.MainActivity
import net.osmand.telegram.utils.AndroidUtils

object OsmandHelper {

	private const val PREFIX = "osmand.api://"

	private const val API_CMD_SHOW_LOCATION = "show_location"

	private const val PARAM_LAT = "lat"
	private const val PARAM_LON = "lon"

	private const val PARAM_AMAP_LAYER_ID = "amap_layer_id"
	private const val PARAM_AMAP_POINT_ID = "amap_point_id"

	fun showUserOnMap(activity: FragmentActivity?, listItem: ListItem) {
		if (listItem.canBeOpenedOnMap()) {
			showLocationPointOnMap(
				activity,
				listItem.latLon?.latitude,
				listItem.latLon?.longitude,
				listItem.getMapPointId()
			)
		}
	}

	fun showLocationPointOnMap(
		activity: FragmentActivity?,
		lat: Double?,
		lon: Double?,
		pointId: String
	) {
		if (activity == null || lat == null || lon == null) {
			return
		}
		val params = mapOf(
			PARAM_LAT to lat.toString(),
			PARAM_LON to lon.toString(),
			PARAM_AMAP_LAYER_ID to MAP_LAYER_ID,
			PARAM_AMAP_POINT_ID to pointId
		)
		val intent = Intent(Intent.ACTION_VIEW, createUri(API_CMD_SHOW_LOCATION, params))
		if (AndroidUtils.isIntentSafe(activity, intent)) {
			activity.startActivity(intent)
		} else {
			MainActivity.OsmandMissingDialogFragment().show(activity.supportFragmentManager, null)
		}
	}

	private fun createUri(command: String, params: Map<String, String>): Uri {
		val sb = StringBuilder(PREFIX).append(command)
		if (params.isNotEmpty()) {
			sb.append("?")
			params.forEach { (key, value) -> sb.append("$key=$value&") }
			sb.delete(sb.length - 1, sb.length)
		}
		return Uri.parse(sb.toString())
	}
}
