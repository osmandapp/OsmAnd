package com.osmand.osm.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
	
	public Map<String, Properties> getSupermarkets() throws IOException {
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
	private Map<String, Properties> parseJSON(JSONArray array) {
		HashMap<String,Properties> map = new HashMap<String, Properties>();
		for (int i=0;i<array.length();i++) {
			try {
				JSONObject object = (JSONObject) array.get(i);
				String id = object.getString("no");
				if (id != null)  {
					Iterator<String> keys = object.keys();
					Properties properties = new Properties();
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
		Map<String, Properties> map = resolver.getSupermarkets();
		assert map.size() == 2;
		assert map.containsKey("5816");
		assert map.containsKey("5817");
		// further  checks
		
		Properties shop = map.get("5816");
		assert "Eindhoven".equals(shop.get("city"));
		
		shop = map.get("5817");
		assert "Amsterdam".equals(shop.get("city"));
	}
	
/*	public static void main(String[] args) throws IOException {
		selfTest();
	}*/
}