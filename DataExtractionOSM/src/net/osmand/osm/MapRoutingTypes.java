package net.osmand.osm;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class MapRoutingTypes {

	private static Set<String> TAGS_TO_SAVE = new HashSet<String>();
	private static Set<String> TAGS_TO_ACCEPT = new HashSet<String>();
	private static Set<String> TAGS_TEXT = new HashSet<String>();
	private static char TAG_DELIMETER = '/'; //$NON-NLS-1$
	static {
		TAGS_TO_ACCEPT.add("highway");
		TAGS_TO_ACCEPT.add("junction");
		TAGS_TO_ACCEPT.add("cycleway");
		TAGS_TO_ACCEPT.add("route");
		
		// TEXT tags
		TAGS_TEXT.add("int_ref");
		TAGS_TEXT.add("ref");
		TAGS_TEXT.add("name");
		TAGS_TEXT.add("direction");
		TAGS_TEXT.add("destination");
		TAGS_TEXT.add("destination:lanes");
		TAGS_TEXT.add("duration");
		
		TAGS_TO_SAVE.add("agricultural");
		TAGS_TO_SAVE.add("barrier");
		TAGS_TO_SAVE.add("bicycle");
		TAGS_TO_SAVE.add("boat");
		TAGS_TO_SAVE.add("bridge");
		TAGS_TO_SAVE.add("bus");
		TAGS_TO_SAVE.add("construction");
		TAGS_TO_SAVE.add("direction");
		TAGS_TO_SAVE.add("ferry");
		TAGS_TO_SAVE.add("foot");
		TAGS_TO_SAVE.add("goods");
		TAGS_TO_SAVE.add("hgv");
		TAGS_TO_SAVE.add("horse");
		TAGS_TO_SAVE.add("lanes");
		TAGS_TO_SAVE.add("maxspeed");
		TAGS_TO_SAVE.add("maxweight");
		TAGS_TO_SAVE.add("minspeed");
		TAGS_TO_SAVE.add("moped");
		TAGS_TO_SAVE.add("motorboat");
		TAGS_TO_SAVE.add("motorcar");
		TAGS_TO_SAVE.add("motorcycle");
		TAGS_TO_SAVE.add("motor_vehicle");
		TAGS_TO_SAVE.add("oneway");
		TAGS_TO_SAVE.add("roundabout");
		TAGS_TO_SAVE.add("route");
		TAGS_TO_SAVE.add("service");
		TAGS_TO_SAVE.add("ship");
		TAGS_TO_SAVE.add("toll");
		TAGS_TO_SAVE.add("toll_booth");
		TAGS_TO_SAVE.add("train");
		TAGS_TO_SAVE.add("tracktype");
		TAGS_TO_SAVE.add("traffic_calming");
		TAGS_TO_SAVE.add("turn:lanes");
		TAGS_TO_SAVE.add("turn");
		TAGS_TO_SAVE.add("tunnel");
		TAGS_TO_SAVE.add("railway");
		
	}
	
	private Map<String, MapRouteType> types = new LinkedHashMap<String, MapRoutingTypes.MapRouteType>();
	private List<MapRouteType> listTypes = new ArrayList<MapRoutingTypes.MapRouteType>(); 
	
	public static String constructRuleKey(String tag, String val) {
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
	
	private boolean contains(Set<String> s, String tag, String value) {
		if(s.contains(tag) || s.contains(tag + TAG_DELIMETER + value)){
			return true;
		}
		return false;
	}
	
	
	public boolean encodeEntity(Way et, TIntArrayList outTypes, Map<MapRouteType, String> names){
		Way e = (Way) et;
		boolean init = false;
		for(Entry<String, String> es : e.getTags().entrySet()) {
			String tag = es.getKey();
			String value = es.getValue();
			if (contains(TAGS_TO_ACCEPT, tag, value)) {
				init = true;
				break;
			}
		}
		if(!init) {
			return false;
		}
		outTypes.clear();
		names.clear();
		for(Entry<String, String> es : e.getTags().entrySet()) {
			String tag = es.getKey();
			String value = converBooleanValue(es.getValue());
			if(contains(TAGS_TO_ACCEPT, tag, value) || contains(TAGS_TO_SAVE, tag, value) || tag.startsWith("access")) {
				outTypes.add(registerRule(tag, value).id);
			}
			if(TAGS_TEXT.contains(tag)) {
				names.put(registerRule(tag, null), value);
			}
		}
		return true;
	}
	
	private String converBooleanValue(String value){
		if(value.equals("true")) {
			return "yes";
		} else if(value.equals("false")) {
			return "no";
		}
		return value;
	}
	
	public void encodePointTypes(Way e, TLongObjectHashMap<TIntArrayList> pointTypes){
		pointTypes.clear();
		for(Node nd : e.getNodes() ) {
			if (nd != null) {
				for (Entry<String, String> es : nd.getTags().entrySet()) {
					String tag = es.getKey();
					String value = converBooleanValue(es.getValue());
					if (contains(TAGS_TO_ACCEPT, tag, value) || contains(TAGS_TO_SAVE, tag, value) || tag.startsWith("access")) {
						if (!pointTypes.containsKey(nd.getId())) {
							pointTypes.put(nd.getId(), new TIntArrayList());
						}
						pointTypes.get(nd.getId()).add(registerRule(tag, value).id);
					}
				}
			}
		}
	}
	
	public MapRouteType getTypeByInternalId(int id) {
		return listTypes.get(id - 1);
	}
	
	private MapRouteType registerRule(String tag, String val) {
		String id = constructRuleKey(tag, val);
		if(!types.containsKey(id)) {
			MapRouteType rt = new MapRouteType();
			// first one is always 1
			rt.id = types.size() + 1;
			rt.tag = tag;
			rt.value = val;
			types.put(id, rt);
			listTypes.add(rt);
		}
		MapRouteType type = types.get(id);
		type.freq ++;
		return type;
	}
	
	public static class MapRouteType {
		int freq = 0;
		int id;
		int targetId;
		String tag;
		String value;
		
		public int getInternalId() {
			return id;
		}
		
		public int getFreq() {
			return freq;
		}
		
		public int getTargetId() {
			return targetId;
		}
		
		public String getTag() {
			return tag;
		}
		
		public String getValue() {
			return value;
		}
		
		public void setTargetId(int targetId) {
			this.targetId = targetId;
		}
		
	}

	public List<MapRouteType> getEncodingRuleTypes() {
		return listTypes;
	}
}
