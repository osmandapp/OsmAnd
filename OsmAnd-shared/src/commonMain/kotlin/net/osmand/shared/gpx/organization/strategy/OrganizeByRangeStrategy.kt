package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeByRangeParams
import kotlin.math.floor

object OrganizeByRangeStrategy : OrganizeByStrategy {

	override fun apply(
		originalGroup: TracksGroup,
		params: OrganizeByParams
	): List<OrganizedTracksGroup> {
		val type = params.type
		val step = (params as OrganizeByRangeParams).stepSize
		val property = type.getGpxParameter() ?: return emptyList()

		val groupedTracks = HashMap<Int, MutableList<TrackItem>>()
		originalGroup.getTrackItems().let { trackItems ->
			for (trackItem in trackItems) {
				val subRangeStart: Int? = getRangeStartIndicator(trackItem, property, step)
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
			val valueIdPart = "from_${startValue}_to_${endValue}"
			val trackItems = entry.value
			val id = OrganizedTracksGroup.createId(originalGroup, type, valueIdPart)
			result.add(OrganizedTracksGroup(
				id = id,
				name = getName(type, limits),
				iconName = getIconName(type, limits),
				type = type,
				comparisonValue = startValue,
				trackItems = trackItems,
				parentGroup = originalGroup
			))
		}
		return result
	}

	private fun getRangeStartIndicator(
		trackItem: TrackItem,
		property: GpxParameter,
		step: Double
	): Int? {
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

	override fun getTrackSortScope() = TracksSortScope.ORGANIZED_BY_VALUE
}