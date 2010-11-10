package net.osmand.data.preparation;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
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
import java.util.TreeMap;
import java.util.Map.Entry;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapIndexWriter;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.DataTileManager;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.data.City.CityType;
import net.osmand.data.index.DataIndexReader;
import net.osmand.data.index.DataIndexWriter;
import net.osmand.data.index.IndexConstants;
import net.osmand.data.index.IndexConstants.IndexBinaryMapRenderObject;
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

import rtree.Element;
import rtree.IllegalValueException;
import rtree.LeafElement;
import rtree.NonLeafElement;
import rtree.Pack;
import rtree.RTree;
import rtree.RTreeException;
import rtree.Rect;


/**
 * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Is_inside.2Foutside
 * http://wiki.openstreetmap.org/wiki/Relations/Proposed/Postal_Addresses
 * http://wiki.openstreetmap.org/wiki/Proposed_features/House_numbers/Karlsruhe_Schema#Tags (node, way)

 * That data extraction has aim, 
 * save runtime memory and generate indexes on the fly.
 * It will be longer than load in memory (needed part) and save into index.
 */
@SuppressWarnings("unchecked")
public class IndexCreator {
	private static final Log log = LogFactory.getLog(IndexCreator.class);

	// ONLY derby.jar needed for derby dialect
	private static final String DERBY_DIALECT = "DERBY";
	private static final String H2_DIALECT = "H2";
	private static final String SQLITE_DIALECT = "SQLITE";
	private static final String CURRENT_DB = H2_DIALECT;

	public static final int BATCH_SIZE = 1000;
	public static final int BATCH_SIZE_OSM = 10000;
	public static final String TEMP_NODES_DB = "nodes.tmp.odb";
	
	public static final int STEP_CITY_NODES = 1;
	public static final int STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS = 2;
	public static final int STEP_MAIN = 3;
	
	private File workingDir = null;
	
	
	private boolean indexMap;
	private boolean indexPOI;
	private boolean indexTransport;
	private boolean indexAddress;
	
	private boolean normalizeStreets = true; // true by default
	private boolean saveAddressWays = true; // true by default

	private String regionName;
	private String poiFileName = null;
	private String mapFileName = null;
	private Long lastModifiedDate = null;
	
	
	private PreparedStatement pselectNode;
	private PreparedStatement pselectWay;
	private PreparedStatement pselectRelation;
	private PreparedStatement pselectTags;

	// constants to start process from the middle and save temporary results
	private boolean recreateOnlyBinaryFile = false; //false;
	private boolean deleteOsmDB = false;
	private boolean deleteDatabaseIndexes = true;
	
	private Connection dbConn;
	private File dbFile;
	
	Map<PreparedStatement, Integer> pStatements = new LinkedHashMap<PreparedStatement, Integer>();
	
	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;
	
	
	private File mapFile;
	private RandomAccessFile mapRAFile;
	private Connection mapConnection;

	private PreparedStatement mapBinaryStat;
	
	private PreparedStatement addressCityStat;
	private PreparedStatement addressStreetStat;
	private PreparedStatement addressBuildingStat;
	private PreparedStatement addressStreetNodeStat;

	
	private Set<Long> visitedStops = new HashSet<Long>();
	private PreparedStatement transRouteStat;
	private PreparedStatement transRouteStopsStat;
	private PreparedStatement transStopsStat;
	private RTree transportStopsTree; 

	
	private RTree[] mapTree = null;
	// maximum 5 limitation => 4 levels allowed
	public static final int[] MAP_ZOOMS = new int[]{5, 8, 11, 14, 22};
//	public static final int[] MAP_ZOOMS = new int[]{5, 9, 14, 22};
	
	
	// MEMORY map :  save it in memory while that is allowed
	private Map<Long, Set<Integer>>[] multiPolygonsWays = new Map[MAP_ZOOMS.length - 1];
	{
		for (int i = 0; i < multiPolygonsWays.length; i++) {
			multiPolygonsWays[i] = new LinkedHashMap<Long, Set<Integer>>();
		}
	}
	private Map<Long, String> multiPolygonsNames = new LinkedHashMap<Long, String>();
	private Map<Long, List<Long>> highwayRestrictions = new LinkedHashMap<Long, List<Long>>();
	
	
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
	
	public static boolean usingSQLite(){
		return CURRENT_DB.equals(SQLITE_DIALECT);
	}
	
	public static boolean usingDerby(){
		return CURRENT_DB.equals(DERBY_DIALECT);
	}
	
	public static boolean usingH2(){
		return CURRENT_DB.equals(H2_DIALECT);
	}
	
	public static String getCurrentLongType(){
		return usingSQLite() ? "long" : "bigint";
	}
	
	
	
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

	
	protected static int defineLevel(int minZoom) {
		int level = 0;
		if (minZoom < 15) {
			for (int i = 1; i < MAP_ZOOMS.length; i++) {
				if (minZoom <= MAP_ZOOMS[i]) {
					level = MAP_ZOOMS.length - 1 - i;
					break;
				}
			}
		}
		return level;
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
			if (usingDerby()) {
				try {
					stat.executeUpdate("drop table node");
				} catch (SQLException e) {
					// ignore it
				}
			} else {
				stat.executeUpdate("drop table if exists node");
			}
			String longType = usingSQLite() ? "long" : "bigint";
			stat.executeUpdate("create table node (id "+longType+", latitude double, longitude double)");
			stat.executeUpdate("create index IdIndex ON node (id, latitude, longitude)");
			if (usingDerby()) {
				try {
					stat.executeUpdate("drop table ways");
				} catch (SQLException e) {
					// ignore it
				}
			} else {
				stat.executeUpdate("drop table if exists ways");
			}
			stat.executeUpdate("create table ways (id "+longType+", node "+longType+", ord smallint)");
			stat.executeUpdate("create index IdWIndex ON ways (id, node)");
			if (usingDerby()) {
				try {
					stat.executeUpdate("drop table relations");
				} catch (SQLException e) {
					// ignore it
				}
			} else {
				stat.executeUpdate("drop table if exists relations");
			}
			stat.executeUpdate("create table relations (id "+longType+", member "+longType+", type smallint, role varchar(255), ord smallint)");
			stat.executeUpdate("create index IdRIndex ON relations (id, member, type)");
			if (usingDerby()) {
				try {
					stat.executeUpdate("drop table tags");
				} catch (SQLException e) {
					// ignore it
				}
			} else {
				stat.executeUpdate("drop table if exists ефпы");
			}
			stat.executeUpdate("create table tags (id "+longType+", type smallint, skeys varchar(255), value varchar(255))");
			stat.executeUpdate("create index IdTIndex ON tags (id, type)");
			stat.close();

			prepNode = dbConn.prepareStatement("insert into node values (?, ?, ?)");
			prepWays = dbConn.prepareStatement("insert into ways values (?, ?, ?)");
			prepRelations = dbConn.prepareStatement("insert into relations values (?, ?, ?, ?, ?)");
			prepTags = dbConn.prepareStatement("insert into tags values (?, ?, ?, ?)");
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
					if (currentCountNode >= BATCH_SIZE_OSM) {
						prepNode.executeBatch();
						dbConn.commit(); // clear memory
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
					if (currentWaysCount >= BATCH_SIZE_OSM) {
						prepWays.executeBatch();
						dbConn.commit(); // clear memory
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
						prepRelations.setLong(5, ord++);
						prepRelations.addBatch();
					}
					if (currentRelationsCount >= BATCH_SIZE_OSM) {
						prepRelations.executeBatch();
						dbConn.commit(); // clear memory
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
				if (currentTagsCount >= BATCH_SIZE_OSM) {
					prepTags.executeBatch();
					dbConn.commit(); // clear memory
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
	
	private Connection getDatabaseConnection(String fileName) throws SQLException {
		return getDatabaseConnection(fileName, false);
	}
	
	public static void removeDatabase(File file) throws SQLException {
		if(usingH2()){
			File[] list = file.getParentFile().listFiles();
			for(File f : list){
				if(f.getName().startsWith(file.getName())){
					Algoritms.removeAllFiles(f);
				}
			}
		} else {
			Algoritms.removeAllFiles(file);
		}
		
	}
	
	public static boolean databaseFileExists(File dbFile){
		if(usingH2()){
			return new File(dbFile.getAbsolutePath() + ".h2.db").exists();
		} else {
			return dbFile.exists();
		}
	}
	private Connection getDatabaseConnection(String fileName, boolean forceSqLite) throws SQLException {
		if (usingSQLite() || forceSqLite) {
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			return DriverManager.getConnection("jdbc:sqlite:" + fileName);
		} else if (usingDerby()) {
			try {
				Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			Connection conn = DriverManager.getConnection("jdbc:derby:" + fileName + ";create=true");
			conn.setAutoCommit(false);
			return conn;
		} else if(usingH2()){
			try {
				Class.forName("org.h2.Driver");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			
			return DriverManager. getConnection("jdbc:h2:file:"+fileName);
		} else {
			throw new UnsupportedOperationException();
		}

	}
	
	public void loadEntityData(Entity e, boolean loadTags) throws SQLException {
		if(e instanceof Node || (e instanceof Way && !((Way) e).getNodes().isEmpty())){
			// do not load tags for nodes inside way
			return;
		}
		Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
		if(e instanceof Relation && ((Relation) e).getMemberIds().isEmpty()){
			pselectRelation.setLong(1, e.getId());
			if (pselectRelation.execute()) {
				ResultSet rs = pselectRelation.getResultSet();
				boolean first = true;
				while (rs.next()) {
					int ord = rs.getInt(4);
					if (ord > 0 || first) {
						first = false;
						((Relation) e).addMember(rs.getLong(1), EntityType.values()[rs.getInt(2)], rs.getString(3));
					}
				}
				rs.close();
			}
		} else if(e instanceof Way && ((Way) e).getEntityIds().isEmpty()) {
			pselectWay.setLong(1, e.getId());
			if (pselectWay.execute()) {
				ResultSet rs = pselectWay.getResultSet();
				boolean first = true;
				while (rs.next()) {
					int ord = rs.getInt(2);
					if (ord > 0 || first) {
						first = false;
						((Way) e).addNode(new Node(rs.getDouble(5), rs.getDouble(6), rs.getLong(1)));
					}
				}
				rs.close();
			}
		}
		Collection<EntityId> ids = e instanceof Relation? ((Relation)e).getMemberIds() : ((Way)e).getEntityIds();

		for (EntityId i : ids) {
			// pselectNode = dbConn.prepareStatement("select n.latitude, n.longitude, t.skeys, t.value from node n left join tags t on n.id = t.id and t.type = 0 where n.id = ?");
			if (i.getType() == EntityType.NODE) {
				pselectNode.setLong(1, i.getId());
				if (pselectNode.execute()) {
					ResultSet rs = pselectNode.getResultSet();
					Node n = null;
					while (rs.next()) {
						if(n == null){
							n = new Node(rs.getDouble(1), rs.getDouble(2), i.getId());
						}
						if(rs.getObject(3) != null){
							n.putTag(rs.getString(3), rs.getString(4));
						}
					}
					map.put(i, n);
					rs.close();
				}
			} else if (i.getType() == EntityType.WAY) {
//				pselectWay = dbConn.prepareStatement("select w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " +
//				"from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " +
//				"where w.id = ? order by w.ord");
				pselectWay.setLong(1, i.getId());
				if (pselectWay.execute()) {
					ResultSet rs = pselectWay.getResultSet();
					Way way = new Way(i.getId());
					map.put(i, way);
					boolean first = true;
					while (rs.next()) {
						int ord = rs.getInt(2);
						if (ord > 0 || first) {
							first = false;
							way.addNode(new Node(rs.getDouble(5), rs.getDouble(6), rs.getLong(1)));
						}
						if(ord == 0 && rs.getObject(3) != null){
							way.putTag(rs.getString(3), rs.getString(4));
						}
					}
					rs.close();
				}
			} else if (i.getType() == EntityType.RELATION) {
				pselectRelation.setLong(1, i.getId());
//				pselectRelation = dbConn.prepareStatement("select r.member, r.type, r.role, r.ord, t.skeys, t.value" +
//				"from relations r left join tags t on r.id = t.id and t.type = 2 and r.ord = 0 " +
//				"where r.id = ? order by r.ord");
				if (pselectRelation.execute()) {
					ResultSet rs = pselectRelation.getResultSet();
					Relation rel = new Relation(i.getId());
					map.put(i, rel);
					boolean first = true;
					while (rs.next()) {
						int ord = rs.getInt(4);
						if (ord > 0 || first) {
							first = false;
							rel.addMember(rs.getLong(1), EntityType.values()[rs.getInt(2)], rs.getString(3));
						}
						if(ord == 0 && rs.getObject(5) != null){
							rel.putTag(rs.getString(5), rs.getString(6));
						}
					}
					// do not load relation members recursively ? It is not needed for transport, address, poi before
					rs.close();
				}
			}
		}

		e.initializeLinks(map);
	}
	
	public void setPoiFileName(String poiFileName) {
		this.poiFileName = poiFileName;
	}
	
	public void setNodesDBFile(File file){
		dbFile = file;
	}
	
	public void setMapFileName(String mapFileName) {
		this.mapFileName = mapFileName;
	}
	public String getMapFileName() {
		if(mapFileName == null){
			return getRegionName() + IndexConstants.BINARY_MAP_INDEX_EXT;
		}
		return mapFileName;
	}
	
	public String getTempMapDBFileName(){
		return getMapFileName() + ".tmp";
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
	
	public int iterateOverEntities(IProgress progress, EntityType type, int allCount, int step) throws SQLException{
		Statement statement = dbConn.createStatement();
		String select;
		int count = 0;
		
//		stat.executeUpdate("create table tags (id "+longType+", type smallint, skeys varchar(255), value varchar(255))");
//		stat.executeUpdate("create table ways (id "+longType+", node "+longType+", ord smallint)");
//		stat.executeUpdate("create table relations (id "+longType+", member "+longType+", type smallint, role varchar(255), ord smallint)");
		if(type == EntityType.NODE){
			// filter out all nodes without tags
			select = "select n.id, n.latitude, n.longitude, t.skeys, t.value from node n inner join tags t on n.id = t.id and t.type = 0 order by n.id";
		} else if(type == EntityType.WAY){
			select = "select w.id, w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " +
					"from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " +
					"order by w.id, w.ord";
		} else {
			select = "select r.id, t.skeys, t.value  from relations r inner join tags t on t.id = r.id and t.type = 2 and r.ord = 0";
		}
		
		ResultSet rs = statement.executeQuery(select);
		Entity prevEntity = null;
		
		long prevId = -1;
		while(rs.next()){
			long curId = rs.getLong(1);
			boolean newEntity = curId != prevId;
			Entity e = prevEntity;
			if(type == EntityType.NODE){
				if(newEntity){
					e = new Node(rs.getDouble(2),rs.getDouble(3), curId);
				}
				e.putTag(rs.getString(4), rs.getString(5));
			} else if(type == EntityType.WAY){
				if(newEntity){
					e = new Way(curId);
				}
				int ord = rs.getInt(3);
				if(ord == 0 && rs.getObject(4) != null){
					e.putTag(rs.getString(4), rs.getString(5));
				}
				if(newEntity || ord > 0){
					((Way) e).addNode(new Node(rs.getDouble(6), rs.getDouble(7), rs.getLong(2)));
				}
			} else {
				if(newEntity){
					e = new Relation(curId);
				}
				e.putTag(rs.getString(2), rs.getString(3));
			}
			if(newEntity){
				count++;
				if(progress != null){
					progress.progress(1);
				}
				if(prevEntity != null){
					iterateEntity(prevEntity, step);
				}
				prevEntity = e;
			}
			prevId = curId;
		}
		if(prevEntity != null){
			count++;
			iterateEntity(prevEntity, step);
		}
		rs.close();
		return count;
	}

	protected void loadEntityTags(EntityType type, Entity e) throws SQLException {
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
				mapConnection.commit();
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
						DataIndexWriter.insertTransportIntoIndex(transRouteStat, transRouteStopsStat, transStopsStat, transportStopsTree, 
								visitedStops, route, pStatements, BATCH_SIZE);
					}
				}
			}
			
			if (indexMap && (e instanceof Way || e instanceof Node)) {
				// manipulate what kind of way to load
				loadEntityData(e, false);
				boolean inverse = "-1".equals(e.getTag(OSMTagKey.ONEWAY));
				for (int i = 0; i < MAP_ZOOMS.length - 1; i++) {
					writeBinaryEntityToMapDatabase(e, e.getId(), i == 0 ? inverse : false, i);
				}
				
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
					
					// if we saved address ways we could checked that we registered before 
					if (saveAddressWays) {
						if (loadInMemory) {
							exist = addressStreetNodeLocalSet.contains(e.getId());
						} else {
							addressSearchStreetNodeStat.setLong(1, e.getId());
							ResultSet rs = addressSearchStreetNodeStat.executeQuery();
							exist = rs.next();
							rs.close();
						}
					}
					
					// check that street way is not registered already 
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
						loadEntityData(e, true);
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
						for (Way es : l) {
							boolean inner = "inner".equals(entities.get(es));
							if (innerType != inner) {
								throw new IllegalStateException();
							}
							if (!inner && name != null) {
								multiPolygonsNames.put(es.getId(), name);
							}
							putMultipolygonType(multiPolygonsWays[0], es.getId(), mtType, inverse);
							for (int i = 1; i < multiPolygonsWays.length; i++) {
								int type = findMultiPolygonType(e, i);
								if (type != 0) {
									putMultipolygonType(multiPolygonsWays[i], es.getId(), type, inverse);
								}
							}
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
		int t = MapRenderingTypes.encodeEntityWithType(e, MAP_ZOOMS[MAP_ZOOMS.length - level - 1], true, typeUse);
		int mtType = 0;
		if (t != 0) {
			if ((t & 3) == MapRenderingTypes.MULTY_POLYGON_TYPE) {
				mtType = t;
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
	
	

	private void writeBinaryEntityToMapDatabase(Entity e, long baseId, boolean inverse, int level) throws SQLException {
		int type = MapRenderingTypes.encodeEntityWithType(e, MAP_ZOOMS[MAP_ZOOMS.length - level - 1], false, typeUse);
		Map<Long, Set<Integer>> multiPolygonsWays = this.multiPolygonsWays[level];
		boolean hasMulti = e instanceof Way && multiPolygonsWays.containsKey(e.getId());
		if (hasMulti) {
			Set<Integer> set = multiPolygonsWays.get(e.getId());
			boolean first = true;
			for (Integer i : set) {
				if (first && type == 0) {
					type = i;
					first = false;
				} else {
					// do not compare direction
					int k = i & 0x7fff;
					int ks = k | MapRenderingTypes.POLYGON_TYPE;
					// turn of polygon type 3 ^ (suppose polygon = multipolygon)
					if (ks == type) {
						type = i;
					} else {
						int ind = typeUse.indexOf(ks);
						if (ind == -1) {
							typeUse.add(i);
						} else {
							typeUse.set(ind, i);
						}
					}
				}
			}
		}

		if (type == 0) {
			return;
		}

		restrictionsUse.clear();
		if (MapRenderingTypes.isHighwayType(type)) {
			// try to find restrictions only for max zoom level
			if (level == 0 && highwayRestrictions.containsKey(baseId)) {
				restrictionsUse.addAll(highwayRestrictions.get(baseId));
			}

		}

		boolean point = (type & 3) == MapRenderingTypes.POINT_TYPE;
		RTree rtree = null;
		int zoom;
		long id = (baseId << 3) | ((level & 3) << 1);
		rtree = mapTree[level];
		zoom = MAP_ZOOMS[MAP_ZOOMS.length - level - 1] - 2;
		boolean skip = false;
		if (e instanceof Way) {
			id |= 1;
			// simplify route
			if (level > 0) {
				List<Node> nodes = ((Way) e).getNodes();
				Way way = new Way(id);
				for (String t : e.getTagKeySet()) {
					way.putTag(t, e.getTag(t));
				}
				int prevX = 0;
				int prevY = 0;
				boolean cycle = ((Way) e).getNodeIds().get(0).longValue() == ((Way) e).getNodeIds().get(nodes.size() - 1).longValue();
				boolean addLast = hasMulti || cycle;  
				
				int minX = Integer.MAX_VALUE;
				int maxX = Integer.MIN_VALUE;
				int minY = Integer.MAX_VALUE;
				int maxY = Integer.MIN_VALUE;
				for (int i = 0; i < nodes.size(); i++) {
					int r = i == nodes.size() - 1 ? 1 : 3;
					// do not simplify last node it could be important node for multipolygon
					if (nodes.get(i) != null) {
						int x = (int) (MapUtils.getTileNumberX(zoom, nodes.get(i).getLongitude()) * 256d);
						int y = (int) (MapUtils.getTileNumberY(zoom, nodes.get(i).getLatitude()) * 256d);
						int dy = Math.abs(y - prevY);
						int dx = Math.abs(x - prevX);
						if (dx > r || dy > r || (addLast && i == nodes.size() - 1)) {
							way.addNode(nodes.get(i));
							prevX = x;
							prevY = y;
						}
						minX = Math.min(minX, x);
						maxX = Math.max(maxX, x);
						minY = Math.min(minY, y);
						maxY = Math.max(maxY, y);
					}
				}
				e = way;
				skip = way.getNodes().size() < 2;
				if (/* !hasMulti && */(maxX - minX) <= 4 && (maxY - minY) <= 4) {
					skip = true;
				}
				if(!cycle && hasMulti){
					skip = false;
				}
			}

		}

		if (!skip) {
			int highwayAttributes = 0;
			if(MapRenderingTypes.isHighwayType(type)){
				highwayAttributes = MapRenderingTypes.getHighwayAttributes(e);
			}
			String eName = MapRenderingTypes.getEntityName(e, type);
			if (eName == null) {
				eName = multiPolygonsNames.get(baseId);
			}
			DataIndexWriter.insertBinaryMapRenderObjectIndex(pStatements, mapBinaryStat, rtree, e, eName, id, 
					type, typeUse, highwayAttributes, restrictionsUse, inverse, point, BATCH_SIZE);
		}
	}
	
	
	public boolean nodeIsLastSubTree(RTree tree, long ptr) throws RTreeException{
		rtree.Node parent = tree.getReadNode(ptr);
		Element[] e = parent.getAllElements();
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				return false;
			}
		}
		return true;
		
	}
	
	
	public void writeBinaryAddressIndex(BinaryMapIndexWriter writer, IProgress progress) throws IOException, SQLException {
		boolean readWayNodes = saveAddressWays;
		
		writer.startWriteAddressIndex(getRegionName());
		DataIndexReader reader = new DataIndexReader();
		List<City> cities = reader.readCities(mapConnection);
		List<Street> streets = new ArrayList<Street>();
		Collections.sort(cities, new Comparator<City>(){

			@Override
			public int compare(City o1, City o2) {
				if (o1.getType() != o2.getType()) {
					return (o1.getType().ordinal() - o2.getType().ordinal());
				}
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		PreparedStatement streetstat = reader.getStreetsBuildingPreparedStatement(mapConnection);
		PreparedStatement waynodesStat = null;
		if(readWayNodes){
			waynodesStat = reader.getStreetsWayNodesPreparedStatement(mapConnection);
		}
		
		int j = 0;
		for (; j < cities.size(); j++) {
			City c = cities.get(j);
			if (c.getType() != CityType.CITY && c.getType() != CityType.TOWN) {
				break;
			}
		}
		progress.startTask("Searalizing city addresses...", j + ((cities.size() - j) / 100 + 1));
		
		Map<String, Set<Street>> postcodes = new TreeMap<String, Set<Street>>();
		boolean writeCities = true;
		
		// write cities and after villages
		writer.startCityIndexes(false);
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			if(writeCities){
				progress.progress(1);
			} else if((cities.size() - i) % 100 == 0){
				progress.progress(1);
			}
			if (writeCities && c.getType() != CityType.CITY && c.getType() != CityType.TOWN) {
				writer.endCityIndexes(false);
				writer.startCityIndexes(true);
				writeCities = false;
			}

			streets.clear();
			Map<Street, List<Node>> streetNodes = null;
			if(readWayNodes){
				streetNodes = new LinkedHashMap<Street, List<Node>>();
			}
			reader.readStreetsBuildings(streetstat, c, streets, waynodesStat, streetNodes);
			writer.writeCityIndex(c, streets, streetNodes);
			for (Street s : streets) {
				for (Building b : s.getBuildings()) {
					if (b.getPostcode() != null) {
						if (!postcodes.containsKey(b.getPostcode())) {
							postcodes.put(b.getPostcode(), new LinkedHashSet<Street>(3));
						}
						postcodes.get(b.getPostcode()).add(s);
					}
				}
			}
		}
		writer.endCityIndexes(!writeCities);
		
		
		// write postcodes
		writer.startPostcodes();
		for(String s : postcodes.keySet()){
			writer.writePostcode(s, postcodes.get(s));
		}
		writer.endPostcodes();
		
	
		progress.finishTask();
		
		writer.endWriteAddressIndex();
		
	}


	public void writeBinaryMapIndex(BinaryMapIndexWriter writer) throws IOException, SQLException {
		try {
			assert IndexConstants.IndexBinaryMapRenderObject.values().length == 6;
			PreparedStatement selectData = mapConnection.prepareStatement("SELECT * FROM " + IndexBinaryMapRenderObject.getTable() + " WHERE id = ?");
			
			writer.startWriteMapIndex(regionName);
			
			for (int i = 0; i < MAP_ZOOMS.length - 1; i++) {
				RTree rtree = mapTree[i];
				long rootIndex = rtree.getFileHdr().getRootIndex();
				rtree.Node root = rtree.getReadNode(rootIndex);
				Rect rootBounds = calcBounds(root);
				if (rootBounds != null) {
					boolean last = nodeIsLastSubTree(rtree, rootIndex);
					writer.startWriteMapLevelIndex(MAP_ZOOMS[MAP_ZOOMS.length - i - 2] + 1, MAP_ZOOMS[MAP_ZOOMS.length - i - 1], rootBounds
							.getMinX(), rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());
					if (last) {
						writer.startMapTreeElement(rootBounds.getMinX(), rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());
					
					}
					writeBinaryMapTree(root, rtree, writer, selectData);
					if (last) {
						writer.endWriteMapTreeElement();
					}

					writer.endWriteMapLevelIndex();
				}
			}
			
			
			writer.endWriteMapIndex();
		} catch (RTreeException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void writeBinaryMapTree(rtree.Node parent, RTree r, BinaryMapIndexWriter writer, PreparedStatement selectData) throws IOException, RTreeException, SQLException {
		Element[] e = parent.getAllElements();
		
		
		for (int i = 0; i < parent.getTotalElements(); i++) {
			Rect re = e[i].getRect();
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				long id = ((LeafElement) e[i]).getPtr();
				selectData.setLong(1, id);
				ResultSet rs = selectData.executeQuery();
				if (rs.next()) {
					 writer.writeMapData(id, rs.getBytes(IndexBinaryMapRenderObject.NODES.ordinal()+ 1), 
							 rs.getBytes(IndexBinaryMapRenderObject.TYPES.ordinal()+ 1), rs.getString(IndexBinaryMapRenderObject.NAME.ordinal()+ 1),  
							 rs.getInt(IndexBinaryMapRenderObject.HIGHWAY.ordinal()+ 1),  rs.getBytes(IndexBinaryMapRenderObject.RESTRICTIONS.ordinal()+ 1));
				} else {
					log.error("Something goes wrong with id = " + id);
				}
			} else {
				long ptr = ((NonLeafElement) e[i]).getPtr();
				rtree.Node ns = r.getReadNode(ptr);

				writer.startMapTreeElement(re.getMinX(), re.getMaxX(), re.getMinY(), re.getMaxY());

				writeBinaryMapTree(ns, r, writer, selectData);
				writer.endWriteMapTreeElement();
			}
		}
	}
	
	private int registerString(Map<String, Integer> stringTable, String s) {
		if (stringTable.containsKey(s)) {
			return stringTable.get(s);
		}
		int size = stringTable.size();
		stringTable.put(s, size);
		return size;
	}
	
	private Map<String, Integer> createStringTableForTransport(){
		Map<String, Integer> stringTable = new LinkedHashMap<String, Integer>();
		registerString(stringTable, "bus");
		registerString(stringTable, "trolleybus");
		registerString(stringTable, "subway");
		registerString(stringTable, "tram");
		registerString(stringTable, "share_taxi");
		registerString(stringTable, "taxi");
		registerString(stringTable, "train");
		registerString(stringTable, "ferry");
		return stringTable;
	}
	
	public void writeBinaryTransportIndex(BinaryMapIndexWriter writer) throws IOException, SQLException {
		try {
			visitedStops = null; // allow gc to collect it
			PreparedStatement selectTransportRouteData = mapConnection.prepareStatement("SELECT * FROM " + IndexTransportRoute.getTable());
			PreparedStatement selectTransportData = mapConnection.prepareStatement("SELECT S.stop, S.direction," +
					"  A.latitude,  A.longitude, A.name, A.name_en " +
					"FROM transport_route_stop S INNER JOIN transport_stop A ON A.id = S.stop WHERE S.route = ? ORDER BY S.ord asc");
			
			writer.startWriteTransportIndex(regionName);
			
			writer.startWriteTransportRoutes();
			
			// expect that memory would be enough
			Map<String, Integer> stringTable = createStringTableForTransport();
			Map<Long, Long> transportRoutes = new LinkedHashMap<Long, Long>();
			
			ResultSet rs = selectTransportRouteData.executeQuery();
			List<TransportStop> directStops = new ArrayList<TransportStop>();
			List<TransportStop> reverseStops = new ArrayList<TransportStop>();
			while(rs.next()){
				
				long idRoute = rs.getLong(IndexTransportRoute.ID.ordinal() + 1);
				int dist = rs.getInt(IndexTransportRoute.DIST.ordinal() + 1);
				String routeName = rs.getString(IndexTransportRoute.NAME.ordinal() + 1);
				String routeEnName = rs.getString(IndexTransportRoute.NAME_EN.ordinal() + 1);
				if(routeEnName != null && routeEnName.equals(Junidecode.unidecode(routeName))){
					routeEnName = null;
				}
				String ref = rs.getString(IndexTransportRoute.REF.ordinal() + 1);
				String operator = rs.getString(IndexTransportRoute.OPERATOR.ordinal() + 1);
				String type = rs.getString(IndexTransportRoute.TYPE.ordinal() + 1);
				
				selectTransportData.setLong(1, idRoute);
				ResultSet rset = selectTransportData.executeQuery();
				reverseStops.clear();
				directStops.clear();
				while (rset.next()) {
					boolean dir = rset.getInt(2) != 0;
					long idStop = rset.getInt(1);
					String stopName = rset.getString(5);
					String stopEnName = rset.getString(6);
					if (stopEnName != null && stopEnName.equals(Junidecode.unidecode(stopName))) {
						stopEnName = null;
					}
					TransportStop st = new TransportStop();
					st.setId(idStop);
					st.setName(stopName);
					st.setLocation(rset.getDouble(3), rset.getDouble(4));
					if (stopEnName != null) {
						st.setEnName(stopEnName);
					}
					if(dir){
						directStops.add(st);
					} else {
						reverseStops.add(st);
					}
				}
				writer.writeTransportRoute(idRoute, routeName, routeEnName, ref, operator, type, dist, directStops, reverseStops, 
						stringTable, transportRoutes);
			}
			rs.close();
			selectTransportRouteData.close();
			writer.endWriteTransportRoutes();
			
			PreparedStatement selectTransportStop = mapConnection.prepareStatement(
					"SELECT A.id,  A.latitude,  A.longitude, A.name, A.name_en FROM transport_stop A where A.id = ?");
			PreparedStatement selectTransportRouteStop = mapConnection.prepareStatement(
					"SELECT DISTINCT S.route FROM transport_route_stop S WHERE S.stop = ? ");
			long rootIndex = transportStopsTree.getFileHdr().getRootIndex();
			rtree.Node root = transportStopsTree.getReadNode(rootIndex);
			Rect rootBounds = calcBounds(root);
			if (rootBounds != null) {
				writer.startTransportTreeElement(rootBounds.getMinX(), rootBounds.getMaxX(), rootBounds.getMinY(), rootBounds.getMaxY());
				writeBinaryTransportTree(root, transportStopsTree, writer, selectTransportStop, selectTransportRouteStop, 
						transportRoutes, stringTable);
				writer.endWriteTransportTreeElement();
			}
			
			writer.writeTransportStringTable(stringTable);

			writer.endWriteTransportIndex();
		} catch (RTreeException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void writeBinaryTransportTree(rtree.Node parent, RTree r, BinaryMapIndexWriter writer, 
			PreparedStatement selectTransportStop, PreparedStatement selectTransportRouteStop, 
			Map<Long, Long> transportRoutes, Map<String, Integer> stringTable) throws IOException, RTreeException, SQLException {
		Element[] e = parent.getAllElements();
		List<Long> routes = null;
		for (int i = 0; i < parent.getTotalElements(); i++) {
			Rect re = e[i].getRect();
			if (e[i].getElementType() == rtree.Node.LEAF_NODE) {
				long id = ((LeafElement) e[i]).getPtr();
				selectTransportStop.setLong(1, id);
				selectTransportRouteStop.setLong(1, id);
				ResultSet rs = selectTransportStop.executeQuery();
				if (rs.next()) {
					int x24 = (int) MapUtils.getTileNumberX(24, rs.getDouble(3));
					int y24 = (int) MapUtils.getTileNumberY(24, rs.getDouble(2));
					String name = rs.getString(4);
					String nameEn = rs.getString(5);
					if(nameEn != null && nameEn.equals(Junidecode.unidecode(name))){
						nameEn = null;
					}
					ResultSet rset = selectTransportRouteStop.executeQuery();
					if(routes == null){
						routes = new ArrayList<Long>();
					} else {
						routes.clear();
					}
					while(rset.next()){
						Long route = transportRoutes.get(rset.getLong(1));
						if(route == null){
							log.error("Something goes wrong with route id = " + rset.getLong(1));
						} else {
							routes.add(route);
						}
					}
					rset.close();
					writer.writeTransportStop(id, x24, y24, name, nameEn, stringTable, routes);
				} else {
					log.error("Something goes wrong with id = " + id);
				}
			} else {
				long ptr = ((NonLeafElement) e[i]).getPtr();
				rtree.Node ns = r.getReadNode(ptr);

				writer.startTransportTreeElement(re.getMinX(), re.getMaxX(), re.getMinY(), re.getMaxY());
				writeBinaryTransportTree(ns, r, writer, selectTransportStop, selectTransportRouteStop, transportRoutes, stringTable);
				writer.endWriteTransportTreeElement();
			}
		}
	}

	
	
	public Rect calcBounds(rtree.Node n) {
		Rect r = null;
		Element[] e = n.getAllElements();
		for (int i = 0; i < n.getTotalElements(); i++) {
			Rect re = e[i].getRect();
			if (r == null) {
				try {
					r = new Rect(re.getMinX(), re.getMinY(), re.getMaxX(), re.getMaxY());
				} catch (IllegalValueException ex) {
				}
			} else {
				r.expandToInclude(re);
			}
		}
		return r;
	}
	
	
	
	private void convertEnglishName(MapObject o){
		String name = o.getName();
		if(name != null && (o.getEnName() == null || o.getEnName().isEmpty())){
			o.setEnName(Junidecode.unidecode(name));
		}
	}
	
	public String getRTreeMapIndexNonPackFileName(){
		return mapFile.getAbsolutePath()+".rtree";
	}
	public String getRTreeTransportStopsFileName(){
		return mapFile.getAbsolutePath()+".trans";
	}
	
	public String getRTreeTransportStopsPackFileName(){
		return mapFile.getAbsolutePath()+".ptrans";
	}
	public String getRTreeMapIndexPackFileName(){
		return mapFile.getAbsolutePath()+".prtree";
	}
	
	
	public void generateIndexes(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException,
			SQLException {
		
		// clear previous results and setting variables
		if (readFile != null && regionName == null) {
			int i = readFile.getName().indexOf('.');
			if(i > -1){
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(readFile.getName().substring(0, i));
			}
		}
		
		cities.clear();
		cityManager.clear();
		postalCodeRelations.clear();
		if (normalizeStreets) {
			normalizeDefaultSuffixes = DataExtractionSettings.getSettings().getDefaultSuffixesToNormalizeStreets();
			normalizeSuffixes = DataExtractionSettings.getSettings().getSuffixesToNormalizeStreets();
		}
		
		// Main generation method
		try {
			//////////////////////////////////////////////////////////////////////////
			// 1. creating nodes db to fast access for all nodes and simply import all relations, ways, nodes to it
			boolean loadFromPath = dbFile == null || !databaseFileExists(dbFile);
			if (dbFile == null) {
				dbFile = new File(workingDir, TEMP_NODES_DB);
				// to save space
				if (databaseFileExists(dbFile)) {
					removeDatabase(dbFile);
				}
			}
			dbConn = getDatabaseConnection(dbFile.getAbsolutePath());
			
			int allRelations = 100000;
			int allWays = 1000000;
			int allNodes = 10000000;
			if (loadFromPath){
				progress.setGeneralProgress("[35 of 100]");
				progress.startTask("Loading file " + readFile.getAbsolutePath(), -1);
				
				NewDataExtractionOsmFilter filter = extractOsmToNodesDB(readFile, progress, addFilter);
				if(filter != null){
					allNodes = filter.getAllNodes();
					allWays = filter.getAllWays();
					allRelations = filter.getAllRelations();
				}
			}
			
			pselectNode = dbConn.prepareStatement("select n.latitude, n.longitude, t.skeys, t.value from node n left join tags t on n.id = t.id and t.type = 0 where n.id = ?");
			pselectWay = dbConn.prepareStatement("select w.node, w.ord, t.skeys, t.value, n.latitude, n.longitude " +
					"from ways w left join tags t on w.id = t.id and t.type = 1 and w.ord = 0 inner join node n on w.node = n.id " +
					"where w.id = ? order by w.ord");
			pselectRelation = dbConn.prepareStatement("select r.member, r.type, r.role, r.ord, t.skeys, t.value " +
					"from relations r left join tags t on r.id = t.id and t.type = 2 and r.ord = 0 " +
					"where r.id = ? order by r.ord");
			pselectTags = dbConn.prepareStatement("select skeys, value from tags where id = ? and type = ?");
			
			// do not create temp map file and rtree files
			if (recreateOnlyBinaryFile) {
				mapFile = new File(workingDir, getMapFileName());
				File tempDBMapFile = new File(workingDir, getTempMapDBFileName());
				mapConnection = getDatabaseConnection(tempDBMapFile.getAbsolutePath());
				mapConnection.setAutoCommit(false);
				try {
					if (indexMap) {
						mapTree = new RTree[MAP_ZOOMS.length - 1];
						for (int i = 0; i < MAP_ZOOMS.length - 1; i++) {
							mapTree[i] = new RTree(getRTreeMapIndexPackFileName() + i);
						}

					}
					if (indexTransport) {
						transportStopsTree = new RTree(getRTreeTransportStopsPackFileName());
					}
				} catch (RTreeException e) {
					log.error("Error flushing", e);
					throw new IOException(e);
				}
			} else {

				// 2. Create index connections and index structure
				createDatabaseIndexesStructure();

				// 3. Processing all entries
				// 3.1 write all cities

				if (indexAddress) {
					progress.setGeneralProgress("[40 of 100]");
					progress.startTask("Indexing cities...", allNodes);
					if (!loadFromPath) {
						allNodes = iterateOverEntities(progress, EntityType.NODE, allNodes, STEP_CITY_NODES);
					}

					for (City c : cities.values()) {
						DataIndexWriter.writeCity(addressCityStat, pStatements, c, BATCH_SIZE);
					}
					// commit to put all cities
					if (pStatements.get(addressCityStat) > 0) {
						addressCityStat.executeBatch();
						pStatements.put(addressCityStat, 0);
						mapConnection.commit();
					}

				}

				// 3.2 index address relations
				if (indexAddress || indexMap) {
					progress.setGeneralProgress("[40 of 100]");
					progress.startTask("Preindexing address and map ways...", allRelations);
					allRelations = iterateOverEntities(progress, EntityType.RELATION, allRelations,
							STEP_ADDRESS_RELATIONS_AND_MULTYPOLYGONS);
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
						mapConnection.commit();
					}
				}

				// 3.3 MAIN iterate over all entities
				if (indexPOI || indexAddress || indexMap) {
					progress.setGeneralProgress("[50 of 100]");
					progress.startTask("Processing osm nodes...", allNodes);
					iterateOverEntities(progress, EntityType.NODE, allNodes, STEP_MAIN);
					progress.setGeneralProgress("[70 of 100]");
					progress.startTask("Processing osm ways...", allWays);
					iterateOverEntities(progress, EntityType.WAY, allWays, STEP_MAIN);
				}
				progress.setGeneralProgress("[85 of 100]");
				progress.startTask("Processing osm relations...", allRelations);
				iterateOverEntities(progress, EntityType.RELATION, allRelations, STEP_MAIN);

				// 3.4 update all postal codes from relations
				if (indexAddress && !postalCodeRelations.isEmpty()) {
					progress.setGeneralProgress("[90 of 100]");
					progress.startTask("Registering postcodes...", -1);
					if (pStatements.get(addressBuildingStat) > 0) {
						addressBuildingStat.executeBatch();
						pStatements.put(addressBuildingStat, 0);
						mapConnection.commit();
					}
					processingPostcodes();
				}

				// 4. packing map rtree indexes
				if (indexMap) {
					progress.setGeneralProgress("[90 of 100]");
					progress.startTask("Serializing map data...", -1);
					for (int i = 0; i < MAP_ZOOMS.length - 1; i++) {
						mapTree[i] = packRtreeFile(mapTree[i], getRTreeMapIndexNonPackFileName() + i, getRTreeMapIndexPackFileName() + i);
					}
					log.info("Finish packing RTree files");
				}
				
				if(indexTransport){
					progress.setGeneralProgress("[90 of 100]");
					progress.startTask("Serializing transport data...", -1);
					transportStopsTree = packRtreeFile(transportStopsTree, getRTreeTransportStopsFileName(), getRTreeTransportStopsPackFileName());
				}
			}
			
			// 5. Writing binary file
			if(indexMap || indexAddress || indexTransport){
				if(mapFile.exists()){
					mapFile.delete();
				}
				mapRAFile = new RandomAccessFile(mapFile, "rw");
				BinaryMapIndexWriter writer = new BinaryMapIndexWriter(mapRAFile);
				if(indexMap){
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing map index to binary file...", -1);
					closePreparedStatements(mapBinaryStat);
					mapConnection.commit();
					writeBinaryMapIndex(writer);
				}
				
				if(indexAddress){
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing address index to binary file...", -1);
					closePreparedStatements(addressCityStat, addressStreetStat, addressStreetNodeStat, addressBuildingStat);
					mapConnection.commit();
					writeBinaryAddressIndex(writer, progress);
				}
				if(indexTransport){
					progress.setGeneralProgress("[95 of 100]");
					progress.startTask("Writing transport index to binary file...", -1);
					closePreparedStatements(transRouteStat, transRouteStopsStat, transStopsStat);
					mapConnection.commit();
					writeBinaryTransportIndex(writer);
				}
				progress.finishTask();
				writer.close();
				log.info("Finish writing binary file");
			}
		} finally {
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
					poiConnection = null;
					if (lastModifiedDate != null) {
						poiIndexFile.setLastModified(lastModifiedDate);
					}
				}
				
				if (mapConnection != null) {
					mapConnection.commit();
					mapConnection.close();
					mapConnection = null;
					File tempDBFile = new File(workingDir, getTempMapDBFileName());
					if(databaseFileExists(tempDBFile) && deleteDatabaseIndexes){
						// do not delete it for now
						removeDatabase(tempDBFile);
					}
				}
				
				// delete map rtree files
				if (mapTree != null) {
					for (int i = 0; i < mapTree.length; i++) {
						if (mapTree[i] != null) {
							RandomAccessFile file = mapTree[i].getFileHdr().getFile();
							file.close();
						}

					}
					for (int i = 0; i < mapTree.length; i++) {
						File f = new File(getRTreeMapIndexNonPackFileName() + i);
						if (f.exists() && deleteDatabaseIndexes) {
							f.delete();
						}
						f = new File(getRTreeMapIndexPackFileName() + i);
						if (f.exists() && deleteDatabaseIndexes) {
							f.delete();
						}
					}
				}
				
				// delete transport rtree files
				if(transportStopsTree != null){
					transportStopsTree.getFileHdr().getFile().close();
					File f = new File(getRTreeTransportStopsFileName());
					if (f.exists() && deleteDatabaseIndexes) {
						f.delete();
					}
					f = new File(getRTreeTransportStopsPackFileName());
					if (f.exists() && deleteDatabaseIndexes) {
						f.delete();
					}
				}
				
				// do not delete first db connection
				if(dbConn != null){
					if(usingH2()){
						dbConn.createStatement().execute("SHUTDOWN COMPACT");
					}
					dbConn.close();
				}
				if(deleteOsmDB){
					if (usingDerby()) {
						try {
							DriverManager.getConnection("jdbc:derby:;shutdown=true");
						} catch (SQLException e) {
							// ignore exception
						}
					}
					removeDatabase(dbFile);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}


	private void processingPostcodes() throws SQLException {
		PreparedStatement pstat = mapConnection.prepareStatement("UPDATE " + IndexBuildingTable.getTable() + " SET "
				+ IndexBuildingTable.POSTCODE.name() + " = ? WHERE " + IndexBuildingTable.ID.name() + " = ?");
		pStatements.put(pstat, 0);
		for (Relation r : postalCodeRelations) {
			String tag = r.getTag(OSMTagKey.POSTAL_CODE);
			for (EntityId l : r.getMemberIds()) {
				pstat.setString(1, tag);
				pstat.setLong(2, l.getId());
				DataIndexWriter.addBatch(pStatements, pstat, BATCH_SIZE);
			}
		}
		if(pStatements.get(pstat) > 0){
			pstat.executeBatch();
		}
		pStatements.remove(pstat);
	}

	private RTree packRtreeFile(RTree tree, String nonPackFileName, String packFileName) throws IOException {
		try {
			assert rtree.Node.MAX < 50 : "It is better for search performance";
			tree.flush();
			File file = new File(packFileName);
			if (file.exists()) {
				file.delete();
			}
			long rootIndex = tree.getFileHdr().getRootIndex();
			if (!nodeIsLastSubTree(tree, rootIndex)) {
				// there is a bug for small files in packing method
				new Pack().packTree(tree, packFileName);
				tree.getFileHdr().getFile().close();
				file = new File(nonPackFileName);
				file.delete();

				return new RTree(packFileName);
			}
		} catch (RTreeException e) {
			log.error("Error flushing", e);
			throw new IOException(e);
		}
		return tree;
	}


	private NewDataExtractionOsmFilter extractOsmToNodesDB(File readFile, IProgress progress, IOsmStorageFilter addFilter) throws FileNotFoundException,
			IOException, SQLException, SAXException {
		boolean pbfFile = false;
		InputStream stream = new FileInputStream(readFile);
		InputStream streamFile = stream;
		long st = System.currentTimeMillis();
		if (readFile.getName().endsWith(".bz2")) {
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
			} else {
				stream = new CBZip2InputStream(stream);
			}
		} else if (readFile.getName().endsWith(".pbf")) {
			pbfFile = true;
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
			filter.initDatabase();
			storage.getFilters().add(filter);
			if (pbfFile) {
				storage.parseOSMPbf(stream, progress, false);
			} else {
				storage.parseOSM(stream, progress, streamFile, false);
			}
			filter.finishLoading();
			dbConn.commit();

			if (log.isInfoEnabled()) {
				log.info("File parsed : " + (System.currentTimeMillis() - st));
			}
			progress.finishTask();
			return filter;
		} finally {
			if (log.isInfoEnabled()) {
				log.info("File indexed : " + (System.currentTimeMillis() - st));
			}
		}
	}


	private void createDatabaseIndexesStructure() throws SQLException, IOException {
		// 2.1 create temporary sqlite database to put temporary results to it
		if (indexMap || indexAddress || indexTransport) {
			mapFile = new File(workingDir, getMapFileName());
			// to save space
			mapFile.getParentFile().mkdirs();
			File tempDBMapFile = new File(workingDir, getTempMapDBFileName());
			removeDatabase(tempDBMapFile);
			mapConnection = getDatabaseConnection(tempDBMapFile.getAbsolutePath());
			mapConnection.setAutoCommit(false);
		}
		
		// 2.2 create rtree map
		if(indexMap){
			DataIndexWriter.createMapIndexStructure(mapConnection);
			mapBinaryStat = DataIndexWriter.createStatementMapBinaryInsert(mapConnection);
			try {
				mapTree = new RTree[MAP_ZOOMS.length - 1];
				for (int i = 0; i < MAP_ZOOMS.length - 1; i++) {
					File file = new File(getRTreeMapIndexNonPackFileName() + i);
					if (file.exists()) {
						file.delete();
					}
					mapTree[i] = new RTree(getRTreeMapIndexNonPackFileName() + i);
					// very slow
					//mapTree[i].getFileHdr().setBufferPolicy(true);
				}
			} catch (RTreeException e) {
			   throw new IOException(e);
			}
			pStatements.put(mapBinaryStat, 0);
		}
		

		if (indexAddress) {
			DataIndexWriter.createAddressIndexStructure(mapConnection);
			addressCityStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexCityTable
					.getTable(), IndexCityTable.values().length));
			addressStreetStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexStreetTable
					.getTable(), IndexStreetTable.values().length));
			addressSearchStreetStat = mapConnection.prepareStatement("SELECT " + IndexStreetTable.ID.name() + " FROM "
					+ IndexStreetTable.getTable() + " WHERE ? = " + IndexStreetTable.CITY.name() + " AND ? ="
					+ IndexStreetTable.NAME.name());
			addressSearchBuildingStat = mapConnection.prepareStatement("SELECT " + IndexBuildingTable.ID.name() + " FROM "
					+ IndexBuildingTable.getTable() + " WHERE ? = " + IndexBuildingTable.ID.name());
			addressSearchStreetNodeStat = mapConnection.prepareStatement("SELECT " + IndexStreetNodeTable.WAY.name() + " FROM "
					+ IndexStreetNodeTable.getTable() + " WHERE ? = " + IndexStreetNodeTable.WAY.name());
			addressBuildingStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexBuildingTable
					.getTable(), IndexBuildingTable.values().length));
			addressStreetNodeStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(
					IndexStreetNodeTable.getTable(), IndexStreetNodeTable.values().length));
			pStatements.put(addressCityStat, 0);
			pStatements.put(addressStreetStat, 0);
			pStatements.put(addressStreetNodeStat, 0);
			pStatements.put(addressBuildingStat, 0);
			// put search statements to close them after all
			pStatements.put(addressSearchBuildingStat, 0);
			pStatements.put(addressSearchStreetNodeStat, 0);
			pStatements.put(addressSearchStreetStat, 0);

		}

		if (indexPOI) {
			poiIndexFile = new File(workingDir, getPoiFileName());
			// to save space
			if (poiIndexFile.exists()) {
				Algoritms.removeAllFiles(poiIndexFile);
			}
			poiIndexFile.getParentFile().mkdirs();
			// creating nodes db to fast access for all nodes
			poiConnection = getDatabaseConnection(poiIndexFile.getAbsolutePath(), true);
			DataIndexWriter.createPoiIndexStructure(poiConnection);
			poiPreparedStatement = DataIndexWriter.createStatementAmenityInsert(poiConnection);
			pStatements.put(poiPreparedStatement, 0);
			poiConnection.setAutoCommit(false);
		}

		if (indexTransport) {
			DataIndexWriter.createTransportIndexStructure(mapConnection);
			try {
				File file = new File(getRTreeTransportStopsFileName());
				if (file.exists()) {
					file.delete();
				}
				transportStopsTree = new RTree(file.getAbsolutePath());
			} catch (RTreeException e) {
			   throw new IOException(e);
			}
			transRouteStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexTransportRoute
					.getTable(), IndexTransportRoute.values().length));
			transRouteStopsStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(
					IndexTransportRouteStop.getTable(), IndexTransportRouteStop.values().length));
			transStopsStat = mapConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexTransportStop
					.getTable(), IndexTransportStop.values().length));
			pStatements.put(transRouteStat, 0);
			pStatements.put(transRouteStopsStat, 0);
			pStatements.put(transStopsStat, 0);
			mapConnection.setAutoCommit(false);

		}
	}
	
	protected void closePreparedStatements(PreparedStatement... preparedStatements) throws SQLException{
		for(PreparedStatement p : preparedStatements){
			if (p != null) {
				p.executeBatch();
				p.close();
				pStatements.remove(p);
			}
		}
	}
	
	
	public static void removeWayNodes(File sqlitedb) throws SQLException{
		Connection dbConn = DriverManager.getConnection("jdbc:sqlite:" + sqlitedb.getAbsolutePath());
		dbConn.setAutoCommit(false);
		Statement st = dbConn.createStatement();
		st.execute("DELETE FROM " + IndexStreetNodeTable.getTable() + " WHERE 1=1");
		st.close();
		dbConn.commit();
		st = dbConn.createStatement();
		st.execute("VACUUM");
		st.close();
		dbConn.close();
	}
	
	 public static void main(String[] args) throws IOException, SAXException, SQLException {
		 long time = System.currentTimeMillis();
		 IndexCreator creator = new IndexCreator(new File("e:/Information/OSM maps/osmand/"));
		 creator.setIndexMap(true);
		 creator.setIndexAddress(true);
		 creator.setSaveAddressWays(true);
		 creator.setNormalizeStreets(true);
//		 creator.setIndexPOI(true);
		 creator.setIndexTransport(true);
		 
		 creator.recreateOnlyBinaryFile = false;
		 creator.deleteDatabaseIndexes = false;
		 
		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/minsk.tmp.odb"));
		 creator.generateIndexes(new File("e:/Information/OSM maps/belarus osm/minsk.osm"), new ConsoleProgressImplementation(3), null);

//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/belarus_nodes.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/belarus osm/belarus.osm.bz2"), new ConsoleProgressImplementation(3), null);
		 
		 
//		 creator.generateIndexes(new File("e:/Information/OSM maps/belarus osm/forest.osm"), new ConsoleProgressImplementation(3), null);
		 

//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/ams.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/ams_part_map.osm"), new ConsoleProgressImplementation(3), null);
		 
//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/den_haag.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/den_haag.osm"), new ConsoleProgressImplementation(3), null);
		 
//		 creator.setNodesDBFile(new File("e:/Information/OSM maps/osmand/netherlands.tmp.odb"));
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/netherlands.osm.bz2"), new ConsoleProgressImplementation(1), null);
		 
//		 creator.generateIndexes(new File("e:/Information/OSM maps/osm_map/forest_complex.osm"), new ConsoleProgressImplementation(25), null);

		 System.out.println("WHOLE GENERATION TIME :  " + (System.currentTimeMillis() - time));
		 System.out.println("COORDINATES_SIZE " + BinaryMapIndexWriter.COORDINATES_SIZE + " count " + BinaryMapIndexWriter.COORDINATES_COUNT);
		 System.out.println("TYPES_SIZE " + BinaryMapIndexWriter.TYPES_SIZE);
		 System.out.println("ID_SIZE " + BinaryMapIndexWriter.ID_SIZE);
		 System.out.println("- COORD_TYPES_ID SIZE " + (BinaryMapIndexWriter.COORDINATES_SIZE + BinaryMapIndexWriter.TYPES_SIZE + BinaryMapIndexWriter.ID_SIZE));
		 System.out.println("- MAP_DATA_SIZE " + BinaryMapIndexWriter.MAP_DATA_SIZE);
		 System.out.println("- STRING_TABLE_SIZE " + BinaryMapIndexWriter.STRING_TABLE_SIZE);
		 System.out.println("-- MAP_DATA_AND_STRINGS SIZE " + (BinaryMapIndexWriter.MAP_DATA_SIZE + BinaryMapIndexWriter.STRING_TABLE_SIZE));
		 
	}
}