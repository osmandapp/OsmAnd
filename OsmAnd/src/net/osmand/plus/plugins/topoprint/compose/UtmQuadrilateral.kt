package net.osmand.plus.plugins.topoprint.compose

import net.osmand.data.LatLon

/**
 * A quadrilateral of four UTM coordinates sharing one zone/hemisphere
 * convention — the common shape of georeferenced rasters and map frames in
 * the print pipeline.
 *
 * The shared convention matters: northings of points converted under
 * different hemisphere conventions differ by ~10,000,000 m, and points on
 * opposite sides of a zone boundary get shifted eastings. Batch-converting
 * the corners here keeps the convention uniform by construction.
 */
class UtmQuadrilateral(
	val topLeft: UtmCoordinate,
	val topRight: UtmCoordinate,
	val bottomRight: UtmCoordinate,
	val bottomLeft: UtmCoordinate
) {
	/** Axis-aligned UTM bounds of the four corners. */
	val eastingMin: Double =
		minOf(topLeft.easting, topRight.easting, bottomRight.easting, bottomLeft.easting)
	val eastingMax: Double =
		maxOf(topLeft.easting, topRight.easting, bottomRight.easting, bottomLeft.easting)
	val northingMin: Double =
		minOf(topLeft.northing, topRight.northing, bottomRight.northing, bottomLeft.northing)
	val northingMax: Double =
		maxOf(topLeft.northing, topRight.northing, bottomRight.northing, bottomLeft.northing)

	/**
	 * Coverage width averaged over the east and west edges — the measure the
	 * GL capture calibration compares against the requested ground extent
	 * (the Mercator-north-up raster is a slightly rotated rectangle in UTM;
	 * edge averaging smooths the UTM non-linearity out).
	 */
	val averagedWidthMeters: Double
		get() = ((topRight.easting + bottomRight.easting) -
				(topLeft.easting + bottomLeft.easting)) / 2.0

	/** Coverage height averaged over the north and south edges. */
	val averagedHeightMeters: Double
		get() = ((topLeft.northing + topRight.northing) -
				(bottomLeft.northing + bottomRight.northing)) / 2.0

	/**
	 * True when this quadrilateral's axis-aligned bounds contain the frame
	 * rect E[eMin..eMax] × N[nMin..nMax] (e.g. the printed map frame inside
	 * the basemap raster). The Mercator raster corners form a slightly
	 * rotated quadrilateral in UTM; the axis-aligned bounds approximate the
	 * coverage.
	 */
	fun containsFrame(eMin: Double, eMax: Double, nMin: Double, nMax: Double): Boolean =
		eastingMin <= eMin && eastingMax >= eMax &&
				northingMin <= nMin && northingMax >= nMax

	companion object {

		/**
		 * Converts four lat/lons into the given zone and hemisphere. All
		 * corners share the convention, so northings stay continuous for
		 * extents near the zone boundary or across the equator.
		 */
		@JvmStatic
		fun fromLatLons(zone: Int, isNorthern: Boolean, topLeft: LatLon, topRight: LatLon,
		                bottomRight: LatLon, bottomLeft: LatLon): UtmQuadrilateral =
			UtmQuadrilateral(
				UtmMath.fromLatLon(topLeft.latitude, topLeft.longitude, zone, isNorthern),
				UtmMath.fromLatLon(topRight.latitude, topRight.longitude, zone, isNorthern),
				UtmMath.fromLatLon(bottomRight.latitude, bottomRight.longitude, zone, isNorthern),
				UtmMath.fromLatLon(bottomLeft.latitude, bottomLeft.longitude, zone, isNorthern))
	}
}
