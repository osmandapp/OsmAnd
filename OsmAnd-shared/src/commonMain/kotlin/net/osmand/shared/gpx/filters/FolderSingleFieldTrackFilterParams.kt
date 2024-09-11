package net.osmand.shared.gpx.filters

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization
import net.osmand.shared.util.PlatformUtil

class FolderSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			Localization.getString("root_folder")
		} else {
			itemName.replace("/", " / ")
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