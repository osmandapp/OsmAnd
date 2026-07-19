package net.osmand.plus.download.ui

import android.graphics.Typeface
import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.download.AbstractDownloadActivity
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.IndexItem
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButtonType
import java.io.File

class DuplicateMapDownloadDialogController(
    app: OsmandApplication,
    private val targetItem: IndexItem,
    private val conflictingFile: File
) : BaseDialogController(app) {

    private enum class ConflictDirection {
        ROAD_TO_STANDARD,
        STANDARD_TO_ROAD
    }

    private val mapName = targetItem.getVisibleName(app, app.regions, false)
    private val direction = when (targetItem.type) {
        DownloadActivityType.NORMAL_FILE -> ConflictDirection.ROAD_TO_STANDARD
        DownloadActivityType.ROADS_FILE -> ConflictDirection.STANDARD_TO_ROAD
        else -> throw IllegalArgumentException("Unsupported duplicate map type: ${targetItem.type}")
    }

    override fun getProcessId(): String = PROCESS_ID

    val title: String
        get() = getString(R.string.duplicate_map)

    val description: CharSequence
        get() {
            val descRes = when (direction) {
                ConflictDirection.ROAD_TO_STANDARD -> R.string.duplicate_map_road_only_exists_desc
                ConflictDirection.STANDARD_TO_ROAD -> R.string.duplicate_map_standard_exists_desc
            }
            val fullDescription = getString(descRes, mapName)
            return UiUtilities.createSpannableString(fullDescription, Typeface.BOLD, mapName)
        }

    @get:StringRes
    val replaceButtonTitleId: Int
        get() = when (direction) {
            ConflictDirection.ROAD_TO_STANDARD -> R.string.duplicate_map_replace_with_standard
            ConflictDirection.STANDARD_TO_ROAD -> R.string.duplicate_map_replace_with_road
        }

    val replaceButtonType: DialogButtonType
        get() = when (direction) {
            ConflictDirection.ROAD_TO_STANDARD -> DialogButtonType.PRIMARY
            ConflictDirection.STANDARD_TO_ROAD -> DialogButtonType.SECONDARY
        }

    fun onReplace(activity: AbstractDownloadActivity) {
        activity.startReplacementDownload(targetItem, conflictingFile)
    }

    fun onKeepBoth(activity: AbstractDownloadActivity) {
        activity.startDownload(targetItem)
    }

    companion object {
        const val PROCESS_ID = "duplicate_map_download"

        fun getExistedInstance(app: OsmandApplication): DuplicateMapDownloadDialogController? {
            return app.dialogManager.findController(PROCESS_ID) as? DuplicateMapDownloadDialogController
        }

        @JvmStatic
        fun showDialog(
            activity: AbstractDownloadActivity,
            targetItem: IndexItem,
            conflictingFile: File
        ) {
            val manager = activity.supportFragmentManager
            if (!DuplicateMapDownloadBottomSheet.canBeAdded(manager)) {
                return
            }
            val app = activity.application as OsmandApplication
            val controller = DuplicateMapDownloadDialogController(app, targetItem, conflictingFile)
            app.dialogManager.register(PROCESS_ID, controller)
            DuplicateMapDownloadBottomSheet.showInstance(manager)
        }
    }
}
