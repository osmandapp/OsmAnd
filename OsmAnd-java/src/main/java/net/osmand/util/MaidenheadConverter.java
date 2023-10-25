package net.osmand.util;

import net.osmand.data.LatLon;

/**
 * <p>Maidenhead locator conversion utilities.</p>
 * <p>For more information about the Maidenhead Locator System,
 * see <a href="https://en.wikipedia.org/wiki/Maidenhead_Locator_System">Wikipedia:Maidenhead Locator System</a>.</p>
 * <p>Their format is as follows: AA00AA... (alternating pairs of letters and numbers)<br>
 * where the first character of a pair designates longitude and the second latitude.
 * The first pair has a range of A..R, numbers are always 0..9, and subsequent letters are A..X.
 * Each pair increases precision by the number of subdivisions at that step.</p>
 * <p>These locators are commonly used by radio amateurs to express geographical locations of
 * radio stations. 3 pairs are commonly used, resulting in a precision of 2.5' latitude and
 * 5' longitude, which corresponds with a maximum grid distance of 10.4km.</p>
 * <p>The maidenheadToLatLon function will parse as many pairs as possible, and
 * latLonToMaidenhead accepts a second argument to express the number of groups desired.
 * There's also a latLonToMaidenhead variant that generates the default precision of 3 pairs.</p>
 * <p>The grid is based on WGS-84, so you need to make sure that latitude and longitude are
 * specified correctly. No correction is done to the coordinates, except for aligning them to the
 * Maidenhead grid, which starts at the South pole (90° South / -90°) and at the
 * Greenwich antimeridian (180° West / -180°).</p>
 */
public class MaidenheadConverter {
	private final static int[] LAT_DIVISIONS_STEP = {18, 10, 24, 10, 24, 10, 24, 10, 24, 10};
	private final static double[] LAT_DIVISIONS_TOTAL = new double[LAT_DIVISIONS_STEP.length];
	private final static char[] BASE = {'A', '0', 'A', '0', 'A', '0', 'A', '0', 'A', '0'};

	static {
		double div = 1.0;
		for (int i = 0; i < LAT_DIVISIONS_STEP.length; i++) {
			div *= LAT_DIVISIONS_STEP[i];
			LAT_DIVISIONS_TOTAL[i] = div;
		}
	}

	/**
	 * <p>Convert a Maidenhead locator into matching WGS-84 grid coordinates.</p>
	 * <p>The chosen coordinate pair is at the lower (Southern/Western) corner of the grid square.</p>
	 * <p>Locators with invalid characters are rejected and will throw an IllegalArgumentException.
	 * If the locator has an odd number of characters, the extra character at the end will be ignored.
	 * Both lower case and upper case letters are accepted.</p>
	 * @param maidenhead a Maidenhead locator
	 * @return a WGS-84 coordinate pair
	 * @exception IllegalArgumentException if the locator cannot be parsed.
	 */
	public static LatLon maidenheadToLatLon(String maidenhead) {
		// enforce upper case notation - we don't really case which one is used
		char[] loc = maidenhead.toUpperCase().toCharArray();
		int pairs = loc.length / 2;
		double lat = 0.0;
		double lon = 0.0;
		double xscale = 360.0;
		double yscale = 180.0;
		for (int i = 0; i < pairs; i++) {
			double div = LAT_DIVISIONS_STEP[i];
			double x = (loc[i * 2] - BASE[i]) / div;
			if (x < 0.0 || x >= 1.0) {
				throw new IllegalArgumentException("Invalid range for longitude component");
			}
			double y = (loc[i * 2 + 1] - BASE[i]) / div;
			if (y < 0.0 || y >= 1.0) {
				throw new IllegalArgumentException("Invalid range for latitude component");
			}
			lon += x * xscale;
			lat += y * yscale;
			xscale /= div;
			yscale /= div;
		}
		// apply MH easting and northing
		return new LatLon(lat - 90.0, lon - 180.0);
	}

	/**
	 * Convert WGS-83 coordinates into the corresponding Maidenhead grid square with standard precision (3 pairs).
	 * @param latlon a WGS-83 coordinate pair. Latitude must be in range -90° to 90° (inclusive), and longitude
	 *               must be from -180° to 180° (inclusive). Both extremes will be mapped to the bottom value.
	 * @return a Maidenhead locator in standard notation (upper case letters and numbers) with 3 pairs.
	 */
	public static String latLonToMaidenhead(LatLon latlon) {
		return latLonToMaidenhead(latlon, 3);
	}

	/**
	 * Convert WGS-83 coordinates into the corresponding Maidenhead grid square.
	 * The precision can be specified with the pairs parameter.
	 * @param latlon a WGS-83 coordinate pair. Latitude must be in range -90° to 90° (inclusive), and longitude
	 *               must be from -180° to 180° (inclusive). Both extremes will be mapped to the bottom value.
	 * @param pairs the number of coordinate pairs to generate. This determines the precision of the locator.
	 * @return a Maidenhead locator in standard notation (upper case letters and numbers)
	 *         with the specified number of pairs.
	 */
	public static String latLonToMaidenhead(LatLon latlon, int pairs) {
		if (pairs < 1 || pairs > LAT_DIVISIONS_TOTAL.length) {
			throw new IllegalArgumentException("Invalid number of pairs");
		}
		// apply MH easting by 180° and scale to 0..1
		// we accept both extreme values: -180.0° is the bottom edge, and +180.0° will be wrapped around to 0.0
		double lon = (latlon.getLongitude() + 180.0) / 360.0;
		if (lon < 0.0 || lon > 1.0) {
			throw new IllegalArgumentException("Invalid range for longitude");
		}
		// apply MH northing by 90° and scale to 0..1
		// we accept both extreme values: -90.0° is the bottom edge, and +90.0° will be wrapped around to -90.0
		double lat = (latlon.getLatitude() + 90.0) / 180.0;
		if (lat < 0.0 || lat > 1.0) {
			throw new IllegalArgumentException("Invalid range for latitude");
		}
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < pairs; i++) {
			double div = LAT_DIVISIONS_STEP[i];
			lon = lon % 1.0 * div;
			lat = lat % 1.0 * div;
			ret.append((char) ((int) lon + BASE[i]));
			ret.append((char) ((int) lat + BASE[i]));
		}
		return ret.toString();
	}

	/**
	 * Get the size of a grid square for a specific locator.
	 * This is equivalent to the precision of the locator.
	 * @param locator the locator to measure.
	 * @return the size of a single grid square in degrees latitude and longitude.
	 */
	public static LatLon precision(String locator) {
		int pairs = locator.length() / 2;
		return precision(pairs);
	}

	/**
	 * Get the size of a grid square for a specific number of pairs in a locator.
	 * This is equivalent to the precision of the locator.
	 * @param pairs the size of the locator (number of pairs).
	 * @return the size of a single grid square in degrees latitude and longitude.
	 */
	public static LatLon precision(int pairs) {
		if (pairs < 1 || pairs > LAT_DIVISIONS_TOTAL.length) {
			throw new IllegalArgumentException("Invalid number of pairs");
		}
		return new LatLon(180.0 / LAT_DIVISIONS_TOTAL[pairs - 1], 360.0 / LAT_DIVISIONS_TOTAL[pairs - 1]);
	}
}
