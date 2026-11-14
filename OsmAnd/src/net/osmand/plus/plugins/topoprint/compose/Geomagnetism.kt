package net.osmand.plus.plugins.topoprint.compose

import android.hardware.GeomagneticField

/**
 * Magnetic declination lookup backed by the platform's built-in WMM model
 * ([GeomagneticField]). Zero extra dependencies and fully offline. Accuracy is
 * within a few tenths of a degree of the high-resolution WMMHR2025 model used
 * by the desktop pipeline — sufficient for the map's declination legend.
 */
object Geomagnetism {

	/**
	 * Magnetic declination (magnetic variation) in degrees at the given
	 * location and time; positive values mean magnetic north is east of true
	 * north.
	 *
	 * @param altitudeMeters altitude above mean sea level
	 * @param timeMillis     epoch milliseconds of the observation
	 */
	fun declinationDegrees(latitude: Double, longitude: Double, altitudeMeters: Double, timeMillis: Long): Double {
		val field = GeomagneticField(
			latitude.toFloat(), longitude.toFloat(), altitudeMeters.toFloat(), timeMillis)
		return field.declination.toDouble()
	}
}
