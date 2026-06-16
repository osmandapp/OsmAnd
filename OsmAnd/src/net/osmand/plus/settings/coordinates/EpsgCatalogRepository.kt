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

		private const val BASE_SELECT =
			"SELECT crs.code, crs.name, group_concat(DISTINCT e.name), crs.deprecated " +
				"FROM projected_crs crs " +
				"LEFT JOIN usage u ON u.object_table_name = 'projected_crs' " +
				"AND u.object_auth_name = crs.auth_name AND u.object_code = crs.code " +
				"LEFT JOIN extent e ON e.auth_name = u.extent_auth_name AND e.code = u.extent_code " +
				"AND IFNULL(e.deprecated, 0) = 0 "
	}
}
