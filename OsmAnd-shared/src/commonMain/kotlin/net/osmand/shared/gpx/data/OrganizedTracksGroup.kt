package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.organization.OrganizeByResourcesMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

class OrganizedTracksGroup(
    private val id: String,
    private val type: OrganizeByType,
    private val representedValue: Any,
    private val trackItems: List<TrackItem>,
    private val relatedSmartFolder: SmartFolder,
    private val resourcesMapper: OrganizeByResourcesMapper
) : TracksGroup {

    override fun getId() = id

    override fun getName() = resourcesMapper.getName(type, representedValue)

    override fun getTrackItems() = trackItems

    fun getIconName() = resourcesMapper.getIconName(type, representedValue)

    fun getType() = type

    fun getRelatedSmartFolder() = relatedSmartFolder

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