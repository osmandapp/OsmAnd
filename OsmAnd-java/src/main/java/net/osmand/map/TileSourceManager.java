package net.osmand.map;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class TileSourceManager {

	private static final Log log = PlatformUtil.getLog(TileSourceManager.class);

	public static final String RULE_YANDEX_TRAFFIC = "yandex_traffic";
	public static final String MAPILLARY_VECTOR_TILE_EXT = ".pbf";
	public static final String MAPILLARY_ACCESS_TOKEN = "MLY|4444816185556934|29475a355616c979409a5adc377a00fa";

	private static final String RULE_WMS = "wms_tile";
	private static final String RULE_TEMPLATE_1 = "template:1";
	private static final String RND_ALG_WIKIMAPIA = "wikimapia";

	private static final String MAPNIK_URL = "https://tile.osmand.net/hd/{0}/{1}/{2}.png";
	private static final String MAPILLARY_VECTOR_URL = "https://tiles.mapillary.com/maps/vtp/mly1_public/2/{0}/{1}/{2}/?access_token="
			+ MAPILLARY_ACCESS_TOKEN;

	private static final TileSourceTemplate MAPNIK_SOURCE =
			new TileSourceTemplate("OsmAnd (online tiles)", MAPNIK_URL, ".png", 19,
					1, 512, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$
	private static final TileSourceTemplate MAPILLARY_VECTOR_SOURCE =
			new TileSourceTemplate("Mapillary (vector tiles)", MAPILLARY_VECTOR_URL,
					MAPILLARY_VECTOR_TILE_EXT, 22, 13, 256, 16, 3200);
	private static final TileSourceTemplate MAPILLARY_CACHE_SOURCE =
			new TileSourceTemplate("Mapillary (raster tiles)", "", ".png", 22, 13,
					256, 32, 18000);

	static {
		int oneDayMinutes = 60 * 24;
		MAPILLARY_VECTOR_SOURCE.setExpirationTimeMinutes(oneDayMinutes);
		MAPILLARY_VECTOR_SOURCE.setHidden(true);
		MAPILLARY_CACHE_SOURCE.setExpirationTimeMinutes(oneDayMinutes);		
		MAPILLARY_CACHE_SOURCE.setHidden(true);
	}

	public static final String PARAM_BING_QUAD_KEY = "{q}";
	private static final String PARAM_RND = "{rnd}";
	private static final String PARAM_BOUNDING_BOX = "{bbox}";
	public static final String PARAMETER_NAME = "{PARAM}";

	public static class TileSourceTemplate implements ITileSource, Cloneable {
		private int maxZoom;
		private int minZoom;
		private String name;
		protected int tileSize;
		protected String urlToLoad;
		private final Map<String, String> urlParameters = new ConcurrentHashMap<>();
		protected String ext;
		private int avgSize;
		private int bitDensity;
		// -1 never expires, 
		private long expirationTimeMillis = -1;
		private boolean ellipticYTile;
		private boolean invertedYTile;
		private String randoms;
		private String[] randomsArray;
		private String rule;
		private String referer;
		private String userAgent;
		private boolean hidden; // if hidden in configure map settings, for example mapillary sources

		private ParameterType paramType = ParameterType.UNDEFINED;
		private long paramMin;
		private long paramStep;
		private long paramMax;

		private boolean isRuleAcceptable = true;

		public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom,
		                          int tileSize, int bitDensity, int avgSize) {
			this.maxZoom = maxZoom;
			this.minZoom = minZoom;
			this.name = name;
			this.tileSize = tileSize;
			this.urlToLoad = urlToLoad;
			this.ext = ext;
			this.avgSize = avgSize;
			this.bitDensity = bitDensity;
		}

		public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom,
		                          int tileSize, int bitDensity, int avgSize,
		                          ParameterType paramType, long paramMin, long paramStep, long paramMax) {
			this(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
			this.paramType = paramType;
			this.paramMin = paramMin;
			this.paramStep = paramStep;
			this.paramMax = paramMax;
		}

		public static String normalizeUrl(String url) {
			if (url != null) {
				url = url.replaceAll("\\{\\$z\\}", "{0}");
				url = url.replaceAll("\\{\\$x\\}", "{1}");
				url = url.replaceAll("\\{\\$y\\}", "{2}");
				url = url.replaceAll("\\{z\\}", "{0}");
				url = url.replaceAll("\\{x\\}", "{1}");
				url = url.replaceAll("\\{y\\}", "{2}");
			}
			return url;
		}

		public static String[] buildRandomsArray(String randomsStr) {
			List<String> randoms = new ArrayList<>();
			if (!Algorithms.isEmpty(randomsStr)) {
				if (randomsStr.equals(RND_ALG_WIKIMAPIA)) {
					return new String[]{RND_ALG_WIKIMAPIA};
				}
				String[] valuesArray = randomsStr.split(",");
				for (String s : valuesArray) {
					String[] rangeArray = s.split("-");
					if (rangeArray.length == 2) {
						String s1 = rangeArray[0];
						String s2 = rangeArray[1];
						boolean rangeValid = false;
						try {
							int a = Integer.parseInt(s1);
							int b = Integer.parseInt(s2);
							if (b > a) {
								for (int i = a; i <= b; i++) {
									randoms.add(String.valueOf(i));
								}
								rangeValid = true;
							}
						} catch (NumberFormatException e) {
							if (s1.length() == 1 && s2.length() == 1) {
								char a = s1.charAt(0);
								char b = s2.charAt(0);
								if (b > a) {
									for (char i = a; i <= b; i++) {
										randoms.add(String.valueOf(i));
									}
									rangeValid = true;
								}
							}
						}
						if (!rangeValid) {
							randoms.add(s1);
							randoms.add(s2);
						}
					} else {
						randoms.add(s);
					}
				}
			}
			return randoms.toArray(new String[0]);
		}

		public void setMinZoom(int minZoom) {
			this.minZoom = minZoom;
		}
		
		public void setMaxZoom(int maxZoom) {
			this.maxZoom = maxZoom;
		}

		public boolean isHidden() {
			return hidden;
		}

		public void setHidden(boolean hidden) {
			this.hidden = hidden;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setEllipticYTile(boolean ellipticYTile) {
			this.ellipticYTile = ellipticYTile;
		}

		@Override
		public boolean isEllipticYTile() {
			return ellipticYTile;
		}

		public boolean isInvertedYTile() {
			return invertedYTile;
		}

		@Override
		public boolean isTimeSupported() {
			return expirationTimeMillis != -1;
		}

		@Override
		public boolean getInversiveZoom() {
			return false;
		}

		public void setInvertedYTile(boolean invertedYTile) {
			this.invertedYTile = invertedYTile;
		}

		public String getRandoms() {
			return randoms;
		}

		public void setRandoms(String randoms) {
			this.randoms = randoms;
			this.randomsArray = buildRandomsArray(randoms);
		}

		public String[] getRandomsArray() {
			return randomsArray;
		}

		public void setRandomsArray(String[] randomsArray) {
			this.randomsArray = randomsArray;
		}

		@Override
		public int getBitDensity() {
			return bitDensity;
		}

		public int getAverageSize() {
			return avgSize;
		}

		@Override
		public int getMaximumZoomSupported() {
			return maxZoom;
		}

		@Override
		public int getMinimumZoomSupported() {
			return minZoom;
		}

		@Override
		public String getName() {
			return name;
		}
		
		public void setExpirationTimeMillis(long timeMillis) {
			this.expirationTimeMillis = timeMillis;
		}
		
		public void setExpirationTimeMinutes(int minutes) {
			if(minutes < 0) {
				this.expirationTimeMillis = -1;
			} else {
				this.expirationTimeMillis = minutes * 60 * 1000l;
			}
		}
		
		public int getExpirationTimeMinutes() {
			if(expirationTimeMillis < 0) {
				return -1;
			}
			return (int) (expirationTimeMillis / (60  * 1000));
		}
		
		public long getExpirationTimeMillis() {
			return expirationTimeMillis;
		}
		
		public String getReferer() {
			return referer;
		}

		public void setReferer(String referer) {
			this.referer = referer;
		}

		public String getUserAgent() {
			return userAgent;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}

		@Override
		public int getTileSize() {
			return tileSize;
		}

		@Override
		public String getTileFormat() {
			return ext;
		}
		
		public void setTileFormat(String ext) {
			this.ext = ext;
		}
		
		public void setUrlToLoad(String urlToLoad) {
			this.urlToLoad = urlToLoad;
		}
		
		public boolean isRuleAcceptable() {
			return isRuleAcceptable;
		}
		
		public void setRuleAcceptable(boolean isRuleAcceptable) {
			this.isRuleAcceptable = isRuleAcceptable;
		}

		public ParameterType getParamType() {
			return paramType;
		}

		public long getParamMin() {
			return paramMin;
		}

		public long getParamStep() {
			return paramStep;
		}

		public long getParamMax() {
			return paramMax;
		}

		public void setParamType(ParameterType paramType) {
			this.paramType = paramType;
		}

		public void setParamMin(long paramMin) {
			this.paramMin = paramMin;
		}

		public void setParamStep(long paramStep) {
			this.paramStep = paramStep;
		}

		public void setParamMax(long paramMax) {
			this.paramMax = paramMax;
		}

		@Override
		public Map<String, String> getUrlParameters() {
			return Collections.unmodifiableMap(urlParameters);
		}

		@Override
		public String getUrlParameter(String name) {
			return urlParameters.get(name);
		}

		@Override
		public void setUrlParameter(String name, String value) {
			urlParameters.put(name, value);
		}

		@Override
		public void resetUrlParameter(String name) {
			urlParameters.remove(name);
		}

		@Override
		public void resetUrlParameters() {
			urlParameters.clear();
		}

		public TileSourceTemplate copy() {
			try {
				return (TileSourceTemplate) this.clone();
			} catch (CloneNotSupportedException e) {
				return this;
			}
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			// use int to string not format numbers! (non-nls)
			if (urlToLoad == null) {
				return null;
			}
			if (isInvertedYTile()) {
				y = (1 << zoom) - 1 - y;
			}
			return buildUrlToLoad(urlToLoad, randomsArray, x, y, zoom, urlParameters);
		}

		public static String eqtBingQuadKey(int z, int x, int y) {
			char[] NUM_CHAR = {'0', '1', '2', '3'};
			char[] tn = new char[z];
			for (int i = z - 1; i >= 0; i--) {
				int num = (x % 2) | ((y % 2) << 1);
				tn[i] = NUM_CHAR[num];
				x >>= 1;
				y >>= 1;
			}
			return new String(tn);
		}

		private static String calcBoundingBoxForTile(int zoom, int x, int y) {
			double xmin = MapUtils.getLongitudeFromTile(zoom, x);
			double xmax = MapUtils.getLongitudeFromTile(zoom, x+1);
			double ymin = MapUtils.getLatitudeFromTile(zoom, y+1);
			double ymax = MapUtils.getLatitudeFromTile(zoom, y);
			return String.format(Locale.US, "%.8f,%.8f,%.8f,%.8f", xmin, ymin, xmax, ymax);
		}

		public static String buildUrlToLoad(String urlTemplate, String[] randomsArray, int x, int y, int zoom, Map<String, String> params) {
			try {
				if (randomsArray != null && randomsArray.length > 0) {
					String rand;
					if (RND_ALG_WIKIMAPIA.equals(randomsArray[0])) {
						rand = String.valueOf(x % 4 + (y % 4) * 4);
					} else {
						rand = randomsArray[(x + y) % randomsArray.length];
					}
					urlTemplate = urlTemplate.replace(PARAM_RND, rand);
				} else if (urlTemplate.contains(PARAM_RND)) {
					log.error("Cannot resolve randoms for template: " + urlTemplate);
					return null;
				}

				int bingQuadKeyParamIndex = urlTemplate.indexOf(PARAM_BING_QUAD_KEY);
				if (bingQuadKeyParamIndex != -1) {
					return urlTemplate.replace(PARAM_BING_QUAD_KEY, eqtBingQuadKey(zoom, x, y));
				}

				int bbKeyParamIndex = urlTemplate.indexOf(PARAM_BOUNDING_BOX);
				if (bbKeyParamIndex != -1) {
					return urlTemplate.replace(PARAM_BOUNDING_BOX, calcBoundingBoxForTile(zoom, x, y));
				}

				if (!Algorithms.isEmpty(params)) {
					for (Entry<String, String> pv : params.entrySet()) {
						String name = pv.getKey();
						int paramIndex = urlTemplate.indexOf(name);
						if (paramIndex != -1) {
							urlTemplate = urlTemplate.replace(name, pv.getValue());
						}
					}
				}

				return MessageFormat.format(urlTemplate, zoom + "", x + "", y + "");

			} catch (IllegalArgumentException e) {
				log.error("Cannot build url for template: " + urlTemplate, e);
				return null;
			}
		}


		public String getUrlTemplate() {
			return urlToLoad;
		}

		@Override
		public boolean couldBeDownloadedFromInternet() {
			return urlToLoad != null;
		}
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TileSourceTemplate other = (TileSourceTemplate) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		public void setRule(String rule) {
			this.rule = rule;
		}

		public String getRule() {
			return rule;
		}

		public String calculateTileId(int x, int y, int zoom) {
			StringBuilder builder = new StringBuilder(getName());
			builder.append('/');
			builder.append(zoom).append('/').append(x).append('/').append(y).append(getTileFormat()).append(".tile"); //$NON-NLS-1$ //$NON-NLS-2$
			return builder.toString();
		}

		@Override
		public long getTileModifyTime(int x, int y, int zoom, String dirWithTiles) {
			File en = new File(dirWithTiles, calculateTileId(x, y, zoom));
			if (en.exists()) {
				return en.lastModified();
			}
			return System.currentTimeMillis();
		}

		@Override
		public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException {
			File f = new File(dirWithTiles, calculateTileId(x, y, zoom));
			if (!f.exists())
				return null;

			ByteArrayOutputStream bous = new ByteArrayOutputStream();
			FileInputStream fis = new FileInputStream(f);
			Algorithms.streamCopy(fis, bous);
			fis.close();
			bous.close();
			return bous.toByteArray();
		}


		@Override
		public void deleteTiles(String path) {
			File pf = new File(path);
			File[] list = pf.listFiles();
			if (list != null) {
				for (File l : list) {
					if (l.isDirectory()) {
						Algorithms.removeAllFiles(l);
					}
				}
			}
		}

		@Override
		public int getAvgSize() {
			return avgSize;
		}

		public Map<String, String> getProperties() {
			Map<String, String> properties = new LinkedHashMap<>();

			if (!Algorithms.isEmpty(getRule())) {
				properties.put("rule", getRule());
			}
			if (getUrlTemplate() != null) {
				properties.put("url_template", getUrlTemplate());
			}
			if (!Algorithms.isEmpty(getReferer())) {
				properties.put("referer", getReferer());
			}
			if (!Algorithms.isEmpty(getUserAgent())) {
				properties.put("user_agent", getUserAgent());
			}

			properties.put("ext", getTileFormat());
			properties.put("min_zoom", getMinimumZoomSupported() + "");
			properties.put("max_zoom", getMaximumZoomSupported() + "");
			properties.put("tile_size", getTileSize() + "");
			properties.put("img_density", getBitDensity() + "");
			properties.put("avg_img_size", getAverageSize() + "");

			if (isEllipticYTile()) {
				properties.put("ellipsoid", isEllipticYTile() + "");
			}
			if (isInvertedYTile()) {
				properties.put("inverted_y", isInvertedYTile() + "");
			}
			if (getRandoms() != null) {
				properties.put("randoms", getRandoms());
			}
			if (getExpirationTimeMinutes() != -1) {
				properties.put("expiration_time_minutes", getExpirationTimeMinutes() + "");
			}
			if (paramType != ParameterType.UNDEFINED) {
				properties.put("param_type", paramType.getParamName());
				properties.put("param_min", paramMin + "");
				properties.put("param_step", paramStep + "");
				properties.put("param_max", paramMax + "");
			}
			return properties;
		}
	}

	private static int parseInt(Map<String, String> attributes, String value, int def) {
		String val = attributes.get(value);
		if (val == null) {
			return def;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public static void createMetaInfoFile(File dir, TileSourceTemplate template, boolean override) throws IOException {
		File metainfo = new File(dir, ".metainfo");
		Map<String, String> properties = template.getProperties();

		if (override || !metainfo.exists()) {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metainfo)));
			for (Entry<String, String> entry : properties.entrySet()) {
				writer.write("[" + entry.getKey() + "]\n" + entry.getValue() + "\n");
			}
			writer.close();
		}
	}
	
	public static boolean isTileSourceMetaInfoExist(File dir){
		return new File(dir, ".metainfo").exists() || new File(dir, "url").exists();
	}
	
	/**
	 * @param dir
	 * @return nonnull
	 */
	public static TileSourceTemplate createTileSourceTemplate(File dir) {
		// read metainfo file
		Map<String, String> metaInfo = readMetaInfoFile(dir);
		boolean ruleAcceptable = true;
		if (!metaInfo.isEmpty()){
			metaInfo.put("name", dir.getName());
			TileSourceTemplate template = createTileSourceTemplate(metaInfo);
			if (template != null){
				return template;
			}
			ruleAcceptable = false;
		}
		
		// try to find url
		String ext = findOneTile(dir);
		ext = ext == null ? ".jpg" : ext;
		String url = null;
			File readUrl = new File(dir, "url"); //$NON-NLS-1$
			try {
				if (readUrl.exists()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							new FileInputStream(readUrl), "UTF-8"));
					url = reader.readLine();
					url = TileSourceTemplate.normalizeUrl(url);
					reader.close();
				}
			} catch (IOException e) {
				log.debug("Error reading url " + dir.getName(), e); //$NON-NLS-1$
			}

		TileSourceTemplate template = new TileSourceManager.TileSourceTemplate(dir.getName(), url,
				ext, 18, 1, 256, 16, 20000); //$NON-NLS-1$
		template.setRuleAcceptable(ruleAcceptable);
		return template;
	}

	private static Map<String, String> readMetaInfoFile(File dir) {
		Map<String, String> keyValueMap = new LinkedHashMap<>();
		try {
			File metainfo = new File(dir, ".metainfo"); //$NON-NLS-1$
			if (metainfo.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream(metainfo), StandardCharsets.UTF_8));
				String line;
				String key = null;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("[")) {
						key = line.substring(1, line.length() - 1).toLowerCase();
					} else if (key != null && line.length() > 0) {
						keyValueMap.put(key, line);
						key = null;
					}
				}
				reader.close();
			}
		} catch (IOException e) {
			log.error("Error reading metainfo file " + dir.getAbsolutePath(), e);
		}
		return keyValueMap;
	}

	private static String findOneTile(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files == null) {
				return null;
			}
			for (File file : files) {
				if (file.isDirectory()) {
					String ext = findOneTile(file);
					if (ext != null) {
						return ext;
					}
				} else {
					String fileName = file.getName();
					if (fileName.endsWith(".tile")) {
						String substring = fileName.substring(0, fileName.length() - ".tile".length());
						int extInt = substring.lastIndexOf('.');
						if (extInt != -1) {
							return substring.substring(extInt, substring.length());
						}
					}
				}
			}
		}
		return null;
	}
	
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates() {
		java.util.List<TileSourceTemplate> list = new ArrayList<>();
		list.add(getMapnikSource());
		list.add(getMapillaryVectorSource());
		list.add(getMapillaryCacheSource());
		return list;
	}

	public static TileSourceTemplate getMapnikSource(){
		return MAPNIK_SOURCE;
	}

	public static TileSourceTemplate getMapillaryVectorSource() {
		return MAPILLARY_VECTOR_SOURCE;
	}

	public static TileSourceTemplate getMapillaryCacheSource() {
		return MAPILLARY_CACHE_SOURCE;
	}

	public static List<TileSourceTemplate> downloadTileSourceTemplates(String versionAsUrl, boolean https) {
		final List<TileSourceTemplate> templates = new ArrayList<>();
		try {
			URLConnection connection = NetworkUtils.getHttpURLConnection((https ? "https" : "http")
					+ "://download.osmand.net/tile_sources?" + versionAsUrl);
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(connection.getInputStream(), "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("tile_source")) {
						Map<String, String> attrs = new LinkedHashMap<String, String>();
						for(int i=0; i< parser.getAttributeCount(); i++) {
							attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
						}
						TileSourceTemplate template = createTileSourceTemplate(attrs);
						if (template != null) {
							templates.add(template);
						}
					}
				}
			}
		} catch (IOException | XmlPullParserException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		}
		return templates;
	}
	
	public static TileSourceTemplate createTileSourceTemplate(Map<String, String> attrs) {
		TileSourceTemplate template;
		String rule = attrs.get("rule");
		if (rule == null) {
			template = createSimpleTileSourceTemplate(attrs, false);
		} else if (RULE_TEMPLATE_1.equalsIgnoreCase(rule)) {
			template = createSimpleTileSourceTemplate(attrs, false);
		} else if (RULE_WMS.equalsIgnoreCase(rule)) {
			template = createWmsTileSourceTemplate(attrs);
		} else if (RULE_YANDEX_TRAFFIC.equalsIgnoreCase(rule)) {
			template = createSimpleTileSourceTemplate(attrs, true);
		} else {
			return null;
		}
		if (template != null){
			template.setRule(rule);
		}
		return template;
	}
	
	
	private static TileSourceTemplate createWmsTileSourceTemplate(Map<String, String> attributes) {
		String name = attributes.get("name");
		String layer = attributes.get("layer");
		String urlTemplate = attributes.get("url_template");
		
		if (name == null || urlTemplate == null || layer == null) {
			return null;
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		String randoms = attributes.get("randoms");
		urlTemplate = "http://whoots.mapwarper.net/tms/{0}/{1}/{2}/"+layer+"/"+urlTemplate;
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setRandoms(randoms);
		return templ;
	}

	private static TileSourceTemplate createSimpleTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}

		urlTemplate = TileSourceTemplate.normalizeUrl(urlTemplate);

		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		int expirationTime = parseInt(attributes, "expiration_time_minutes", -1);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		boolean ellipsoid = false;
		if (Boolean.parseBoolean(attributes.get("ellipsoid"))) {
			ellipsoid = true;
		}
		boolean invertedY = false;
		if (Boolean.parseBoolean(attributes.get("inverted_y"))) {
			invertedY = true;
		}
		String randoms = attributes.get("randoms");
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		if (attributes.get("referer") != null) {
			templ.setReferer(attributes.get("referer"));
		}
		if (attributes.get("user_agent") != null) {
			templ.setUserAgent(attributes.get("user_agent"));
		}
		if(expirationTime >= 0) {
			templ.setExpirationTimeMinutes(expirationTime);
		}
		templ.setEllipticYTile(ellipsoid);
		templ.setInvertedYTile(invertedY);
		templ.setRandoms(randoms);
		if (attributes.get("param_type") != null && attributes.get("param_min") != null
				&& attributes.get("param_step") != null && attributes.get("param_max") != null) {
			templ.setParamType(ParameterType.fromName(attributes.get("param_type")));
			try {
				templ.setParamMin(Long.parseLong(attributes.get("param_min")));
			} catch (NumberFormatException ignore) {
			}
			try {
				templ.setParamStep(Long.parseLong(attributes.get("param_step")));
			} catch (NumberFormatException ignore) {
			}
			try {
				templ.setParamMax(Long.parseLong(attributes.get("param_max")));
			} catch (NumberFormatException ignore) {
			}
		}
		return templ;
	}
}
