package net.osmand.plus.plugins.astro

import android.content.Context
import android.graphics.Color
import io.github.cosinekitty.astronomy.Body
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyColor
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyName
import net.osmand.plus.plugins.astro.SkyObject
import net.osmand.plus.plugins.astro.SkyObject.Type
import net.osmand.shared.util.LoggerFactory
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object AstroDataProvider {

	private val log = LoggerFactory.getLogger("AstroDataProvider")

	private var cachedSkyObjects: List<SkyObject>? = null
	private var cachedConstellations: List<Constellation>? = null

	data class Constellation(
		val name: String,
		val wid: String, // Wikipedia ID
		val lines: List<Pair<Int, Int>> // Pairs of SkyObject HIP IDs
	)

	@Synchronized
	fun getInitialSkyObjects(ctx: Context): List<SkyObject> {
		cachedSkyObjects?.let { return it.toList() }

		val objects = mutableListOf<SkyObject>()

		// Planets
		val planets = listOf(
			Triple(Body.Sun, bodyColor(Body.Sun), "Q525"),
			Triple(Body.Moon, bodyColor(Body.Moon), "Q405"),
			Triple(Body.Mercury, bodyColor(Body.Mercury), "Q308"),
			Triple(Body.Venus, bodyColor(Body.Venus), "Q313"),
			Triple(Body.Mars, bodyColor(Body.Mars), "Q111"),
			Triple(Body.Jupiter, bodyColor(Body.Jupiter), "Q319"),
			Triple(Body.Saturn, bodyColor(Body.Saturn), "Q193"),
			Triple(Body.Uranus, bodyColor(Body.Uranus), "Q324"),
			Triple(Body.Neptune, bodyColor(Body.Neptune), "Q332")
		)

		planets.forEach { (body, color, wid) ->
			objects.add(SkyObject(
				id = body.name.lowercase(),
				hip = -1,
				wid = wid,
				type = when (body) {
					Body.Sun -> Type.SUN
					Body.Moon -> Type.MOON
					else -> Type.PLANET
				},
				body = body,
				name = bodyName(ctx, body),
				ra = 0.0, dec = 0.0,
				magnitude = -2f,
				color = color
			))
		}

		objects.addAll(loadObjectsFromJson(ctx, "astro/stars.json", Type.STAR, Color.WHITE))
		objects.addAll(loadObjectsFromJson(ctx, "astro/galaxies.json", Type.GALAXY, Color.LTGRAY))
		objects.addAll(loadObjectsFromJson(ctx, "astro/black_holes.json", Type.BLACK_HOLE, Color.MAGENTA))

		cachedSkyObjects = objects
		return objects.toList()
	}

	private fun loadObjectsFromJson(
		ctx: Context,
		fileName: String,
		type: Type,
		defaultColor: Int
	): List<SkyObject> {
		val loadedObjects = mutableListOf<SkyObject>()
		try {
			val inputStream = ctx.assets.open(fileName)
			val reader = BufferedReader(InputStreamReader(inputStream))
			val jsonString = reader.use { it.readText() }
			val jsonArray = JSONArray(jsonString)

			for (i in 0 until jsonArray.length()) {
				val obj = jsonArray.getJSONObject(i)
				val name = obj.getString("name")
				val ra = obj.getDouble("ra")
				val dec = obj.getDouble("dec")
				val mag = obj.getDouble("mag").toFloat()

				val hip = if (obj.isNull("hip")) null else obj.getInt("hip")
				val wid = if (obj.isNull("wid")) null else obj.getString("wid")

				// ID Generation Logic (Replicated from original code to maintain compatibility)
				val id = when (type) {
					Type.STAR -> name.lowercase().replace(' ', '_')
					Type.GALAXY -> name.lowercase().replace(' ', '_').filter { it.isLetterOrDigit() || it == '_' }
					Type.BLACK_HOLE -> "bh_" + name.lowercase()
						.replace('*', '_')
						.replace(' ', '_')
						.replace('-', '_')
						.replace('(', '_')
						.replace(')', '_')
					else -> name.lowercase()
				}

				// Prevent duplicates if necessary (though JSON shouldn't have them)
				if (loadedObjects.none { it.id == id }) {
					loadedObjects.add(SkyObject(
						id = id,
						hip = hip ?: -1,
						wid = wid ?: "",
						type = type,
						body = null,
						name = name,
						ra = ra,
						dec = dec,
						magnitude = mag,
						color = defaultColor
					))
				}
			}
		} catch (e: Exception) {
			log.error("Error loading objects from JSON", e)
		}
		return loadedObjects
	}

	@Synchronized
	fun getConstellations(ctx: Context): List<Constellation> {
		cachedConstellations?.let { return it.toList() }

		val constellations = mutableListOf<Constellation>()
		try {
			val inputStream = ctx.assets.open("astro/constellations.json")
			val reader = BufferedReader(InputStreamReader(inputStream))
			val jsonString = reader.use { it.readText() }
			val jsonArray = JSONArray(jsonString)

			for (i in 0 until jsonArray.length()) {
				val obj = jsonArray.getJSONObject(i)
				val name = obj.getString("name")
				val wid = obj.optString("wid", "")
				val linesArray = obj.getJSONArray("lines")

				val lines = mutableListOf<Pair<Int, Int>>()
				for (j in 0 until linesArray.length()) {
					val linePair = linesArray.getJSONArray(j)
					if (linePair.length() >= 2) {
						val start = linePair.getInt(0)
						val end = linePair.getInt(1)
						lines.add(Pair(start, end))
					}
				}

				constellations.add(Constellation(name, wid, lines))
			}
		} catch (e: Exception) {
			log.error("Error loading constellations from JSON", e)
		}
		cachedConstellations = constellations
		return constellations.toList()
	}
}