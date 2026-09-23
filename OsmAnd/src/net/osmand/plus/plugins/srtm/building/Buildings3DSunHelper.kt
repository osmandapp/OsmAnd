package net.osmand.plus.plugins.srtm.building

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import java.util.Calendar

object Buildings3DSunHelper {

	const val MINUTES_IN_DAY = 24 * 60

	class SunPosition(val azimuth: Float, val altitude: Float)

	@JvmStatic
	fun getSunPosition(lat: Double, lon: Double, timeMillis: Long): SunPosition {
		val time = Time.fromMillisecondsSince1970(timeMillis)
		val observer = Observer(lat, lon, 0.0)
		val eq = equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(time, observer, eq.ra, eq.dec, Refraction.Normal)
		return SunPosition(hor.azimuth.toFloat(), hor.altitude.toFloat())
	}

	/**
	 * Today's date at the given minute of the day in the device time zone.
	 */
	@JvmStatic
	fun getTimeOfDayMillis(minuteOfDay: Int): Long {
		val calendar = Calendar.getInstance()
		calendar.set(Calendar.HOUR_OF_DAY, 0)
		calendar.set(Calendar.MINUTE, 0)
		calendar.set(Calendar.SECOND, 0)
		calendar.set(Calendar.MILLISECOND, 0)
		return calendar.timeInMillis + minuteOfDay.coerceIn(0, MINUTES_IN_DAY - 1) * 60_000L
	}

	@JvmStatic
	fun getCurrentMinuteOfDay(): Int {
		val calendar = Calendar.getInstance()
		return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
	}

	@JvmStatic
	fun formatMinuteOfDay(minuteOfDay: Int): String =
		String.format("%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)
}
