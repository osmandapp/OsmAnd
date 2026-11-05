package net.osmand.plus.download.ui

import android.content.Intent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.PlatformUtil
import net.osmand.plus.AppVersionUpgradeOnInit
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.download.DownloadActivity
import net.osmand.plus.download.local.LocalItemType
import net.osmand.plus.download.local.LocalItemUtils
import net.osmand.plus.helpers.FileNameTranslationHelper
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.util.Algorithms
import java.text.ParseException

/**
 * The new search of 5.5 does not work with maps built before 01.07.2026. Once, on the update from an
 * older version - when What's New is closed - the user is asked to update such maps; the updates are
 * free in the free version too.
 */
class OutdatedMapsWarning(private val app: OsmandApplication) {

	fun showAfterUpgrade(activity: FragmentActivity) {
		if (app.appInitializer.prevAppVersion >= AppVersionUpgradeOnInit.VERSION_5_5_00) {
			return
		}
		// map dates and regions are known only when the app is initialized
		app.appInitializer.addOnFinishListener {
			if (AndroidUtils.isActivityNotDestroyed(activity) && hasMapsOlderThanNewSearch()) {
				show(activity)
			}
		}
	}

	/** Standard and roads-only maps of a region; world and custom maps are left out. */
	private fun hasMapsOlderThanNewSearch(): Boolean {
		val resourceManager = app.resourceManager
		val dates = resourceManager.indexFileNames
		val dateFormat = resourceManager.dateFormat
		for ((fileName, file) in resourceManager.indexFiles) {
			val type = LocalItemUtils.getItemType(app, file)
			if (type != LocalItemType.MAP_DATA && type != LocalItemType.ROAD_DATA || !isRegionMap(fileName)) {
				continue
			}
			val date = try {
				dates[fileName]?.let { dateFormat.parse(it)?.time }
			} catch (e: ParseException) {
				LOG.error(e)
				null
			}
			if ((date ?: file.lastModified()) < NEW_SEARCH_MAPS_DATE) {
				return true
			}
		}
		return false
	}

	private fun isRegionMap(fileName: String): Boolean {
		// "Ukraine_kyiv-city_europe_2.road.obf" -> "ukraine_kyiv-city_europe"
		val downloadName = Algorithms.removeFileVersionSuffix(fileName.substringBefore('.')).lowercase()
		return FileNameTranslationHelper.getStandardMapName(app, downloadName) == null
				&& app.regions.getRegionDataByDownloadName(downloadName) != null
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

	companion object {
		private val LOG = PlatformUtil.getLog(OutdatedMapsWarning::class.java)

		/** 01.07.2026: maps built before it do not work with the new search. */
		private const val NEW_SEARCH_MAPS_DATE = 1782864000000L
	}
}
