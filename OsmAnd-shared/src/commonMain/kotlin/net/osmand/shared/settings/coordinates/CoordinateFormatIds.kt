package net.osmand.shared.settings.coordinates

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object CoordinateFormatIds {

	const val BUILTIN_DDD = "builtin:ddd"
	const val BUILTIN_DDM = "builtin:ddm"
	const val BUILTIN_DMS = "builtin:dms"
	const val BUILTIN_UTM = "builtin:utm"
	const val BUILTIN_OLC = "builtin:olc"
	const val BUILTIN_MGRS = "builtin:mgrs"
	const val BUILTIN_SWISS_GRID = "builtin:swiss_grid"
	const val BUILTIN_SWISS_GRID_PLUS = "builtin:swiss_grid_plus"
	const val BUILTIN_MAIDENHEAD = "builtin:maidenhead"

	const val EPSG_PREFIX = "epsg:"

	@JvmField
	val DEFAULT_FORMAT_IDS: List<String> =
		listOf(BUILTIN_DDD, BUILTIN_DDM, BUILTIN_DMS, BUILTIN_UTM, BUILTIN_OLC)

	@JvmField
	val ALL_BUILT_IN_FORMAT_IDS: List<String> = listOf(
		BUILTIN_DDD,
		BUILTIN_DDM,
		BUILTIN_DMS,
		BUILTIN_UTM,
		BUILTIN_OLC,
		BUILTIN_MGRS,
		BUILTIN_SWISS_GRID,
		BUILTIN_SWISS_GRID_PLUS,
		BUILTIN_MAIDENHEAD
	)

	private val builtInIds = ALL_BUILT_IN_FORMAT_IDS.toSet()

	@JvmStatic
	fun epsg(code: Int): String = EPSG_PREFIX + code

	@JvmStatic
	fun normalize(id: String?): String? {
		normalizeBuiltInId(id)?.let { return it }
		return getEpsgCode(id)?.let { epsg(it) }
	}

	@JvmStatic
	fun getEpsgCode(id: String?): Int? {
		val trimmed = id?.trim() ?: return null
		if (!trimmed.lowercase().startsWith(EPSG_PREFIX)) {
			return null
		}
		val code = trimmed.substring(EPSG_PREFIX.length).trim()
		return code.toIntOrNull()?.takeIf { it > 0 }
	}

	private fun normalizeBuiltInId(id: String?): String? {
		val normalized = id?.trim()?.lowercase() ?: return null
		return normalized.takeIf { it in builtInIds }
	}
}
