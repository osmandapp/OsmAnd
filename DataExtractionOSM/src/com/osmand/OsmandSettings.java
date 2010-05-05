package com.osmand;

import com.osmand.map.ITileSource;

public class OsmandSettings {
	
	public static boolean useInternetToDownloadTiles = DefaultLauncherConstants.loadMissingImages;
	
	public static ITileSource tileSource = DefaultLauncherConstants.MAP_defaultTileSource;
	
	public static boolean showPoiOverMap = true;

}
