package net.osmand.plus.plugins.astro

import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
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

	// ---------- Extensions for Type Conversions ----------

	fun Time.toZoned(zoneId: ZoneId): ZonedDateTime =
		Instant.ofEpochMilli(this.toMillisecondsSince1970()).atZone(zoneId)

	fun ZonedDateTime.toAstroTime(): Time =
		Time.fromMillisecondsSince1970(this.toInstant().toEpochMilli())

	// ---------- Physics / Math Helpers ----------

	/**
	 * Calculates the apparent altitude of a body at a specific time.
	 */
	fun altitude(body: Body, tLocal: ZonedDateTime, obs: Observer): Double {
		val tUtc = tLocal.toAstroTime()
		val eq = equator(body, tUtc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(tUtc, obs, eq.ra, eq.dec, Refraction.Normal)
		return hor.altitude
	}

	/**
	 * Searches for the next Rise and Set times after the given start time.
	 * Returns null if the event doesn't happen within the limit (default 2 days)
	 * or if the result falls outside the optional filter window [windowStart, windowEnd].
	 */
	fun nextRiseSet(
		body: Body,
		startSearch: ZonedDateTime,
		obs: Observer,
		windowStart: ZonedDateTime? = null,
		windowEnd: ZonedDateTime? = null
	): Pair<ZonedDateTime?, ZonedDateTime?> {
		val searchStartUtc = startSearch.toAstroTime()
		val zone = startSearch.zone
		val limitDays = 2.0

		val nextRise = searchRiseSet(body, obs, Direction.Rise, searchStartUtc, limitDays)
		val nextSet  = searchRiseSet(body, obs, Direction.Set , searchStartUtc, limitDays)

		val r = nextRise?.toZoned(zone)
		val s = nextSet?.toZoned(zone)

		// Filter if window is provided
		val rFiltered = if (windowStart != null && windowEnd != null) {
			r?.takeIf { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
		} else r

		val sFiltered = if (windowStart != null && windowEnd != null) {
			s?.takeIf { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
		} else s

		return rFiltered to sFiltered
	}

	/**
	 * Shared logic to compute twilight events for a specific day window.
	 */
	fun computeTwilight(startLocal: ZonedDateTime, endLocal: ZonedDateTime, obs: Observer, zoneId: ZoneId): Twilight {
		fun findAlt(direction: Direction, deg: Double): ZonedDateTime? {
			val t0 = startLocal.toAstroTime()
			val t = searchAltitude(Body.Sun, obs, direction, t0, 2.0, deg)
			return t?.toZoned(zoneId)
		}

		val searchStart = startLocal.toAstroTime()

		val sr = searchRiseSet(Body.Sun, obs, Direction.Rise, searchStart, 2.0)
		val ss = searchRiseSet(Body.Sun, obs, Direction.Set, searchStart, 2.0)

		return Twilight(
			sr?.toZoned(zoneId), ss?.toZoned(zoneId),
			findAlt(Direction.Rise, -6.0), findAlt(Direction.Set, -6.0),
			findAlt(Direction.Rise, -12.0), findAlt(Direction.Set, -12.0),
			findAlt(Direction.Rise, -18.0), findAlt(Direction.Set, -18.0)
		)
	}
}