package net.osmand.plus.plugins.astro

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import androidx.core.graphics.toColorInt
import net.osmand.IndexConstants
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.astro.SkyObject.Type
import org.json.JSONArray
import java.io.File

class AstroDataDbProvider : AstroDataProvider() {

	companion object {
		private val LOG = PlatformUtil.getLog(AstroDataDbProvider::class.java)

		private const val DATABASE_NAME = "stars.db"
		private const val DATABASE_VERSION = 1

		// Table and Columns
		private const val TABLE_OBJECTS = "Objects"
		private const val COL_WIKIDATA = "wikidata"
		private const val COL_NAME = "name"
		private const val COL_TYPE = "type"
		private const val COL_RA = "ra"
		private const val COL_DEC = "dec"
		private const val COL_LINES = "lines"
		private const val COL_MAG = "mag"
		private const val COL_HIP = "hip"
	}

	private class DbHelper(val app: OsmandApplication) : SQLiteOpenHelper(
			app, app.getAppPath(IndexConstants.ASTRO_DIR).absolutePath + File.separator + DATABASE_NAME, null, DATABASE_VERSION) {

		override fun onCreate(db: SQLiteDatabase) {
		}

		override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		}

		override fun getReadableDatabase(): SQLiteDatabase {
			val dbFile = File(app.getAppPath(IndexConstants.ASTRO_DIR), DATABASE_NAME)
			if (!dbFile.exists()) {
				throw IllegalStateException("Database file does not exist: $dbFile")
			}
			return super.getReadableDatabase()
		}
	}

	override fun getInitialSkyObjectsImpl(ctx: Context): List<SkyObject> {
		val objects = mutableListOf<SkyObject>()

		// Planets
		getPlanets(objects, ctx)

		// 2. Read from DB
		val dbHelper = DbHelper(ctx.applicationContext as OsmandApplication)
		try {
			val db = dbHelper.readableDatabase
			// Select all except constellations (which are lines)
			val cursor = db.query(
				TABLE_OBJECTS,
				null,
				"$COL_TYPE != ?",
				arrayOf("constellations"),
				null, null, null
			)

			cursor.use { c ->
				val idxWiki = c.getColumnIndex(COL_WIKIDATA)
				val idxName = c.getColumnIndex(COL_NAME)
				val idxType = c.getColumnIndex(COL_TYPE)
				val idxRa = c.getColumnIndex(COL_RA)
				val idxDec = c.getColumnIndex(COL_DEC)
				val idxMag = c.getColumnIndex(COL_MAG)
				val idxHip = c.getColumnIndex(COL_HIP)

				while (c.moveToNext()) {
					val typeStr = c.getString(idxType)
					val type = mapType(typeStr) ?: continue

					val name = c.getString(idxName)
					val wikidata = if (c.isNull(idxWiki)) "" else c.getString(idxWiki)
					val ra = if (c.isNull(idxRa)) 0.0 else c.getDouble(idxRa)
					val dec = if (c.isNull(idxDec)) 0.0 else c.getDouble(idxDec)
					val mag =
						if (c.isNull(idxMag)) 50f else c.getFloat(idxMag) // High mag if null (invisible)
					val hip = if (c.isNull(idxHip)) -1 else c.getInt(idxHip)

					// ID Generation logic (matching AstroDataProvider for consistency where possible)
					val id = generateId(type, name)

					val color = getTypeColor(type)

					objects.add(
						SkyObject(
							id = id,
							hip = hip,
							wid = wikidata,
							type = type,
							body = null,
							name = name,
							ra = ra,
							dec = dec,
							magnitude = mag,
							color = color
						)
					)
				}
			}

			db.close()
		} catch (e: Exception) {
			LOG.error("Error reading initial sky objects from DB", e)
		}

		return objects
	}

	override fun getConstellationsImpl(ctx: Context): List<Constellation> {
		val constellations = mutableListOf<Constellation>()
		val dbHelper = DbHelper(ctx.applicationContext as OsmandApplication)
		try {
			val db = dbHelper.readableDatabase
			val cursor = db.query(
				TABLE_OBJECTS,
				arrayOf(COL_NAME, COL_WIKIDATA, COL_LINES),
				"$COL_TYPE = ?",
				arrayOf("constellations"),
				null,
				null,
				null
			)

			cursor.use { c ->
				val idxName = c.getColumnIndex(COL_NAME)
				val idxWiki = c.getColumnIndex(COL_WIKIDATA)
				val idxLines = c.getColumnIndex(COL_LINES)

				while (c.moveToNext()) {
					val name = c.getString(idxName)
					val wid = if (c.isNull(idxWiki)) "" else c.getString(idxWiki)
					val linesJson = c.getString(idxLines)

					val lines = parseLines(linesJson)
					if (lines.isNotEmpty()) {
						constellations.add(Constellation(name, wid, lines))
					}
				}
			}

			db.close()
		} catch (e: Exception) {
			LOG.error("Error reading constellations from DB", e)
		}
		return constellations
	}

	private fun mapType(typeStr: String): Type? {
		return when (typeStr) {
			"stars" -> Type.STAR
			"galaxies" -> Type.GALAXY
			"black_holes" -> Type.BLACK_HOLE
			"nebulae" -> Type.NEBULA
			"open_clusters" -> Type.OPEN_CLUSTER
			"globular_clusters" -> Type.GLOBULAR_CLUSTER
			"galaxy_clusters" -> Type.GALAXY_CLUSTER
			"constellations" -> Type.CONSTELLATION
			else -> null
		}
	}

	private fun generateId(type: Type, name: String): String {
		return when (type) {
			Type.STAR -> name.lowercase().replace(' ', '_')
			Type.GALAXY -> name.lowercase().replace(' ', '_')
				.filter { it.isLetterOrDigit() || it == '_' }

			Type.BLACK_HOLE -> "bh_" + name.lowercase().replace('*', '_').replace(' ', '_')
				.replace('-', '_').replace('(', '_').replace(')', '_')

			else -> name.lowercase().replace(' ', '_')
		}
	}

	private fun getTypeColor(type: Type): Int {
		return when (type) {
			Type.STAR -> Color.WHITE
			Type.GALAXY, Type.GALAXY_CLUSTER -> Color.LTGRAY
			Type.BLACK_HOLE -> Color.MAGENTA
			Type.NEBULA -> "#E0CEF5".toColorInt() // Light
			// lilac/purple
			Type.OPEN_CLUSTER -> "#FFFFE0".toColorInt() // Light yellow
			Type.GLOBULAR_CLUSTER -> "#FFFACD".toColorInt() // Lemon chiffon
			else -> Color.WHITE
		}
	}

	private fun parseLines(json: String?): List<Pair<Int, Int>> {
		if (json.isNullOrEmpty()) return emptyList()
		val list = mutableListOf<Pair<Int, Int>>()
		try {
			val jsonArray = JSONArray(json)
			for (i in 0 until jsonArray.length()) {
				val segment = jsonArray.getJSONArray(i)
				if (segment.length() >= 2) {
					list.add(segment.getInt(0) to segment.getInt(1))
				}
			}
		} catch (e: Exception) {
			LOG.error("Error parsing constellation lines: $json", e)
		}
		return list
	}
}
