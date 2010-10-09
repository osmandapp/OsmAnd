package net.osmand.data.preparation;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.DataTileManager;
import net.osmand.data.MapObject;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.data.City.CityType;
import net.osmand.data.index.DataIndexWriter;
import net.osmand.data.index.IndexConstants;
import net.osmand.data.index.IndexConstants.IndexBuildingTable;
import net.osmand.data.index.IndexConstants.IndexCityTable;
import net.osmand.data.index.IndexConstants.IndexStreetNodeTable;
import net.osmand.data.index.IndexConstants.IndexStreetTable;
import net.osmand.data.index.IndexConstants.IndexTransportRoute;
import net.osmand.data.index.IndexConstants.IndexTransportRouteStop;
import net.osmand.data.index.IndexConstants.IndexTransportStop;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.io.IOsmStorageFilter;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.swing.DataExtractionSettings;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;


/**
 * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Is_inside.2Foutside
 * http://wiki.openstreetmap.org/wiki/Relations/Proposed/Postal_Addresses
 * http://wiki.openstreetmap.org/wiki/Proposed_features/House_numbers/Karlsruhe_Schema#Tags (node, way)

 * That data extraction has aim, 
 * save runtime memory and generate indexes on the fly.
 * It will be longer than load in memory (needed part) and save into index.
 */
public class IndexCreator {
	private static final Log log = LogFactory.getLog(IndexCreator.class);


	public static final int BATCH_SIZE = 10;
	public static final String TEMP_NODES_DB = "nodes.tmp.odb";
	
	public static final int STEP_CITY_NODES = 1;
	public static final int STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS = 2;
	public static final int STEP_MAIN = 3;
	
	private File workingDir = null;
	
	private boolean indexMap;
	private boolean indexPOI;
	private boolean indexTransport;
	
	private boolean indexAddress;
	private boolean normalizeStreets;
	private boolean saveAddressWays;

	private String regionName;
	
	private String transportFileName = null;
	private String poiFileName = null;
	private String addressFileName = null;
	private String mapFileName = null;
	private Long lastModifiedDate = null;
	
	
	private PreparedStatement pselectNode;
	private PreparedStatement pselectWay;
	private PreparedStatement pselectRelation;
	private PreparedStatement pselectTags;

	private Connection dbConn;
	private File dbFile;
	
	Map<PreparedStatement, Integer> pStatements = new LinkedHashMap<PreparedStatement, Integer>();
	
	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;
	
	
	Set<Long> visitedStops = new HashSet<Long>();
	private File transportIndexFile;
	private Connection transportConnection;
	private PreparedStatement transRouteStat;
	private PreparedStatement transRouteStopsStat;
	private PreparedStatement transStopsStat;

	private File addressIndexFile;
	private Connection addressConnection;
	private PreparedStatement addressCityStat;
	private PreparedStatement addressStreetStat;
	private PreparedStatement addressBuildingStat;
	private PreparedStatement addressStreetNodeStat;
	
	private File mapFile;
	private Connection mapConnection;
	private PreparedStatement mapObjStat;
	private PreparedStatement mapLocsStatLevel0;
	private PreparedStatement mapLocsStatLevel1;
	private PreparedStatement mapLocsStatLevel2;
//	private RTree mapTree;
	
	// MEMORY map :  save it in memory while that is allowed
	private Map<Long, Set<Integer>> multiPolygonsWays0 = new LinkedHashMap<Long, Set<Integer>>();
	private Map<Long, Set<Integer>> multiPolygonsWays1 = new LinkedHashMap<Long, Set<Integer>>();
	private Map<Long, Set<Integer>> multiPolygonsWays2 = new LinkedHashMap<Long, Set<Integer>>();
	private Map<Long, String> multiPolygonsNames = new LinkedHashMap<Long, String>();
	private Map<Long, List<Long>> highwayRestrictions = new LinkedHashMap<Long, List<Long>>();
	private Map<Long, List<Way>> lowLevelWaysSt = new LinkedHashMap<Long, List<Way>>();
	private Map<Long, List<Way>> lowLevelWaysEnd = new LinkedHashMap<Long, List<Way>>();
	
	
	// MEMORY address : choose what to use ?
	private boolean loadInMemory = true;
	private PreparedStatement addressSearchStreetStat;
	private PreparedStatement addressSearchBuildingStat;
	private PreparedStatement addressSearchStreetNodeStat;
	
	private Map<String, Long> addressStreetLocalMap = new LinkedHashMap<String, Long>();
	private Set<Long> addressBuildingLocalSet = new LinkedHashSet<Long>();
	private Set<Long> addressStreetNodeLocalSet = new LinkedHashSet<Long>();
	
	
	// MEMORY address : address structure
	// load it in memory
	private Map<EntityId, City> cities = new LinkedHashMap<EntityId, City>();
	private DataTileManager<City> cityVillageManager = new DataTileManager<City>(13);
	private DataTileManager<City> cityManager = new DataTileManager<City>(10);
	private List<Relation> postalCodeRelations = new ArrayList<Relation>(); 

	private String[] normalizeDefaultSuffixes;
	private String[] normalizeSuffixes;
	
	// local purpose
	List<Integer> typeUse= new ArrayList<Integer>(8);
	List<Long> restrictionsUse= new ArrayList<Long>(8);
	
	public IndexCreator(File workingDir){
		this.workingDir = workingDir;
	}
	
	
	public void setIndexAddress(boolean indexAddress) {
		this.indexAddress = indexAddress;
	}
	
	public void setIndexMap(boolean indexMap) {
		this.indexMap = indexMap;
	}
	
	public void setIndexPOI(boolean indexPOI) {
		this.indexPOI = indexPOI;
	}
	
	public void setIndexTransport(boolean indexTransport) {
		this.indexTransport = indexTransport;
	}
	
	public void setSaveAddressWays(boolean saveAddressWays) {
		this.saveAddressWays = saveAddressWays;
	}
	
	public void setNormalizeStreets(boolean normalizeStreets) {
		this.normalizeStreets = normalizeStreets;
	}

	
	protected class NewDataExtractionOsmFilter implements IOsmStorageFilter {

		int currentCountNode = 0;
		private PreparedStatement prepNode;
		int allNodes = 0;
		
		int currentRelationsCount = 0;
		private PreparedStatement prepRelations;
		int allRelations = 0;
		
		int currentWaysCount = 0;
		private PreparedStatement prepWays;
		int allWays = 0;
		
		int currentTagsCount = 0;
		private PreparedStatement prepTags;
		

		
		public void initDatabase() throws SQLException {
			// prepare tables
			Statement stat = dbConn.createStatement();
			stat.executeUpdate("drop table if exists node;");
			stat.executeUpdate("create table node (id long, latitude double, longitude double);");
			stat.executeUpdate("create index IdIndex ON node (id, latitude, longitude);");
			stat.executeUpdate("drop table if exists ways;");
			stat.executeUpdate("create table ways (id long, node long, ord smallint);");
			stat.executeUpdate("create index IdWIndex ON ways (id, node);");
			stat.executeUpdate("drop table if exists relations;");
			stat.executeUpdate("create table relations (id long, member long, type byte, role text, ord smallint);");
			stat.executeUpdate("create index IdRIndex ON relations (id, member, type);");
			stat.executeUpdate("drop table if exists tags;");
			stat.executeUpdate("create table tags (id long, type byte, key, value);");
			stat.executeUpdate("create index IdTIndex ON tags (id, type);");
			stat.execute("PRAGMA user_version = " + IndexConstants.MAP_TABLE_VERSION); //$NON-NLS-1$
			stat.close();

			prepNode = dbConn.prepareStatement("insert into node values (?, ?, ?);");
			prepWays = dbConn.prepareStatement("insert into ways values (?, ?, ?);");
			prepRelations = dbConn.prepareStatement("insert into relations values (?, ?, ?, ?, ?);");
			prepTags = dbConn.prepareStatement("insert into tags values (?, ?, ?, ?);");
			dbConn.setAutoCommit(false);
		}
		
		public void finishLoading() throws SQLException{
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
			dbConn.setAutoCommit(true);
		}

		@Override
		public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity e) {
			// Register all city labelbs
			registerCityIfNeeded(e);
			// put all nodes into temporary db to get only required nodes after loading all data 
			try {
				if (e instanceof Node) {
					currentCountNode++;
					allNodes++;
					prepNode.setLong(1, e.getId());
					prepNode.setDouble(2, ((Node) e).getLatitude());
					prepNode.setDouble(3, ((Node) e).getLongitude());
					prepNode.addBatch();
					if (currentCountNode >= BATCH_SIZE) {
						prepNode.executeBatch();
						currentCountNode = 0;
					}
				} else if (e instanceof Way) {
					allWays++;
					short ord = 0;
					for (Long i : ((Way) e).getNodeIds()) {
						currentWaysCount++;
						prepWays.setLong(1, e.getId());
						prepWays.setLong(2, i);
						prepWays.setLong(3, ord++);
						prepWays.addBatch();
					}
					if (currentWaysCount >= BATCH_SIZE) {
						prepWays.executeBatch();
						currentWaysCount = 0;
					}
				} else {
					allRelations++;
					short ord = 0;
					for (Entry<EntityId, String> i : ((Relation) e).getMembersMap().entrySet()) {
						currentRelationsCount++;
						prepRelations.setLong(1, e.getId());
						prepRelations.setLong(2, i.getKey().getId());
						prepRelations.setLong(3, i.getKey().getType().ordinal());
						prepRelations.setString(4, i.getValue());
						prepWays.setLong(5, ord++);
						prepRelations.addBatch();
					}
					if (currentRelationsCount >= BATCH_SIZE) {
						prepRelations.executeBatch();
						currentRelationsCount = 0;
					}
				}
				for (Entry<String, String> i : e.getTags().entrySet()) {
					currentTagsCount++;
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
			} catch (SQLException ex) {
				log.error("Could not save in db", ex);
			}
			// do not add to storage
			return false;
		}
		
		public int getAllNodes() {
			return allNodes;
		}
		
		public int getAllRelations() {
			return allRelations;
		}
		
		public int getAllWays() {
			return allWays;
		}
	}
	
	
	public String getRegionName() {
		if(regionName == null){
			return "Region";
		}
		return regionName;
	}
	
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}
	
	
	
	public void loadEntityData(Entity e, boolean loadTags) throws SQLException {
		if(e instanceof Node){
			return;
		}
		Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
		if(e instanceof Relation && ((Relation) e).getMemberIds().isEmpty()){
			pselectRelation.setLong(1, e.getId());
			if (pselectRelation.execute()) {
				ResultSet rs = pselectRelation.getResultSet();
				while (rs.next()) {
					((Relation) e).addMember(rs.getLong(2), EntityType.values()[rs.getByte(3)], rs.getString(4));
				}
				rs.close();
			}
		} else if(e instanceof Way && ((Way) e).getEntityIds().isEmpty()) {
			pselectWay.setLong(1, e.getId());
			if (pselectWay.execute()) {
				ResultSet rs = pselectWay.getResultSet();
				while (rs.next()) {
					((Way) e).addNode(rs.getLong(2));
				}
				rs.close();
			}
		}
		Collection<EntityId> ids = e instanceof Relation? ((Relation)e).getMemberIds() : ((Way)e).getEntityIds();
		for (EntityId i : ids) {
			if (i.getType() == EntityType.NODE) {
				pselectNode.setLong(1, i.getId());
				if (pselectNode.execute()) {
					ResultSet rs = pselectNode.getResultSet();
					if (rs.next()) {
						map.put(i, new Node(rs.getDouble(2), rs.getDouble(3), rs.getLong(1)));
					}
					rs.close();
				}
			} else if (i.getType() == EntityType.WAY) {
				pselectWay.setLong(1, i.getId());
				if (pselectWay.execute()) {
					ResultSet rs = pselectWay.getResultSet();
					Way way = new Way(i.getId());
					map.put(i, way);
					while (rs.next()) {
						way.addNode(rs.getLong(2));
					}
					rs.close();
					// load way nodes
					loadEntityData(way, loadTags);
				}
			} else if (i.getType() == EntityType.RELATION) {
				pselectRelation.setLong(1, i.getId());
				if (pselectRelation.execute()) {
					ResultSet rs = pselectNode.getResultSet();
					Relation rel = new Relation(i.getId());
					map.put(i, rel);
					while (rs.next()) {
						rel.addMember(rs.getLong(1), EntityType.values()[rs.getByte(2)], rs.getString(3));
					}
					// do not load relation members recursively ? It is not needed for transport, address, poi before
					rs.close();
				}
			}
		}
		if(loadTags && e.getTagKeySet().isEmpty()){
			for(Map.Entry<EntityId, Entity> es : map.entrySet()){
				loadEntityTags(es.getKey().getType(), es.getValue());
			}
		}
		e.initializeLinks(map);
	}
	
	public void setAddressFileName(String addressFileName) {
		this.addressFileName = addressFileName;
	}
	
	public void setPoiFileName(String poiFileName) {
		this.poiFileName = poiFileName;
	}
	
	public void setTransportFileName(String transportFileName) {
		this.transportFileName = transportFileName;
	}
	
	public void setNodesDBFile(File file){
		dbFile = file;
	}
	
	public void setMapFileName(String mapFileName) {
		this.mapFileName = mapFileName;
	}
	public String getMapFileName() {
		if(mapFileName == null){
			return getRegionName() + IndexConstants.MAP_INDEX_EXT;
		}
		return mapFileName;
	}
	
	public String getTransportFileName() {
		if(transportFileName == null){
			return IndexConstants.TRANSPORT_INDEX_DIR + getRegionName() + IndexConstants.TRANSPORT_INDEX_EXT;
		}
		return transportFileName;
	}
	
	public Long getLastModifiedDate() {
		return lastModifiedDate;
	}
	
	public void setLastModifiedDate(Long lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
	
	public String getPoiFileName() {
		if(poiFileName == null){
			return IndexConstants.POI_INDEX_DIR + getRegionName() + IndexConstants.POI_INDEX_EXT;
		}
		return poiFileName;
	}
	
	public String getAddressFileName() {
		if(addressFileName == null){
			return IndexConstants.ADDRESS_INDEX_DIR + getRegionName() + IndexConstants.ADDRESS_INDEX_EXT;
		}
		return addressFileName;
	}
	
	public void iterateOverAllEntities(IProgress progress, int allNodes, int allWays, int allRelations, int step) throws SQLException{
		progress.setGeneralProgress("[65 of 100]");
		iterateOverEntities(progress, EntityType.NODE, allNodes, step);
		progress.setGeneralProgress("[85 of 100]");
		iterateOverEntities(progress, EntityType.WAY, allWays, step);
		progress.setGeneralProgress("[90 of 100]");
		iterateOverEntities(progress, EntityType.RELATION, allRelations, step);
	}
	
	public int iterateOverEntities(IProgress progress, EntityType type, int allCount, int step) throws SQLException{
		Statement statement = dbConn.createStatement();
		String select;
		String info;
		int count = 0;
		
		if(type == EntityType.NODE){
			select = "select * from node";
			info = "nodes";
		} else if(type == EntityType.WAY){
			select = "select distinct id from ways";
			info = "ways";
		} else {
			select = "select distinct id from relations";
			info = "relations";
		}
		progress.startTask("Indexing "+info +"...", allCount);
		ResultSet rs = statement.executeQuery(select);
		while(rs.next()){
			count++;
			progress.progress(1);
			Entity e;
			if(type == EntityType.NODE){
				e = new Node(rs.getDouble(2),rs.getDouble(3),rs.getLong(1));
			} else if(type == EntityType.WAY){
				e = new Way(rs.getLong(1));
			} else {
				e = new Relation(rs.getLong(1));
			}
			loadEntityTags(type, e);
			iterateEntity(e, step);
		}
		rs.close();
		return count;
	}

	private void loadEntityTags(EntityType type, Entity e) throws SQLException {
		pselectTags.setLong(1, e.getId());
		pselectTags.setByte(2, (byte) type.ordinal());
		ResultSet rsTags = pselectTags.executeQuery();
		while(rsTags.next()){
			e.putTag(rsTags.getString(1), rsTags.getString(2));
		}
		rsTags.close();
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
	
	private TransportRoute indexTransportRoute(Relation rel) {
		String ref = rel.getTag(OSMTagKey.REF);
		String route = rel.getTag(OSMTagKey.ROUTE);
		String operator = rel.getTag(OSMTagKey.OPERATOR);
		if (route == null || ref == null) {
			return null;
		}
		if (!acceptedRoutes.contains(route)) {
			return null;
		}
		TransportRoute r = new TransportRoute(rel, ref);
		convertEnglishName(r);
		r.setOperator(operator);
		r.setType(route);
		

		if (operator != null) {
			route = operator + " : " + route;
		}

		final Map<TransportStop, Integer> forwardStops = new LinkedHashMap<TransportStop, Integer>();
		final Map<TransportStop, Integer> backwardStops = new LinkedHashMap<TransportStop, Integer>();
		int currentStop = 0;
		int forwardStop = 0;
		int backwardStop = 0;
		for (Entry<Entity, String> e : rel.getMemberEntities().entrySet()) {
			if (e.getValue().contains("stop")) {
				if (e.getKey() instanceof Node) {
					TransportStop stop = new TransportStop(e.getKey());
					convertEnglishName(stop);
					boolean forward = e.getValue().contains("forward");
					boolean backward = e.getValue().contains("backward");
					currentStop++;
					if (forward || !backward) {
						forwardStop++;
					}
					if (backward) {
						backwardStop++;
					}
					boolean common = !forward && !backward;
					int index = -1;
					int i = e.getValue().length() - 1;
					int accum = 1;
					while (i >= 0 && Character.isDigit(e.getValue().charAt(i))) {
						if (index < 0) {
							index = 0;
						}
						index = accum * Character.getNumericValue(e.getValue().charAt(i)) + index;
						accum *= 10;
						i--;
					}
					if (index < 0) {
						index = forward ? forwardStop : (backward ? backwardStop : currentStop);
					}
					if (forward || common) {
						forwardStops.put(stop, index);
						r.getForwardStops().add(stop);
					}
					if (backward || common) {
						if (common) {
							// put with negative index
							backwardStops.put(stop, -index);
						} else {
							backwardStops.put(stop, index);
						}

						r.getBackwardStops().add(stop);
					}

				}

			} else if (e.getKey() instanceof Way) {
				r.addWay((Way) e.getKey());
			}
		}
		if (forwardStops.isEmpty() && backwardStops.isEmpty()) {
			return null;
		}
		Collections.sort(r.getForwardStops(), new Comparator<TransportStop>() {
			@Override
			public int compare(TransportStop o1, TransportStop o2) {
				return forwardStops.get(o1) - forwardStops.get(o2);
			}
		});
		// all common stops are with negative index (reeval them)
		for (TransportStop s : new ArrayList<TransportStop>(backwardStops.keySet())) {
			if (backwardStops.get(s) < 0) {
				backwardStops.put(s, backwardStops.size() + backwardStops.get(s) - 1);
			}
		}
		Collections.sort(r.getBackwardStops(), new Comparator<TransportStop>() {
			@Override
			public int compare(TransportStop o1, TransportStop o2) {
				return backwardStops.get(o1) - backwardStops.get(o2);
			}
		});
		
		return r;

	}


	public void indexAddressRelation(Relation i) throws SQLException {
		String type = i.getTag(OSMTagKey.ADDRESS_TYPE);
		boolean house = "house".equals(type);
		boolean street = "a6".equals(type);
		if (house || street) {
			// try to find appropriate city/street
			City c = null;
			// load with member ways with their nodes and tags !
			loadEntityData(i, true);


			Collection<Entity> members = i.getMembers("is_in");
			Relation a3 = null;
			Relation a6 = null;
			if (!members.isEmpty()) {
				if (street) {
					a6 = i;
				}
				Entity in = members.iterator().next();
				loadEntityData(in, true);
				if (in instanceof Relation) {
					// go one level up for house
					if (house) {
						a6 = (Relation) in;
						members = ((Relation) in).getMembers("is_in");
						if (!members.isEmpty()) {
							in = members.iterator().next();
							loadEntityData(in, true);
							if (in instanceof Relation) {
								a3 = (Relation) in;
							}
						}

					} else {
						a3 = (Relation) in;
					}
				}
			}

			if (a3 != null) {
				Collection<EntityId> memberIds = a3.getMemberIds("label");
				if (!memberIds.isEmpty()) {
					c = cities.get(memberIds.iterator().next());
				}
			}
			if (c != null && a6 != null) {
				String name = a6.getTag(OSMTagKey.NAME);

				if (name != null) {
					LatLon location = c.getLocation();
					for(Entity e : i.getMembers(null)){
						if(e instanceof Way){
							LatLon l= ((Way) e).getLatLon();
							if(l != null ){
								location = l;
								break;
							}
						}
					}
					
					Long streetId = getStreetInCity(c, name, location, a6.getId());
					if(streetId == null){
						return;
					}
					if (street) {
						for (Map.Entry<Entity, String> r : i.getMemberEntities().entrySet()) {
							if ("street".equals(r.getValue())) {
								if (r.getKey() instanceof Way && saveAddressWays) {
									DataIndexWriter.writeStreetWayNodes(addressStreetNodeStat, 
											pStatements, streetId, (Way) r.getKey(), BATCH_SIZE);
									if(loadInMemory){
										addressStreetNodeLocalSet.add(r.getKey().getId());
									}
								}
							} else if ("house".equals(r.getValue())) {
								// will be registered further in other case
								if (!(r.getKey() instanceof Relation)) {
									String hno = r.getKey().getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
									if(hno != null){
										Building building = new Building(r.getKey());
										building.setName(hno);
										convertEnglishName(building);
										DataIndexWriter.writeBuilding(addressBuildingStat, pStatements, 
												streetId, building, BATCH_SIZE);
										if(loadInMemory){
											addressBuildingLocalSet.add(r.getKey().getId());
										}
									}
								}
							}
						}
					} else {
						String hno = i.getTag(OSMTagKey.ADDRESS_HOUSE);
						if (hno == null) {
							hno = i.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
						}
						if (hno == null) {
							hno = i.getTag(OSMTagKey.NAME);
						}
						members = i.getMembers("border");
						if (!members.isEmpty()) {
							Entity border = members.iterator().next();
							if (border != null) {
								EntityId id = EntityId.valueOf(border);
								// special check that address do not contain twice in a3 - border and separate a6
								if (!a6.getMemberIds().contains(id)) {
									Building building = new Building(border);
									building.setName(hno);
									convertEnglishName(building);
									DataIndexWriter.writeBuilding(addressBuildingStat, pStatements, 
											streetId, building, BATCH_SIZE);
									if(loadInMemory){
										addressBuildingLocalSet.add(id.getId());
									}
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
	
	public City getClosestCity(LatLon point) {
		if(point == null){
			return null;
		}
		City closest = null;
		double relDist = Double.POSITIVE_INFINITY;
		for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			double rel = MapUtils.getDistance(c.getLocation(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if(relDist < 0.2d){
					break;
				}
			}
		}
		if(relDist < 0.2d){
			return closest;
		}
		for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			double rel = MapUtils.getDistance(c.getLocation(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if(relDist < 0.2d){
					break;
				}
			}
		}
		return closest;
	}
	
	
	public String normalizeStreetName(String name){
		name = name.trim();
		if (normalizeStreets) {
			String newName = name;
			boolean processed = newName.length() != name.length();
			for (String ch : normalizeDefaultSuffixes) {
				int ind = checkSuffix(newName, ch);
				if (ind != -1) {
					newName = cutSuffix(newName, ind, ch.length());
					processed = true;
					break;
				}
			}

			if (!processed) {
				for (String ch : normalizeSuffixes) {
					int ind = checkSuffix(newName, ch);
					if (ind != -1) {
						newName = putSuffixToEnd(newName, ind, ch.length());
						processed = true;
						break;
					}
				}
			}
			if (processed) {
				return newName;
			}
		}
		return name;
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
	
	
	public Long getStreetInCity(City city, String name, LatLon location, long initId) throws SQLException{
		if(name == null || city == null){
			return null;
		}
		Long foundId = null;
		
		name = normalizeStreetName(name);
		if(loadInMemory){
			foundId = addressStreetLocalMap.get(name+"_"+city.getId());
		} else {
			addressSearchStreetStat.setLong(1, city.getId());
			addressSearchStreetStat.setString(2, name);
			ResultSet rs = addressSearchStreetStat.executeQuery();
			if (rs.next()) {
				foundId = rs.getLong(1);
			}
			rs.close();
		}
		
		if (foundId == null) {
			assert IndexStreetTable.values().length == 6;
			addressStreetStat.setLong(IndexStreetTable.ID.ordinal() + 1, initId);
			addressStreetStat.setString(IndexStreetTable.NAME_EN.ordinal() + 1, Junidecode.unidecode(name));
			addressStreetStat.setString(IndexStreetTable.NAME.ordinal() + 1, name);
			addressStreetStat.setDouble(IndexStreetTable.LATITUDE.ordinal() + 1, location.getLatitude());
			addressStreetStat.setDouble(IndexStreetTable.LONGITUDE.ordinal() + 1, location.getLongitude());
			addressStreetStat.setLong(IndexStreetTable.CITY.ordinal() + 1, city.getId());
			if(loadInMemory){
				DataIndexWriter.addBatch(pStatements, addressStreetStat, BATCH_SIZE);
				addressStreetLocalMap.put(name+"_"+city.getId(), initId);
			} else {
				addressStreetStat.execute();
				// commit immediately to search after
				addressConnection.commit();
			}
			foundId = initId;
		}
		return foundId;
	}
	
	private void iterateEntity(Entity e, int step) throws SQLException {
		if (step == STEP_MAIN) {
			if (indexPOI && Amenity.isAmenity(e)) {
				loadEntityData(e, false);
				if (poiPreparedStatement != null) {
					Amenity a = new Amenity(e);
					if (a.getLocation() != null) {
						convertEnglishName(a);
						DataIndexWriter.insertAmenityIntoPoi(poiPreparedStatement, pStatements, a, BATCH_SIZE);
					}
				}
			}
			if (indexTransport) {
				if (e instanceof Relation && e.getTag(OSMTagKey.ROUTE) != null) {
					loadEntityData(e, true);
					TransportRoute route = indexTransportRoute((Relation) e);
					if (route != null) {
						DataIndexWriter.insertTransportIntoIndex(transRouteStat, transRouteStopsStat, transStopsStat, visitedStops, route,
								pStatements, BATCH_SIZE);
					}
				}
			}
			
			if (indexMap && (e instanceof Way || e instanceof Node)) {
				// manipulate what kind of way to load
				loadEntityData(e, true);
				boolean inverse = "-1".equals(e.getTag(OSMTagKey.ONEWAY));
				writeEntityToMapDatabase(e, e.getId(), inverse, 0);
				writeEntityToMapDatabase(e, e.getId(), false, 1);
				writeEntityToMapDatabase(e, e.getId(), false, 2);
//				indexLowLevelMap(e, 1);
//				indexLowLevelMap(e, 2);
			}

			if (indexAddress) {
				// index not only buildings but also nodes that belongs to addr:interpolation ways
				if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
					// TODO e.getTag(OSMTagKey.ADDR_CITY) could be used to find city however many cities could have same name!
					// check that building is not registered already
					boolean exist = false;
					if(loadInMemory){
						exist = addressBuildingLocalSet.contains(e.getId());
					} else {
						addressSearchBuildingStat.setLong(1, e.getId());
						ResultSet rs = addressSearchBuildingStat.executeQuery();
						exist = rs.next();
						rs.close();
						
					}
					if (!exist) {
						loadEntityData(e, false);
						LatLon l = e.getLatLon();
						City city = getClosestCity(l);
						Long idStreet = getStreetInCity(city, e.getTag(OSMTagKey.ADDR_STREET), l, e.getId());
						if (idStreet != null) {
							Building building = new Building(e);
							building.setName(e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER));
							convertEnglishName(building);
							DataIndexWriter.writeBuilding(addressBuildingStat, pStatements, idStreet, building, BATCH_SIZE);
						}
					}
				}
				// suppose that streets with names are ways for car
				if (e instanceof Way /* && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY)) */
						&& e.getTag(OSMTagKey.HIGHWAY) != null && e.getTag(OSMTagKey.NAME) != null) {
					boolean exist = false;
					if(loadInMemory){
						exist = addressStreetNodeLocalSet.contains(e.getId());
					} else {
						addressSearchStreetNodeStat.setLong(1, e.getId());
						ResultSet rs = addressSearchStreetNodeStat.executeQuery();
						exist = rs.next();
						rs.close();
						
					}
					// check that building is not registered already 
					if (!exist) {
						loadEntityData(e, false);
						LatLon l = e.getLatLon();
						City city = getClosestCity(l);
						Long idStreet = getStreetInCity(city, e.getTag(OSMTagKey.NAME), l, e.getId());
						if (idStreet != null && saveAddressWays) {
							DataIndexWriter.writeStreetWayNodes(addressStreetNodeStat, pStatements, idStreet, (Way) e, BATCH_SIZE);
						}
					}
				}
				if (e instanceof Relation) {
					if (e.getTag(OSMTagKey.POSTAL_CODE) != null) {
						loadEntityData(e, false);
						postalCodeRelations.add((Relation) e);
					}
				}
			}
		} else if (step == STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS) {
			if (indexAddress) {
				if (e instanceof Relation && "address".equals(e.getTag(OSMTagKey.TYPE))) {
					indexAddressRelation((Relation) e);
				}
			}
			if(indexMap && e instanceof Relation && "restriction".equals(e.getTag(OSMTagKey.TYPE))){
				String val = e.getTag("restriction");
				if(val != null){
					byte type = -1;
					if("no_right_turn".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN;
					} else if("no_left_turn".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_NO_LEFT_TURN;
					} else if("no_u_turn".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_NO_U_TURN;
					} else if("no_straight_on".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON;
					} else if("only_right_turn".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN;
					} else if("only_left_turn".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_ONLY_LEFT_TURN;
					} else if("only_straight_on".equalsIgnoreCase(val)){
						type = MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON;
					}
					if(type != -1){
						Collection<EntityId> fromL = ((Relation) e).getMemberIds("from");
						Collection<EntityId> toL = ((Relation) e).getMemberIds("to");
						if(!fromL.isEmpty() && !toL.isEmpty()) {
							EntityId from = fromL.iterator().next();
							EntityId to = toL.iterator().next();
							if(from.getType() == EntityType.WAY){
								if(!highwayRestrictions.containsKey(from.getId())){
									highwayRestrictions.put(from.getId(), new ArrayList<Long>(4));
								}
								highwayRestrictions.get(from.getId()).add((to.getId() << 3) | (long) type);
							}
						}
					}
				}
			}
			if (indexMap && e instanceof Relation && "multipolygon".equals(e.getTag(OSMTagKey.TYPE))) {
				loadEntityData(e, true);
				Map<Entity, String> entities = ((Relation) e).getMemberEntities();
				
				for (Entity es : entities.keySet()) {
					if (es instanceof Way) {
						boolean inner = "inner".equals(entities.get(es));
						if (!inner) {
							for (String t : es.getTagKeySet()) {
								e.putTag(t, es.getTag(t));
							}
							break;
						}
					}
				}
				
				int mtType = findMultiPolygonType(e, 0);
				if (mtType != 0) {
					int mtType1 = findMultiPolygonType(e, 1);
					int mtType2 = findMultiPolygonType(e, 2);
					String name = MapRenderingTypes.getEntityName(e, mtType);
					List<List<Way>> completedRings = new ArrayList<List<Way>>();
					List<List<Way>> incompletedRings = new ArrayList<List<Way>>();
					for (Entity es : entities.keySet()) {
						if (es instanceof Way) {
							if(!((Way) es).getNodeIds().isEmpty()){
								combineMultiPolygons((Way) es, completedRings, incompletedRings);
							}
						}
					}
					// skip incompleted rings and do not add whole relation ?
					if(!incompletedRings.isEmpty()){
						return;
//						completedRings.addAll(incompletedRings);
					}
					for(List<Way> l : completedRings){
						boolean innerType = "inner".equals(entities.get(l.get(0)));
						boolean clockwise = isClockwiseWay(l);
						// clockwise - outer (like coastline), anticlockwise - inner
						boolean inverse = clockwise != !innerType;
						for(Way es : l){
							boolean inner = "inner".equals(entities.get(es));
							if(innerType != inner){
								throw new IllegalStateException();
							}
							if(!inner && name != null){
								multiPolygonsNames.put(es.getId(), name);
							}
							putMultipolygonType(multiPolygonsWays0, es.getId(), mtType, inverse);
							putMultipolygonType(multiPolygonsWays1, es.getId(), mtType1, inverse);
							putMultipolygonType(multiPolygonsWays2, es.getId(), mtType2, inverse);
						}
					}

				}
			}
		} else if (step == STEP_CITY_NODES) {
			registerCityIfNeeded(e);
		}
	}
	
	
	public void combineMultiPolygons(Way w, List<List<Way>> completedRings, List<List<Way>> incompletedRings){
		long lId = w.getEntityIds().get(w.getEntityIds().size() - 1).getId().longValue();
		long fId = w.getEntityIds().get(0).getId().longValue();
		if (fId == lId) {
			completedRings.add(Collections.singletonList(w));
		} else {
			List<Way> l = new ArrayList<Way>();
			l.add(w);
			boolean add = true;
			for (int k = 0; k < incompletedRings.size();) {
				boolean remove = false;
				List<Way> i = incompletedRings.get(k);
				Way last = i.get(i.size() - 1);
				Way first = i.get(0);
				long lastId = last.getEntityIds().get(last.getEntityIds().size() - 1).getId().longValue();
				long firstId = first.getEntityIds().get(0).getId().longValue();
				if (fId == lastId) {
					i.addAll(l);
					remove = true;
					l = i;
					fId = firstId;
				} else if (lId == firstId) {
					l.addAll(i);
					remove = true;
					lId = lastId;
				}
				if (remove) {
					incompletedRings.remove(k);
				} else {
					k++;
				}
				if (fId == lId) {
					completedRings.add(l);
					add = false;
					break;
				}
			}
			if (add) {
				incompletedRings.add(l);
			}
		}
	}
	
	
	private void putMultipolygonType(Map<Long, Set<Integer>> multiPolygonsWays, long baseId, int mtType, boolean inverse){
		if(mtType == 0){
			return;
		}
		if(!multiPolygonsWays.containsKey(baseId)){
			multiPolygonsWays.put(baseId, new LinkedHashSet<Integer>());
		}
		if(inverse){
			multiPolygonsWays.get(baseId).add(mtType | (1 << 15));
		} else {
			multiPolygonsWays.get(baseId).add(mtType );
		}
	}
	
	private int findMultiPolygonType(Entity e, int level){
		int t = MapRenderingTypes.encodeEntityWithType(e, level, true, typeUse);
		int mtType = 0;
		if (t != 0) {
			if (((t >> 1) & 3) == MapRenderingTypes.MULTY_POLYGON_TYPE) {
				// last bit is used for direction
				mtType = (t >> 1) & 0x7fff;
			} else if (((t >> 16) & 3) == MapRenderingTypes.MULTY_POLYGON_TYPE && (t >> 16) != 0) {
				mtType = t >> 16;
			} else {
				for (Integer i : typeUse) {
					if ((i & 3) == MapRenderingTypes.MULTY_POLYGON_TYPE) {
						mtType = i;
						break;
					}
				}
			}
		}
		return mtType;
	}
	
	public Point2D.Float getIntersectionPoint(Line2D.Float line1, Line2D.Float line2) {
		if (!line1.intersectsLine(line2))
			return null;
		double px = line1.getX1(), py = line1.getY1(), rx = line1.getX2() - px, ry = line1.getY2() - py;
		double qx = line2.getX1(), qy = line2.getY1(), sx = line2.getX2() - qx, sy = line2.getY2() - qy;

		double det = sx * ry - sy * rx;
		if (det == 0) {
			return null;
		} else {
			double z = (sx * (qy - py) + sy * (px - qx)) / det;
			if (z <= 0 || z >= 1)
				return null; // intersection at end point!
			return new Point2D.Float((float) (px + z * rx), (float) (py + z * ry));
		}
	} // end intersection line-line
	

	private boolean isClockwiseWay(List<Way> ways){
		if(ways.isEmpty()){
			return false;
		}
		List<Node> nodes;
		if(ways.size() == 1){
			nodes = ways.get(0).getNodes();
		} else {
			nodes = new ArrayList<Node>();
			boolean first = true;
			for(Way e : ways){
				if(first) {
					first = false;
					nodes.addAll(e.getNodes());
				} else {
					nodes.addAll(e.getNodes().subList(1, e.getNodes().size()));
				}
			}
		}
		if(nodes.isEmpty()){
			return false;
		}
		double angle = 0;
		double prevAng = 0;
		double firstAng = 0;
		double selfIntersection = 0;
		boolean open = nodes.get(nodes.size() - 1).getId() != nodes.get(0).getId();
		
		for (int i = 1; open ? i < nodes.size() : i <= nodes.size(); i++) {//nodes.get(i).getId()
			double ang;
			if(i < nodes.size()){
				ang = Math.atan2(nodes.get(i).getLatitude() - nodes.get(i - 1).getLatitude(), 
						nodes.get(i).getLongitude() - nodes.get(i - 1).getLongitude());
				// find self intersection
				Line2D.Float l = new Line2D.Float((float) nodes.get(i).getLongitude(), (float) nodes.get(i).getLatitude(), 
						(float) nodes.get(i - 1).getLongitude(), (float) nodes.get(i -1).getLatitude());
				for(int j = i - 2; j > i - 7; j--){
					if(j < 1){
						break;
					}
					Line2D.Float l2 = new Line2D.Float((float) nodes.get(j).getLongitude(), (float) nodes.get(j).getLatitude(), 
							(float) nodes.get(j - 1).getLongitude(), (float) nodes.get(j -1).getLatitude());
					java.awt.geom.Point2D.Float point = getIntersectionPoint(l, l2);
					if(point != null){
						double dang = Math.atan2(nodes.get(j).getLatitude() - nodes.get(j - 1).getLatitude(), 
								nodes.get(j).getLongitude() - nodes.get(j - 1).getLongitude());
						if (adjustDirection(ang - dang) < 0) {
							selfIntersection += 2 * Math.PI;
						} else {
							selfIntersection -= 2 * Math.PI;
						}
					}
					
				}
			} else {
				ang = firstAng;
			}
			if (i > 1) {
				angle += adjustDirection(ang - prevAng);
				prevAng = ang;
			} else {
				prevAng = ang;
				firstAng = ang;
			}

		}
		return (angle - selfIntersection) < 0;
	}
	
	private double adjustDirection(double ang){
		if (ang < -Math.PI) {
			ang += 2 * Math.PI;
		} else if (ang > Math.PI) {
			ang -= 2 * Math.PI;
		}
		return ang;
	}


	private void registerCityIfNeeded(Entity e) {
		if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
			City city = new City((Node) e);
			if(city.getType() != null && !Algoritms.isEmpty(city.getName())){
				convertEnglishName(city);
				if(city.getType() == CityType.CITY || city.getType() == CityType.TOWN){
					cityManager.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), city);
				} else {
					cityVillageManager.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), city);
				}
				cities.put(city.getEntityId(), city);
			}
		}
	}
	
	
	private <T, R> void putIntoMap(Map<T, List<R>> map, T key, R value){
		if(!map.containsKey(key)){
			map.put(key, new ArrayList<R>());
		}
		map.get(key).add(value);
	}
	
	private <T, R> void removeValueMap(Map<T, List<R>> map, T key, R value){
		boolean remove = map.get(key).remove(value);
		if(!remove){
			throw new IllegalStateException();
		}
		if(map.get(key).isEmpty()){
			map.remove(key);
		}
	}
	
	// TODO do not use that method
	@SuppressWarnings("unused")
	private void indexLowLevelMap(Entity e, int level) throws SQLException{
		int type = MapRenderingTypes.encodeEntityWithType(e, level, false, typeUse);
		// TODO that is not correct because multiPolygonsWays contains types for level 0
		// TODO that method should be redesigned (!!!) to support multipolygon types and multipolygon names 
//		if(type == 0 && multiPolygonsWays.containsKey(e.getId())){
//			type = multiPolygonsWays.get(e.getId()).iterator().next();
//		}
		if(type == 0){
			return;
		}
		boolean writeIntoDB = true;
		boolean ring = true;
		if(e instanceof Way && level > 0){
			List<Node> nodes = ((Way) e).getNodes();
			Node n = nodes.get(0);
			Node l = nodes.get(nodes.size() - 1);
			// ring
			ring = l.getId() == n.getId();
			if (!ring) {
				writeIntoDB = false;
				Way start = null;
				if (lowLevelWaysEnd.containsKey(n.getId())) {
					for (Way w : lowLevelWaysEnd.get(n.getId())) {
						int t = MapRenderingTypes.encodeEntityWithType(w, level, false, typeUse);
						if (t == type && Algoritms.objectEquals(MapRenderingTypes.getEntityName(w, t), 
								MapRenderingTypes.getEntityName(e, t))) {
							start = w;
							break;
						}
					}
				}
				
				if (start != null) {
					ring = start.getNodeIds().get(0) == l.getId();
					if(ring){
						removeValueMap(lowLevelWaysEnd, n.getId(), start);
						removeValueMap(lowLevelWaysSt, l.getId(), start);
					} else {
						// add nodes to start
						for (int i = 1; i < nodes.size(); i++) {
							start.addNode(nodes.get(i));
						}
						removeValueMap(lowLevelWaysEnd, n.getId(), start);
						putIntoMap(lowLevelWaysEnd, l.getId(), start);
					}
				} else {
					long tempId = (e.getId() << 2) | level;
					start = new Way(tempId);
					for(String t : e.getTagKeySet()){
						start.putTag(t, e.getTag(t));
					}
					// add nodes to start
					for (int i = 0; i < nodes.size(); i++) {
						start.addNode(nodes.get(i));
					}
					
					putIntoMap(lowLevelWaysSt, n.getId(), start);
					putIntoMap(lowLevelWaysEnd, l.getId(), start);
				}

				if (!ring) {
					Way end = null;
					if (lowLevelWaysSt.containsKey(l.getId())) {
						for (Way w : lowLevelWaysSt.get(l.getId())) {
							int t = MapRenderingTypes.encodeEntityWithType(w, level, false, typeUse);
							if (t == type && Algoritms.objectEquals(MapRenderingTypes.getEntityName(w, t), 
									MapRenderingTypes.getEntityName(e, t))) {
								end = w;
								break;
							}
						}
					}
					if (end != null) {
						Long ll = end.getNodeIds().get(end.getNodeIds().size() - 1);
						// remove end line
						removeValueMap(lowLevelWaysSt, l.getId(), end);
						removeValueMap(lowLevelWaysEnd, ll, end);
						ring = ll == n.getId(); 
							
						if (!ring) {

							// add nodes to start
							for (int i = 1; i < end.getNodes().size(); i++) {
								start.addNode(end.getNodes().get(i));
							}

							// remove end start
							removeValueMap(lowLevelWaysEnd, l.getId(), start);
							putIntoMap(lowLevelWaysEnd, ll, start);
						}
					}
				}
			}
		} 
		
		if(writeIntoDB || ring){
			writeEntityToMapDatabase(e, e.getId(), false, level);
		}
	}



	private void writeEntityToMapDatabase(Entity e, long baseId, boolean inverse, int level) throws SQLException {
		int type = MapRenderingTypes.encodeEntityWithType(e, level, false, typeUse);
		Map<Long, Set<Integer>> multiPolygonsWays;
		if(level == 0){
			multiPolygonsWays = multiPolygonsWays0;
		} else if(level == 1){
			multiPolygonsWays = multiPolygonsWays1;
		} else if(level == 2){
			multiPolygonsWays = multiPolygonsWays2;
		} else {
			multiPolygonsWays = Collections.emptyMap();
		}
		boolean hasMulti = e instanceof Way && multiPolygonsWays.containsKey(e.getId());
		if(type == 0){
			if(hasMulti){
				Set<Integer> set = multiPolygonsWays.get(e.getId());
				boolean first = true;
				for(Integer i : set){
					if(first){
						type = i << 1;
						first = false;
					} else {
						typeUse.add(i);
					}
				}
			} else {
				return;
			}
		} else if(hasMulti){
			Set<Integer> set = multiPolygonsWays.get(e.getId());
			for(Integer i : set){
				// do not compare direction
				int k = i & 0x7fff; 
				int ks = k | MapRenderingTypes.POLYGON_TYPE;
				// turn of polygon type 3 ^ (suppose polygon = multipolygon)
				if(ks == ((type >> 1) & 0xffff)){
					type = ((type >> 16) << 16) | (i << 1); 
				} else if(ks == type >> 16){
					type = (type & 0xffff) | (i << 16);
				} else  {
					int ind = typeUse.indexOf(ks);
					if (ind == -1) {
						typeUse.add(i);
					} else {
						typeUse.set(ind, i);
					}
				}
				
			}
		}
		
		// be sure about last segments
		if(!typeUse.isEmpty()){
			type |= 1;
		}
		
		boolean useRestrictions = false;
		restrictionsUse.clear();
		if(MapRenderingTypes.isHighwayType(type >> 1)){
			useRestrictions = true;
			// try to find restrictions only for max zoom level
			if(level == 0 && highwayRestrictions.containsKey(baseId)){
				restrictionsUse.addAll(highwayRestrictions.get(baseId));
			}
			
		}
		
		boolean point = ((type >> 1) & 3) == MapRenderingTypes.POINT_TYPE;
		PreparedStatement mapLocations;
		int zoom;
		long id = baseId << 3;
		if (level == 1) {
			id |= 2;
			mapLocations = mapLocsStatLevel1;
			zoom = 12;
		} else if (level == 2) {
			id |= 4;
			zoom = 7;
			mapLocations = mapLocsStatLevel2;
		} else {
			zoom = 18;
			mapLocations = mapLocsStatLevel0;
		}
		boolean skip = false;
		if (e instanceof Way) {
			id |= 1;
			// simplify route
			if (level > 0) {
				List<Node> nodes = ((Way) e).getNodes();
				Way way = new Way(id);
				for(String t : e.getTagKeySet()){
					way.putTag(t, e.getTag(t));
				}
				int prevX = 0;
				int prevY = 0;
				int len = 0;
				for (int i = 0; i < nodes.size(); i++) {
					// d	o not simplify last node it could be important node for multipolygon
					int r = i < nodes.size() - 1 ? 4 : 0;
					int x = (int) (MapUtils.getTileNumberX(zoom, nodes.get(i).getLongitude()) * 256d);
					int y = (int) (MapUtils.getTileNumberY(zoom, nodes.get(i).getLatitude()) * 256d);
					int dy = Math.abs(y - prevY);
					int dx = Math.abs(x - prevX);
					if (dx > r || dy > r) {
						way.addNode(nodes.get(i));
						len += (dx + dy);
						prevX = x;
						prevY = y;
					}
				}
				e = way;
				skip = way.getNodes().size() < 2;
				if(!hasMulti && len < 8){
					skip = true;
				}
			}
			
		}

		
		if (!skip) {
			String eName = MapRenderingTypes.getEntityName(e, type);
			if(eName == null ){
				eName = multiPolygonsNames.get(baseId);
			}
			DataIndexWriter.insertMapRenderObjectIndex(pStatements, mapObjStat, mapLocations, /*mapTree, */e,  
					eName, id, type, typeUse, restrictionsUse, useRestrictions,
					inverse, point, BATCH_SIZE);
		}
	}
	
	
	
	
	private void convertEnglishName(MapObject o){
		String name = o.getName();
		if(name != null && (o.getEnName() == null || o.getEnName().isEmpty())){
			o.setEnName(Junidecode.unidecode(name));
		}
	}
	
	public void generateIndexes(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException,
	SQLException {
		if (readFile != null && regionName == null) {
			int i = readFile.getName().indexOf('.');
			if(i > -1){
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(readFile.getName().substring(0, i));
			}
		}
		
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e);
			throw new IllegalStateException(e);
		}
		boolean loadFromPath = dbFile == null || !dbFile.exists();
		if (dbFile == null) {
			dbFile = new File(workingDir, TEMP_NODES_DB);
			// to save space
			if (dbFile.exists()) {
				dbFile.delete();
			}
		}
		// creating nodes db to fast access for all nodes
		dbConn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
		
		// clear previous results
		cities.clear();
		cityManager.clear();
		postalCodeRelations.clear();
		
		int allRelations = 100000;
		int allWays = 1000000;
		int allNodes = 10000000;
		if (loadFromPath) {
			InputStream stream = new FileInputStream(readFile);
			InputStream streamFile = stream;
			long st = System.currentTimeMillis();
			if (readFile.getName().endsWith(".bz2")) {
				if (stream.read() != 'B' || stream.read() != 'Z') {
					throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
				} else {
					stream = new CBZip2InputStream(stream);
				}
			}

			if (progress != null) {
				progress.startTask("Loading file " + readFile.getAbsolutePath(), -1);
			}

			OsmBaseStorage storage = new OsmBaseStorage();
			storage.setSupressWarnings(DataExtractionSettings.getSettings().isSupressWarningsForDuplicatedId());
			if (addFilter != null) {
				storage.getFilters().add(addFilter);
			}

			// 1. Loading osm file
			NewDataExtractionOsmFilter filter = new NewDataExtractionOsmFilter();
			try {
				// 1 init database to store temporary data
				progress.setGeneralProgress("[50 of 100]");

				filter.initDatabase();
				storage.getFilters().add(filter);
				storage.parseOSM(stream, progress, streamFile, false);
				filter.finishLoading();
				allNodes = filter.getAllNodes();
				allWays = filter.getAllWays();
				allRelations = filter.getAllRelations();

				if (log.isInfoEnabled()) {
					log.info("File parsed : " + (System.currentTimeMillis() - st));
				}
				progress.finishTask();

			} finally {
				if (log.isInfoEnabled()) {
					log.info("File indexed : " + (System.currentTimeMillis() - st));
				}
			}
		}
		


		// 2. Processing all entries

		pselectNode = dbConn.prepareStatement("select * from node where id = ?");
		pselectWay = dbConn.prepareStatement("select * from ways where id = ? order by ord");
		pselectRelation = dbConn.prepareStatement("select * from relations where id = ? order by ord");
		pselectTags = dbConn.prepareStatement("select key, value from tags where id = ? and type = ?");
		
		if(indexMap){
			mapFile = new File(workingDir, getMapFileName());
			// to save space
			if (mapFile.exists()) {
				mapFile.delete();
			}
			mapFile.getParentFile().mkdirs();
			mapConnection = DriverManager.getConnection("jdbc:sqlite:" + mapFile.getAbsolutePath());
			
			DataIndexWriter.createMapIndexStructure(mapConnection);
			mapObjStat = DataIndexWriter.createStatementMapWaysInsert(mapConnection);
			mapLocsStatLevel0 = DataIndexWriter.createStatementMapWaysLocationsInsert(mapConnection);
			mapLocsStatLevel1 = DataIndexWriter.createStatementMapWaysLocationsInsertLevel2(mapConnection);
			mapLocsStatLevel2 = DataIndexWriter.createStatementMapWaysLocationsInsertLevel3(mapConnection);
//			try {
//				mapTree = new RTree(mapFile.getAbsolutePath()+"_ind");
//			} catch (RTreeException e) {
//				e.printStackTrace();
//			}
			pStatements.put(mapObjStat, 0);
			pStatements.put(mapLocsStatLevel0, 0);
			pStatements.put(mapLocsStatLevel1, 0);
			pStatements.put(mapLocsStatLevel2, 0);
			mapConnection.setAutoCommit(false);
		}

		if (indexPOI) {
			poiIndexFile = new File(workingDir, getPoiFileName());
			// to save space
			if (poiIndexFile.exists()) {
				poiIndexFile.delete();
			}
			poiIndexFile.getParentFile().mkdirs();
			// creating nodes db to fast access for all nodes
			poiConnection = DriverManager.getConnection("jdbc:sqlite:" + poiIndexFile.getAbsolutePath());
			DataIndexWriter.createPoiIndexStructure(poiConnection);
			poiPreparedStatement = DataIndexWriter.createStatementAmenityInsert(poiConnection);
			pStatements.put(poiPreparedStatement, 0);
			poiConnection.setAutoCommit(false);
		}

		if (indexTransport) {
			transportIndexFile = new File(workingDir, getTransportFileName());
			// to save space
			if (transportIndexFile.exists()) {
				transportIndexFile.delete();
			}
			transportIndexFile.getParentFile().mkdirs();
			// creating nodes db to fast access for all nodes
			transportConnection = DriverManager.getConnection("jdbc:sqlite:" + transportIndexFile.getAbsolutePath());

			DataIndexWriter.createTransportIndexStructure(transportConnection);
			transRouteStat = transportConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexTransportRoute
					.getTable(), IndexTransportRoute.values().length));
			transRouteStopsStat = transportConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(
					IndexTransportRouteStop.getTable(), IndexTransportRouteStop.values().length));
			transStopsStat = transportConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexTransportStop
					.getTable(), IndexTransportStop.values().length));
			pStatements.put(transRouteStat, 0);
			pStatements.put(transRouteStopsStat, 0);
			pStatements.put(transStopsStat, 0);
			transportConnection.setAutoCommit(false);

		}
		
		if (indexAddress) {
			addressIndexFile = new File(workingDir, getAddressFileName());
			// to save space
			if (addressIndexFile.exists()) {
				addressIndexFile.delete();
			}
			addressIndexFile.getParentFile().mkdirs();
			// creating nodes db to fast access for all nodes
			addressConnection = DriverManager.getConnection("jdbc:sqlite:" + addressIndexFile.getAbsolutePath());

			DataIndexWriter.createAddressIndexStructure(addressConnection);
			addressCityStat = addressConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexCityTable.getTable(),
					IndexCityTable.values().length));
			addressStreetStat = addressConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexStreetTable
					.getTable(), IndexStreetTable.values().length));
			addressSearchStreetStat = addressConnection.prepareStatement("SELECT " + IndexStreetTable.ID.name() + " FROM " + 
					IndexStreetTable.getTable() + " WHERE ? = " + IndexStreetTable.CITY.name() + " AND ? =" + IndexStreetTable.NAME.name());
			addressSearchBuildingStat = addressConnection.prepareStatement("SELECT " + IndexBuildingTable.ID.name() + " FROM " + 
					IndexBuildingTable.getTable() + " WHERE ? = " + IndexBuildingTable.ID.name());
			addressSearchStreetNodeStat = addressConnection.prepareStatement("SELECT " + IndexStreetNodeTable.WAY.name() + " FROM " + 
					IndexStreetNodeTable.getTable() + " WHERE ? = " + IndexStreetNodeTable.WAY.name());
			addressBuildingStat = addressConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexBuildingTable
					.getTable(), IndexBuildingTable.values().length));
			addressStreetNodeStat = addressConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(
					IndexStreetNodeTable.getTable(), IndexStreetNodeTable.values().length));
			pStatements.put(addressCityStat, 0);
			pStatements.put(addressStreetStat, 0);
			pStatements.put(addressStreetNodeStat, 0);
			pStatements.put(addressBuildingStat, 0);
			// put search statements to close them after all
			pStatements.put(addressSearchBuildingStat, 0);
			pStatements.put(addressSearchStreetNodeStat, 0);
			pStatements.put(addressSearchStreetStat, 0);
			
			addressConnection.setAutoCommit(false);
		}

		if(normalizeStreets){
			normalizeDefaultSuffixes = DataExtractionSettings.getSettings().getDefaultSuffixesToNormalizeStreets();
			normalizeSuffixes = DataExtractionSettings.getSettings().getSuffixesToNormalizeStreets();
		}
		

		// 1. write all cities
		
		if(indexAddress){
			progress.setGeneralProgress("[55 of 100]");
			progress.startTask("Indexing cities...", -1);
			if(!loadFromPath){
				allNodes = iterateOverEntities(progress, EntityType.NODE, allNodes, STEP_CITY_NODES);
			}
			
			for(City c : cities.values()){
				DataIndexWriter.writeCity(addressCityStat, pStatements, c, BATCH_SIZE);
			}
			// commit to put all cities
			if(pStatements.get(addressCityStat) > 0){
				addressCityStat.executeBatch();
				pStatements.put(addressCityStat, 0);
				addressConnection.commit();
			}
			
		}
		
		// 2. index address relations
		if(indexAddress || indexMap){
			progress.setGeneralProgress("[55 of 100]");
			progress.startTask("Preindexing address...", -1);
			allRelations = iterateOverEntities(progress, EntityType.RELATION, allRelations, STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS);
			// commit to put all cities
			if (indexAddress) {
				if (pStatements.get(addressBuildingStat) > 0) {
					addressBuildingStat.executeBatch();
					pStatements.put(addressBuildingStat, 0);
				}
				if (pStatements.get(addressStreetNodeStat) > 0) {
					addressStreetNodeStat.executeBatch();
					pStatements.put(addressStreetNodeStat, 0);
				}
				addressConnection.commit();
			}
		}
		

		// 3. iterate over all entities
		iterateOverAllEntities(progress, allNodes, allWays, allRelations, STEP_MAIN);
		
		
		// 4. update all postal codes from relations
		if(indexAddress && !postalCodeRelations.isEmpty()){
			progress.setGeneralProgress("[95 of 100]");
			progress.startTask("Registering postcodes...", -1);
			if(pStatements.get(addressBuildingStat) > 0){
				addressBuildingStat.executeBatch();
				pStatements.put(addressBuildingStat, 0);
				addressConnection.commit();
			}
			
			PreparedStatement pstat = addressConnection.prepareStatement("UPDATE " + IndexBuildingTable.getTable() + 
					" SET " + IndexBuildingTable.POSTCODE.name() + " = ? WHERE " + IndexBuildingTable.ID.name() + " = ?");
			pStatements.put(pstat, 0);
			for(Relation r : postalCodeRelations){
				String tag = r.getTag(OSMTagKey.POSTAL_CODE);
				for(EntityId l : r.getMemberIds()){
					pstat.setString(1, tag);
					pstat.setLong(2, l.getId());
					DataIndexWriter.addBatch(pStatements, pstat, BATCH_SIZE);
				}
			}
			
		}
		// 5. writing low level maps
		progress.setGeneralProgress("[95 of 100]");
		progress.startTask("Indexing low levels for map ...", -1);
		if(indexMap){
			for(Long l : lowLevelWaysSt.keySet()){
				for(Way w : lowLevelWaysSt.get(l)){
					int level = (int) (w.getId() & 3);
					writeEntityToMapDatabase(w, w.getId() >> 2, false, level);
				}
			}
			
		}

		try {
			if (pselectNode != null) {
				pselectNode.close();
			}
			if (pselectWay != null) {
				pselectWay.close();
			}
			if (pselectRelation != null) {
				pselectRelation.close();
			}
			if (pselectTags != null) {
				pselectTags.close();
			}
			for (PreparedStatement p : pStatements.keySet()) {
				if (pStatements.get(p) > 0) {
					p.executeBatch();
				}
				p.close();
			}

			if (poiConnection != null) {
				poiConnection.commit();
				poiConnection.close();
				if (lastModifiedDate != null) {
					poiIndexFile.setLastModified(lastModifiedDate);
				}
			}
			if (transportConnection != null) {
				transportConnection.commit();
				transportConnection.close();
				if (lastModifiedDate != null) {
					transportIndexFile.setLastModified(lastModifiedDate);
				}
			}
			
			if (mapConnection != null) {
				mapConnection.commit();
				mapConnection.close();
//				try {
//					mapTree.flush();
//				} catch (RTreeException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				if (lastModifiedDate != null) {
					mapFile.setLastModified(lastModifiedDate);
				}
			}
			
			if (addressConnection != null) {
				addressConnection.commit();
				addressConnection.close();
				if (lastModifiedDate != null) {
					addressIndexFile.setLastModified(lastModifiedDate);
				}
			}

			dbConn.close();
		} catch (SQLException e) {
		}
	}
	
	public static void removeWayNodes(File sqlitedb) throws SQLException{
		Connection dbConn = DriverManager.getConnection("jdbc:sqlite:" + sqlitedb.getAbsolutePath());
		dbConn.setAutoCommit(false);
		Statement st = dbConn.createStatement();
		st.execute("DELETE FROM " + IndexStreetNodeTable.getTable() + " WHERE 1=1");
		st.close();
		dbConn.commit();
		dbConn.setAutoCommit(true);
		st = dbConn.createStatement();
		st.execute("VACUUM");
		st.close();
		dbConn.close();
	}
	
	 public static void main(String[] args) throws IOException, SAXException, SQLException {
		
		 IndexCreator creator = new IndexCreator(new File("e:/Information/OSM maps/osmand/"));
//		 creator.setIndexMap(true);
		 creator.setIndexPOI(true);
		 
		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/minsk.tmp.odb"));
		 creator.generateIndexes(new File("e:/Information/OSM maps/belarus osm/minsk.osm"), new ConsoleProgressImplementation(3), null);

//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/belarus_nodes.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/belarus osm/belarus_2010_09_03.osm.bz2"), new ConsoleProgressImplementation(3), null);

//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/ams.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/ams_part_map.osm"), new ConsoleProgressImplementation(3), null);
		 
//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/den_haag.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/den_haag.osm"), new ConsoleProgressImplementation(3), null);
		 
//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/netherlands.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/netherlands.osm.bz2"), new ConsoleProgressImplementation(1), null);
		 
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/forest_complex.osm"), new ConsoleProgressImplementation(25), null);
		/*try {
//			RTree rtree = new RTree("e:/Information/OSM maps/osmand/Belarus_2010_09_03.map.odb_ind");
//			new Pack().packTree(rtree, "e:/Information/OSM maps/osmand/pack.ind");
			RTree rtree = new RTree("e:/Information/OSM maps/osmand/pack.ind");
			long rootIndex = rtree.getFileHdr().getRootIndex();
			int s = calculateSize(rtree.getReadNode(rootIndex), rtree, "!-");
			System.out.println(s);
		} catch (RTreeException e) {
			e.printStackTrace();
		}*/
		 
		 

		 
		
		 // download base 
	/*	MapTileDownloader instance = MapTileDownloader.getInstance();
		instance.addDownloaderCallback(new MapTileDownloader.IMapDownloaderCallback() {
			@Override
			public void tileDownloaded(DownloadRequest request) {
				System.out.println(request.url);
			}

		});
		File baseDir = new File("e:/Information/OSM maps/planet_tiles");
		baseDir.mkdirs();
		TileSourceTemplate map = TileSourceManager.getMapnikSource();
		for (int zoom = 5; zoom <= 5; zoom++) {
			int length = 1 << zoom;
			for (int x = 0; x < length; x++) {
				for (int y = 0; y < length; y++) {
					String file = zoom + "/" + (x) + "/" + y + map.getTileFormat() + ".tile";
					DownloadRequest req = new DownloadRequest(map.getUrlToLoad(x, y, zoom), new File(baseDir, file), x, y, zoom);
					instance.requestToDownload(req);
				}
			}
		}*/

	}
	/* 
	 public static int calculateSize(rtree.Node n, RTree r, String level){
		 Element[] e = n.getAllElements();
		 int exc = 0; //20 + (169 - n.getTotalElements()) * 24;
		 for(int i=0; i< n.getTotalElements(); i++){
			 if(e[i].getElementType() == rtree.Node.LEAF_NODE){
				 exc += 1;
			 } else {
//				 exc += 1;
				 
				 long ptr = ((NonLeafElement) e[i]).getPtr();
				 try {
					 rtree.Node ns = r.getReadNode(ptr);
					 System.out.println(level + "   " +  ns.getTotalElements());
					exc += calculateSize(ns, r, level +"-");
				} catch (RTreeException e1) {
					e1.printStackTrace();
				}
			 }
		 }
		 return exc;
	 }
	 */
}
