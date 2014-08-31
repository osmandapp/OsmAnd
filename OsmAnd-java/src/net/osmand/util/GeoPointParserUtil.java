package net.osmand.util;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoPointParserUtil {

	public static void main(String[] args) {
		final double lat = 34.99, lon = -106.61;
		final String name = "Treasure";
		int z = GeoParsedPoint.NO_ZOOM;
		String url;

		// 0,0?q=34.99,-106.61(Treasure)
		url = "geo:0,0?q=" + lat + "," + lon + "(" + name + ")";
		System.out.println("url: " + url);
		GeoParsedPoint actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(lat, lon, z, name));

		// geo:0,0?z=11&q=34.99,-106.61(Treasure)
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + lat + "," + lon + "(" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(lat, lon, z, name));

		// geo:0,0?z=11&q=34.99,-106.61
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + lat + "," + lon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(lat, lon, z));

		// geo:34.99,-106.61
		z = -1;
		url = "geo:" + lat + "," + lon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(lat, lon, z));

		// geo:34.99,-106.61?z=11
		z = 11;
		url = "geo:" + lat + "," + lon + "?" + "z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(lat, lon, z));

		// geo:0,0?q=1600+Amphitheatre+Parkway,+CA
		String qstr = "q=1600+Amphitheatre+Parkway,+CA";
		url = "geo:0,0?" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
		qstr = "q=1600+Amphitheatre+Parkway,+CA";
		url = "geo:0,0?z=11&" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
	}

	private static void assertGeoPoint(GeoParsedPoint actual, GeoParsedPoint expected) {
		if (expected.getQuery() != null) {
			if (!expected.getQuery().equals(actual.getQuery()))
				throw new RuntimeException("Query param not equal");
		} else {
			double aLat = actual.getLat(), eLat = expected.getLat(), aLon = actual.getLon(), eLon = expected.getLon();
			int aZoom = actual.getZoom(), eZoom = expected.getZoom();
			String aName = actual.getName(), eName = expected.getName();
			if (eName != null) {
				if (!aName.equals(eName)) {
					throw new RuntimeException("Point name\\capture is not equal; actual=" + aName + ", expected="
							+ eName);
				}
			}
			if (eLat != aLat) {
				throw new RuntimeException("Latitude is not equal; actual=" + aLat + ", expected=" + eLat);
			}
			if (eLon != aLon) {
				throw new RuntimeException("Longitude is not equal; actual=" + aLon + ", expected=" + eLon);
			}
			if (eZoom != aZoom) {
				throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + aZoom);
			}
		}
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
	 * Parses geo and map intents:
	 * 
	 * @param scheme
	 *            The intent scheme
	 * @param data
	 *            The URI object
	 * @return {@link GeoParsedPoint}
	 */
	public static GeoParsedPoint parse(final String scheme, final String uri) {
		final URI data = URI.create(uri.replaceAll("\\s+", ""));
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

									return new GeoParsedPoint(lat, lon, zoom);
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
					return new GeoParsedPoint(llat, llon);
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
				String query = schemeSpecific.substring("0,0?".length());
				final String pattern = "(?:z=([0-9]{1,2})?)?&?q=([\\-0-9\\.]+)?,([\\-0-9\\.]+)?\\s*(?:\\((.+?)\\))?";
				final Matcher matcher = Pattern.compile(pattern).matcher(query);
				if (matcher.matches()) {
					final String z = matcher.group(1);
					final String name = matcher.group(4);
					final int zoom = z != null ? Integer.parseInt(z) : GeoParsedPoint.NO_ZOOM;
					final double lat = Double.parseDouble(matcher.group(2));
					final double lon = Double.parseDouble(matcher.group(3));
					return new GeoParsedPoint(lat, lon, zoom, name);
				} else {
					// geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA
					// geo:0,0?z=11&q=1600+Amphitheatre+Parkway%2C+CA
					// zoom parameter is not used in GeoAddressSearch
					if (query.contains("z="))
						query = query.substring(query.indexOf("&") + 1);
					return new GeoParsedPoint(query);
				}
			} else {
				// geo:47.6,-122.3
				// geo:47.6,-122.3?z=11
				// allow for http://tools.ietf.org/html/rfc5870 (geo uri) ,
				// just
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
						return new GeoParsedPoint(lat, lon);
					} else {
						return new GeoParsedPoint(lat, lon, Integer.valueOf(matcher.group(4)));
					}
				} else if (matcher2.matches()) {
					final double lat = Double.valueOf(matcher2.group(1));
					final double lon = Double.valueOf(matcher2.group(2));
					return new GeoParsedPoint(lat, lon);
				} else {
					return null;
				}
			}
		}
		return null;
	}

	public static class GeoParsedPoint {
		private static final int NO_ZOOM = -1;

		private double lat;
		private double lon;
		private int zoom = NO_ZOOM;
		private String name;
		private String query;
		private boolean geoPoint;
		private boolean geoAddress;

		public GeoParsedPoint(double lat, double lon) {
			super();
			this.lat = lat;
			this.lon = lon;
			this.geoPoint = true;
		}

		public GeoParsedPoint(double lat, double lon, String name) {
			this(lat, lon);
			this.name = name;
		}

		public GeoParsedPoint(double lat, double lon, int zoom) {
			this(lat, lon);
			this.zoom = zoom;
		}

		public GeoParsedPoint(double lat, double lon, int zoom, String name) {
			this(lat, lon, zoom);
			this.name = name;
		}

		public GeoParsedPoint(String query) {
			super();
			this.query = query;
			this.geoAddress = true;
		}

		public double getLat() {
			return lat;
		}

		public double getLon() {
			return lon;
		}

		public int getZoom() {
			return zoom;
		}

		public String getName() {
			return name;
		}

		public String getQuery() {
			return query;
		}

		public boolean isGeoPoint() {
			return geoPoint;
		}

		public boolean isGeoAddress() {
			return geoAddress;
		}
	}
}
