package com.osmand.data.preparation;

import java.io.File;
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

import com.osmand.Algoritms;
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
	
//	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
//		new DataExtraction().testReadingOsmFile();
//	}
	
	// External files
	public static String pathToTestDataDir = "E:\\Information\\OSM maps\\";
	public static String pathToOsmFile =  pathToTestDataDir + "minsk.osm";
	public static String pathToOsmBz2File =  pathToTestDataDir + "belarus_2010_04_01.osm.bz2";
	public static String pathToWorkingDir = pathToTestDataDir +"osmand\\";
	public static String pathToDirWithTiles = pathToWorkingDir +"tiles";
	public static String writeTestOsmFile = "C:\\1_tmp.osm"; // could be null - wo writing
	private static boolean parseSmallFile = true;
	private static boolean parseOSM = true;
	
	

	///////////////////////////////////////////
	// Test method for local purposes
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
			country = readCountry(f, new ConsoleProgressImplementation(), null);
		} else {
			country = new Region();
			country.setStorage(new OsmBaseStorage());
		}
		
       
        // TODO add interested objects
		List<Long> interestedObjects = new ArrayList<Long>();
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

	
	public Region readCountry(String path, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException{
		File f = new File(path);
		InputStream stream = new FileInputStream(f);
		InputStream streamFile = stream;
		long st = System.currentTimeMillis();
		if (path.endsWith(".bz2")) {
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
			} else {
				stream = new CBZip2InputStream(stream);
			}
		}
		
		if(progress != null){
			progress.startTask("Loading file " + path, -1);
		}

		// preloaded data
		final ArrayList<Node> places = new ArrayList<Node>();
		final ArrayList<Entity> buildings = new ArrayList<Entity>();
		final ArrayList<Amenity> amenities = new ArrayList<Amenity>();
		final ArrayList<Way> ways = new ArrayList<Way>();
		
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
		if (addFilter != null) {
			storage.getFilters().add(addFilter);
		}
		storage.getFilters().add(filter);

		storage.parseOSM(stream, progress, streamFile);
		if (log.isDebugEnabled()) {
			log.debug("File parsed : " + (System.currentTimeMillis() - st));
		}
        
        // 1. Initialize region
        Region country = new Region();
        int i = f.getName().indexOf('.');
        country.setName(Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i)));
        country.setStorage(storage);

        // 2. Reading amenities
        readingAmenities(amenities, country);

        // 3. Reading cities
        readingCities(places, country);

        // 4. Reading streets
        readingStreets(progress, ways, country);
        
        // 5. reading buildings
        readingBuildings(progress, buildings, country);
        
        country.doDataPreparation();
        return country;
	}


	private void readingBuildings(IProgress progress, final ArrayList<Entity> buildings, Region country) {
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
	}


	private void readingStreets(IProgress progress, final ArrayList<Way> ways, Region country) {
		progress.startTask("Indexing streets...", ways.size());
        DataTileManager<Way> waysManager = new DataTileManager<Way>();
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
	}


	private void readingAmenities(final ArrayList<Amenity> amenities, Region country) {
		for(Amenity a: amenities){
        	country.registerAmenity(a);
        }
	}

	
	public void readingCities(ArrayList<Node> places, Region country) {
		for (Node s : places) {
			String place = s.getTag(OSMTagKey.PLACE);
			if (place == null) {
				continue;
			}
			country.registerCity(s);
		}
	}
	
}
