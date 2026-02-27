package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import java.time.Instant
import java.time.ZonedDateTime

data class AstroChartDaySamples(
	val startMillis: Long,
	val endMillis: Long,
	val sunAltitudes: DoubleArray,
	val objectAltitudes: DoubleArray,
	val objectAzimuths: DoubleArray?
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as AstroChartDaySamples

		if (startMillis != other.startMillis) return false
		if (endMillis != other.endMillis) return false
		if (!sunAltitudes.contentEquals(other.sunAltitudes)) return false
		if (!objectAltitudes.contentEquals(other.objectAltitudes)) return false
		if (!objectAzimuths.contentEquals(other.objectAzimuths)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = startMillis.hashCode()
		result = 31 * result + endMillis.hashCode()
		result = 31 * result + sunAltitudes.contentHashCode()
		result = 31 * result + objectAltitudes.contentHashCode()
		result = 31 * result + (objectAzimuths?.contentHashCode() ?: 0)
		return result
	}
}

data class AstroChartCulmination(
	val time: ZonedDateTime?,
	val altitude: Double?
)

object AstroChartMath {

	const val DAY_MINUTES: Int = 24 * 60
	const val VISIBILITY_SAMPLE_COUNT: Int = DAY_MINUTES + 1
	const val SCHEDULE_SAMPLE_STEP_MINUTES: Int = 5
	const val SCHEDULE_SAMPLE_COUNT: Int = DAY_MINUTES / SCHEDULE_SAMPLE_STEP_MINUTES + 1

	private data class HorizontalPoint(
		val altitude: Double,
		val azimuth: Double
	)

	fun computeDaySamples(
		objectToRender: SkyObject,
		observer: Observer,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime,
		sampleCount: Int,
		includeAzimuth: Boolean
	): AstroChartDaySamples {
		val safeSamples = sampleCount.coerceAtLeast(2)
		val startMillis = startLocal.toInstant().toEpochMilli()
		val endMillis = endLocal.toInstant().toEpochMilli()
		val spanMillis = (endMillis - startMillis).coerceAtLeast(1L)
		val objectAltitudes = DoubleArray(safeSamples)
		val sunAltitudes = DoubleArray(safeSamples)
		val objectAzimuths = if (includeAzimuth) DoubleArray(safeSamples) else null

		for (index in 0 until safeSamples) {
			val fraction = index.toDouble() / (safeSamples - 1).toDouble()
			val millis = startMillis + (spanMillis.toDouble() * fraction).toLong()
			val time = Instant.ofEpochMilli(millis).atZone(startLocal.zone)
			val objectHorizontal = calculateObjectHorizontal(objectToRender, time, observer)
			objectAltitudes[index] = objectHorizontal.altitude
			if (objectAzimuths != null) {
				objectAzimuths[index] = objectHorizontal.azimuth
			}
			sunAltitudes[index] = calculateBodyHorizontal(Body.Sun, time, observer).altitude
		}

		return AstroChartDaySamples(
			startMillis = startMillis,
			endMillis = endMillis,
			sunAltitudes = sunAltitudes,
			objectAltitudes = objectAltitudes,
			objectAzimuths = objectAzimuths
		)
	}

	fun findCulmination(
		obj: SkyObject,
		observer: Observer,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime
	): AstroChartCulmination {
		val coarseBest = sampleBestAltitudeTime(
			obj = obj,
			observer = observer,
			startLocal = startLocal,
			endLocal = endLocal,
			stepMinutes = CULMINATION_COARSE_STEP_MINUTES
		) ?: return AstroChartCulmination(null, null)

		var refineStart = coarseBest.minusMinutes(CULMINATION_COARSE_STEP_MINUTES)
		var refineEnd = coarseBest.plusMinutes(CULMINATION_COARSE_STEP_MINUTES)
		if (refineStart.isBefore(startLocal)) {
			refineStart = startLocal
		}
		if (refineEnd.isAfter(endLocal)) {
			refineEnd = endLocal
		}

		val fineResult = sampleBestAltitudeTime(
			obj = obj,
			observer = observer,
			startLocal = refineStart,
			endLocal = refineEnd,
			stepMinutes = CULMINATION_FINE_STEP_MINUTES
		)
		val culminationTime = fineResult ?: coarseBest
		return AstroChartCulmination(
			time = culminationTime,
			altitude = culminationTime.let { AstroUtils.altitude(obj, it, observer) }
		)
	}

	private fun sampleBestAltitudeTime(
		obj: SkyObject,
		observer: Observer,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime,
		stepMinutes: Long
	): ZonedDateTime? {
		var cursor = startLocal
		var bestTime: ZonedDateTime? = null
		var bestAltitude = Double.NEGATIVE_INFINITY
		while (!cursor.isAfter(endLocal)) {
			val altitude = AstroUtils.altitude(obj, cursor, observer)
			if (altitude > bestAltitude) {
				bestAltitude = altitude
				bestTime = cursor
			}
			cursor = cursor.plusMinutes(stepMinutes)
		}
		return bestTime
	}

	private fun calculateObjectHorizontal(
		objectToRender: SkyObject,
		time: ZonedDateTime,
		observer: Observer
	): HorizontalPoint {
		val body = objectToRender.body
		return if (body != null) {
			calculateBodyHorizontal(body, time, observer)
		} else {
			AstroUtils.withCustomStar(objectToRender.ra, objectToRender.dec) { star ->
				calculateBodyHorizontal(star, time, observer)
			}
		}
	}

	private fun calculateBodyHorizontal(
		body: Body,
		time: ZonedDateTime,
		observer: Observer
	): HorizontalPoint {
		val t = Time.fromMillisecondsSince1970(time.toInstant().toEpochMilli())
		val eq = equator(body, t, observer, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(t, observer, eq.ra, eq.dec, Refraction.Normal)
		return HorizontalPoint(altitude = hor.altitude, azimuth = normalizeAzimuth(hor.azimuth))
	}

	private fun normalizeAzimuth(value: Double): Double {
		var az = value % 360.0
		if (az < 0) {
			az += 360.0
		}
		return az
	}

	private const val CULMINATION_COARSE_STEP_MINUTES = 10L
	private const val CULMINATION_FINE_STEP_MINUTES = 1L
}

class AstroChartColorPalette private constructor(
	private val sunGt15: Int,
	private val sun6To15: Int,
	private val sun0To6: Int,
	private val sunM6To0: Int,
	private val sunM12ToM6: Int,
	private val sunLtM12: Int,
	val fillGt45: Int,
	val fill15To45: Int,
	val fill0To15: Int,
	val fillLt0: Int
) {

	fun colorForSunAltitude(altitude: Double): Int {
		return when {
			altitude >= 15.0 -> sunGt15
			altitude >= 6.0 -> sun6To15
			altitude >= 0.0 -> sun0To6
			altitude >= -6.0 -> sunM6To0
			altitude >= -12.0 -> sunM12ToM6
			else -> sunLtM12
		}
	}

	fun colorForObjectAltitude(altitude: Double): Int {
		return when {
			altitude >= 45.0 -> fillGt45
			altitude >= 15.0 -> fill15To45
			altitude >= 0.0 -> fill0To15
			else -> fillLt0
		}
	}

	fun colorForPositiveObjectAltitude(altitude: Double): Int {
		val transitionHalf = OBJECT_GRADIENT_TRANSITION_DEGREES / 2.0
		return when {
			altitude >= (45.0 + transitionHalf) -> fillGt45
			altitude >= (45.0 - transitionHalf) -> blend(
				from = fill15To45,
				to = fillGt45,
				ratio = (altitude - (45.0 - transitionHalf)) / (2.0 * transitionHalf)
			)

			altitude >= (15.0 + transitionHalf) -> fill15To45
			altitude >= (15.0 - transitionHalf) -> blend(
				from = fill0To15,
				to = fill15To45,
				ratio = (altitude - (15.0 - transitionHalf)) / (2.0 * transitionHalf)
			)

			else -> fill0To15
		}
	}

	private fun blend(from: Int, to: Int, ratio: Double): Int {
		return ColorUtils.blendARGB(from, to, ratio.coerceIn(0.0, 1.0).toFloat())
	}

	companion object {
		const val OBJECT_GRADIENT_TRANSITION_DEGREES = 15.0

		fun fromContext(context: Context): AstroChartColorPalette {
			return AstroChartColorPalette(
				sunGt15 = color(context, R.color.astro_visibility_sun_gt_15),
				sun6To15 = color(context, R.color.astro_visibility_sun_6_15),
				sun0To6 = color(context, R.color.astro_visibility_sun_0_6),
				sunM6To0 = color(context, R.color.astro_visibility_sun_m6_0),
				sunM12ToM6 = color(context, R.color.astro_visibility_sun_m12_m6),
				sunLtM12 = color(context, R.color.astro_visibility_sun_lt_m12),
				fillGt45 = color(context, R.color.astro_visibility_fill_gt_45),
				fill15To45 = color(context, R.color.astro_visibility_fill_15_45),
				fill0To15 = color(context, R.color.astro_visibility_fill_0_15),
				fillLt0 = color(context, R.color.astro_visibility_fill_lt_0)
			)
		}

		private fun color(context: Context, resId: Int): Int =
			ContextCompat.getColor(context, resId)
	}
}
