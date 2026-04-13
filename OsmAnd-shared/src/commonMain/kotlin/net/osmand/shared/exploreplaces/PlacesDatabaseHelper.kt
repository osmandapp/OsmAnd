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
		private const val DATABASE_VERSION = 3

		private const val DATA_EXPIRATION_TIME = 30L * 24 * 60 * 60 * 1000 // 1 month
		private const val EMPTY_DATA_EXPIRATION_TIME = 7L * 24 * 60 * 60 * 1000 // 7 days

		private const val TABLE_PLACES = "places"
		private const val COLUMN_ZOOM = "zoom"
		private const val COLUMN_TILE_X = "tileX"
		private const val COLUMN_TILE_Y = "tileY"
		private const val COLUMN_LANG = "lang"
		private const val COLUMN_DATA_CHUNK = "dataChunk"
		private const val COLUMN_DATA = "data"
		private const val COLUMN_TIMESTAMP = "timestamp"
		private const val ALL_LANGUAGES_MARKER_LANG = "all_langs"
		private const val ALL_LANGUAGES_EMPTY_MARKER_DATA = "[]"
		private const val ALL_LANGUAGES_NON_EMPTY_MARKER_DATA = "[ ]"
		private const val MAX_DATA_CHUNK_SIZE = 256 * 1024

		private const val CREATE_TABLE_PLACES =
			"CREATE TABLE IF NOT EXISTS $TABLE_PLACES (" +
					"$COLUMN_ZOOM INTEGER," +
					"$COLUMN_TILE_X INTEGER," +
					"$COLUMN_TILE_Y INTEGER," +
					"$COLUMN_LANG TEXT," +
					"$COLUMN_DATA_CHUNK INTEGER," +
					"$COLUMN_DATA TEXT," +
					"$COLUMN_TIMESTAMP INTEGER," +
					"PRIMARY KEY ($COLUMN_ZOOM, $COLUMN_TILE_X, $COLUMN_TILE_Y, $COLUMN_LANG, $COLUMN_DATA_CHUNK)" +
					")"

		private const val INSERT_OR_REPLACE =
			"INSERT OR REPLACE INTO $TABLE_PLACES " +
					"($COLUMN_ZOOM, $COLUMN_TILE_X, $COLUMN_TILE_Y, $COLUMN_LANG, $COLUMN_DATA_CHUNK, $COLUMN_DATA, $COLUMN_TIMESTAMP) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?)"

		private const val DELETE_LANGUAGE_DATA =
			"DELETE FROM $TABLE_PLACES WHERE $COLUMN_ZOOM=? AND $COLUMN_TILE_X=? AND $COLUMN_TILE_Y=? AND $COLUMN_LANG=?"
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
		if (oldVersion < 3) {
			db.execSQL("DROP TABLE IF EXISTS $TABLE_PLACES")
			onCreate(db)
		}
	}

	fun insertPlaces(
		zoom: Int,
		tileX: Int,
		tileY: Int,
		placesByLang: Map<String, List<OsmandApiFeatureData>>,
		allLanguagesEmptyResult: Boolean?
	): Boolean {
		var db: SQLiteConnection? = null
		try {
			db = openConnection(readOnly = false) ?: return false
			db.beginTransaction()
			try {
				for ((lang, places) in placesByLang) {
					val dataJson = try {
						json.encodeToString(serializer, places)
					} catch (e: Throwable) {
						LOG.error("Failed to serialize places for insert", e)
<<<<<<< HEAD
						continue
					}
					db.execSQL(
						INSERT_OR_REPLACE,
						arrayOf(zoom, tileX, tileY, lang, dataJson, currentTimeMillis())
					)
=======
						return false
					}
					insertPlaceData(db, zoom, tileX, tileY, lang, dataJson)
				}
				if (allLanguagesEmptyResult != null) {
					insertAllLanguagesRow(db, zoom, tileX, tileY, allLanguagesEmptyResult)
>>>>>>> ef81d3e717 (Fix online top places pipeline (#24829))
				}
				db.setTransactionSuccessful()
			} finally {
				db.endTransaction()
			}
			return true
		} catch (e: Throwable) {
			LOG.error("Failed insert places", e)
			return false
		} finally {
			runCatching { db?.close() }
		}
	}

	private fun insertAllLanguagesRow(
		db: SQLiteConnection,
		zoom: Int,
		tileX: Int,
		tileY: Int,
		emptyResult: Boolean
	) {
		insertPlaceData(
			db,
			zoom,
			tileX,
			tileY,
			ALL_LANGUAGES_MARKER_LANG,
			if (emptyResult) ALL_LANGUAGES_EMPTY_MARKER_DATA else ALL_LANGUAGES_NON_EMPTY_MARKER_DATA
		)
	}

	private fun insertPlaceData(
		db: SQLiteConnection,
		zoom: Int,
		tileX: Int,
		tileY: Int,
		lang: String,
		dataJson: String
	) {
		db.execSQL(DELETE_LANGUAGE_DATA, arrayOf(zoom, tileX, tileY, lang))
		val timestamp = currentTimeMillis()
		var chunk = 0
		var start = 0
		do {
			val end = getChunkEnd(dataJson, start)
			db.execSQL(
				INSERT_OR_REPLACE,
				arrayOf(zoom, tileX, tileY, lang, chunk, dataJson.substring(start, end), timestamp)
			)
			chunk++
			start = end
		} while (start < dataJson.length)
	}

	private fun getChunkEnd(dataJson: String, start: Int): Int {
		var end = minOf(start + MAX_DATA_CHUNK_SIZE, dataJson.length)
		if (end < dataJson.length && isHighSurrogate(dataJson[end - 1])) {
			end--
		}
		return end
	}

	private fun isHighSurrogate(ch: Char): Boolean {
		return ch.code in 0xD800..0xDBFF
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
			db = openConnection(readOnly = true) ?: return emptyList()
			val (selection, args) = getSelectionWithArgs(zoom, tileX, tileY, languages)
			val sql = "SELECT $COLUMN_LANG, $COLUMN_DATA_CHUNK, $COLUMN_DATA FROM $TABLE_PLACES WHERE $selection " +
					"ORDER BY $COLUMN_LANG, $COLUMN_DATA_CHUNK"

			cursor = db.rawQuery(sql, args)
			val chunksByLang = LinkedHashMap<String, StringBuilder>()
			if (cursor != null && cursor.moveToFirst()) {
				val langIndex = cursor.getColumnIndex(COLUMN_LANG)
				val dataIndex = cursor.getColumnIndex(COLUMN_DATA)
				do {
					val lang = cursor.getString(langIndex)
					if (lang == ALL_LANGUAGES_MARKER_LANG) {
						continue
					}
					val dataChunk = cursor.getString(dataIndex)
					if (dataChunk.isNotEmpty()) {
						chunksByLang.getOrPut(lang) { StringBuilder() }.append(dataChunk)
					}
				} while (cursor.moveToNext())
			}
			for ((_, dataBuilder) in chunksByLang) {
				val jsonStr = dataBuilder.toString()
				if (jsonStr.isNotEmpty()) {
					try {
						places.addAll(json.decodeFromString(serializer, jsonStr))
					} catch (e: Throwable) {
						LOG.error("Failed to parse places JSON", e)
						return emptyList()
					}
				}
			}
		} catch (e: Throwable) {
			LOG.error("Failed get places", e)
			return emptyList()
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
			val langsToCheck = if (filterByLang) languages else listOf(ALL_LANGUAGES_MARKER_LANG)
			val (selection, args) = getSelectionWithArgs(zoom, tileX, tileY, langsToCheck)
			val sql = "SELECT $COLUMN_LANG, min($COLUMN_TIMESTAMP) as $COLUMN_TIMESTAMP, " +
					"sum(length($COLUMN_DATA)) as data_length FROM $TABLE_PLACES WHERE $selection GROUP BY $COLUMN_LANG"

			cursor = db.rawQuery(sql, args)
			if (cursor != null && cursor.moveToFirst()) {
				val foundLangs = HashSet<String>()
				val currentTime = currentTimeMillis()
				val timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP)
				val langIndex = cursor.getColumnIndex(COLUMN_LANG)
				val dataLengthIndex = cursor.getColumnIndex("data_length")
				do {
					val lang = cursor.getString(langIndex)
					val ts = cursor.getLong(timestampIndex)
					val dataLen = if (cursor.isNull(dataLengthIndex)) 0L else cursor.getLong(dataLengthIndex)
					val emptyData = dataLen <= 2L // Check for empty JSON array "[]"

					val expirationTime = if (emptyData) EMPTY_DATA_EXPIRATION_TIME else DATA_EXPIRATION_TIME
					if ((currentTime - ts) > expirationTime) {
						return true
					}
					foundLangs.add(lang)
				} while (cursor.moveToNext())

<<<<<<< HEAD
				return filterByLang && foundLangs.size < languages.size
=======
				if (filterByLang) {
					return foundLangs.size < languages.size
				}
				return false
>>>>>>> ef81d3e717 (Fix online top places pipeline (#24829))
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
