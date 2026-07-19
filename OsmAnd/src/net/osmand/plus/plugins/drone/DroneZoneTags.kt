package net.osmand.plus.plugins.drone

object DroneZoneTags {
	const val ZONE = "uas_zone"
	const val SCHEMA = "uas_schema"
	const val SOURCE = "uas_source"
	const val SOURCE_ID = "uas_source_id"
	const val IDENTIFIER = "uas_identifier"
	const val COUNTRY = "uas_country"
	const val RESTRICTION = "uas_restriction"
	const val VARIANT = "uas_variant"
	const val TYPE_CODE = "uas_type_code"
	const val REASONS = "uas_reasons"
	const val LOWER = "uas_lower"
	const val LOWER_REF = "uas_lower_ref"
	const val UPPER = "uas_upper"
	const val UPPER_REF = "uas_upper_ref"
	const val UNIT = "uas_uom"
	const val PROVIDER = "uas_provider"
	const val DATASET = "uas_dataset"
	const val VALID_FROM = "uas_valid_from"
	const val VALID_TO = "uas_valid_to"

	@JvmStatic
	fun isDroneZone(tags: Map<String, String>): Boolean = tags[ZONE] == "yes"

	fun localized(prefix: String, language: String): String = "$prefix:$language"
}
