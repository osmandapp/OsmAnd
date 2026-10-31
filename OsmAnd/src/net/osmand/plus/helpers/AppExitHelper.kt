package net.osmand.plus.helpers

import android.content.Intent
import net.osmand.plus.NavigationService
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.activities.RestartActivity
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert

object AppExitHelper {

	private const val FAVORITES_SAVE_TIMEOUT_MS = 5000L

	@JvmStatic
	fun exit(activity: MapActivity, nightMode: Boolean) {
		if (hasRunningWork(activity.app)) {
			val dialogData = AlertDialogData(activity, nightMode)
				.setTitle(R.string.shared_string_exit)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_exit) { _, _ -> exitNow(activity) }
			CustomAlert.showSimpleMessage(dialogData, R.string.exit_app_running_work_warning)
		} else {
			exitNow(activity)
		}
	}

	private fun hasRunningWork(app: OsmandApplication): Boolean {
		return app.navigationService != null || app.downloadThread.isDownloading
	}

	private fun exitNow(activity: MapActivity) {
		val app = activity.app
		Thread({
			// Saving is asynchronous, killing the process now would lose a favorite that was just added
			app.favoritesHelper.fileHelper.waitForPendingSaves(FAVORITES_SAVE_TIMEOUT_MS)
			app.runInUIThread {
				// A running NavigationService would be redelivered by the system and start the process again
				app.stopService(Intent(app, NavigationService::class.java))
				activity.finishAndRemoveTask()
				RestartActivity.exitApp()
			}
		}, "AppExit").start()
	}
}
