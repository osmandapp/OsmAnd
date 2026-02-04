package net.osmand.plus.plugins.astro.utils

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
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
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.Constellation
import net.osmand.plus.plugins.astro.SkyObject
import net.osmand.plus.plugins.astro.StarWatcherSettings
import net.osmand.plus.plugins.astro.StarWatcherSettings.StarMapConfig
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

	@DrawableRes
	fun getObjectTypeIcon(type: SkyObject.Type): Int {
		return when (type) {
			SkyObject.Type.SUN -> R.drawable.ic_action_sun
			SkyObject.Type.MOON -> R.drawable.ic_action_moon
			SkyObject.Type.PLANET -> R.drawable.ic_action_ufo
			SkyObject.Type.STAR -> R.drawable.ic_action_favorite
			SkyObject.Type.GALAXY, SkyObject.Type.GALAXY_CLUSTER -> R.drawable.ic_world_globe_dark
			SkyObject.Type.NEBULA -> R.drawable.ic_action_clouds
			SkyObject.Type.BLACK_HOLE -> R.drawable.ic_action_circle
			SkyObject.Type.CONSTELLATION -> R.drawable.ic_action_celestial_path
			else -> R.drawable.ic_action_favorite
		}
	}

	fun getObjectTypeName(ctx: Context, type: SkyObject.Type): String {
		return when (type) {
			SkyObject.Type.SUN -> ctx.getString(R.string.astro_name_sun)
			SkyObject.Type.MOON -> ctx.getString(R.string.astro_name_moon)
			SkyObject.Type.PLANET -> ctx.getString(R.string.astro_planets)
			SkyObject.Type.STAR -> ctx.getString(R.string.astro_stars)
			SkyObject.Type.GALAXY -> ctx.getString(R.string.astro_galaxies)
			SkyObject.Type.NEBULA -> ctx.getString(R.string.astro_nebulae)
			SkyObject.Type.BLACK_HOLE -> ctx.getString(R.string.astro_black_holes)
			SkyObject.Type.OPEN_CLUSTER -> ctx.getString(R.string.astro_open_clusters)
			SkyObject.Type.GLOBULAR_CLUSTER -> ctx.getString(R.string.astro_globular_clusters)
			SkyObject.Type.GALAXY_CLUSTER -> ctx.getString(R.string.astro_galaxy_clusters)
			SkyObject.Type.CONSTELLATION -> ctx.getString(R.string.astro_constellations)
			else -> type.name
		}
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
		return String.format(
			Locale.getDefault(), "%02d:%02d",
			calendar.get(Calendar.HOUR_OF_DAY),
			calendar.get(Calendar.MINUTE))
	}

	// ---------- Extensions for Type Conversions ----------

	fun Time.toZoned(zoneId: ZoneId): ZonedDateTime =
		Instant.ofEpochMilli(this.toMillisecondsSince1970()).atZone(zoneId)

	fun ZonedDateTime.toAstroTime(): Time =
		Time.Companion.fromMillisecondsSince1970(this.toInstant().toEpochMilli())

	// ---------- Physics / Math Helpers ----------

	fun <T> withCustomStar(ra: Double, dec: Double, block: (Body) -> T): T {
		synchronized(this) {
			defineStar(Body.Star1, ra, dec, 1000.0)
			return block(Body.Star1)
		}
	}

	fun altitude(body: Body, tLocal: ZonedDateTime, obs: Observer): Double {
		val tUtc = tLocal.toAstroTime()
		val eq = equator(body, tUtc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(tUtc, obs, eq.ra, eq.dec, Refraction.Normal)
		return hor.altitude
	}

	fun altitude(obj: SkyObject, tLocal: ZonedDateTime, obs: Observer): Double {
		val body = obj.body
		return if (body != null) {
			altitude(body, tLocal, obs)
		} else {
			withCustomStar(obj.ra, obj.dec) { star ->
				altitude(star, tLocal, obs)
			}
		}
	}

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

		val rFiltered = if (windowStart != null && windowEnd != null) {
			r?.takeIf { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
		} else r

		val sFiltered = if (windowStart != null && windowEnd != null) {
			s?.takeIf { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
		} else s

		return rFiltered to sFiltered
	}

	fun nextRiseSet(
		obj: SkyObject,
		startSearch: ZonedDateTime,
		obs: Observer,
		windowStart: ZonedDateTime? = null,
		windowEnd: ZonedDateTime? = null
	): Pair<ZonedDateTime?, ZonedDateTime?> {
		val body = obj.body
		return if (body != null) {
			nextRiseSet(body, startSearch, obs, windowStart, windowEnd)
		} else {
			withCustomStar(obj.ra, obj.dec) { star ->
				nextRiseSet(star, startSearch, obs, windowStart, windowEnd)
			}
		}
	}

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

	fun calculateConstellationCenter(c: Constellation, skyObjectMap: Map<Int, SkyObject>): Pair<Double, Double>? {
		var sumX = 0.0
		var sumY = 0.0
		var sumZ = 0.0
		var count = 0

		val uniqueStars = mutableSetOf<Int>()
		c.lines.forEach { (id1, id2) -> uniqueStars.add(id1); uniqueStars.add(id2) }

		uniqueStars.forEach { id ->
			val star = skyObjectMap[id]
			if (star != null) {
				val raRad = Math.toRadians(star.ra * 15.0)
				val decRad = Math.toRadians(star.dec)
				sumX += cos(decRad) * cos(raRad)
				sumY += cos(decRad) * sin(raRad)
				sumZ += sin(decRad)
				count++
			}
		}

		if (count > 0) {
			val avgX = sumX / count
			val avgY = sumY / count
			val avgZ = sumZ / count
			val hyp = sqrt(avgX * avgX + avgY * avgY)
			val decRad = atan2(avgZ, hyp)
			var raRad = atan2(avgY, avgX)
			if (raRad < 0) raRad += 2 * PI
			return (Math.toDegrees(raRad) / 15.0) to Math.toDegrees(decRad)
		}
		return null
	}
}
