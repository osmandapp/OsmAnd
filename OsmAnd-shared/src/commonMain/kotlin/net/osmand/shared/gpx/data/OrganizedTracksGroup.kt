package net.osmand.shared.gpx.data

import kotlinx.serialization.Transient
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.filters.TrackFolderAnalysis
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

class OrganizedTracksGroup(
    private val id: String,
    private val type: OrganizeByType,
    private val representedValue: Any,
    private val trackItems: List<TrackItem>,
    private val relatedSmartFolder: SmartFolder,
    private val resourcesMapper: OrganizeTracksResourceMapper
) : TracksGroup, ComparableTracksGroup {

    @Transient
    private var groupAnalysis: TrackFolderAnalysis? = null

    override fun getId() = id

    override fun getName() = resourcesMapper.getName(type, representedValue)

    override fun getTrackItems() = trackItems

    fun getIconName() = resourcesMapper.getIconName(type, representedValue)

    fun getType() = type

    fun getRelatedSmartFolder() = relatedSmartFolder

    override fun getDirName(includingSubdirs: Boolean) = getName()

    override fun lastModified() = 0L

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
}