package net.osmand.map;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.LogUtil;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class TileSourceManager {
	private static final Log log = LogUtil.getLog(TileSourceManager.class);
	private static final String RULE_CYCLOATLAS = "cykloatlas_cz";
	private static final String RULE_WMS = "wms_tile";
	private static final String RULE_MICROSOFT = "microsoft";
	
	

	public static class TileSourceTemplate implements ITileSource {
		private int maxZoom;
		private int minZoom;
		private String name;
		protected int tileSize;
		protected String urlToLoad;
		protected String ext;
		private int avgSize;
		private int bitDensity;
		private boolean ellipticYTile;
		private String rule;

		public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom, int tileSize, int bitDensity,
				int avgSize) {
			this.maxZoom = maxZoom;
			this.minZoom = minZoom;
			this.name = name;
			this.tileSize = tileSize;
			this.urlToLoad = urlToLoad;
			this.ext = ext;
			this.avgSize = avgSize;
			this.bitDensity = bitDensity;
		}

		public void setEllipticYTile(boolean ellipticYTile) {
			this.ellipticYTile = ellipticYTile;
		}

		@Override
		public boolean isEllipticYTile() {
			return ellipticYTile;
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

		@Override
		public int getTileSize() {
			return tileSize;
		}

		public String getTileFormat() {
			return ext;
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			// use int to string not format numbers! (non-nls)
			if (urlToLoad == null) {
				return null;
			}
			return MessageFormat.format(urlToLoad, zoom + "", x + "", y + ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	}
	
	private static Map<String, String> readMetaInfoFile(File dir) {
		Map<String, String> keyValueMap = new LinkedHashMap<String, String>();
		try {

			File metainfo = new File(dir, ".metainfo"); //$NON-NLS-1$
			if (metainfo.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream(metainfo), "UTF-8")); //$NON-NLS-1$
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
			}
		} catch (IOException e) {
			log.error("Error reading metainfo file " + dir.getAbsolutePath(), e);
		}
		return keyValueMap;
	}	
	
	private static int parseInt(Map<String, String> attributes, String value, int def){
		String val = attributes.get(value);
		if(val == null){
			return def;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static void createMetaInfoFile(File dir, TileSourceTemplate tm, boolean override) throws IOException{
		File metainfo = new File(dir, ".metainfo"); //$NON-NLS-1$
		Map<String, String> properties = new LinkedHashMap<String, String>();
		if(tm instanceof MicrosoftTileSourceTemplate){
			properties.put("rule", RULE_MICROSOFT);
			properties.put("map_type", ((MicrosoftTileSourceTemplate) tm).getMapTypeChar()+"");
			properties.put("map_ext", ((MicrosoftTileSourceTemplate) tm).getTileType());
		} else {
			if(tm instanceof CykloatlasSourceTemplate){
				properties.put("rule", RULE_CYCLOATLAS);
			}
			if(tm.getUrlTemplate() == null){
				return;
			}
			properties.put("url_template", tm.getUrlTemplate());
		}
		
		properties.put("ext", tm.getTileFormat());
		properties.put("min_zoom", tm.getMinimumZoomSupported()+"");
		properties.put("max_zoom", tm.getMaximumZoomSupported()+"");
		properties.put("tile_size", tm.getTileSize()+"");
		properties.put("img_density", tm.getBitDensity()+"");
		properties.put("avg_img_size", tm.getAverageSize()+"");
		
		if(tm.isEllipticYTile()){
			properties.put("ellipsoid", tm.isEllipticYTile()+"");
		}
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metainfo)));
		for(String key : properties.keySet()){
			writer.write("["+key+"]\n"+properties.get(key)+"\n");
		}
		writer.close();
	}
	
	public static boolean isTileSourceMetaInfoExist(File dir){
		return new File(dir, ".metainfo").exists() || new File(dir, "url").exists();
	}
	
	public static TileSourceTemplate createTileSourceTemplate(File dir) {
		// read metainfo file
		Map<String, String> metaInfo = readMetaInfoFile(dir);
		if(!metaInfo.isEmpty()){
			metaInfo.put("name", dir.getName());
			TileSourceTemplate template = createTileSourceTemplate(metaInfo);
			return template;
		}
		
		// try to find url
		String ext = findOneTile(dir);
		ext = ext == null ? ".jpg" : ext;
		String url = null;
			File readUrl = new File(dir, "url"); //$NON-NLS-1$
			try {
				if (readUrl.exists()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							new FileInputStream(readUrl), "UTF-8")); //$NON-NLS-1$
					url = reader.readLine();
					url = url.replaceAll(Pattern.quote("{$z}"), "{0}"); //$NON-NLS-1$ //$NON-NLS-2$
					url = url.replaceAll(Pattern.quote("{$x}"), "{1}"); //$NON-NLS-1$//$NON-NLS-2$
					url = url.replaceAll(Pattern.quote("{$y}"), "{2}"); //$NON-NLS-1$ //$NON-NLS-2$
					reader.close();
				}
			} catch (IOException e) {
				log.debug("Error reading url " + dir.getName(), e); //$NON-NLS-1$
			}

		return new TileSourceManager.TileSourceTemplate(dir.getName(), url,
				ext, 18, 1, 256, 16, 20000); //$NON-NLS-1$
	}

	private static String findOneTile(File dir) {
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					String ext = findOneTile(file);
					if (ext != null) {
						return ext;
					}
				} else {
					String fileName = file.getName();
					if (fileName.endsWith(".tile")) {
						String substring = fileName.substring(0, fileName.length() - ".tile".length());
						int extInt = substring.lastIndexOf(".");
						if (extInt != -1) {
							return substring.substring(extInt, substring.length());
						}
					}
				}
			}
		}
		return null;
	}
	
	
//	public static class WMSSourceTemplate extends TileSourceTemplate {
//
//		public WMSSourceTemplate(String name, String wmsUrl) {
//			super("WMS " + name, wmsUrl, ".jpg", 18, 3, 256, 16, 20000); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		
//		@Override
//		public String getUrlToLoad(int x, int y, int zoom) {
//			double yEnd = MapUtils.getLatitudeFromTile(zoom, y + 1);
//			double yStart = MapUtils.getLatitudeFromTile(zoom, y );
//			double xStart = MapUtils.getLongitudeFromTile(zoom, x);
//			double xEnd = MapUtils.getLongitudeFromTile(zoom, x + 1);
//			StringBuilder load = new StringBuilder();
//			load.append(urlToLoad).append("bbox=").append(xStart).append(','). //$NON-NLS-1$
//				 append(yEnd).append(',').append(xEnd).append(',').append(yStart);
//			load.append("&srs=EPSG:4326").append("&width=").append(tileSize).append("&height=").append(tileSize); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//			return load.toString();
//		}
//		
//	}

	
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates() {
		java.util.List<TileSourceTemplate> list = new ArrayList<TileSourceTemplate>();
		list.add(getMapnikSource());
		list.add(getOsmaRenderSource());
		list.add(getCycleMapSource());
		list.add(getCloudMadeSource());

		return list;

	}
	
	public static TileSourceTemplate getMapnikSource(){
		return new TileSourceTemplate("Mapnik", "http://tile.openstreetmap.org/{0}/{1}/{2}.png", ".png", 18, 1, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getOsmaRenderSource(){
		return new TileSourceTemplate("OsmaRender", "http://tah.openstreetmap.org/Tiles/tile/{0}/{1}/{2}.png", ".png", 17, 1, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getCycleMapSource(){
		return new TileSourceTemplate("CycleMap", "http://b.tile.opencyclemap.org/cycle/{0}/{1}/{2}.png", ".png", 17, 0, 256, 32, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
		
	public static TileSourceTemplate getCloudMadeSource(){
		return new TileSourceTemplate("Cloudmade", "http://tile.cloudmade.com/7ded028e030c5929b28bf823486ce84f/1/256/{0}/{1}/{2}.png", ".png", 18, 0, 256, 16, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	public static List<TileSourceTemplate> downloadTileSourceTemplates() {
		final List<TileSourceTemplate> templates = new ArrayList<TileSourceTemplate>();
		try {
			URLConnection connection = new URL("http://download.osmand.net/tile_sources.php").openConnection();
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(connection.getInputStream(), new DefaultHandler(){
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					Map<String, String> attrs = new LinkedHashMap<String, String>();
					if(qName.equals("tile_source")){
						attrs.clear();
						for(int i=0; i< attributes.getLength(); i++){
							attrs.put(attributes.getQName(i), attributes.getValue(i));
						}
						TileSourceTemplate template = createTileSourceTemplate(attrs);
						if(template != null){
							templates.add(template);
						}
					}
				}

				
				
			});
		} catch (IOException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		} catch (SAXException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		} catch (ParserConfigurationException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		}
		return templates;
	}
	
	private static TileSourceTemplate createTileSourceTemplate(Map<String, String> attrs) {
		TileSourceTemplate template = null;
		String rule = attrs.get("rule");
		if(rule == null){
			template = createSimpleTileSourceTemplate(attrs, false);
		} else if(RULE_CYCLOATLAS.equalsIgnoreCase(rule)){
			template = createSimpleTileSourceTemplate(attrs, true);
		} else if (RULE_MICROSOFT.equalsIgnoreCase(rule)) {
			template = createMicrofsoftTileSourceTemplate(attrs);
		} else if (RULE_WMS.equalsIgnoreCase(rule)) {
			template = createWmsTileSourceTemplate(attrs);
		} else {
			// TODO rule == yandex_traffic
		}
		if(template != null){
			template.setRule(rule);
		}
		return template;
	}

	private static TileSourceTemplate createSimpleTileSourceTemplate(Map<String, String> attributes, boolean cycloAtlas) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || urlTemplate == null) {
			return null;
		}
		urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		boolean ellipsoid = false;
		if (Boolean.parseBoolean(attributes.get("ellipsoid"))) {
			ellipsoid = true;
		}
		TileSourceTemplate templ;
		if (cycloAtlas) {
			templ = new CykloatlasSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		} else {
			templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		}
		templ.setEllipticYTile(ellipsoid);
		return templ;
	}
	
	private static TileSourceTemplate createMicrofsoftTileSourceTemplate(Map<String, String> attributes) {
		String name = attributes.get("name");
		String mapType = attributes.get("map_type");
		String mapExt = attributes.get("map_ext");
		
		if (name == null || mapExt == null || mapType == null) {
			return null;
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new MicrosoftTileSourceTemplate(name, mapType.charAt(0), mapType, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		return templ;
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
		urlTemplate = " http://whoots.mapwarper.net/tms/{0}/{1}/{2}/"+layer+"/"+urlTemplate;
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		return templ;
	}
	
	
	

	
	protected static final char[] NUM_CHAR = { '0', '1', '2', '3' };

	/**
	 * See: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * @param zoom
	 * @param tilex
	 * @param tiley
	 * @return quadtree encoded tile number
	 * 
	 */
	public static String encodeQuadTree(int zoom, int tilex, int tiley) {
		char[] tileNum = new char[zoom];
		for (int i = zoom - 1; i >= 0; i--) {
			// Binary encoding using ones for tilex and twos for tiley. if a bit
			// is set in tilex and tiley we get a three.
			int num = (tilex % 2) | ((tiley % 2) << 1);
			tileNum[i] = NUM_CHAR[num];
			tilex >>= 1;
			tiley >>= 1;
		}
		return new String(tileNum);
	}
	
	public static class MicrosoftTileSourceTemplate extends TileSourceTemplate {

		private final char mapTypeChar;
		int serverNum = 0; // 0..3
		protected String urlBase = ".ortho.tiles.virtualearth.net/tiles/"; //$NON-NLS-1$
		protected String urlAppend = "?g=45"; //$NON-NLS-1$
		private final String tileType;

		public MicrosoftTileSourceTemplate(String name, char mapTypeChar , String type, 
				String ext, int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, null, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
			this.mapTypeChar = mapTypeChar;
			this.tileType = type;
		}
		
		public char getMapTypeChar() {
			return mapTypeChar;
		}
		
		public String getTileType() {
			return tileType;
		}
		
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			String tileNum = encodeQuadTree(zoom, x, y);
//			serverNum = (serverNum + 1) % serverNumMax;
			return "http://" + mapTypeChar + serverNum + urlBase + mapTypeChar + tileNum + "." //$NON-NLS-1$ //$NON-NLS-2$
					+ tileType + urlAppend;
			
		}
		
		@Override
		public boolean couldBeDownloadedFromInternet() {
			return true;
		}
		
	}
	
	public static class CykloatlasSourceTemplate extends TileSourceTemplate {

		public CykloatlasSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom, int tileSize, int bitDensity,
				int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			String z = Integer.toString(zoom);
			// use int to string not format numbers! (non-nls)
			if(urlToLoad == null){
				return null;
			}
			if (zoom >= 13)
				z += "c";
			return MessageFormat.format(urlToLoad, z, x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
	}

	
}
