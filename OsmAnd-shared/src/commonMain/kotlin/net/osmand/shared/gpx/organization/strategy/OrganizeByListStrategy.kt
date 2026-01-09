package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

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
			val value = entry.key
			val trackItems = entry.value
			val id = createId(originalGroup, type, value)
			result.add(
				OrganizedTracksGroup(
					id,
					type,
					value,
					trackItems,
					originalGroup,
					resourcesMapper))
		}
		return result
	}

	private fun createId(
		originalGroup: TracksGroup,
		type: OrganizeByType,
		value: String
	) = "${getBaseId(originalGroup, type)}${value.lowercase()}"

	override fun getTrackSortScope() = TracksSortScope.ORGANIZED_BY_NAME
}