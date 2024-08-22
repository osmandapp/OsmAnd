package net.osmand.shared.filters

import net.osmand.plus.myplaces.tracks.filters.SingleFieldTrackFilterParams
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil

class FolderSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			PlatformUtil.getStringResource("root_folder")
		} else {
			itemName.replace("/", " / ")
		}
	}

//	override fun getItemIcon(itemName: String): Drawable? {
//		return app.uiUtilities.getPaintedIcon(
//			R.drawable.ic_action_folder,
//			app.getColor(R.color.icon_color_default_light))
//	}
//
//	override fun getAllItemsIcon(
//		app: OsmandApplication,
//		isChecked: Boolean,
//		nightMode: Boolean): Drawable? {
//		return if (isChecked) {
//			app.uiUtilities.getActiveIcon(
//				R.drawable.ic_action_group_select_all,
//				nightMode)
//		} else {
//			app.uiUtilities.getPaintedIcon(
//				R.drawable.ic_action_group_select_all,
//				app.getColor(R.color.icon_color_default_light))
//		}
//	}

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