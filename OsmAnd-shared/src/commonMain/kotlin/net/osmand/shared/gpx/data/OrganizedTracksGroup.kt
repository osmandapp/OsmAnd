package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.filters.TrackFolderAnalysis
import net.osmand.shared.gpx.organization.enums.OrganizeByType

class OrganizedTracksGroup(
    private val id: String,
    private val name: String,
    private val iconName: String,
    private val type: OrganizeByType,
    private val sortValue: Double = 0.0,
    private val trackItems: List<TrackItem>,
    private val parentGroup: TracksGroup,
) : TracksGroup, ComparableTracksGroup {

    private var groupAnalysis: TrackFolderAnalysis? = null

    override fun getId() = id

    override fun getName() = name

    override fun getTrackItems() = trackItems

    fun getIconName() = iconName

    fun getType() = type

    fun getParentGroup() = parentGroup

    override fun getDirName(includingSubdirs: Boolean) = getName()

    override fun lastModified() = 0L

    override fun getSortValue() = sortValue

    override fun getFolderAnalysis(): TrackFolderAnalysis {
        var analysis = groupAnalysis
        if (analysis == null) {
            analysis = TrackFolderAnalysis(this)
            groupAnalysis = analysis
        }
        return analysis
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as OrganizedTracksGroup
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        private const val ORGANIZED_PREFIX = "organized_by_"

        fun createId(
            tracksGroup: TracksGroup,
            type: OrganizeByType,
            valuePart: String
        ): String {
            val parentId = tracksGroup.getId()
            val typeName = type.name.lowercase()
            return "${getBaseId(parentId)}${typeName}__${valuePart.lowercase()}"
        }

        fun getBaseId(parentId: String) = "${parentId}__${ORGANIZED_PREFIX}"
    }
}