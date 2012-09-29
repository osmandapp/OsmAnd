
/**
 * This utility class includes : 
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 *   
 *
 */
public class MapUtils_1 {
	
	private static final String BASE_SHORT_OSM_URL = "http://osm.org/go/";
	
	/**
     * This array is a lookup table that translates 6-bit positive integger
     * index values into their "Base64 Alphabet" equivalents as specified
     * in Table 1 of RFC 2045.
     */
    private static final char intToBase64[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '@'
    };

	/**
	 * Gets distance in meters
	 */
	public static double getDistance(double lat1, double lon1, double lat2, double lon2){
		double R = 6371; // km
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1); 
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * 
		        Math.sin(dLon/2) * Math.sin(dLon/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return R * c * 1000;
	}
	
	public static double checkLongitude(double longitude) {
		while (longitude < -180 || longitude > 180) {
			if (longitude < 0) {
				longitude += 360;
			} else {
				longitude -= 360;
			}
		}
		return longitude;
	}
	
	public static double checkLatitude(double latitude) {
		while (latitude < -90 || latitude > 90) {
			if (latitude < 0) {
				latitude += 180;
			} else {
				latitude -= 180;
			}
		}
		if(latitude < -85.0511) {
			return -85.0511;
 		} else if(latitude > 85.0511){
 			return 85.0511;
 		}
		return latitude;
	}
	
	public static int get31TileNumberX(double longitude){
		longitude = checkLongitude(longitude);
		long l = 1l << 31;
		return (int)((longitude + 180d)/360d * l);
	}
	public static int get31TileNumberY( double latitude){
		latitude = checkLatitude(latitude);
		double eval = Math.log( Math.tan(Math.toRadians(latitude)) + 1/Math.cos(Math.toRadians(latitude)) );
		long l = 1l << 31;
		if(eval > Math.PI){
			eval = Math.PI;
		}
		return  (int) ((1 - eval / Math.PI) / 2 * l);
	}
	
	public static double get31LongitudeX(int tileX){
		return getLongitudeFromTile(21, tileX /1024f);
	}
	
	public static double get31LatitudeY(int tileY){
		return getLatitudeFromTile(21, tileY /1024f);
	}
	
	
	
	/**
	 * 
	 * Theses methods operate with degrees (evaluating tiles & vice versa) 
	 * degree longitude measurements (-180, 180) [27.56 Minsk]
	// degree latitude measurements (90, -90) [53.9]
	 */
	
	public static double getTileNumberX(float zoom, double longitude){
		if(longitude == 180d) {
			return getPowZoom(zoom) - 1;
		}
		longitude = checkLongitude(longitude);
		return (longitude + 180d)/360d * getPowZoom(zoom);
	}
	
	public static double getTileNumberY(float zoom,  double latitude){
		latitude = checkLatitude(latitude);
		double eval = Math.log( Math.tan(Math.toRadians(latitude)) + 1/Math.cos(Math.toRadians(latitude)) );
		if (Double.isInfinite(eval) || Double.isNaN(eval)) {
			latitude = latitude < 0 ? - 89.9 : 89.9;
			eval = Math.log( Math.tan(Math.toRadians(latitude)) + 1/Math.cos(Math.toRadians(latitude)) );
		}
		double result = (1 - eval / Math.PI) / 2 * getPowZoom(zoom);
		return  result;
	}
	
	public static double getTileEllipsoidNumberY(float zoom, double latitude){
		final double E2 = (double) latitude * Math.PI / 180;
		final long sradiusa = 6378137;
		final long sradiusb = 6356752;
		final double J2 = (double) Math.sqrt(sradiusa * sradiusa - sradiusb * sradiusb)	/ sradiusa;
		final double M2 = (double) Math.log((1 + Math.sin(E2))
				/ (1 - Math.sin(E2)))/ 2- J2	* Math.log((1 + J2 * Math.sin(E2))/ (1 - J2 * Math.sin(E2))) / 2;
		final double B2 = getPowZoom(zoom);
		return B2 / 2 - M2 * B2 / 2 / Math.PI;
	}
	
	public static double getLatitudeFromEllipsoidTileY(float zoom, float tileNumberY){
		final double MerkElipsK = 0.0000001;
		final long sradiusa = 6378137;
		final long sradiusb = 6356752;
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
	
	
	public static double getLongitudeFromTile(float zoom, double x) {
		return x / getPowZoom(zoom) * 360.0 - 180.0;
	}
	
	public static double getPowZoom(float zoom){
		if(zoom >= 0 && zoom - Math.floor(zoom) < 0.05f){
			return 1 << ((int)zoom); 
		} else {
			return Math.pow(2, zoom);
		}
	}

	public static float calcDiffPixelX(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize){
		return (rotateCos * dTileX - rotateSin * dTileY) * tileSize ;
	}
	
	public static float calcDiffPixelY(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize){
		return (rotateSin * dTileX + rotateCos * dTileY) * tileSize ;
	}
	
	public static double getLatitudeFromTile(float zoom, double y){
		int sign = y < 0 ? -1 : 1;
		double result = Math.atan(sign*Math.sinh(Math.PI * (1 - 2 * y / getPowZoom(zoom)))) * 180d / Math.PI;
		return result;
	}
	
	
	public static int getPixelShiftX(int zoom, double long1, double long2, int tileSize){
		return (int) ((getTileNumberX(zoom, long1) - getTileNumberX(zoom, long2)) * tileSize);
	}
	
	
	public static int getPixelShiftY(int zoom, double lat1, double lat2, int tileSize){
		return (int) ((getTileNumberY(zoom, lat1) - getTileNumberY(zoom, lat2)) * tileSize);
	}
	
	// Examples
//	System.out.println(buildShortOsmUrl(51.51829d, 0.07347d, 16)); // http://osm.org/go/0EEQsyfu
//	System.out.println(buildShortOsmUrl(52.30103d, 4.862927d, 18)); // http://osm.org/go/0E4_JiVhs
//	System.out.println(buildShortOsmUrl(40.59d, -115.213d, 9)); // http://osm.org/go/TelHTB--
	public static String buildShortOsmUrl(double latitude, double longitude, int zoom){
		long lat = (long) (((latitude + 90d)/180d)*(1l << 32));
		long lon = (long) (((longitude + 180d)/360d)*(1l << 32));
		long code = interleaveBits(lon, lat);
		StringBuilder str = new StringBuilder(10);
		str.append(BASE_SHORT_OSM_URL);
	    // add eight to the zoom level, which approximates an accuracy of one pixel in a tile.
		for(int i=0; i< Math.ceil((zoom+8)/3d); i++){
		    str.append(intToBase64[(int) ((code >> (58 - 6 * i)) & 0x3f)]);
		}
			    // append characters onto the end of the string to represent
			    // partial zoom levels (characters themselves have a granularity of 3 zoom levels).
		for(int j=0; j< (zoom + 8) % 3 ; j++){
			str.append('-');
		}
		str.append("?m");
		return str.toString();
	}
	
	/**	
	 * interleaves the bits of two 32-bit numbers. the result is known as a Morton code.	   
	 */
	private static long interleaveBits(long x, long y){
		long c = 0;
		for(byte b = 31; b>=0; b--){
			c = (c << 1) | ((x >> b) & 1);
			c = (c << 1) | ((y >> b) & 1);
		}
		return c;
	}

	/**
	 * Calculate rotation diff D, that R (rotate) + D = T (targetRotate)
	 * D is between -180, 180 
	 * @param rotate
	 * @param targetRotate
	 * @return 
	 */
	public static float unifyRotationDiff(float rotate, float targetRotate) {
		float d = targetRotate - rotate;
		while(d >= 180){
			d -= 360;
		}
		while(d < -180){
			d += 360;
		}
		return d;
	}
	
	/**
	 * Calculate rotation diff D, that R (rotate) + D = T (targetRotate)
	 * D is between -180, 180 
	 * @param rotate
	 * @param targetRotate
	 * @return 
	 */
	public static float unifyRotationTo360(float rotate) {
		while(rotate < 0){
			rotate += 360;
		}
		while(rotate > 360){
			rotate -= 360;
		}
		return rotate;
	}

	/**
	 * @param diff align difference between 2 angles ]-PI, PI] 
	 * @return 
	 */
	public static double alignAngleDifference(double diff) {
		while(diff > Math.PI) {
			diff -= 2 * Math.PI;
		}
		while(diff <=-Math.PI) {
			diff += 2 * Math.PI;
		}
		return diff;
		
	}
	
	/**
	 * @param diff align difference between 2 angles ]-180, 180] 
	 * @return 
	 */
	public static double degreesDiff(double a1, double a2) {
		double diff = a1 - a2;
		while(diff > 180) {
			diff -= 360;
		}
		while(diff <=-180) {
			diff += 360;
		}
		return diff;
		
	}	

}


