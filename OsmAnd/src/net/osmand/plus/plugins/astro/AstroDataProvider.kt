package net.osmand.plus.plugins.astro

import android.content.Context
import android.graphics.Color
import io.github.cosinekitty.astronomy.Body
import net.osmand.plus.plugins.astro.AstroUtils.bodyColor
import net.osmand.plus.plugins.astro.AstroUtils.bodyName
import net.osmand.plus.plugins.astro.views.SkyObject
import net.osmand.plus.plugins.astro.views.SkyObject.Type

object AstroDataProvider {

	fun getInitialSkyObjects(ctx: Context): List<SkyObject> {
		val objects = mutableListOf<SkyObject>()

		// Planets
		val planets = listOf(
			Pair(Body.Sun, bodyColor(Body.Sun)),
			Pair(Body.Moon, bodyColor(Body.Moon)),
			Pair(Body.Mercury, bodyColor(Body.Mercury)),
			Pair(Body.Venus, bodyColor(Body.Venus)),
			Pair(Body.Mars, bodyColor(Body.Mars)),
			Pair(Body.Jupiter, bodyColor(Body.Jupiter)),
			Pair(Body.Saturn, bodyColor(Body.Saturn)),
			Pair(Body.Uranus, bodyColor(Body.Uranus)),
			Pair(Body.Neptune, bodyColor(Body.Neptune))
		)

		planets.forEach { (body, color) ->
			objects.add(SkyObject(
				id = body.name.lowercase(),
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

		// Bright Stars
		val brightStars = listOf(
			"Sirius" to (6.75 to -16.72),
			"Canopus" to (6.40 to -52.70),
			"Alpha Centauri" to (14.66 to -60.83),
			"Arcturus" to (14.26 to 19.18),
			"Vega" to (18.62 to 38.78),
			"Capella" to (5.28 to 46.00),
			"Rigel" to (5.24 to -8.20),
			"Procyon" to (7.65 to 5.21),
			"Achernar" to (1.63 to -57.23),
			"Betelgeuse" to (5.92 to 7.41),
			"Hadar" to (14.06 to -60.37),
			"Altair" to (19.85 to 8.87),
			"Acrux" to (12.44 to -63.10),
			"Aldebaran" to (4.60 to 16.51),
			"Antares" to (16.49 to -26.43),
			"Spica" to (13.42 to -11.16),
			"Pollux" to (7.76 to 28.03),
			"Fomalhaut" to (22.96 to -29.62),
			"Deneb" to (20.69 to 45.28),
			"Mimosa" to (12.80 to -59.69),
			"Polaris" to (2.53 to 89.26)
		)

		brightStars.forEach { (name, coords) ->
			objects.add(SkyObject(
				id = name.lowercase().replace(' ', '_'),
				type = Type.STAR,
				body = null,
				name = name,
				ra = coords.first,
				dec = coords.second,
				magnitude = 1.0f,
				color = Color.WHITE
			))
		}

		return objects
	}
}