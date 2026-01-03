package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.enums.OrganizeByType

object OrganizeByActivityStrategy: OrganizeByStrategy<String> {

	override fun apply(
		originalGroup: SmartFolder,
		rules: OrganizeByRules,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		val type = rules.type
		if (type != OrganizeByType.ACTIVITY) return null

		val groupedTracks = HashMap<String, MutableList<TrackItem>>()
		originalGroup.getTrackItems().let { trackItems ->
			for (trackItem in trackItems) {
				val key = trackItem.dataItem?.getParameter<String>(GpxParameter.ACTIVITY_TYPE) ?: ""
				groupedTracks.getOrPut(key) { mutableListOf() }.add(trackItem)
			}
		}

		val result = mutableListOf<OrganizedTracksGroup>()
		for (entry in groupedTracks.entries) {
			val value = entry.key
			val trackItems = entry.value
			val id = createId(value, originalGroup, type)
			result.add(OrganizedTracksGroup(id, type, value, trackItems, originalGroup, resourcesMapper))
		}
		return result
	}

	override fun createRepresentedValueId(value: String): String {
		return value.lowercase()
	}
}