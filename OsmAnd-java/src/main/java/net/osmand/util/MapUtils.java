package net.osmand.util;

import static com.jwetherell.openmap.common.MoreMath.QUAD_PI_D;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * This utility class includes :
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 */
public class MapUtils {

	public static final int ROUNDING_ERROR = 3;
	private static final int EARTH_RADIUS_B = 6356752;
	private static final int EARTH_RADIUS_A = 6378137;
	public static final double MIN_LATITUDE = -85.0511;
	public static final double MAX_LATITUDE = 85.0511;
	public static final double LATITUDE_TURN = 180.0;
	public static final double MIN_LONGITUDE = -180.0;
	public static final double MAX_LONGITUDE = 180.0;
	public static final double LONGITUDE_TURN = 360.0;
	public static final double DEFAULT_LATLON_PRECISION = 0.00001;
	public static final double HIGH_LATLON_PRECISION = 0.0000001;

	// TODO change the hostname back to osm.org once HTTPS works for it
	// https://github.com/openstreetmap/operations/issues/2
	private static final String BASE_SHORT_OSM_URL = "https://openstreetmap.org/go/";

	/**
	 * This array is a lookup table that translates 6-bit positive integer
	 * index values into their "Base64 Alphabet" equivalents as specified
	 * in Table 1 of RFC 2045.
	 */
	private static final char[] intToBase64 = {
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '~'
	};
	
	public static int calculateFromBaseZoomPrecisionXY(int baseZoom, int finalZoom, int xFinal, int yFinal) {
		int px = xFinal;
		int py = yFinal;
		int precisionNumber = 1;
		for (int zoom = finalZoom - 1; zoom >= baseZoom; zoom--) {
			int x = px / 2; // (int) MapUtils.getTileNumberX(zoom, lon);
			int y = py / 2; // (int) MapUtils.getTileNumberY(zoom, lat);
			int deltax = px - x * 2;
			int deltay = py - y * 2;
			precisionNumber = (precisionNumber << 2) + (deltax << 1) + deltay;
			// StringBuilder spaces = new StringBuilder();
			// for (int i = 0; i < 32 - zoom; i++) {
			//	spaces.append(' ');
			// }
			// System.out.println(String.format("%d %s + %d %s %s + %d", zoom, Integer.toBinaryString(x), deltax, spaces.toString(), Integer.toBinaryString(y), deltay));
			px = x;
			py = y;
		}
		// System.out.println(String.format("Bits: %d %s (%d)", Integer.toBinaryString(precisionNumber).length(), Integer.toBinaryString(precisionNumber), precisionNumber));
		return precisionNumber;
	}
	
	public static int[] calculateFinalXYFromBaseAndPrecisionXY(int bazeZoom, int finalZoom,
			int precisionXY, int xBase, int yBase, boolean ignoreNotEnoughPrecision) {
		// System.out.println(String.format("Base x, y at zoom %d: %d %d", zoomToStart, xBaseApproximation, yBaseApproximation));
		// calculate finish approximation using precisionNumber
		int finalX = xBase;
		int finalY = yBase;
		int precisionCalc = precisionXY;
		for (int zoom = bazeZoom; zoom < finalZoom; zoom++) {
			if (precisionCalc <= 1 && precisionCalc > 0 && !ignoreNotEnoughPrecision) {
				 throw new IllegalArgumentException("Not enough bits to retrieve zoom approximation");
			}
			finalY = finalY * 2 + (precisionXY & 1);
			finalX = finalX * 2 + ((precisionXY & 2) >> 1);
			precisionXY = precisionXY >> 2;
		}
		// System.out.println(String.format("Calc x, y at zoom %d: %d %d", finalZoom, finalX, finalY));
		return new int[] { finalX, finalY };
	}



	public static double getDistance(LatLon l, double latitude, double longitude) {
		return getDistance(l.getLatitude(), l.getLongitude(), latitude, longitude);
	}

	public static double scalarMultiplication(double xA, double yA, double xB, double yB, double xC, double yC) {
		// Scalar multiplication between (AB, AC)
		return (xB - xA) * (xC - xA) + (yB - yA) * (yC - yA);
	}

	public static Location calculateMidPoint(Location s1, Location s2) {
		double[] latLon = calculateMidPoint(s1.getLatitude(), s1.getLongitude(), s2.getLatitude(), s2.getLongitude());
		return new Location("", latLon[0], latLon[1]);
	}

	public static LatLon calculateMidPoint(LatLon s1, LatLon s2) {
		double[] latLon = calculateMidPoint(s1.getLatitude(), s1.getLongitude(), s2.getLatitude(), s2.getLongitude());
		return new LatLon(latLon[0], latLon[1]);
	}

	public static double[] calculateMidPoint(double firstLat, double firstLon, double secondLat, double secondLon) {
		double lat1 = firstLat / 180 * Math.PI;
		double lon1 = firstLon / 180 * Math.PI;
		double lat2 = secondLat / 180 * Math.PI;
		double lon2 = secondLon / 180 * Math.PI;
		double Bx = Math.cos(lat2) * Math.cos(lon2 - lon1);
		double By = Math.cos(lat2) * Math.sin(lon2 - lon1);
		double latMid = Math.atan2(Math.sin(lat1) + Math.sin(lat2),
				Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
		double lonMid = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);
		return new double[] {MapUtils.checkLatitude(latMid * 180 / Math.PI),
				MapUtils.checkLongitude(lonMid * 180 / Math.PI)};
	}

	public static LatLon calculateIntermediatePoint(double fromLat, double fromLon, double toLat, double toLon, double coeff) {
		double lat1 = toRadians(fromLat);
		double lon1 = toRadians(fromLon);
		double lat2 = toRadians(toLat);
		double lon2 = toRadians(toLon);

		double lat1Cos = Math.cos(lat1);
		double lat2Cos = Math.cos(lat2);

		double d = 2 * Math.asin(Math.sqrt(Math.pow((Math.sin((lat1 - lat2) / 2)), 2)
					+ lat1Cos * lat2Cos * Math.pow(Math.sin((lon1 - lon2) / 2), 2)));
		double A = Math.sin((1 - coeff) * d) / Math.sin(d);
		double B = Math.sin(coeff * d) / Math.sin(d);
		double x = A * lat1Cos * Math.cos(lon1) + B * lat2Cos * Math.cos(lon2);
		double y = A * lat1Cos * Math.sin(lon1) + B * lat2Cos * Math.sin(lon2);
		double z = A * Math.sin(lat1) + B * Math.sin(lat2);

		double lat = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
		double lon = Math.atan2(y, x);
		return new LatLon(checkLatitude(lat * 180 / Math.PI), checkLongitude(lon * 180 / Math.PI));
	}

	public static double getOrthogonalDistance(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
		return getDistance(getProjection(lat, lon, fromLat, fromLon, toLat, toLon), lat, lon);
	}

	public static LatLon getProjection(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
		// not very accurate computation on sphere but for distances < 1000m it is ok
		double mDist = (fromLat - toLat) * (fromLat - toLat) + (fromLon - toLon) * (fromLon - toLon);
		double projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon);
		double prlat;
		double prlon;
		if (projection < 0) {
			prlat = fromLat;
			prlon = fromLon;
		} else if (projection >= mDist) {
			prlat = toLat;
			prlon = toLon;
		} else {
			prlat = fromLat + (toLat - fromLat) * (projection / mDist);
			prlon = fromLon + (toLon - fromLon) * (projection / mDist);
		}
		return new LatLon(prlat, prlon);
	}

	public static double getProjectionCoeff(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
		// not very accurate computation on sphere but for distances < 1000m it is ok
		double mDist = (fromLat - toLat) * (fromLat - toLat) + (fromLon - toLon) * (fromLon - toLon);
		double projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon);
		if (projection < 0) {
			return 0;
		} else if (projection >= mDist) {
			return 1;
		} else {
			return (projection / mDist);
		}
	}

	private static double toRadians(double angdeg) {
//		return Math.toRadians(angdeg);
		return angdeg / 180.0 * Math.PI;
	}

	/**
	 * Gets distance in meters
	 */
	public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double R = 6372.8; // for haversine use R = 6372.8 km instead of 6371 km
		double dLat = toRadians(lat2 - lat1);
		double dLon = toRadians(lon2 - lon1);
		double sinHalfLat = Math.sin(dLat / 2);
		double sinHalfLon = Math.sin(dLon / 2);
		double a = sinHalfLat * sinHalfLat +
				Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
						sinHalfLon * sinHalfLon;
		//double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		//return R * c * 1000;
		// simplify haversine:
		return (2 * R * 1000 * Math.asin(Math.sqrt(a)));
	}

	/**
	 * Gets distance in meters
	 */
	public static double getDistance(LatLon l1, LatLon l2) {
		return getDistance(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude());
	}

	/**
	 * Gets distance in meters
	 */
	public static double getDistance(Location l1, Location l2) {
		return getDistance(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude());
	}

	public static double checkLongitude(double longitude) {
		if (longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE) {
			return longitude;
		}
		while (longitude <= MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
			if (longitude < 0) {
				longitude += LONGITUDE_TURN;
			} else {
				longitude -= LONGITUDE_TURN;
			}
		}
		return longitude;
	}

	public static double checkLatitude(double latitude) {
		if (latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE) {
			return latitude;
		}
		while (latitude < -90 || latitude > 90) {
			if (latitude < 0) {
				latitude += LATITUDE_TURN;
			} else {
				latitude -= LATITUDE_TURN;
			}
		}
		if (latitude < MIN_LATITUDE) {
			return MIN_LATITUDE;
		} else if (latitude > MAX_LATITUDE) {
			return MAX_LATITUDE;
		}
		return latitude;
	}

	public static int get31TileNumberX(double longitude) {
		longitude = checkLongitude(longitude);
		long l = 1L << 31;
		return (int) ((longitude + 180d)/360d * l);
	}

	public static int get31TileNumberY(double latitude) {
		latitude = checkLatitude(latitude);
		double eval = Math.log(Math.tan(toRadians(latitude)) + 1/Math.cos(toRadians(latitude)));
		long l = 1L << 31;
		if (eval > Math.PI) {
			eval = Math.PI;
		}
		return (int) ((1 - eval / Math.PI) / 2 * l);
	}

	public static double get31LongitudeX(int tileX) {
		return MapUtils.getLongitudeFromTile(21, tileX / 1024.0);
	}

	public static double get31LatitudeY(int tileY) {
		return MapUtils.getLatitudeFromTile(21, tileY / 1024.0);
	}


	/**
	 * These methods operate with degrees (evaluating tiles & vice versa)
	 * degree longitude measurements (-180, 180) [27.56 Minsk]
	 * // degree latitude measurements (90, -90) [53.9]
	 */

	public static double getTileNumberX(float zoom, double longitude) {
		longitude = checkLongitude(longitude);
		final double powZoom = getPowZoom(zoom);
		double dz = (longitude + 180d)/360d * powZoom;
		if (dz >= powZoom) {
			return powZoom - 0.01;
		}
		return dz;
	}

	public static double getTileNumberY(float zoom, double latitude) {
		latitude = checkLatitude(latitude);
		double eval = Math.log(Math.tan(toRadians(latitude)) + 1/Math.cos(toRadians(latitude)));
		if (Double.isInfinite(eval) || Double.isNaN(eval)) {
			latitude = latitude < 0 ? -89.9 : 89.9;
			eval = Math.log(Math.tan(toRadians(latitude)) + 1/Math.cos(toRadians(latitude)));
		}
		return (1 - eval / Math.PI) / 2 * getPowZoom(zoom);
	}

	public static double getTileEllipsoidNumberY(float zoom, double latitude) {
		final double E2 = (double) latitude * Math.PI / 180;
		final long sradiusa = EARTH_RADIUS_A;
		final long sradiusb = EARTH_RADIUS_B;
		final double J2 = (double) Math.sqrt(sradiusa * sradiusa - sradiusb * sradiusb) / sradiusa;
		final double M2 = (double) Math.log((1 + Math.sin(E2))
				/ (1 - Math.sin(E2))) / 2 - J2 * Math.log((1 + J2 * Math.sin(E2)) / (1 - J2 * Math.sin(E2))) / 2;
		final double B2 = getPowZoom(zoom);
		return B2 / 2 - M2 * B2 / 2 / Math.PI;
	}

	public static double[] getTileEllipsoidNumberAndOffsetY(int zoom, double latitude, int tileSize) {
		final double E2 = (double) latitude * Math.PI / 180;
		final long sradiusa = EARTH_RADIUS_A;
		final long sradiusb = EARTH_RADIUS_B;
		final double J2 = (double) Math.sqrt(sradiusa * sradiusa - sradiusb * sradiusb) / sradiusa;
		final double M2 = (double) Math.log((1 + Math.sin(E2))
				/ (1 - Math.sin(E2))) / 2 - J2 * Math.log((1 + J2 * Math.sin(E2)) / (1 - J2 * Math.sin(E2))) / 2;
		final double B2 = getPowZoom(zoom);
		double tileY = B2 / 2 - M2 * B2 / 2 / Math.PI;

		double tilesCount = (double) (1 << zoom);
		double yTileNumber = Math.floor(tilesCount * ( 0.5 - M2 / 2 / Math.PI));
		double offsetY = Math.floor((tilesCount * ( 0.5 - M2 / 2 / Math.PI) - yTileNumber) * tileSize);
		return new double[]{tileY, offsetY};
	}

	public static double getLatitudeFromEllipsoidTileY(float zoom, float tileNumberY) {
		final double MerkElipsK = 0.0000001;
		final long sradiusa = EARTH_RADIUS_A;
		final long sradiusb = EARTH_RADIUS_B;
		final double FExct = (double) Math.sqrt(sradiusa * sradiusa
				- sradiusb * sradiusb)
				/ sradiusa;
		final double TilesAtZoom = getPowZoom(zoom);
		double result = (tileNumberY - TilesAtZoom / 2)
				/ -(TilesAtZoom / (2 * Math.PI));
		result = (2 * Math.atan(Math.exp(result)) - Math.PI / 2) * 180
				/ Math.PI;
		double Zu = result / (180 / Math.PI);
		double yy = (tileNumberY - TilesAtZoom / 2);

		double Zum1 = Zu;
		Zu = Math.asin(1 - ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct * Math.sin(Zum1), FExct))
				/ (Math.exp((2 * yy) / -(TilesAtZoom / (2 * Math.PI))) * Math.pow(1 + FExct * Math.sin(Zum1), FExct)));
		while (Math.abs(Zum1 - Zu) >= MerkElipsK) {
			Zum1 = Zu;
			Zu = Math.asin(1 - ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct * Math.sin(Zum1), FExct))
					/ (Math.exp((2 * yy) / -(TilesAtZoom / (2 * Math.PI))) * Math.pow(1 + FExct * Math.sin(Zum1), FExct)));
		}

		return Zu * 180 / Math.PI;
	}

	public static double getTileDistanceWidth(float zoom) {
		return getTileDistanceWidth(30, zoom);
	}
	
	public static double getTileDistanceWidth(double lat, float zoom) {
		LatLon ll = new LatLon(lat, MapUtils.getLongitudeFromTile(zoom, 0));
		LatLon ll2 = new LatLon(lat, MapUtils.getLongitudeFromTile(zoom, 1));
		return getDistance(ll, ll2);
	}

	public static double getTileDistanceHeight(double lat, float zoom) {
		// equals to getTileDistanceWidth (so no difference)
		double y = MapUtils.getTileNumberY(zoom, lat);
		LatLon ll = new LatLon(MapUtils.getLatitudeFromTile(zoom, Math.floor(y)), 0);
		LatLon ll2 = new LatLon(MapUtils.getLatitudeFromTile(zoom, Math.floor(y) + 1), 0);
		return getDistance(ll, ll2);
	}

	public static double getLongitudeFromTile(double zoom, double x) {
		return x / getPowZoom(zoom) * 360.0 - 180.0;
	}

	public static double getPowZoom(double zoom) {
		if (zoom >= 0 && zoom - Math.floor(zoom) < 0.001f) {
			return 1 << ((int) zoom);
		} else {
			return Math.pow(2, zoom);
		}
	}


	public static float calcDiffPixelX(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize) {
		return (rotateCos * dTileX - rotateSin * dTileY) * tileSize;
	}

	public static float calcDiffPixelY(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize) {
		return (rotateSin * dTileX + rotateCos * dTileY) * tileSize;
	}

	public static double getLatitudeFromTile(float zoom, double y) {
		int sign = y < 0 ? -1 : 1;
		return Math.atan(sign * Math.sinh(Math.PI * (1 - 2 * y / getPowZoom(zoom)))) * 180d / Math.PI;
	}


	public static int getPixelShiftX(float zoom, double long1, double long2, double tileSize) {
		return (int) ((getTileNumberX(zoom, long1) - getTileNumberX(zoom, long2)) * tileSize);
	}


	public static int getPixelShiftY(float zoom, double lat1, double lat2, double tileSize) {
		return (int) ((getTileNumberY(zoom, lat1) - getTileNumberY(zoom, lat2)) * tileSize);
	}


	public static void sortListOfMapObject(List<? extends MapObject> list, final double lat, final double lon) {
		Collections.sort(list, new Comparator<MapObject>() {
			@Override
			public int compare(MapObject o1, MapObject o2) {
				return Double.compare(MapUtils.getDistance(o1.getLocation(), lat, lon), MapUtils.getDistance(o2.getLocation(),
						lat, lon));
			}
		});
	}

	public static String buildGeoUrl(String latitude, String longitude, int zoom) {
		return "geo:" + latitude + "," + longitude + "?z=" + zoom;
	}

	// Examples
//	System.out.println(buildShortOsmUrl(51.51829d, 0.07347d, 16)); // https://osm.org/go/0EEQsyfu
//	System.out.println(buildShortOsmUrl(52.30103d, 4.862927d, 18)); // https://osm.org/go/0E4_JiVhs
//	System.out.println(buildShortOsmUrl(40.59d, -115.213d, 9)); // https://osm.org/go/TelHTB--
	public static String buildShortOsmUrl(double latitude, double longitude, int zoom) {
		return BASE_SHORT_OSM_URL + createShortLinkString(latitude, longitude, zoom) + "?m";
	}

	public static String createShortLinkString(double latitude, double longitude, int zoom) {
		long lat = (long) (((latitude + 90d)/180d)*(1L << 32));
		long lon = (long) (((longitude + 180d)/360d)*(1L << 32));
		long code = interleaveBits(lon, lat);
		String str = "";
		// add eight to the zoom level, which approximates an accuracy of one pixel in a tile.
		for (int i = 0; i < Math.ceil((zoom + 8) / 3d); i++) {
			str += intToBase64[(int) ((code >> (58 - 6 * i)) & 0x3f)];
		}
		// append characters onto the end of the string to represent
		// partial zoom levels (characters themselves have a granularity of 3 zoom levels).
		for (int j = 0; j < (zoom + 8) % 3; j++) {
			str += '-';
		}
		return str;
	}

	public static GeoParsedPoint decodeShortLinkString(String s) {
		// convert old shortlink format to current one
		s = s.replaceAll("@", "~");
		int i = 0;
		long x = 0;
		long y = 0;
		int z = -8;

		for (i = 0; i < s.length(); i++) {
			int digit = -1;
			char c = s.charAt(i);
			for (int j = 0; j < intToBase64.length; j++)
				if (c == intToBase64[j]) {
					digit = j;
					break;
				}
			if (digit < 0)
				break;
			if (digit < 0)
				break;
			// distribute 6 bits into x and y
			x <<= 3;
			y <<= 3;
			for (int j = 2; j >= 0; j--) {
				x |= ((digit & (1 << (j + j + 1))) == 0 ? 0 : (1 << j));
				y |= ((digit & (1 << (j + j))) == 0 ? 0 : (1 << j));
			}
			z += 3;
		}
		double lon = x * Math.pow(2, 2 - 3 * i) * 90. - 180;
		double lat = y * Math.pow(2, 2 - 3 * i) * 45. - 90;
		// adjust z
		if (i < s.length() && s.charAt(i) == '-') {
			z -= 2;
			if (i + 1 < s.length() && s.charAt(i + 1) == '-')
				z++;
		}
		return new GeoParsedPoint(lat, lon, z);
	}

	/**
	 * interleaves the bits of two 32-bit numbers. the result is known as a Morton code.
	 */
	public static long interleaveBits(long x, long y) {
		long c = 0;
		for (byte b = 31; b >= 0; b--) {
			c = (c << 1) | ((x >> b) & 1);
			c = (c << 1) | ((y >> b) & 1);
		}
		return c;
	}

	/**
	 * Calculate rotation diff D, that R (rotate) + D = T (targetRotate)
	 * D is between -180, 180
	 *
	 * @param rotate
	 * @param targetRotate
	 * @return
	 */
	public static float unifyRotationDiff(float rotate, float targetRotate) {
		float d = targetRotate - rotate;
		while (d >= 180) {
			d -= 360;
		}
		while (d < -180) {
			d += 360;
		}
		return d;
	}

	/**
	 * Calculate rotation diff D, that R (rotate) + D = T (targetRotate)
	 * D is between -180, 180
	 *
	 * @param rotate
	 * @return
	 */
	public static float unifyRotationTo360(float rotate) {
		while (rotate < -180) {
			rotate += 360;
		}
		while (rotate > +180) {
			rotate -= 360;
		}
		return rotate;
	}
	
	public static float normalizeDegrees360(float degrees) {
		while (degrees < 0.0f) {
			degrees += 360.0f;
		}
		while (degrees >= 360.0f) {
			degrees -= 360.0f;
		}
		return degrees;
	}

	/**
	 * @param diff align difference between 2 angles ]-PI, PI]
	 * @return
	 */
	public static double alignAngleDifference(double diff) {
		while (diff > Math.PI) {
			diff -= 2 * Math.PI;
		}
		while (diff <= -Math.PI) {
			diff += 2 * Math.PI;
		}
		return diff;

	}

	/**
	 * diff align difference between 2 angles [-180, 180]
	 *
	 * @return
	 */
	public static double degreesDiff(double a1, double a2) {
		double diff = a1 - a2;
		while (diff > 180) {
			diff -= 360;
		}
		while (diff <= -180) {
			diff += 360;
		}
		return diff;
	}



	
	public static QuadPointDouble getProjectionPoint31(int px, int py, int st31x, int st31y, int end31x, int end31y) {
		// st31x, st31y - A, end31x, end31y - B, px, py - C
		double tWidth = getTileWidth(py);
		// Scalar multiplication between (AB, AC)
		double projection = (end31x - st31x) * tWidth * (px - st31x) * tWidth
				+ (end31y - st31y) * tWidth * (py - st31y) * tWidth;
		double mDist = squareRootDist31(end31x, end31y, st31x, st31y);
		double pry = end31y;
		double prx = end31x;
		if (projection < 0) {
			prx = st31x;
			pry = st31y;
		} else if (projection >= mDist * mDist) {
			prx = end31x;
			pry = end31y;
		} else {
			prx = st31x + (end31x - st31x) * (projection / (mDist * mDist));
			pry = st31y + (end31y - st31y) * (projection / (mDist * mDist));
		}
		return new QuadPointDouble(prx, pry);
	}


	public static double squareRootDist31(int x1, int y1, int x2, int y2) {
		return Math.sqrt(squareDist31TileMetric(x1, y1, x2, y2));
	}

	public static double measuredDist31(int x1, int y1, int x2, int y2) {
		return getDistance(get31LatitudeY(y1), get31LongitudeX(x1), get31LatitudeY(y2), get31LongitudeX(x2));
	}

	public static final int EQUATOR = 1 << 30; 
	public static double squareDist31TileMetric(int x1, int y1, int x2, int y2) {
		boolean top1 = y1 > EQUATOR;
		boolean top2 = y2 > EQUATOR;
		if (top1 != top2 && y1 != EQUATOR && y2 != EQUATOR) {
			int mx = x1 + (int) ((x2 - x1) * (double) (EQUATOR - y1) / (y2 - y1));
			double d1 = Math.sqrt(squareDist31TileMetric(mx, EQUATOR, x2, y2));
			double d2 = Math.sqrt(squareDist31TileMetric(mx, EQUATOR, x1, y1));
			return (d1 + d2) * (d1 + d2);
		}
		// translate into meters
		int ymidx = y1 / 2 + y2 / 2;
		double tw = getTileWidth(ymidx);
		
		double dy = (y1 - y2) * tw;
		double dx = (x2 - x1) * tw;
		return dx * dx + dy * dy;
	}


	// 14 precision, gives 10x speedup, 0.02% error
	public static int PRECISION_ZOOM = 14; // 16 doesn't fit into tile int
	private static final TIntObjectHashMap<Double> DIST_CACHE = new TIntObjectHashMap<>();
	private static double getTileWidth(int y31) {
		double y = y31 / 1.0 / (1 << (31 - PRECISION_ZOOM));
		int tileY = (int) y; // width the same for all x
		double ry = y - tileY;
		Double d = null, dp = null;
		try {
			d = DIST_CACHE.get(tileY);
		} catch (RuntimeException e) {
			// parallel access crash
		}
		if (d == null) {
			synchronized (MapUtils.class) {
				d = getTileDistanceWidth(MapUtils.get31LatitudeY(tileY << (31 - PRECISION_ZOOM)), PRECISION_ZOOM) / (1 << (31 - PRECISION_ZOOM));
				DIST_CACHE.put(tileY, d);
			}
		}
		tileY = tileY + 1;
		try {
			dp = DIST_CACHE.get(tileY);
		} catch (RuntimeException e) {
			// parallel access crash
		}
		if (dp == null) {
			synchronized (MapUtils.class) {
				dp = getTileDistanceWidth(MapUtils.get31LatitudeY(tileY << (31 - PRECISION_ZOOM)), PRECISION_ZOOM) / (1 << (31 - PRECISION_ZOOM));
				DIST_CACHE.put(tileY, dp);
			}
		}
		return ry * dp + (1 - ry) * d;
	}

	public static boolean rightSide(double lat, double lon,
			double aLat, double aLon,
			double bLat, double bLon) {
		double ax = aLon - lon;
		double ay = aLat - lat;
		double bx = bLon - lon;
		double by = bLat - lat;
		double sa = ax * by - bx * ay;
		return sa < 0;
	}

	public static long deinterleaveY(long coord) {
		long x = 0;
		for (byte b = 31; b >= 0; b--) {
			x = (x << 1) | (1 & coord >> (b * 2));
		}
		return x;
	}
	
	public static long deinterleaveX(long coord) {
		long x = 0;
		for (byte b = 31; b >= 0; b--) {
			x = (x << 1) | (1 & coord >> (b * 2 + 1));
		}
		return x;
	}

	public static QuadRect calculateLatLonBbox(double latitude, double longitude, int radiusMeters) {
		int zoom = 16;
		float coeff = (float) (radiusMeters / MapUtils.getTileDistanceWidth(zoom));
		double tx = MapUtils.getTileNumberX(zoom, longitude);
		double ty = MapUtils.getTileNumberY(zoom, latitude);
		double topLeftX = Math.max(0, tx - coeff);
		double topLeftY = Math.max(0, ty - coeff);
		int max = (1 << zoom)  - 1;
		double bottomRightX = Math.min(max, tx + coeff);
		double bottomRightY = Math.min(max, ty + coeff);
		double pw = MapUtils.getPowZoom(31 - zoom);
		QuadRect rect = new QuadRect(topLeftX * pw, topLeftY * pw, bottomRightX * pw, bottomRightY * pw);
		rect.left = MapUtils.get31LongitudeX((int) rect.left);
		rect.top = MapUtils.get31LatitudeY((int) rect.top);
		rect.right = MapUtils.get31LongitudeX((int) rect.right);
		rect.bottom = MapUtils.get31LatitudeY((int) rect.bottom);
		return rect;
	}

	public static float getInterpolatedY(float x1, float y1, float x2, float y2, float x) {

		float a = y1 - y2;
		float b = x2 - x1;

		float d = -a * b;
		if (d != 0) {
			float c1 = y2 * x1 - x2 * y1;
			float c2 = x * (y2 - y1);

			return (a * (c1 - c2)) / d;
		} else {
			return y1;
		}
	}

	public static void insetLatLonRect(QuadRect r, double latitude, double longitude) {
		if (r.left == 0 && r.right == 0) {
			r.left = longitude;
			r.right = longitude;
			r.top = latitude;
			r.bottom = latitude;
		} else {
			r.left = Math.min(r.left, longitude);
			r.right = Math.max(r.right, longitude);
			r.top = Math.max(r.top, latitude);
			r.bottom = Math.min(r.bottom, latitude);
		}
	}

	public static boolean areLatLonEqual(Location l1, Location l2) {
		return l1 == null && l2 == null
				|| (l2 != null && areLatLonEqual(l1, l2.getLatitude(), l2.getLongitude()));
	}

	public static boolean areLatLonEqual(Location l1, Location l2, double precision) {
		return l1 == null && l2 == null || l1 != null && l2 != null
				&& areLatLonEqual(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude(), precision);
	}

	public static boolean areLatLonEqual(Location l, double lat, double lon) {
		return l != null && areLatLonEqual(l.getLatitude(), l.getLongitude(), lat, lon);
	}

	public static boolean areLatLonEqual(LatLon l1, LatLon l2) {
		return l1 == null && l2 == null || (l2 != null && areLatLonEqual(l1, l2.getLatitude(), l2.getLongitude()));
	}

	public static boolean areLatLonEqual(LatLon l, double lat, double lon) {
		return l != null && areLatLonEqual(l.getLatitude(), l.getLongitude(), lat, lon);
	}

	public static boolean areLatLonEqual(double lat1, double lon1, double lat2, double lon2) {
		return areLatLonEqual(lat1, lon1, lat2, lon2, DEFAULT_LATLON_PRECISION);
	}

	public static boolean areLatLonEqual(LatLon l1, LatLon l2, double precision) {
		return l1 == null && l2 == null || l1 != null && l2 != null
				&& areLatLonEqual(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude(), precision);
	}

	public static boolean areLatLonEqual(double lat1, double lon1, double lat2, double lon2, double precision) {
		return Math.abs(lat1 - lat2) < precision && Math.abs(lon1 - lon2) < precision;
	}

	public static LatLon rhumbDestinationPoint(LatLon latLon, double distance, double bearing) {
		return rhumbDestinationPoint(latLon.getLatitude(), latLon.getLongitude(), distance, bearing);
	}

	public static LatLon rhumbDestinationPoint(double lat, double lon, double distance, double bearing) {
		double radius = EARTH_RADIUS_A;

		double d = distance / radius; // angular distance in radians
		double phi1 = Math.toRadians(lat);
		double lambda1 = Math.toRadians(lon);
		double theta = Math.toRadians(bearing);

		double deltaPhi = d * Math.cos(theta);
		double phi2 = phi1 + deltaPhi;

		// check for some daft bugger going past the pole, normalise latitude if so
		// if (ABS(phi2) > M_PI_2)
		// phi2 = phi2>0 ? M_PI-phi2 : -M_PI-phi2;

		double deltaPsi = Math.log(Math.tan(phi2 / 2 + QUAD_PI_D) / Math.tan(phi1 / 2 + QUAD_PI_D));
		double q = Math.abs(deltaPsi) > 10e-12 ? deltaPhi / deltaPsi : Math.cos(phi1); // E-W course becomes incorrect
																						// with 0/0

		double deltalambda = d * Math.sin(theta) / q;
		double lambda2 = lambda1 + deltalambda;

		return new LatLon(Math.toDegrees(phi2), Math.toDegrees(lambda2));
	}

	public static double getSqrtDistance(int startX, int startY, int endX, int endY) {
		return Math.sqrt((double) (endX - startX) * (endX - startX) + (double) (endY - startY) * (endY - startY));
	}

	public static double getSqrtDistance(float startX, float startY, float endX, float endY) {
		return Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
	}

	/**
	 * convert distance to char to store in the obf file
	 *
	 * @param dist        integer distance in meters
	 * @param firstLetter first letter to start sequence
	 * @param firstDist   distance to start sequence
	 * @param mult1       first multiplier
	 * @param mult2       second multiplier
	 * @return String
	 * for firstLetter = A, firstDist = 5000, mult1 = 2, mult2 = 5 return letter will be depends on distance
	 * A <= 5 km, B <= 10 km, C <= 50 km, D <= 100 km, E <= 500 km, F <= 1000 km, G <= 5000 km, H <= 10000 km
	 */

	public static String convertDistToChar(int dist, char firstLetter, int firstDist, int mult1, int mult2) {
		int iteration = 0;
		while (dist - firstDist > 0) {
			iteration++;
			firstDist = firstDist * (iteration % 2 == 1 ? mult1 : mult2);
		}
		return String.valueOf((char) (firstLetter + iteration));
	}

	/**
	 * convert char to distance
	 *
	 * @param ch          input char
	 * @param firstLetter first letter to start sequence
	 * @param firstDist   distance to start sequence
	 * @param mult1       first multiplier
	 * @param mult2       second multiplier
	 * @return integer distance in meters
	 * for firstLetter = A, firstDist = 5000, mult1 = 2, mult2 = 5 return will be depends on input char
	 * *  A = 5000, B = 10000, C = 50000, D = 100000, E = 500000, F = 1000000, G = 5000000, H = 10000000
	 */

	public static int convertCharToDist(char ch, char firstLetter, int firstDist, int mult1, int mult2) {
		int dist = firstDist;
		for (int iteration = 1; iteration < ch - firstLetter + 1; iteration++) {
			dist = dist * (iteration % 2 == 1 ? mult1 : mult2);
		}
		return dist;
	}
}