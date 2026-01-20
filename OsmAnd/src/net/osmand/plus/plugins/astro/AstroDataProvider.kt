package net.osmand.plus.plugins.astro

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Body
import net.osmand.PlatformUtil
import net.osmand.plus.plugins.astro.SkyObject.Type
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyColor
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyName
import org.json.JSONArray

abstract class AstroDataProvider {

	private var cachedSkyObjects:List<SkyObject>?=null
	private var cachedConstellations:List<Constellation>?=null

	companion object {
		private val LOG = PlatformUtil.getLog(AstroDataProvider::class.java)
	}

	abstract fun getSkyObjectsImpl(ctx: Context): List<SkyObject>

	@Synchronized
	fun getSkyObjects(ctx: Context): List<SkyObject> {
		cachedSkyObjects?.let { return it }

		val objects = getSkyObjectsImpl(ctx)

		cachedSkyObjects = objects
		return objects
	}

	@Synchronized
	fun clearCache() {
		cachedSkyObjects = null
		cachedConstellations = null
	}

	abstract fun getConstellationsImpl(ctx: Context): List<Constellation>

	@Synchronized
	fun getConstellations(ctx: Context): List<Constellation> {
		cachedConstellations?.let { return it }

		val constellations = getConstellationsImpl(ctx)
		val skyObjectMap = getSkyObjects(ctx).associateBy { it.hip }
		constellations.forEach { c ->
			val center = AstroUtils.calculateConstellationCenter(c, skyObjectMap)
			c.apply {
				ra = center?.first ?: 0.0
				dec = center?.second ?: 0.0
			}
		}
		cachedConstellations = constellations
		return constellations
	}

	protected fun getPlanets(
		objects: MutableList<SkyObject>,
		ctx: Context
	) {
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
			objects.add(
				SkyObject(
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
				)
			)
		}
	}

	protected fun getTypeColor(type: Type): Int {
		return when (type) {
			Type.STAR -> Color.WHITE
			Type.GALAXY, Type.GALAXY_CLUSTER -> Color.LTGRAY
			Type.BLACK_HOLE -> Color.MAGENTA
			Type.NEBULA -> "#E0CEF5".toColorInt() // Light
			Type.OPEN_CLUSTER -> "#FFFFE0".toColorInt() // Light yellow
			Type.GLOBULAR_CLUSTER -> "#FFFACD".toColorInt() // Lemon chiffon
			else -> Color.WHITE
		}
	}

	protected fun parseLines(json: String?): List<Pair<Int, Int>> {
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

	protected fun generateId(type: Type, name: String): String {
		return when (type) {
			Type.STAR -> name.lowercase().replace(' ', '_')
			Type.GALAXY -> name.lowercase().replace(' ', '_')
				.filter { it.isLetterOrDigit() || it == '_' }

			Type.BLACK_HOLE -> "bh_" + name.lowercase().replace('*', '_').replace(' ', '_')
				.replace('-', '_').replace('(', '_').replace(')', '_')

			else -> name.lowercase().replace(' ', '_')
		}
	}
}