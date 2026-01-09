package net.osmand.plus.plugins.astro

import android.content.Context
import android.graphics.Color
import io.github.cosinekitty.astronomy.Body
import net.osmand.PlatformUtil
import net.osmand.plus.plugins.astro.SkyObject.Type
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyColor
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyName
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class AstroDataJsonProvider : AstroDataProvider() {

	companion object {
		private val LOG = PlatformUtil.getLog(AstroDataJsonProvider::class.java)
	}

	override fun getInitialSkyObjectsImpl(ctx: Context): List<SkyObject> {
		val objects = mutableListOf<SkyObject>()

		// Planets
		getPlanets(objects, ctx)

		objects.addAll(loadObjectsFromJson(ctx, "astro/stars.json", Type.STAR, Color.WHITE))
		objects.addAll(loadObjectsFromJson(ctx, "astro/galaxies.json", Type.GALAXY, Color.LTGRAY))
		objects.addAll(loadObjectsFromJson(ctx, "astro/black_holes.json", Type.BLACK_HOLE, Color.MAGENTA))

		return objects
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
			LOG.error("Error loading objects from JSON", e)
		}
		return loadedObjects
	}

	override fun getConstellationsImpl(ctx: Context): List<Constellation> {
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
			LOG.error("Error loading constellations from JSON", e)
		}

		return constellations
	}
}