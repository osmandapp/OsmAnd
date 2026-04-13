package net.osmand.plus;

import net.osmand.data.LatLon;

/**
 * Approximate transformations between Dutch RD (Rijksdriehoekscoordinaten, EPSG:28992)
 * and WGS84 based on formulas published by the Dutch Kadaster.
 */
public class DutchGridApproximation {

	private static final double REFERENCE_LAT = 52.15517440;
	private static final double REFERENCE_LON = 5.38720621;
	private static final double REFERENCE_X = 155000.0;
	private static final double REFERENCE_Y = 463000.0;

	private DutchGridApproximation() {
	}

	public static double[] convertWGS84ToRD(LatLon loc) {
		double dLat = 0.36 * (loc.getLatitude() - REFERENCE_LAT);
		double dLon = 0.36 * (loc.getLongitude() - REFERENCE_LON);

		double x = REFERENCE_X
				+ (190094.945 * dLon)
				+ (-11832.228 * dLat * dLon)
				+ (-114.221 * dLat * dLat * dLon)
				+ (-32.391 * Math.pow(dLon, 3))
				+ (-0.705 * dLat)
				+ (-2.340 * Math.pow(dLat, 3) * dLon)
				+ (-0.608 * dLat * Math.pow(dLon, 3))
				+ (-0.008 * dLon * dLon)
				+ (0.148 * dLat * dLat * Math.pow(dLon, 3));

		double y = REFERENCE_Y
				+ (309056.544 * dLat)
				+ (3638.893 * dLon * dLon)
				+ (73.077 * dLat * dLat)
				+ (-157.984 * dLat * dLon * dLon)
				+ (59.788 * Math.pow(dLat, 3))
				+ (0.433 * dLon)
				+ (-6.439 * dLat * dLat * dLon * dLon)
				+ (-0.032 * dLat * dLon)
				+ (0.092 * Math.pow(dLon, 4))
				+ (-0.054 * dLat * Math.pow(dLon, 4));

		return new double[] {x, y};
	}

	public static LatLon convertRDToWGS84(double easting, double northing) {
		double dX = (easting - REFERENCE_X) / 100000.0;
		double dY = (northing - REFERENCE_Y) / 100000.0;

		double latitude = REFERENCE_LAT
				+ ((3235.65389 * dY)
				+ (-32.58297 * dX * dX)
				+ (-0.2475 * dY * dY)
				+ (-0.84978 * dX * dX * dY)
				+ (-0.0655 * Math.pow(dY, 3))
				+ (-0.01709 * dX * dX * dY * dY)
				+ (-0.00738 * dX)
				+ (0.0053 * Math.pow(dX, 4))
				+ (-0.00039 * dX * dX * Math.pow(dY, 3))
				+ (0.00033 * Math.pow(dX, 4) * dY)
				+ (-0.00012 * dX * dY)) / 3600.0;

		double longitude = REFERENCE_LON
				+ ((5260.52916 * dX)
				+ (105.94684 * dX * dY)
				+ (2.45656 * dX * dY * dY)
				+ (-0.81885 * Math.pow(dX, 3))
				+ (0.05594 * dX * Math.pow(dY, 3))
				+ (-0.05607 * Math.pow(dX, 3) * dY)
				+ (0.01199 * dY)
				+ (-0.00256 * Math.pow(dX, 3) * dY * dY)
				+ (0.00128 * dX * Math.pow(dY, 4))
				+ (0.00022 * dY * dY)
				+ (-0.00022 * dX * dX)
				+ (0.00026 * Math.pow(dX, 5))) / 3600.0;

		return new LatLon(latitude, longitude);
	}
}
