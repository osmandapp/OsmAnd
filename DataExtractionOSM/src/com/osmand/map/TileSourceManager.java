package com.osmand.map;

import java.text.MessageFormat;
import java.util.ArrayList;


public class TileSourceManager {
	
	public static class TileSourceTemplate implements ITileSource {
		private int maxZoom;
		private int minZoom;
		private String name;
		private int tileSize;
		private String urlToLoad;
		private String ext;
		private int avgSize;
		
		public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom, int tileSize,  int avgSize) {
			this.maxZoom = maxZoom;
			this.minZoom = minZoom;
			this.name = name;
			this.tileSize = tileSize;
			this.urlToLoad = urlToLoad;
			this.ext = ext;
			this.avgSize = avgSize;
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
			return MessageFormat.format(urlToLoad, zoom+"", x+"", y+"");
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
	
	static java.util.List<TileSourceTemplate> list;
	public static java.util.List<TileSourceTemplate> getKnownSourceTemplates(){
		if(list == null){
			list = new ArrayList<TileSourceTemplate>();
			list.add(getMapnikSource());
			list.add(getOsmaRenderSource());
			list.add(getCycleMapSource());
//			list.add(getAerialMapSource());
			list.add(getCloudMadeSource());
			list.add(getOpenPisteMapSource());
			list.add(getGoogleMapsSource());
			list.add(getGoogleMapsSatelliteSource());
			list.add(getGoogleMapsTerrainSource());
			list.add(getMicrosoftMapsSource());
			list.add(getMicrosoftEarthSource());
			list.add(getMicrosoftHybridSource());
			
		}
		return list;
		
	}
	
	
	
	
	public static TileSourceTemplate getMapnikSource(){
		return new TileSourceTemplate("Mapnik", "http://tile.openstreetmap.org/{0}/{1}/{2}.png", ".png", 18, 1, 256, 18000);
	}
	
	public static TileSourceTemplate getOsmaRenderSource(){
		return new TileSourceTemplate("OsmaRender", "http://tah.openstreetmap.org/Tiles/tile/{0}/{1}/{2}.png", ".png", 17, 1, 256, 18000);
	}
	
	public static TileSourceTemplate getCycleMapSource(){
		return new TileSourceTemplate("CycleMap", "http://b.andy.sandbox.cloudmade.com/tiles/cycle/{0}/{1}/{2}.png", ".png", 17, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getAerialMapSource(){
		return new TileSourceTemplate("OpenAerialMap", "http://tile.openaerialmap.org/tiles/1.0.0/openaerialmap-900913/{0}/{1}/{2}.jpg", ".jpg", 13, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getCloudMadeSource(){
		return new TileSourceTemplate("Cloudmade", "http://tile.cloudmade.com/7ded028e030c5929b28bf823486ce84f/1/256/{0}/{1}/{2}.png", ".png", 18, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getOpenPisteMapSource(){
		return new TileSourceTemplate("OpenPisteMap", "http://openpistemap.org/tiles/contours/{0}/{1}/{2}.png", ".png", 17, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getGoogleMapsSource(){
		return new TileSourceTemplate("GoogleMaps", "http://mt3.google.com/vt/v=w2.97&x={1}&y={2}&z={0}", ".png", 19, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getGoogleMapsSatelliteSource(){
		return new TileSourceTemplate("GoogleMaps Satellite", "http://khm1.google.com/kh/v=59&x={1}&y={2}&z={0}", ".jpg", 20, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getGoogleMapsTerrainSource(){
		return new TileSourceTemplate("GoogleMaps Terrain", "http://mt3.google.com/vt/v=w2p.111&hl=en&x={1}&y={2}&z={0}", ".jpg", 15, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getMicrosoftMapsSource(){
		return new MicrosoftTileSourceTemplate("Microsoft Maps", 'r', "png", ".png", 19, 1, 256, 18000);
	}
	
	public static TileSourceTemplate getMicrosoftEarthSource(){
		return new MicrosoftTileSourceTemplate("Microsoft Earth", 'a', "jpg", ".jpg", 19, 1, 256, 18000);
	}
	
	public static TileSourceTemplate getMicrosoftHybridSource(){
		return new MicrosoftTileSourceTemplate("Microsoft Hybrid", 'h', "jpg", ".jpg", 19, 1, 256, 18000);
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
		protected String urlBase = ".ortho.tiles.virtualearth.net/tiles/";
		protected String urlAppend = "?g=45";
		private final String tileType;

		public MicrosoftTileSourceTemplate(String name, char mapTypeChar , String type, 
				String ext, int maxZoom, int minZoom, int tileSize, int avgSize) {
			super(name, null, ext, maxZoom, minZoom, tileSize, avgSize);
			this.mapTypeChar = mapTypeChar;
			this.tileType = type;
		}
		
		
		@Override
		public String getUrlToLoad(int x, int y, int zoom) {
			String tileNum = encodeQuadTree(zoom, x, y);
//			serverNum = (serverNum + 1) % serverNumMax;
			return "http://" + mapTypeChar + serverNum + urlBase + mapTypeChar + tileNum + "."
					+ tileType + urlAppend;
			
		}
	}

	
}
