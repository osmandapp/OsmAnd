package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem

class OrganizedTracks(
    private val id: String,
    private val name: String,
    private val iconName: String,
    private val trackItems: List<TrackItem>,
    private val relatedSmartFolder: SmartFolder
) : TracksGroup {

    override fun getId() = id

    override fun getName() = name

    override fun getTrackItems() = trackItems

    fun getIconName() = iconName

    fun getRelatedSmartFolder() = relatedSmartFolder

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as OrganizedTracks
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}