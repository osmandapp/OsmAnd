package com.osmand;

import com.osmand.map.ITileSource;

public class OsmandSettings {
	
	public static boolean useInternetToDownloadTiles = DefaultLauncherConstants.loadMissingImages;
	
	public static boolean showGPSLocationOnMap = DefaultLauncherConstants.showGPSCoordinates;
	
	public static ITileSource tileSource = DefaultLauncherConstants.MAP_defaultTileSource;

}
