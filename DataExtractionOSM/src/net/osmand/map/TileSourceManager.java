package net.osmand.map;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import bsh.Interpreter;


public class TileSourceManager {
	private static final Log log = LogUtil.getLog(TileSourceManager.class);
	private static final String RULE_BEANSHELL = "beanshell";
	public static final String RULE_YANDEX_TRAFFIC = "yandex_traffic";
	private static final String RULE_WMS = "wms_tile";

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
		private boolean isRuleAcceptable = true;

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
		
		public boolean isRuleAcceptable() {
			return isRuleAcceptable;
		}
		
		public void setRuleAcceptable(boolean isRuleAcceptable) {
			this.isRuleAcceptable = isRuleAcceptable;
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
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static void createMetaInfoFile(File dir, TileSourceTemplate tm, boolean override) throws IOException {
		File metainfo = new File(dir, ".metainfo"); //$NON-NLS-1$
		Map<String, String> properties = new LinkedHashMap<String, String>();
		if (tm instanceof BeanShellTileSourceTemplate) {
			properties.put("rule", RULE_BEANSHELL);
		}
		if (tm.getUrlTemplate() == null) {
			return;
		}
		properties.put("url_template", tm.getUrlTemplate());

		properties.put("ext", tm.getTileFormat());
		properties.put("min_zoom", tm.getMinimumZoomSupported() + "");
		properties.put("max_zoom", tm.getMaximumZoomSupported() + "");
		properties.put("tile_size", tm.getTileSize() + "");
		properties.put("img_density", tm.getBitDensity() + "");
		properties.put("avg_img_size", tm.getAverageSize() + "");

		if (tm.isEllipticYTile()) {
			properties.put("ellipsoid", tm.isEllipticYTile() + "");
		}
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metainfo)));
		for (String key : properties.keySet()) {
			writer.write("[" + key + "]\n" + properties.get(key) + "\n");
		}
		writer.close();
	}
	
	public static boolean isTileSourceMetaInfoExist(File dir){
		return new File(dir, ".metainfo").exists() || new File(dir, "url").exists();
	}
	
	/**
	 * @param dir
	 * @return doesn't return null 
	 */
	public static TileSourceTemplate createTileSourceTemplate(File dir) {
		// read metainfo file
		Map<String, String> metaInfo = readMetaInfoFile(dir);
		boolean ruleAcceptable = true;
		if(!metaInfo.isEmpty()){
			metaInfo.put("name", dir.getName());
			TileSourceTemplate template = createTileSourceTemplate(metaInfo);
			if(template != null){
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

		TileSourceTemplate template = new TileSourceManager.TileSourceTemplate(dir.getName(), url,
				ext, 18, 1, 256, 16, 20000); //$NON-NLS-1$
		template.setRuleAcceptable(ruleAcceptable);
		return template;
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
	
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates() {
		java.util.List<TileSourceTemplate> list = new ArrayList<TileSourceTemplate>();
		list.add(getMapnikSource());
		list.add(getCycleMapSource());
		return list;

	}

	public static TileSourceTemplate getMapnikSource(){
		return new TileSourceTemplate("Mapnik", "http://tile.openstreetmap.org/{0}/{1}/{2}.png", ".png", 18, 1, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public static TileSourceTemplate getCycleMapSource(){
		return new TileSourceTemplate("CycleMap", "http://b.tile.opencyclemap.org/cycle/{0}/{1}/{2}.png", ".png", 17, 0, 256, 32, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	
	public static List<TileSourceTemplate> downloadTileSourceTemplates() {
		try {
			URLConnection connection = new URL("http://download.osmand.net//tile_sources.php").openConnection();
			return createTileSourceTemplates(connection.getInputStream());
			
		} catch (IOException e) {
			log.error("Exception while downloading tile sources", e);
			return null;
		}
	}
	
	public static List<TileSourceTemplate> getLocalTileSourceTemplates(File tilesDir) {
		try {
			File customTiles = new File(tilesDir, "custom_tile_sources.xml");
			return createTileSourceTemplates(new FileInputStream(customTiles));
			
		} catch (FileNotFoundException e) {
			log.info("Geting local tile sources: No custom file specified (" + tilesDir.getAbsolutePath() + File.separator + "custom_tile_sources.xml" + ")");
			return null;
		}
	}

	/**
	 * Create tile source template list from an input stream
	 * 
	 * @param inputStream
	 * @return tile source template list
	 */
	private static List<TileSourceTemplate> createTileSourceTemplates(InputStream inputStream) {
		final List<TileSourceTemplate> templates = new ArrayList<TileSourceTemplate>();
		try {
			
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(inputStream, new DefaultHandler(){
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					String name = saxParser.isNamespaceAware() ? localName : qName;
					Map<String, String> attrs = new LinkedHashMap<String, String>();
					if(name.equals("tile_source")){
						attrs.clear();
						for(int i=0; i< attributes.getLength(); i++){
							String local = attributes.getLocalName(i);
							if(local != null){
								attrs.put(local, attributes.getValue(i));
							} else if(attributes.getQName(i) != null){
								attrs.put(attributes.getQName(i), attributes.getValue(i));
							} else {
								return;
							}
						}
						TileSourceTemplate template = createTileSourceTemplate(attrs);
						if(template != null){
							templates.add(template);
						}
					}
				}
			});
		} catch (IOException e) {
			log.error("Exception while creating tile sources", e);
			return null;
		} catch (SAXException e) {
			log.error("Exception while creating tile sources", e);
			return null;
		} catch (ParserConfigurationException e) {
			log.error("Exception while creating tile sources", e);
			return null;
		}
		return templates;
	}
	
	private static TileSourceTemplate createTileSourceTemplate(Map<String, String> attrs) {
		TileSourceTemplate template = null;
		String rule = attrs.get("rule");
		if(rule == null){
			template = createSimpleTileSourceTemplate(attrs, false);
		} else if(RULE_BEANSHELL.equalsIgnoreCase(rule)){
			template = createBeanshellTileSourceTemplate(attrs);
		} else if (RULE_WMS.equalsIgnoreCase(rule)) {
			template = createWmsTileSourceTemplate(attrs);
		} else if (RULE_YANDEX_TRAFFIC.equalsIgnoreCase(rule)) {
			template = createSimpleTileSourceTemplate(attrs, true);
		} else {
			return null;
		}
		if(template != null){
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
		urlTemplate = " http://whoots.mapwarper.net/tms/{0}/{1}/{2}/"+layer+"/"+urlTemplate;
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		return templ;
	}
	


	private static TileSourceTemplate createSimpleTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || (urlTemplate == null && !ignoreTemplate)) {
			return null;
		}
		if(urlTemplate != null){
			urlTemplate.replace("${x}", "{1}").replace("${y}", "{2}").replace("${z}", "{0}");
		}
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
		TileSourceTemplate templ = new TileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setEllipticYTile(ellipsoid);
		return templ;
	}
	
	private static TileSourceTemplate createBeanshellTileSourceTemplate(Map<String, String> attributes) {
		String name = attributes.get("name");
		String urlTemplate = attributes.get("url_template");
		if (name == null || urlTemplate == null) {
			return null;
		}
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
		templ = new BeanShellTileSourceTemplate(name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize);
		templ.setEllipticYTile(ellipsoid);
		return templ;
	}
	
	public static class BeanShellTileSourceTemplate extends TileSourceTemplate {

		Interpreter bshInterpreter;
		
		public BeanShellTileSourceTemplate(String name, String urlToLoad, String ext,
				int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			super(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
			bshInterpreter = new Interpreter();
			try {
				bshInterpreter.eval(urlToLoad);
				bshInterpreter.getClassManager().setClassLoader(new ClassLoader() {
					@Override
					public URL getResource(String resName) {
						return null;
					}
					
					@Override
					public InputStream getResourceAsStream(String resName) {
						return null;
					}
					
					@Override
					public Class<?> loadClass(String className) throws ClassNotFoundException {
						throw new ClassNotFoundException("Error requesting " + className);
					}
				});
			} catch (bsh.EvalError e) {
				log.error("Error executing the map init script " + urlToLoad, e);
			}
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			try {
				return (String) bshInterpreter.eval(String.format("getTileUrl(%d,%d,%d);", zoom, x, y));
			} catch (bsh.EvalError e) {
				log.error(e.getMessage(), e);
				return null;
			}
		}
		
	}

	
}
