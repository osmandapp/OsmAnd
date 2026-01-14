package net.osmand.shared.exploreplaces

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.api.SQLiteAPI.SQLiteCursor
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KLock
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import net.osmand.shared.util.synchronized
import net.osmand.shared.wiki.WikiCoreHelper.OsmandApiFeatureData

class PlacesDatabaseHelper {

	companion object {
		private val LOG = LoggerFactory.getLogger("PlacesDatabaseHelper")

		private val DB_LOCK = KLock()

		private const val DATABASE_NAME = "places.db"
		private const val DATABASE_VERSION = 2

		private const val DATA_EXPIRATION_TIME = 30L * 24 * 60 * 60 * 1000 // 1 month

		private const val TABLE_PLACES = "places"
		private const val COLUMN_ZOOM = "zoom"
		private const val COLUMN_TILE_X = "tileX"
		private const val COLUMN_TILE_Y = "tileY"
		private const val COLUMN_LANG = "lang"
		private const val COLUMN_DATA = "data"
		private const val COLUMN_TIMESTAMP = "timestamp"

		private const val CREATE_TABLE_PLACES =
			"CREATE TABLE IF NOT EXISTS $TABLE_PLACES (" +
					"$COLUMN_ZOOM INTEGER," +
					"$COLUMN_TILE_X INTEGER," +
					"$COLUMN_TILE_Y INTEGER," +
					"$COLUMN_LANG TEXT," +
					"$COLUMN_DATA TEXT," +
					"$COLUMN_TIMESTAMP INTEGER," +
					"PRIMARY KEY ($COLUMN_ZOOM, $COLUMN_TILE_X, $COLUMN_TILE_Y, $COLUMN_LANG)" +
					")"

		private const val INSERT_OR_REPLACE =
			"INSERT OR REPLACE INTO $TABLE_PLACES " +
					"($COLUMN_ZOOM, $COLUMN_TILE_X, $COLUMN_TILE_Y, $COLUMN_LANG, $COLUMN_DATA, $COLUMN_TIMESTAMP) " +
					"VALUES (?, ?, ?, ?, ?, ?)"
	}

	@OptIn(ExperimentalSerializationApi::class)
	private val json = Json {
		ignoreUnknownKeys = true
		isLenient = true
		explicitNulls = false
		coerceInputValues = true
		encodeDefaults = true
	}

	private val serializer = ListSerializer(OsmandApiFeatureData.serializer())

	private fun onCreate(db: SQLiteConnection) {
		db.execSQL(CREATE_TABLE_PLACES)
	}

	private fun onDowngrade(db: SQLiteConnection) {
		db.execSQL("DROP TABLE IF EXISTS $TABLE_PLACES")
		onCreate(db)
	}

	private fun onUpgrade(db: SQLiteConnection, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 2) {
			db.execSQL("DROP TABLE IF EXISTS $TABLE_PLACES")
			onCreate(db)
		}
	}

	fun insertPlaces(
		zoom: Int,
		tileX: Int,
		tileY: Int,
		placesByLang: Map<String, List<OsmandApiFeatureData>>
	) {
		var db: SQLiteConnection? = null
		try {
			db = openConnection(readOnly = false) ?: return
			db.beginTransaction()
			try {
				for ((lang, places) in placesByLang) {
					val dataJson = try {
						json.encodeToString(serializer, places)
					} catch (e: Throwable) {
						LOG.error("Failed to serialize places for insert", e)
						continue
					}
					db.execSQL(
						INSERT_OR_REPLACE,
						arrayOf(zoom, tileX, tileY, lang, dataJson, currentTimeMillis())
					)
				}
				db.setTransactionSuccessful()
			} finally {
				db.endTransaction()
			}
		} catch (e: Throwable) {
			LOG.error("Failed insert places", e)
		} finally {
			runCatching { db?.close() }
		}
	}

	fun getPlaces(
		zoom: Int,
		tileX: Int,
		tileY: Int,
		languages: List<String>
	): List<OsmandApiFeatureData> {
		var db: SQLiteConnection? = null
		var cursor: SQLiteCursor? = null
		val places = mutableListOf<OsmandApiFeatureData>()
		try {
			db = openConnection(readOnly = true) ?: return places
			val (selection, args) = getSelectionWithArgs(zoom, tileX, tileY, languages)
			val sql = "SELECT $COLUMN_DATA, $COLUMN_TIMESTAMP FROM $TABLE_PLACES WHERE $selection"

			cursor = db.rawQuery(sql, args)
			if (cursor != null && cursor.moveToFirst()) {
				val dataIndex = cursor.getColumnIndex(COLUMN_DATA)
				do {
					val jsonStr = cursor.getString(dataIndex)
					if (jsonStr.isNotEmpty()) {
						try {
							places.addAll(json.decodeFromString(serializer, jsonStr))
						} catch (e: Throwable) {
							LOG.error("Failed to parse places JSON", e)
						}
					}
				} while (cursor.moveToNext())
			}
		} catch (e: Throwable) {
			LOG.error("Failed get places", e)
		} finally {
			runCatching { cursor?.close() }
			runCatching { db?.close() }
		}
		return places
	}

	fun isDataExpired(
		zoom: Int,
		tileX: Int,
		tileY: Int,
		languages: List<String>
	): Boolean {
		var db: SQLiteConnection? = null
		var cursor: SQLiteCursor? = null
		val filterByLang = !KAlgorithms.isEmpty(languages)

		try {
			db = openConnection(readOnly = true) ?: return true
			val (selection, args) = getSelectionWithArgs(zoom, tileX, tileY, languages)
			val sql = "SELECT $COLUMN_LANG, $COLUMN_TIMESTAMP FROM $TABLE_PLACES WHERE $selection"

			cursor = db.rawQuery(sql, args)
			if (cursor != null && cursor.moveToFirst()) {
				val foundLangs = HashSet<String>()
				val currentTime = currentTimeMillis()
				val timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP)
				do {
					val ts = cursor.getLong(timestampIndex)
					if ((currentTime - ts) > DATA_EXPIRATION_TIME) return true
					val langIndex = cursor.getColumnIndex(COLUMN_LANG)
					val lang = cursor.getString(langIndex)
					foundLangs.add(lang)
				} while (cursor.moveToNext())

				return filterByLang && foundLangs.size < languages.size
			}
			return true // Data is expired if it doesn't exist
		} catch (e: Throwable) {
			LOG.error("Failed check places expired", e)
			return true
		} finally {
			runCatching { cursor?.close() }
			runCatching { db?.close() }
		}
	}

	private fun openConnection(readOnly: Boolean): SQLiteConnection? = synchronized(DB_LOCK) {
		val cacheDir = PlatformUtil.getOsmAndContext().getCacheDir()
		val path = KFile(cacheDir, DATABASE_NAME).absolutePath()
		val api = PlatformUtil.getSQLiteAPI()

		var conn = api.openByAbsolutePath(path, readOnly)
			?: if (readOnly) api.openByAbsolutePath(path, false) else null

		if (conn == null) {
			null
		} else if (conn.getVersion() == DATABASE_VERSION) {
			conn
		} else {
			if (conn.isReadOnly()) {
				runCatching { conn.close() }
				conn = api.openByAbsolutePath(path, false)
			}
			if (conn != null && updateDatabaseVersion(conn)) conn else null
		}
	}

	private fun updateDatabaseVersion(conn: SQLiteConnection): Boolean {
		return try {
			conn.beginTransaction()
			try {
				val v = conn.getVersion()
				when {
					v == 0 -> onCreate(conn)
					v > DATABASE_VERSION -> onDowngrade(conn)
					else -> onUpgrade(conn, v, DATABASE_VERSION)
				}
				conn.setVersion(DATABASE_VERSION)
				conn.setTransactionSuccessful()
			} finally {
				conn.endTransaction()
			}
			true
		} catch (t: Throwable) {
			LOG.error("Failed to init/upgrade/downgrade database", t)
			runCatching { conn.close() }
			false
		}
	}

	private fun getSelectionWithArgs(
		zoom: Int,
		tileX: Int,
		tileY: Int,
		languages: List<String>
	): Pair<String, Array<String>> {
		val args = ArrayList<String>(3 + languages.size)
		args.add(zoom.toString())
		args.add(tileX.toString())
		args.add(tileY.toString())

		val builder = StringBuilder("$COLUMN_ZOOM=? AND $COLUMN_TILE_X=? AND $COLUMN_TILE_Y=?")
		if (!KAlgorithms.isEmpty(languages)) {
			builder.append(" AND $COLUMN_LANG IN (")
			for (i in languages.indices) {
				builder.append("?")
				if (i < languages.size - 1) builder.append(",")
				args.add(languages[i])
			}
			builder.append(")")
		}
		return Pair(builder.toString(), args.toTypedArray())
	}
}