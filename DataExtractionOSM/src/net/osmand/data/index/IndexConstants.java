package net.osmand.data.index;


public class IndexConstants {
	
	// Important : Every time you change schema of db upgrade version!!! 
	// If you want that new application support old index : put upgrade code in android app ResourceManager
	
	
	public final static int POI_TABLE_VERSION = 1;
	public final static int BINARY_MAP_VERSION = 1; // starts with 1
	public final static int VOICE_VERSION = 0;
	
	// these indexes are deprecated 
	public final static int TRANSPORT_TABLE_VERSION = 0;
	public final static int ADDRESS_TABLE_VERSION = 1;
	
	
	public static final String POI_INDEX_DIR = "POI/"; //$NON-NLS-1$
	public static final String ADDRESS_INDEX_DIR = "Address/"; //$NON-NLS-1$
	public static final String VOICE_INDEX_DIR = "voice/"; //$NON-NLS-1$
	public static final String TRANSPORT_INDEX_DIR = "Transport/"; //$NON-NLS-1$
	public static final String RENDERERS_DIR = "rendering/"; //$NON-NLS-1$
	
	public static final String POI_INDEX_EXT = ".poi.odb"; //$NON-NLS-1$
	public static final String ADDRESS_INDEX_EXT = ".addr.odb"; //$NON-NLS-1$
	public static final String TRANSPORT_INDEX_EXT = ".trans.odb"; //$NON-NLS-1$
	public static final String BINARY_MAP_INDEX_EXT = ".obf"; //$NON-NLS-1$
	
	public static final String POI_INDEX_EXT_ZIP = ".poi.zip"; //$NON-NLS-1$
	public static final String ADDRESS_INDEX_EXT_ZIP = ".addr.zip"; //$NON-NLS-1$
	public static final String TRANSPORT_INDEX_EXT_ZIP = ".trans.zip"; //$NON-NLS-1$
	public static final String VOICE_INDEX_EXT_ZIP = ".voice.zip"; //$NON-NLS-1$
	public static final String BINARY_MAP_INDEX_EXT_ZIP = ".obf.zip"; //$NON-NLS-1$
	
	public static final String RENDERER_INDEX_EXT = ".render.xml"; //$NON-NLS-1$
	

	public final static String STREET_NODE_TABLE = "street_node"; //$NON-NLS-1$
	public final static String STREET_TABLE = "street"; //$NON-NLS-1$
	public final static String CITY_TABLE = "city"; //$NON-NLS-1$
	public final static String BUILDING_TABLE = "building"; //$NON-NLS-1$
	
	
	public final static String POI_TABLE = "poi"; //$NON-NLS-1$
	
	public final static String BINARY_MAP_TABLE = "binary_map_objects"; //$NON-NLS-1$
	
	public final static String TRANSPORT_STOP_TABLE = "transport_stop"; //$NON-NLS-1$
	public final static String TRANSPORT_ROUTE_STOP_TABLE = "transport_route_stop"; //$NON-NLS-1$
	public final static String TRANSPORT_ROUTE_TABLE = "transport_route"; //$NON-NLS-1$

	
	
}
