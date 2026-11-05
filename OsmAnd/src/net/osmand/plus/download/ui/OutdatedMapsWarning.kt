package net.osmand.plus.download.ui

import android.content.Intent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.IndexConstants
import net.osmand.plus.AppVersionUpgradeOnInit
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.download.DownloadActivity
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.spatial.SpatialTextSearchAPI

/**
 * The new search of 5.5 needs maps built from 2026-07 on. Once, on the update from an older version -
 * when What's New is closed - the user is asked to update older maps; the updates are free in the free
 * version too.
 */
class OutdatedMapsWarning(private val app: OsmandApplication) {

	fun showAfterUpgrade(activity: FragmentActivity) {
		if (app.appInitializer.prevAppVersion >= AppVersionUpgradeOnInit.VERSION_5_5_00) {
			return
		}
		// the maps are read only when the app is initialized
		app.appInitializer.addOnFinishListener {
			if (AndroidUtils.isActivityNotDestroyed(activity) && hasMapsWithoutSpatialSearch()) {
				show(activity)
			}
		}
	}

	/**
	 * Maps with POI data but without the new search - the same check as the search makes
	 * ([SpatialTextSearchAPI.hasPoiTypesInNameIndex]). The world basemap, Wikipedia and travel maps
	 * don't count.
	 */
	private fun hasMapsWithoutSpatialSearch(): Boolean {
		return app.resourceManager.fileReaders.any { resource ->
			val fileName = resource.fileName
			val reader = resource.shallowReader
			reader != null && !reader.isBasemap
					&& !fileName.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)
					&& !fileName.endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
					&& reader.containsPoiData()
					&& !SpatialTextSearchAPI.hasPoiTypesInNameIndex(reader)
		}
	}

	private fun show(activity: FragmentActivity) {
		// the map screen still uses the Material 2 activity theme, the dialog is Material 3
		val nightMode = app.daynightHelper.isNightMode(app.settings.applicationMode, ThemeUsageContext.APP)
		val context = ContextThemeWrapper(activity,
			if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme)
		MaterialAlertDialogBuilder(context)
			.setTitle(R.string.outdated_maps_warning_title)
			.setMessage(R.string.outdated_maps_warning_descr)
			.setNegativeButton(R.string.later, null)
			.setPositiveButton(R.string.shared_string_update) { _, _ -> openUpdates(activity) }
			.show()
	}

	private fun openUpdates(activity: FragmentActivity) {
		val intent = Intent(activity, app.appCustomization.downloadIndexActivity)
		intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.UPDATES_TAB)
		activity.startActivity(intent)
	}
}
