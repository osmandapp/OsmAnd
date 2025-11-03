package net.osmand.plus.plugins.astro

import io.github.cosinekitty.astronomy.Body

object AstroUtils {

	fun bodyName(b: Body) = when (b) {
		Body.Sun -> "Sun"
		Body.Moon -> "Moon"
		Body.Mercury -> "Mercury"
		Body.Venus -> "Venus"
		Body.Mars -> "Mars"
		Body.Jupiter -> "Jupiter"
		Body.Saturn -> "Saturn"
		Body.Uranus -> "Uranus"
		Body.Neptune -> "Neptune"
		Body.Pluto -> "Pluto"
		else -> b.toString()
	}
}