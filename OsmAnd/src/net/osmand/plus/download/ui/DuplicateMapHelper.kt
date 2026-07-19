package net.osmand.plus.download.ui

import net.osmand.plus.OsmandApplication
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.DownloadResourceGroup
import net.osmand.plus.download.IndexItem
import java.io.File

object DuplicateMapHelper {

    @JvmStatic
    fun findConflictingFile(
        app: OsmandApplication,
        target: IndexItem,
        group: DownloadResourceGroup
    ): File? {
        if (target.getExistedFile(app) != null) {
            return null
        }
        val conflictingType = when (target.type) {
            DownloadActivityType.NORMAL_FILE -> DownloadActivityType.ROADS_FILE
            DownloadActivityType.ROADS_FILE -> DownloadActivityType.NORMAL_FILE
            else -> return null
        }

        val conflictingItem = group.individualResources?.find { it.type == conflictingType }
        return conflictingItem?.getExistedFile(app)
    }
}
