package net.osmand.util;

import net.osmand.data.LatLon;

import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoPointParserUtil {


	private static String getQueryParameter(final String param, URI uri) {
		final String query = uri.getQuery();
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
	 * This parses out all of the parameters in the query string for both
	 * http: and geo: URIs.  This will only work on URIs with valid syntax, so
	 * it will not work on URIs that do odd things like have a query string in
	 * the fragment, like this one:
	 * http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000
	 *
	 * @param uri
	 * @return {@link Map<String, String>} a Map of the query parameters
	 */
	static Map<String, String> getQueryParameters(URI uri) {
		String query = null;
		if (uri.isOpaque()) {
			String schemeSpecificPart = uri.getSchemeSpecificPart();
			int pos = schemeSpecificPart.indexOf("?");
			if (pos == schemeSpecificPart.length()) {
				query = "";
			} else if (pos > -1) {
				query = schemeSpecificPart.substring(pos + 1);
			}
		} else {
			query = uri.getRawQuery();
		}
		return getQueryParameters(query);
	}

	private static Map<String, String> getQueryParameters(String query) {
		final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		if (query != null && !query.isEmpty()) {
			String[] params = query.split("[&/]");
			for (String p : params) {
				String[] keyValue = p.split("=");
				if (keyValue.length == 1)
					map.put(keyValue[0], "");
				else if (keyValue.length > 1)
					map.put(keyValue[0], URLDecoder.decode(keyValue[1]));
			}
		}
		return map;
	}

	private static int kMaxPointBytes = 10;
	private static int kMaxCoordBits = kMaxPointBytes * 3;

	public static LatLon decodeMapsMeLatLonToInt(String s) {
		// 44TvlEGXf-
		int lat = 0, lon = 0;
		int shift = kMaxCoordBits - 3;
		for (int i = 0; i < s.length(); ++i, shift -= 3) {
			int a = net.osmand.osm.io.Base64.indexOf(s.charAt(i));
			if (a < 0)
				return null;

			int lat1 = (((a >> 5) & 1) << 2 | ((a >> 3) & 1) << 1 | ((a >> 1) & 1));
			int lon1 = (((a >> 4) & 1) << 2 | ((a >> 2) & 1) << 1 | (a & 1));
			lat |= lat1 << shift;
			lon |= lon1 << shift;
		}
		double middleOfSquare = 1 << (3 * (kMaxPointBytes - s.length()) - 1);
		lat += middleOfSquare;
		lon += middleOfSquare;

		double dlat = ((double) lat) / ((1 << kMaxCoordBits) - 1) * 180 - 90;
		double dlon = ((double) lon) / ((1 << kMaxCoordBits) - 1 + 1) * 360.0 - 180;
		return new LatLon(dlat, dlon);
	}

	/**
	 * Parses geo and map intents:
	 *
	 * @param uriString The URI as a String
	 * @return {@link GeoParsedPoint}
	 */
	public static GeoParsedPoint parse(String uriString) {
		List<GeoParsedPoint> points = parsePoints(uriString);
		if (!Algorithms.isEmpty(points)) {
			return points.get(0);
		}
		return null;
	}

	public static List<GeoParsedPoint> parsePoints(String uriString) {
		URI uri = createUri(uriString);
		String scheme = uri != null ? uri.getScheme() : null;
		if (scheme != null) {
			scheme = scheme.toLowerCase(Locale.US);

			if ("http".equals(scheme) || "https".equals(scheme) || "google.navigation".equals(scheme)) {
				return parseLinkUri(uri, scheme);
			} else if ("geo".equals(scheme) || "osmand.geo".equals(scheme)) {
				return parseGeoUri(uri);
			}
		}
		return null;
	}

	private static URI createUri(final String uriString) {
		try {
			// amap.com uses | in their URLs, which is an illegal character for a URL
			return URI.create(uriString.trim().replaceAll("\\s+", "+")
					.replaceAll("%20", "+")
					.replaceAll("%2C", ",")
					.replaceAll("\\|", ";")
					.replaceAll("\\(\\(\\S+\\)\\)", ""));
		} catch (IllegalArgumentException e) {
		}
		return null;
	}

	private static List<GeoParsedPoint> parseLinkUri(URI uri, String scheme) {
		String host = uri.getHost();
		Map<String, String> params = getQueryParameters(uri);
		if ("google.navigation".equals(scheme)) {
			host = scheme;
			if (uri.getSchemeSpecificPart() == null) {
				return null;
			} else if (!uri.getSchemeSpecificPart().contains("=")) {
				params = getQueryParameters("q=" + uri.getSchemeSpecificPart());
			} else {
				params = getQueryParameters(uri.getSchemeSpecificPart());
			}
		} else if (host == null) {
			return null;
		} else {
			host = host.toLowerCase(Locale.US);
		}
		String path = uri.getPath();
		if (path == null) {
			path = "";
		}
		String fragment = uri.getFragment();

		// lat-double, lon - double, zoom or z - int
		Set<String> simpleDomains = new HashSet<String>();
		simpleDomains.add("osmand.net");
		simpleDomains.add("www.osmand.net");
		simpleDomains.add("download.osmand.net");
		simpleDomains.add("openstreetmap.de");
		simpleDomains.add("www.openstreetmap.de");

		Pattern commaSeparatedPairPattern = Pattern.compile("(?:loc:)?([N|S]?[+-]?\\d+(?:\\.\\d+)?),([E|W]?[+-]?\\d+(?:\\.\\d+)?)");

		try {
			if (host.equals("osm.org") || host.endsWith("openstreetmap.org")) {
				return parseOsmUri(uri, path, fragment);
			} else if (host.startsWith("map.baidu.")) { // .com and .cn both work
				return parseBaiduUri(params);
			} else if (host.equals("ge0.me")) {
				return parseGe0meUri(path);
			} else if (simpleDomains.contains(host)) {
				return parseSimpleDomainsUri(uri, path, params, fragment);
			} else if (host.matches("(?:www\\.)?(?:maps\\.)?yandex\\.[a-z]+")) {
				return parseYandexUri(params, commaSeparatedPairPattern);
			} else if (host.matches("(?:www\\.)?(?:maps\\.)?google\\.[a-z.]+")) {
				return parseGoogleUri(uri, path, params, fragment, commaSeparatedPairPattern);
			} else if (host.endsWith(".amap.com")) {
				return parseAmapUri(uri, scheme, host);
			} else if (host.equals("here.com") || host.endsWith(".here.com")) { // www.here.com, share.here.com, here.com
				return parseHereUri(path, params);
			} else if (host.endsWith(".qq.com")) {
				return parseQqUri(params, commaSeparatedPairPattern);
			} else if (host.equals("maps.apple.com")) {
				return parseAppleUri(params, commaSeparatedPairPattern);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static List<GeoParsedPoint> parseOsmUri(URI uri, String path, String fragment) {
		Pattern p;
		Matcher matcher;
		if (path.startsWith("/go/")) { // short URL form
			p = Pattern.compile("^/go/([A-Za-z0-9_@~]+-*)(?:.*)");
			matcher = p.matcher(path);
			if (matcher.matches()) {
				return Collections.singletonList(MapUtils.decodeShortLinkString(matcher.group(1)));
			}
		} else { // data in the query and/or feature strings
			double lat = 0;
			double lon = 0;
			int zoom = GeoParsedPoint.NO_ZOOM;
			Map<String, String> queryMap = getQueryParameters(uri);

			if (queryMap.containsKey("route")) {
				String routeValue = queryMap.get("route");
				Pattern coordinatesPattern = Pattern.compile("^(\\d+[.]?\\d*),(\\d+[.]?\\d*);(\\d+[.]?\\d*),(\\d+[.]?\\d*)");
				Matcher coordinatesMatcher = coordinatesPattern.matcher(routeValue);
				if (coordinatesMatcher.matches()) {
					GeoParsedPoint pointFrom = new GeoParsedPoint(parseSilentDouble(coordinatesMatcher.group(1)), parseSilentDouble(coordinatesMatcher.group(2)));
					GeoParsedPoint pointTo = new GeoParsedPoint(parseSilentDouble(coordinatesMatcher.group(3)), parseSilentDouble(coordinatesMatcher.group(4)));

					List<GeoParsedPoint> parsedPoints = new ArrayList<>();
					parsedPoints.add(pointFrom);
					parsedPoints.add(pointTo);

					return parsedPoints;
				}
			} else if (queryMap.containsKey("from") || queryMap.containsKey("to")) {
				GeoParsedPoint pointFrom = null;
				String from = queryMap.get("from");
				if (!Algorithms.isEmpty(from)) {
					String[] coordinates = from.split(",");
					lat = parseSilentDouble(coordinates[0]);
					lon = parseSilentDouble(coordinates[1]);
					pointFrom = new GeoParsedPoint(lat, lon);
				}
				GeoParsedPoint pointTo = null;
				String to = queryMap.get("to");
				if (!Algorithms.isEmpty(to)) {
					String[] coordinates = to.split(",");
					lat = parseSilentDouble(coordinates[0]);
					lon = parseSilentDouble(coordinates[1]);
					pointTo = new GeoParsedPoint(lat, lon);
				}
				List<GeoParsedPoint> parsedPoints = new ArrayList<>();
				parsedPoints.add(pointFrom);
				parsedPoints.add(pointTo);

				return parsedPoints;
			}
			if (fragment != null) {
				if (fragment.startsWith("map=")) {
					fragment = fragment.substring("map=".length());
				}
				String[] vls = fragment.split("/|&"); //"&" to split off trailing extra parameters
				if (vls.length >= 3) {
					zoom = parseZoom(vls[0]);
					lat = parseSilentDouble(vls[1]);
					lon = parseSilentDouble(vls[2]);
				}
			} else if (queryMap != null) {
				String queryStr = queryMap.get("query");
				if (queryStr != null) {
					queryStr = queryStr.replace("+", " ").replace(",", " ");
					String[] vls = queryStr.split(" ");
					if (vls.length == 2) {
						lat = parseSilentDouble(vls[0]);
						lon = parseSilentDouble(vls[1]);
					}
					if (lat == 0 || lon == 0) {
						return Collections.singletonList(new GeoParsedPoint(queryStr));
					}
				}
			}
			// the query string sometimes has higher resolution values
			String mlat = getQueryParameter("mlat", uri);
			if (mlat != null) {
				lat = parseSilentDouble(mlat);
			}
			String mlon = getQueryParameter("mlon", uri);
			if (mlon != null) {
				lon = parseSilentDouble(mlon);
			}
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
		}
		return null;
	}

	private static List<GeoParsedPoint> parseBaiduUri(Map<String, String> params) {
		/* Baidu Map uses a custom format for lat/lon., it is basically standard lat/lon
		 * multiplied by 100,000, then rounded to an integer */
		String zm = params.get("l");
		String[] vls = silentSplit(params.get("c"), ",");
		if (vls != null && vls.length >= 2) {
			double lat = parseSilentInt(vls[0]) / 100000.;
			double lon = parseSilentInt(vls[1]) / 100000.;
			int zoom = parseZoom(zm);
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
		}
		return null;
	}

	private static List<GeoParsedPoint> parseGe0meUri(String path) {
		// http:///44TvlEGXf-/Kyiv
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		String[] pms = path.split("/");
		String label = "";
		if (pms.length > 1) {
			label = pms[1];
		}
		String qry = pms[0];
		if (qry.length() < 10) {
			return null;
		}
		int indZoom = net.osmand.osm.io.Base64.indexOf(qry.charAt(0));
		int zoom = 15;
		if (indZoom >= 0) {
			zoom = indZoom / 4 + 4;
		}
		LatLon l = decodeMapsMeLatLonToInt(qry.substring(1).replace('-', '/'));
		if (l == null) {
			return null;
		}
		return Collections.singletonList(new GeoParsedPoint(l.getLatitude(), l.getLongitude(), zoom, label));
	}

	private static List<GeoParsedPoint> parseSimpleDomainsUri(URI uri, String path, Map<String, String> params, String fragment) {
		if (uri.getQuery() == null && params.size() == 0) {
			// DOUBLE check this may be wrong test of openstreetmap.de (looks very weird url and server doesn't respond)
			params = getQueryParameters(path.substring(1));
		}
		if (params.containsKey("lat") && params.containsKey("lon")) {
			final double lat = parseSilentDouble(params.get("lat"));
			final double lon = parseSilentDouble(params.get("lon"));
			int zoom = GeoParsedPoint.NO_ZOOM;
			if (params.containsKey("z")) {
				zoom = parseZoom(params.get("z"));
			} else if (params.containsKey("zoom")) {
				zoom = parseZoom(params.get("zoom"));
			}
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
		} else if (params.containsKey("pin")) {
			String[] coordinates = params.get("pin").split(",");
			final double lat = parseSilentDouble(coordinates[0]);
			final double lon = parseSilentDouble(coordinates[1]);
			int zoom = GeoParsedPoint.NO_ZOOM;
			if (!Algorithms.isEmpty(fragment)) {
				zoom = parseZoom(fragment.split("/")[0]);
			}
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
		}
		return null;
	}

	private static List<GeoParsedPoint> parseYandexUri(Map<String, String> params, Pattern commaSeparatedPairPattern) {
		String ll = params.get("ll");
		if (ll != null) {
			Matcher matcher = commaSeparatedPairPattern.matcher(ll);
			if (matcher.matches()) {
				String z = String.valueOf(parseZoom(params.get("z")));
				return Collections.singletonList(new GeoParsedPoint(matcher.group(1), matcher.group(2), z, params.get("text")));
			}
		}
		return null;
	}

	private static List<GeoParsedPoint> parseGoogleUri(URI uri, String path, Map<String, String> params,
	                                                   String fragment, Pattern commaSeparatedPairPattern) {
		String latString = null;
		String lonString = null;
		String z = String.valueOf(GeoParsedPoint.NO_ZOOM);

		if (params.containsKey("q")) {
			Matcher matcher = commaSeparatedPairPattern.matcher(params.get("q"));
			if (matcher.matches()) {
				latString = matcher.group(1);
				lonString = matcher.group(2);
			}
		} else if (params.containsKey("ll")) {
			Matcher matcher = commaSeparatedPairPattern.matcher(params.get("ll"));
			if (matcher.matches()) {
				latString = matcher.group(1);
				lonString = matcher.group(2);
			}
		}
		if (latString != null && lonString != null) {
			if (params.containsKey("z")) {
				z = params.get("z");
			}
			return Collections.singletonList(new GeoParsedPoint(latString, lonString, z));
		}
		if (params.containsKey("daddr")) {
			return parseGoogleMapsPath(params.get("daddr"), params);
		} else if (params.containsKey("saddr")) {
			return parseGoogleMapsPath(params.get("saddr"), params);
		} else if (params.containsKey("q")) {
			String opath = params.get("q");
			final String pref = "loc:";
			if (opath.contains(pref)) {
				opath = opath.substring(opath.lastIndexOf(pref) + pref.length());
			}
			final String postf = "\\s\\((\\p{L}|\\p{M}|\\p{Z}|\\p{S}|\\p{N}|\\p{P}|\\p{C})*\\)$";
			opath = opath.replaceAll(postf, "");
			return parseGoogleMapsPath(opath, params);
		} else if (params.containsKey("destination") || params.containsKey("origin")) {
			GeoParsedPoint origin = null;
			if (params.containsKey("origin")) {
				String[] coordinates = params.get("origin").split(",");
				double lat = parseSilentDouble(coordinates[0]);
				double lon = parseSilentDouble(coordinates[1]);
				origin = new GeoParsedPoint(lat, lon);
			}
			GeoParsedPoint destination = null;
			if (params.containsKey("destination")) {
				String[] coordinates = params.get("destination").split(",");
				double lat = parseSilentDouble(coordinates[0]);
				double lon = parseSilentDouble(coordinates[1]);
				destination = new GeoParsedPoint(lat, lon);
			}
			List<GeoParsedPoint> parsedPoints = new ArrayList<>();
			parsedPoints.add(origin);
			parsedPoints.add(destination);

			return parsedPoints;
		}
		if (fragment != null) {
			Pattern p = Pattern.compile(".*[!&]q=([^&!]+).*");
			Matcher m = p.matcher(fragment);
			if (m.matches()) {
				return Collections.singletonList(new GeoParsedPoint(m.group(1)));
			}
		}
		String DATA_PREFIX = "/data=";
		String[] pathPrefixes = new String[] {DATA_PREFIX, "/@", "/ll=", "loc:", "/"};
		for (String pref : pathPrefixes) {
			path = uri.getPath();
			if (path.contains(pref)) {
				path = path.substring(path.lastIndexOf(pref) + pref.length());
				if (path.contains("/")) {
					path = path.substring(0, path.indexOf('/'));
				}
				if (pref.equals(DATA_PREFIX)) {
					String[] vls = path.split("!");
					String lat = null;
					String lon = null;
					for (String v : vls) {
						if (v.startsWith("3d")) {
							lat = v.substring(2);
						} else if (v.startsWith("4d")) {
							lon = v.substring(2);
						}
					}
					if (lat != null && lon != null) {
						return Collections.singletonList(new GeoParsedPoint(Double.parseDouble(lat), Double.parseDouble(lon)));
					}
				} else {
					return parseGoogleMapsPath(path, params);
				}
			}
		}
		return null;
	}

	private static List<GeoParsedPoint> parseAmapUri(URI uri, String scheme, String host) {
		/* amap (mis)uses the Fragment, which is not included in the Scheme Specific Part,
		 * so instead we make a custom "everything but the Authority subString */
		// +4 for the :// and the /
		final String subString = uri.toString().substring(scheme.length() + host.length() + 4);
		Pattern p;
		Matcher matcher;
		final String[] patterns = {
				/* though this looks like Query String, it is also used as part of the Fragment */
				".*q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*&radius=(\\d+).*",
				".*q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*",
				".*p=(?:[A-Z0-9]+),([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*",};
		for (int i = 0; i < patterns.length; i++) {
			p = Pattern.compile(patterns[i]);
			matcher = p.matcher(subString);
			if (matcher.matches()) {
				if (matcher.groupCount() == 3) {
					// amap uses radius in meters, so do rough conversion into zoom level
					float radius = Float.valueOf(matcher.group(3));
					long zoom = Math.round(23. - Math.log(radius) / Math.log(2.0));
					return Collections.singletonList(new GeoParsedPoint(matcher.group(1), matcher.group(2), String.valueOf(zoom)));
				} else if (matcher.groupCount() == 2) {
					return Collections.singletonList(new GeoParsedPoint(matcher.group(1), matcher.group(2)));
				}
			}
		}
		return null;
	}

	private static List<GeoParsedPoint> parseHereUri(String path, Map<String, String> params) {
		String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
		String label = null;
		if (params.containsKey("msg")) {
			label = params.get("msg");
		}
		if (params.containsKey("z")) {
			z = params.get("z");
		}
		if (params.containsKey("map")) {
			String[] mapArray = params.get("map").split(",");
			if (mapArray.length > 2) {
				return Collections.singletonList(new GeoParsedPoint(mapArray[0], mapArray[1], mapArray[2], label));
			} else if (mapArray.length > 1) {
				return Collections.singletonList(new GeoParsedPoint(mapArray[0], mapArray[1], z, label));
			}
		}
		if (path.startsWith("/l/")) {
			Pattern p = Pattern.compile("^/l/([+-]?\\d+(?:\\.\\d+)),([+-]?\\d+(?:\\.\\d+)),(.*)");
			Matcher matcher = p.matcher(path);
			if (matcher.matches()) {
				return Collections.singletonList(new GeoParsedPoint(matcher.group(1), matcher.group(2), z, matcher.group(3)));
			}
		}
		return null;
	}

	private static List<GeoParsedPoint> parseQqUri(Map<String, String> params, Pattern commaSeparatedPairPattern) {
		String x = null;
		String y = null;
		String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
		String label = null;
		if (params.containsKey("city")) {
			label = params.get("city");
		} else if (params.containsKey("key")) {
			label = params.get("key");
		} else if (params.containsKey("a")) {
			label = params.get("a");
		} else if (params.containsKey("n")) {
			label = params.get("n");
		}
		String m = params.get("m");
		if (m != null) {
			Matcher matcher = commaSeparatedPairPattern.matcher(m);
			if (matcher.matches()) {
				x = matcher.group(2);
				y = matcher.group(1);
			}
		}
		String c = params.get("c");
		if (c != null) {
			// there are two different patterns of data that can be in ?c=
			Matcher matcher = commaSeparatedPairPattern.matcher(c);
			if (matcher.matches()) {
				x = matcher.group(2);
				y = matcher.group(1);
			} else {
				x = c.replaceAll(".*\"lng\":\\s*([+\\-]?[0-9.]+).*", "$1");
				if (x == null) // try 'lon' for the second time
					x = c.replaceAll(".*\"lon\":\\s*([+\\-]?[0-9.]+).*", "$1");
				y = c.replaceAll(".*\"lat\":\\s*([+\\-]?[0-9.]+).*", "$1");
				z = c.replaceAll(".*\"l\":\\s*([+-]?[0-9.]+).*", "$1");
				return Collections.singletonList(new GeoParsedPoint(y, x, z, label));
			}
		}
		for (String key : new String[] {"centerX", "x", "x1", "x2"}) {
			if (params.containsKey(key)) {
				x = params.get(key);
				break;
			}
		}
		for (String key : new String[] {"centerY", "y", "y1", "y2"}) {
			if (params.containsKey(key)) {
				y = params.get(key);
				break;
			}
		}
		if (x != null && y != null) {
			return Collections.singletonList(new GeoParsedPoint(y, x, z, label));
		}
		return null;
	}

	private static List<GeoParsedPoint> parseAppleUri(Map<String, String> params, Pattern commaSeparatedPairPattern) {
		// https://developer.apple.com/library/iad/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
		String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
		String label = null;
		if (params.containsKey("q")) {
			label = params.get("q");
		}
		if (params.containsKey("near")) {
			label = params.get("near");
		}
		if (params.containsKey("z")) {
			z = params.get("z");
		}
		String ll = params.get("ll");
		if (ll != null) {
			Matcher matcher = commaSeparatedPairPattern.matcher(ll);
			if (matcher.matches()) {
				return Collections.singletonList(new GeoParsedPoint(matcher.group(1), matcher.group(2), z, label));
			}
		}
		String sll = params.get("sll");
		if (sll != null) {
			Matcher matcher = commaSeparatedPairPattern.matcher(sll);
			if (matcher.matches()) {
				return Collections.singletonList(new GeoParsedPoint(matcher.group(1), matcher.group(2), z, label));
			}
		}
		// if no ll= or sll=, then just use the q string
		if (params.containsKey("q")) {
			return Collections.singletonList(new GeoParsedPoint(params.get("q")));
		}
		// if no q=, then just use the destination address
		if (params.containsKey("daddr")) {
			return Collections.singletonList(new GeoParsedPoint(params.get("daddr")));
		}
		// if no daddr=, then just use the source address
		if (params.containsKey("saddr")) {
			return Collections.singletonList(new GeoParsedPoint(params.get("saddr")));
		}
		return null;
	}

	private static List<GeoParsedPoint> parseGeoUri(URI uri) {
		String schemeSpecific = uri.getSchemeSpecificPart();
		if (schemeSpecific == null) {
			return null;
		}
		if (uri.getRawSchemeSpecificPart().contains("%2B")) {
			schemeSpecific = schemeSpecific.replace("+", "%2B");
		}

		String name = null;
		final Pattern namePattern = Pattern.compile("[\\+\\s]*\\((.*)\\)[\\+\\s]*$");
		final Matcher nameMatcher = namePattern.matcher(schemeSpecific);
		if (nameMatcher.find()) {
			name = URLDecoder.decode(nameMatcher.group(1));
			if (name != null) {
				schemeSpecific = schemeSpecific.substring(0, nameMatcher.start());
			}
		}

		String positionPart;
		String queryPart = "";
		int queryStartIndex = schemeSpecific.indexOf('?');
		if (queryStartIndex == -1) {
			positionPart = schemeSpecific;
		} else {
			positionPart = schemeSpecific.substring(0, queryStartIndex);
			if (queryStartIndex < schemeSpecific.length()) {
				queryPart = schemeSpecific.substring(queryStartIndex + 1);
			}
		}

		final Pattern positionPattern = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");
		final Matcher positionMatcher = positionPattern.matcher(positionPart);
		double lat = 0.0;
		double lon = 0.0;
		if (positionMatcher.find()) {
			lat = Double.valueOf(positionMatcher.group(1));
			lon = Double.valueOf(positionMatcher.group(2));
		}

		int zoom = GeoParsedPoint.NO_ZOOM;
		String searchRequest = null;
		for (String param : queryPart.split("&")) {
			String paramName;
			String paramValue = null;
			int nameValueDelimititerIndex = param.indexOf('=');
			if (nameValueDelimititerIndex == -1) {
				paramName = param;
			} else {
				paramName = param.substring(0, nameValueDelimititerIndex);
				if (nameValueDelimititerIndex < param.length()) {
					paramValue = param.substring(nameValueDelimititerIndex + 1);
				}
			}

			if ("z".equals(paramName) && paramValue != null) {
				zoom = (int) Float.parseFloat(paramValue);
			} else if ("q".equals(paramName) && paramValue != null) {
				searchRequest = URLDecoder.decode(paramValue);
			}
		}

		if (searchRequest != null) {
			String searchPattern = Pattern.compile("(?:\\.|,|\\s+|\\+|[+-]?\\d+(?:\\.\\d+)?)").pattern();
			String[] search = searchRequest.split(searchPattern);
			if (search.length > 0) {
				return Collections.singletonList(new GeoParsedPoint(searchRequest));
			}
			final Matcher positionInSearchRequestMatcher = positionPattern.matcher(searchRequest);
			if (lat == 0.0 && lon == 0.0 && positionInSearchRequestMatcher.find()) {
				double tempLat = Double.valueOf(positionInSearchRequestMatcher.group(1));
				double tempLon = Double.valueOf(positionInSearchRequestMatcher.group(2));
				if (tempLat >= -90 && tempLat <= 90 && tempLon >= -180 && tempLon <= 180) {
					lat = tempLat;
					lon = tempLon;
				}
			}
		}
		if (lat == 0.0 && lon == 0.0 && searchRequest != null) {
			return Collections.singletonList(new GeoParsedPoint(searchRequest));
		}
		if (zoom != GeoParsedPoint.NO_ZOOM) {
			return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom, name));
		}
		return Collections.singletonList(new GeoParsedPoint(lat, lon, name));
	}

	private static List<GeoParsedPoint> parseGoogleMapsPath(String opath, Map<String, String> params) {
		String zmPart = "";
		String descr = "";
		String path = opath;
		if (path.contains("&")) {
			String[] vls = path.split("&");
			path = vls[0];
			for (int i = 1; i < vls.length; i++) {
				int ik = vls[i].indexOf('=');
				if (ik > 0) {
					params.put(vls[i].substring(0, ik), vls[i].substring(ik + 1));
				}
			}
		}
		if (path.contains("+")) {
			path = path.substring(0, path.indexOf("+"));
			descr = path.substring(path.indexOf("+") + 1);
			if (descr.contains(")")) {
				descr = descr.substring(0, descr.indexOf(")"));
			}

		}
		if (params.containsKey("z")) {
			zmPart = params.get("z");
		}
		String[] vls = null;
		if (path.contains("@")) {
			path = path.substring(path.indexOf("@") + 1);
			if (path.contains(",")) {
				vls = silentSplit(path, ",");
			}
		}
		if (vls == null) {
			vls = silentSplit(path, ",");
		}

		if (vls.length >= 2) {
			double lat = parseSilentDouble(vls[0], Double.NaN);
			double lon = parseSilentDouble(vls[1], Double.NaN);
			int zoom = GeoParsedPoint.NO_ZOOM;
			if (vls.length >= 3 || zmPart.length() > 0) {
				if (zmPart.length() == 0) {
					zmPart = vls[2];
				}
				if (zmPart.startsWith("z=")) {
					zmPart = zmPart.substring(2);
				} else if (zmPart.contains("z")) {
					zmPart = zmPart.substring(0, zmPart.indexOf('z'));
				}
				zoom = parseZoom(zmPart);
			}
			if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
				return Collections.singletonList(new GeoParsedPoint(lat, lon, zoom));
			}
		}
		return Collections.singletonList(new GeoParsedPoint(URLDecoder.decode(opath)));
	}

	private static String[] silentSplit(String vl, String split) {
		if (vl == null) {
			return null;
		}
		return vl.split(split);
	}

	protected static int parseZoom(String zoom) {
		try {
			if (zoom != null) {
				return (int) Float.parseFloat(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return GeoParsedPoint.NO_ZOOM;
	}

	private static double parseSilentDouble(String zoom) {
		return parseSilentDouble(zoom, 0);
	}

	private static double parseSilentDouble(String zoom, double vl) {
		try {
			if (zoom != null) {
				return Double.valueOf(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return vl;
	}

	private static int parseSilentInt(String zoom) {
		try {
			if (zoom != null) {
				return Integer.valueOf(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return 0;
	}
}
