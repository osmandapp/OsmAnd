package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.gpx.GpxUtilities

class ActivitySingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

	private var routeActivitySelectionHelper: RouteActivitySelectionHelper? = null

	override fun getItemIcon(
		app: OsmandApplication,
		itemName: String,
		selected: Boolean,
		nightMode: Boolean
	): Drawable? {
		val helper = getActivityHelper()
		val routeActivity = GpxUtilities.findRouteActivity(itemName, helper.activities)

		val iconColor = if (selected) {
			ColorUtilities.getActiveColor(app, nightMode)
		} else {
			ColorUtilities.getDefaultIconColor(app, nightMode)
		}

		val iconId = if (routeActivity != null) {
			AndroidUtils.getIconId(app, routeActivity.iconName)
		} else {
			R.drawable.ic_action_activity
		}

		return app.uiUtilities.getPaintedIcon(iconId, iconColor)
	}

	override fun getItemText(
		app: OsmandApplication,
		itemName: String,
		selected: Boolean
	): String {
		val helper = getActivityHelper()
		val routeActivity = GpxUtilities.findRouteActivity(itemName, helper.activities)
		return routeActivity?.label ?: app.getString(R.string.shared_string_none)
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}

	private fun getActivityHelper(): RouteActivitySelectionHelper {
		if (routeActivitySelectionHelper == null) {
			routeActivitySelectionHelper = RouteActivitySelectionHelper()
		}
		return routeActivitySelectionHelper!!
	}

}