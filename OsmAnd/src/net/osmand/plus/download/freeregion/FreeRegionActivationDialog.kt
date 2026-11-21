package net.osmand.plus.download.freeregion

import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.map.WorldRegion
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.download.local.BaseLocalItem
import net.osmand.plus.download.local.LocalItemUtils
import net.osmand.plus.download.local.LocalOperationTask
import net.osmand.plus.download.local.LocalOperationTask.OperationListener
import net.osmand.plus.download.local.OperationType
import net.osmand.plus.settings.enums.ThemeUsageContext
import java.util.function.Consumer

/**
 * Activating deactivated maps. The free version keeps the maps of one region active: maps of another
 * region first ask whether the maps of other regions may be deactivated, then that region becomes
 * free - its maps are on the device already, so there is no server call and no replace is counted.
 */
class FreeRegionActivationDialog(private val app: OsmandApplication) {

	private val freeVersionMapDownloads: FreeVersionMapDownloads
		get() = app.downloadThread.freeVersionMapDownloads

	/** Activates the items, after the prompt when they change the free region. */
	fun activate(activity: FragmentActivity, items: Array<out BaseLocalItem>, listener: OperationListener?) {
		activate(activity, items) { itemsToActivate -> restore(itemsToActivate, listener) }
	}

	/**
	 * Runs [onActivate] with the items to activate - right away, or after the prompt when they change
	 * the free region. For a screen that activates with its own task.
	 */
	fun activate(activity: FragmentActivity, items: Array<out BaseLocalItem>,
	             onActivate: Consumer<List<BaseLocalItem>>) {
		val region = freeVersionMapDownloads.getActivatedRegion(LocalItemUtils.collectLocalItems(items.toSet()))
		if (region == null) {
			onActivate.accept(items.toList())
			return
		}
		val otherRegions = freeVersionMapDownloads.collectMapsToDelete(region).keys
		if (otherRegions.isEmpty()) {
			activateRegion(region, items, onActivate)
			return
		}
		// the screens of maps are Material 2, the dialog is Material 3 like the other free region dialogs
		val nightMode = app.daynightHelper.isNightMode(app.settings.applicationMode, ThemeUsageContext.APP)
		val context = ContextThemeWrapper(activity,
			if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme)
		MaterialAlertDialogBuilder(context)
			.setTitle(R.string.free_region_activate_title)
			.setMessage(app.getString(R.string.free_region_activate_descr, region.localeName,
				otherRegions.joinToString { it.localeName }))
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_change) { _, _ -> activateRegion(region, items, onActivate) }
			.show()
	}

	private fun activateRegion(region: WorldRegion, items: Array<out BaseLocalItem>,
	                           onActivate: Consumer<List<BaseLocalItem>>) {
		val mapsToDeactivate = freeVersionMapDownloads.activateFreeRegion(region)
		// maps of a third region in the same selection stay deactivated
		val itemsToActivate = items.filter {
			freeVersionMapDownloads.getActivatedRegion(LocalItemUtils.collectLocalItems(setOf(it))) == null
		}
		val onDeactivated = object : OperationListener {
			override fun onOperationFinished(type: OperationType, result: String) {
				onActivate.accept(itemsToActivate)
			}
		}
		OsmAndTaskManager.executeTask(LocalOperationTask(app, OperationType.BACKUP_OPERATION, onDeactivated),
			*mapsToDeactivate.toTypedArray())
	}

	private fun restore(items: List<BaseLocalItem>, listener: OperationListener?) {
		OsmAndTaskManager.executeTask(LocalOperationTask(app, OperationType.RESTORE_OPERATION, listener),
			*items.toTypedArray())
	}
}
