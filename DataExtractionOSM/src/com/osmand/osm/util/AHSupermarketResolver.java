package com.osmand.osm.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import com.osmand.Algoritms;
import com.osmand.data.DataTileManager;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.EntityInfo;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OpeningHoursParser;
import com.osmand.osm.Entity.EntityId;
import com.osmand.osm.OpeningHoursParser.BasicDayOpeningHourRule;
import com.osmand.osm.OpeningHoursParser.OpeningHoursRule;
import com.osmand.osm.io.IOsmStorageFilter;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.osm.io.OsmStorageWriter;
import com.osmand.swing.DataExtractionSettings;
import com.osmand.swing.MapPanel;
import com.osmand.swing.MapPointsLayer;

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
	// http://xapi.openstreetmap.org/api/0.6/*[shop=supermarket][bbox=2.5,50,7.8,53.5]
	public void updateOSMFile(String pathToOsmFile, String pathToModifiedFile, boolean show) throws IOException, SAXException, XMLStreamException, JSONException{
		OsmBaseStorage storage = new OsmBaseStorage();
		final Map<String, EntityId> winkelNumbers = new LinkedHashMap<String, EntityId>();
		
		storage.getFilters().add(new IOsmStorageFilter(){

			@Override
			public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity) {
				if(entity.getTag("winkelnummer") !=null && entity.getTag("name").contains("eijn")){
					winkelNumbers.put(entity.getTag("winkelnummer"), entityId);
					return true;
				}
				// register all nodes in order to operate with ways
				return true;
			}
			
		});
		storage.parseOSM(new FileInputStream(pathToOsmFile), new ConsoleProgressImplementation(2), null, true);
		Map<String, Map<String, Object>> supermarkets = getSupermarkets();
		
		DataTileManager<Entity> deleted = new DataTileManager<Entity>();
		
		for(String s : winkelNumbers.keySet()){
			if(!supermarkets.containsKey(s)){
				System.err.println("Shop " + s + " id=" +winkelNumbers.get(s) + " doesn't present on the site.");
				EntityId e = winkelNumbers.get(s);
				Entity en = storage.getRegisteredEntities().get(e);
				deleted.registerObject(en.getLatLon().getLatitude(), en.getLatLon().getLongitude(), 
						en);
			}
		}
		
		DataTileManager<Entity> notCorrelated = new DataTileManager<Entity>();
		DataTileManager<Entity> notShown = new DataTileManager<Entity>();
		
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
					System.err.println("Winkel number = " + s + " is too far from site info - " + dist + " m !!! " + real);
					if(dist > 300){
						notCorrelated.registerObject(real.getLatitude(), real.getLongitude(), e);
					}
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
				// TODO?
				LatLon real = new LatLon((Double)props.get("lat"), (Double) props.get("lng"));
				System.err.println("Winkel number = " + s + " is not found in database !!! " + real);
				Node n = new Node(real.getLatitude(), real.getLongitude(), -1);
				n.putTag("winkelnummer", "REG : " + s);
				notShown.registerObject(real.getLatitude(), real.getLongitude(), n);
			}
			
		}
		
		OsmStorageWriter writer = new OsmStorageWriter();
		writer.saveStorage(new FileOutputStream(pathToModifiedFile), storage, null, true);
		if(show){
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			final MapPanel panel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
			panel.setFocusable(true);
			MapPointsLayer toAdd = panel.getLayer(MapPointsLayer.class);
			toAdd.setPoints(notShown);
			toAdd.setPointSize(5);
			toAdd.setTagToShow("winkelnummer");
			
			
			MapPointsLayer red = new MapPointsLayer();
			red.setPoints(deleted);
			red.setColor(Color.red);
			red.setPointSize(5);
			panel.addLayer(red);
			
			MapPointsLayer blue = new MapPointsLayer();
			blue.setPoints(notCorrelated);
			blue.setColor(Color.blue);
			blue.setPointSize(4);
			panel.addLayer(blue);
			
			
			JFrame frame = new JFrame("Map view");
		    
			
		    frame.addWindowListener(new WindowAdapter(){
		    	@Override
		    	public void windowClosing(WindowEvent e) {
		    		DataExtractionSettings settings = DataExtractionSettings.getSettings();
					settings.saveDefaultLocation(panel.getLatitude(), panel.getLongitude());
					settings.saveDefaultZoom(panel.getZoom());
		    		System.exit(0);
		    	}
		    });
		    Container content = frame.getContentPane();
		    content.add(panel, BorderLayout.CENTER);

		    JMenuBar bar = new JMenuBar();
		    bar.add(MapPanel.getMenuToChooseSource(panel));
		    frame.setJMenuBar(bar);
		    frame.setSize(512, 512);
		    frame.setVisible(true);
			
		}
	}
	
	public static void main(String[] args) throws IOException, SAXException, XMLStreamException, JSONException {
		AHSupermarketResolver resolver = new AHSupermarketResolver();
		resolver.updateOSMFile("e:/Information/OSM maps/osm_map/holl_supermarket.osm", "e:/Information/OSM maps/osm_map/ams_poi_mod.osm",
				true);
	}
}