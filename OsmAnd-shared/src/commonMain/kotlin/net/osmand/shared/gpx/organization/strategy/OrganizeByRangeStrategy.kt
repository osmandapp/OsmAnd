package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import kotlin.math.floor

object OrganizeByRangeStrategy: OrganizeByStrategy<Limits> {

	override fun apply(
		originalGroup: SmartFolder,
		rules: OrganizeByRules,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		val type = rules.type
		val step = rules.stepSize ?: return null // TODO: maybe throw an exception

		val groupedTracks = HashMap<Int, MutableList<TrackItem>>()
		originalGroup.getTrackItems().let { trackItems ->
			for (trackItem in trackItems) {
				val subRangeStart: Int? = getRangeStartIndicator(trackItem, type, step)
				if (subRangeStart != null) {
					groupedTracks.getOrPut(subRangeStart) { mutableListOf() }.add(trackItem)
				}
			}
		}

		val result = mutableListOf<OrganizedTracksGroup>()
		for (entry in groupedTracks.entries) {
			val startValue = entry.key * step
			val endValue = startValue + step
			val limits = Limits(startValue, endValue)

			val trackItems = entry.value
			val id = createId(limits, originalGroup, type)
			result.add(OrganizedTracksGroup(id, type, limits, trackItems, originalGroup, resourcesMapper))
		}
		return result
	}

	override fun createRepresentedValueId(value: Limits) = "from_${value.min}_to_${value.max}"


	private fun getRangeStartIndicator(trackItem: TrackItem, type: OrganizeByType, step: Double): Int? {
		val property = type.filterType.property ?: return null

		val value: Comparable<Any> = trackItem.dataItem?.getParameter(property) ?: return null
		val valueInt = getInt(property.getComparableValue<Double>(value))
		return floor((valueInt/step)).toInt()
	}

	private fun getInt(value: Any?): Int {
		return when (value) {
			is Number -> value.toInt()
			else -> value.toString().toDouble().toInt()
		}
	}
}