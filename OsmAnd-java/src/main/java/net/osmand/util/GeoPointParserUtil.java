package net.osmand.util;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.data.LatLon;
import net.osmand.util.GeoPointParserUtil.GeoParsedPoint;

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
	public static GeoParsedPoint parse(final String uriString) {
		URI uri;
		try {
			// amap.com uses | in their URLs, which is an illegal character for a URL
			uri = URI.create(uriString.replaceAll("\\s+", "+")
					.replaceAll("%20", "+")
					.replaceAll("%2C", ",")
					.replaceAll("\\|", ";")
					.replaceAll("\\(\\(\\S+\\)\\)", ""));
		} catch (IllegalArgumentException e) {
			return null;
		}

		String scheme = uri.getScheme();
		if (scheme == null)
			return null;
		else
			scheme = scheme.toLowerCase(Locale.US);

		if ("http".equals(scheme) || "https".equals(scheme) || "google.navigation".equals(scheme)) {
			String host = uri.getHost();
			Map<String, String> params = getQueryParameters(uri);
			if("google.navigation".equals(scheme)) {
				host = scheme;
				if(uri.getSchemeSpecificPart() == null) {
					return null;
				} else if(!uri.getSchemeSpecificPart().contains("=")) {
					params = getQueryParameters("q="+uri.getSchemeSpecificPart());
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


			final Pattern commaSeparatedPairPattern = Pattern.compile("(?:loc:)?([N|S]?[+-]?\\d+(?:\\.\\d+)?),([E|W]?[+-]?\\d+(?:\\.\\d+)?)");

			try {
				if (host.equals("osm.org") || host.endsWith("openstreetmap.org")) {
					Pattern p;
					Matcher matcher;
					if (path.startsWith("/go/")) { // short URL form
						p = Pattern.compile("^/go/([A-Za-z0-9_@~]+-*)(?:.*)");
						matcher = p.matcher(path);
						if (matcher.matches()) {
							return MapUtils.decodeShortLinkString(matcher.group(1));
						}
					} else { // data in the query and/or feature strings
						double lat = 0;
						double lon = 0;
						int zoom = GeoParsedPoint.NO_ZOOM;
						Map<String, String> queryMap = getQueryParameters(uri);
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
									return new GeoParsedPoint(queryStr);
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
						return new GeoParsedPoint(lat, lon, zoom);
					}
				} else if (host.startsWith("map.baidu.")) { // .com and .cn both work
					/* Baidu Map uses a custom format for lat/lon., it is basically standard lat/lon
					 * multiplied by 100,000, then rounded to an integer */
					String zm = params.get("l");
					String[] vls = silentSplit(params.get("c"), ",");
					if (vls != null && vls.length >= 2) {
						double lat = parseSilentInt(vls[0]) / 100000.;
						double lon = parseSilentInt(vls[1]) / 100000.;
						int zoom = parseZoom(zm);
						return new GeoParsedPoint(lat, lon, zoom);
					}
				} else if (host.equals("ge0.me")) {
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
					return new GeoParsedPoint(l.getLatitude(), l.getLongitude(), zoom, label);

				} else if (simpleDomains.contains(host)) {
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
						return new GeoParsedPoint(lat, lon, zoom);
					}
				} else if (host.matches("(?:www\\.)?(?:maps\\.)?yandex\\.[a-z]+")) {
					String ll = params.get("ll");
					if (ll != null) {
						Matcher matcher = commaSeparatedPairPattern.matcher(ll);
						if (matcher.matches()) {
							String z = String.valueOf(parseZoom(params.get("z")));
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, params.get("text"));
						}
					}
				} else if (host.matches("(?:www\\.)?(?:maps\\.)?google\\.[a-z.]+")) {
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
						return new GeoParsedPoint(latString, lonString, z);
					}
					if (params.containsKey("daddr")) {
						return parseGoogleMapsPath(params.get("daddr"), params);
					} else if (params.containsKey("saddr")) {
						return parseGoogleMapsPath(params.get("saddr"), params);
					} else if (params.containsKey("q")) {
						String opath = params.get("q");
						final String pref = "loc:";
						if(opath.contains(pref)) {
							opath = opath.substring(opath.lastIndexOf(pref) + pref.length());
						}
						final String postf = "\\s\\((\\p{L}|\\p{M}|\\p{Z}|\\p{S}|\\p{N}|\\p{P}|\\p{C})*\\)$";
						opath = opath.replaceAll(postf, "");
						return parseGoogleMapsPath(opath, params);
					}
					if (fragment != null) {
						Pattern p = Pattern.compile(".*[!&]q=([^&!]+).*");
						Matcher m = p.matcher(fragment);
						if (m.matches()) {
							return new GeoParsedPoint(m.group(1));
						}
					}
					String DATA_PREFIX = "/data=";
					String[] pathPrefixes = new String[]{"/@", "/ll=",
							"loc:", DATA_PREFIX, "/"};
					for (String pref : pathPrefixes) {
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
									return new GeoParsedPoint(Double.valueOf(lat), Double.valueOf(lon));
								}
							} else {
								return parseGoogleMapsPath(path, params);
							}
						}
					}
				} else if (host.endsWith(".amap.com")) {
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
								return new GeoParsedPoint(matcher.group(1), matcher.group(2), String.valueOf(zoom));
							} else if (matcher.groupCount() == 2) {
								return new GeoParsedPoint(matcher.group(1), matcher.group(2));
							}
						}
					}
				} else if (host.equals("here.com") || host.endsWith(".here.com")) { // www.here.com, share.here.com, here.com
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
							return new GeoParsedPoint(mapArray[0], mapArray[1], mapArray[2], label);
						} else if (mapArray.length > 1) {
							return new GeoParsedPoint(mapArray[0], mapArray[1], z, label);
						}
					}
					if (path.startsWith("/l/")) {
						Pattern p = Pattern.compile("^/l/([+-]?\\d+(?:\\.\\d+)),([+-]?\\d+(?:\\.\\d+)),(.*)");
						Matcher matcher = p.matcher(path);
						if (matcher.matches()) {
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, matcher.group(3));
						}
					}
				} else if (host.endsWith(".qq.com")) {
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
							return new GeoParsedPoint(y, x, z, label);
						}
					}
					for (String key : new String[]{"centerX", "x", "x1", "x2"}) {
						if (params.containsKey(key)) {
							x = params.get(key);
							break;
						}
					}
					for (String key : new String[]{"centerY", "y", "y1", "y2"}) {
						if (params.containsKey(key)) {
							y = params.get(key);
							break;
						}
					}
					if (x != null && y != null)
						return new GeoParsedPoint(y, x, z, label);
				} else if (host.equals("maps.apple.com")) {
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
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, label);
						}
					}
					String sll = params.get("sll");
					if (sll != null) {
						Matcher matcher = commaSeparatedPairPattern.matcher(sll);
						if (matcher.matches()) {
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, label);
						}
					}
					// if no ll= or sll=, then just use the q string
					if (params.containsKey("q")) {
						return new GeoParsedPoint(params.get("q"));
					}
					// if no q=, then just use the destination address
					if (params.containsKey("daddr")) {
						return new GeoParsedPoint(params.get("daddr"));
					}
					// if no daddr=, then just use the source address
					if (params.containsKey("saddr")) {
						return new GeoParsedPoint(params.get("saddr"));
					}
				}

			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			return null;
		} else if ("geo".equals(scheme) || "osmand.geo".equals(scheme)) {
			String schemeSpecific = uri.getSchemeSpecificPart();
			if (schemeSpecific == null) {
				return null;
			}
			if(uri.getRawSchemeSpecificPart().contains("%2B")) {
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

			final Pattern positionPattern = Pattern.compile(
					"([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");
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
					if (nameValueDelimititerIndex < param.length())
						paramValue = param.substring(nameValueDelimititerIndex + 1);
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
					return new GeoParsedPoint(searchRequest);
				}
				final Matcher positionInSearchRequestMatcher =
						positionPattern.matcher(searchRequest);
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
				return new GeoParsedPoint(searchRequest);
			}

			if (zoom != GeoParsedPoint.NO_ZOOM) {
				return new GeoParsedPoint(lat, lon, zoom, name);
			}
			return new GeoParsedPoint(lat, lon, name);
		}
		return null;
	}

	private static GeoParsedPoint parseGoogleMapsPath(String opath, Map<String, String> params) {
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
		if(path.contains("@")) {
			path = path.substring(path.indexOf("@") + 1);
			if(path.contains(",")) {
				vls = silentSplit(path, ",");
			}
		}
		if(vls == null) {
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
			if(!Double.isNaN(lat) && !Double.isNaN(lon)) {
				return new GeoParsedPoint(lat, lon, zoom);
			}
		}
		return new GeoParsedPoint(URLDecoder.decode(opath));
	}

	private static String[] silentSplit(String vl, String split) {
		if (vl == null) {
			return null;
		}
		return vl.split(split);
	}

	private static int parseZoom(String zoom) {
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

	public static class GeoParsedPoint {
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
			this.zoom = parseZoom(zoomString);
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
					String.format("GeoParsedPoint [query=%s]",query);
		}
	}
}
