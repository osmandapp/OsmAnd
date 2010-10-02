package net.osmand.osm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.osmand.osm.OSMSettings.OSMTagKey;


/**
 * SOURCE : http://wiki.openstreetmap.org/wiki/Map_Features
 * 
 * Describing types of polygons :
 * 1. Last 3 bits define type of element : polygon, polyline, point 
 */
public class MapRenderingTypes {
	
	// TODO Internet access bits for point, polygon
	/** standard schema :	 
	  	Last bit describe whether types additional exist (it is needed only for main type, other consists only from 15 bits)
	 	polygon :   aaaaa ttttt 11 _ : 15 bits
	 	multi   :   aaaaa ttttt 00 _ : 15 bits  
				    t - object type, a - area subtype,l - layer
		polyline :   ll ppppp ttttt 10 _ : 15 bits + 2 bits for special info (+16) 
				   t - object type, p - polyline object type, l - layer 
		point :   ssssssss ttttt 10 _ : 16 bits  
				   t - object type, a - subtype
	 */

	public final static int MULTY_POLYGON_TYPE = 0;
	public final static int POLYGON_TYPE = 3;
	public final static int POLYLINE_TYPE = 2;
	public final static int POINT_TYPE = 1;
	
	
	public final static int PG_SUBTYPE_MASK_LEN = 5;
	public final static int PG_SUBTYPE_MASK = (1 << PG_SUBTYPE_MASK_LEN) -1;
	public final static int PO_SUBTYPE_MASK_LEN = 8;
	public final static int PO_SUBTYPE_MASK = (1 << PO_SUBTYPE_MASK_LEN) -1;
	public final static int PL_SUBTYPE_MASK_LEN = 5;
	public final static int PL_SUBTYPE_MASK = (1 << PL_SUBTYPE_MASK_LEN) -1;
	
	// TYPES : 
	public final static int OBJ_TYPE_MASK_LEN = 5;
	public final static int OBJ_TYPE_MASK = (1 << OBJ_TYPE_MASK_LEN) -1;
	public final static int MASK_13 = (1 << 13) - 1;
	public final static int MASK_12 = (1 << 12) - 1;
	public final static int MASK_4 = (1 << 4) - 1;
	public final static int MASK_5 = (1 << 5) - 1;
	public final static int MASK_10 = (1 << 10) - 1;
	
	public final static char REF_CHAR = ((char)0x0019);
	public final static char DELIM_CHAR = ((char)0x0018);
	

	public final static int HIGHWAY = 1; 
	public final static int BARRIER = 2; 
	public final static int WATERWAY = 3; 
	public final static int RAILWAY = 4;
	public final static int AEROWAY = 5;
	public final static int AERIALWAY = 6;  
	public final static int POWER = 7; 
	public final static int MAN_MADE = 8; 
	public final static int LEISURE = 9;  
	public final static int OFFICE = 10; 
	public final static int SHOP = 11;  
	public final static int EMERGENCY = 12;  
	public final static int TOURISM = 13; 
	public final static int HISTORIC = 14; 
	public final static int LANDUSE = 15;  
	public final static int MILITARY = 16; 
	public final static int NATURAL = 17;
	public final static int AMENITY_SUSTENANCE = 18; 
	public final static int AMENITY_EDUCATION = 19; 
	public final static int AMENITY_TRANSPORTATION = 20; 
	public final static int AMENITY_FINANCE = 21; 
	public final static int AMENITY_HEALTHCARE = 22; 
	public final static int AMENITY_ENTERTAINMENT = 23;
	public final static int AMENITY_OTHER = 24; 
	public final static int ADMINISTRATIVE = 25;
	public final static int ROUTE = 26; //NOT DONE YET
	public final static int SPORT = 27; //+no icons
	
	
	public final static int SUBTYPE_BUILDING = 1;
	public final static int SUBTYPE_GARAGES = 5;
	public final static int SUBTYPE_PARKING = 1;
	
	
	public final static int PL_HW_TRUNK = 1;
	public final static int PL_HW_MOTORWAY = 2;
	public final static int PL_HW_PRIMARY = 3;
	public final static int PL_HW_SECONDARY = 4;
	public final static int PL_HW_TERTIARY = 5;
	public final static int PL_HW_RESIDENTIAL = 6;
	public final static int PL_HW_SERVICE = 7;
	public final static int PL_HW_UNCLASSIFIED = 8;
	public final static int PL_HW_TRACK = 9;
	public final static int PL_HW_PATH = 10;
	public final static int PL_HW_LIVING_STREET = 11;
	
	public final static int PL_HW_PEDESTRIAN = 16;
	public final static int PL_HW_CYCLEWAY = 17;
	public final static int PL_HW_BYWAY = 18;
	public final static int PL_HW_FOOTWAY = 19;
	public final static int PL_HW_STEPS = 20;
	public final static int PL_HW_BRIDLEWAY = 21;
	public final static int PL_HW_SERVICES = 22;
	public final static int PL_HW_FORD = 23;
	
	public final static int PL_HW_CONSTRUCTION = 25;
	public final static int PL_HW_PROPOSED = 26;
	
	
	public final static byte RESTRICTION_NO_RIGHT_TURN = 1;
	public final static byte RESTRICTION_NO_LEFT_TURN = 2;
	public final static byte RESTRICTION_NO_U_TURN = 3;
	public final static byte RESTRICTION_NO_STRAIGHT_ON = 4;
	public final static byte RESTRICTION_ONLY_RIGHT_TURN = 5;
	public final static byte RESTRICTION_ONLY_LEFT_TURN = 6;
	public final static byte RESTRICTION_ONLY_STRAIGHT_ON = 7;
	
	
	// two bytes pass!	
	public static int getMainObjectType(int type){
		return (type >> 2) & OBJ_TYPE_MASK;
	}
	
	// two bytes pass!
	public static int getObjectSubType(int type){
		if((type & 3) == 1){
			return (type >> 7) & PO_SUBTYPE_MASK;
		} else {
			return (type >> 7) & PG_SUBTYPE_MASK;
		}
	}
	

	// stored information to convert from osm tags to int type
	private static Map<String, MapRulType> types = null;
	

	private final static int POLYGON_WITH_CENTER_TYPE = 9;
	// special type means that ways will transform in area with the same point subtype = as area subtype
	// instead of zero point subtype (only for ways!)
	private final static int DEFAULT_POLYGON_BUILDING = 10;
	private static class MapRulType {
		private String tag;
		// val could be null means others for that tag
		private Integer nullRule;
		private Map<String, Integer> rules = new LinkedHashMap<String, Integer>();
		
		public MapRulType(String tag){
			this.tag = tag;
		}
		
		public String getTag() {
			return tag;
		}
		
		public void registerType(int level, String val, int pointRule, int polylineRule, int polygonRule, int type, int subtype){
			int r = encodeRule(level, pointRule, polylineRule, polygonRule, type, subtype);
			if(val != null){
				rules.put(val, r);
			} else {
				nullRule = r;
			}
		}
		
		public boolean registered(String val){
			return rules.containsKey(val);
		}
		
		public boolean registeredAsNull(){
			return nullRule != null && nullRule > 0;
		}
		
		private int encodeRule(int level, int pointRule, int polylineRule, int polygonRule, int type, int subtype){
			int rule = (((((level << 4) | polygonRule) << 4) | polylineRule) << 4) | pointRule; // 14 bit
			rule = (((rule << 8) | subtype) << 5) | type; // + 13 bits
			return rule;
		}
		
		public int getPointRule(String val){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return (i >> 13) &  MASK_4;
		}
		
		public int getPolylineRule(String val){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return (i >> 17) & MASK_4;
		}
		
		public int getPolygonRule(String val){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return (i >> 21) & MASK_4;
		}
		
		public int getLevel(String val){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return (i >> 25) & MASK_4;
		}
		
		
		public int getType(String val, int mask){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return i & mask;
		}
	}
	
	// if type equals 0 no need to save that point
	public static int encodeEntityWithType(Entity e, int level, boolean multipolygon, List<Integer> addTypes) {
		if (types == null) {
			types = new LinkedHashMap<String, MapRulType>();
			init();
		}
		addTypes.clear();
		if("coastline".equals(e.getTag(OSMTagKey.NATURAL))){ //$NON-NLS-1$
			multipolygon = true;
		}

		boolean point = e instanceof Node;
		boolean polygon = multipolygon;
		if (!point && !polygon) {
			// determining area or path
			List<Long> ids = ((Way) e).getNodeIds();
			if (ids.size() > 1) {
				polygon = ((long) ids.get(0) == (long)ids.get(ids.size() - 1));
			}
		}
		
		Collection<String> tagKeySet = e.getTagKeySet();
		// 1. sort tags : important tags (type) will be rendered first
		if(tagKeySet.size() > 1 && tagKeySet.contains("building")){ //$NON-NLS-1$
			// first of all process building tag (in order to distinguish area and buildings)
			LinkedHashSet<String> set = new LinkedHashSet<String>();
			set.add("building"); //$NON-NLS-1$
			set.addAll(tagKeySet);
			tagKeySet = set;
		}
		
		
		int pointType = 0;
		int polylineType = 0;
		int polygonType = 0;
		
		// 2. 2 iterations first for exact tag=value match, second for any tag match
		for (int i = 0; i < 2; i++) {
			if (i == 1 && !addTypes.isEmpty()) {
				break;
			}
			for (String tag : tagKeySet) {
				if (types.containsKey(tag)) {
					MapRulType rType = types.get(tag);
					String val = i == 1 ? null : e.getTag(tag);
					if(rType.getLevel(val) < level){
						continue;
					}
					int pr = point ? rType.getPointRule(val) : (polygon ? rType.getPolygonRule(val) : rType.getPolylineRule(val));
					int typeVal = rType.getType(val, MASK_13) << 2;
					if (pr == POINT_TYPE && pointType == 0) {
						pointType = POINT_TYPE | typeVal;
						addTypes.add(pointType);
					} else if (!point && pr == POLYLINE_TYPE) {
						int attr = getLayerAttributes(e) << 12;
						boolean prevPoint = (polylineType == 0 && polygonType == 0);
						polylineType = POLYLINE_TYPE | (typeVal & MASK_12) | attr;
						if (((polylineType >> 2) & MASK_4) == HIGHWAY || prevPoint){
							addTypes.add(0, polylineType);
						} else { 
							addTypes.add(polylineType);
						}
					} else if (polygon && (pr == POLYGON_WITH_CENTER_TYPE || pr == POLYGON_TYPE)) {
						boolean prevPoint = (polylineType == 0 && polygonType == 0);
						int attr = getLayerAttributes(e) << 12;
						polygonType = (multipolygon ? MULTY_POLYGON_TYPE : POLYGON_TYPE) | (typeVal & MASK_12) | attr;
						if (prevPoint){
							addTypes.add(0, polygonType);
						} else { 
							addTypes.add(polygonType);
						}
						if (pr == POLYGON_WITH_CENTER_TYPE) {
							pointType = POINT_TYPE | typeVal;
							addTypes.add(pointType);
						}
					} else if (polygon && (pr == DEFAULT_POLYGON_BUILDING)) {
						if(polygonType == 0 && polylineType == 0){
							polygonType = (multipolygon ? MULTY_POLYGON_TYPE : POLYGON_TYPE) | (((SUBTYPE_BUILDING << 5) | MAN_MADE) << 3);
							addTypes.add(0, polygonType);
						}
						pointType = POINT_TYPE | typeVal;
						addTypes.add(pointType);
					}
				}
			}
		}
		
		int type = 0;
		if(addTypes.isEmpty()){
			return type;
		}
		boolean twoBytes = true;
		int first = addTypes.get(0);
		addTypes.remove(0);
		if(isHighwayType(first)){
			twoBytes = false;
			int	attr = getHighwayAttributes(e) << 16;
			type = attr | (first << 1);
		} else {
			type = first << 1;
		}
		if(twoBytes && addTypes.size() > 0){
			type |= (addTypes.get(0) << 16);
			addTypes.remove(0);
		}
		if(!addTypes.isEmpty()){
			type |= 1;
		}
		
		return type;
	}
	
	// 
	public static boolean isHighwayType(int t){
		return (t & 3) == POLYLINE_TYPE && ((t >> 2) & MASK_5) == HIGHWAY;
	}
	
	public static boolean isOneWayWay(int type){
		return ((1 << 15) & type) > 0;
	}
	
	// 0 - normal, 1 - under, 2 - bridge,over
	public static int getWayLayer(int type){
		return (3 & (type >> 12));
	}
	
	// HIGHWAY special attributes :
	// T/Type			5 bit
	// l/layer 			2 bit
	// o/oneway			1 bit
	// f/free toll 		1 bit
	// r/roundabout  	1 bit (+ 1 bit direction)
	// s/max speed   	3 bit [0 - 30km/h, 1 - 50km/h, 2 - 70km/h, 3 - 90km/h, 4 - 110km/h, 5 - 130 km/h, 6 >]
	// a/vehicle access 4 bit   (height, weight?) - one bit bicycle
	// p/parking      	1 bit
	// c/cycle oneway 	1 bit
	
	// ENCODING :  c|p|aaaa|sss|rr|f|o|ll|TTTTT|00001|01_ - 28 bit
	
	public static int getHighwayAttributes(Entity e){
		int attr = 0;
		// cycle oneway
		attr <<= 1;
		String c = e.getTag("cycleway");
		if(c != null && ("opposite_lane".equals(c) || "opposite_track".equals(c) || "opposite".equals(c))){
			attr |= 1;
		}
		// parking
		attr <<= 1;
		String park = e.getTag("service");
		if("parking_aisle".equals(park)){
			attr |= 1;
		}

		// ACCESS not complete (should be redesigned)
		// vehicle access (not implement yet) 
		attr <<= 3;
		boolean hgv = "yes".equals(e.getTag("hgv"));
		if(hgv){
			attr |= 3;
		} else {
			boolean goods = "yes".equals(e.getTag("goods"));
			if(goods){
				attr |= 2;
			} else {
				attr |= 1;
			}
		}
		// bicycle access
		attr <<= 1;
		
		
		// speed
		// very simple algorithm doesn't consider city rules (country rules) and miles per hour
		attr <<= 3;
		String maxspeed = e.getTag("maxspeed");
		if(maxspeed != null){
			int kmh = 0;
			try {
				kmh = Integer.parseInt(maxspeed);
			} catch (NumberFormatException es) {
			}
			if(kmh <= 0){
				attr |= 2;
			} else if(kmh <= 30){
				attr |= 0;
			} else if(kmh <= 50){
				attr |= 1;
			} else if(kmh <= 70){
				attr |= 2;
			} else if(kmh <= 90){
				attr |= 3;
			} else if(kmh <= 110){
				attr |= 4;
			} else if(kmh <= 130){
				attr |= 5;
			} else {
				attr |= 6;
			}
		} else {
			attr |= 2;
		}
		
		
		// roundabout
		attr <<= 2;
		String jun = e.getTag(OSMTagKey.JUNCTION);
		if(jun != null){
			if("roundabout".equals(jun)){ //$NON-NLS-1$
				attr |= 1;
				if("clockwise".equals(e.getTag("direction"))){ //$NON-NLS-1$ //$NON-NLS-2$
					attr |= 2;
				}
			} 
		}
		
		// toll
		String toll = e.getTag(OSMTagKey.TOLL);
		attr <<= 1;
		if(toll != null){
			if("yes".equals(toll)){ //$NON-NLS-1$
				attr |= 1;
			}
		}
		
		
		// oneway
		String one = e.getTag(OSMTagKey.ONEWAY);
		attr <<= 1;
		if(one != null){
			attr |= 1;
		}
		return attr;
	}
	
	private static int getLayerAttributes(Entity e){
		// layer
		String l = e.getTag(OSMTagKey.LAYER);
		if(l != null){
			if(l.startsWith("+")){ //$NON-NLS-1$
				l = l.substring(1);
			}
			int la = 0;
			try {
				la = Integer.parseInt(l);
			} catch (NumberFormatException es) {
			}
			if(la < 0){
				return 1;
			} else if(la > 0){
				return 2;
			}
		} else if(e.getTag(OSMTagKey.BRIDGE) != null){
			return 2;
		} else if(e.getTag(OSMTagKey.TUNNEL) != null){
			return 1;
		}
		return 0;
	}
	
	public static boolean isLayerUnder(int attr){
		return (attr & 3) == 1;
	}
	
	public static String getEntityName(Entity e, int mainType) {
		mainType >>= 1;
		if (e.getTag(OSMTagKey.REF) != null && getMainObjectType(mainType) == HIGHWAY) {
			String ref = e.getTag(OSMTagKey.REF);
			if (ref.length() > 5 && ref.indexOf('_') != -1) {
				ref = ref.substring(0, ref.indexOf('_'));
			}
			String name = e.getTag(OSMTagKey.NAME);
			if(name != null && !name.equals(ref)){
				return REF_CHAR + ref + REF_CHAR + name;
			} else {
				return REF_CHAR + ref;
			}
		}
		String name = e.getTag(OSMTagKey.NAME);
		if (name == null) {
			name = e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
		}
		if(name == null && getMainObjectType(mainType) == NATURAL && getObjectSubType(mainType) == 13){
			name = e.getTag("ele"); //$NON-NLS-1$
		}
		if(name == null && getMainObjectType(mainType) == AEROWAY && getObjectSubType(mainType) == 17){
			name = e.getTag(OSMTagKey.REF); 
		}
		return name;
	}
	
	
	private static void register(String tag, String val, int type, int subtype, int renderType){
		register(0, tag, val, type, subtype, renderType);
	}
	private static void register(int level, String tag, String val, int type, int subtype, int renderType){
		int polygonRule = 0;
		int polylineRule = 0;
		int pointRule = 0;
		if(renderType == POINT_TYPE){
			polygonRule = polylineRule = pointRule = POINT_TYPE;
		} else if(renderType == POLYLINE_TYPE){
			polylineRule = POLYLINE_TYPE;
			polygonRule = POLYLINE_TYPE;
		} else {
			polygonRule = renderType;
		}
		registerRules(level, tag, val, type, subtype, pointRule, polylineRule, polygonRule);
	}
	
	private static void register(String tag, String val, int type, int subtype, int renderType, int renderType2){
		register(0, tag, val, type, subtype, renderType, renderType2);
	}
	
	private static void register(int level, String tag, String val, int type, int subtype, int renderType, int renderType2){
		int polygonRule = 0;
		int polylineRule = 0;
		int pointRule = 0;
		if(renderType == POINT_TYPE || renderType2 == POINT_TYPE){
			int second = renderType == POINT_TYPE ? renderType2 : renderType;
			pointRule = POINT_TYPE;
			if(second == POLYLINE_TYPE){
				polygonRule = POINT_TYPE;
				polylineRule = second;
			} else {
				polygonRule = second;
				polylineRule = POINT_TYPE;
			}
		} else {
			if(renderType == POLYLINE_TYPE){
				polylineRule = renderType;
				polygonRule = renderType2;
			} else {
				polylineRule = renderType2;
				polygonRule = renderType;
			}
		}
		registerRules(level, tag, val, type, subtype, pointRule, polylineRule, polygonRule);
	}
	
	private static void registerRules(int level, 
			String tag, String val, int type, int subtype, int pointRule, int polylineRule, int polygonRule){
		MapRulType rtype = types.get(tag);
		if(rtype == null){
			rtype = new MapRulType(tag);
			types.put(tag, rtype);
		}
		rtype.registerType(level, val, pointRule, polylineRule, polygonRule, type, subtype);
	}
	
	private static void registerAsBuilding(String tag, String val, int type, int subtype){
		// transforms point -> as renderType ?
		// transforms way -> as renderType point and way as building ?
		MapRulType rtype = types.get(tag);
		if(rtype == null){
			rtype = new MapRulType(tag);
			types.put(tag, rtype);
		}
		rtype.registerType(0, val, POINT_TYPE, 0, DEFAULT_POLYGON_BUILDING, type, subtype);
	}
	
	private static void init(){
		
	// 1. highway	
		register(2, "highway", "motorway", HIGHWAY, PL_HW_MOTORWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "motorway_link", HIGHWAY, PL_HW_MOTORWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "highway", "trunk", HIGHWAY, PL_HW_TRUNK, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "trunk_link", HIGHWAY, PL_HW_TRUNK, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "highway", "primary", HIGHWAY, PL_HW_PRIMARY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "primary_link", HIGHWAY, PL_HW_PRIMARY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "secondary", HIGHWAY, PL_HW_SECONDARY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "secondary_link", HIGHWAY, PL_HW_SECONDARY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "tertiary", HIGHWAY, PL_HW_TERTIARY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "unclassified", HIGHWAY, PL_HW_UNCLASSIFIED, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "road", HIGHWAY, PL_HW_UNCLASSIFIED, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "residential", HIGHWAY, PL_HW_RESIDENTIAL, POLYLINE_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "living_street", HIGHWAY, PL_HW_LIVING_STREET, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "service", HIGHWAY, PL_HW_SERVICE, POLYLINE_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "track", HIGHWAY, PL_HW_TRACK, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "pedestrian", HIGHWAY, PL_HW_PEDESTRIAN, POLYLINE_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
//		register("highway", "raceway", HIGHWAY, PL_HW_RACEWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "services", HIGHWAY, PL_HW_SERVICES, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
//		register("highway", "bus_guideway", HIGHWAY, PL_HW_MOTORWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "path", HIGHWAY, PL_HW_PATH, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "cycleway", HIGHWAY, PL_HW_CYCLEWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "footway", HIGHWAY, PL_HW_FOOTWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "bridleway", HIGHWAY, PL_HW_BRIDLEWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "highway", "byway", HIGHWAY, PL_HW_BYWAY, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "steps", HIGHWAY, PL_HW_STEPS, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "ford", HIGHWAY, PL_HW_FORD, POLYLINE_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "construction", HIGHWAY, PL_HW_CONSTRUCTION, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "proposed", HIGHWAY, PL_HW_PROPOSED, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("highway", "mini_roundabout", HIGHWAY, 35, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "stop", HIGHWAY, 36, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "give_way", HIGHWAY, 37, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "traffic_signals", HIGHWAY, 38, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "motorway_junction", HIGHWAY, 39, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "bus_stop", HIGHWAY, 40, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "platform", HIGHWAY, 41, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "turning_circle", HIGHWAY, 42, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "emergency_access_point", HIGHWAY, 43, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "speed_camera", HIGHWAY, 44, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "street_lamp", HIGHWAY, 45, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("traffic_calming", "yes", HIGHWAY, 50, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "bump", HIGHWAY, 51, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "chicane", HIGHWAY, 52, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "cushion", HIGHWAY, 53, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "hump", HIGHWAY, 54, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "rumble_strip", HIGHWAY, 55, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "table", HIGHWAY, 56, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", "choker", HIGHWAY, 57, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("traffic_calming", null, HIGHWAY, 58, POINT_TYPE); //$NON-NLS-1$
		
	// 2. barrier	
		register("barrier", "hedge", BARRIER, 1, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "hedge", BARRIER, 1, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "fence", BARRIER, 2, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("fenced", "yes", BARRIER, 2, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "wall", BARRIER, 3, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "ditch", BARRIER, 1, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "retaining_wall", BARRIER, 4, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "barrier", "city_wall", BARRIER, 5, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "historic", "city_walls", BARRIER, 5, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "bollard", BARRIER, 6, POLYLINE_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "cycle_barrier", BARRIER, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "block", BARRIER, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "cattle_grid", BARRIER, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "toll_booth", BARRIER, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "entrance", BARRIER, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "gate", BARRIER, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("highway", "gate", BARRIER, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "lift_gate", BARRIER, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "stile", BARRIER, 23, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "horse_stile", BARRIER, 24, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "kissing_gate", BARRIER, 25, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "sally_port", BARRIER, 26, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "turnstile", BARRIER, 27, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("barrier", "kent_carriage_gap", BARRIER, 28, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
	// 3. waterway	
		register(1, "waterway", "stream", WATERWAY, 1, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$		
		register(2, "waterway", "riverbank", WATERWAY, 3, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		// Questionable index river & canals for level=2 (depends on target)
		register(2, "waterway", "river", WATERWAY, 2, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "waterway", "canal", WATERWAY, 4, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "ditch", WATERWAY, 5, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "drain", WATERWAY, 6, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "waterway", "dock", WATERWAY, 7, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "lock_gate", WATERWAY, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "lock", WATERWAY, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "turning_point", WATERWAY, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "boatyard", WATERWAY, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("waterway", "weir", WATERWAY, 11, POLYLINE_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "waterway", "dam", WATERWAY, 12, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "waterway", "mill_pond", WATERWAY, 13, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$

	// 4. railway	
		register(2, "railway", "rail", RAILWAY, 1, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "railway", "tram", RAILWAY, 2, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "railway", "light_rail", RAILWAY, 3, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "abandoned", RAILWAY, 4, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "disused", RAILWAY, 5, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "railway", "subway", RAILWAY, 6, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "railway", "preserved", RAILWAY, 7, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "narrow_gauge", RAILWAY, 8, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "construction", RAILWAY, 9, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "monorail", RAILWAY, 10, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "funicular", RAILWAY, 11, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("railway", "platform", RAILWAY, 12, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "railway", "station", RAILWAY, 13, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "turntable", RAILWAY, 14, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("railway", "halt", RAILWAY, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "tram_stop", RAILWAY, 23, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "crossing", RAILWAY, 24, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "level_crossing", RAILWAY, 25, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "subway_entrance", RAILWAY, 26, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("railway", "buffer_stop", RAILWAY, 27, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$

	
	// 5. aeroway
		register(1, "aeroway", "aerodrome", AEROWAY, 1, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("aeroway", "terminal", AEROWAY, 2, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aeroway", "helipad", AEROWAY, 3, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aeroway", "runway", AEROWAY, 7, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aeroway", "taxiway", AEROWAY, 8, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aeroway", "apron", AEROWAY, 9, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aeroway", "airport", AEROWAY, 10, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("aeroway", "gate", AEROWAY, 12, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("aeroway", "windsock", AEROWAY, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$

	// 6. aerialway
		register(1, "aerialway", "cable_car", AERIALWAY, 1, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aerialway", "gondola", AERIALWAY, 2, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aerialway", "chair_lift", AERIALWAY, 3, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aerialway", "mixed_lift", AERIALWAY, 4, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "aerialway", "drag_lift", AERIALWAY, 5, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("aerialway", "goods", AERIALWAY, 6, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("aerialway", "station", AERIALWAY, 7); //$NON-NLS-1$ //$NON-NLS-2$ 
		register("aerialway", "pylon", AERIALWAY, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("aerialway", "goods", AERIALWAY, 9, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	
	// 7. power
		register(1, "power", "tower", POWER, 1, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("power", "pole", POWER, 2, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "power", "line", POWER, 3, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("power", "minor_line", POWER, 4, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "power", "station", POWER, 5, POINT_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "power", "sub_station", POWER, 6, POINT_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("power", "generator", POWER, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("power", "cable_distribution_cabinet", POWER, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	
	// 8. man_made
		register("building", "yes", MAN_MADE, SUBTYPE_BUILDING, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("building", null, MAN_MADE, SUBTYPE_BUILDING, POLYGON_TYPE); //$NON-NLS-1$ 
		register("man_made", "wastewater_plant", MAN_MADE, 2, POINT_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("man_made", "water_works", MAN_MADE, 3); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "works", MAN_MADE, 4, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("building", "garages", MAN_MADE, SUBTYPE_GARAGES, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		
		register("man_made", "cutline", MAN_MADE, 7, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "man_made", "groyne", MAN_MADE, 8, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "man_made", "breakwater", MAN_MADE, 8, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "man_made", "pier", MAN_MADE, 9, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "pipeline", MAN_MADE, 10, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "reservoir_covered", MAN_MADE, 11, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "embankment", MAN_MADE, 12, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("man_made", "beacon", MAN_MADE, 15, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "crane", MAN_MADE, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "lighthouse", MAN_MADE, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "mineshaft", MAN_MADE, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "adit", MAN_MADE, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "petroleum_well", MAN_MADE, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "surveillance", MAN_MADE, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "survey_point", MAN_MADE, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "tower", MAN_MADE, 23, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "watermill", MAN_MADE, 24, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "water_tower", MAN_MADE, 25, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "water_well", MAN_MADE, 26, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("man_made", "windmill", MAN_MADE, 27, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 9. leisure
		register("leisure", "dog_park", LEISURE, 1, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "sports_centre", LEISURE, 2, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "golf_course", LEISURE, 3, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "stadium", LEISURE, 4, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerRules(1, "leisure", "track", LEISURE, 5, POINT_TYPE, POLYLINE_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "pitch", LEISURE, 6, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "water_park", LEISURE, 7, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "leisure", "marina", LEISURE, 8, POLYLINE_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "slipway", LEISURE, 9, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "fishing", LEISURE, 10, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "leisure", "nature_reserve", LEISURE, 11, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "leisure", "park", LEISURE, 12, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "leisure", "recreation_ground", LEISURE, 12, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "playground", LEISURE, 13, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "leisure", "garden", LEISURE, 14, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "leisure", "common", LEISURE, 15, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "ice_rink", LEISURE, 16, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "miniature_golf", LEISURE, 17, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("leisure", "dance", LEISURE, 18, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 10. office
		register("office", "accountant", OFFICE, 5, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "architect", OFFICE, 6, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "company", OFFICE, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "employment_agency", OFFICE, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "estate_agent", OFFICE, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "government", OFFICE, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "insurance", OFFICE, 11, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "it", OFFICE, 12, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "lawyer", OFFICE, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "ngo", OFFICE, 14, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "quango", OFFICE, 15, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "research", OFFICE, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "telecommunication", OFFICE, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("office", "travel_agent", OFFICE, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		// changed
		register("amenity", "architect_office", OFFICE, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 11. shop
		// reserve numbers from 1-10
		register("shop", "alcohol", SHOP, 41, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "bakery", SHOP, 42, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "beauty", SHOP, 43, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "beverages", SHOP, 44, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "bicycle", SHOP, 45, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "books", SHOP, 46, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "boutique", SHOP, 47, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "butcher", SHOP, 48, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "car", SHOP, 49, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "car_repair", SHOP, 50, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "charity", SHOP, 11, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "chemist", SHOP, 12, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "clothes", SHOP, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "fashion", SHOP, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "computer", SHOP, 14, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "confectionery", SHOP, 15, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "convenience", SHOP, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "department_store", SHOP, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "dry_cleaning", SHOP, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "doityourself", SHOP, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "electronics", SHOP, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "fabrics", SHOP, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "farm", SHOP, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "florist", SHOP, 23, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "funeral_directors", SHOP, 24, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "furniture", SHOP, 25, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "garden_centre", SHOP, 26, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "general", SHOP, 27, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "gift", SHOP, 28, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "glaziery", SHOP, 29, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "greengrocer", SHOP, 30, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hairdresser", SHOP, 31, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hardware", SHOP, 32, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hearing_aids", SHOP, 33, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hifi", SHOP, 34, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "ice_cream", SHOP, 35, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hardware", SHOP, 36, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hearing_aids", SHOP, 37, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "hifi", SHOP, 38, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "ice_cream", SHOP, 39, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "jewelry", SHOP, 40, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "kiosk", SHOP, 51, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "laundry", SHOP, 52, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "mall", SHOP, 53, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "massage", SHOP, 54, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "money_lender", SHOP, 55, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "motorcycle", SHOP, 56, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "newsagent", SHOP, 57, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "optician", SHOP, 58, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "organic", SHOP, 59, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "outdoor", SHOP, 60, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "pawnbroker", SHOP, 61, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "second_hand", SHOP, 62, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "sports", SHOP, 63, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "stationery", SHOP, 64, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "supermarket", SHOP, 65, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "shoes", SHOP, 66, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "tattoo", SHOP, 67, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "toys", SHOP, 68, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "travel_agency", SHOP, 69, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "variety_store", SHOP, 70, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("shop", "video", SHOP, 71, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
		
	// 12. emergency
		registerAsBuilding("emergency", "ambulance_station", EMERGENCY, 1); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("emergency", "ses_station", EMERGENCY, 2); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("emergency", "fire_extinguisher", EMERGENCY, 3, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("emergency", "fire_flapper", EMERGENCY, 4,POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("emergency", "fire_hose", EMERGENCY, 5, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("emergency", "fire_hydrant", EMERGENCY, 6, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("emergency", "phone", EMERGENCY, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("emergency", "ses_station", EMERGENCY, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("emergency", "siren", EMERGENCY, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$

		// change category
		register("amenity", "fire_station", EMERGENCY, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 13. tourism
		register(1, "tourism", "attraction", TOURISM, 2, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", "artwork", TOURISM, 3, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", "camp_site", TOURISM, 4, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", "caravan_site", TOURISM, 5, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", "picnic_site", TOURISM, 6, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "tourism", "theme_park", TOURISM, 7, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", "zoo", TOURISM, 8, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		registerAsBuilding("tourism", "alpine_hut",  TOURISM, 9); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("tourism", "chalet", TOURISM, 10); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("tourism", "guest_house", TOURISM, 11); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("tourism", "hostel", TOURISM, 12); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("tourism", "hotel", TOURISM, 13); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("tourism", "motel", TOURISM, 14); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("tourism", "museum", TOURISM, 15); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("tourism", "information", TOURISM, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", "viewpoint", TOURISM, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("tourism", null, TOURISM, 18, POINT_TYPE); //$NON-NLS-1$ 
	
	// 14. historic
		register("historic", "archaeological_site", HISTORIC, 1, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "battlefield", HISTORIC, 2, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "castle", HISTORIC, 4, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "fort", HISTORIC, 5, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "ruins", HISTORIC, 8, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("historic", "monument", HISTORIC, 7); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("historic", "boundary_stone", HISTORIC, 3, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "memorial", HISTORIC, 6, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "wayside_cross", HISTORIC, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "wayside_shrine", HISTORIC, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", "wreck", HISTORIC, 11, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("historic", null, HISTORIC, 12, POINT_TYPE); //$NON-NLS-1$ 
	
	// 15. landuse
		register(1, "landuse", "allotments", LANDUSE, 1, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "basin", LANDUSE, 2, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "brownfield", LANDUSE, 3, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "cemetery", LANDUSE, 4, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("landuse", "grave_yard", LANDUSE, 4, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "commercial", LANDUSE, 5, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "construction", LANDUSE, 6, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "farm", LANDUSE, 7, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "farmland", LANDUSE, 7, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register(1, "landuse", "farmyard", LANDUSE, 9, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "landuse", "forest", LANDUSE, 10, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("landuse", "garages", LANDUSE, 11, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "grass", LANDUSE, 12, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "greenfield", LANDUSE, 13, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "greenhouse_horticulture", LANDUSE, 14, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "industrial", LANDUSE, 15, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "landfill", LANDUSE, 16, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "meadow", LANDUSE, 17, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "military", LANDUSE, 18, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$ 
		register(1, "landuse", "orchard", LANDUSE, 19, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "railway", LANDUSE, 20, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$]
		register(1, "landuse", "recreation_ground", LANDUSE, 21, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "conservation", LANDUSE, 21, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "village_green", LANDUSE, 21, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "landuse", "reservoir", LANDUSE, 22, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "landuse", "water", LANDUSE, 22, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "residential", LANDUSE, 23, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "retail", LANDUSE, 24, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "salt_pond", LANDUSE, 25, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "quarry", LANDUSE, 26, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "landuse", "vineyard", LANDUSE, 27, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 16. military
		register("military", "airfield", MILITARY, 1, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("military", "bunker", MILITARY, 2, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("military", "barracks", MILITARY, 3, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("military", "danger_area", MILITARY, 4, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("military", "range", MILITARY, 5, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("military", "naval_base", MILITARY, 6, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		
	// 17. natural	
		register(3, "natural", "coastline", NATURAL, 5, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		register("natural", "bay", NATURAL, 1, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "beach", NATURAL, 2, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "cave_entrance", NATURAL, 3, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "cliff", NATURAL, 4, POINT_TYPE, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "fell", NATURAL, 6, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "natural", "glacier", NATURAL, 7, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "heath", NATURAL, 8, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1,"natural", "land", NATURAL, 9, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "marsh", NATURAL, 11, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "mud", NATURAL, 12, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "peak", NATURAL, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "sand", NATURAL, 14, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "scree", NATURAL, 15, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "scrub", NATURAL, 16, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "spring", NATURAL, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "stone", NATURAL, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("natural", "tree", NATURAL, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "volcano", NATURAL, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "natural", "water", NATURAL, 21, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "natural", "lake", NATURAL, 21, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "natural", "wetland", NATURAL, 22, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "natural", "wood", NATURAL, 23, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "landuse", "wood", NATURAL, 23, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$

	// 18. amenity sustenance
		registerAsBuilding("amenity", "restaurant", AMENITY_SUSTENANCE, 1); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "cafe", AMENITY_SUSTENANCE, 2); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "food_court", AMENITY_SUSTENANCE, 3); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "fast_food", AMENITY_SUSTENANCE, 4); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "pub", AMENITY_SUSTENANCE, 5); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "bar", AMENITY_SUSTENANCE, 6); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "biergarten", AMENITY_SUSTENANCE, 7); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "drinking_water", AMENITY_SUSTENANCE, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "bbq", AMENITY_SUSTENANCE, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 19. amenity education
		register("amenity", "kindergarten", AMENITY_EDUCATION, 1, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "school", AMENITY_EDUCATION, 2, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "college", AMENITY_EDUCATION, 3, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "library", AMENITY_EDUCATION, 4, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "university", AMENITY_EDUCATION, 5, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	
	
	// 20. amenity transportation
		register("amenity", "parking", AMENITY_TRANSPORTATION, SUBTYPE_PARKING, //$NON-NLS-1$ //$NON-NLS-2$ 
				POLYGON_WITH_CENTER_TYPE, POINT_TYPE); 
		register("amenity", "bicycle_parking", AMENITY_TRANSPORTATION, 2, //$NON-NLS-1$ //$NON-NLS-2$ 
				POLYGON_WITH_CENTER_TYPE, POINT_TYPE); 
		register(1, "amenity", "ferry_terminal", AMENITY_TRANSPORTATION, 3, POLYGON_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "amenity", "fuel", AMENITY_TRANSPORTATION, 4, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		// do not register small objects as area
		register("amenity", "taxi", AMENITY_TRANSPORTATION, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "bicycle_rental", AMENITY_TRANSPORTATION, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "amenity", "bus_station", AMENITY_TRANSPORTATION, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$ 
		register("amenity", "car_rental", AMENITY_TRANSPORTATION, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "car_sharing", AMENITY_TRANSPORTATION, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "car_wash", AMENITY_TRANSPORTATION, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "grit_bin", AMENITY_TRANSPORTATION, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 21. amenity finance
		register("amenity", "atm", AMENITY_FINANCE, 1, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "bank", AMENITY_FINANCE, 2); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "bureau_de_change", AMENITY_FINANCE, 3, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 22. amenity healthcare
		registerAsBuilding("amenity", "pharmacy", AMENITY_HEALTHCARE, 1); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "hospital", AMENITY_HEALTHCARE, 2, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "baby_hatch", AMENITY_HEALTHCARE, 3, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "dentist", AMENITY_HEALTHCARE, 4, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "doctors", AMENITY_HEALTHCARE, 5, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "veterinary", AMENITY_HEALTHCARE, 6, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "first_aid", AMENITY_HEALTHCARE, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 23. amenity entertainment
		
		register("amenity", "arts_centre", AMENITY_ENTERTAINMENT, 2, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "cinema", AMENITY_ENTERTAINMENT, 3, POLYGON_WITH_CENTER_TYPE, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "community_centre", AMENITY_ENTERTAINMENT, 4, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "social_centre", AMENITY_ENTERTAINMENT, 5, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		registerAsBuilding("amenity", "nightclub", AMENITY_ENTERTAINMENT, 6); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "stripclub", AMENITY_ENTERTAINMENT, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "studio", AMENITY_ENTERTAINMENT, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "theatre", AMENITY_ENTERTAINMENT, 9); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "sauna", AMENITY_ENTERTAINMENT, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "brothel", AMENITY_ENTERTAINMENT, 11); //$NON-NLS-1$ //$NON-NLS-2$
	
	// 24. amenity others
		register("amenity", "marketplace", AMENITY_OTHER, 1, POINT_TYPE, POLYGON_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		registerAsBuilding("amenity", "courthouse", AMENITY_OTHER, 5); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "crematorium", AMENITY_OTHER, 6); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "embassy", AMENITY_OTHER, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "grave_yard", AMENITY_OTHER, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "hunting_stand", AMENITY_OTHER, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "police", AMENITY_OTHER, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "post_box", AMENITY_OTHER, 11, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "post_office", AMENITY_OTHER, 12); //$NON-NLS-1$ //$NON-NLS-2$
		// do not mark as polygon when it is unknown how to render area
		register("amenity", "prison", AMENITY_OTHER, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "public_building", AMENITY_OTHER, 14, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "recycling", AMENITY_OTHER, 15, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "shelter", AMENITY_OTHER, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "telephone", AMENITY_OTHER, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "toilets", AMENITY_OTHER, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		registerAsBuilding("amenity", "townhall", AMENITY_OTHER, 19); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "vending_machine", AMENITY_OTHER, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "waste_basket", AMENITY_OTHER, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "waste_disposal", AMENITY_OTHER, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "fountain", AMENITY_OTHER, 23, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "bench", AMENITY_OTHER, 24, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "clock", AMENITY_OTHER, 25, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("amenity", "place_of_worship", AMENITY_OTHER, 26, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		// "amenity", "place_of_worship"

	// 25. administrative 
		register(3, "place", "continent", ADMINISTRATIVE, 41, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3, "place", "country", ADMINISTRATIVE, 42, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3, "place", "state", ADMINISTRATIVE, 43, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3,"place", "region", ADMINISTRATIVE, 44, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3,"place", "county", ADMINISTRATIVE, 45, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3,"place", "city", ADMINISTRATIVE, 6, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2,"place", "town", ADMINISTRATIVE, 7, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "place", "village", ADMINISTRATIVE, 8, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "place", "hamlet", ADMINISTRATIVE, 9, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "place", "suburb", ADMINISTRATIVE, 10, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "place", "locality", ADMINISTRATIVE, 11, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "place", "island", ADMINISTRATIVE, 12, POINT_TYPE, POLYGON_WITH_CENTER_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
//		register("boundary", "administrative", ADMINISTRATIVE, 15, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		//"boundary", "administrative"
		register("admin_level", "1", ADMINISTRATIVE, 21, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3, "admin_level", "2", ADMINISTRATIVE, 22, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3, "admin_level", "3", ADMINISTRATIVE, 23, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3, "admin_level", "4", ADMINISTRATIVE, 24, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "admin_level", "5", ADMINISTRATIVE, 25, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "admin_level", "6", ADMINISTRATIVE, 26, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "admin_level", "7", ADMINISTRATIVE, 27, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "admin_level", "8", ADMINISTRATIVE, 28, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "admin_level", "9", ADMINISTRATIVE, 29, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(1, "admin_level", "10", ADMINISTRATIVE, 30, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		
		
		register(2, "boundary", "civil", ADMINISTRATIVE, 16, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "boundary", "political", ADMINISTRATIVE, 17, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(3, "boundary", "maritime", ADMINISTRATIVE, 18, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "boundary", "national_park", ADMINISTRATIVE, 19, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register(2, "boundary", "protected_area", ADMINISTRATIVE, 20, POLYLINE_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("addr:housenumber", null, ADMINISTRATIVE, 33, POINT_TYPE); //$NON-NLS-1$

		
		
	// 27. sport
		register("sport", "9pin", SPORT, 1, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "10pin", SPORT, 2, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "archery", SPORT, 3, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "athletics", SPORT, 4, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "australian_football", SPORT, 5, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "baseball", SPORT, 6, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "basketball", SPORT, 7, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "beachvolleyball", SPORT, 8, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "boules", SPORT, 9, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "bowls", SPORT, 10, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "canoe", SPORT, 11, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "chess", SPORT, 12, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "climbing", SPORT, 13, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "cricket", SPORT, 14, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "cricket_nets", SPORT, 15, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "croquet", SPORT, 16, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "cycling", SPORT, 17, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "diving", SPORT, 18, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "dog_racing", SPORT, 19, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "equestrian", SPORT, 20, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "football", SPORT, 21, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "golf", SPORT, 22, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "gymnastics", SPORT, 23, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "hockey", SPORT, 24, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "horse_racing", SPORT, 25, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "ice_stock", SPORT, 26, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "korfball", SPORT, 27, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "motor", SPORT, 28, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "multi", SPORT, 29, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "orienteering", SPORT, 30, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "paddle_tennis", SPORT, 31, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "paragliding", SPORT, 32, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "pelota", SPORT, 33, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "racquet", SPORT, 34, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "rowing", SPORT, 35, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "rugby", SPORT, 36, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "shooting", SPORT, 37, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "skating", SPORT, 38, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "skateboard", SPORT, 39, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "skiing", SPORT, 40, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "soccer", SPORT, 41, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "swimming", SPORT, 42, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "table_tennis", SPORT, 43, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "team_handball", SPORT, 44, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "tennis", SPORT, 45, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "toboggan", SPORT, 46, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		register("sport", "volleyball", SPORT, 47, POINT_TYPE); //$NON-NLS-1$ //$NON-NLS-2$		
	}

}

