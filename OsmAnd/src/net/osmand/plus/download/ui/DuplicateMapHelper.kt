package net.osmand.plus.download.ui

import net.osmand.plus.OsmandApplication
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.DownloadResourceGroup
import net.osmand.plus.download.IndexItem

object DuplicateMapHelper {

    @JvmStatic
    fun findConflictingItem(
        app: OsmandApplication,
        target: IndexItem,
        group: DownloadResourceGroup
    ): IndexItem? {
        if (target.getExistedFile(app) != null) {
            return null
        }
        val targetType = target.type
        val conflictType = when (targetType) {
            DownloadActivityType.NORMAL_FILE -> DownloadActivityType.ROADS_FILE
            DownloadActivityType.ROADS_FILE -> DownloadActivityType.NORMAL_FILE
            else -> return null
        }

        return group.individualResources?.find {
            it.type == conflictType && it.getExistedFile(app) != null
        }
    }
}
