// **********************************************************************
//
// <copyright>
//
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************
//
// Modified by OsmAnd: a Kotlin copy of com.jwetherell.openmap.common.LatLonPoint in OsmAnd-java,
// with only the parts LocationParser calls.

package net.osmand.shared.util.openmap

/** A place in decimal degrees, the latitude clipped to the poles and the longitude wrapped around. */
internal class LatLonPoint(lat: Double, lon: Double) {

	private val lat: Double = normalizeLatitude(lat)
	private val lon: Double = wrapLongitude(lon)

	/** As java's, a float. */
	val latitude: Float get() = lat.toFloat()

	/** As java's, a float. */
	val longitude: Float get() = lon.toFloat()

	companion object {
		const val NORTH_POLE = 90.0
		const val SOUTH_POLE = -NORTH_POLE
		const val DATELINE = 180.0
		const val LON_RANGE = 360.0

		/** Sets latitude to something sane: -90 to 90. */
		fun normalizeLatitude(lat: Double): Double {
			var result = lat
			if (result > NORTH_POLE) {
				result = NORTH_POLE
			}
			if (result < SOUTH_POLE) {
				result = SOUTH_POLE
			}
			return result
		}

		/** Sets longitude to something sane: -180 to 180. */
		fun wrapLongitude(lon: Double): Double {
			var result = lon
			if ((result < -DATELINE) || (result > DATELINE)) {
				result += DATELINE
				result %= LON_RANGE
				result = if (result < 0) DATELINE + result else -DATELINE + result
			}
			return result
		}
	}
}
