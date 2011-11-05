package net.osmand.osm;

import gnu.trove.map.TIntByteMap;
import gnu.trove.map.hash.TIntByteHashMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.data.AmenityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * SOURCE : http://wiki.openstreetmap.org/wiki/Map_Features
 * 
 * Describing types of polygons :
 * 1. Last 2 bits define type of element : polygon, polyline, point 
 */
public class MapRenderingTypes {

	private static final Log log = LogUtil.getLog(MapRenderingTypes.class);
	
	/** standard schema :	 
	 	polygon : ll aaaaa ttttt 11 : 14 bits
	 	multi   : ll aaaaa ttttt 00 : 14 bits  
				    t - object type, a - area subtype,l - layer
		polyline :   ll ppppp ttttt 10 : 14 bits  
				   t - object type, p - polyline object type, l - layer 
		point :   ssss ssss ttttt 10 : 15 bits  
				   t - object type, s - subtype
	 */

	// keep sync ! not change values
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

	private String resourceName = null;

	// stored information to convert from osm tags to int type
	private Map<String, MapRulType> types = null;
	
	private TIntByteMap objectsToMinZoom = null;
	
	
	private static Map<String, AmenityType> amenityTagValToType = null;
	private static Map<String, String> amenityTagValToPrefix = null;
	private static String TAG_DELIMETER = "&&"; //$NON-NLS-1$
	
	private Map<AmenityType, Map<String, String>> amenityTypeNameToTagVal = null;
	private Map<String, AmenityType> amenityNameToType = null;
	
	public MapRenderingTypes(String fileName){
		this.resourceName = fileName;
	}
	
	
	private static MapRenderingTypes DEFAULT_INSTANCE = null;
	
	public static MapRenderingTypes getDefault() {
		if(DEFAULT_INSTANCE == null){
			DEFAULT_INSTANCE = new MapRenderingTypes(null);
		}
		return DEFAULT_INSTANCE;
	}

	private final static int POLYGON_WITH_CENTER_TYPE = 9;
	// special type means that ways will transform in area with the same point subtype = as area subtype
	// instead of zero point subtype (only for ways!)
	private final static int DEFAULT_POLYGON_BUILDING = 10;
	public static class MapRulType {
		private String tag;
		// val could be null means others for that tag
		private Integer nullRule;
		private Map<String, Integer> rules = new LinkedHashMap<String, Integer>();
		private Map<String, String> nameNullTag = new LinkedHashMap<String, String>();
		
		public MapRulType(String tag, String nameNullTag){
			this.tag = tag;
			this.nameNullTag.put(null, nameNullTag);
		}
		
		public String getTag() {
			return tag;
		}
		
		public Map<String, String> getNameNullTag() {
			return nameNullTag;
		}
		
		public Collection<String> getValuesSet(){
			return rules.keySet();
		}
		
		public void registerType(int minZoom, String val, int pointRule, int polylineRule, int polygonRule, int type, int subtype){
			int r = encodeRule(minZoom, pointRule, polylineRule, polygonRule, type, subtype);
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
		
		private int encodeRule(int minZoom, int pointRule, int polylineRule, int polygonRule, int type, int subtype){
			int rule = (((((minZoom << 4) | polygonRule) << 4) | polylineRule) << 4) | pointRule; // 17 bit
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
		
		public int getMinZoom(String val){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return (i >> 25);
		}
		
		public int getType(String val) {
			Integer i = val == null ? nullRule : rules.get(val);
			if (i == null) {
				return 0;
			}
			return i & MASK_5;
		}
		
		public int getSubType(String val) {
			Integer i = val == null ? nullRule : rules.get(val);
			if (i == null) {
				return 0;
			}
			return (i & MASK_13) >> 5;
		}
		
		public int getType(String val, int mask){
			Integer i = val == null ? nullRule : rules.get(val);
			if(i == null){
				return 0;
			}
			return i & mask;
		}
	}
	

	public Map<String, MapRulType> getEncodingRuleTypes(){
		if (types == null) {
			types = new LinkedHashMap<String, MapRulType>();
			init(INIT_RULE_TYPES);
		}
		return types;
	}
	
	
	// if type equals 0 no need to save that point
	public int encodeEntityWithType(Entity e, int zoom, boolean multipolygon, List<Integer> additionalTypes) {
		if (types == null) {
			types = new LinkedHashMap<String, MapRulType>();
			init(INIT_RULE_TYPES);
		}
		additionalTypes.clear();
		if("coastline".equals(e.getTag(OSMTagKey.NATURAL))){ //$NON-NLS-1$
			multipolygon = true;
		}

		boolean point = e instanceof Node;
		boolean polygon = multipolygon;
		if (!point && !polygon) {
			// determining area or path
			boolean area = "yes".equals(e.getTag("area")); //$NON-NLS-1$ //$NON-NLS-2$
			boolean highway = e.getTag("highway") != null; //$NON-NLS-1$
			if(highway && !area){
				// skip the check for first and last point
			} else {
				List<Long> ids = ((Way) e).getNodeIds();
				if (ids.size() > 1) {
					polygon = ((long) ids.get(0) == (long) ids.get(ids.size() - 1));
				}
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
			if (i == 1 && !additionalTypes.isEmpty()) {
				break;
			}
			for (String tag : tagKeySet) {
				if (types.containsKey(tag)) {
					MapRulType rType = types.get(tag);
					String val = i == 1 ? null : e.getTag(tag);
					if(rType.getMinZoom(val) > zoom){
						continue;
					}
					int pr = point ? rType.getPointRule(val) : (polygon ? rType.getPolygonRule(val) : rType.getPolylineRule(val));
					int typeVal = rType.getType(val, MASK_13) << 2;
					if (pr == POINT_TYPE && pointType == 0) {
						pointType = POINT_TYPE | typeVal;
						additionalTypes.add(pointType);
					} else if (!point && pr == POLYLINE_TYPE) {
						int attr = getLayerAttributes(e) << 12;
						boolean prevPoint = (polylineType == 0 && polygonType == 0);
						polylineType = POLYLINE_TYPE | (typeVal & MASK_12) | attr;
						if (tag.equals("highway") || prevPoint){ //$NON-NLS-1$
							additionalTypes.add(0, polylineType);
						} else { 
							additionalTypes.add(polylineType);
						}
					} else if (polygon && (pr == POLYGON_WITH_CENTER_TYPE || pr == POLYGON_TYPE)) {
						boolean prevPoint = (polylineType == 0 && polygonType == 0);
						int attr = getLayerAttributes(e) << 12;
						polygonType = (multipolygon ? MULTY_POLYGON_TYPE : POLYGON_TYPE) | (typeVal & MASK_12) | attr;
						if (prevPoint){
							additionalTypes.add(0, polygonType);
						} else { 
							additionalTypes.add(polygonType);
						}
						if (pr == POLYGON_WITH_CENTER_TYPE) {
							pointType = POINT_TYPE | typeVal;
							additionalTypes.add(pointType);
						}
					} else if (polygon && (pr == DEFAULT_POLYGON_BUILDING)) {
						// TODO get polygon type
						int MAN_MADE = 8;
						int SUBTYPE_BUILDING = 1;
						if(polygonType == 0 && polylineType == 0){
							int attr = getLayerAttributes(e) << 12;
							polygonType = (multipolygon ? MULTY_POLYGON_TYPE : POLYGON_TYPE) | (((SUBTYPE_BUILDING << 5) | MAN_MADE) << 2) | attr;
							additionalTypes.add(0, polygonType);
						}
						pointType = POINT_TYPE | typeVal;
						additionalTypes.add(pointType);
					}
				}
			}
		}
		
		int type = 0;
		if(!additionalTypes.isEmpty()){
			type = additionalTypes.get(0);
			additionalTypes.remove(0);
		}
		return type;
	}
	
	public static boolean isOneWayWay(int highwayAttributes){
		return (highwayAttributes & 1) > 0;
	}
	
	public static boolean isRoundabout(int highwayAttributes){
		return ((highwayAttributes >> 2) & 1) > 0;
	}
	
	// 0 - normal, 1 - under, 2 - bridge,over
	public static int getWayLayer(int type){
		return (3 & (type >> 12));
	}
	
	// 0 - normal, -1 - under, 1 - bridge,over
	// SYNCHRONIZED WITH C++
	public static int getNegativeWayLayer(int type) {
		int i = (3 & (type >> 12));
		if (i == 1) {
			return -1;
		} else if (i == 2) {
			return 1;
		}
		return 0;
	}
	
	// return 0 if not defined
	public static int getMaxSpeedIfDefined(int highwayAttributes){
		switch((highwayAttributes >> 4) & 7) {
		case 0:
			return 20;
		case 1:
			return 40;
		case 2:
			// for old format it should return 0;
			// TODO it should be uncommented because now it is fixed
			// return 60;
			return 0;
		case 3:
			return 80;
		case 4:
			return 100;
		case 5:
			return 120;
		case 6:
			return 140;
		case 7:
			return 0;
		
		}
		return 0;
	}
	
	// HIGHWAY special attributes :
	// o/oneway			1 bit
	// f/free toll 		1 bit
	// r/roundabout  	2 bit (+ 1 bit direction)
	// s/max speed   	3 bit [0 - 30km/h, 1 - 50km/h, 2 - 70km/h, 3 - 90km/h, 4 - 110km/h, 5 - 130 km/h, 6 >, 7 - 0/not specified]
	// a/vehicle access 4 bit   (height, weight?) - one bit bicycle
	// p/parking      	1 bit
	// c/cycle oneway 	1 bit
	// TODO 
	// ci/inside city   1 bit
	
	// ENCODING :  ci|c|p|aaaa|sss|rr|f|o - 14 bit
	
	public static int getHighwayAttributes(Entity e){
		int attr = 0;
		// cycle oneway
		attr <<= 1;
		String c = e.getTag("cycleway"); //$NON-NLS-1$
		if(c != null && ("opposite_lane".equals(c) || "opposite_track".equals(c) || "opposite".equals(c))){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			attr |= 1;
		}
		// parking
		attr <<= 1;
		String park = e.getTag("service"); //$NON-NLS-1$
		if("parking_aisle".equals(park)){ //$NON-NLS-1$
			attr |= 1;
		}

		// ACCESS not complete (should be redesigned)
		// vehicle access (not implement yet) 
		attr <<= 3;
		boolean hgv = "yes".equals(e.getTag("hgv")); //$NON-NLS-1$ //$NON-NLS-2$
		if(hgv){
			attr |= 3;
		} else {
			boolean goods = "yes".equals(e.getTag("goods")); //$NON-NLS-1$ //$NON-NLS-2$
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
		String maxspeed = e.getTag("maxspeed"); //$NON-NLS-1$
		if(maxspeed != null){
			int kmh = 0;
			try {
				kmh = Integer.parseInt(maxspeed);
			} catch (NumberFormatException es) {
			}
			if(kmh <= 0){
				attr |= 7;
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
			attr |= 7;
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
		if(one != null && (one.equals("yes") || one.equals("1") || one.equals("-1"))){  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
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
		} else if(e.getTag(OSMTagKey.TUNNEL) != null && !e.getTag(OSMTagKey.TUNNEL).equalsIgnoreCase("no")){
			return 1;
		}
		return 0;
	}
	
	public static boolean isLayerUnder(int attr){
		return (attr & 3) == 1;
	}
	
	public String getEntityName(Entity e) {
		if (e.getTag(OSMTagKey.REF) != null && e.getTag(OSMTagKey.HIGHWAY) != null) {
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
		if (name == null) {
			Collection<String> tagKeySet = e.getTagKeySet();
			Map<String, MapRulType> types = getEncodingRuleTypes();
			for (int i = 0; i < 2 && name == null; i++) {
				for (String tag : tagKeySet) {
					if (types.containsKey(tag)) {
						MapRulType rType = types.get(tag);
						String val = i == 1 ? null : e.getTag(tag);
						String nameNullTag = rType.getNameNullTag().get(val);
						if (nameNullTag != null) {
							name = e.getTag(nameNullTag);
							if (name != null) {
								break;
							}

						}
					}
				}
			}
		}
		
		return name;
	}
	
	
	private void initAmenityMap(){
		if (amenityTypeNameToTagVal == null) {
			amenityTypeNameToTagVal = new LinkedHashMap<AmenityType, Map<String, String>>();
			init(INIT_AMENITY_MAP);
		}
	}
	
	/**
	 * 
	 * @return <type, minzoom> map
	 * only when minzoom < 15
	 */
	public TIntByteMap getObjectTypeMinZoom(){
		if(objectsToMinZoom == null){
			objectsToMinZoom = new TIntByteHashMap();
			init(INIT_TYPE_ZOOM);
		}
		return objectsToMinZoom;
	}
	
	
	public Map<AmenityType, Map<String, String>> getAmenityTypeNameToTagVal() {
		initAmenityMap();
		return amenityTypeNameToTagVal;
	}
	
	public Map<String, AmenityType> getAmenityNameToType(){
		initAmenityMap();
		if(amenityNameToType == null){
			amenityNameToType = new LinkedHashMap<String, AmenityType>();
			for(AmenityType s : amenityTypeNameToTagVal.keySet()){
				Map<String, String> map = amenityTypeNameToTagVal.get(s);
				for(String t : map.keySet()){
//					if(amenityNameToType.containsKey(t)){
//						System.err.println("Conflict " + t + " " + amenityNameToType.get(t) + " <> " + s);
//					}
					amenityNameToType.put(t, s);
				}
			}
		}
		return amenityNameToType; 
	}
	
	

	
	private void registerAmenity(String tag, String val, int type, int subtype){
		AmenityType t = getAmenityType(tag, val);
		if (t != null) {
			if (val != null) {
				if (!amenityTypeNameToTagVal.containsKey(t)) {
					amenityTypeNameToTagVal.put(t, new LinkedHashMap<String, String>());
				}
				String name = val;
				String prefix = getAmenitySubtypePrefix(tag, val);
				if (prefix != null) {
					name = prefix + name;
				}
				Map<String, String> map = amenityTypeNameToTagVal.get(t);
				if (map.containsKey(val)) {
					if (type == 17 && subtype == 23) {
						// natural wood
					} else if (type == 2 && subtype == 21) {
						// barrier gate
					} else {
						// debug purpose
						// System.err.println("Duplicate entry " + tag + " " + val + " for " + t);
					}
				} else {
					if (prefix != null) {
						map.put(name, tag + " " + val); //$NON-NLS-1$
					} else {
						map.put(name, tag);
					}
				}
			}
		}else {
			// debug purpose
//			System.out.println("NOT ACCEPTED " + tag + " " + val);
		}
	}

	
	
	
	
	private final static int INIT_RULE_TYPES = 0;
	private final static int INIT_AMENITY_MAP = 1;
	private final static int INIT_TYPE_ZOOM = 2;
	
	private void init(final int st){
		InputStream is;
		try {
			if(resourceName == null){
				is = MapRenderingTypes.class.getResourceAsStream("rendering_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(resourceName);
			}
			long time = System.currentTimeMillis();
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			
			parser.parse(is, new DefaultHandler(){
				
				int currentType = 1;
				@Override
				public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
					name = parser.isNamespaceAware() ? localName : name;
					if(name.equals("type")){ //$NON-NLS-1$
						currentType = Integer.parseInt(attributes.getValue("id")); //$NON-NLS-1$
					} else if (name.equals("subtype")) { //$NON-NLS-1$
						String val = attributes.getValue("minzoom"); //$NON-NLS-1$
						int maxzoom = 15;
						if (val != null) {
							maxzoom = Integer.parseInt(val);
						}
						String nameNullTag = attributes.getValue("nameNullTag"); //$NON-NLS-1$

						String tag = attributes.getValue("tag"); //$NON-NLS-1$
						val = attributes.getValue("value"); //$NON-NLS-1$
						if (val != null && (val.equalsIgnoreCase("null") || val.length() == 0)) { //$NON-NLS-1$
							val = null;
						}
						int subtype = Integer.parseInt(attributes.getValue("id")); //$NON-NLS-1$
						boolean building = Boolean.parseBoolean(attributes.getValue("building")); //$NON-NLS-1$
						boolean polygon = Boolean.parseBoolean(attributes.getValue("polygon")); //$NON-NLS-1$
						boolean polyline = Boolean.parseBoolean(attributes.getValue("polyline")); //$NON-NLS-1$
						boolean point = Boolean.parseBoolean(attributes.getValue("point")); //$NON-NLS-1$
						boolean polygon_center = Boolean.parseBoolean(attributes.getValue("polygon_center")); //$NON-NLS-1$
						int polygonRule = 0;
						int pointRule = 0;
						int polylineRule = 0;
						if(building || point || polygon_center){
							pointRule = POINT_TYPE;
						}
						if(!polyline && polygon) {
							pointRule = POINT_TYPE;
						}
						
						if(polyline){
							polylineRule = POLYLINE_TYPE;
						} else if(point){
							polylineRule = POINT_TYPE;
						}
						
						if(building){
							polygonRule = DEFAULT_POLYGON_BUILDING;
						} else if(polygon_center){
							polygonRule = POLYGON_WITH_CENTER_TYPE;
						} else if(polygon){
							polygonRule = POLYGON_TYPE;
						}else if(polyline){
							polygonRule = POLYLINE_TYPE;
						} else if(point){
							polygonRule = POINT_TYPE;
						}
						
						stepSubtype(st, maxzoom, tag, val, currentType, subtype, polygonRule, polylineRule, pointRule, nameNullTag);
					}
				}
			});
			log.info("Time to init " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			is.close();
		} catch (IOException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw e;
		} catch (ParserConfigurationException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (SAXException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void stepSubtype(int st, int minZoom, String tag, String val, int type, int subtype, int polygonRule, int polylineRule,
			int pointRule, String nameNullTag) {
		if(st == INIT_RULE_TYPES){
			MapRulType rtype = types.get(tag);
			if(rtype == null){
				rtype = new MapRulType(tag, null);
				types.put(tag, rtype);
			}
			rtype.registerType(minZoom, val, pointRule, polylineRule, polygonRule, type, subtype);
			if(nameNullTag != null){
				rtype.getNameNullTag().put(val, nameNullTag);
			}
		} else if(st == INIT_AMENITY_MAP){
			if(pointRule == POINT_TYPE || polygonRule == POLYGON_WITH_CENTER_TYPE || polygonRule == POLYGON_TYPE){
				registerAmenity(tag, val, type, subtype);
			}
		} else if(st == INIT_TYPE_ZOOM){
			if(minZoom < 15){
				int value = (((subtype) << 5) | type) << 2;
				if(!objectsToMinZoom.containsKey(value)){
					// add only first
					objectsToMinZoom.put(value, (byte) minZoom);
				}
			}
		}
	}
	
	public static String getAmenitySubtype(String tag, String val){
		String prefix = getAmenitySubtypePrefix(tag, val);
		if(prefix != null){
			return prefix + val;
		}
		return val;
	}
	
	public static String getAmenitySubtypePrefix(String tag, String val){
		if(amenityTagValToPrefix == null){
			amenityTagValToPrefix = new LinkedHashMap<String, String>();
			amenityTagValToPrefix.put("traffic_calming", "traffic_calming_"); //$NON-NLS-1$ //$NON-NLS-2$
			amenityTagValToPrefix.put("power", "power_");  //$NON-NLS-1$//$NON-NLS-2$
			amenityTagValToPrefix.put("waterway", "water_"); //$NON-NLS-1$ //$NON-NLS-2$
			amenityTagValToPrefix.put("waterway"+TAG_DELIMETER+"riverbank", null); //$NON-NLS-1$ //$NON-NLS-2$
			
			amenityTagValToPrefix.put("railway", "railway_"); //$NON-NLS-1$ //$NON-NLS-2$
			amenityTagValToPrefix.put("railway"+TAG_DELIMETER+"subway_entrance", null); //$NON-NLS-1$ //$NON-NLS-2$
			
			amenityTagValToPrefix.put("aeroway", "aeroway_"); //$NON-NLS-1$ //$NON-NLS-2$
			amenityTagValToPrefix.put("aerialway", "aerialway_"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(val != null && amenityTagValToPrefix.containsKey(tag+TAG_DELIMETER+val)){
			return amenityTagValToPrefix.get(tag+TAG_DELIMETER+val);
		}
		return amenityTagValToPrefix.get(tag);
	}
	
	public static AmenityType getAmenityType(String tag, String val){
		// register amenity types
		if(amenityTagValToType == null){
			initAmenityTagValToType();
		}
		if(amenityTagValToType.containsKey(tag+TAG_DELIMETER+val)){
			return amenityTagValToType.get(tag+TAG_DELIMETER+val);
		}
		return amenityTagValToType.get(tag);
	}

	
	
	private static void initAmenityTagValToType() {
		amenityTagValToType = new LinkedHashMap<String, AmenityType>();

		amenityTagValToType.put("highway"+TAG_DELIMETER+"bus_stop", AmenityType.TRANSPORTATION); //$NON-NLS-1$ //$NON-NLS-2$
		amenityTagValToType.put("highway"+TAG_DELIMETER+"platform", AmenityType.TRANSPORTATION); //$NON-NLS-1$ //$NON-NLS-2$
		amenityTagValToType.put("highway"+TAG_DELIMETER+"turning_circle", AmenityType.TRANSPORTATION); //$NON-NLS-1$ //$NON-NLS-2$
		amenityTagValToType.put("highway"+TAG_DELIMETER+"emergency_access_point", AmenityType.TRANSPORTATION); //$NON-NLS-1$ //$NON-NLS-2$
		amenityTagValToType.put("highway"+TAG_DELIMETER+"speed_camera", AmenityType.TRANSPORTATION); //$NON-NLS-1$ //$NON-NLS-2$
		
		
		amenityTagValToType.put("traffic_calming", AmenityType.BARRIER); //$NON-NLS-1$
		amenityTagValToType.put("barrier", AmenityType.BARRIER); //$NON-NLS-1$
		amenityTagValToType.put("natural"+TAG_DELIMETER+"hedge", AmenityType.BARRIER);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("historic"+TAG_DELIMETER+"city_walls", AmenityType.BARRIER); //$NON-NLS-1$ //$NON-NLS-2$
		amenityTagValToType.put("highway"+TAG_DELIMETER+"gate", AmenityType.BARRIER); //$NON-NLS-1$ //$NON-NLS-2$
		
		amenityTagValToType.put("waterway"+TAG_DELIMETER+"stream", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("waterway"+TAG_DELIMETER+"riverbank", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("waterway"+TAG_DELIMETER+"river", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("waterway"+TAG_DELIMETER+"canal", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("waterway"+TAG_DELIMETER+"ditch", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("waterway"+TAG_DELIMETER+"drain", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("waterway", AmenityType.MAN_MADE);  //$NON-NLS-1$
		
		amenityTagValToType.put("railway", AmenityType.TRANSPORTATION);  //$NON-NLS-1$
		amenityTagValToType.put("aeroway", AmenityType.TRANSPORTATION);  //$NON-NLS-1$
		amenityTagValToType.put("aerialway", AmenityType.TRANSPORTATION);  //$NON-NLS-1$
		amenityTagValToType.put("aerialway"+TAG_DELIMETER+"pylon", null);  //$NON-NLS-1$//$NON-NLS-2$
		
		// do not add power tower to index
		amenityTagValToType.put("power"+TAG_DELIMETER+"station", AmenityType.MAN_MADE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("power"+TAG_DELIMETER+"sub_station", AmenityType.MAN_MADE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("power"+TAG_DELIMETER+"generator", AmenityType.MAN_MADE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("power"+TAG_DELIMETER+"cable_distribution_cabinet", AmenityType.MAN_MADE);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("building", null);  //$NON-NLS-1$
		amenityTagValToType.put("man_made"+TAG_DELIMETER+"wastewater_plant", null);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("man_made"+TAG_DELIMETER+"water_works", null);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("man_made"+TAG_DELIMETER+"works", null);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("man_made", AmenityType.MAN_MADE);  //$NON-NLS-1$
		
		amenityTagValToType.put("leisure", AmenityType.LEISURE);  //$NON-NLS-1$
		amenityTagValToType.put("natural"+TAG_DELIMETER+"park", AmenityType.LEISURE);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("office", AmenityType.OFFICE);  //$NON-NLS-1$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"architect_office", AmenityType.OFFICE);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("shop", AmenityType.SHOP);  //$NON-NLS-1$
		
		amenityTagValToType.put("emergency", AmenityType.EMERGENCY);  //$NON-NLS-1$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"fire_station", AmenityType.EMERGENCY);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("tourism", AmenityType.TOURISM);  //$NON-NLS-1$
		
		amenityTagValToType.put("historic", AmenityType.HISTORIC);  //$NON-NLS-1$
		
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"basin", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"grave_yard", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"cemetery", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"forest", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"meadow", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"military", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"orchard", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"recreation_ground", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"conservation", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"village_green", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"reservoir", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"water", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"salt_pond", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"quarry", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"vineyard", AmenityType.LANDUSE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("landuse"+TAG_DELIMETER+"wood", AmenityType.NATURAL);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("military", AmenityType.MILITARY);  //$NON-NLS-1$
		
		amenityTagValToType.put("natural", AmenityType.NATURAL);  //$NON-NLS-1$
		amenityTagValToType.put("natural"+TAG_DELIMETER+"field", null);  //$NON-NLS-1$ //$NON-NLS-2$
		
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"restaurant", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"cafe", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"food_court", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"fast_food", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"pub", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bar", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"biergarten", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"drinking_water", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bbq", AmenityType.SUSTENANCE);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"kindergarten", AmenityType.EDUCATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"school", AmenityType.EDUCATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"college", AmenityType.EDUCATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"library", AmenityType.EDUCATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"university", AmenityType.EDUCATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"kindergarten", AmenityType.EDUCATION);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"parking", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bicycle_parking", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"ferry_terminal", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"fuel", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"taxi", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bicycle_rental", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bus_station", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"car_rental", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"car_sharing", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"car_wash", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"grit_bin", AmenityType.TRANSPORTATION);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"atm", AmenityType.FINANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bank", AmenityType.FINANCE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"bureau_de_change", AmenityType.FINANCE);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"pharmacy", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"hospital", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"baby_hatch", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"dentist", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"doctors", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"veterinary", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"first_aid", AmenityType.HEALTHCARE);  //$NON-NLS-1$//$NON-NLS-2$
		
		
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"arts_centre", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"cinema", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"community_centre", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"social_centre", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"nightclub", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"stripclub", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"studio", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"theatre", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"sauna", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		amenityTagValToType.put("amenity"+TAG_DELIMETER+"brothel", AmenityType.ENTERTAINMENT);  //$NON-NLS-1$//$NON-NLS-2$
		
		amenityTagValToType.put("geocache", AmenityType.GEOCACHE); //$NON-NLS-1$
		
		amenityTagValToType.put("amenity", AmenityType.OTHER);  //$NON-NLS-1$
		amenityTagValToType.put("place", AmenityType.ADMINISTRATIVE);  //$NON-NLS-1$
		amenityTagValToType.put("sport", AmenityType.SPORT);  //$NON-NLS-1$
		
		amenityTagValToType.put("osmwiki", AmenityType.OSMWIKI);  //$NON-NLS-1$
		amenityTagValToType.put("user_defined", AmenityType.USER_DEFINED);  //$NON-NLS-1$
	}
	
	
	public static void main(String[] args) {
//		Map<String, Map<String, AmenityType>> amenityMap = getAmenityTagValToTypeMap();
//		for(String s : amenityMap.keySet()){
//			System.out.println(s + " - " + amenityMap.get(s));
//		}
//		Map<AmenityType, Map<String, String>> amenityType = getAmenityTypeNameToTagVal();
//		for(AmenityType s : amenityType.keySet()){
//			Map<String, String> map = amenityType.get(s);
//			for(String t : map.keySet()){
//				System.out.println(s + " - " + t + " " + map.get(t));
//			}
//		}
//		System.out.println(getAmenityNameToType());
//		long ts = System.currentTimeMillis();
		System.out.println(MapUtils.getTileNumberX(13, 23.95)+ " " + MapUtils.getTileNumberY(13, 52.136));
		
		MapRenderingTypes def = MapRenderingTypes.getDefault();
		def.initAmenityMap();
		System.out.println(def.amenityTypeNameToTagVal);
		System.out.println(def.getAmenityNameToType());
	}

}

