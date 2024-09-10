package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class ActivitySingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

	override fun getItemIcon(app: OsmandApplication, itemName: String, selected: Boolean, nightMode: Boolean): Drawable? {
		val routeActivity = app.routeActivityHelper.findRouteActivity(itemName)

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

	override fun getItemText(app: OsmandApplication, itemName: String, selected: Boolean): String {
		val routeActivity = app.routeActivityHelper.findRouteActivity(itemName)
		return routeActivity?.label ?: app.getString(R.string.shared_string_none)
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}

}