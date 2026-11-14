package net.osmand.plus.plugins.topoprint.compose

import net.osmand.plus.plugins.topoprint.PrintParameters

/**
 * Pure geometry of a map, shared by the render pipeline (needs the ground
 * coverage before rasterizing) and the SVG composer (needs the km grid frame).
 *
 * Mirrors `TopoMap.__init__` in topo_map_generator.py:
 * - bbox: the printed map frame on the ground (map area × 1.1 overscan)
 * - grid: bbox extended to whole kilometre lines
 * - convergence γ (tn2gn): grid north vs. true north
 */
data class PrintGeometry(
	val centerLatitude: Double,
	val centerLongitude: Double,
	val centerUtm: UtmCoordinate,
	val bbox: UtmBoundingBox,
	val grid: UtmBoundingBox,
	val convergenceDeg: Double,
	val declinationDeg: Double,
	val params: PrintParameters
) {
	/** Standard UTM latitude-band letter (e.g. 'T' for Beijing). */
	val zoneBand: Char
		get() = UtmMath.latitudeBand(centerLatitude)

	/** Zone label for printout, e.g. "50T". */
	val zoneLabel: String
		get() = "${centerUtm.zoneNumber}$zoneBand"

	/** Paper millimetres per ground meter (scale 1:N → 1000/N). */
	val kMm: Double
		get() = 1000.0 / params.scaleDenominator

	/**
	 * Ground extent (east-west, north-south) that a north-up raster must cover
	 * so it fills the rotated grid frame at every corner.
	 */
	val rotatedCoverageWidthMeters: Double
		get() {
			val gamma = Math.abs(Math.toRadians(convergenceDeg))
			val w = bbox.widthMeters.toDouble()
			val h = bbox.heightMeters.toDouble()
			return w * Math.cos(gamma) + h * Math.sin(gamma)
		}

	val rotatedCoverageHeightMeters: Double
		get() {
			val gamma = Math.abs(Math.toRadians(convergenceDeg))
			val w = bbox.widthMeters.toDouble()
			val h = bbox.heightMeters.toDouble()
			return h * Math.cos(gamma) + w * Math.sin(gamma)
		}

	companion object {
		fun compute(params: PrintParameters, latitude: Double, longitude: Double,
		            declinationDeg: Double): PrintGeometry {
			val centerUtm = UtmMath.fromLatLon(latitude, longitude)
			val bbox = UtmMath.mapFrameBoundingBox(
				centerUtm, params.layout.mapWidthMm, params.layout.mapHeightMm,
				params.scaleDenominator)
			return PrintGeometry(
				centerLatitude = latitude,
				centerLongitude = longitude,
				centerUtm = centerUtm,
				bbox = bbox,
				grid = bbox.toKilometreGrid(),
				// Numerical convergence from the bbox itself (same as the
				// Python pipeline) — consistent with the built-in UTM math.
				convergenceDeg = UtmMath.gridConvergenceDegrees(bbox),
				declinationDeg = declinationDeg,
				params = params
			)
		}
	}
}
