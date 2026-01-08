package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.organization.OrganizeByParameter
import net.osmand.shared.gpx.organization.OrganizeByRangeParameter
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import kotlin.math.floor

object OrganizeByRangeStrategy : OrganizeByStrategy {

	override fun apply(
		originalGroup: TracksGroup,
		param: OrganizeByParameter,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup> {
		val type = param.type
		val step = (param as OrganizeByRangeParameter).stepSize
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
			result.add(
				OrganizedTracksGroup(
					id,
					type,
					limits,
					trackItems,
					originalGroup,
					resourcesMapper))
		}
		return result
	}

	private fun createId(limits: Limits, originalGroup: TracksGroup, type: OrganizeByType): String {
		return getBaseId(originalGroup, type) + "from_${limits.min}_to_${limits.max}"
	}

	private fun getRangeStartIndicator(
		trackItem: TrackItem,
		type: OrganizeByType,
		step: Double): Int? {
		val property = type.filterType.property ?: return null

		val value: Comparable<Any> = trackItem.dataItem?.getParameter(property) ?: return null
		val valueInt = getInt(property.getComparableValue<Double>(value))
		return floor((valueInt / step)).toInt()
	}

	private fun getInt(value: Any?): Int {
		return when (value) {
			is Number -> value.toInt()
			else -> value.toString().toDouble().toInt()
		}
	}
}