package net.osmand.util;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoPointParserUtil {

	public static void main(String[] args) {
		final int ilat = 34, ilon = -106;
		final double dlat = 34.99, dlon = -106.61;
		final String name = "Treasure Island";
		int z = GeoParsedPoint.NO_ZOOM;
		String url;

		// geo:34,-106
		url = "geo:" + ilat + "," + ilon;
		System.out.println("url: " + url);
		GeoParsedPoint actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon));

		// geo:34.99,-106.61
		url = "geo:" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));

		// geo:34.99,-106.61?z=11
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// geo:34.99,-106.61 (Treasure Island)
		url = "geo:" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, name));

		// geo:34.99,-106.61?z=11 (Treasure Island)
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// 0,0?q=34,-106(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + ilat + "," + ilon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z, name));

		// 0,0?q=34.99,-106.61(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:0,0?z=11&q=34.99,-106.61(Treasure Island)
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:0,0?z=11&q=34.99,-106.61
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// google calendar
		// geo:0,0?q=760 West Genesee Street Syracuse NY 13204
		String qstr = "q=760 West Genesee Street Syracuse NY 13204";
		url = "geo:0,0?" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr.replaceAll("\\s+", "+")));

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
		qstr = "q=1600+Amphitheatre+Parkway,+CA";
		url = "geo:0,0?z=11&" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("geo", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://download.osmand.net/go?lat=34&lon=-106&z=11
		url = "http://download.osmand.net/go?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://download.osmand.net/go?lat=34.99&lon=-106.61&z=11
		url = "http://download.osmand.net/go?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.org/map=11/34/-106
		url = "http://openstreetmap.org/map=" + z + "/" + ilat + "/" + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://openstreetmap.org/map=11/34.99/-106.61
		url = "http://openstreetmap.org/map=" + z + "/" + dlat + "/" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.de/zoom=11&lat=34&lon=-106
		url = "http://openstreetmap.de/zoom=" + z + "&lat=" + ilat + "&lon=" + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://openstreetmap.de/zoom=11&lat=34.99&lon=-106.61
		url = "http://openstreetmap.de/zoom=" + z + "&lat=" + dlat + "&lon=" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.de/lat=34.99&lon=-106.61&zoom=11
		url = "http://openstreetmap.de/lat=" + dlat + "&lon=" + dlon + "&zoom=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps/@34,-106,11z
		url = "http://maps.google.com/maps/@" + ilat + "," + ilon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps/@34.99,-106.61,11z
		url = "http://maps.google.com/maps/@" + dlat + "," + dlon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps/ll=34,-106,z=11
		url = "http://maps.google.com/maps/ll=" + ilat + "," + ilon + ",z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps/ll=34.99,-106.61,z=11
		url = "http://maps.google.com/maps/ll=" + dlat + "," + dlon + ",z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps/q=loc:34,-106&z=11
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps/q=loc:34.99,-106.61&z=11
		url = "http://maps.google.com/maps/q=loc:" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// whatsapp
		// http://maps.google.com/maps/q=loc:34,-106 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// whatsapp
		// http://maps.google.com/maps/q=loc:34.99,-106.61 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/q=loc:" + dlat + "," + dlon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/search/food/34,-106,14z
		url = "http://www.google.com/maps/search/food/" + ilat + "," + ilon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.google.com/maps/search/food/34.99,-106.61,14z
		url = "http://www.google.com/maps/search/food/" + dlat + "," + dlon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com?saddr=Current+Location&daddr=34,-106
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com?saddr=Current+Location&daddr=" + ilat + "," + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com?saddr=Current+Location&daddr=34.99,-106.61
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com?saddr=Current+Location&daddr=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/dir/Current+Location/34,-106
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://www.google.com/maps/dir/Current+Location/" + ilat + "," + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.google.com/maps/dir/Current+Location/34.99,-106.61
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://www.google.com/maps/dir/Current+Location/" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps?q=34,-106
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps?q=" + ilat + "," + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps?q=34.99,-106.61
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps?q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/place/760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps/place/" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.google.com/maps?q=760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps?q=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.google.com/maps?daddr=760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps?daddr=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://www.google.com/maps/dir/Current+Location/760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps/dir/Current+Location/" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.yandex.ru/?ll=34,-106&z=11
		z = 11;
		url = "http://maps.yandex.ru/?ll=" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.yandex.ru/?ll=34.99,-106.61&z=11
		z = 11;
		url = "http://maps.yandex.ru/?ll=" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse("http", url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));
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
				throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + eZoom);
			}
		}
		System.out.println("Passed!");
	}

	private static String getQueryParameter(final String param, URI data) {
		final String query = data.getQuery();
		String value = null;
		if (query != null && query.contains(param)) {
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
		final URI data = URI.create(uri.replaceAll("\\s+", "+").replaceAll("%20", "+"));
		if ("http".equals(scheme) || "https".equals(scheme)) {

			final String schemeSpecific = data.getSchemeSpecificPart();

			if (schemeSpecific == null) {
				return null;
			}

			final String[] osmandNetSite = { "//download.osmand.net/go?" };

			final String[] osmandNetPattern = { "lat=([+-]?\\d+(?:\\.\\d+)?)&lon=([+-]?\\d+(?:\\.\\d+)?)&?(z=\\d{1,2})" };

			final String[] openstreetmapOrgSite = { "//openstreetmap.org/", "//www.openstreetmap.org/" };

			final String[] openstreetmapOrgPattern = { "(?:.*)(?:map=)(\\d{1,2})/([+-]?\\d+(?:\\.\\d+)?)/([+-]?\\d+(?:\\.\\d+)?)(?:.*)" };

			final String[] openstreetmapDeSite = { "//openstreetmap.de/", "//www.openstreetmap.de/" };

			final String[] openstreetmapDePattern = {
					"(?:.*)zoom=(\\d{1,2})&lat=([+-]?\\d+(?:\\.\\d+)?)&lon=([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
					"(?:.*)lat=([+-]?\\d+(?:\\.\\d+)?)&lon=([+-]?\\d+(?:\\.\\d+)?)&?(z(?:oom)?=\\d{1,2})(?:.*)" };

			final String[] googleComSite = { "//www.google.com/maps/", "//maps.google.com/maps", "//maps.google.com" };

			final String[] googleComPattern = {
					"(?:.*)[@/]([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?),(\\d{1,2}z)(?:.*)",
					"(?:.*)ll=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.+)(z=\\d{1,2})(?:.*)",
					"(?:.*)q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)&?(z=\\d{1,2})",
					"(?:.*)(q=)([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
					"(?:.*)q=loc:([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)&?(z=\\d{1,2})(?:.*)",
					"(?:.*)(q=loc:)([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
					"(.*)daddr=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
					"(.*)/([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)" };

			final String[] yandexRuSite = { "//maps.yandex.ru/" };

			final String[] yandexRuPattern = { "(?:.*)ll=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.+)(z=\\d{1,2})(?:.*)" };

			final String sites[][] = { osmandNetSite, openstreetmapOrgSite, openstreetmapDeSite, googleComSite,
					yandexRuSite };

			final String patterns[][] = { osmandNetPattern, openstreetmapOrgPattern, openstreetmapDePattern,
					googleComPattern, yandexRuPattern };
			// search by geo coordinates
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
									int zoom;

									// check sequence of values
									if (matcher.group(3).contains("z=")) {
										lat = Double.valueOf(matcher.group(1));
										lon = Double.valueOf(matcher.group(2));
										zoom = Integer.valueOf(matcher.group(3).substring("z=".length()));
									} else if (matcher.group(3).contains("zoom=")) {
										lat = Double.valueOf(matcher.group(1));
										lon = Double.valueOf(matcher.group(2));
										zoom = Integer.valueOf(matcher.group(3).substring("zoom=".length()));
									} else if (matcher.group(3).contains("z")) {
										lat = Double.valueOf(matcher.group(1));
										lon = Double.valueOf(matcher.group(2));
										zoom = Integer.valueOf(matcher.group(3).substring(0,
												matcher.group(3).length() - 1));
									} else {
										lat = Double.valueOf(matcher.group(2));
										lon = Double.valueOf(matcher.group(3));
										try {
											zoom = Integer.valueOf(matcher.group(1));
										} catch (NumberFormatException e) {
											zoom = GeoParsedPoint.NO_ZOOM;
										}
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
			// search by address string
			final String[] googleComAddressPattern = new String[] { "(?:.*)daddr=(.*)", "q=(.*)", "(?:.*)/(.*)" };
			for (int s = 0; s < googleComSite.length; s++) {
				for (int p = 0; p < googleComAddressPattern.length; p++) {
					final String subString = schemeSpecific.substring(googleComSite[s].length());
					final Matcher matcher = Pattern.compile(googleComAddressPattern[p]).matcher(subString);
					if (matcher.matches()) {
						return new GeoParsedPoint(matcher.group(1));
					}
				}
			}
			return null;
		}
		if ("geo".equals(scheme) || "osmand.geo".equals(scheme)) {
			final String schemeSpecific = data.getSchemeSpecificPart();
			if (schemeSpecific == null) {
				return null;
			}
			if (schemeSpecific.startsWith("0,0?")) {
				// geo:0,0?q=34.99,-106.61(Treasure Island)
				// geo:0,0?z=11&q=34.99,-106.61(Treasure Island)
				String query = schemeSpecific.substring("0,0?".length());
				final String pattern = "(?:z=(\\d{1,2}))?&?q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)[\\+]?(?:\\((.+?)\\))?";
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
					if (query.contains("z="))
						query = query.substring(query.indexOf("&") + 1);
					return new GeoParsedPoint(query);
				}
			} else {
				// geo:47.6,-122.3
				// geo:47.6,-122.3?z=11 (Treasure Island)
				final String pattern = "([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:\\?z=(\\d{1,2}))?[\\+]?(?:\\((.*?)\\))?";
				final Matcher matcher = Pattern.compile(pattern).matcher(schemeSpecific);
				if (matcher.matches()) {
					final double lat = Double.valueOf(matcher.group(1));
					final double lon = Double.valueOf(matcher.group(2));
					final String name = matcher.group(4);
					int zoom = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : GeoParsedPoint.NO_ZOOM;
					if (zoom != GeoParsedPoint.NO_ZOOM) {
						return new GeoParsedPoint(lat, lon, zoom, name);
					} else {
						return new GeoParsedPoint(lat, lon, name);
					}
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
			if (name != null)
				this.name = name.replaceAll("\\+", " ");
		}

		public GeoParsedPoint(double lat, double lon, int zoom) {
			this(lat, lon);
			this.zoom = zoom;
		}

		public GeoParsedPoint(double lat, double lon, int zoom, String name) {
			this(lat, lon, name);
			this.zoom = zoom;
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

		@Override
		public String toString() {
			return isGeoPoint() ? "GeoParsedPoint [lat=" + lat + ", lon=" + lon + ", zoom=" + zoom + ", name=" + name
					+ "]" : "GeoParsedPoint [query=" + query;
		}

	}
}
