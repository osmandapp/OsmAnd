package net.osmand.plus.download.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.map.WorldRegion
import net.osmand.plus.AppInitializeListener
import net.osmand.plus.AppInitializer
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.chooseplan.OsmAndFeature
import net.osmand.plus.download.DownloadActivity
import net.osmand.plus.download.FreeRegionHelper
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils

/**
 * Dialogs of the free version: a map of another region opens [FreeRegionBottomSheet]; after the
 * update of the app - the warning to update the maps.
 */
object FreeRegionDialogs {

	/**
	 * Dialogs have to be built in the Material 3 theme - the download screens still use the
	 * Material 2 activity theme.
	 */
	@JvmStatic
	fun materialContext(activity: FragmentActivity): Context {
		val app = activity.application as OsmandApplication
		val nightMode = app.daynightHelper.isNightMode(app.settings.applicationMode, ThemeUsageContext.APP)
		return ContextThemeWrapper(activity,
			if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme)
	}

	@JvmStatic
	fun showOtherRegion(activity: FragmentActivity, freeRegion: WorldRegion, region: WorldRegion, onReplaced: Runnable) {
		FreeRegionBottomSheet.showInstance(activity, freeRegion, region, onReplaced)
	}

	/**
	 * After the update of the app: the new search does not work with the old maps. Waits for the app
	 * initialization - the maps are matched to regions only when the region tree is loaded.
	 */
	@JvmStatic
	fun showUpdateMapsWarning(activity: FragmentActivity) {
		val app = activity.application as OsmandApplication
		app.appInitializer.addListener(object : AppInitializeListener {
			override fun onFinish(init: AppInitializer) {
				init.removeListener(this)
				app.runInUIThread {
					val helper = FreeRegionHelper(app)
					if (AndroidUtils.isActivityNotDestroyed(activity) && helper.shouldShowUpdateWarning()) {
						helper.setUpdateWarningShown()
						MaterialAlertDialogBuilder(materialContext(activity))
							.setTitle(R.string.free_region_update_maps_title)
							.setMessage(R.string.free_region_update_maps_descr)
							.setNegativeButton(R.string.later, null)
							.setPositiveButton(R.string.shared_string_update) { _, _ -> openUpdates(activity) }
							.show()
					}
				}
			}
		})
	}

	private fun openUpdates(activity: FragmentActivity) {
		val app = activity.application as OsmandApplication
		val intent = Intent(activity, app.appCustomization.downloadIndexActivity)
		intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.UPDATES_TAB)
		activity.startActivity(intent)
	}

	@JvmStatic
	fun showChoosePlan(activity: FragmentActivity) {
		ChoosePlanFragment.showInstance(activity, OsmAndFeature.UNLIMITED_MAP_DOWNLOADS)
	}
}
