package net.osmand.plus.plugins.astro.utils

import android.content.Context
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.StarObjectsViewModel
import net.osmand.plus.plugins.astro.StarWatcherSettings
import net.osmand.plus.plugins.astro.views.SkyObject
import net.osmand.plus.plugins.astro.views.StarView
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object AstroUtils {

	fun bodyName(ctx: Context, b: Body) = when (b) {
		Body.Sun -> ctx.getString(R.string.astro_name_sun)
		Body.Moon -> ctx.getString(R.string.astro_name_moon)
		Body.Mercury -> ctx.getString(R.string.astro_name_mercury)
		Body.Venus -> ctx.getString(R.string.astro_name_venus)
		Body.Mars -> ctx.getString(R.string.astro_name_mars)
		Body.Jupiter -> ctx.getString(R.string.astro_name_jupiter)
		Body.Saturn -> ctx.getString(R.string.astro_name_saturn)
		Body.Uranus -> ctx.getString(R.string.astro_name_uranus)
		Body.Neptune -> ctx.getString(R.string.astro_name_neptune)
		Body.Pluto -> ctx.getString(R.string.astro_name_pluto)
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

	fun formatLocalTime(astronomyTime: Time): String {
		val calendar = Calendar.getInstance(TimeZone.getDefault())
		calendar.timeInMillis = astronomyTime.toMillisecondsSince1970()
		return String.Companion.format(
			Locale.getDefault(), "%02d:%02d",
			calendar.get(Calendar.HOUR_OF_DAY),
			calendar.get(Calendar.MINUTE))
	}

	fun showStarMapOptionsDialog(
		context: Context,
		starView: StarView,
		starMapViewModel: StarObjectsViewModel,
		swSettings: StarWatcherSettings,
		onApply: (() -> Unit)? = null
	) {
		val config = swSettings.getStarMapConfig()

		val items = arrayOf(
			context.getString(R.string.azimuthal_grid),
			context.getString(R.string.equatorial_grid),
			context.getString(R.string.ecliptic_line),
			"Constellations",
			"Stars",
			"Galaxies",
			"Black Holes"
		)

		var tempAzimuthal = config.showAzimuthalGrid
		var tempEquatorial = config.showEquatorialGrid
		var tempEcliptic = config.showEclipticLine
		var tempConstellations = config.showConstellations
		var tempStars = config.showStars
		var tempGalaxies = config.showGalaxies
		var tempBlackHoles = config.showBlackHoles

		val checked = booleanArrayOf(
			tempAzimuthal, tempEquatorial, tempEcliptic, tempConstellations,
			tempStars, tempGalaxies, tempBlackHoles
		)

		AlertDialog.Builder(context)
			.setTitle(R.string.visible_layers_and_objects)
			.setMultiChoiceItems(items, checked) { _, which, isChecked ->
				when (which) {
					0 -> tempAzimuthal = isChecked
					1 -> tempEquatorial = isChecked
					2 -> tempEcliptic = isChecked
					3 -> tempConstellations = isChecked
					4 -> tempStars = isChecked
					5 -> tempGalaxies = isChecked
					6 -> tempBlackHoles = isChecked
				}
			}
			.setPositiveButton(R.string.shared_string_apply) { _, _ ->
				starView.showAzimuthalGrid = tempAzimuthal
				starView.showEquatorialGrid = tempEquatorial
				starView.showEclipticLine = tempEcliptic
				starView.showConstellations = tempConstellations

				starView.showStars = tempStars
				starView.showGalaxies = tempGalaxies
				starView.showBlackHoles = tempBlackHoles

				starView.updateVisibility()

				val newConfig = StarWatcherSettings.StarMapConfig(
					showAzimuthalGrid = tempAzimuthal,
					showEquatorialGrid = tempEquatorial,
					showEclipticLine = tempEcliptic,
					showConstellations = tempConstellations,
					showStars = tempStars,
					showGalaxies = tempGalaxies,
					showBlackHoles = tempBlackHoles,
					items = config.items
				)
				swSettings.setStarMapConfig(newConfig)
				onApply?.invoke()
			}
			.setNegativeButton(R.string.shared_string_cancel, null)
			.show()
	}

	// ---------- Extensions for Type Conversions ----------

	fun Time.toZoned(zoneId: ZoneId): ZonedDateTime =
		Instant.ofEpochMilli(this.toMillisecondsSince1970()).atZone(zoneId)

	fun ZonedDateTime.toAstroTime(): Time =
		Time.Companion.fromMillisecondsSince1970(this.toInstant().toEpochMilli())

	// ---------- Physics / Math Helpers ----------

	/**
	 * Executes a block of code with a temporarily defined custom star (Body.Star1).
	 * This method is synchronized to prevent race conditions when multiple threads
	 * try to define and use Body.Star1 simultaneously.
	 */
	fun <T> withCustomStar(ra: Double, dec: Double, block: (Body) -> T): T {
		synchronized(this) {
			defineStar(Body.Star1, ra, dec, 1000.0)
			return block(Body.Star1)
		}
	}

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
	 * Calculates altitude for a SkyObject. Handles custom stars safely.
	 */
	fun altitude(obj: SkyObject, tLocal: ZonedDateTime, obs: Observer): Double {
		return if (obj.body != null) {
			altitude(obj.body, tLocal, obs)
		} else {
			withCustomStar(obj.ra, obj.dec) { star ->
				altitude(star, tLocal, obs)
			}
		}
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
		val nextSet  = searchRiseSet(body, obs, Direction.Set, searchStartUtc, limitDays)

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
	 * Searches for next Rise and Set for a SkyObject. Handles custom stars safely.
	 */
	fun nextRiseSet(
		obj: SkyObject,
		startSearch: ZonedDateTime,
		obs: Observer,
		windowStart: ZonedDateTime? = null,
		windowEnd: ZonedDateTime? = null
	): Pair<ZonedDateTime?, ZonedDateTime?> {
		return if (obj.body != null) {
			nextRiseSet(obj.body, startSearch, obs, windowStart, windowEnd)
		} else {
			withCustomStar(obj.ra, obj.dec) { star ->
				nextRiseSet(star, startSearch, obs, windowStart, windowEnd)
			}
		}
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