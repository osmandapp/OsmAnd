package net.osmand.plus.plugins.topoprint.compose

import com.jwetherell.openmap.common.Ellipsoid
import com.jwetherell.openmap.common.LatLonPoint
import com.jwetherell.openmap.common.UTMPoint
import net.osmand.data.LatLon

/**
 * UTM coordinates within a single zone, used for the printed map frame and
 * the kilometre grid.
 *
 * Note: `zoneLetter` carries only the hemisphere ('N'/'S') because that is all
 * the built-in [UTMPoint] API works with; use [latitudeBand] for the standard
 * latitude-band letter shown on printed maps.
 */
data class UtmCoordinate(
	val zoneNumber: Int,
	val zoneLetter: Char,
	val easting: Double,
	val northing: Double
) {
	val isNorthern: Boolean
		get() = zoneLetter == 'N'

	/** Longitude of the zone's central meridian in degrees. */
	val centralMeridianDegrees: Double
		get() = (zoneNumber - 1) * 6.0 - 180.0 + 3.0

	val zone: String
		get() = "$zoneNumber$zoneLetter"
}

/**
 * Axis-aligned UTM rectangle in meters, mirroring `schemas/utm.py`'s
 * UTMBoundingBox. Bounds are whole meters like the desktop pipeline.
 */
data class UtmBoundingBox(
	val zoneNumber: Int,
	val zoneLetter: Char,
	val left: Long,
	val top: Long,
	val right: Long,
	val bottom: Long
) {
	/**
	 * Floating-point centre (the Python original truncates to whole meters —
	 * an intentional ≤0.5 m difference, invisible on paper).
	 */
	val center: UtmCoordinate
		get() = UtmCoordinate(
			zoneNumber, zoneLetter,
			(left + right) / 2.0,
			(top + bottom) / 2.0
		)

	val widthMeters: Long
		get() = right - left

	val heightMeters: Long
		get() = top - bottom

	/** Math.ceilDiv is only available since Android 15 (API 35); the kilometre grid must work on older runtimes too. */
	private fun ceilDiv(value: Long, divisor: Long): Long = -Math.floorDiv(-value, divisor)

	/** Extends the rectangle outwards to whole kilometre lines. */
	fun toKilometreGrid(): UtmBoundingBox = UtmBoundingBox(
		zoneNumber, zoneLetter,
		left = Math.floorDiv(left, 1000L) * 1000L,
		top = ceilDiv(top, 1000L) * 1000L,
		right = ceilDiv(right, 1000L) * 1000L,
		bottom = Math.floorDiv(bottom, 1000L) * 1000L
	)
}

/**
 * Thin facade over OsmAnd's built-in UTM implementation
 * (`com.jwetherell.openmap.common.UTMPoint`) plus the UTM grid convergence
 * formula.
 */
object UtmMath {

	fun fromLatLon(latitude: Double, longitude: Double): UtmCoordinate {
		val point = UTMPoint(LatLonPoint(latitude, longitude))
		return UtmCoordinate(point.zone_number, point.zone_letter, point.easting, point.northing)
	}

	/**
	 * Converts to UTM forcing the given zone, with the hemisphere taken from
	 * the point's own latitude.
	 *
	 * WARNING: each point gets its own hemisphere convention, so northings of
	 * points on opposite sides of the equator differ by ~10,000,000 m and
	 * must never be mixed arithmetically. For sheet-wide geometry (frames,
	 * grids, coverage checks) use [fromLatLon] with an explicit hemisphere —
	 * the centre's — so northings stay continuous across the equator.
	 */
	fun fromLatLon(latitude: Double, longitude: Double, zoneNumber: Int): UtmCoordinate {
		val point = UTMPoint.LLtoUTM(
			LatLonPoint(latitude, longitude), Ellipsoid.WGS_84, UTMPoint(),
			zoneNumber, latitude >= 0.0
		)
		return UtmCoordinate(point.zone_number, point.zone_letter, point.easting, point.northing)
	}

	/**
	 * Converts to UTM forcing both the zone and the hemisphere. A sheet that
	 * crosses the equator must use one hemisphere convention throughout, so
	 * its northings stay continuous: with the centre's hemisphere a point on
	 * the far side of the equator simply takes a negative northing (or one
	 * above 10M in the southern convention) instead of jumping 10,000 km.
	 */
	fun fromLatLon(latitude: Double, longitude: Double, zoneNumber: Int, isNorthern: Boolean): UtmCoordinate {
		val point = UTMPoint.LLtoUTM(
			LatLonPoint(latitude, longitude), Ellipsoid.WGS_84, UTMPoint(),
			zoneNumber, isNorthern
		)
		return UtmCoordinate(point.zone_number, point.zone_letter, point.easting, point.northing)
	}

	/**
	 * Converts UTM coordinates back to (latitude, longitude) in degrees within
	 * the same zone.
	 */
	fun toLatLon(coordinate: UtmCoordinate): Pair<Double, Double> {
		val point = UTMPoint(coordinate.northing, coordinate.easting, coordinate.zoneNumber, coordinate.zoneLetter)
		val latLon = point.toLatLonPoint()
		return latLon.y to latLon.x
	}

	/**
	 * UTM grid convergence γ: the angle between grid north and true north at
	 * the given position, positive to the east. The map's kilometre grid is
	 * rotated by this angle so grid lines stay parallel to the UTM axes.
	 */
	fun gridConvergenceDegrees(latitude: Double, longitude: Double, zoneNumber: Int): Double {
		val centralMeridian = (zoneNumber - 1) * 6.0 - 180.0 + 3.0
		val deltaLon = Math.toRadians(longitude - centralMeridian)
		return Math.toDegrees(Math.atan(Math.tan(deltaLon) * Math.sin(Math.toRadians(latitude))))
	}

	/**
	 * Grid convergence computed numerically exactly like `TopoMap.__init__`:
	 * take the bbox top-centre point (grid north of the centre), convert both
	 * to lat/lon through the same UTM implementation, and measure the true
	 * bearing from the centre to that point. The bearing of the grid-north
	 * direction *is* the convergence, which stays consistent with the built-in
	 * UTM math at high latitudes where the analytic approximation drifts.
	 */
	fun gridConvergenceDegrees(bbox: UtmBoundingBox): Double {
		val centerLatLon = toLatLon(bbox.center)
		val topCenter = toLatLon(
			UtmCoordinate(bbox.zoneNumber, bbox.zoneLetter, bbox.center.easting, bbox.top.toDouble())
		)
		return bearingDegrees(centerLatLon, topCenter)
	}

	/**
	 * Initial true bearing from one lat/lon to another, in degrees —
	 * atan2(E, N) of the centre→target direction.
	 */
	fun bearingDegrees(from: Pair<Double, Double>, to: Pair<Double, Double>): Double {
		val phi1 = Math.toRadians(from.first)
		val phi2 = Math.toRadians(to.first)
		val deltaLambda = Math.toRadians(to.second - from.second)
		val sinDelta = Math.sin(deltaLambda)
		val y = sinDelta * Math.cos(phi2)
		val x = Math.cos(phi1) * Math.sin(phi2) -
				Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
		return Math.toDegrees(Math.atan2(y, x))
	}

	/**
	 * Hemisphere convention for a centre latitude: 'N' for lat >= 0.0 — the
	 * same rule the openmap implementation uses for its zone letter. All
	 * sheet-wide conversions must share the centre's convention so northings
	 * stay continuous across the equator.
	 */
	fun isNorthern(latitude: Double): Boolean = latitude >= 0.0

	/**
	 * Equirectangular ground distance in meters (~111.32 km per degree,
	 * longitude scaled by cos of the target latitude) — the cheap drift
	 * metric the GL capture gates compare against their tolerances. Accurate
	 * far below the 0.2% print tolerances at the few-kilometre scales where
	 * it is used.
	 */
	fun distanceMeters(from: LatLon, to: LatLon): Double {
		val metersPerDegLat = 111320.0
		val metersPerDegLon = 111320.0 * Math.cos(Math.toRadians(to.latitude))
		return Math.hypot((from.latitude - to.latitude) * metersPerDegLat,
				(from.longitude - to.longitude) * metersPerDegLon)
	}

	/**
	 * Standard UTM latitude-band letter (C–X, no I/O) for the given latitude,
	 * e.g. "50T" for Beijing instead of the hemisphere-only "50N".
	 */
	fun latitudeBand(latitude: Double): Char {
		val bands = "CDEFGHJKLMNPQRSTUVWX"
		val index = ((latitude + 80.0) / 8.0).toInt().coerceIn(0, bands.length - 1)
		return bands[index]
	}

	/**
	 * The ground extent of the printed map frame, derived exactly like
	 * `TopoMap.__init__`: map area millimetres × 1.1 overscan, converted to
	 * meters and truncated to whole meters around the center point.
	 */
	fun mapFrameBoundingBox(coordinate: UtmCoordinate, mapWidthMm: Double, mapHeightMm: Double,
	                        scaleDenominator: Int): UtmBoundingBox {
		val widthMeters = mapWidthMm * 1.1 * scaleDenominator / 1000.0
		val heightMeters = mapHeightMm * 1.1 * scaleDenominator / 1000.0
		val left = coordinate.easting - (widthMeters / 2.0).toInt().toDouble()
		val bottom = coordinate.northing - (heightMeters / 2.0).toInt().toDouble()
		val right = Math.ceil(left + widthMeters)
		val top = Math.ceil(bottom + heightMeters)
		return UtmBoundingBox(
			coordinate.zoneNumber, coordinate.zoneLetter,
			left = left.toLong(), top = top.toLong(),
			right = right.toLong(), bottom = bottom.toLong()
		)
	}
}
