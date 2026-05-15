package net.osmand.plus.myplaces.favorites.dialogs

import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.myplaces.favorites.FavoriteFolderNode

class FavoriteFolderAnalysis {
    var pointsCount = 0
    var foldersCount = 0
    var fileSize = 0L

    constructor(group: FavoriteGroup) {
        analyzeGroup(group)
    }

    constructor(node: FavoriteFolderNode) {
        pointsCount = node.subtreePointsCount
        foldersCount = node.subtreeFoldersCount
        fileSize = node.subtreeFileSize
    }

    constructor(groups: List<FavoriteGroup>) {
        for (group in groups) {
            analyzeGroup(group)
            foldersCount++
        }
    }

    private fun analyzeGroup(group: FavoriteGroup) {
        pointsCount += group.points.size
        fileSize += group.size
    }
}