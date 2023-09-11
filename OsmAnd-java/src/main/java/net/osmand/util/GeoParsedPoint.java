package net.osmand.util;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class GeoParsedPoint {

	public static final int NO_ZOOM = -1;

	private double lat = 0;
	private double lon = 0;
	private int zoom = NO_ZOOM;
	private String label;
	private String query;
	private boolean geoPoint;
	private boolean geoAddress;

	public GeoParsedPoint(double lat, double lon) {
		super();
		this.lat = lat;
		this.lon = lon;
		this.geoPoint = true;
	}

	public GeoParsedPoint(double lat, double lon, String label) {
		this(lat, lon);
		if (label != null)
			this.label = label.replaceAll("\\+", " ");
	}

	public GeoParsedPoint(double lat, double lon, int zoom) {
		this(lat, lon);
		this.zoom = zoom;
	}

	public GeoParsedPoint(double lat, double lon, int zoom, String label) {
		this(lat, lon, label);
		this.zoom = zoom;
	}

	public GeoParsedPoint(String latString, String lonString, String zoomString, String label) throws NumberFormatException {
		this(latString, lonString, zoomString);
		this.label = label;
	}

	public GeoParsedPoint(String latString, String lonString, String zoomString) throws NumberFormatException {
		this(parseLat(latString), parseLon(lonString));
		this.zoom = GeoPointParserUtil.parseZoom(zoomString);
	}

	private static double parseLon(String lonString) {
		if (lonString.startsWith("E")) {
			return Double.valueOf(lonString.substring(1));
		} else if (lonString.startsWith("W")) {
			return -Double.valueOf(lonString.substring(1));
		}
		return Double.valueOf(lonString);
	}

	private static double parseLat(String latString) {
		if (latString.startsWith("S")) {
			return -Double.valueOf(latString.substring(1));
		} else if (latString.startsWith("N")) {
			return Double.valueOf(latString.substring(1));
		}
		return Double.valueOf(latString);
	}

	public GeoParsedPoint(String latString, String lonString) throws NumberFormatException {
		this(parseLat(latString), parseLon(lonString));
		this.zoom = NO_ZOOM;
	}

	/**
	 * Accepts a plain {@code String}, not URL-encoded
	 */
	public GeoParsedPoint(String query) {
		super();
		this.query = query;
		this.geoAddress = true;
	}

	public double getLatitude() {
		return lat;
	}

	public double getLongitude() {
		return lon;
	}

	public int getZoom() {
		return zoom;
	}

	public String getLabel() {
		return label;
	}

	public String getQuery() {
		return query;
	}

	public boolean isGeoPoint() {
		return geoPoint;
	}

	private String formatDouble(double d) {
		if (d == (long) d)
			return String.format(Locale.ENGLISH, "%d", (long) d);
		else
			return String.format("%s", d);
	}

	public boolean isGeoAddress() {
		return geoAddress;
	}

	/**
	 * Generates a URI string according to https://tools.ietf.org/html/rfc5870 and
	 * https://developer.android.com/guide/components/intents-common.html#Maps
	 */
	public String getGeoUriString() {
		String uriString;
		if (isGeoPoint()) {
			String latlon = formatDouble(lat) + "," + formatDouble(lon);
			uriString = "geo:" + latlon;
			LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
			if (zoom != NO_ZOOM)
				map.put("z", String.valueOf(zoom));
			if (query != null)
				map.put("q", URLEncoder.encode(query));
			if (label != null)
				if (query == null)
					map.put("q", latlon + "(" + URLEncoder.encode(label) + ")");
			if (map.size() > 0)
				uriString += "?";
			int i = 0;
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (i > 0)
					uriString += "&";
				uriString += entry.getKey() + "=" + entry.getValue();
				i++;
			}
			return uriString;
		}
		if (isGeoAddress()) {
			uriString = "geo:0,0";
			if (query != null) {
				uriString += "?";
				if (zoom != NO_ZOOM)
					uriString += "z=" + zoom + "&";
				uriString += "q=" + URLEncoder.encode(query);
			}
			return uriString;
		}
		return null;
	}

	@Override
	public String toString() {
		return isGeoPoint() ?
				String.format("GeoParsedPoint [lat=%.5f, lon=%.5f, zoom=%d, label=%s]", lat, lon, zoom, label) :
				String.format("GeoParsedPoint [query=%s]", query);
	}
}
