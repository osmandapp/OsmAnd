package net.osmand.shared.gpx.organization.strategy

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParameter
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByCategory
import net.osmand.shared.gpx.organization.enums.OrganizeByType

object OrganizeByDateStrategy : OrganizeByStrategy {

	override fun apply(
		originalGroup: TracksGroup,
		param: OrganizeByParameter,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		val type = param.type
		if (type.category != OrganizeByCategory.DATE_TIME) return null

		val groupedTracks = HashMap<Long, MutableList<TrackItem>>()
		originalGroup.getTrackItems().let { trackItems ->
			for (trackItem in trackItems) {
				val date =
					trackItem.dataItem?.getParameter<Long>(GpxParameter.FILE_CREATION_TIME) ?: 0
				val time = Instant.fromEpochMilliseconds(date)
					.toLocalDateTime(TimeZone.currentSystemDefault())
				val key = if (type == OrganizeByType.YEAR_OF_CREATION) {
					getStartOfYearMillis(time.year)
				} else {
					getStartOfMonthMillis(time.year, time.month)
				}
				groupedTracks.getOrPut(key) { mutableListOf() }.add(trackItem)
			}
		}

		val result = mutableListOf<OrganizedTracksGroup>()
		for (entry in groupedTracks.entries) {
			val value = entry.key
			val trackItems = entry.value
			val id = createId(originalGroup, type, value.toString())
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

	private fun getStartOfMonthMillis(year: Int, month: Month): Long {
		val date = LocalDate(year, month, 1)
		val instant = date.atStartOfDayIn(TimeZone.UTC)
		return instant.toEpochMilliseconds()
	}

	private fun getStartOfYearMillis(year: Int): Long {
		val date = LocalDate(year, Month.JANUARY, 1)
		val instant = date.atStartOfDayIn(TimeZone.UTC)
		return instant.toEpochMilliseconds()
	}

	private fun createId(
		originalGroup: TracksGroup,
		type: OrganizeByType,
		value: String
	) = "${getBaseId(originalGroup, type)}${value.lowercase()}"

	override fun getTrackSortScope() = TracksSortScope.ORGANIZED_BY_VALUE
}