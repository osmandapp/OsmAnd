package net.osmand.plus.settings.coordinates

import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor
import org.apache.commons.logging.Log
import java.io.File
import java.util.LinkedHashMap
import java.util.Locale

class EpsgCatalogRepository(private val app: OsmandApplication) {

	private val epsgCache = object : LinkedHashMap<Int, CoordinateFormat>(MAX_CACHE_SIZE, 0.75f, true) {
		override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, CoordinateFormat>?): Boolean {
			return size > MAX_CACHE_SIZE
		}
	}
	private val gridDefinitionCache = object : LinkedHashMap<Int, EpsgGridDefinition>(MAX_CACHE_SIZE, 0.75f, true) {
		override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, EpsgGridDefinition>?): Boolean {
			return size > MAX_CACHE_SIZE
		}
	}
	private val unsupportedGridCodes = mutableSetOf<Int>()

	fun getByCode(code: Int): CoordinateFormat? {
		if (code <= 0) {
			return null
		}
		epsgCache[code]?.let { return it }
		val db = openConnection() ?: return null
		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				BASE_SELECT +
					"WHERE crs.auth_name = 'EPSG' AND crs.code = ? AND IFNULL(crs.deprecated, 0) = 0 " +
					"GROUP BY crs.code, crs.name, crs.deprecated",
				arrayOf(code.toString())
			)
			val format = if (cursor != null && cursor.moveToFirst()) readFormat(cursor) else null
			if (format != null) {
				epsgCache[code] = format
			}
			return format
		} catch (e: RuntimeException) {
			LOG.error("Failed to read EPSG CRS $code", e)
			return null
		} finally {
			cursor?.close()
			db.close()
		}
	}

	fun resolveFormat(id: String): CoordinateFormat {
		val code = CoordinateFormatIds.getEpsgCode(id) ?: return CoordinateFormat.unknown(id)
		return getByCode(code) ?: CoordinateFormat.unresolvedEpsg(code)
	}

	@Synchronized
	fun getGridDefinition(code: Int): EpsgGridDefinition? {
		if (code <= 0 || code in unsupportedGridCodes) {
			return null
		}
		gridDefinitionCache[code]?.let { return it }
		val db = openConnection() ?: return null
		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				"SELECT c.method_code, crs.geodetic_crs_auth_name, crs.geodetic_crs_code " +
					"FROM projected_crs crs " +
					"JOIN conversion c ON c.auth_name = crs.conversion_auth_name AND c.code = crs.conversion_code " +
					"WHERE crs.auth_name = 'EPSG' AND crs.code = ? AND IFNULL(crs.deprecated, 0) = 0 " +
					"AND c.method_auth_name = 'EPSG' AND c.method_code IN ($SUPPORTED_PROJECTION_METHODS) " +
					SUPPORTED_AREA_FILTER,
				arrayOf(code.toString())
			)
			if (cursor == null || !cursor.moveToFirst()) {
				unsupportedGridCodes.add(code)
				return null
			}
			val methodCode = cursor.getString(0).toIntOrNull()
			val baseCrsAuthName = cursor.getString(1)
			val baseCrsCode = cursor.getString(2)
			if (methodCode == null || baseCrsAuthName.isNullOrEmpty() || baseCrsCode.isNullOrEmpty()) {
				unsupportedGridCodes.add(code)
				return null
			}
			cursor.close()
			cursor = null

			val usesWgs84 = baseCrsAuthName == EPSG_AUTH_NAME && baseCrsCode == WGS84_CRS_CODE
			val transformationCodes = if (usesWgs84) {
				emptyList()
			} else {
				queryTransformationCodes(db, baseCrsAuthName, baseCrsCode)
			}
			if (!usesWgs84 && transformationCodes.isEmpty()) {
				unsupportedGridCodes.add(code)
				return null
			}
			return EpsgGridDefinition(
				epsgCode = code,
				projectionMethodCode = methodCode,
				usesWgs84 = usesWgs84,
				transformationCodes = transformationCodes
			).also { gridDefinitionCache[code] = it }
		} catch (e: RuntimeException) {
			LOG.error("Failed to read grid definition for EPSG:$code", e)
			return null
		} finally {
			cursor?.close()
			db.close()
		}
	}

	fun listGridFormats(limit: Int = DEFAULT_LIST_LIMIT): List<CoordinateFormat> {
		return queryGridFormats(null, limit)
	}

	fun searchGridFormats(query: String?, limit: Int = DEFAULT_SEARCH_LIMIT): List<CoordinateFormat> {
		val normalizedQuery = normalizeSearchQuery(query)
		return if (normalizedQuery.isEmpty()) {
			listGridFormats(limit)
		} else {
			queryGridFormats(normalizedQuery, limit)
		}
	}

	@JvmOverloads
	fun listAll(limit: Int = DEFAULT_LIST_LIMIT): List<CoordinateFormat> {
		val db = openConnection() ?: return emptyList()
		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				BASE_SELECT +
					"WHERE crs.auth_name = 'EPSG' AND IFNULL(crs.deprecated, 0) = 0 " +
					"GROUP BY crs.code, crs.name, crs.deprecated " +
					"ORDER BY crs.name " +
					"LIMIT ?",
				arrayOf(limit.coerceAtLeast(1).toString())
			)
			val result = mutableListOf<CoordinateFormat>()
			if (cursor != null && cursor.moveToFirst()) {
				do {
					result.add(readFormat(cursor))
				} while (cursor.moveToNext())
			}
			return result
		} catch (e: RuntimeException) {
			LOG.error("Failed to read EPSG CRS list", e)
			return emptyList()
		} finally {
			cursor?.close()
			db.close()
		}
	}

	@JvmOverloads
	fun search(query: String?, limit: Int = DEFAULT_SEARCH_LIMIT): List<CoordinateFormat> {
		val normalizedQuery = normalizeSearchQuery(query)
		if (normalizedQuery.isEmpty()) {
			return emptyList()
		}
		val db = openConnection() ?: return emptyList()
		val numeric = normalizedQuery.isNumeric()
		val exactCode = if (numeric) normalizedQuery else ""
		val codePrefix = if (numeric) "$normalizedQuery%" else ""
		val likeQuery = "%${normalizedQuery.lowercase(Locale.US)}%"
		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				BASE_SELECT +
					"WHERE crs.auth_name = 'EPSG' AND IFNULL(crs.deprecated, 0) = 0 AND (" +
					"crs.code = ? OR crs.code LIKE ? OR lower(crs.name) LIKE ? " +
					"OR lower(IFNULL(crs.description, '')) LIKE ? OR lower(IFNULL(e.name, '')) LIKE ? " +
					"OR lower(IFNULL(e.description, '')) LIKE ?) " +
					"GROUP BY crs.code, crs.name, crs.deprecated " +
					"ORDER BY CASE WHEN crs.code = ? THEN 0 WHEN crs.code LIKE ? THEN 1 ELSE 2 END, crs.name " +
					"LIMIT ?",
				arrayOf(
					exactCode,
					codePrefix,
					likeQuery,
					likeQuery,
					likeQuery,
					likeQuery,
					exactCode,
					codePrefix,
					limit.coerceAtLeast(1).toString()
				)
			)
			val result = mutableListOf<CoordinateFormat>()
			if (cursor != null && cursor.moveToFirst()) {
				do {
					result.add(readFormat(cursor))
				} while (cursor.moveToNext())
			}
			return result
		} catch (e: RuntimeException) {
			LOG.error("Failed to search EPSG CRS by query: $query", e)
			return emptyList()
		} finally {
			cursor?.close()
			db.close()
		}
	}

	private fun queryGridFormats(query: String?, limit: Int): List<CoordinateFormat> {
		val db = openConnection() ?: return emptyList()
		val normalizedQuery = query.orEmpty()
		val numeric = normalizedQuery.isNumeric()
		val exactCode = if (numeric) normalizedQuery else ""
		val codePrefix = if (numeric) "$normalizedQuery%" else ""
		val likeQuery = "%${normalizedQuery.lowercase(Locale.US)}%"
		val queryFilter = if (normalizedQuery.isEmpty()) {
			""
		} else {
			"AND (crs.code = ? OR crs.code LIKE ? OR lower(crs.name) LIKE ? " +
				"OR lower(IFNULL(crs.description, '')) LIKE ? OR lower(IFNULL(e.name, '')) LIKE ? " +
				"OR lower(IFNULL(e.description, '')) LIKE ?) "
		}
		val orderBy = if (normalizedQuery.isEmpty()) {
			"ORDER BY crs.name "
		} else {
			"ORDER BY CASE WHEN crs.code = ? THEN 0 WHEN crs.code LIKE ? THEN 1 ELSE 2 END, crs.name "
		}
		val args = mutableListOf<String>()
		if (normalizedQuery.isNotEmpty()) {
			args.addAll(listOf(exactCode, codePrefix, likeQuery, likeQuery, likeQuery, likeQuery, exactCode, codePrefix))
		}
		args.add(limit.coerceAtLeast(1).toString())

		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				GRID_BASE_SELECT + GRID_SUPPORTED_FILTER + queryFilter +
					"GROUP BY crs.code, crs.name, crs.deprecated " + orderBy + "LIMIT ?",
				args.toTypedArray()
			)
			val result = mutableListOf<CoordinateFormat>()
			if (cursor != null && cursor.moveToFirst()) {
				do {
					result.add(readFormat(cursor))
				} while (cursor.moveToNext())
			}
			return result
		} catch (e: RuntimeException) {
			LOG.error("Failed to read supported Coordinate Grid formats", e)
			return emptyList()
		} finally {
			cursor?.close()
			db.close()
		}
	}

	private fun queryTransformationCodes(
		db: SQLiteConnection,
		baseCrsAuthName: String,
		baseCrsCode: String
	): List<Int> {
		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				"SELECT h.code FROM helmert_transformation h " +
					"WHERE h.auth_name = 'EPSG' AND IFNULL(h.deprecated, 0) = 0 " +
					"AND h.source_crs_auth_name = ? AND h.source_crs_code = ? " +
					"AND h.target_crs_auth_name = 'EPSG' AND h.target_crs_code = '4326' " +
					"AND h.method_auth_name = 'EPSG' AND h.method_code IN ($SUPPORTED_HELMERT_METHODS) " +
					"ORDER BY CASE WHEN lower(IFNULL(h.description, '')) LIKE '%replaced by%' THEN 1 ELSE 0 END, " +
					"CASE WHEN h.accuracy IS NULL THEN 1 ELSE 0 END, h.accuracy, CAST(h.code AS INTEGER) " +
					"LIMIT $MAX_TRANSFORMATION_CANDIDATES",
				arrayOf(baseCrsAuthName, baseCrsCode)
			)
			val result = mutableListOf<Int>()
			if (cursor != null && cursor.moveToFirst()) {
				do {
					cursor.getString(0).toIntOrNull()?.let(result::add)
				} while (cursor.moveToNext())
			}
			return result
		} finally {
			cursor?.close()
		}
	}

	private fun openConnection(): SQLiteConnection? {
		val projDb: File = app.getAppPath(PROJ_DB_NAME)
		if (!projDb.exists()) {
			LOG.warn("EPSG catalog is unavailable: ${projDb.absolutePath}")
			return null
		}
		return try {
			app.getSQLiteAPI().openByAbsolutePath(projDb.absolutePath, true)
		} catch (e: RuntimeException) {
			LOG.error("Failed to open EPSG catalog: ${projDb.absolutePath}", e)
			null
		}
	}

	private fun readFormat(cursor: SQLiteCursor): CoordinateFormat {
		val code = cursor.getString(0).toIntOrNull() ?: 0
		val name = cursor.getString(1)
		val area = if (cursor.isNull(2)) null else cursor.getString(2)
		val deprecated = !cursor.isNull(3) && cursor.getInt(3) != 0
		return CoordinateFormat.epsg(code, name, area, deprecated)
	}

	private fun normalizeSearchQuery(query: String?): String {
		val trimmed = query?.trim() ?: return ""
		return if (trimmed.lowercase(Locale.US).startsWith(CoordinateFormatIds.EPSG_PREFIX)) {
			trimmed.substring(CoordinateFormatIds.EPSG_PREFIX.length).trim()
		} else {
			trimmed
		}
	}

	private fun String.isNumeric(): Boolean = isNotEmpty() && all { it.isDigit() }

	private companion object {
		private val LOG: Log = PlatformUtil.getLog(EpsgCatalogRepository::class.java)
		private const val PROJ_DB_NAME = "proj.db"
		private const val DEFAULT_LIST_LIMIT = 1000
		private const val DEFAULT_SEARCH_LIMIT = 50
		private const val MAX_CACHE_SIZE = 64
		private const val MAX_TRANSFORMATION_CANDIDATES = 16
		private const val EPSG_AUTH_NAME = "EPSG"
		private const val WGS84_CRS_CODE = "4326"
		// Projection methods implemented by GridConfiguration: TM, OSTEREO and HOMV2.
		private const val SUPPORTED_PROJECTION_METHODS = "'9807', '9809', '9815'"
		// Direct geog2D Helmert methods parsed by CoordinateTransformer.getEllipsoidParameters().
		private const val SUPPORTED_HELMERT_METHODS = "'9603', '9606', '9607'"

		private const val BASE_SELECT =
			"SELECT crs.code, crs.name, group_concat(DISTINCT e.name), crs.deprecated " +
				"FROM projected_crs crs " +
				"LEFT JOIN usage u ON u.object_table_name = 'projected_crs' " +
				"AND u.object_auth_name = crs.auth_name AND u.object_code = crs.code " +
				"LEFT JOIN extent e ON e.auth_name = u.extent_auth_name AND e.code = u.extent_code " +
				"AND IFNULL(e.deprecated, 0) = 0 "

		private const val GRID_BASE_SELECT =
			"SELECT crs.code, crs.name, group_concat(DISTINCT e.name), crs.deprecated " +
				"FROM projected_crs crs " +
				"JOIN conversion c ON c.auth_name = crs.conversion_auth_name AND c.code = crs.conversion_code " +
				"LEFT JOIN usage u ON u.object_table_name = 'projected_crs' " +
				"AND u.object_auth_name = crs.auth_name AND u.object_code = crs.code " +
				"LEFT JOIN extent e ON e.auth_name = u.extent_auth_name AND e.code = u.extent_code " +
				"AND IFNULL(e.deprecated, 0) = 0 "

		// The renderer currently treats longitude bounds as a simple west-to-east range.
		private const val SUPPORTED_AREA_FILTER =
			"AND NOT EXISTS (SELECT 1 FROM usage area_usage " +
				"JOIN extent area_extent ON area_extent.auth_name = area_usage.extent_auth_name " +
				"AND area_extent.code = area_usage.extent_code " +
				"WHERE area_usage.object_table_name = 'projected_crs' " +
				"AND area_usage.object_auth_name = crs.auth_name AND area_usage.object_code = crs.code " +
				"AND IFNULL(area_extent.deprecated, 0) = 0 " +
				"AND area_extent.west_lon > area_extent.east_lon) "

		private const val GRID_SUPPORTED_FILTER =
			"WHERE crs.auth_name = 'EPSG' AND IFNULL(crs.deprecated, 0) = 0 " +
				"AND c.method_auth_name = 'EPSG' AND c.method_code IN ($SUPPORTED_PROJECTION_METHODS) " +
				SUPPORTED_AREA_FILTER +
				"AND ((crs.geodetic_crs_auth_name = 'EPSG' AND crs.geodetic_crs_code = '4326') " +
				"OR EXISTS (SELECT 1 FROM helmert_transformation h " +
				"WHERE h.auth_name = 'EPSG' AND IFNULL(h.deprecated, 0) = 0 " +
				"AND h.source_crs_auth_name = crs.geodetic_crs_auth_name " +
				"AND h.source_crs_code = crs.geodetic_crs_code " +
				"AND h.target_crs_auth_name = 'EPSG' AND h.target_crs_code = '4326' " +
				"AND h.method_auth_name = 'EPSG' AND h.method_code IN ($SUPPORTED_HELMERT_METHODS))) "
	}
}

data class EpsgGridDefinition(
	val epsgCode: Int,
	val projectionMethodCode: Int,
	val usesWgs84: Boolean,
	val transformationCodes: List<Int>
)
