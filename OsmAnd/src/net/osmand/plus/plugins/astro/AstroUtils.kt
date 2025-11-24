package net.osmand.plus.plugins.astro

import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

	@ColorInt
	fun bodyColor(b: Body): Int = when (b) {
		Body.Sun -> "#FFD54F".toColorInt()
		Body.Moon -> "#BDBDBD".toColorInt()
		Body.Mercury -> "#F9A825".toColorInt()
		Body.Venus -> "#66BB6A".toColorInt()
		Body.Mars -> "#EF5350".toColorInt()
		Body.Jupiter -> "#8D6E63".toColorInt()
		Body.Saturn -> "#D4A373".toColorInt()
		else -> "#FFFFFF".toColorInt()
	}

	data class Twilight(
		val sunrise: ZonedDateTime?, val sunset: ZonedDateTime?,
		val civilDawn: ZonedDateTime?, val civilDusk: ZonedDateTime?,
		val nauticalDawn: ZonedDateTime?, val nauticalDusk: ZonedDateTime?,
		val astroDawn: ZonedDateTime?, val astroDusk: ZonedDateTime?
	)

	/**
	 * Shared logic to compute twilight events for a specific day window.
	 */
	fun computeTwilight(startLocal: ZonedDateTime, endLocal: ZonedDateTime, obs: Observer, zoneId: ZoneId): Twilight {
		fun findAlt(direction: Direction, deg: Double): ZonedDateTime? {
			val t0 = Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli())
			val t = searchAltitude(Body.Sun, obs, direction, t0, 2.0, deg)
			return t?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(zoneId) }
		}

		val searchStart = Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli())

		val sr = searchRiseSet(Body.Sun, obs, Direction.Rise, searchStart, 2.0)
		val ss = searchRiseSet(Body.Sun, obs, Direction.Set, searchStart, 2.0)

		// Convert to ZonedDateTime and filter to ensure they are within our view window
		// Use 'let' safely to convert, handling nulls from astronomy engine
		val sunrise = sr?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(zoneId) }
		val sunset = ss?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(zoneId) }

		return Twilight(
			sunrise, sunset,
			findAlt(Direction.Rise, -6.0), findAlt(Direction.Set, -6.0),
			findAlt(Direction.Rise, -12.0), findAlt(Direction.Set, -12.0),
			findAlt(Direction.Rise, -18.0), findAlt(Direction.Set, -18.0)
		)
	}
}