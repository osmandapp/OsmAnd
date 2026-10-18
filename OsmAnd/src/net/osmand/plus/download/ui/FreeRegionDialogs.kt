package net.osmand.plus.download.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.map.WorldRegion
import net.osmand.plus.AppInitializeListener
import net.osmand.plus.AppInitializer
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.chooseplan.OsmAndFeature
import net.osmand.plus.download.DownloadActivity
import net.osmand.plus.download.FreeRegionHelper
import net.osmand.plus.download.FreeRegionServer
import net.osmand.plus.download.local.LocalOperationTask
import net.osmand.plus.download.local.OperationType
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils

/**
 * Dialogs of the free version: a map of another region asks to replace the free region (the server
 * decides); when that is not allowed - an email gives the replace, otherwise Maps+.
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
		val app = activity.application as OsmandApplication
		val message = StringBuilder(activity.getString(R.string.free_region_other_descr,
			freeRegion.localeName, region.localeName))
		val toDelete = FreeRegionHelper(app).collectMapsToDelete(region)
		if (toDelete.isNotEmpty()) {
			message.append("\n\n").append(activity.getString(R.string.free_region_replace_note))
			for ((deleted, items) in toDelete) {
				val size = AndroidUtils.formatSize(app, FreeRegionHelper.getSize(items))
				message.append("\n• ").append(deleted.localeName).append(": ")
					.append(activity.getString(R.string.free_region_maps_size, items.size, size))
			}
		}
		MaterialAlertDialogBuilder(materialContext(activity))
			.setTitle(activity.getString(R.string.free_region_other_title, region.localeName))
			.setMessage(message)
			.setNeutralButton(R.string.free_region_get_maps_plus) { _, _ -> showChoosePlan(activity) }
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_replace) { _, _ ->
				requestReplace(activity, freeRegion, region, onReplaced)
			}
			.show()
	}

	private fun requestReplace(activity: FragmentActivity, from: WorldRegion, to: WorldRegion, onReplaced: Runnable) {
		val app = activity.application as OsmandApplication
		val progress = showProgress(activity)
		FreeRegionServer(app).requestReplace(from, to) { allowed ->
			if (!AndroidUtils.isActivityNotDestroyed(activity)) {
				return@requestReplace
			}
			progress.dismiss()
			if (allowed) {
				applyReplace(activity, to, onReplaced)
			} else {
				showNotEligible(activity, to, onReplaced)
			}
		}
	}

	private fun applyReplace(activity: FragmentActivity, region: WorldRegion, onReplaced: Runnable) {
		val app = activity.application as OsmandApplication
		val helper = FreeRegionHelper(app)
		val items = helper.collectMapsToDelete(region).values.flatten()
		helper.setFreeRegion(region)
		if (items.isEmpty()) {
			onReplaced.run()
			return
		}
		val listener = object : LocalOperationTask.OperationListener {
			override fun onOperationFinished(type: OperationType, result: String) {
				onReplaced.run()
			}
		}
		OsmAndTaskManager.executeTask(
			LocalOperationTask(app, OperationType.DELETE_OPERATION, listener), *items.toTypedArray())
	}

	private fun showNotEligible(activity: FragmentActivity, to: WorldRegion, onReplaced: Runnable) {
		val app = activity.application as OsmandApplication
		val context = materialContext(activity)
		val builder = MaterialAlertDialogBuilder(context)
			.setTitle(R.string.free_region_not_eligible_title)
		if (app.settings.EMAIL_SUBSCRIBED.get()) {
			builder.setMessage(R.string.free_region_not_eligible_buy_descr)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.free_region_get_maps_plus) { _, _ -> showChoosePlan(activity) }
				.show()
			return
		}
		val view = LayoutInflater.from(context).inflate(R.layout.dialog_free_region_email, null)
		val emailLayout: TextInputLayout = view.findViewById(R.id.email_layout)
		val emailEdit: TextInputEditText = view.findViewById(R.id.email_edit)
		val dialog = builder.setMessage(R.string.free_region_not_eligible_email_descr)
			.setView(view)
			.setNeutralButton(R.string.free_region_get_maps_plus) { _, _ -> showChoosePlan(activity) }
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_send, null)
			.show()

		fun email() = emailEdit.text?.toString()?.trim().orEmpty()
		emailEdit.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus && email().isNotEmpty() && !AndroidUtils.isValidEmail(email())) {
				emailLayout.error = activity.getString(R.string.osm_live_enter_email)
			}
		}
		emailEdit.doAfterTextChanged {
			if (emailLayout.error != null && AndroidUtils.isValidEmail(email())) {
				emailLayout.error = null
			}
		}
		/* the dialog stays open while the address is wrong - the error is shown under the field */
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
			if (!AndroidUtils.isValidEmail(email())) {
				emailLayout.error = activity.getString(R.string.osm_live_enter_email)
				return@setOnClickListener
			}
			dialog.dismiss()
			val progress = showProgress(activity)
			FreeRegionServer(app).registerEmail(email()) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss()
					// a left email always allows the replace - no second check by the server
					applyReplace(activity, to, onReplaced)
				}
			}
		}
	}

	private fun showProgress(activity: FragmentActivity): AlertDialog {
		val context = materialContext(activity)
		val padding = context.resources.getDimensionPixelSize(R.dimen.ui_dialog_padding)
		val container = FrameLayout(context)
		container.setPaddingRelative(padding, padding / 2, padding, 0)
		container.addView(CircularProgressIndicator(context).apply { isIndeterminate = true })
		return MaterialAlertDialogBuilder(context)
			.setTitle(R.string.shared_string_loading)
			.setView(container)
			.setCancelable(false)
			.show()
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
