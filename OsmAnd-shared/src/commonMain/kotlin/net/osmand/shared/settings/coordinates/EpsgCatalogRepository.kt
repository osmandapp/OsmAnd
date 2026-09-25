package net.osmand.shared.settings.coordinates

import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.api.SQLiteAPI.SQLiteCursor
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KLock
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import net.osmand.shared.util.synchronized
import kotlin.jvm.JvmOverloads

class EpsgCatalogRepository internal constructor(
	// Tests supply their own proj.db connection; the app opens the one in its directory.
	private val connectionFactory: () -> SQLiteConnection?
) {

	constructor() : this(::openProjDb)

	private val epsgCache = LruCache<Int, CoordinateFormat>(MAX_CACHE_SIZE)
	private val gridDefinitionCache = LruCache<Int, EpsgGridDefinition>(MAX_CACHE_SIZE)
	private val gridDefinitionLock = KLock()
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
		} catch (e: Exception) {
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

	fun getGridDefinition(code: Int): EpsgGridDefinition? = synchronized(gridDefinitionLock) {
		readGridDefinition(code)
	}

	private fun readGridDefinition(code: Int): EpsgGridDefinition? {
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
					SUPPORTED_AREA_FILTER +
					METRIC_AXES_FILTER,
				arrayOf(code.toString())
			)
			if (cursor == null || !cursor.moveToFirst()) {
				unsupportedGridCodes.add(code)
				return null
			}
			val methodCode = cursor.getString(0).toIntOrNull()
			val baseCrsAuthName = if (cursor.isNull(1)) "" else cursor.getString(1)
			val baseCrsCode = if (cursor.isNull(2)) "" else cursor.getString(2)
			if (methodCode == null || baseCrsAuthName.isEmpty() || baseCrsCode.isEmpty()) {
				unsupportedGridCodes.add(code)
				return null
			}
			cursor.close()
			cursor = null

			val usesWgs84 = baseCrsAuthName == EPSG_AUTH_NAME && baseCrsCode == WGS84_CRS_CODE
			val transformationCodes = if (usesWgs84) {
				emptyList()
			} else {
				queryTransformationCodes(db, code, baseCrsAuthName, baseCrsCode)
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
		} catch (e: Exception) {
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
		} catch (e: Exception) {
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
		val likeQuery = "%${normalizedQuery.lowercase()}%"
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
		} catch (e: Exception) {
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
		val likeQuery = "%${normalizedQuery.lowercase()}%"
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
		} catch (e: Exception) {
			LOG.error("Failed to read supported Coordinate Grid formats", e)
			return emptyList()
		} finally {
			cursor?.close()
			db.close()
		}
	}

	/**
	 * Helmert candidates for the base CRS, best first. A transformation is usable only where it
	 * is valid, so candidates whose area of use misses the CRS's are dropped and the rest are
	 * ranked by how much of the CRS's area they cover, then by the old accuracy order.
	 */
	private fun queryTransformationCodes(
		db: SQLiteConnection,
		crsCode: Int,
		baseCrsAuthName: String,
		baseCrsCode: String
	): List<Int> {
		var cursor: SQLiteCursor? = null
		try {
			cursor = db.rawQuery(
				CRS_AREA_OF_USE +
					"candidates AS (" +
					"SELECT h.code code, h.accuracy accuracy, a.area crs_area, " +
					"CASE WHEN lower(IFNULL(h.description, '')) LIKE '%replaced by%' THEN 1 ELSE 0 END superseded, " +
					"$TRANSFORMATION_INTERSECTS intersects, $TRANSFORMATION_OVERLAP overlap " +
					"FROM helmert_transformation h " +
					"CROSS JOIN crs_area a " +
					"LEFT JOIN usage hu ON hu.object_table_name = 'helmert_transformation' " +
					"AND hu.object_auth_name = h.auth_name AND hu.object_code = h.code " +
					"LEFT JOIN extent he ON he.auth_name = hu.extent_auth_name AND he.code = hu.extent_code " +
					"AND IFNULL(he.deprecated, 0) = 0 " +
					"WHERE h.auth_name = 'EPSG' AND IFNULL(h.deprecated, 0) = 0 " +
					"AND h.source_crs_auth_name = ? AND h.source_crs_code = ? " +
					"AND h.target_crs_auth_name = 'EPSG' AND h.target_crs_code = '4326' " +
					"AND h.method_auth_name = 'EPSG' AND h.method_code IN ($SUPPORTED_HELMERT_METHODS) " +
					"GROUP BY h.code, h.accuracy, h.description, a.area) " +
					"SELECT code FROM candidates WHERE intersects = 1 " +
					"ORDER BY IFNULL(ROUND(overlap / NULLIF(crs_area, 0.0), 1), 0.0) DESC, " +
					"superseded, CASE WHEN accuracy IS NULL THEN 1 ELSE 0 END, accuracy, CAST(code AS INTEGER) " +
					"LIMIT $MAX_TRANSFORMATION_CANDIDATES",
				arrayOf(crsCode.toString(), baseCrsAuthName, baseCrsCode)
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

	private fun openConnection(): SQLiteConnection? = connectionFactory()

	private fun readFormat(cursor: SQLiteCursor): CoordinateFormat {
		val code = cursor.getString(0).toIntOrNull() ?: 0
		val name = if (cursor.isNull(1)) null else cursor.getString(1)
		val area = if (cursor.isNull(2)) null else cursor.getString(2)
		val deprecated = !cursor.isNull(3) && cursor.getInt(3) != 0
		return CoordinateFormat.epsg(code, name, area, deprecated)
	}

	private fun normalizeSearchQuery(query: String?): String {
		val trimmed = query?.trim() ?: return ""
		return if (trimmed.lowercase().startsWith(CoordinateFormatIds.EPSG_PREFIX)) {
			trimmed.substring(CoordinateFormatIds.EPSG_PREFIX.length).trim()
		} else {
			trimmed
		}
	}

	private fun String.isNumeric(): Boolean = isNotEmpty() && all { it.isDigit() }

	/**
	 * Least-recently-used cache, the common-code stand-in for an access-ordered
	 * java.util.LinkedHashMap with removeEldestEntry().
	 */
	private class LruCache<K, V>(private val maxSize: Int) {

		private val lock = KLock()
		private val entries = LinkedHashMap<K, V>()

		operator fun get(key: K): V? = synchronized(lock) {
			val value = entries.remove(key)
			if (value != null) {
				entries[key] = value
			}
			value
		}

		operator fun set(key: K, value: V) = synchronized(lock) {
			entries.remove(key)
			entries[key] = value
			if (entries.size > maxSize) {
				entries.remove(entries.keys.first())
			}
		}
	}

	private companion object {
		private val LOG = LoggerFactory.getLogger("EpsgCatalogRepository")
		private const val PROJ_DB_NAME = "proj.db"

		private fun openProjDb(): SQLiteConnection? {
			return try {
				// Resolved on every open: the app directory can be moved by the user at runtime.
				val projDb = KFile(PlatformUtil.getOsmAndContext().getAppDir(), PROJ_DB_NAME)
				if (!projDb.exists()) {
					LOG.warn("EPSG catalog is unavailable: ${projDb.absolutePath()}")
					return null
				}
				PlatformUtil.getSQLiteAPI().openByAbsolutePath(projDb.absolutePath(), true)
			} catch (e: Exception) {
				LOG.error("Failed to open EPSG catalog", e)
				null
			}
		}

		private const val DEFAULT_LIST_LIMIT = 1000
		private const val DEFAULT_SEARCH_LIMIT = 50
		private const val MAX_CACHE_SIZE = 64
		private const val MAX_TRANSFORMATION_CANDIDATES = 16
		private const val EPSG_AUTH_NAME = "EPSG"
		private const val WGS84_CRS_CODE = "4326"
		private const val METRE_UOM_CODE = 9001
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

		// CoordinateTransformer.getConstants() takes false easting/northing as metres, so a CRS
		// whose axes are in feet (or any other unit) would get a wrong grid.
		private const val METRIC_AXES_FILTER =
			"AND EXISTS (SELECT 1 FROM axis metric_axis " +
				"WHERE metric_axis.coordinate_system_auth_name = crs.coordinate_system_auth_name " +
				"AND metric_axis.coordinate_system_code = crs.coordinate_system_code " +
				"AND metric_axis.uom_auth_name = 'EPSG' AND CAST(metric_axis.uom_code AS INTEGER) = $METRE_UOM_CODE) " +
				"AND NOT EXISTS (SELECT 1 FROM axis other_axis " +
				"WHERE other_axis.coordinate_system_auth_name = crs.coordinate_system_auth_name " +
				"AND other_axis.coordinate_system_code = crs.coordinate_system_code " +
				"AND (IFNULL(other_axis.uom_auth_name, '') <> 'EPSG' " +
				"OR IFNULL(CAST(other_axis.uom_code AS INTEGER), 0) <> $METRE_UOM_CODE)) "

		// A CRS is listed only if at least one Helmert transformation is valid somewhere in its area
		// of use; the transformation's extent (grid_extent) must intersect the CRS's (crs_extent).
		private const val GRID_SUPPORTED_FILTER =
			"WHERE crs.auth_name = 'EPSG' AND IFNULL(crs.deprecated, 0) = 0 " +
				"AND c.method_auth_name = 'EPSG' AND c.method_code IN ($SUPPORTED_PROJECTION_METHODS) " +
				SUPPORTED_AREA_FILTER +
				METRIC_AXES_FILTER +
				"AND ((crs.geodetic_crs_auth_name = 'EPSG' AND crs.geodetic_crs_code = '4326') " +
				"OR EXISTS (SELECT 1 FROM helmert_transformation h " +
				"JOIN usage grid_usage ON grid_usage.object_table_name = 'helmert_transformation' " +
				"AND grid_usage.object_auth_name = h.auth_name AND grid_usage.object_code = h.code " +
				"JOIN extent grid_extent ON grid_extent.auth_name = grid_usage.extent_auth_name " +
				"AND grid_extent.code = grid_usage.extent_code AND IFNULL(grid_extent.deprecated, 0) = 0 " +
				"WHERE h.auth_name = 'EPSG' AND IFNULL(h.deprecated, 0) = 0 " +
				"AND h.source_crs_auth_name = crs.geodetic_crs_auth_name " +
				"AND h.source_crs_code = crs.geodetic_crs_code " +
				"AND h.target_crs_auth_name = 'EPSG' AND h.target_crs_code = '4326' " +
				"AND h.method_auth_name = 'EPSG' AND h.method_code IN ($SUPPORTED_HELMERT_METHODS) " +
				"AND EXISTS (SELECT 1 FROM usage crs_usage " +
				"JOIN extent crs_extent ON crs_extent.auth_name = crs_usage.extent_auth_name " +
				"AND crs_extent.code = crs_usage.extent_code AND IFNULL(crs_extent.deprecated, 0) = 0 " +
				"WHERE crs_usage.object_table_name = 'projected_crs' " +
				"AND crs_usage.object_auth_name = crs.auth_name AND crs_usage.object_code = crs.code " +
				"AND grid_extent.south_lat <= crs_extent.north_lat " +
				"AND grid_extent.north_lat >= crs_extent.south_lat " +
				"AND (CASE WHEN grid_extent.west_lon <= grid_extent.east_lon " +
				"THEN grid_extent.west_lon <= crs_extent.east_lon AND grid_extent.east_lon >= crs_extent.west_lon " +
				"ELSE grid_extent.west_lon <= crs_extent.east_lon " +
				"OR grid_extent.east_lon >= crs_extent.west_lon END)))) "

		// Bounding box of the CRS's area of use (the union of its extents); antimeridian-crossing
		// extents are already rejected by SUPPORTED_AREA_FILTER. Bound parameter: the CRS code.
		private const val CRS_AREA_OF_USE =
			"WITH crs_area AS (" +
				"SELECT MIN(e.south_lat) south, MAX(e.north_lat) north, " +
				"MIN(e.west_lon) west, MAX(e.east_lon) east, " +
				"MAX(MAX(e.north_lat) - MIN(e.south_lat), 0.0) * MAX(MAX(e.east_lon) - MIN(e.west_lon), 0.0) area " +
				"FROM usage u " +
				"JOIN extent e ON e.auth_name = u.extent_auth_name AND e.code = u.extent_code " +
				"WHERE u.object_table_name = 'projected_crs' AND u.object_auth_name = 'EPSG' AND u.object_code = ? " +
				"AND IFNULL(e.deprecated, 0) = 0 AND e.west_lon <= e.east_lon), "

		// 1 when the transformation extent (he) touches the CRS bounding box (a); a transformation
		// extent may cross the antimeridian, in which case it is two longitude ranges.
		private const val TRANSFORMATION_INTERSECTS =
			"MAX(CASE WHEN he.south_lat <= a.north AND he.north_lat >= a.south " +
				"AND (CASE WHEN he.west_lon <= he.east_lon " +
				"THEN he.west_lon <= a.east AND he.east_lon >= a.west " +
				"ELSE he.west_lon <= a.east OR he.east_lon >= a.west END) THEN 1 ELSE 0 END)"

		// Area (in square degrees) of the intersection of the transformation extent with the CRS box.
		private const val TRANSFORMATION_OVERLAP =
			"IFNULL(SUM(" +
				"MAX(0.0, MIN(he.north_lat, a.north) - MAX(he.south_lat, a.south)) * " +
				"(CASE WHEN he.west_lon <= he.east_lon " +
				"THEN MAX(0.0, MIN(he.east_lon, a.east) - MAX(he.west_lon, a.west)) " +
				"ELSE MAX(0.0, MIN(180.0, a.east) - MAX(he.west_lon, a.west)) " +
				"+ MAX(0.0, MIN(he.east_lon, a.east) - MAX(-180.0, a.west)) END)), 0.0)"
	}
}

data class EpsgGridDefinition(
	val epsgCode: Int,
	val projectionMethodCode: Int,
	val usesWgs84: Boolean,
	val transformationCodes: List<Int>
)
