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
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

import com.osmand.Algoritms;
import com.osmand.IProgress;
import com.osmand.data.Amenity;
import com.osmand.data.TransportRoute;
import com.osmand.data.TransportStop;
import com.osmand.data.index.DataIndexWriter;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.index.IndexConstants.IndexTransportRoute;
import com.osmand.data.index.IndexConstants.IndexTransportRouteStop;
import com.osmand.data.index.IndexConstants.IndexTransportStop;
import com.osmand.impl.ConsoleProgressImplementation;
import com.osmand.osm.Entity;
import com.osmand.osm.Node;
import com.osmand.osm.Relation;
import com.osmand.osm.Way;
import com.osmand.osm.Entity.EntityId;
import com.osmand.osm.Entity.EntityType;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.IOsmStorageFilter;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.swing.DataExtractionSettings;

/**
 * That data extraction has aim, 
 * save runtime memory and generate indexes on the fly.
 * It will be longer than load in memory (needed part) and save into index.
 *  
 *
 */
public class IndexCreator {
	private static final Log log = LogFactory.getLog(DataExtraction.class);

	public static final int BATCH_SIZE = 5000;
	public static final String TEMP_NODES_DB = "nodes"+IndexConstants.MAP_INDEX_EXT;
	
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

			prepNode = dbConn.prepareStatement("insert into node values (?, ?, ?);");
			prepWays = dbConn.prepareStatement("insert into ways values (?, ?);");
			prepRelations = dbConn.prepareStatement("insert into relations values (?, ?, ?, ?);");
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
					for (Long i : ((Way) e).getNodeIds()) {
						currentWaysCount++;
						prepWays.setLong(1, e.getId());
						prepWays.setLong(2, i);
						prepWays.addBatch();
					}
					if (currentWaysCount >= BATCH_SIZE) {
						prepWays.executeBatch();
						currentWaysCount = 0;
					}
				} else {
					allRelations++;
					for (Entry<EntityId, String> i : ((Relation) e).getMembersMap().entrySet()) {
						currentRelationsCount++;
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
	
	public void generateIndexes(String path, IProgress progress, IOsmStorageFilter addFilter) throws IOException, SAXException,
			SQLException {
		File f = new File(path);
		InputStream stream = new FileInputStream(f);
		int i = f.getName().indexOf('.');
		if (regionName == null) {
			regionName = Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i));
		}

		InputStream streamFile = stream;
		long st = System.currentTimeMillis();
		if (path.endsWith(".bz2")) {
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
			} else {
				stream = new CBZip2InputStream(stream);
			}
		}

		if (progress != null) {
			progress.startTask("Loading file " + path, -1);
		}
		OsmBaseStorage storage = new OsmBaseStorage();
		storage.setSupressWarnings(DataExtractionSettings.getSettings().isSupressWarningsForDuplicatedId());
		if (addFilter != null) {
			storage.getFilters().add(addFilter);
		}

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e);
			throw new IllegalStateException(e);
		}
		if (indexMap) {
			dbFile = new File(workingDir, getMapFileName());
		} else {
			dbFile = new File(workingDir, TEMP_NODES_DB);
		}
		// to save space
		if (dbFile.exists()) {
			dbFile.delete();
		}
		// creating nodes db to fast access for all nodes
		dbConn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

		// 1. Loading osm file
		NewDataExtractionOsmFilter filter = new NewDataExtractionOsmFilter();
		try {
			// 1 init database to store temporary data
			progress.setGeneralProgress("[40 of 100]");

			filter.initDatabase();
			storage.getFilters().add(filter);
			storage.parseOSM(stream, progress, streamFile, false);
			filter.finishLoading();

			if (log.isInfoEnabled()) {
				log.info("File parsed : " + (System.currentTimeMillis() - st));
			}
			progress.finishTask();

		} finally {
			if (log.isInfoEnabled()) {
				log.info("File indexed : " + (System.currentTimeMillis() - st));
			}
		}

		// 2. Processing all entries
		progress.setGeneralProgress("[90 of 100]");

		pselectNode = dbConn.prepareStatement("select * from node where id = ?");
		pselectWay = dbConn.prepareStatement("select * from ways where id = ?");
		pselectRelation = dbConn.prepareStatement("select * from relations where id = ?");
		pselectTags = dbConn.prepareStatement("select key, value from tags where id = ? and type = ?");

		if (indexPOI) {
			poiIndexFile = new File(workingDir, getPoiFileName());
			// to save space
			if (poiIndexFile.exists()) {
				poiIndexFile.delete();
			}
			poiIndexFile.getParentFile().mkdirs();
			// creating nodes db to fast access for all nodes
			poiConnection = DriverManager.getConnection("jdbc:sqlite:" + poiIndexFile.getAbsolutePath());
			poiConnection.setAutoCommit(false);
			DataIndexWriter.createPoiIndexStructure(poiConnection);
			poiPreparedStatement = DataIndexWriter.createStatementAmenityInsert(poiConnection);
			pStatements.put(poiPreparedStatement, 0);
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
			transRouteStat = transportConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(
					IndexTransportRoute.getTable(), IndexTransportRoute.values().length));
			transRouteStopsStat = transportConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(
					IndexTransportRouteStop.getTable(), IndexTransportRouteStop.values().length));
			transStopsStat = transportConnection.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexTransportStop
					.getTable(), IndexTransportStop.values().length));
			pStatements.put(transRouteStat, 0);
			pStatements.put(transRouteStopsStat, 0);
			pStatements.put(transStopsStat, 0);
			transportConnection.setAutoCommit(false);

		}

		iterateOverAllEntities(progress, filter);

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
					p.close();
				}
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
			
			dbConn.close();
		} catch (SQLException e) {
		}
	}
	
	public void loadEntityData(Entity e, boolean loadTags) throws SQLException {
		if(e instanceof Node){
			return;
		}
		Map<EntityId, Entity> map = new LinkedHashMap<EntityId, Entity>();
		if(e instanceof Relation){
			pselectRelation.setLong(1, e.getId());
			if (pselectRelation.execute()) {
				ResultSet rs = pselectRelation.getResultSet();
				while (rs.next()) {
					((Relation) e).addMember(rs.getLong(2), EntityType.values()[rs.getByte(3)], rs.getString(4));
				}
				rs.close();
			}
		} else if(e instanceof Way) {
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
					loadEntityData(way, false);
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
		if(loadTags){
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
	
	public void iterateOverAllEntities(IProgress progress, NewDataExtractionOsmFilter f) throws SQLException{
		iterateOverEntities(progress, EntityType.NODE, f.getAllNodes());
		iterateOverEntities(progress, EntityType.WAY, f.getAllWays());
		iterateOverEntities(progress, EntityType.RELATION, f.getAllRelations());
	}
	
	public void iterateOverEntities(IProgress progress, EntityType type, int allCount) throws SQLException{
		Statement statement = dbConn.createStatement();
		String select;
		String info;
		
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
			iterateEntity(e);
		}
		rs.close();
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

	private void iterateEntity(Entity e) throws SQLException {
		if (indexPOI && Amenity.isAmenity(e)) {
			loadEntityData(e, false);
			if(poiPreparedStatement != null){
				Amenity a = new Amenity(e);
				if(a.getLocation() != null){
					DataIndexWriter.insertAmenityIntoPoi(poiPreparedStatement, pStatements, a, BATCH_SIZE);
				}
			}
		}
		if(indexTransport){
			if(e instanceof Relation && e.getTag(OSMTagKey.ROUTE) != null){
				loadEntityData(e, true);
				TransportRoute route = indexTransportRoute((Relation) e);
				if(route != null){
					DataIndexWriter.insertTransportIntoIndex(transRouteStat, transRouteStopsStat, transStopsStat, visitedStops, route, pStatements,
							BATCH_SIZE);
				}
			}
		}
		
/*		if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
			places.add((Node) e);
			processed = true;
		}
		if (indexAddress) {
			// index not only buildings but also addr:interpolation ways
//			if ("yes".equals(e.getTag(OSMTagKey.BUILDING))) {
				if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
					buildings.put(entityId, e);
					processed = true;
				}
//			}
			// suppose that streets are way for car
			if (e instanceof Way && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY))
					&& e.getTag(OSMTagKey.HIGHWAY) != null
					&& e.getTag(OSMTagKey.NAME) != null) {
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
		} */
		
	}
	

	// TODO transliteration !!!
	public static void main(String[] args) throws IOException, SAXException, SQLException {
		IndexCreator extr = new IndexCreator(new File("e:/Information/OSM maps/osmand/"));
		extr.setIndexPOI(true);
		extr.setIndexTransport(true);
		extr.generateIndexes("e:/Information/OSM maps/belarus osm/minsk.osm", new ConsoleProgressImplementation(4), null);
		
	}
}
