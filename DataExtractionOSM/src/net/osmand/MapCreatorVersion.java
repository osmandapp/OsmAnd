package net.osmand;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MapCreatorVersion {
	
	public static String APP_VERSION = "0.7.1"; //$NON-NLS-1$
	public static final String APP_DESCRIPTION = "alpha"; //$NON-NLS-1$

	public static final String APP_MAP_CREATOR_NAME = "OsmAndMapCreator"; //$NON-NLS-1$
	public static final String APP_MAP_CREATOR_VERSION = APP_MAP_CREATOR_NAME + " " + APP_VERSION; //$NON-NLS-1$
	public static final String APP_MAP_CREATOR_FULL_NAME = APP_MAP_CREATOR_NAME + " " + APP_VERSION + " " +APP_DESCRIPTION; //$NON-NLS-1$ //$NON-NLS-2$
	
	public static String getVersionAsURLParam() {
		try {
			return "osmandver=" + URLEncoder.encode(APP_VERSION + " " + APP_DESCRIPTION, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	} 
}
