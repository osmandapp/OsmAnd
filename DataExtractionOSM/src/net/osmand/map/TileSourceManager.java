package net.osmand.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

import net.osmand.Algoritms;
import net.osmand.osm.MapUtils;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;



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
		
		public TileSourceTemplate(File dir, String name, String urlToLoad) {
			this(name, urlToLoad, determineExt(dir,".jpg"), 18, 1, 256, 16, 20000); //$NON-NLS-1$
		}

		private static String determineExt(File dir, String defaultExt) {
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
							String substring = fileName.substring(0,fileName.length()-".tile".length());
							int extInt = substring.lastIndexOf(".");
							if (extInt != -1) {
								return substring.substring(extInt,substring.length());
							}
						}
					}
				}
			}
			return null;
		}

		// default constructor
		public TileSourceTemplate(String name, String urlToLoad) {
			this(name, urlToLoad, ".jpg", 18, 1, 256, 16, 20000); //$NON-NLS-1$
		}
		
		public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
			this.maxZoom = maxZoom;
			this.minZoom = minZoom;
			this.name = name;
			this.tileSize = tileSize;
			this.urlToLoad = urlToLoad;
			this.ext = ext;
			this.avgSize = avgSize;
			this.bitDensity = bitDensity;
		}

		@Override
		public int getBitDensity() {
			return bitDensity;
		}
		
		public int getAverageSize(){
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
		
		public String getTileFormat(){
			return ext;
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			// use int to string not format numbers! (non-nls)
			if(urlToLoad == null){
				return null;
			}
			return MessageFormat.format(urlToLoad, zoom+"", x+"", y+""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		
		public String getUrlTemplate(){
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
	
	static java.util.List<TileSourceTemplate> list;
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates(){
		if(list == null){
			list = new ArrayList<TileSourceTemplate>();
			list.add(getMapnikSource());
			list.add(getOsmaRenderSource());
			list.add(getCycleMapSource());
			list.add(getMapSurferSource());
			list.add(getNavigationDebugSource());
			
			list.add(getCloudMadeSource());
			list.add(getOpenPisteMapSource());
			list.add(getGoogleMapsSource());
			list.add(getGoogleMapsSatelliteSource());
			list.add(getGoogleMapsTerrainSource());
			
			list.add(getCykloatlasCzSource());
			list.add(getHikeBikeMapDeSource());
			
			list.add(getMicrosoftMapsSource());
			list.add(getMicrosoftEarthSource());
			list.add(getMicrosoftHybridSource());
			
			list.add(getEniroMapSource());
			list.add(getEniroAerialSource());
			list.add(getEniroNauticalSource());
			list.add(getStatkartTopoSource());
			list.add(getStatkartNauticalSource());
		}
		return list;
		
	}
	
	public static class EniroTileSourceTemplate extends TileSourceTemplate {  // special Eniro y-tile addressing
		public EniroTileSourceTemplate(String name, String urlToLoad, int maxZoom, int minZoom){
			super(name, urlToLoad, ".png", maxZoom, minZoom, 256, 32, 18000);
		}

		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			if(urlToLoad == null){
				return null;
			}
			y = (1 << zoom) - 1 - y;
			return MessageFormat.format(urlToLoad, zoom+"", x+"", y+"");
		}
	}
	
	public static class CykloatlasSourceTemplate extends TileSourceTemplate {

		public CykloatlasSourceTemplate(String name, String urlToLoad){
			super(name, urlToLoad, ".png", 15, 7, 256, 8, 26000); //$NON-NLS-1$
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
	
	
	public static TileSourceTemplate getMapnikSource(){
		return new TileSourceTemplate("Mapnik", "http://tile.openstreetmap.org/{0}/{1}/{2}.png", ".png", 18, 1, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getOsmaRenderSource(){
		return new TileSourceTemplate("OsmaRender", "http://tah.openstreetmap.org/Tiles/tile/{0}/{1}/{2}.png", ".png", 17, 1, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getCycleMapSource(){
//		return new TileSourceTemplate("CycleMap", "http://b.andy.sandbox.cloudmade.com/tiles/cycle/{0}/{1}/{2}.png", ".png", 17, 0, 256, 32, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		return new TileSourceTemplate("CycleMap", "http://b.tile.opencyclemap.org/cycle/{0}/{1}/{2}.png", ".png", 17, 0, 256, 32, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getCykloatlasCzSource(){
		return new CykloatlasSourceTemplate("Cykloatlas CZ", "http://services.tmapserver.cz/tiles/gm/shc/{0}/{1}/{2}.png");  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	public static TileSourceTemplate getHikeBikeMapDeSource(){
		return new TileSourceTemplate("HikeBikeMap DE", "http://toolserver.org/tiles/hikebike/{0}/{1}/{2}.png", ".png", 17, 0, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public static TileSourceTemplate getAerialMapSource(){
		return new TileSourceTemplate("OpenAerialMap", "http://tile.openaerialmap.org/tiles/1.0.0/openaerialmap-900913/{0}/{1}/{2}.jpg", ".jpg", 13, 0, 256, 8, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getCloudMadeSource(){
		return new TileSourceTemplate("Cloudmade", "http://tile.cloudmade.com/7ded028e030c5929b28bf823486ce84f/1/256/{0}/{1}/{2}.png", ".png", 18, 0, 256, 16, 18000);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getMapSurferSource(){
		return new TileSourceTemplate("MapSurfer", "http://tiles1.mapsurfer.net/tms_r.ashx?z={0}&x={1}&y={2}", ".png", 19, 0, 256, 16, 18000);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getNavigationDebugSource(){
		return new TileSourceTemplate("NavigationDebug", "http://ec2-184-73-15-218.compute-1.amazonaws.com/6700/256/{0}/{1}/{2}.png", ".png", 18, 0, 256, 16, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	public static TileSourceTemplate getOpenPisteMapSource(){
		return new TileSourceTemplate("OpenPisteMap", "http://openpistemap.org/tiles/contours/{0}/{1}/{2}.png", ".png", 17, 0, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getGoogleMapsSource(){
		return new TileSourceTemplate("GoogleMaps", "http://mt3.google.com/vt/v=w2.97&x={1}&y={2}&z={0}", ".png", 19, 0, 256, 16, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getGoogleMapsSatelliteSource(){
		return new TileSourceTemplate("GoogleMaps Satellite", "http://khm1.google.com/kh/v=65&x={1}&y={2}&z={0}", ".jpg", 20, 0, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getGoogleMapsTerrainSource(){
		return new TileSourceTemplate("GoogleMaps Terrain", "http://mt3.google.com/vt/v=w2p.111&hl=en&x={1}&y={2}&z={0}", ".jpg", 15, 0, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	// not working
	public static TileSourceTemplate getGoogleMapsHybridSource(){
//		Google Map Earth + Overlay
//		GoogleEarthMapsOverlay.url=http://mt{$servernum}.google.com/vt/lyrs=h@130&hl={$lang}&x={$x}&y={$y}&z={$z}
		return new TileSourceTemplate("Google Hybrid", "http://khm1.google.com/kh/v=59&x={1}&y={2}&z={0}", ".jpg", 20, 0, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	
	// wrong projection ???
	public static TileSourceTemplate getYandexMapSource(){
		return new TileSourceTemplate("Yandex map", "http://vec01.maps.yandex.ru/tiles?l=map&v=2.15.0&x={1}&y={2}&z={0}", ".jpg", 18, 0, 256, 16, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getYandexSatSource(){
		return new TileSourceTemplate("Yandex Satellite", "http://sat01.maps.yandex.ru/tiles?l=sat&v=1.19.0&x={1}&y={2}&z={0}", ".jpg", 18, 0, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	public static TileSourceTemplate getMicrosoftMapsSource(){
		return new MicrosoftTileSourceTemplate("Microsoft Maps", 'r', "png", ".png", 19, 1, 256, 16, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getMicrosoftEarthSource(){
		return new MicrosoftTileSourceTemplate("Microsoft Earth", 'a', "jpg", ".jpg", 19, 1, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getMicrosoftHybridSource(){
		return new MicrosoftTileSourceTemplate("Microsoft Hybrid", 'h', "jpg", ".jpg", 19, 1, 256, 32, 18000); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static TileSourceTemplate getEniroMapSource(){
		return new EniroTileSourceTemplate("Eniro Map NO SE FI DK PL", "http://map.eniro.com/geowebcache/service/tms1.0.0/map/{0}/{1}/{2}.png", 20, 2);
	}
	
	public static TileSourceTemplate getEniroAerialSource(){
		return new EniroTileSourceTemplate("Eniro Aerial NO SE DK", "http://map.eniro.com/geowebcache/service/tms1.0.0/aerial/{0}/{1}/{2}.png", 19, 2);
	}
	
	public static TileSourceTemplate getEniroNauticalSource(){
		return new EniroTileSourceTemplate("Eniro Nautical NO SE", "http://map.eniro.com/geowebcache/service/tms1.0.0/nautical/{0}/{1}/{2}.png", 16, 5);
	}
	
	public static TileSourceTemplate getStatkartTopoSource(){
		return new TileSourceTemplate("Statkart Topo NO", "http://opencache.statkart.no/gatekeeper/gk/gk.open_gmaps?layers=topo2&zoom={0}&x={1}&y={2}", ".png", 17, 5, 256, 32, 18000);
	}
	
	public static TileSourceTemplate getStatkartNauticalSource(){
		return new TileSourceTemplate("Statkart Nautical NO", "http://opencache.statkart.no/gatekeeper/gk/gk.open_gmaps?layers=sjo_hovedkart2&zoom={0}&x={1}&y={2}", ".png", 14, 3, 256, 32, 18000);
	}
	
	
	// WMS layers : http://whoots.mapwarper.net/tms/{$z}/{$x}/{$y}/ {layer}/{Path}
	// 1. Landsat http://onearth.jpl.nasa.gov/wms.cgi global_mosaic (NOT WORK)
	// 2. Genshtab http://wms.latlon.org gshtab
	
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

	
}
