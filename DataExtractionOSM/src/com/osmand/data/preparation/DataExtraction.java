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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.osmand.data.index.IndexConstants;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;
import com.osmand.osm.Entity.EntityId;
import com.osmand.osm.Entity.EntityType;
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
	public static final String TEMP_NODES_DB = "nodes"+IndexConstants.MAP_INDEX_EXT;
	
	
	private final boolean loadAllObjects;
	private final boolean normalizeStreets;
	private final boolean indexAddress;
	private final boolean indexPOI;
	private final boolean parseEntityInfo;
	private final boolean indexTransport;
	private File workingDir = null;

	
	// TODO : type=address
	//        address:a6=name_street
	//        address:type=a6
	// is_in..., label...
	
	// TODO     <member type="relation" ref="81833" role="is_in"/>
//    <member type="way" ref="25426285" role="border"/>
//    <tag k="address:house" v="13"/>
//    <tag k="address:type" v="house"/>
//    <tag k="type" v="address"/>
	
	
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
		ArrayList<Entity> amenities = new ArrayList<Entity>();
		
		Map<EntityId, Relation> addressRelations = new LinkedHashMap<EntityId, Relation>();
		Map<EntityId, Way> ways = new LinkedHashMap<EntityId, Way>();
		Map<EntityId, Entity> buildings = new LinkedHashMap<EntityId, Entity>();
		
		ArrayList<Relation> transport = new ArrayList<Relation>();
		Map<EntityId, String> postalCodes = new LinkedHashMap<EntityId, String>();

		
		private Connection conn;
//		
		private boolean preloadRelationAndWaysIntoDB = false; 
		private boolean createWholeOsmDB = false;
		
		int currentCountNode = 0;
		private PreparedStatement prepNode;
		int currentRelationsCount = 0;
		private PreparedStatement prepRelations;
		int currentWaysCount = 0;
		private PreparedStatement prepWays;
		int currentTagsCount = 0;
		private PreparedStatement prepTags;
		private final String regionName;
		private File dbFile;
		

		public DataExtractionOsmFilter(String regionName) {
			this.regionName = regionName;
		}
		
		public DataExtractionOsmFilter() {
			createWholeOsmDB = false;
			this.regionName = null;
		}
		
		public ArrayList<Node> getPlaces() {
			return places;
		}
		public Map<EntityId, Entity> getBuildings() {
			return buildings;
		}
		public ArrayList<Entity> getAmenities() {
			return amenities;
		}
		
		public Map<EntityId, Relation> getAddressRelations() {
			return addressRelations;
		}
		
		public Map<EntityId, Way> getWays() {
			return ways;
		}
		public ArrayList<Relation> getTransport() {
			return transport;
		}
		
		public Map<EntityId, String> getPostalCodes() {
			return postalCodes;
		}

		public void initDatabase() throws SQLException {
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			if(createWholeOsmDB){
				dbFile = new File(workingDir, regionName+IndexConstants.MAP_INDEX_EXT); 
			} else {
				dbFile = new File(workingDir, TEMP_NODES_DB);
			}
			// to save space
			if(dbFile.exists()){
				dbFile.delete();
			}
			// creating nodes db to fast access for all nodes
			conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

			// prepare tables
			Statement stat = conn.createStatement();
			stat.executeUpdate("drop table if exists node;");
			stat.executeUpdate("create table node (id long, latitude double, longitude double);");
			stat.executeUpdate("create index IdIndex ON node (id, latitude, longitude);");
			stat.executeUpdate("drop table if exists ways;");
			stat.executeUpdate("create table ways (id long, node long);");
			stat.executeUpdate("create index IdWIndex ON ways (id, node);");
			stat.executeUpdate("drop table if exists relations;");
			stat.executeUpdate("create table relations (id long, member long, type byte, role text);");
			stat.executeUpdate("create index IdRIndex ON relations (id, member, type);");
			stat.executeUpdate("drop table if exists tags;");
			stat.executeUpdate("create table tags (id long, type byte, key, value);");
			stat.executeUpdate("create index IdTIndex ON tags (id, type);");
			stat.execute("PRAGMA user_version = " + IndexConstants.MAP_TABLE_VERSION); //$NON-NLS-1$
			stat.close();

			prepNode = conn.prepareStatement("insert into node values (?, ?, ?);");
			prepWays = conn.prepareStatement("insert into ways values (?, ?);");
			prepRelations = conn.prepareStatement("insert into relations values (?, ?, ?, ?);");
			prepTags = conn.prepareStatement("insert into tags values (?, ?, ?, ?);");
			preloadRelationAndWaysIntoDB = indexTransport || indexAddress;
			conn.setAutoCommit(false);
		}

		public void correlateData(OsmBaseStorage storage, IProgress progress) throws SQLException {
			if (currentCountNode > 0) {
				prepNode.executeBatch();
			}
			prepNode.close();
			if (currentWaysCount > 0) {
				prepWays.executeBatch();
			}
			prepWays.close();
			if (currentRelationsCount > 0) {
				prepRelations.executeBatch();
			}
			prepRelations.close();
			if (currentTagsCount > 0) {
				prepTags.executeBatch();
			}
			prepTags.close();
			conn.setAutoCommit(true);
			
			final PreparedStatement pselectNode = conn.prepareStatement("select * from node where id = ?");
			final PreparedStatement pselectWay = conn.prepareStatement("select * from ways where id = ?");
			final PreparedStatement pselectRelation = conn.prepareStatement("select * from relations where id = ?");
			
			Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
			progress.startTask("Correlating data...", storage.getRegisteredEntities().size());
			ArrayList<Entity> values = new ArrayList<Entity>(storage.getRegisteredEntities().values());
			for (int ind = 0; ind < values.size(); ind++) {
				Entity e = values.get(ind);
				progress.progress(1);
				if (e instanceof Node) {
					continue;
				}
				map.clear();
				Collection<EntityId> ids = e instanceof Way ? ((Way) e).getEntityIds() : ((Relation) e).getMemberIds();
				for (EntityId i : ids) {
					if (!storage.getRegisteredEntities().containsKey(i)) {
						if (i.getType() == EntityType.NODE) {
							pselectNode.setLong(1, i.getId());
							if (pselectNode.execute()) {
								ResultSet rs = pselectNode.getResultSet();
								if (rs.next()) {
									storage.getRegisteredEntities().put(i, new Node(rs.getDouble(2), rs.getDouble(3), rs.getLong(1)));
								}
								rs.close();
							}
						} else if (i.getType() == EntityType.WAY) {
							pselectWay.setLong(1, i.getId());
							if (pselectWay.execute()) {
								ResultSet rs = pselectWay.getResultSet();
								Way way = new Way(i.getId());
								storage.getRegisteredEntities().put(i, way); 
								while (rs.next()) {
									way.addNode(rs.getLong(2));
								}
								// add way to load referred nodes
								values.add(way);
								rs.close();
							}
						} else if (i.getType() == EntityType.RELATION) {
							pselectRelation.setLong(1, i.getId());
							if (pselectRelation.execute()) {
								ResultSet rs = pselectNode.getResultSet();
								Relation rel = new Relation(i.getId());
								storage.getRegisteredEntities().put(i, rel);
								while (rs.next()) {
									rel.addMember(rs.getLong(1), EntityType.values()[rs.getByte(2)], rs.getString(3));
								}
								// do not load relation members recursively ? It is not needed for transport, address, poi before
								rs.close();
							}
						}
					}
					if (storage.getRegisteredEntities().containsKey(i)) {
						map.put(i, storage.getRegisteredEntities().get(i));
					}
				}
				e.initializeLinks(map);
			}
			
			pselectNode.close();
		}

		public void close() {
			if (conn != null) {
				try {
					conn.close();
					if(!createWholeOsmDB){
						dbFile.delete();
					}
				} catch (SQLException e) {
				}
			}
		}

		@Override
		public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity e) {
			boolean processed = false;
			if (indexPOI && Amenity.isAmenity(e)) {
				amenities.add(e);
				processed = true;
			}
			if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
				places.add((Node) e);
				processed = true;
			}
			if (indexAddress) {
				if ("yes".equals(e.getTag(OSMTagKey.BUILDING))) {
					if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null/*&& e.getTag(OSMTagKey.ADDR_STREET) != null*/) {
						buildings.put(entityId, e);
						processed = true;
					}
				}
				// suppose that streets are way for car
				if (e instanceof Way && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY)) && e.getTag(OSMTagKey.NAME) != null) {
					ways.put(entityId, (Way) e);
					processed = true;
				}
				if(e instanceof Relation){
					// do not need to mark processed
					if(e.getTag(OSMTagKey.POSTAL_CODE) != null){
						String tag = e.getTag(OSMTagKey.POSTAL_CODE);
						for(EntityId l : ((Relation)e).getMemberIds()){
							postalCodes.put(l, tag);
						}
					}
					
					if("address".equals(e.getTag(OSMTagKey.TYPE))){
						addressRelations.put(entityId, (Relation) e);
						processed = true;
					}

				}
			}
			if(indexTransport){
				if(e instanceof Relation && e.getTag(OSMTagKey.ROUTE) != null){
					transport.add((Relation) e);
					processed = true;
				}
				if(e instanceof Node && "bus_stop".equals(e.getTag(OSMTagKey.HIGHWAY))){
					// load all stops in order to get their names
					processed = true;
				}
				
				if (e instanceof Node && e.getTag(OSMTagKey.RAILWAY) != null) {
					if ("tram_stop".equals(e.getTag(OSMTagKey.RAILWAY)) || "station".equals(e.getTag(OSMTagKey.RAILWAY))) {
						// load all stops in order to get their names
						processed = true;
					}
				}
			}
			// put all nodes into temporary db to get only required nodes after loading all data 
			try {
				if (e instanceof Node) {
					currentCountNode++;
					prepNode.setLong(1, e.getId());
					prepNode.setDouble(2, ((Node) e).getLatitude());
					prepNode.setDouble(3, ((Node) e).getLongitude());
					prepNode.addBatch();
					if (currentCountNode >= BATCH_SIZE) {
						prepNode.executeBatch();
						currentCountNode = 0;
					}
				} else if(preloadRelationAndWaysIntoDB || createWholeOsmDB) {
					if (e instanceof Way) {
						for(Long i : ((Way)e).getNodeIds()){
							currentWaysCount ++;
							prepWays.setLong(1, e.getId());
							prepWays.setLong(2, i);
							prepWays.addBatch();
						}
						if (currentWaysCount >= BATCH_SIZE) {
							prepWays.executeBatch();
							currentWaysCount = 0;
						}
					} else {
						for(Entry<EntityId,String> i : ((Relation)e).getMembersMap().entrySet()){
							currentRelationsCount ++;
							prepRelations.setLong(1, e.getId());
							prepRelations.setLong(2, i.getKey().getId());
							prepRelations.setLong(3, i.getKey().getType().ordinal());
							prepRelations.setString(4, i.getValue());
							prepRelations.addBatch();
						}
						if (currentRelationsCount >= BATCH_SIZE) {
							prepRelations.executeBatch();
							currentRelationsCount = 0;
						}
					}
				}
				if(createWholeOsmDB){
					for(Entry<String,String> i : e.getTags().entrySet()){
						currentTagsCount ++;
						prepTags.setLong(1, e.getId());
						prepTags.setLong(2, EntityType.valueOf(e).ordinal());
						prepTags.setString(3, i.getKey());
						prepTags.setString(4, i.getValue());
						prepTags.addBatch();
					}
					if (currentTagsCount >= BATCH_SIZE) {
						prepTags.executeBatch();
						currentTagsCount = 0;
					}
				}
			} catch (SQLException ex) {
				log.error("Could not save in db", ex);
			}
			return processed || loadAllObjects;
		}

	}

	
	public Region readCountry(String path, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException, SQLException{
		File f = new File(path);
		InputStream stream = new FileInputStream(f);
		int i = f.getName().indexOf('.');
		String regionName = Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i)); 
		
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

        DataExtractionOsmFilter filter = new DataExtractionOsmFilter(regionName);
		// data to load & index
		final ArrayList<Node> places = filter.getPlaces();
		final Map<EntityId, Entity> buildings = filter.getBuildings();
		final Map<EntityId, Way> ways = filter.getWays();
		final ArrayList<Entity> amenities = filter.getAmenities();
		final ArrayList<Relation> transport = filter.getTransport();
		final Map<EntityId, Relation> addressRelations = filter.getAddressRelations();
		final Map<EntityId, String> postalCodes = filter.getPostalCodes();
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
        
        country.setName(regionName);
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
        LinkedHashMap<EntityId, City> registeredCities = new LinkedHashMap<EntityId, City>();
        readingCities(places, country, registeredCities);

        if (indexAddress) {
        	progress.setGeneralProgress("[80 of 100]");
        	// 4.1 Reading address relations & remove read streets/buildings
        	readingAddresses(progress, addressRelations, registeredCities, ways, buildings, postalCodes, country);
        	
			// 4. Reading streets
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


	private void readingBuildings(IProgress progress, final Map<EntityId, Entity> buildings, Region country, Map<EntityId, String> postalCodes) {
		// found buildings (index addresses)
        progress.startTask("Indexing buildings...", buildings.size());
        for(Entity b : buildings.values()){
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
					n.putTag(OSMTagKey.NAME.getValue(), "Unknown city");
					country.registerCity(n);
					city = country.getClosestCity(center);
				}
				if (city != null) {
					Building building = city.registerBuilding(b);
					if (building != null) {
						EntityId i = building.getEntityId();
						if (postalCodes.containsKey(i)) {
							building.setPostcode(postalCodes.get(i));
						}
					}
				}
			}
        }
        progress.finishTask();
	}
	
	private void readingAddresses(IProgress progress, Map<EntityId, Relation> addressRelations, Map<EntityId, City> cities,
			Map<EntityId, Way> ways,  Map<EntityId, Entity> buildings,
			Map<EntityId, String> postalCodes, Region country) {
		progress.startTask("Indexing addresses...", addressRelations.size());
		for(Relation i : addressRelations.values()){
			progress.progress(1);
			String type = i.getTag(OSMTagKey.ADDRESS_TYPE);
			
			boolean house = "house".equals(type);
			boolean street = "a6".equals(type);
			if(house || street){
				// try to find appropriate city/street
				City c = null;
				
				Collection<Entity> members = i.getMembers("is_in");
				Relation a3 = null;
				Relation a6 = null;
				if(!members.isEmpty()){
					if(street){
						a6 = i;
					}
					Entity in = members.iterator().next();
					if(in instanceof Relation){
						// go one level up for house
						if(house){
							a6 = (Relation) in;
							members = ((Relation)in).getMembers("is_in");
							if(!members.isEmpty()){
								in = members.iterator().next();
								if(in instanceof Relation){
									a3 = (Relation) in;
								}
							}
							
						} else {
							a3 = (Relation) in;
						}
					}
				}
				
				if(a3 != null){
					Collection<EntityId> memberIds = a3.getMemberIds("label");
					if(!memberIds.isEmpty()){
						c = cities.get(memberIds.iterator().next());
					}
				}
				if(c != null && a6 != null){
					String name = a6.getTag(OSMTagKey.NAME);
					
					if(name != null){
						Street s = c.registerStreet(name);
						if(street){
						for (Map.Entry<Entity, String> r : i.getMemberEntities().entrySet()) {
								if ("street".equals(r.getValue())) {
									if (r.getKey() instanceof Way) {
										s.getWayNodes().add((Way) r.getKey());
										ways.remove(EntityId.valueOf(r.getKey()));
									}
								} else if ("house".equals(r.getValue())) {
									// will be registered further in other case
									if (!(r.getKey() instanceof Relation)) {
										EntityId id = EntityId.valueOf(r.getKey());
										Building b = s.registerBuilding(r.getKey());
										buildings.remove(id);
										if (b != null) {
											if (postalCodes.containsKey(id)) {
												b.setPostcode(postalCodes.get(id));
											}
										}
									}
								}
							}
						} else {
							String hno = i.getTag(OSMTagKey.ADDRESS_HOUSE);
							if(hno == null){
								hno = i.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
							}
							if(hno == null){
								hno = i.getTag(OSMTagKey.NAME);
							}
							members = i.getMembers("border");
							if(!members.isEmpty()){
								Entity border = members.iterator().next();
								if (border != null) {
									EntityId id = EntityId.valueOf(border);
									// special check that address do not contain twice in a3 - border and separate a6
									if (!a6.getMemberIds().contains(id)) {
										Building b = s.registerBuilding(border, hno);
										if (b != null && postalCodes.containsKey(id)) {
											b.setPostcode(postalCodes.get(id));
										}
										buildings.remove(id);
									}
								}
							} else {
								log.info("For relation " + i.getId() + " border not found");
							}
								
							
						}
						
						
					} 
				}
			}
			
		}
        progress.finishTask();
	}



	private void readingStreets(IProgress progress, final Map<EntityId, Way> ways, Region country) {
		progress.startTask("Indexing streets...", ways.size());
        DataTileManager<Way> waysManager = new DataTileManager<Way>();
        for (Way w : ways.values()) {
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

	private static Set<String> acceptedRoutes = new HashSet<String>();
	static {
		acceptedRoutes.add("bus");
		acceptedRoutes.add("trolleybus");
		acceptedRoutes.add("share_taxi");
		
		acceptedRoutes.add("subway");
		acceptedRoutes.add("train");
		
		acceptedRoutes.add("tram");
		
		acceptedRoutes.add("ferry");
	}
	
	public void readingTransport(final ArrayList<Relation> transport, Region country, IProgress progress){
		progress.startTask("Reading transport...", -1);
		Map<String, List<TransportRoute>> routes = country.getTransportRoutes();
		Map<Long, TransportStop> routeStops = new LinkedHashMap<Long, TransportStop>();
		for(Relation rel : transport){
			String ref = rel.getTag(OSMTagKey.REF);
			String route = rel.getTag(OSMTagKey.ROUTE);
			String operator = rel.getTag(OSMTagKey.OPERATOR);
			if(route == null || ref == null){
				continue;
			}
			if(!acceptedRoutes.contains(route)){
				continue;
			}
			TransportRoute r = new TransportRoute(rel, ref);
			r.setOperator(operator);
			r.setType(route);
			
			
			if(operator != null){
				route = operator + " : " + route;
			} 
			if(!routes.containsKey(route)){
				routes.put(route, new ArrayList<TransportRoute>());
			}
			
			
			final Map<TransportStop, Integer> forwardStops = new LinkedHashMap<TransportStop, Integer>();
			final Map<TransportStop, Integer> backwardStops = new LinkedHashMap<TransportStop, Integer>();
			int currentStop = 0;
			int forwardStop = 0;
			int backwardStop = 0;
			for (Entry<Entity, String> e : rel.getMemberEntities().entrySet()) {
				if(e.getValue().contains("stop")){
					if(e.getKey() instanceof Node){
						if(!routeStops.containsKey(e.getKey().getId())){
							routeStops.put(e.getKey().getId(), new TransportStop(e.getKey()));
						}
						TransportStop stop = routeStops.get(e.getKey().getId());
						boolean forward = e.getValue().contains("forward");
						boolean backward = e.getValue().contains("backward");
						currentStop++;
						if(forward || !backward){
							forwardStop ++;
						}
						if(backward){
							backwardStop ++;
						}
						boolean common = !forward && !backward;
						int index = -1;
						int i = e.getValue().length() -1;
						int accum = 1;
						while(i >= 0 && Character.isDigit(e.getValue().charAt(i))){
							if(index < 0){
								index = 0;
							}
							index = accum * Character.getNumericValue(e.getValue().charAt(i)) + index; 
							accum *= 10;
							i --;
						}
						if(index < 0){
							index = forward ? forwardStop : (backward ? backwardStop : currentStop) ;
						}
						if(forward || common){
							forwardStops.put(stop, index);
							r.getForwardStops().add(stop);
						}
						if(backward || common){
							if(common){
								// put with negative index
								backwardStops.put(stop, -index);
							} else {
								backwardStops.put(stop, index);
							}
							
							r.getBackwardStops().add(stop);
						}
						
					}
					
				} else if(e.getKey() instanceof Way){
					r.addWay((Way) e.getKey());
				}
			}
			if(forwardStops.isEmpty() && backwardStops.isEmpty()){
				continue;
			}
			Collections.sort(r.getForwardStops(), new Comparator<TransportStop>(){
				@Override
				public int compare(TransportStop o1, TransportStop o2) {
					return forwardStops.get(o1) - forwardStops.get(o2);
				}
			});
			// all common stops are with negative index (reeval them)
			for(TransportStop s : new ArrayList<TransportStop>(backwardStops.keySet())){
				if(backwardStops.get(s) < 0){
					backwardStops.put(s, backwardStops.size() + backwardStops.get(s) -1);
				}
			}
			Collections.sort(r.getBackwardStops(), new Comparator<TransportStop>(){
				@Override
				public int compare(TransportStop o1, TransportStop o2) {
					return backwardStops.get(o1) - backwardStops.get(o2);
				}
			});
			routes.get(route).add(r);
			
			// validate that all is ok
//			if (validateTransportStops(r.getBackwardStops(), r) && validateTransportStops(r.getForwardStops(), r)) {
//				System.out.println("Route " + r + " is valid ");
//			}
			
		}
		
		progress.finishTask();
	}
	
	protected boolean validateTransportStops(List<TransportStop> stops, TransportRoute r){
		boolean valid = true;
		for (int i = 2; i < stops.size(); i++) {
			TransportStop s1 = stops.get(i - 2);
			TransportStop s2 = stops.get(i - 1);
			TransportStop s3 = stops.get(i);
			if (MapUtils.getDistance(s1.getLocation(), s2.getLocation())  > MapUtils.getDistance(s1.getLocation(), s3.getLocation()) * 1.3) {
				System.out.println("SOMETHING WRONG with " + i + "th of  " + r.getRef() +" "+ r );
				valid = false;
			}
		}
		return valid;
	}
	
	private void readingAmenities(final ArrayList<Entity> amenities, Region country) {
		for(Entity a: amenities){
        	country.registerAmenity(new Amenity(a));
        }
	}

	
	public void readingCities(ArrayList<Node> places, Region country, Map<EntityId, City> citiesMap) {
		for (Node s : places) {
			String place = s.getTag(OSMTagKey.PLACE);
			if (place == null) {
				continue;
			}
			City city = country.registerCity(s);
			if(city != null){
				citiesMap.put(city.getEntityId(), city);
			}
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
			public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity) {
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
