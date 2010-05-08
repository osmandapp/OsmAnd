package com.osmand;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.xml.sax.SAXException;

import com.osmand.data.Amenity;
import com.osmand.data.City;
import com.osmand.data.DataTileManager;
import com.osmand.data.Region;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.OSMStorageWriter;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.swing.OsmExtractionUI;


// TO implement
// 1. Full structured search for town/street/building.

/**
 * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Is_inside.2Foutside
 * http://wiki.openstreetmap.org/wiki/Relations/Proposed/Postal_Addresses
 * http://wiki.openstreetmap.org/wiki/Proposed_features/House_numbers/Karlsruhe_Schema#Tags (node, way)
 * 
 * 1. node  - place : country, state, region, county, city, town, village, hamlet, suburb
 *    That node means label for place ! It is widely used in OSM.
 *   
 * 2. way  - highway : primary, secondary, service. 
 *    That node means label for street if it is in city (primary, secondary, residential, tertiary, living_street), 
 *    beware sometimes that roads could outside city. Usage : often 
 *    
 *    outside city : trunk, motorway, motorway_link...
 *    special tags : lanes - 3, maxspeed - 90,  bridge
 * 
 * 3. relation - type = address. address:type : country, a1, a2, a3, a4, a5, a6, ... hno.
 *    member:node 		role=label :
 *    member:relation 	role=border :
 *    member:node		role=a1,a2... :
 * 
 * 4. node, way - addr:housenumber(+), addr:street(+), addr:country(+/-), addr:city(-) 
 * 	        building=yes
 * 
 * 5. relation - boundary=administrative, admin_level : 1, 2, ....
 * 
 * 6. node, way - addr:postcode =?
 *    relation  - type=postal_code (members way, node), postal_code=?
 *    
 * 7. node, way - amenity=?    
 *
 */
public class DataExtraction  {
	private static final Log log = LogFactory.getLog(DataExtraction.class);
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
		new DataExtraction().testReadingOsmFile();
	}
	
	
	private static boolean parseSmallFile = true;
	private static boolean parseOSM = true;
	private ArrayList<Way> mapWays;
	private ArrayList<Amenity> amenities;
	private ArrayList<Entity> buildings;
	private ArrayList<Node> places;
	private DataTileManager<Way> waysManager;

	///////////////////////////////////////////
	// 1. Reading data - preparing data for UI
	public void testReadingOsmFile() throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
		String f;
		if(parseSmallFile){
			f = DefaultLauncherConstants.pathToOsmFile;
		} else {
			f = DefaultLauncherConstants.pathToOsmBz2File;
		}
		long st = System.currentTimeMillis();
		
		Region country;
		if(parseOSM){
			country = readCountry(f, new ConsoleProgressImplementation());
		} else {
			country = new Region(null);
			country.setStorage(new OsmBaseStorage());
		}
		
       
        OsmExtractionUI ui = new OsmExtractionUI(country);
        ui.createUI();
        
		List<Long> interestedObjects = new ArrayList<Long>();
//		MapUtils.addIdsToList(places, interestedObjects);
//		MapUtils.addIdsToList(amenities, interestedObjects);
		MapUtils.addIdsToList(waysManager.getAllObjects(), interestedObjects);
//		MapUtils.addIdsToList(buildings, interestedObjects);
		if (DefaultLauncherConstants.writeTestOsmFile != null) {
			OSMStorageWriter writer = new OSMStorageWriter(country.getStorage().getRegisteredEntities());
			OutputStream output = new FileOutputStream(DefaultLauncherConstants.writeTestOsmFile);
			if (DefaultLauncherConstants.writeTestOsmFile.endsWith(".bz2")) {
				output.write('B');
				output.write('Z');
				output = new CBZip2OutputStream(output);
			}
			
			writer.saveStorage(output, interestedObjects, false);
			output.close();
		}
        
        System.out.println();
		System.out.println("USED Memory " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e6);
		System.out.println("TIME : " + (System.currentTimeMillis() - st));
	}

	
	public Region readCountry(String path, IProgress progress) throws IOException, SAXException{
		InputStream stream = new FileInputStream(path);
		InputStream streamFile = stream;
		long st = System.currentTimeMillis();
		if(path.endsWith(".bz2")){
			if (stream.read() != 'B' || stream.read() != 'Z')
				throw new RuntimeException(
						"The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
			else
				stream = new CBZip2InputStream(stream);	
		}
		
		if(progress != null){
			progress.startTask("Loading file " + path, -1);
		}

		// preloaded data
		places = new ArrayList<Node>();
		buildings = new ArrayList<Entity>();
		amenities = new ArrayList<Amenity>();
		// highways count
		mapWays = new ArrayList<Way>();
		
		OsmBaseStorage storage = new OsmBaseStorage(){
			@Override
			public boolean acceptEntityToLoad(Entity e) {
				if ("yes".equals(e.getTag(OSMTagKey.BUILDING))) {
					if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
						buildings.add(e);
						return true;
					}
				}
				return super.acceptEntityToLoad(e);
			}
			
			@Override
			public boolean acceptNodeToLoad(Node n) {
				// TODO accept amenity for way! hospital, university, parking, fast_food...
				if(Amenity.isAmenity(n)){
					amenities.add(new Amenity(n));
				}
				if (n.getTag(OSMTagKey.PLACE) != null) {
					places.add(n);
				}
				
				return true;
			}
			
			@Override
			public boolean acceptRelationToLoad(Relation w) {
				return false;
			}
			@Override
			public boolean acceptWayToLoad(Way w) {
				if (OSMSettings.wayForCar(w.getTag(OSMTagKey.HIGHWAY))) {
					mapWays.add(w);
					return true;
				}
				return false;
			}
		};
		

		storage.parseOSM(stream, progress, streamFile);
		if (log.isDebugEnabled()) {
			log.debug("File parsed : " + (System.currentTimeMillis() - st));
		}
        
        // 1. found towns !
        Region country = new Region(null);
        country.setStorage(storage);
        for (Node s : places) {
        	String place = s.getTag(OSMTagKey.PLACE);
        	if(place == null){
        		continue;
        	}
        	if("country".equals(place)){
        		country.setEntity(s);
        	} else {
        		City registerCity = country.registerCity(s);
        		if(registerCity == null){
        			System.out.println(place + " - " + s.getTag(OSMTagKey.NAME));
        		}
        	}
		}
        
        // 2. found buildings (index addresses)
        for(Entity b : buildings){
        	LatLon center = b.getLatLon();
        	// TODO first of all tag could be checked NodeUtil.getTag(e, "addr:city")
        	if(center == null){
        		// no nodes where loaded for this way
        	} else {
				City city = country.getClosestCity(center);
				if (city != null) {
					city.registerBuilding(center, b);
				}
			}
        }
        
        
        for(Amenity a: amenities){
        	country.registerAmenity(a);
        }
        
        
        waysManager = new DataTileManager<Way>();
        for (Way w : mapWays) {
        	if (w.getTag(OSMTagKey.NAME) != null) {
				LatLon latLon = MapUtils.getWeightCenterForNodes(w.getNodes());
				waysManager.registerObject(latLon.getLatitude(), latLon.getLongitude(), w);
			}
		}
        /// way with name : МЗОР, ул. ..., 
        
        return country;
	}
	
}
