package net.osmand.plus.plugins.astro

import android.content.Context
import io.github.cosinekitty.astronomy.Body
import net.osmand.plus.plugins.astro.SkyObject.Type
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyColor
import net.osmand.plus.plugins.astro.utils.AstroUtils.bodyName

abstract class AstroDataProvider {

	private var cachedSkyObjects: List<SkyObject>? = null
	private var cachedConstellations: List<Constellation>? = null

	abstract fun getInitialSkyObjectsImpl(ctx: Context): List<SkyObject>

	@Synchronized
	fun getInitialSkyObjects(ctx: Context): List<SkyObject> {
		cachedSkyObjects?.let { return it.toList() }

		val objects = getInitialSkyObjectsImpl(ctx)

		cachedSkyObjects = objects
		return objects.toList()
	}

	abstract fun getConstellationsImpl(ctx: Context): List<Constellation>

	@Synchronized
	fun getConstellations(ctx: Context): List<Constellation> {
		cachedConstellations?.let { return it.toList() }

		val constellations = getConstellationsImpl(ctx)

		cachedConstellations = constellations
		return constellations.toList()
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
}