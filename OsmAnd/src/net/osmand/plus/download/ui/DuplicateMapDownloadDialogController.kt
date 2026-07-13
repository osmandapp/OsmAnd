package net.osmand.plus.download.ui

import android.graphics.Typeface
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButtonType

class DuplicateMapDownloadDialogController(
    app: OsmandApplication,
    private val mapName: String,
    private val direction: ConflictDirection,
    private val replaceAction: Runnable,
    private val keepBothAction: Runnable
) : BaseDialogController(app) {

    enum class ConflictDirection {
        ROAD_TO_STANDARD,
        STANDARD_TO_ROAD
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

    fun onReplace() {
        replaceAction.run()
    }

    fun onKeepBoth() {
        keepBothAction.run()
    }

    companion object {
        const val PROCESS_ID = "duplicate_map_download"

        fun getExistedInstance(app: OsmandApplication): DuplicateMapDownloadDialogController? {
            return app.dialogManager.findController(PROCESS_ID) as? DuplicateMapDownloadDialogController
        }

        @JvmStatic
        fun showDialog(
            activity: FragmentActivity,
            mapName: String,
            direction: ConflictDirection,
            onReplace: Runnable,
            onKeepBoth: Runnable
        ) {
            val manager = activity.supportFragmentManager
            if (!DuplicateMapDownloadBottomSheet.canBeAdded(manager)) {
                return
            }
            val app = activity.application as OsmandApplication
            val controller = DuplicateMapDownloadDialogController(
                app,
                mapName,
                direction,
                onReplace,
                onKeepBoth
            )
            app.dialogManager.register(PROCESS_ID, controller)
            DuplicateMapDownloadBottomSheet.showInstance(manager)
        }
    }
}
