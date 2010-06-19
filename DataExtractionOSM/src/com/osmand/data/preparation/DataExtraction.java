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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

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
import com.osmand.data.TransportRoute;
import com.osmand.data.TransportStop;
import com.osmand.data.City.CityType;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings;
import com.osmand.osm.Relation;
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
	private final boolean parseEntityInfo;
	private final boolean indexTransport;
	private File workingDir = null;

	

	
	
	public DataExtraction(boolean indexAddress, boolean indexPOI, boolean indexTransport, boolean normalizeStreets, 
			boolean loadAllObjects, boolean parseEntityInfo, File workingDir){
		this.indexAddress = indexAddress;
		this.indexPOI = indexPOI;
		this.indexTransport = indexTransport;
		this.normalizeStreets = normalizeStreets;
		this.loadAllObjects = loadAllObjects;
		this.parseEntityInfo = parseEntityInfo;
		this.workingDir = workingDir;
		
	}

	
	protected class DataExtractionOsmFilter implements IOsmStorageFilter {
		ArrayList<Node> places = new ArrayList<Node>();
		ArrayList<Entity> buildings = new ArrayList<Entity>();
		ArrayList<Entity> amenities = new ArrayList<Entity>();
		ArrayList<Way> ways = new ArrayList<Way>();
		ArrayList<Relation> transport = new ArrayList<Relation>();
		Map<Long, String> postalCodes = new LinkedHashMap<Long, String>();

		int currentCount = 0;
		private Connection conn;
		private PreparedStatement prep;
		

		public DataExtractionOsmFilter() {
		}
		
		public ArrayList<Node> getPlaces() {
			return places;
		}
		public ArrayList<Entity> getBuildings() {
			return buildings;
		}
		public ArrayList<Entity> getAmenities() {
			return amenities;
		}
		public ArrayList<Way> getWays() {
			return ways;
		}
		public ArrayList<Relation> getTransport() {
			return transport;
		}
		
		public Map<Long, String> getPostalCodes() {
			return postalCodes;
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
			Collection<Entity> values = new ArrayList<Entity>(storage.getRegisteredEntities().values());
			for (Entity e : values) {
				progress.progress(1);
				if (e instanceof Way || e instanceof Relation) {
					map.clear();
					Collection<Long> ids = e instanceof Way ? ((Way) e).getNodeIds() : ((Relation) e).getMemberIds();
					for (Long i : ids) {
						if (!storage.getRegisteredEntities().containsKey(i)) {
							pselect.setLong(1, i);
							if (pselect.execute()) {
								ResultSet rs = pselect.getResultSet();
								if (rs.next()) {
									storage.getRegisteredEntities().put(i, new Node(rs.getDouble(2), rs.getDouble(3), rs.getLong(1)));
								}
								rs.close();
							}
						}
						if(storage.getRegisteredEntities().containsKey(i)){
							map.put(i, storage.getRegisteredEntities().get(i));
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
				if(e instanceof Relation){
					if(e.getTag(OSMTagKey.POSTAL_CODE) != null){
						String tag = e.getTag(OSMTagKey.POSTAL_CODE);
						for(Long l : ((Relation)e).getMemberIds()){
							postalCodes.put(l, tag);
						}
					}
					// do not need to mark processed
				}
			}
			if(indexTransport){
				if(e instanceof Relation && e.getTag(OSMTagKey.ROUTE) != null){
					transport.add((Relation) e);
					processed = true;
				}
				if(e instanceof Way){
					processed = true;
				}
			}
			// put all nodes into temporary db to get only required nodes after loading all data 
			try {
				if (e instanceof Node) {
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
	// netherlands.osm.bz2 1674 seconds - read
	
	// Information about progress for belarus.osm [165 seconds] - 580mb
//	FINE: Loading file E:\Information\OSM maps\belarus_2010_06_02.osm started - 61%
//	FINE: Correlating data... started after 101921 ms - 10%
//	FINE: Indexing poi... started after 17062 ms - 0 %
//	FINE: Indexing cities... started after 47 ms - 0%
//	FINE: Indexing streets... started after 94 ms - 10%
//	FINE: Indexing buildings... started after 16531 ms - 20 %
//	FINE: Normalizing name streets... started after 30890 ms - 0%
	
	// belarus.osm.bz2 [273 ms] - 40mb
//	FINE: Memory before task exec: 16 252 928 free : 11 676 888
//	FINE: Loading file E:\Information\OSM maps\belarus osm\belarus_2010_06_02.osm.bz2 started - 73 % 
//	FINE: Memory before task exec: 95 760 384 free : 17 704 984
//	FINE: Correlating data... started after 203 657 ms - 7 %
//	FINE: Indexing poi... started after 20 204 ms
//	FINE: Indexing cities... started after 62 ms
//	FINE: Memory before task exec: 95 760 384 free : 45 752 80
//	FINE: Indexing streets... started after 94 ms - 7 %
//	FINE: Memory before task exec: 167 510 016 free : 91 616 528
//	FINE: Indexing buildings... started after 18 672 ms - 13 %
//	FINE: Memory before task exec: 167 510 016 free : 76 993 976
//	FINE: Normalizing name streets... started after 32 719 ms
	
	// minsk.bz2 [36] - 4mb
//	FINE: Total mem: 16252928 free : 8370296
//	FINE: Loading file E:\Information\OSM maps\minsk_extr.bz2 started - 63% - 90 % (memory)
//	FINE: Total mem: 64139264 free : 25069688
//	FINE: Correlating data... started after 23829 ms - 27%
//	FINE: Indexing poi... started after 10547 ms - 0%
//	FINE: Indexing cities... started after 31 ms - 0%
//	FINE: Indexing streets... started after 94 ms - 1%
//	FINE: Indexing buildings... started after 672 ms - 7%
//	FINE: Normalizing name streets... started after 2421 ms - 0%
//	FINE: Total mem: 64139264 free : 22226792
	
	// chech.bz2 [1090 ms] - 185mb
//	FINE: Total mem: 16 252 928 free : 9 132 960
//	FINE: Loading file E:\Information\OSM maps\czech_republic.osm.bz2 started - 78 % - 90 % (memory)
//	FINE: Total mem: 226 877 440 free : 42 500 592
//	FINE: Correlating data... started after 857 788 ms  - 5 %
//	FINE: Indexing poi... started after 58 173 ms
//	FINE: Total mem: 259 522 560 free : 77 918 344
//	FINE: Indexing cities... started after 171 ms
//	FINE: Indexing streets... started after 188 ms - 12 %
//	FINE: Indexing buildings... started after 135 250 ms -  3 %
//	FINE: Normalizing name streets... started after 36 657 ms
//	FINE: Total mem: 259 522 560 free : 8 697 952

	
	public Region readCountry(String path, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException, SQLException{

		
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

        DataExtractionOsmFilter filter = new DataExtractionOsmFilter();
		// data to load & index
		final ArrayList<Node> places = filter.getPlaces();
		final ArrayList<Entity> buildings = filter.getBuildings();
		final ArrayList<Entity> amenities = filter.getAmenities();
		final ArrayList<Way> ways = filter.getWays();
		final ArrayList<Relation> transport = filter.getTransport();
		Map<Long, String> postalCodes = filter.getPostalCodes();
        storage.getFilters().add(filter);
        // 0. Loading osm file
		try {
			// 0.1 init database to store temporary data
			filter.initDatabase();
			
			// 0.2 parsing osm itself
			progress.setGeneralProgress("[40 of 100]");
			storage.parseOSM(stream, progress, streamFile, parseEntityInfo);
			if (log.isInfoEnabled()) {
				log.info("File parsed : " + (System.currentTimeMillis() - st));
			}
			progress.finishTask();
			
			progress.setGeneralProgress("[55 of 100]");
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
        progress.setGeneralProgress("[60 of 100]");
        progress.startTask("Indexing poi...", -1);
		if (indexPOI) {
			readingAmenities(amenities, country);
		}

        // 3. Reading cities
		progress.setGeneralProgress("[65 of 100]");
        progress.startTask("Indexing cities...", -1);
        readingCities(places, country);

        if (indexAddress) {
			// 4. Reading streets
        	progress.setGeneralProgress("[80 of 100]");
			readingStreets(progress, ways, country);

			// 5. reading buildings
			progress.setGeneralProgress("[95 of 100]");
			readingBuildings(progress, buildings, country, postalCodes);
		}
        
        progress.setGeneralProgress("[100 of 100]");
        if(normalizeStreets){
        	// 	6. normalizing streets
        	normalizingStreets(progress, country);
        }

        // 7. Indexing transport
        if(indexTransport){
        	readingTransport(transport, country, progress);
        }
        // 8. Call data preparation to sort cities, calculate center location, assign id to objects 
        country.doDataPreparation();
        
        // 9. Transliterate names to english
        
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
        for(List<TransportRoute> r : country.getTransportRoutes().values()){
        	for(TransportRoute route : r){
        		convertEnglishName(route);
        		for(TransportStop s : route.getBackwardStops()){
        			convertEnglishName(s);
        		}
        		for(TransportStop s : route.getForwardStops()){
        			convertEnglishName(s);
        		}
        	}
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


	private void readingBuildings(IProgress progress, final ArrayList<Entity> buildings, Region country, Map<Long, String> postalCodes) {
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
					Building building = city.registerBuilding(b);
					if(postalCodes.containsKey(building.getId()) ){
						building.setPostcode(postalCodes.get(building.getId()));
					}
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

	
	public void readingTransport(final ArrayList<Relation> transport, Region country, IProgress progress){
		progress.startTask("Reading transport...", -1);
		Map<String, List<TransportRoute>> routes = country.getTransportRoutes();
		Map<Long, TransportStop> routeStops = new LinkedHashMap<Long, TransportStop>();
		for(Relation rel : transport){
			String ref = rel.getTag(OSMTagKey.REF);
			String route = rel.getTag(OSMTagKey.ROUTE);
			if(route == null || ref == null){
				continue;
			}
			String operator = rel.getTag(OSMTagKey.OPERATOR);
			if(operator != null){
				route = operator + " : " + route;
			} 
			if(!routes.containsKey(route)){
				routes.put(route, new ArrayList<TransportRoute>());
			}
			
			TransportRoute r = new TransportRoute(rel, ref);
			for(Entry<Entity, String> e: rel.getMemberEntities().entrySet()){
				if(e.getValue().contains("stop")){
					if(e.getKey() instanceof Node){
						if(!routeStops.containsKey(e.getKey().getId())){
							routeStops.put(e.getKey().getId(), new TransportStop(e.getKey()));
						}
						TransportStop stop = routeStops.get(e.getKey().getId());
						boolean forward = e.getValue().contains("forward") || !e.getValue().contains("backward");
						if(forward){
							r.getForwardStops().add(stop);
						} else {
							r.getBackwardStops().add(stop);
						}
					}
					
				} else if(e.getKey() instanceof Way){
					r.addWay((Way) e.getKey());
				}
			}
			if(r.getBackwardStops().isEmpty() && !r.getForwardStops().isEmpty()){
				List<TransportStop> stops = r.getBackwardStops();
				for(TransportStop s : r.getForwardStops()){
					stops.add(0, s);
				}
			} else if(!r.getForwardStops().isEmpty()){
				if(r.getForwardStops().get(0) != r.getBackwardStops().get(r.getBackwardStops().size() - 1)){
					r.getBackwardStops().add(r.getForwardStops().get(0));
				}
			} else {
				continue;
			}
			routes.get(route).add(r);
		}
		
		progress.finishTask();
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
	
	// Performance testing methods
	public static void main(String[] args) throws IOException, SAXException, SQLException, ParserConfigurationException {
		long time = System.currentTimeMillis();
		OsmBaseStorage storage = new OsmBaseStorage();
		String path = "E:\\Information\\OSM maps\\belarus_2010_06_02.osm";
//		String path = "E:\\Information\\OSM maps\\minsk_extr.bz2";
//		String path = "E:\\Information\\OSM maps\\netherlands.osm.bz2";
		String wDir = "E:\\Information\\OSM maps\\osmand\\";
		
		
		File f = new File(path);
		InputStream stream = new FileInputStream(f);
		InputStream streamFile = stream;
		if (path.endsWith(".bz2")) {
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
			} else {
				stream = new CBZip2InputStream(stream);
			}
		}
		// only sax parser
//		if(true){
//			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
//			parser.parse(f, new DefaultHandler());
//			System.out.println("All time " + (System.currentTimeMillis() - time) + " ms"); //
//		}
		
		
		storage.getFilters().add(new IOsmStorageFilter(){
			@Override
			public boolean acceptEntityToLoad(OsmBaseStorage storage, Entity entity) {
				return false;
			}
		});
		DataExtraction e = new DataExtraction(true, true, true, true, false, true, new File(wDir));
		
		DataExtractionOsmFilter filter = e.new DataExtractionOsmFilter(); 
		filter.initDatabase();
		storage.getFilters().add(filter);
		
		// belarus.osm - 22 843 (only sax), 33 344 (wo filter), 82 829 (filter)
//		storage.parseOSM(stream, null, streamFile, false);  
		// belarus.osm - 46 938 (wo filter), 98 188 (filter)
		// netherlands.osm - 1743 511 (wo filter)
		storage.parseOSM(stream, new ConsoleProgressImplementation(), streamFile, true);
		System.out.println("Total mem: " + Runtime.getRuntime().totalMemory() + " free : " + Runtime.getRuntime().freeMemory());
		System.out.println("All time " + (System.currentTimeMillis() - time) + " ms"); //
//		System.out.println(amenities.size() + " " + buildings.size() + " " + places.size() + " " + ways.size());
	}
	
}
