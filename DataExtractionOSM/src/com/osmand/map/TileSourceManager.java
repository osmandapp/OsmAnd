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
		return new TileSourceTemplate("GoogleMaps Satellite", "http://khm1.google.com/kh/v=37&x={1}&y={2}&z={0}", ".png", 19, 0, 256, 18000);
	}
	
	public static TileSourceTemplate getGoogleMapsTerrainSource(){
		return new TileSourceTemplate("GoogleMaps Terrain", "http://mt3.google.com/mt/v=w2p.87&x={1}&y={2}&z={0}", ".png", 15, 0, 256, 18000);
	}
	
}
