package com.osmand.data.preparation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

import com.osmand.Algoritms;
import com.osmand.IProgress;
import com.osmand.data.Amenity;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.DataTileManager;
import com.osmand.data.MapObject;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.City.CityType;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings;
import com.osmand.osm.Way;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.IOsmStorageFilter;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.swing.DataExtractionSettings;


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

	public static final int BATCH_SIZE = 5000;
	public static final String NODES_DB = "nodes.db";
	
	private final boolean loadAllObjects;
	private final boolean normalizeStreets;
	private final boolean indexAddress;
	private final boolean indexPOI;
	private File workingDir = null;
	
	public DataExtraction(boolean indexAddress, boolean indexPOI, boolean normalizeStreets, boolean loadAllObjects, File workingDir){
		this.indexAddress = indexAddress;
		this.indexPOI = indexPOI;
		this.normalizeStreets = normalizeStreets;
		this.loadAllObjects = loadAllObjects;
		this.workingDir = workingDir;
		
	}

	
	protected class DataExtractionOsmFilter implements IOsmStorageFilter {
		final ArrayList<Node> places;
		final ArrayList<Entity> buildings;
		final ArrayList<Entity> amenities;
		final ArrayList<Way> ways;

		int currentCount = 0;
		private Connection conn;
		private PreparedStatement prep;

		public DataExtractionOsmFilter(ArrayList<Entity> amenities, ArrayList<Entity> buildings, ArrayList<Node> places,
				ArrayList<Way> ways) {
			this.amenities = amenities;
			this.buildings = buildings;
			this.places = places;
			this.ways = ways;
		}

		public void initDatabase() throws SQLException {
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}

			File file = new File(workingDir, NODES_DB);
			// to save space
			if(file.exists()){
				file.delete();
			}
			// creating nodes db to fast access for all nodes
			conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

			// prepare tables
			Statement stat = conn.createStatement();
			stat.executeUpdate("drop table if exists node;");
			stat.executeUpdate("create table node (id long, latitude double, longitude double);");
			stat.executeUpdate("create index IdIndex ON node (id);");
			stat.close();

			prep = conn.prepareStatement("insert into node values (?, ?, ?);");
			conn.setAutoCommit(false);
		}

		public void correlateData(OsmBaseStorage storage, IProgress progress) throws SQLException {
			if (currentCount > 0) {
				prep.executeBatch();
			}
			prep.close();
			conn.setAutoCommit(true);
			final PreparedStatement pselect = conn.prepareStatement("select * from node where id = ?");
			Map<Long, Entity> map = new LinkedHashMap<Long, Entity>();
			progress.startTask("Correlating data...", storage.getRegisteredEntities().size());
			for (Entity e : storage.getRegisteredEntities().values()) {
				progress.progress(1);
				if (e instanceof Way) {
					map.clear();
					for (Long i : ((Way) e).getNodeIds()) {
						pselect.setLong(1, i);
						if (pselect.execute()) {
							ResultSet rs = pselect.getResultSet();
							if (rs.next()) {
								map.put(i, new Node(rs.getDouble(2), rs.getDouble(3), rs.getLong(1)));
							}
							rs.close();
						}
					}
					e.initializeLinks(map);
				}
			}
		}

		public void close() {
			if (conn != null) {
				try {
					conn.close();
					new File(workingDir, NODES_DB).delete();
				} catch (SQLException e) {
				}
			}
		}

		@Override
		public boolean acceptEntityToLoad(OsmBaseStorage storage, Entity e) {
			boolean processed = false;
			if (indexAddress) {
				if ("yes".equals(e.getTag(OSMTagKey.BUILDING))) {
					if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
						buildings.add(e);
						processed = true;
					}
				}
			}
			if (indexPOI && Amenity.isAmenity(e)) {
				amenities.add(e);
				processed = true;
			}
			if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
				places.add((Node) e);
				processed = true;
			}
			if (indexAddress) {
				// suppose that streets are way for car
				if (e instanceof Way && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY)) && e.getTag(OSMTagKey.NAME) != null) {
					ways.add((Way) e);
					processed = true;
				}
			}
			// put all nodes into temporary db to get only required nodes after loading all data 
			try {
				if (e instanceof Node && indexAddress) {
					currentCount++;
					prep.setLong(1, e.getId());
					prep.setDouble(2, ((Node) e).getLatitude());
					prep.setDouble(3, ((Node) e).getLongitude());
					prep.addBatch();
					if (currentCount >= BATCH_SIZE) {
						prep.executeBatch();
						currentCount = 0;
					}
				}
			} catch (SQLException ex) {
				log.error("Could not save node", ex);
			}
			return processed || loadAllObjects;
		}

	}
	
	
	public Region readCountry(String path, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException, SQLException{
		// data to load & index
		final ArrayList<Node> places = new ArrayList<Node>();
		final ArrayList<Entity> buildings = new ArrayList<Entity>();
		final ArrayList<Entity> amenities = new ArrayList<Entity>();
		final ArrayList<Way> ways = new ArrayList<Way>();
		
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
        OsmBaseStorage storage = new OsmBaseStorage();
        storage.setSupressWarnings(DataExtractionSettings.getSettings().isSupressWarningsForDuplicatedId());
        if (addFilter != null) {
			storage.getFilters().add(addFilter);
		}

        DataExtractionOsmFilter filter = new DataExtractionOsmFilter(amenities, buildings, places, ways);
        storage.getFilters().add(filter);
        // 0. Loading osm file
		try {
			// 0.1 init database to store temporary data
			filter.initDatabase();
			
			// 0.2 parsing osm itself
			storage.parseOSM(stream, progress, streamFile);
			if (log.isInfoEnabled()) {
				log.info("File parsed : " + (System.currentTimeMillis() - st));
			}
			progress.finishTask();
			
			// 0.3 Correlating data (linking way & node)
			filter.correlateData(storage, progress);
			
		} finally {
			if (log.isInfoEnabled()) {
				log.info("File indexed : " + (System.currentTimeMillis() - st));
			}
			filter.close();
		}
        
        // 1. Initialize region
        Region country = new Region();
        int i = f.getName().indexOf('.');
        country.setName(Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i)));
        country.setStorage(storage);

        // 2. Reading amenities
		if (indexPOI) {
			readingAmenities(amenities, country);
		}

        // 3. Reading cities
        readingCities(places, country);

        if (indexAddress) {
			// 4. Reading streets
			readingStreets(progress, ways, country);

			// 5. reading buildings
			readingBuildings(progress, buildings, country);
		}
        
        if(normalizeStreets){
        	// 	6. normalizing streets
        	normalizingStreets(progress, country);
        }
        // 7. Call data preparation to sort cities, calculate center location, assign id to objects 
        country.doDataPreparation();
        // 8. Transliterate names to english
        
        convertEnglishName(country);
        for (CityType c : CityType.values()) {
        	for (City city : country.getCitiesByType(c)) {
				convertEnglishName(city);
				for (Street s : city.getStreets()) {
					convertEnglishName(s);
					for (Building b : s.getBuildings()) {
						convertEnglishName(b);
					}
				}
			}
		}
        for (Amenity a : country.getAmenityManager().getAllObjects()) {
			convertEnglishName(a);
		}
        return country;
	}
	// icu4j example - icu is not good in transliteration russian names
//	Transliterator latin = Transliterator.getInstance("Any-Latin;NFD;[:Nonspacing Mark:] Remove;NFC");
	
	private void convertEnglishName(MapObject o){
		String name = o.getName();
		if(name != null && (o.getEnName() == null || o.getEnName().isEmpty())){
			o.setEnName(Junidecode.unidecode(name));
//			o.setEnName(transliterator.transliterate(name));
		}
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
						str.getWayNodes().add(w);
					}
					waysManager.registerObject(center.getLatitude(), center.getLongitude(), w);
				}
			}
		}
        progress.finishTask();
        /// way with name : МЗОР, ул. ...,
	}


	private void readingAmenities(final ArrayList<Entity> amenities, Region country) {
		for(Entity a: amenities){
        	country.registerAmenity(new Amenity(a));
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
	
	
	private int checkSuffix(String name, String suffix){
		int i = -1;
		boolean searchAgain = false;
		do {
			i = name.indexOf(suffix, i);
			searchAgain = false;
			if (i > 0) {
				if (Character.isLetterOrDigit(name.charAt(i -1))) {
					i ++;
					searchAgain = true;
				}
			}
		} while (searchAgain);
		return i; 
	}
	
	private String cutSuffix(String name, int ind, int suffixLength){
		String newName = name.substring(0, ind);
		if (name.length() > ind + suffixLength + 1) {
			newName += name.substring(ind + suffixLength + 1);
		}
		return newName.trim();
	}
	
	private String putSuffixToEnd(String name, int ind, int suffixLength) {
		if (name.length() <= ind + suffixLength) {
			return name;
		
		}
		String newName;
		if(ind > 0){
			newName = name.substring(0, ind);
			newName += name.substring(ind + suffixLength);
			newName += name.substring(ind - 1, ind + suffixLength );
		} else {
			newName = name.substring(suffixLength + 1) + name.charAt(suffixLength) + name.substring(0, suffixLength);
		}
		
		return newName.trim();
	}
	
	public void normalizingStreets(IProgress progress, Region region){
		progress.startTask("Normalizing name streets...", -1);
		String[] defaultSuffixes = DataExtractionSettings.getSettings().getDefaultSuffixesToNormalizeStreets();
		String[] suffixes = DataExtractionSettings.getSettings().getSuffixesToNormalizeStreets();
		for(CityType t : CityType.values()){
			for(City c : region.getCitiesByType(t)){
				ArrayList<Street> list = new ArrayList<Street>(c.getStreets());
				for (Street s : list) {
					String name = s.getName();
					String newName = name.trim();
					boolean processed = newName.length() != name.length();
					for (String ch : defaultSuffixes) {
						int ind = checkSuffix(newName, ch);
						if (ind != -1) {
							newName = cutSuffix(newName, ind, ch.length());
							processed = true;
							break;
						}
					}

					if (!processed) {
						for (String ch : suffixes) {
							int ind = checkSuffix(newName, ch);
							if (ind != -1) {
								newName = putSuffixToEnd(newName, ind, ch.length());
								processed = true;
								break;
							}
						}
					}
					if (processed) {
						s.setName(newName);
					}
				}
			}
		}
	}
	
}
