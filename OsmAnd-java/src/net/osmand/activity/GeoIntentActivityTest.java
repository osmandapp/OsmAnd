package net.osmand.activity;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.MapObject;

public class GeoIntentActivityTest {

	public static void main(String[] args) {
		GeoIntentActivityTest t = new GeoIntentActivityTest();

		final String lat = "34.99", lon = "-106.61", name = "Treasure";
		int z = ExecutionResult.NO_ZOOM;
		String url;

		// 0,0?q=34.99,-106.61(Treasure)
		url = "geo:0,0?q=" + lat + "," + lon + "(" + name + ")";
		System.out.println("url: " + url);
		MyService actual = t.extract("geo", URI.create(url));
		assertGPS(actual, lat, lon, z, name);

		// geo:0,0?z=11&q=34.99,-106.61(Treasure)
		z = 11;
		url = "geo:0,0?z=" + z + "q=" + lat + "," + lon + "(" + name + ")";
		System.out.println("url: " + url);
		actual = t.extract("geo", URI.create(url));
		assertGPS(actual, lat, lon, z, name);

		// geo:34.99,-106.61
		z = -1;
		url = "geo:" + lat + "," + lon;
		System.out.println("url: " + url);
		actual = t.extract("geo", URI.create(url));
		assertGPS(actual, lat, lon, z, null);

		// geo:34.99,-106.61?z=11
		z = 11;
		url = "geo:" + lat + "," + lon + "?" + "z=" + z;
		System.out.println("url: " + url);
		actual = t.extract("geo", URI.create(url));
		assertGPS(actual, lat, lon, z, null);

		// geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA
		String qstr = "1600+Amphitheatre+Parkway%2C+CA";
		String[] arr = new String[] { "1600 Amphitheatre Parkway", "CA" };
		url = "geo:0,0?q=" + qstr;
		System.out.println("url: " + url);
		actual = t.extract("geo", URI.create(url));
		assertGAS(actual, arr);

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway%2C+CA
		// TODO: zoom parameter is not used in GeoAddressSearch
		qstr = "q=1600+Amphitheatre+Parkway%2C+CA";
		arr = new String[] { "1600 Amphitheatre Parkway", "CA" };
		url = "geo:0,0?z=11&" + qstr;
		System.out.println("url: " + url);
		actual = t.extract("geo", URI.create(url));
		assertGAS(actual, arr);
	}

	private static void assertGPS(MyService actual, final String lat, final String lon, final int zoom,
			final String name) {
		if (!(actual instanceof GeoPointSearch)) {
			throw new RuntimeException("Actual is not of type: " + GeoPointSearch.class);
		}
		GeoPointSearch point = (GeoPointSearch) actual;
		double aLat = point.getPoint().getLocation().getLatitude();
		double aLon = point.getPoint().getLocation().getLongitude();
		int aZoom = point.getZoom();
		if (name != null) {
			String aName = point.getPoint().getName();
			if (!aName.equals(name)) {
				throw new RuntimeException("Capture is not equal; actual=" + aName + ", expected=" + name);
			}
		}

		if (Double.parseDouble(lat) != aLat) {
			throw new RuntimeException("Latitude is not equal; actual=" + aLat + ", expected=" + lat);
		}
		if (Double.parseDouble(lon) != aLon) {
			throw new RuntimeException("Longitude is not equal; actual=" + aLon + ", expected=" + lon);
		}
		if (aZoom != zoom) {
			throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + zoom);
		}

		System.out.println("Passed!");
	}

	private static void assertGAS(MyService actual, String[] q) {
		if (!(actual instanceof GeoAddressSearch)) {
			throw new RuntimeException("Actual is not of type: " + GeoAddressSearch.class);
		}
		GeoAddressSearch address = (GeoAddressSearch) actual;
		if (!Arrays.equals(q, address.getElements().toArray()))
			throw new RuntimeException("Query param arrays not equal");

		System.out.println("Passed!");
	}

	private static String getQueryParameter(final String param, URI data) {
		final String query = data.getQuery();
		String value = null;
		if (query.contains(param)) {
			String[] params = query.split("&");
			for (String p : params) {
				if (p.contains(param)) {
					value = p.substring(p.indexOf("=") + 1, p.length());
					break;
				}
			}
		}
		return value;
	}

	/**
	 * Extracts information from geo and map intents:
	 * 
	 * @param scheme
	 *            The intent scheme
	 * @param data
	 *            The intent uri
	 * @return
	 */
	private MyService extract(final String scheme, final URI data) {
		if ("http".equals(scheme) || "https".equals(scheme)) {

			final String schemeSpecific = data.getSchemeSpecificPart();

			if (schemeSpecific == null) {
				return null;
			}

			final String[] osmandNetSite = { "//download.osmand.net/go?" };

			final String[] osmandNetPattern = { "lat=(-?\\d{1,3}.\\d+)&lon=(-?\\d{1,3}.\\d+)&z=(\\d{1,2})" };

			final String[] openstreetmapOrgSite = { "//openstreetmap.org/", "//www.openstreetmap.org/" };

			final String[] openstreetmapOrgPattern = { "(?:.*)(?:map=)(\\d{1,2})/(-?\\d{1,3}.\\d+)/(-?\\d{1,3}.\\d+)(?:.*)" };

			final String[] openstreetmapDeSite = { "//openstreetmap.de/", "//www.openstreetmap.de/" };

			final String[] openstreetmapDePattern = {
					"(?:.*)zoom=(\\d{1,2})&lat=(-?\\d{1,3}.\\d+)&lon=(-?\\d{1,3}.\\d+)(?:.*)",
					"(?:.*)lat=(-?\\d{1,3}.\\d+)&lon=(-?\\d{1,3}.\\d+)&z(?:oom)?=(\\d{1,2})(?:.*)" };

			final String[] googleComSite = { "//www.google.com/maps/", "//maps.google.com/maps" };

			final String[] googleComPattern = { "(?:.*)@(-?\\d{1,3}.\\d+),(-?\\d{1,3}.\\d+),(\\d{1,2})z(?:.*)",
					"(?:.*)ll=(-?\\d{1,3}.\\d+),(-?\\d{1,3}.\\d+)(?:.+)z=(\\d{1,2})(?:.*)",
					"(?:.*)q=([\\-+]?\\d{1,3}.\\d+),([\\-+]?\\d{1,3}.\\d+)(?:.*)&z=(\\d{1,2})",
					"(?:.*)q=loc:(-?\\d{1,3}.\\d+),(-?\\d{1,3}.\\d+)&z=(\\d{1,2})(?:.*)" };

			final String[] yandexRuSite = { "//maps.yandex.ru/" };

			final String[] yandexRuPattern = { "(?:.*)ll=(-?\\d{1,3}.\\d+),(-?\\d{1,3}.\\d+)(?:.+)z=(\\d{1,2})(?:.*)" };

			final String sites[][] = { osmandNetSite, openstreetmapOrgSite, openstreetmapDeSite, googleComSite,
					yandexRuSite };

			final String patterns[][] = { osmandNetPattern, openstreetmapOrgPattern, openstreetmapDePattern,
					googleComPattern, yandexRuPattern };

			for (int s = 0; s < sites.length; s++) {
				for (int si = 0; si < sites[s].length; si++) {
					if (schemeSpecific.startsWith(sites[s][si])) {
						for (int p = 0; p < patterns[s].length; p++) {
							String subString = schemeSpecific.substring(sites[s][si].length());

							if (subString.equals("")) {
								subString = data.getFragment();
							}

							final Matcher matcher = Pattern.compile(patterns[s][p]).matcher(subString);

							if (matcher.matches()) {
								try {

									final double lat;
									final double lon;
									final int zoom;

									// check sequence of values
									if (!matcher.group(3).contains(".")) {
										lat = Double.valueOf(matcher.group(1));
										lon = Double.valueOf(matcher.group(2));
										zoom = Integer.valueOf(matcher.group(3));
									} else {
										zoom = Integer.valueOf(matcher.group(1));
										lat = Double.valueOf(matcher.group(2));
										lon = Double.valueOf(matcher.group(3));
									}

									return new GeoPointSearch(lat, lon, zoom);
								} catch (NumberFormatException e) {
									return null;
								}
							}
						}
						break;
					}
				}
			}

			String q = null;
			String parameter = getQueryParameter("q", data);
			if (parameter == null) {
				parameter = getQueryParameter("daddr", data);
			}
			if (parameter != null) {
				q = parameter.split(" ")[0];
			}
			if (q.indexOf(',') != -1) {
				int i = q.indexOf(',');
				String lat = q.substring(0, i);
				String lon = q.substring(i + 1);
				if (lat.indexOf(':') != -1) {
					i = lat.indexOf(':');
					lat = lat.substring(i + 1);
				}
				try {
					double llat = Double.parseDouble(lat.trim());
					double llon = Double.parseDouble(lon.trim());
					return new GeoPointSearch(llat, llon);
				} catch (NumberFormatException e) {
					return null;
				}
			} else {
				return null;
			}
		}
		if ("geo".equals(scheme) || "osmand.geo".equals(scheme)) {
			final String schemeSpecific = data.getSchemeSpecificPart();
			if (schemeSpecific == null) {
				return null;
			}
			if (schemeSpecific.startsWith("0,0?")) {
				// geo:0,0?q=34.99,-106.61(Treasure)
				// geo:0,0?z=11&q=34.99,-106.61(Treasure)
				// geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA
				// geo:0,0?z=11&q=1600+Amphitheatre+Parkway%2C+CA
				final String pattern = "(z=[0-9]{1,2})?&?q=([\\-0-9\\.]+)?,([\\-0-9\\.]+)?\\s*\\((.+?)\\)";
				final String query = schemeSpecific.substring("0,0?".length());

				final Matcher matcher = Pattern.compile(pattern).matcher(query);
				if (matcher.matches()) {
					String zg = matcher.group(1);
					String zn = matcher.group(4);
					final int zoom = zg != null ? Integer.parseInt(zg.substring("z=".length())) : -1;
					final String name = zn;
					final double lat = Double.parseDouble(matcher.group(2));
					final double lon = Double.parseDouble(matcher.group(3));
					if (zoom != -1) {
						return new GeoPointSearch(lat, lon, name, zoom);
					} else {
						return new GeoPointSearch(lat, lon, name);
					}
				} else {
					// we suppose it's a search
					return new GeoAddressSearch(query);
				}
			} else {
				// geo:47.6,-122.3
				// geo:47.6,-122.3?z=11
				// allow for http://tools.ietf.org/html/rfc5870 (geo uri) , just
				// ignore everything after ';'
				final String pattern = "([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?z=([0-9]+))?(?:;.*)?";
				int indexQ = schemeSpecific.indexOf("&q");
				final Matcher matcher;
				if (indexQ != -1) {
					final String schemeQ = schemeSpecific.substring(0, indexQ);
					matcher = Pattern.compile(pattern).matcher(schemeQ);
				} else {
					matcher = Pattern.compile(pattern).matcher(schemeSpecific);
				}

				final String pattern2 = "([\\-0-9.]+),([\\-0-9.]+)(?:.*)"; // c:geo
				final Matcher matcher2 = Pattern.compile(pattern2).matcher(schemeSpecific);

				if (matcher.matches()) {
					final double lat = Double.valueOf(matcher.group(1));
					final double lon = Double.valueOf(matcher.group(2));
					if (matcher.group(4) == null) {
						return new GeoPointSearch(lat, lon);
					} else {
						return new GeoPointSearch(lat, lon, Integer.valueOf(matcher.group(4)));
					}
				} else if (matcher2.matches()) {
					final double lat = Double.valueOf(matcher2.group(1));
					final double lon = Double.valueOf(matcher2.group(2));
					return new GeoPointSearch(lat, lon);
				} else {
					return null;
				}
			}
		}
		return null;
	}

	private final class GeoAddressSearch implements MyService {
		private List<String> elements;

		public GeoAddressSearch(String query) {
			query = query.replaceAll("%20", ",").replaceAll("%0A", ",").replaceAll("\n", ",").replaceAll("\t", ",")
					.replaceAll(" ", ",");
			// String is split on each comma
			String[] s = query.substring(query.indexOf("q=") + 2).split(",");

			elements = new ArrayList<String>();
			for (int i = 0; i < s.length; i++) {
				if (s[i].isEmpty()) {
					continue;
				}
				elements.add(s[i].replace('+', ' ').trim());
			}
		}

		public MapObject checkGeoPoint() {
			return null;
		}

		@Override
		public ExecutionResult execute() {
			return null;
		}

		public List<String> getElements() {
			return elements;
		}
	}

	private static class GeoPointSearch implements MyService {
		private final MapObject point;
		private final int zoom;

		public GeoPointSearch(double lat, double lon) {
			this(lat, lon, ExecutionResult.NO_ZOOM);
		}

		public GeoPointSearch(double lat, double lon, int zoom) {
			this(lat, lon, "Lat: " + lat + ",Lon: " + lon, zoom);
		}

		public GeoPointSearch(double lat, double lon, String name) {
			this(lat, lon, name, ExecutionResult.NO_ZOOM);
		}

		public GeoPointSearch(double lat, double lon, String name, int zoom) {
			final Amenity amenity = new Amenity();
			amenity.setLocation(lat, lon);
			amenity.setName(name);
			amenity.setType(AmenityType.USER_DEFINED);
			amenity.setSubType("");

			this.point = amenity;
			this.zoom = zoom;
		}

		@Override
		public ExecutionResult execute() {
			return null;
		}

		public MapObject getPoint() {
			return point;
		}

		public int getZoom() {
			return zoom;
		}
	}

	private static class ExecutionResult {
		public static final int NO_ZOOM = -1;
	}

	private static interface MyService {
		public ExecutionResult execute();
	}

}
