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
// Modified by OsmAnd: a Kotlin copy of com.jwetherell.openmap.common.UTMPoint and Ellipsoid in
// OsmAnd-java, with only the parts LocationParser calls: from UTM to lat/lon, on WGS 84.

package net.osmand.shared.util.openmap

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** An earth model: its equatorial radius and the square of its eccentricity. */
internal class Ellipsoid(val name: String, val radius: Double, val eccsq: Double) {

	companion object {
		val WGS_84 = Ellipsoid("WGS 84", 6378137.0, 0.00669438)
	}
}

/** A point in the UTM grid: meters north and east in a zone, and the hemisphere, 'N' or 'S'. */
internal open class UTMPoint() {

	var northing = 0.0
	var easting = 0.0
	var zoneNumber = 0
	var zoneLetter = '\u0000'

	/** Throws [NumberFormatException] when [zoneLetter] is not 'N' or 'S', of either case. */
	constructor(northing: Double, easting: Double, zoneNumber: Int, zoneLetter: Char) : this() {
		this.northing = northing
		this.easting = easting
		this.zoneNumber = zoneNumber
		this.zoneLetter = checkZone(zoneLetter)
	}

	/** On a WGS 84 ellipsoid; null for a zone number out of 0 to 60. */
	open fun toLatLonPoint(): LatLonPoint? =
		UTMtoLL(Ellipsoid.WGS_84, northing, easting, zoneNumber, zoneLetter)

	companion object {

		/** java's `Math.toDegrees`. */
		private const val RADIANS_TO_DEGREES = 57.29577951308232

		/** [zone] upper case; throws [NumberFormatException] when it is not 'N' or 'S'. */
		fun checkZone(zone: Char): Char {
			val upper = zone.uppercaseChar()
			if (upper != 'N' && upper != 'S') {
				throw NumberFormatException("Invalid UTMPoint zone letter: $upper")
			}
			return upper
		}

		/**
		 * Converts UTM coords to lat/long given an ellipsoid.
		 *
		 * Equations from USGS Bulletin 1532. East longitudes are positive, west longitudes are
		 * negative. North latitudes are positive, south latitudes are negative. Only a
		 * [zoneLetter] of 'S' is the southern hemisphere. Null for a [zoneNumber] out of 0 to 60.
		 */
		fun UTMtoLL(
			ellip: Ellipsoid,
			UTMNorthing: Double,
			UTMEasting: Double,
			zoneNumber: Int,
			zoneLetter: Char
		): LatLonPoint? {
			// check the ZoneNummber is valid
			if (zoneNumber < 0 || zoneNumber > 60) {
				return null
			}

			val k0 = 0.9996
			val a = ellip.radius
			val eccSquared = ellip.eccsq
			val e1 = (1 - sqrt(1 - eccSquared)) / (1 + sqrt(1 - eccSquared))

			// remove 500,000 meter offset for longitude
			val x = UTMEasting - 500000.0
			var y = UTMNorthing

			// We must know somehow if we are in the Northern or Southern
			// hemisphere, this is the only time we use the letter So even
			// if the Zone letter isn't exactly correct it should indicate
			// the hemisphere correctly
			if (zoneLetter == 'S') {
				y -= 10000000.0 // remove 10,000,000 meter offset used for southern hemisphere
			}

			// There are 60 zones with zone 1 being at West -180 to -174
			val longOrigin = ((zoneNumber - 1) * 6 - 180 + 3).toDouble() // +3 puts origin in middle of zone

			val eccPrimeSquared = (eccSquared) / (1 - eccSquared)

			val M = y / k0
			val mu = M / (a * (1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256))

			val phi1Rad = mu + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * sin(2 * mu) + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * sin(4 * mu) +
					(151 * e1 * e1 * e1 / 96) * sin(6 * mu)

			val N1 = a / sqrt(1 - eccSquared * sin(phi1Rad) * sin(phi1Rad))
			val T1 = tan(phi1Rad) * tan(phi1Rad)
			val C1 = eccPrimeSquared * cos(phi1Rad) * cos(phi1Rad)
			val R1 = a * (1 - eccSquared) / (1 - eccSquared * sin(phi1Rad) * sin(phi1Rad)).pow(1.5)
			val D = x / (N1 * k0)

			var lat = phi1Rad -
					(N1 * tan(phi1Rad) / R1) *
					(D * D / 2 - (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * eccPrimeSquared) * D * D * D * D / 24 + (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1 - 252 *
							eccPrimeSquared - 3 * C1 * C1) *
							D * D * D * D * D * D / 720)
			lat *= RADIANS_TO_DEGREES

			var lon = (D - (1 + 2 * T1 + C1) * D * D * D / 6 + (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1 + 8 * eccPrimeSquared + 24 * T1 * T1) * D * D * D * D * D /
					120) /
					cos(phi1Rad)
			lon = longOrigin + lon * RADIANS_TO_DEGREES
			return LatLonPoint(lat, lon)
		}
	}
}
