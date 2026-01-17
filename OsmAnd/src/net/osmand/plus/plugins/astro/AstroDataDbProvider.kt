package net.osmand.plus.plugins.astro

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Body
import net.osmand.IndexConstants
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.astro.SkyObject.Type
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyColor
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyName
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

	private class DbHelper(app: OsmandApplication) : SQLiteOpenHelper(
			app, app.getAppPath(IndexConstants.ASTRO_DIR).absolutePath + File.separator + DATABASE_NAME, null, DATABASE_VERSION) {

		override fun onCreate(db: SQLiteDatabase) {
		}

		override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		}

		override fun getReadableDatabase(): SQLiteDatabase {
			val db = super.getReadableDatabase()
			val dbFile = File(db.path)
			if (!dbFile.exists()) {
				throw IllegalStateException("Database file does not exist: $dbFile")
			}
			return db
		}
	}

	override fun getInitialSkyObjectsImpl(ctx: Context): List<SkyObject> {
		val objects = mutableListOf<SkyObject>()

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
					var type = mapType(typeStr) ?: continue

					val originalName = c.getString(idxName)
					var name = originalName
					val wikidata = if (c.isNull(idxWiki)) "" else c.getString(idxWiki)
					val ra = if (c.isNull(idxRa)) 0.0 else c.getDouble(idxRa)
					val dec = if (c.isNull(idxDec)) 0.0 else c.getDouble(idxDec)
					val mag =
						if (c.isNull(idxMag)) 25f else c.getFloat(idxMag) // High mag if null (invisible)
					val hip = if (c.isNull(idxHip)) -1 else c.getInt(idxHip)

					var body: Body? = null
					var color: Int

					if (typeStr == "solar_system") {
						body = getBody(wikidata)
						if (body != null) {
							type = when (body) {
								Body.Sun -> Type.SUN
								Body.Moon -> Type.MOON
								else -> Type.PLANET
							}
							color = bodyColor(body)
							name = bodyName(ctx, body)
						} else {
							continue
						}
					} else {
						color = getTypeColor(type)
					}

					// ID Generation logic (matching AstroDataProvider for consistency where possible)
					val id = generateId(type, originalName)

					objects.add(
						SkyObject(
							id = id,
							hip = hip,
							wid = wikidata,
							type = type,
							body = body,
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

		if (objects.isEmpty()) {
			getPlanets(objects, ctx)
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

	private fun getBody(wid: String): Body? {
		return when (wid) {
			"Q525" -> Body.Sun
			"Q405" -> Body.Moon
			"Q308" -> Body.Mercury
			"Q313" -> Body.Venus
			"Q111" -> Body.Mars
			"Q319" -> Body.Jupiter
			"Q193" -> Body.Saturn
			"Q324" -> Body.Uranus
			"Q332" -> Body.Neptune
			"Q339" -> Body.Pluto
			else -> null
		}
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
			"solar_system" -> Type.PLANET
			else -> null
		}
	}
}