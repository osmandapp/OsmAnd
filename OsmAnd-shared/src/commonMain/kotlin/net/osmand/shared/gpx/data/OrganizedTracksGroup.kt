package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.organization.enums.OrganizeByType

class OrganizedTracksGroup(
    private val id: String,
    private val title: String,
    private val iconName: String,
    private val type: OrganizeByType,
    private val representedValue: Any,
    private val trackItems: List<TrackItem>,
    private val relatedSmartFolder: SmartFolder
) : TracksGroup {

    override fun getId() = id

    override fun getName() = title

    override fun getTrackItems() = trackItems

    fun getIconName() = iconName

    fun getType() = type

    fun getRepresentedValue() = representedValue

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