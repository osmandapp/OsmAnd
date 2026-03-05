package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParams

object OrganizeByListStrategy : OrganizeByStrategy {

	override fun apply(
		originalGroup: TracksGroup,
		params: OrganizeByParams
	): List<OrganizedTracksGroup> {
		val type = params.type
		val property = type.getGpxParameter() ?: return emptyList()

		val groupedTracks = HashMap<String, MutableList<TrackItem>>()
		originalGroup.getTrackItems().let { trackItems ->
			for (trackItem in trackItems) {
				val key = trackItem.dataItem?.getParameter<String>(property) ?: ""
				groupedTracks.getOrPut(key) { mutableListOf() }.add(trackItem)
			}
		}

		val result = mutableListOf<OrganizedTracksGroup>()
		for (entry in groupedTracks.entries) {
			val value = entry.key.ifEmpty { "None" }
			val trackItems = entry.value
			val id = OrganizedTracksGroup.createId(originalGroup, type, value)
			result.add(OrganizedTracksGroup(
				id = id,
				name = getName(type, value),
				iconName = getIconName(type, value),
				type = type,
				trackItems = trackItems,
				parentGroup = originalGroup
			))
		}
		return result
	}

	override fun getTrackSortScope() = TracksSortScope.ORGANIZED_BY_NAME
}