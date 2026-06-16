package net.osmand.plus.settings.coordinates

import net.osmand.data.PointDescription
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.IntPreference
import net.osmand.plus.settings.backend.preferences.ListStringPreference
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import java.util.Collections
import java.util.LinkedHashSet

class CoordinateFormatSettingsStorage(private val settings: OsmandSettings) {

	@Deprecated("Use preferredCoordinateFormatIdsPreference instead.")
	val legacyFormatPreference: OsmandPreference<Int> =
		IntPreference(settings, LEGACY_FORMAT_ID, PointDescription.FORMAT_DEGREES).apply { makeProfile() }

	val preferredCoordinateFormatIdsPreference: ListStringPreference =
		ListStringPreference(
			settings,
			PREFERRED_FORMAT_IDS_ID,
			CoordinateFormatIds.DEFAULT_FORMAT_IDS.joinToString(DELIMITER),
			DELIMITER
		).apply { makeProfile() }

	val recentlyAddedCoordinateFormatIdsPreference: ListStringPreference =
		ListStringPreference(settings, RECENT_FORMAT_IDS_ID, null, DELIMITER).apply { makeGlobal() }

	fun getPreferredIds(): List<String> {
		return getPreferredIds(settings.applicationMode)
	}

	fun getPreferredIds(mode: ApplicationMode): List<String> {
		return sanitizePreferredIds(preferredCoordinateFormatIdsPreference.getStringsListForProfile(mode))
	}

	fun setPreferredIds(mode: ApplicationMode, ids: List<String>): Boolean {
		return preferredCoordinateFormatIdsPreference.setModeValues(mode, sanitizePreferredIds(ids))
	}

	fun getPrimaryId(mode: ApplicationMode): String {
		return getPreferredIds(mode).first()
	}

	fun resetPreferredIds(mode: ApplicationMode): Boolean {
		return preferredCoordinateFormatIdsPreference.setModeValues(mode, CoordinateFormatIds.DEFAULT_FORMAT_IDS)
	}

	fun copyPreferredIds(fromMode: ApplicationMode, toMode: ApplicationMode): Boolean {
		return setPreferredIds(toMode, getPreferredIds(fromMode))
	}

	fun addPreferredId(mode: ApplicationMode, id: String): Boolean {
		val normalized = CoordinateFormatIds.normalize(id) ?: return false
		val ids = ArrayList(getPreferredIds(mode))
		if (normalized in ids) {
			return false
		}
		ids.add(normalized)
		return setPreferredIds(mode, ids)
	}

	fun getRecentIds(): List<String> {
		return sanitizeIds(
			recentlyAddedCoordinateFormatIdsPreference.stringsList,
			emptyList(),
			MAX_RECENT_FORMAT_IDS
		)
	}

	fun addRecentId(id: String): Boolean {
		val normalized = CoordinateFormatIds.normalize(id) ?: return false
		val ids = ArrayList(getRecentIds())
		ids.remove(normalized)
		ids.add(0, normalized)
		while (ids.size > MAX_RECENT_FORMAT_IDS) {
			ids.removeAt(ids.lastIndex)
		}
		return recentlyAddedCoordinateFormatIdsPreference.setModeValues(settings.applicationMode, ids)
	}

	fun isPreferredIdsSetForMode(mode: ApplicationMode): Boolean {
		return preferredCoordinateFormatIdsPreference.isSetForMode(mode)
	}

	private fun sanitizePreferredIds(ids: List<String>?): List<String> {
		return sanitizeIds(ids, CoordinateFormatIds.DEFAULT_FORMAT_IDS)
	}

	private fun sanitizeIds(
		ids: List<String>?,
		fallbackIds: List<String>,
		maxCount: Int = Int.MAX_VALUE
	): List<String> {
		val sanitized = LinkedHashSet<String>()
		ids?.forEach { id ->
			CoordinateFormatIds.normalize(id)?.let { normalized ->
				if (sanitized.size < maxCount) {
					sanitized.add(normalized)
				}
			}
		}
		if (sanitized.isEmpty() && fallbackIds.isNotEmpty()) {
			sanitized.addAll(fallbackIds)
		}
		return immutableCopy(sanitized)
	}

	private fun immutableCopy(ids: Collection<String>): List<String> {
		return Collections.unmodifiableList(ArrayList(ids))
	}

	companion object {
		const val LEGACY_FORMAT_ID = "coordinates_format"
		const val PREFERRED_FORMAT_IDS_ID = "preferred_coordinate_format_ids"
		const val RECENT_FORMAT_IDS_ID = "recently_added_coordinate_format_ids"
		const val MAX_RECENT_FORMAT_IDS = 5
		private const val DELIMITER = ","
	}
}
