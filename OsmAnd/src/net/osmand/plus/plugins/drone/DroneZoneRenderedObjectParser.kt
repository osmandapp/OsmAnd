package net.osmand.plus.plugins.drone

import net.osmand.NativeLibrary.RenderedObject
import net.osmand.plus.plugins.drone.model.DroneZone
import net.osmand.plus.plugins.drone.model.DroneZoneDatasetInfo
import net.osmand.plus.plugins.drone.model.DroneZoneLink
import net.osmand.plus.plugins.drone.model.RawValue
import net.osmand.plus.plugins.drone.model.VerticalLimit
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DroneZoneRenderedObjectParser {
	private val knownRestrictions = setOf("REQ_AUTHORIZATION")
	private val knownReasons = setOf("AIR_TRAFFIC", "NATURE", "NOISE", "POPULATION", "PRIVACY", "SENSITIVE")
	private val knownReferences = setOf("AGL", "AMSL")
	private val knownUnits = setOf("m", "ft")

	fun parse(renderedObject: RenderedObject, language: String = Locale.getDefault().language): DroneZone {
		return parse(renderedObject.tags, language)
	}

	fun parse(tags: Map<String, String>, language: String): DroneZone {
		require(DroneZoneTags.isDroneZone(tags)) { "Rendered object is not a drone zone" }
		val normalizedLanguage = language.lowercase(Locale.ROOT).substringBefore('-')
		val name = tags["name:$normalizedLanguage"] ?: tags["name"] ?: tags[DroneZoneTags.SOURCE_ID].orEmpty()
		val unit = raw(tags[DroneZoneTags.UNIT], knownUnits)

		fun vertical(valueTag: String, referenceTag: String): VerticalLimit? {
			val value = tags[valueTag] ?: return null
			return VerticalLimit(value, unit, raw(tags[referenceTag], knownReferences))
		}

		val validTo = tags[DroneZoneTags.VALID_TO]
		return DroneZone(
			sourceId = tags[DroneZoneTags.SOURCE_ID],
			identifier = tags[DroneZoneTags.IDENTIFIER],
			name = name,
			restriction = raw(tags[DroneZoneTags.RESTRICTION], knownRestrictions),
			variant = tags[DroneZoneTags.VARIANT]?.let { raw(it) },
			typeCode = tags[DroneZoneTags.TYPE_CODE]?.let { raw(it) },
			reasons = tags[DroneZoneTags.REASONS].orEmpty().split(';').filter { it.isNotBlank() }.map { raw(it, knownReasons) },
			lowerLimit = vertical(DroneZoneTags.LOWER, DroneZoneTags.LOWER_REF),
			upperLimit = vertical(DroneZoneTags.UPPER, DroneZoneTags.UPPER_REF),
			legalTexts = localizedValue(tags, "uas_legal", normalizedLanguage)?.lines()?.filter { it.isNotBlank() }.orEmpty(),
			links = localizedLinks(tags, normalizedLanguage),
			dataset = DroneZoneDatasetInfo(
				id = tags[DroneZoneTags.DATASET],
				provider = tags[DroneZoneTags.PROVIDER],
				source = tags[DroneZoneTags.SOURCE],
				validFrom = tags[DroneZoneTags.VALID_FROM],
				validTo = validTo,
				expired = validTo?.let { parseDate(it)?.before(Date()) } ?: false,
			),
		)
	}

	private fun raw(value: String?, known: Set<String>? = null): RawValue {
		val raw = value.orEmpty()
		return RawValue(raw, if (known == null || raw in known) raw else null)
	}

	private fun localizedValue(tags: Map<String, String>, prefix: String, language: String): String? {
		return tags[DroneZoneTags.localized(prefix, language)]
			?: tags[DroneZoneTags.localized(prefix, "de")]
			?: tags[DroneZoneTags.localized(prefix, "en")]
	}

	private fun localizedLinks(tags: Map<String, String>, language: String): List<DroneZoneLink> {
		val urls = localizedValue(tags, "uas_link", language)?.lines().orEmpty()
		val texts = localizedValue(tags, "uas_link_text", language)?.lines().orEmpty()
		return urls.filter { it.isNotBlank() }.mapIndexed { index, url -> DroneZoneLink(texts.getOrNull(index) ?: url, url) }
	}

	private fun parseDate(value: String): Date? {
		for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")) {
			try {
				return SimpleDateFormat(pattern, Locale.US).parse(value)
			} catch (_: ParseException) {
				// Try the next supported ISO-8601 representation.
			}
		}
		return null
	}
}
