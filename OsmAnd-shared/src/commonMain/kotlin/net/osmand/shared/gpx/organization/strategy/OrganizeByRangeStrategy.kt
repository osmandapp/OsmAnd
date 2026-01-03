package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.OrganizeByResourcesResolver
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import kotlin.math.floor

class OrganizeByRangeStrategy<T : Comparable<T>>: OrganizeByStrategy<Limits> {

	override fun apply(
		originalGroup: SmartFolder,
		rules: OrganizeByRules,
		resourcesResolver: OrganizeByResourcesResolver
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

			val name = resourcesResolver.resolveName(limits, type)
			val iconName = resourcesResolver.resolveIconName(limits, type)
			val trackItems = entry.value
			val id = createId(limits, originalGroup, type)
			result.add(OrganizedTracksGroup(id, name, iconName, type, limits, trackItems, originalGroup))
		}
		return result
	}

	override fun createRepresentedValueId(value: Limits) = "from_${value.min}_to_${value.max}"


	private fun getRangeStartIndicator(trackItem: TrackItem, type: OrganizeByType, step: Int): Int? {
		val property = type.filterType.property ?: return null

		val value: Comparable<Any> = trackItem.dataItem?.getParameter(property) ?: return null
		val valueInt = getInt(property.getComparableValue<T>(value))
		return floor((valueInt/step).toDouble()).toInt()
	}

	private fun getInt(value: Any?): Int {
		return when (value) {
			is Number -> value.toInt()
			else -> value.toString().toDouble().toInt()
		}
	}
}