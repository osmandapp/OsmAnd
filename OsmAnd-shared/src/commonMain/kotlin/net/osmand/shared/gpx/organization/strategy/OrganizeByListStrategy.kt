package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper

object OrganizeByListStrategy : OrganizeByStrategy {

	override fun apply(
		originalGroup: TracksGroup,
		params: OrganizeByParams,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup> {
		val type = params.type

		val groupedTracks = HashMap<String, MutableList<TrackItem>>()
		originalGroup.getTrackItems().let { trackItems ->
			for (trackItem in trackItems) {
				val key = trackItem.dataItem?.getParameter<String>(type.getGpxParameter()) ?: ""
				groupedTracks.getOrPut(key) { mutableListOf() }.add(trackItem)
			}
		}

		val result = mutableListOf<OrganizedTracksGroup>()
		for (entry in groupedTracks.entries) {
			val value = entry.key.ifEmpty { "none" }
			val trackItems = entry.value
			val id = OrganizedTracksGroup.createId(originalGroup, type, value)
			result.add(OrganizedTracksGroup(
				id = id,
				name = resourcesMapper.getName(type, value),
				iconName = resourcesMapper.getIconName(type, value),
				type = type,
				trackItems = trackItems,
				parentGroup = originalGroup
			))
		}
		return result
	}

	override fun getTrackSortScope() = TracksSortScope.ORGANIZED_BY_NAME
}