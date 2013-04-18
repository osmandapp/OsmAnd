package net.osmand.osm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.osmand.PlatformUtil;
import net.osmand.data.AmenityType;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * reference : http://wiki.openstreetmap.org/wiki/Map_Features
 */
public class MapRenderingTypes {

	private static final Log log = PlatformUtil.getLog(MapRenderingTypes.class);
	
	public final static byte RESTRICTION_NO_RIGHT_TURN = 1;
	public final static byte RESTRICTION_NO_LEFT_TURN = 2;
	public final static byte RESTRICTION_NO_U_TURN = 3;
	public final static byte RESTRICTION_NO_STRAIGHT_ON = 4;
	public final static byte RESTRICTION_ONLY_RIGHT_TURN = 5;
	public final static byte RESTRICTION_ONLY_LEFT_TURN = 6;
	public final static byte RESTRICTION_ONLY_STRAIGHT_ON = 7;
	
	private static char TAG_DELIMETER = '/'; //$NON-NLS-1$
	
	private String resourceName = null;
	private Map<AmenityType, Map<String, String>> amenityTypeNameToTagVal = null;
	private Map<String, AmenityType> amenityNameToType = null;
	private Map<String, AmenityRuleType> amenityTypes = null;

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

	private Map<String, AmenityRuleType> getAmenityEncodingRuleTypes(){
		checkIfInitNeeded();
		return amenityTypes;
	}
	
	protected void checkIfInitNeeded() {
		if(amenityTypes == null) {
			amenityTypes = new LinkedHashMap<String, MapRenderingTypes.AmenityRuleType>();
			init();
		}
	}

	public Map<AmenityType, Map<String, String>> getAmenityTypeNameToTagVal() {
		if (amenityTypeNameToTagVal == null) {
			Map<String, AmenityRuleType> types = getAmenityEncodingRuleTypes();
			amenityTypeNameToTagVal = new LinkedHashMap<AmenityType, Map<String, String>>();
			for(AmenityRuleType type : types.values()){
				if(type.poiCategory != null && type.targetTagValue == null) {
					if(!amenityTypeNameToTagVal.containsKey(type.poiCategory)) {
						amenityTypeNameToTagVal.put(type.poiCategory, new TreeMap<String, String>());
					}
					String name = type.value;
					if (name != null) {
						if (type.poiPrefix != null) {
							name = type.poiPrefix + name;
							amenityTypeNameToTagVal.get(type.poiCategory).put(name, type.tag + " " + type.value);
						} else {
							amenityTypeNameToTagVal.get(type.poiCategory).put(name, type.tag);
						}
					}
				}
			}
		}
		return amenityTypeNameToTagVal;
	}
	
	
	public Map<String, AmenityType> getAmenityNameToType(){
		if(amenityNameToType == null){
			amenityNameToType = new LinkedHashMap<String, AmenityType>();
			Map<AmenityType, Map<String, String>> map = getAmenityTypeNameToTagVal();
			Iterator<Entry<AmenityType, Map<String, String>>> iter = map.entrySet().iterator();
			while(iter.hasNext()){
				Entry<AmenityType, Map<String, String>> e = iter.next();
				for(String t : e.getValue().keySet()){
					if (t != null) {
						if (amenityNameToType.containsKey(t)) {
							System.err.println("Conflict " + t + " " + amenityNameToType.get(t) + " <> " + e.getKey());
						}
						amenityNameToType.put(t, e.getKey());
					}
				}
			}
		}
		return amenityNameToType; 
	}
	
	public Collection<String> getAmenitySubCategories(AmenityType t){
		Map<AmenityType, Map<String, String>> amenityTypeNameToTagVal = getAmenityTypeNameToTagVal();
		if(!amenityTypeNameToTagVal.containsKey(t)){
			return Collections.emptyList(); 
		}
		return amenityTypeNameToTagVal.get(t).keySet();
	}
	
	
	public String getAmenitySubtype(String tag, String val){
		String prefix = getAmenitySubtypePrefix(tag, val);
		if(prefix != null){
			return prefix + val;
		}
		return val;
	}
	
	public String getAmenitySubtypePrefix(String tag, String val){
		Map<String, AmenityRuleType> rules = getAmenityEncodingRuleTypes();
		AmenityRuleType rt = rules.get(constructRuleKey(tag, val));
		if(rt != null && rt.poiPrefix != null) {
			return rt.poiPrefix;
		}
		rt = rules.get(constructRuleKey(tag, null));
		if(rt != null && rt.poiPrefix != null) {
			return rt.poiPrefix;
		}
		return null;
	}
	
	public AmenityType getAmenityType(String tag, String val){
		return getAmenityType(tag, val, false);
	}
	
	public AmenityType getAmenityType(String tag, String val, boolean relation){
		// register amenity types
		Map<String, AmenityRuleType> rules = getAmenityEncodingRuleTypes();
		AmenityRuleType rt = rules.get(constructRuleKey(tag, val));
		if(rt != null && rt.poiSpecified) {
			if(relation && !rt.relation) {
				return null;
			}
			return rt.poiCategory;
		}
		rt = rules.get(constructRuleKey(tag, null));
		if(rt != null && rt.poiSpecified) {
			if(relation && !rt.relation) {
				return null;
			}
			return rt.poiCategory;
		}
		return null;
	}

	
	protected void init(){
		InputStream is;
		try {
			if(resourceName == null){
				is = MapRenderingTypes.class.getResourceAsStream("rendering_types.xml"); //$NON-NLS-1$
			} else {
				is = new FileInputStream(resourceName);
			}
			long time = System.currentTimeMillis();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			int tok;
			parser.setInput(is, "UTF-8");
			String poiParentCategory = null;
			String poiParentPrefix  = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name.equals("category")) { //$NON-NLS-1$
						poiParentCategory = parser.getAttributeValue("","poi_category");
						poiParentPrefix = parser.getAttributeValue("","poi_prefix");
						parseCategoryFromXml(parser, poiParentCategory, poiParentPrefix);
					} else if (name.equals("type")) {
						parseTypeFromXML(parser, poiParentCategory, poiParentPrefix);
					} else if (name.equals("routing_type")) {
						parseRouteTagFromXML(parser);
					}
				}
			}
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
		} catch (XmlPullParserException e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected void parseRouteTagFromXML(XmlPullParser parser) {
	}

	protected void parseTypeFromXML(XmlPullParser parser, String poiParentCategory, String poiParentPrefix) {
		AmenityRuleType rtype = new AmenityRuleType();
		rtype.tag = parser.getAttributeValue("", "tag"); //$NON-NLS-1$
		rtype.value = parser.getAttributeValue("", "value"); //$NON-NLS-1$
		if (rtype.value != null && rtype.value.length() == 0) { //$NON-NLS-1$
			rtype.value = null;
		}
		if (poiParentCategory != null) {
			rtype.poiCategory = AmenityType.valueOf(poiParentCategory.toUpperCase());
			rtype.poiSpecified = true;
		}
		if (poiParentPrefix != null) {
			rtype.poiPrefix = poiParentPrefix;
		}

		String poiCategory = parser.getAttributeValue("", "poi_category");
		if (poiCategory != null) {
			rtype.poiSpecified = true;
			if (poiCategory.length() == 0) {
				rtype.poiCategory = null;
			} else {
				rtype.poiCategory = AmenityType.valueOf(poiCategory.toUpperCase());
			}
		}
		String poiPrefix = parser.getAttributeValue("", "poi_prefix");
		if (poiPrefix != null) {
			rtype.poiPrefix = poiPrefix;
		}
		rtype.relation = Boolean.parseBoolean(parser.getAttributeValue("", "relation"));
		if (rtype.poiSpecified) {
			registerAmenityType(rtype.tag, rtype.value, rtype);
			String targetTag = parser.getAttributeValue("", "target_tag");
			String targetValue = parser.getAttributeValue("", "target_value");
			if (targetTag != null || targetValue != null) {
				if (targetTag == null) {
					targetTag = rtype.tag;
				}
				if (targetValue == null) {
					targetValue = rtype.value;
				}
				rtype.targetTagValue = amenityTypes.get(constructRuleKey(targetTag, targetValue));
			}
		}
	}

	private AmenityRuleType registerAmenityType(String tag, String val, AmenityRuleType rt) {
		String keyVal = constructRuleKey(tag, val);
		if (amenityTypes.containsKey(keyVal)) {
			throw new RuntimeException("Duplicate " + keyVal);
		} else {
			amenityTypes.put(keyVal, rt);
			return rt;
		}
	}

	protected void parseCategoryFromXml(XmlPullParser parser, String poiParentCategory, String poiParentPrefix) {
		String tag = parser.getAttributeValue("","poi_tag");
		if (tag != null) {
			AmenityRuleType rtype = new AmenityRuleType();
			rtype.poiCategory = AmenityType.valueOf(poiParentCategory.toUpperCase());
			rtype.poiSpecified = true;
			rtype.relation = Boolean.parseBoolean(parser.getAttributeValue("", "relation"));
			rtype.poiPrefix = poiParentPrefix;
			rtype.tag = tag;
			registerAmenityType(tag, null, rtype);
		}
	}
	
	protected static String constructRuleKey(String tag, String val) {
		if(val == null || val.length() == 0){
			return tag;
		}
		return tag + TAG_DELIMETER + val;
	}
	
	protected static String getTagKey(String tagValue) {
		int i = tagValue.indexOf(TAG_DELIMETER);
		if(i >= 0){
			return tagValue.substring(0, i);
		}
		return tagValue;
	}
	
	protected static String getValueKey(String tagValue) {
		int i = tagValue.indexOf(TAG_DELIMETER);
		if(i >= 0){
			return tagValue.substring(i + 1);
		}
		return null;
	}
	
		
	private static class AmenityRuleType {
		protected String tag;
		protected String value;
		protected String poiPrefix;
		protected boolean relation;
		protected AmenityType poiCategory;
		protected boolean poiSpecified;
		protected AmenityRuleType targetTagValue;
	}
	
}

