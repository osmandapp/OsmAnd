package com.osmand.osm.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import com.osmand.Algoritms;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.EntityInfo;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.OpeningHoursParser;
import com.osmand.osm.Entity.EntityId;
import com.osmand.osm.OpeningHoursParser.BasicDayOpeningHourRule;
import com.osmand.osm.OpeningHoursParser.OpeningHoursRule;
import com.osmand.osm.io.IOsmStorageFilter;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.osm.io.OsmStorageWriter;

/**
 * Downloads list of Albert Heijn supermarkets and converts them to a map.
 * 
 * Source for json is:
 * http://www.ah.nl/albertheijn/winkelinformatie/winkels
 * 
 * @author Andrei Adamian
 *
 */
public class AHSupermarketResolver {
	
	private Log log = LogFactory.getLog(AHSupermarketResolver.class);
	
	public Map<String, Map<String, Object>> getSupermarkets() throws IOException {
		InputStream stream = openStream();
		try {
			JSONArray object = new JSONArray(new JSONTokener(new InputStreamReader(stream)));
			return parseJSON(object);
		} catch (JSONException t) {
			throw new IllegalStateException("Can't read json stream",t);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Map<String, Object>> parseJSON(JSONArray array) {
		HashMap<String,Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
		for (int i=0;i<array.length();i++) {
			try {
				JSONObject object = (JSONObject) array.get(i);
				String id = object.getString("no");
				if (id != null)  {
					Iterator<String> keys = object.keys();
					Map<String, Object> properties = new LinkedHashMap<String, Object>();
					while (keys.hasNext()) {
						String name = keys.next();
						properties.put(name, object.get(name));
					}
					map.put(id, properties);
				} else {
					log.warn(String.format("Shop entry '%s' has been skipped, because 'no' property was not found", object));
				}
			} catch (JSONException e) {
				log.warn("Can't get object at index "+i,e);
			}
		}
		return map;
	}

	protected InputStream openStream() throws IOException {
		URL url = new URL("http://www.ah.nl/albertheijn/winkelinformatie/winkels");
		return url.openStream();
	}
	
	
	public static void selfTest() throws IOException {
		final String json = "[{city:'Eindhoven',lat:51.443278,format:'TOGO',lng:5.480161,sunday:true,street:'Neckerspoel',hours:[{F:'0630',U:'2300',D:'09-08-2010'},{F:'0630',U:'2300',D:'10-08-2010'},{F:'0630',U:'2300',D:'11-08-2010'},{F:'0630',U:'2300',D:'12-08-2010'},{F:'0630',U:'2330',D:'13-08-2010'},{F:'0700',U:'2330',D:'14-08-2010'},{F:'0800',U:'2300',D:'15-08-2010'},{F:'0630',U:'2300',D:'16-08-2010'},{F:'0630',U:'2300',D:'17-08-2010'},{F:'0630',U:'2300',D:'18-08-2010'},{F:'0630',U:'2300',D:'19-08-2010'},{F:'0630',U:'2330',D:'20-08-2010'},{F:'0700',U:'2330',D:'21-08-2010'},{F:'0800',U:'2300',D:'22-08-2010'}],no:5816,phone:'040-2376060',zip:'5611 AD',housenr:'10'},{city:'Amsterdam',lat:52.346837,format:'TOGO',lng:4.918422,sunday:true,street:'Julianaplein',hours:[{F:'0630',U:'2359',D:'09-08-2010'},{F:'0630',U:'2359',D:'10-08-2010'},{F:'0630',U:'2359',D:'11-08-2010'},{F:'0630',U:'2359',D:'12-08-2010'},{F:'0630',U:'2359',D:'13-08-2010'},{F:'0700',U:'2359',D:'14-08-2010'},{F:'0900',U:'2359',D:'15-08-2010'},{F:'0630',U:'2359',D:'16-08-2010'},{F:'0630',U:'2359',D:'17-08-2010'},{F:'0630',U:'2359',D:'18-08-2010'},{F:'0630',U:'2359',D:'19-08-2010'},{F:'0630',U:'2359',D:'20-08-2010'},{F:'0700',U:'2359',D:'21-08-2010'},{F:'0900',U:'2359',D:'22-08-2010'}],no:5817,phone:'020-4689944',zip:'1097 DN',housenr:'1'}]";
		AHSupermarketResolver resolver = new AHSupermarketResolver() {
			protected InputStream openStream() throws IOException {
				return new ByteArrayInputStream(json.getBytes());
			}
		};
		Map<String, Map<String, Object>> map = resolver.getSupermarkets();
		assert map.size() == 2;
		assert map.containsKey("5816");
		assert map.containsKey("5817");
		// further  checks
		
		Map<String, Object> shop = map.get("5816");
		assert "Eindhoven".equals(shop.get("city"));
		
		shop = map.get("5817");
		assert "Amsterdam".equals(shop.get("city"));
	}
	
	
	// this file could be retrieved using xapi
	// http://xapi.openstreetmap.org/api/0.6/node[shop=supermarket][bbox=2.5,50,7.8,53.5]
	public void updateOSMFile(String pathToOsmFile, String pathToModifiedFile) throws IOException, SAXException, XMLStreamException, JSONException{
		OsmBaseStorage storage = new OsmBaseStorage();
		final Map<String, EntityId> winkelNumbers = new LinkedHashMap<String, EntityId>();
		
		storage.getFilters().add(new IOsmStorageFilter(){

			@Override
			public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity) {
				if(entity.getTag("winkelnummer") !=null){
					winkelNumbers.put(entity.getTag("winkelnummer"), entityId);
					return true;
				}
				
				return false;
			}
			
		});
		storage.parseOSM(new FileInputStream(pathToOsmFile), new ConsoleProgressImplementation(2), null, true);
		Map<String, Map<String, Object>> supermarkets = getSupermarkets();
		for(String s : supermarkets.keySet()){
			Map<String, Object> props = supermarkets.get(s);
			if(winkelNumbers.get(s) != null){
				EntityId id = winkelNumbers.get(s);
				Entity e = storage.getRegisteredEntities().get(id);
				EntityInfo info = storage.getRegisteredEntityInfo().get(id);
				Map<String, String> newTags = new LinkedHashMap<String, String>();
				String p = props.get("format")+"";
				//IMPORTANT : comment what information should be updated or check
				String name = "Albert Heijn";
				if(!p.equals("AH")){
					name += " " + p;
				}
				newTags.put("name", name);
				newTags.put("phone", props.get("phone")+"");
				newTags.put("addr:city", props.get("city")+"");
				newTags.put("addr:street", props.get("street")+"");
				newTags.put("addr:housenumber", props.get("housenr")+"");
				newTags.put("addr:postcode", props.get("zip")+"");
				
				JSONArray o = (JSONArray) props.get("hours");
				List<OpeningHoursParser.OpeningHoursRule> rules = new ArrayList<OpeningHoursRule>();
				BasicDayOpeningHourRule prev = null;
				for(int i=0; i<7; i++){
					JSONObject obj = o.getJSONObject(i);
					
					if(!obj.isNull("C") && obj.getBoolean("C")){
					} else {
						String opened  = obj.get("F")+"";
						String closed = obj.get("U")+"";
						int start = Integer.parseInt(opened.substring(0, 2)) * 60 + Integer.parseInt(opened.substring(2));
						int end = Integer.parseInt(closed.substring(0, 2)) * 60 + Integer.parseInt(closed.substring(2));
						if(prev != null && prev.getStartTime() == start && prev.getEndTime() == end){
							prev.getDays()[i] = true;
						} else {
							BasicDayOpeningHourRule rule = new OpeningHoursParser.BasicDayOpeningHourRule();
							rule.getDays()[i] = true;
							rule.setStartTime(start);
							rule.setEndTime(end);
							prev = rule;
							rules.add(rule);
						}
					}
					
				}
				newTags.put("opening_hours", OpeningHoursParser.toStringOpenedHours(rules));
				
				// Check distance to info
				LatLon real = new LatLon((Double)props.get("lat"), (Double) props.get("lng"));
				double dist = MapUtils.getDistance(e.getLatLon(), real);
				if(dist > 150){
					// TODO move shop ?
					System.err.println("Winkel number = " + s + " is to far from site info - " + dist + " m !!! " + real);
				}
				boolean changed = false;
				for(String k : newTags.keySet()){
					String val = newTags.get(k);
					if(!Algoritms.objectEquals(val, e.getTag(k))){
						e.putTag(k, val);
						changed = true;
					}
				}
				if(changed){
					info.setAction("modify");
				}
			} else {
				// TODO add new shop ????
				LatLon real = new LatLon((Double)props.get("lat"), (Double) props.get("lng"));
				System.err.println("Winkel number = " + s + " is not found in database !!! " + real);
			}
			
		}
		
		OsmStorageWriter writer = new OsmStorageWriter();
		writer.saveStorage(new FileOutputStream(pathToModifiedFile), storage, null, true);
	}
	
	public static void main(String[] args) throws IOException, SAXException, XMLStreamException, JSONException {
		AHSupermarketResolver resolver = new AHSupermarketResolver();
		resolver.updateOSMFile("C:/ams_poi.osm", "C:/ams_poi_mod.osm");
	}
}