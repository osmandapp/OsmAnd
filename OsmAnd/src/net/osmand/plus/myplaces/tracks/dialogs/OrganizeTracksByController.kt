package net.osmand.plus.myplaces.tracks.dialogs

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.containers.ScreenItem
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.DIALOG_SUMMARY
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.DIVIDER_FULL
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.DIVIDER_WITH_PADDING
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.GROUP_HEADER
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.SELECTABLE_ITEM
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.SPACE
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.shared.gpx.organization.enums.OrganizeByCategory
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import net.osmand.util.CollectionUtils

class OrganizeTracksByController(
    val app: OsmandApplication,
    private val folderId: String
): BaseDialogController(app) {

    companion object {
        const val PROCESS_ID = "select_organize_tracks_by_type"

        fun showDialog(
            app: OsmandApplication,
            fragmentManager: FragmentManager,
            folderId: String,
            appMode: ApplicationMode
        ) {
            val controller = OrganizeTracksByController(app, folderId)
            app.dialogManager.register(PROCESS_ID, controller)
            if (!OrganizeTracksByFragment.showInstance(fragmentManager, appMode)) {
                app.dialogManager.unregister(PROCESS_ID)
            }
        }

        fun getExistedInstance(app: OsmandApplication): OrganizeTracksByController? {
            return app.dialogManager.findController(PROCESS_ID) as? OrganizeTracksByController
        }
    }

    var fragmentActivity: FragmentActivity? = null

    // Current selection state (null represents "None")
    var selectedType: OrganizeByType? = null
        private set

    init {
        val savedType = app.organizeTracksHelper.getOrganizeByType(folderId)
        selectedType = savedType
    }

    override fun getProcessId(): String = PROCESS_ID

    fun populateScreenItems(): List<ScreenItem> {
        val items = mutableListOf<ScreenItem>()

        items.add(ScreenItem(DIALOG_SUMMARY))
        //'None' option represented as a 'null' value
        items.add(ScreenItem(SELECTABLE_ITEM, null))

        var group: OrganizeByCategory? = null
        for (type in OrganizeByType.entries) {
            val currentGroup = type.category
            if (group != currentGroup) {
                group = currentGroup
                items.add(ScreenItem(DIVIDER_FULL))
                items.add(ScreenItem(GROUP_HEADER, group))
            }
            items.add(ScreenItem(SELECTABLE_ITEM, type))
            if (shouldAddParagraphDivider(type)) {
                items.add(ScreenItem(DIVIDER_WITH_PADDING))
            }
        }
        items.add(ScreenItem(DIVIDER_FULL))
        items.add(ScreenItem(SPACE))
        return items
    }

    private fun shouldAddParagraphDivider(type: OrganizeByType): Boolean {
        return CollectionUtils.equalsToAny(
            type,
            OrganizeByType.AVG_ALTITUDE, OrganizeByType.SENSOR_SPEED_AVG,
            OrganizeByType.HEART_RATE_AVG, OrganizeByType.CADENCE_AVG, OrganizeByType.POWER_AVG
        )
    }

    fun selectType(type: OrganizeByType?) {
        if (selectedType != type) {
            selectedType = type
            dialogManager.askRefreshDialogCompletely(PROCESS_ID)
        }
    }

    fun askSaveChanges() {
        app.organizeTracksHelper.setOrganizeByType(folderId, selectedType)
    }
}