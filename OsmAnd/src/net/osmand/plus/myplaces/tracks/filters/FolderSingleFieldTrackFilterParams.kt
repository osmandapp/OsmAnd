package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.util.Algorithms

class FolderSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun getItemText(app: OsmandApplication, itemName: String): String {
		return if (Algorithms.isEmpty(itemName)) {
			app.getString(R.string.root_folder)
		} else {
			itemName.replace("/", " / ")
		}
	}

	override fun getItemIcon(app: OsmandApplication, itemName: String): Drawable? {
		return app.uiUtilities.getPaintedIcon(
			R.drawable.ic_action_folder,
			app.getColor(R.color.icon_color_default_light))
	}

	override fun getAllItemsIcon(
		app: OsmandApplication,
		isChecked: Boolean,
		nightMode: Boolean): Drawable? {
		return if (isChecked) {
			app.uiUtilities.getActiveIcon(
				R.drawable.ic_action_group_select_all,
				nightMode)
		} else {
			app.uiUtilities.getPaintedIcon(
				R.drawable.ic_action_group_select_all,
				app.getColor(R.color.icon_color_default_light))
		}
	}

	override fun hasSelectAllVariant(): Boolean {
		return true
	}

	override fun sortByName(): Boolean {
		return true
	}
	override fun sortDescending(): Boolean {
		return false
	}
}