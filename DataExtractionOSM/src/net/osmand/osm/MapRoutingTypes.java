package net.osmand.osm;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MapRoutingTypes {

	private static Set<String> TAGS_TO_SAVE = new HashSet<String>();
	private static Set<String> TAGS_TO_ACCEPT = new HashSet<String>();
	private static char TAG_DELIMETER = '/'; //$NON-NLS-1$
	static {
		TAGS_TO_ACCEPT.add("highway");
		TAGS_TO_ACCEPT.add("junction");
		TAGS_TO_ACCEPT.add("cycleway");
		
		TAGS_TO_SAVE.add("roundabout");
		TAGS_TO_SAVE.add("oneway");
		TAGS_TO_SAVE.add("service");
		TAGS_TO_SAVE.add("traffic_calming");
		TAGS_TO_SAVE.add("barrier");
		TAGS_TO_SAVE.add("minspeed");
		TAGS_TO_SAVE.add("maxspeed");
		TAGS_TO_SAVE.add("direction");
		TAGS_TO_SAVE.add("hgv");
		TAGS_TO_SAVE.add("goods");
		TAGS_TO_SAVE.add("toll");
		TAGS_TO_SAVE.add("tracktype");
	}
	
	private Map<String, MapRouteType> types = new LinkedHashMap<String, MapRoutingTypes.MapRouteType>(); 
	
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
	
	
	public boolean encodeEntity(Way e, TIntArrayList outTypes, TLongObjectHashMap<TIntArrayList> pointTypes){
		boolean init = false;
		for(String tg : e.getTagKeySet()) {
			if(TAGS_TO_ACCEPT.contains(tg)){
				init  = true;
				break;
			}
		}
		if(!init) {
			return false;
		}
		outTypes.clear();
		pointTypes.clear();
		for(Entry<String, String> es : e.getTags().entrySet()) {
			String tag = es.getKey();
			String value = es.getValue();
			if(TAGS_TO_ACCEPT.contains(tag) || TAGS_TO_SAVE.contains(tag) || tag.startsWith("access")) {
				outTypes.add(registerRule(tag, value).id);
			}
		}
		for(Node nd : e.getNodes() ) {
			for(Entry<String, String> es : nd.getTags().entrySet()) {
				String tag = es.getKey();
				String value = es.getValue();
				if(TAGS_TO_ACCEPT.contains(tag) || TAGS_TO_SAVE.contains(tag) || tag.startsWith("access")) {
					if(!pointTypes.containsKey(nd.getId())) {
						pointTypes.put(nd.getId(), new TIntArrayList());
					}
					pointTypes.get(nd.getId()).add(registerRule(tag, value).id);
				}
			}	
		}
		return true;
	}
	
	private MapRouteType registerRule(String tag, String val) {
		String id = constructRuleKey(tag, val);
		if(!types.containsKey(id)) {
			MapRouteType rt = new MapRouteType();
			rt.id = types.size();
			rt.tag = tag;
			rt.value = val;
			types.put(id, rt);
		}
		MapRouteType type = types.get(id);
		type.count ++;
		return type;
	}
	
	public static class MapRouteType {
		int count = 0;
		int id;
		String tag;
		String value;
		
	}
}
