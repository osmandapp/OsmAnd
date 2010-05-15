package com.osmand.data.preparation;

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

import com.osmand.IProgress;
import com.osmand.data.Amenity;
import com.osmand.data.City;
import com.osmand.data.DataTileManager;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.City.CityType;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings;
import com.osmand.osm.Way;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.IOsmStorageFilter;
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
	// External files
	public static String pathToTestDataDir = "E:\\Information\\OSM maps\\";
	public static String pathToOsmFile =  pathToTestDataDir + "minsk.osm";
	public static String pathToOsmBz2File =  pathToTestDataDir + "belarus_2010_04_01.osm.bz2";
	public static String pathToWorkingDir = pathToTestDataDir +"osmand\\";
	public static String pathToDirWithTiles = pathToWorkingDir +"tiles";
	public static String writeTestOsmFile = "C:\\1_tmp.osm"; // could be null - wo writing
	
	private static boolean parseSmallFile = true;
	private static boolean parseOSM = true;
	private ArrayList<Way> ways;
	private ArrayList<Amenity> amenities;
	private ArrayList<Entity> buildings;
	private ArrayList<Node> places;
	private DataTileManager<Way> waysManager;

	///////////////////////////////////////////
	// 1. Reading data - preparing data for UI
	public void testReadingOsmFile() throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
		String f;
		if(parseSmallFile){
			f = pathToOsmFile;
		} else {
			f = pathToOsmBz2File;
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
		if (writeTestOsmFile != null) {
			OSMStorageWriter writer = new OSMStorageWriter(country.getStorage().getRegisteredEntities());
			OutputStream output = new FileOutputStream(writeTestOsmFile);
			if (writeTestOsmFile.endsWith(".bz2")) {
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

	
	public Region readCountry(String path, IProgress progress, IOsmStorageFilter... filters) throws IOException, SAXException{
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
		ways = new ArrayList<Way>();
		
		IOsmStorageFilter filter = new IOsmStorageFilter(){
			@Override
			public boolean acceptEntityToLoad(OsmBaseStorage storage, Entity e) {
				if ("yes".equals(e.getTag(OSMTagKey.BUILDING))) {
					if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
						buildings.add(e);
						return true;
					}
				}
				if(Amenity.isAmenity(e)){
					amenities.add(new Amenity((Node) e));
					return true;
				}
				if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
					places.add((Node) e);
					return true;
				}
				if (e instanceof Way && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY))) {
					ways.add((Way) e);
					return true;
				}
				return e instanceof Node;
			}
		};
		
		OsmBaseStorage storage = new OsmBaseStorage();
		if(filters != null){
			for(IOsmStorageFilter f : filters){
				if(f != null){
					storage.getFilters().add(f);
				}
			}
		}
		storage.getFilters().add(filter);

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
        		country.registerCity(s);
        	}
		}
        
        
        
        for(Amenity a: amenities){
        	country.registerAmenity(a);
        }
        
        progress.startTask("Indexing streets...", ways.size());
        waysManager = new DataTileManager<Way>();
        for (Way w : ways) {
        	progress.progress(1);
        	if (w.getTag(OSMTagKey.NAME) != null) {
        		String street = w.getTag(OSMTagKey.NAME);
				LatLon center = MapUtils.getWeightCenterForNodes(w.getNodes());
				if (center != null) {
					City city = country.getClosestCity(center);
					if(city == null){
						Node n = new Node(center.getLatitude(), center.getLongitude(), -1);
						n.putTag(OSMTagKey.PLACE.getValue(), CityType.TOWN.name());
						n.putTag(OSMTagKey.NAME.getValue(), "Uknown city");
						country.registerCity(n);
						city = country.getClosestCity(center);
					}
					
					if (city != null) {
						Street str = city.registerStreet(street);
						for (Node n : w.getNodes()) {
							str.getWayNodes().add(n);
						}
					}
					waysManager.registerObject(center.getLatitude(), center.getLongitude(), w);
				}
			}
		}
        progress.finishTask();
        /// way with name : МЗОР, ул. ...,
        
        
        // found buildings (index addresses)
        progress.startTask("Indexing buildings...", buildings.size());
        for(Entity b : buildings){
        	LatLon center = b.getLatLon();
        	progress.progress(1);
        	// TODO first of all tag could be checked NodeUtil.getTag(e, "addr:city")
        	if(center == null){
        		// no nodes where loaded for this way
        	} else {
				City city = country.getClosestCity(center);
				if(city == null){
					Node n = new Node(center.getLatitude(), center.getLongitude(), -1);
					n.putTag(OSMTagKey.PLACE.getValue(), CityType.TOWN.name());
					n.putTag(OSMTagKey.NAME.getValue(), "Uknown city");
					country.registerCity(n);
					city = country.getClosestCity(center);
				}
				if (city != null) {
					city.registerBuilding(b);
				}
			}
        }
        progress.finishTask();
        
        country.doDataPreparation();
        return country;
	}
	
	
}
