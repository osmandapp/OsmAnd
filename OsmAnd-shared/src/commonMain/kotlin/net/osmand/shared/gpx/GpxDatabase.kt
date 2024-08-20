package net.osmand.shared.gpx

import net.osmand.shared.api.SQLiteAPI.*
import net.osmand.shared.extensions.format
import net.osmand.shared.gpx.GpxParameter.*
import net.osmand.shared.io.KFile
import net.osmand.shared.routing.ColoringType
import net.osmand.shared.util.DbUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil

class GpxDatabase {

	class StringIntPair(val string: String?, val integer: Int?);

	companion object {
		val log = LoggerFactory.getLogger("GpxDatabase")

		const val DB_VERSION = 27
		const val DB_NAME = "gpx_database"
		const val GPX_TABLE_NAME = "gpxTable"
		const val GPX_DIR_TABLE_NAME = "gpxDirTable"
		const val TMP_NAME_COLUMN_COUNT = "itemsCount"
		const val UNKNOWN_TIME_THRESHOLD = 10L
		val GPX_UPDATE_PARAMETERS_START = "UPDATE $GPX_TABLE_NAME SET "
		val GPX_FIND_BY_NAME_AND_DIR =
			" WHERE ${FILE_NAME.columnName} = ? AND ${FILE_DIR.columnName} = ?"
		val GPX_MIN_CREATE_DATE =
			"SELECT MIN(${FILE_CREATION_TIME.columnName}) FROM $GPX_TABLE_NAME WHERE ${FILE_CREATION_TIME.columnName} > $UNKNOWN_TIME_THRESHOLD"
		val GPX_MAX_COLUMN_VALUE = "SELECT MAX(%s) FROM $GPX_TABLE_NAME"
		val CHANGE_NULL_TO_EMPTY_STRING_QUERY_PART =
			"case when %1\$s is null then '' else %1\$s end as %1\$s"
		val INCLUDE_NON_NULL_COLUMN_CONDITION = " WHERE %1\$s NOT NULL AND %1\$s <> '' "
		val GET_ITEM_COUNT_COLLECTION_BASE =
			"SELECT %s, count (*) as $TMP_NAME_COLUMN_COUNT FROM $GPX_TABLE_NAME%s group by %s ORDER BY %s %s"
	}

	init {
		val db = openConnection(false)
		db?.close()
	}

	fun openConnection(readonly: Boolean): SQLiteConnection? {
		var conn = PlatformUtil.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly)
		if (conn == null) return null

		if (conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close()
				conn = PlatformUtil.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false)
			}
			if (conn == null) return null
			val version = conn.getVersion()
			conn.setVersion(DB_VERSION) // not correct version but dangerous for crash loop
			if (version == 0) {
				GpxDbUtils.onCreate(conn)
			} else {
				GpxDbUtils.onUpgrade(this, conn, version, DB_VERSION)
			}
			//conn.setVersion(DB_VERSION); // correct version but dangerous for crash loop
		}
		return conn
	}

	fun updateDataItem(item: DataItem): Boolean {
		val file = item.file
		val tableName = GpxDbUtils.getTableName(file)
		val map = GpxDbUtils.getItemParameters(item)
		return updateGpxParameters(map, tableName, GpxDbUtils.getItemRowsToSearch(file))
	}

	private fun updateGpxParameters(
		rowsToUpdate: Map<GpxParameter, Any?>,
		tableName: String,
		rowsToSearch: Map<String, Any?>
	): Boolean {
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			return db?.let { updateGpxParameters(it, tableName, rowsToUpdate, rowsToSearch) } ?: false
		} finally {
			db?.close()
		}
	}

	private fun updateGpxParameters(
		db: SQLiteConnection,
		tableName: String,
		rowsToUpdate: Map<GpxParameter, Any?>,
		rowsToSearch: Map<String, Any?>
	): Boolean {
		val map = GpxDbUtils.convertGpxParameters(rowsToUpdate)
		val pair = DbUtils.createDbUpdateQuery(tableName, map, rowsToSearch)
		db.execSQL(pair.first, pair.second)
		return true
	}

	fun rename(currentFile: KFile, newFile: KFile): Boolean {
		val map = linkedMapOf(
			FILE_NAME to newFile.name(),
			FILE_DIR to GpxDbUtils.getGpxFileDir(newFile)
		)
		val tableName = GpxDbUtils.getTableName(currentFile)
		return updateGpxParameters(map, tableName, GpxDbUtils.getItemRowsToSearch(currentFile))
	}

	fun remove(file: KFile): Boolean {
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			db?.let {
				val fileName = file.name()
				val fileDir = GpxDbUtils.getGpxFileDir(file)
				val tableName = GpxDbUtils.getTableName(file)
				db.execSQL(
					"DELETE FROM $tableName $GPX_FIND_BY_NAME_AND_DIR",
					arrayOf(fileName, fileDir)
				)
				return true
			}
			return false
		} finally {
			db?.close()
		}
	}

	fun add(item: DataItem): Boolean {
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			return db?.let {
				insertItem(item, it)
				true
			} ?: false
		} finally {
			db?.close()
		}
	}

	fun insertItem(item: DataItem, db: SQLiteConnection) {
		val file = item.file
		val tableName = GpxDbUtils.getTableName(file)
		val map = GpxDbUtils.convertGpxParameters(GpxDbUtils.getItemParameters(item))
		val query = DbUtils.createDbInsertQuery(tableName, map.keys)
		db.execSQL(query, map.values.toTypedArray())
	}

	private fun readGpxDataItem(query: SQLiteCursor): GpxDataItem {
		val file = readItemFile(query)
		val item = GpxDataItem(file)
		val analysis = GpxTrackAnalysis()
		processItemParameters(item, query, entries, analysis)
		item.setAnalysis(analysis)
		return item
	}

	private fun readItemFile(query: SQLiteCursor): KFile {
		var fileDir: String = query.getString(query.getColumnIndex(FILE_DIR.columnName))
		val fileName = query.getString(query.getColumnIndex(FILE_NAME.columnName))

		val appDir = PlatformUtil.getAppDir()
		val gpxDir = PlatformUtil.getGpxDir()
		if (fileName == gpxDir.name()) {
			return gpxDir
		}
		fileDir = fileDir.replace(gpxDir.toString(), "")
		fileDir = fileDir.replace(appDir.toString(), "")
		val dir = if (fileDir.isEmpty()) gpxDir else KFile(gpxDir, fileDir)
		return KFile(dir, fileName)
	}

	private fun processItemParameters(
		item: DataItem,
		query: SQLiteCursor,
		parameters: List<GpxParameter>,
		analysis: GpxTrackAnalysis?
	) {
		for (parameter in parameters) {
			var value = GpxDbUtils.queryColumnValue(query, parameter)
			if (value == null && !parameter.isAppearanceParameter()) {
				value = parameter.defaultValue
			}
			if (parameter.analysisParameter) {
				analysis?.setGpxParameter(parameter, value)
			} else {
				if (parameter == COLOR) {
					value = GpxUtilities.parseColor(value as String?)
				} else if (parameter == COLORING_TYPE) {
					val type = value as String?
					var coloringType = ColoringType.valueOf(ColoringPurpose.TRACK, type)
					val scaleType = GradientScaleType.getGradientTypeByName(type)
					if (coloringType == null && scaleType != null) {
						coloringType = ColoringType.valueOf(scaleType)
						value = coloringType?.getName(null)
					}
				}
				item.setParameter(parameter, value)
			}
		}
	}

	private fun readGpxDirItem(query: SQLiteCursor): GpxDirItem {
		val file = readItemFile(query)
		val item = GpxDirItem(file)
		processItemParameters(item, query, GpxParameter.getGpxDirParameters(), null)
		return item
	}

	fun getTracksMinCreateDate(): Long {
		var minDate = -1L
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			db?.let {
				var query: SQLiteCursor? = null
				try {
					query = db.rawQuery(GPX_MIN_CREATE_DATE, null)
					if (query != null && query.moveToFirst()) {
						minDate = query.getLong(0)
					}
				} finally {
					query?.close()
				}
			}
		} finally {
			db?.close()
		}
		return minDate
	}

	fun getColumnMaxValue(parameter: GpxParameter): String {
		var maxValue = ""
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			if (db != null) {
				val queryString = GPX_MAX_COLUMN_VALUE.format(parameter.columnName)
				var query: SQLiteCursor? = null
				try {
					query = db.rawQuery(queryString, null)
					if (query != null && query.moveToFirst()) {
						maxValue = query.getString(0)
					}
				} finally {
					query?.close()
				}
			}
		} finally {
			db?.close()
		}
		return maxValue
	}

	fun getStringIntItemsCollection(
		columnName: String,
		includeEmptyValues: Boolean,
		sortByName: Boolean,
		sortDescending: Boolean
	): List<StringIntPair> {
		val column1 =
			if (includeEmptyValues) CHANGE_NULL_TO_EMPTY_STRING_QUERY_PART.format(columnName) else columnName
		val includeEmptyValuesPart =
			if (includeEmptyValues) "" else INCLUDE_NON_NULL_COLUMN_CONDITION.format(columnName)
		val orderBy = if (sortByName) columnName else TMP_NAME_COLUMN_COUNT
		val sortDirection = if (sortDescending) "DESC" else "ASC"
		val query = GET_ITEM_COUNT_COLLECTION_BASE.format(
			column1,
			includeEmptyValuesPart,
			columnName,
			orderBy,
			sortDirection
		)
		return getStringIntItemsCollection(query)
	}

	private fun getStringIntItemsCollection(dataQuery: String): List<StringIntPair> {
		val folderCollection = mutableListOf<StringIntPair>()
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			db?.let {
				var query: SQLiteCursor? = null
				try {
					query = db.rawQuery(dataQuery, null)
					if (query != null && query.moveToFirst()) {
						do {
							folderCollection.add(StringIntPair(query.getString(0), query.getInt(1)))
						} while (query.moveToNext())
					}
				} finally {
					query?.close()
				}
			}
		} finally {
			db?.close()
		}
		return folderCollection
	}

	fun getGpxDataItems(): List<GpxDataItem> {
		val items = mutableSetOf<GpxDataItem>()
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			db?.let {
				var query: SQLiteCursor? = null
				try {
					query = db.rawQuery(GpxDbUtils.getSelectGpxQuery(), null)
					if (query != null && query.moveToFirst()) {
						do {
							items.add(readGpxDataItem(query))
						} while (query.moveToNext())
					}
				} finally {
					query?.close()
				}
			}
		} finally {
			db?.close()
		}
		return items.toList()
	}

	fun getGpxDirItems(): List<GpxDirItem> {
		val items = mutableSetOf<GpxDirItem>()
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			db?.let {
				var query: SQLiteCursor? = null
				try {
					query = db.rawQuery(GpxDbUtils.getSelectGpxDirQuery(), null)
					if (query != null && query.moveToFirst()) {
						do {
							items.add(readGpxDirItem(query))
						} while (query.moveToNext())
					}
				} finally {
					query?.close()
				}
			}

		} finally {
			db?.close()
		}
		return items.toList()
	}

	fun getGpxDataItem(file: KFile): GpxDataItem? {
		return if (GpxDbUtils.isGpxFile(file)) getDataItem(file) as? GpxDataItem else null
	}

	fun getGpxDirItem(file: KFile): GpxDirItem? {
		return if (file.isDirectory()) getDataItem(file) as? GpxDirItem else null
	}

	private fun getDataItem(file: KFile): DataItem? {
		var db: SQLiteConnection? = null
		try {
			db = openConnection(false)
			db?.let { return getDataItem(file, db) } ?: return null
		} finally {
			db?.close()
		}
	}

	fun getDataItem(file: KFile, db: SQLiteConnection): DataItem? {
		val name = file.name()
		val dir = GpxDbUtils.getGpxFileDir(file)
		val gpxFile = GpxDbUtils.isGpxFile(file)
		val selectQuery =
			if (gpxFile) GpxDbUtils.getSelectGpxQuery() else GpxDbUtils.getSelectGpxDirQuery()
		var query: SQLiteCursor? = null
		try {
			query = db.rawQuery("$selectQuery $GPX_FIND_BY_NAME_AND_DIR", arrayOf(name, dir))
			if (query != null && query.moveToFirst()) {
				return if (gpxFile) readGpxDataItem(query) else readGpxDirItem(query)
			}
		} finally {
			query?.close()
		}
		return null
	}
}

