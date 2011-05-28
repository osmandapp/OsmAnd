package net.osmand.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.Algoritms;
import net.osmand.osm.MapUtils;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class TileSourceManager {
	private static final Log log = LogUtil.getLog(TileSourceManager.class);

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
	
	public static String determineExtOfTiles(File dir, String defaultExt) {
		String foundExt = findOneTile(dir);
		return foundExt == null ? defaultExt : foundExt;
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
	
	public static java.util.List<TileSourceTemplate> getUserDefinedTemplates(File tilesDir){
		java.util.List<TileSourceTemplate> ts = new ArrayList<TileSourceTemplate>();
		if (tilesDir != null) {
			File[] listFiles = tilesDir.listFiles();
			if (listFiles != null) {
				for (File f : listFiles) {
					File ch = new File(f, "url"); //$NON-NLS-1$
					if (f.isDirectory() && ch.exists()) {
						try {
							BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(ch), "UTF-8")); //$NON-NLS-1$
							String url = read.readLine();
							read.close();
							if (!Algoritms.isEmpty(url)) {
								url = url.replaceAll(Pattern.quote("{$x}"), "{1}"); //$NON-NLS-1$ //$NON-NLS-2$
								url = url.replaceAll(Pattern.quote("{$z}"), "{0}"); //$NON-NLS-1$//$NON-NLS-2$
								url = url.replaceAll(Pattern.quote("{$y}"), "{2}"); //$NON-NLS-1$ //$NON-NLS-2$
								TileSourceTemplate t = new TileSourceTemplate(f.getName(), url, ".jpg", 18, 1, 256, 16, 20000); //$NON-NLS-1$
								ts.add(t);
							}
						} catch (IOException e) {
							log.info("Mailformed dir " + f.getName(), e); //$NON-NLS-1$
						}

					}
				}
			}
		}
		return ts;
	}
	
	public static class WMSSourceTemplate extends TileSourceTemplate {

		public WMSSourceTemplate(String name, String wmsUrl) {
			super("WMS " + name, wmsUrl, ".jpg", 18, 3, 256, 16, 20000); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			double yEnd = MapUtils.getLatitudeFromTile(zoom, y + 1);
			double yStart = MapUtils.getLatitudeFromTile(zoom, y );
			double xStart = MapUtils.getLongitudeFromTile(zoom, x);
			double xEnd = MapUtils.getLongitudeFromTile(zoom, x + 1);
			StringBuilder load = new StringBuilder();
			load.append(urlToLoad).append("bbox=").append(xStart).append(','). //$NON-NLS-1$
				 append(yEnd).append(',').append(xEnd).append(',').append(yStart);
			load.append("&srs=EPSG:4326").append("&width=").append(tileSize).append("&height=").append(tileSize); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return load.toString();
		}
		
	}

	
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates(boolean download) {
		java.util.List<TileSourceTemplate> list = new ArrayList<TileSourceTemplate>();
		list.add(getMapnikSource());
		list.add(getOsmaRenderSource());
		list.add(getCycleMapSource());
		list.add(getCloudMadeSource());

		if (download) {
			List<TileSourceTemplate> downloaded = downloadTileSourceTemplates();
			if(downloaded != null){
				list.addAll(downloaded);
			}
		}

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
	private static int parseInt(Attributes attributes, String value, int def){
		String val = attributes.getValue(value);
		if(val == null){
			return def;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	public static List<TileSourceTemplate> downloadTileSourceTemplates() {
		final List<TileSourceTemplate> templates = new ArrayList<TileSourceTemplate>();
		try {
			URLConnection connection = new URL("http://download.osmand.net/tile_sources.php").openConnection();
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(connection.getInputStream(), new DefaultHandler(){
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					if(qName.equals("tile_source")){
						TileSourceTemplate template = null;
						String rule = attributes.getValue("rule");
						if(rule == null){
							template = createTileSourceTemplate(attributes, false);
						} else if("cykloatlas_cz".equalsIgnoreCase(rule)){
							template = createTileSourceTemplate(attributes, true);
						} else if ("microsoft".equalsIgnoreCase(rule)) {
							template = createMicrofsoftTileSourceTemplate(attributes);
						} else if ("wms_tile".equalsIgnoreCase(rule)) {
							template = createWmsTileSourceTemplate(attributes);
						} else {
							// TODO rule == yandex_traffic
						}
						
						if(template != null){
							template.setRule(rule);
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
	

	private static TileSourceTemplate createTileSourceTemplate(Attributes attributes, boolean cycloAtlas) {
		String name = attributes.getValue("name");
		String urlTemplate = attributes.getValue("url_template");
		if (name == null || urlTemplate == null) {
			return null;
		}
		urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.getValue("ext") == null ? ".jpg" : attributes.getValue("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		boolean ellipsoid = false;
		if (Boolean.parseBoolean(attributes.getValue("ellipsoid"))) {
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
	
	private static TileSourceTemplate createMicrofsoftTileSourceTemplate(Attributes attributes) {
		String name = attributes.getValue("name");
		String mapType = attributes.getValue("map_type");
		String mapExt = attributes.getValue("map_ext");
		
		if (name == null || mapExt == null || mapType == null) {
			return null;
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.getValue("ext") == null ? ".jpg" : attributes.getValue("ext");
		int bitDensity = parseInt(attributes, "img_density", 16);
		int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
		TileSourceTemplate templ = new MicrosoftTileSourceTemplate(name, mapType.charAt(0), mapType, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		return templ;
	}
	
	private static TileSourceTemplate createWmsTileSourceTemplate(Attributes attributes) {
		String name = attributes.getValue("name");
		String layer = attributes.getValue("layer");
		String urlTemplate = attributes.getValue("url_template");
		
		if (name == null || urlTemplate == null || layer == null) {
			return null;
		}
		int maxZoom = parseInt(attributes, "max_zoom", 18);
		int minZoom = parseInt(attributes, "min_zoom", 5);
		int tileSize = parseInt(attributes, "tile_size", 256);
		String ext = attributes.getValue("ext") == null ? ".jpg" : attributes.getValue("ext");
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
