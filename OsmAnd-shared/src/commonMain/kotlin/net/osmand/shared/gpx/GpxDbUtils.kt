package net.osmand.shared.gpx

import net.osmand.shared.IndexConstants
import net.osmand.shared.db.SQLiteAPI.*
import net.osmand.shared.gpx.GpxDatabase.Companion.GPX_DIR_TABLE_NAME
import net.osmand.shared.gpx.GpxDatabase.Companion.GPX_TABLE_NAME
import net.osmand.shared.gpx.GpxTrackAnalysis.Companion.ANALYSIS_VERSION
import net.osmand.shared.io.KFile
import net.osmand.shared.util.PlatformUtil
import kotlin.collections.set

object GpxDbUtils {

	private const val GPX_TABLE_INDEX = "indexNameDir"
	private const val GPX_DIR_TABLE_INDEX = "gpxDirIndexNameDir"

	fun getCreateGpxTableQuery(): String {
		return getCreateTableQuery(GpxParameter.entries, GPX_TABLE_NAME)
	}

	fun getCreateGpxDirTableQuery(): String {
		return getCreateTableQuery(GpxParameter.getGpxDirParameters(), GPX_DIR_TABLE_NAME)
	}

	fun getSelectGpxQuery(): String {
		return getSelectQuery(GpxParameter.entries, GPX_TABLE_NAME)
	}

	fun getSelectGpxDirQuery(): String {
		return getSelectQuery(GpxParameter.getGpxDirParameters(), GPX_DIR_TABLE_NAME)
	}

	fun getCreateTableQuery(parameters: List<GpxParameter>, tableName: String): String {
		val builder = StringBuilder("CREATE TABLE IF NOT EXISTS $tableName (")
		val iterator = parameters.iterator()
		while (iterator.hasNext()) {
			val parameter = iterator.next()
			builder.append(" ${parameter.columnName} ${parameter.columnType}")
			if (iterator.hasNext()) {
				builder.append(", ")
			} else {
				builder.append(");")
			}
		}
		return builder.toString()
	}

	fun getSelectQuery(parameters: List<GpxParameter>, tableName: String): String {
		val builder = StringBuilder("SELECT ")
		val iterator = parameters.iterator()
		while (iterator.hasNext()) {
			val parameter = iterator.next()
			builder.append(parameter.columnName)
			if (iterator.hasNext()) {
				builder.append(", ")
			} else {
				builder.append(" FROM $tableName")
			}
		}
		return builder.toString()
	}

	fun getGpxIndexQuery(): String {
		return "CREATE INDEX IF NOT EXISTS $GPX_TABLE_INDEX ON $GPX_TABLE_NAME (${GpxParameter.FILE_NAME.columnName}, ${GpxParameter.FILE_DIR.columnName});"
	}

	fun getGpxDirIndexQuery(): String {
		return "CREATE INDEX IF NOT EXISTS $GPX_DIR_TABLE_INDEX ON $GPX_DIR_TABLE_NAME (${GpxParameter.FILE_NAME.columnName}, ${GpxParameter.FILE_DIR.columnName});"
	}

	fun onCreate(db: SQLiteConnection) {
		db.execSQL(getCreateGpxTableQuery())
		db.execSQL(getGpxIndexQuery())
		db.execSQL(getCreateGpxDirTableQuery())
		db.execSQL(getGpxDirIndexQuery())
	}

	fun onUpgrade(database: GpxDatabase, db: SQLiteConnection, oldVersion: Int, newVersion: Int) {
		val gpxCursor = db.rawQuery("SELECT * FROM $GPX_TABLE_NAME LIMIT 1", null)
		if (gpxCursor != null) {
			val columnNames = gpxCursor.getColumnNames().map { it.lowercase() }.toMutableSet()

			addIfMissingGpxTableColumn(columnNames, db, GpxParameter.FILE_NAME)
			addIfMissingGpxTableColumn(columnNames, db, GpxParameter.FILE_DIR)
			addIfMissingGpxTableColumn(columnNames, db, GpxParameter.TOTAL_DISTANCE)
			// Add other columns similarly...

			GpxParameter.entries.forEach { parameter ->
				if (!columnNames.contains(parameter.columnName.lowercase())) {
					addGpxTableColumn(db, parameter)
				}
			}
		}

		db.execSQL(getCreateGpxDirTableQuery())

		val gpxDirCursor = db.rawQuery("SELECT * FROM $GPX_DIR_TABLE_NAME LIMIT 1", null)
		if (gpxDirCursor != null) {
			val dirColumnNames = gpxDirCursor.getColumnNames().map { it.lowercase() }.toMutableSet()

			GpxParameter.getGpxDirParameters().forEach { parameter ->
				if (!dirColumnNames.contains(parameter.columnName.lowercase())) {
					addGpxDirTableColumn(db, parameter)
				}
			}
		}

		db.execSQL(getGpxIndexQuery())
		db.execSQL(getGpxDirIndexQuery())
	}

	private fun addIfMissingGpxTableColumn(columnNamesLC: MutableSet<String>, db: SQLiteConnection, parameter: GpxParameter) {
		if (columnNamesLC.contains(parameter.columnName.lowercase())) {
			return
		}
		columnNamesLC.add(parameter.columnName.lowercase())
		addGpxTableColumn(db, parameter)
	}

	private fun addGpxTableColumn(db: SQLiteConnection, parameter: GpxParameter) {
		addTableColumn(db, GPX_TABLE_NAME, parameter.columnName, parameter.columnType)
	}

	private fun addGpxDirTableColumn(db: SQLiteConnection, parameter: GpxParameter) {
		addTableColumn(db, GPX_DIR_TABLE_NAME, parameter.columnName, parameter.columnType)
	}

	private fun addTableColumn(db: SQLiteConnection, tableName: String, columnName: String, columnType: String) {
		db.execSQL("ALTER TABLE $tableName ADD $columnName $columnType")
	}

	fun isAnalyseNeeded(item: GpxDataItem?): Boolean {
		if (item != null) {
			return !item.hasData() || item.getAnalysis() == null
					|| item.getAnalysis()!!.wptCategoryNames == null
					|| (item.getAnalysis()!!.getLatLonStart() == null && item.getAnalysis()!!.points > 0)
					|| item.requireParameter(GpxParameter.FILE_LAST_MODIFIED_TIME) as Long != item.file.lastModified()
					|| item.requireParameter(GpxParameter.FILE_CREATION_TIME) as Long <= 0
					|| createDataVersion(ANALYSIS_VERSION) > item.requireParameter(GpxParameter.DATA_VERSION) as Int
		}
		return true
	}

	fun getGpxFileDir(file: KFile): String {
		file.parent()?.let {
			val gpxDir = PlatformUtil.getGpxDir()
			if (file == gpxDir) {
				return ""
			}
			val relativePath = KFile(file.path().replace("${gpxDir.path}/", ""))
			val fileDir = if (file.isDirectory()) relativePath.path else relativePath.parent()
			return fileDir.toString()
		}
		return ""
	}

	fun getItemParameters(item: DataItem): Map<GpxParameter, Any?> {
		return when (item) {
			is GpxDataItem -> getItemParameters(item)
			is GpxDirItem -> getItemParameters(item)
			else -> emptyMap()
		}
	}

	fun getItemParameters(item: GpxDataItem): Map<GpxParameter, Any?> {
		val map = LinkedHashMap<GpxParameter, Any?>()
		val analysis = item.getAnalysis()
		val hasAnalysis = analysis != null
		GpxParameter.entries.forEach { parameter ->
			map[parameter] = if (parameter.analysisParameter) {
				if (hasAnalysis) analysis!!.getGpxParameter(parameter) else null
			} else {
				item.getParameter(parameter)
			}
		}
		return map
	}

	fun getItemParameters(item: GpxDirItem): Map<GpxParameter, Any?> {
		val map = LinkedHashMap<GpxParameter, Any?>()
		GpxParameter.getGpxDirParameters().forEach { parameter ->
			map[parameter] = item.getParameter(parameter)
		}
		return map
	}

	fun convertGpxParameters(parameters: Map<GpxParameter, Any?>): Map<String, Any?> {
		val map = LinkedHashMap<String, Any?>()
		parameters.forEach { (parameter, value) ->
			map[parameter.columnName] = parameter.convertToDbValue(value)
		}
		return map
	}

	fun getItemRowsToSearch(file: KFile): Map<String, Any?> {
		val map = LinkedHashMap<String, Any?>()
		map[GpxParameter.FILE_NAME.columnName] = file.name()
		map[GpxParameter.FILE_DIR.columnName] = getGpxFileDir(file)
		return map
	}

	fun getTableName(file: KFile): String {
		return if (file.exists()) {
			if (!file.isDirectory()) GPX_TABLE_NAME else GPX_DIR_TABLE_NAME
		} else {
			if (isGpxFile(file)) GPX_TABLE_NAME else GPX_DIR_TABLE_NAME
		}
	}

	fun queryColumnValue(query: SQLiteCursor, parameter: GpxParameter): Any? {
		val index = query.getColumnIndex(parameter.columnName)
		if (query.isNull(index)) {
			return null
		}
		return when (parameter.columnType) {
			"TEXT" -> query.getString(index)
			"double" -> query.getDouble(index)
			"int" -> {
				val value = query.getInt(index)
				if (parameter.typeClass == Boolean::class) value == 1 else value
			}
			"bigint", "long" -> query.getLong(index)
			else -> throw IllegalArgumentException("Unknown column type ${parameter.columnType}")
		}
	}

	fun isGpxFile(file: KFile): Boolean {
		return file.name().lowercase().endsWith(IndexConstants.GPX_FILE_EXT)
	}

	fun createDataVersion(analysisVersion: Int): Int {
		return (GpxDatabase.DB_VERSION shl 10) + analysisVersion
	}
}
