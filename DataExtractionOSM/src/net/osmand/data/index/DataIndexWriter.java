package net.osmand.data.index;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.Region;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.data.City.CityType;
import net.osmand.data.index.IndexConstants.IndexBuildingTable;
import net.osmand.data.index.IndexConstants.IndexCityTable;
import net.osmand.data.index.IndexConstants.IndexMapRenderObject;
import net.osmand.data.index.IndexConstants.IndexPoiTable;
import net.osmand.data.index.IndexConstants.IndexStreetNodeTable;
import net.osmand.data.index.IndexConstants.IndexStreetTable;
import net.osmand.data.index.IndexConstants.IndexTransportRoute;
import net.osmand.data.index.IndexConstants.IndexTransportRouteStop;
import net.osmand.data.index.IndexConstants.IndexTransportStop;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;



public class DataIndexWriter {
	
	
	private final File workingDir;
	private final Region region;
	private static final Log log = LogUtil.getLog(DataIndexWriter.class);
	
	
	private static final int BATCH_SIZE = 1000;

	public DataIndexWriter(File workingDir, Region region){
		this.workingDir = workingDir;
		this.region = region;
	}
	
	protected File checkFile(String name) throws IOException {
		String fileName = name;
		File f = new File(workingDir, fileName);
		f.getParentFile().mkdirs();
		// remove existing file
		if (f.exists()) {
			log.warn("Remove existing index : " + f.getAbsolutePath()); //$NON-NLS-1$
			f.delete();
		}
		return f;	
	}
	
	public DataIndexWriter writePOI() throws IOException, SQLException {
		return writePOI(IndexConstants.POI_INDEX_DIR+region.getName()+IndexConstants.POI_INDEX_EXT, null);
	}
	
	
	public DataIndexWriter writePOI(String fileName, Long date) throws IOException, SQLException { 
		File file = checkFile(fileName);
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e); //$NON-NLS-1$
			throw new IllegalStateException(e);
		}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath()); //$NON-NLS-1$
		try {
			createPoiIndexStructure(conn);
			PreparedStatement prep = createStatementAmenityInsert(conn);
			Map<PreparedStatement, Integer> map = new LinkedHashMap<PreparedStatement, Integer>();
			map.put(prep, 0);
	        conn.setAutoCommit(false);
			for (Amenity a : region.getAmenityManager().getAllObjects()) {
				insertAmenityIntoPoi(prep, map, a, BATCH_SIZE);
			}
			if(map.get(prep) > 0){
				prep.executeBatch();
			}
			prep.close();
			conn.setAutoCommit(true);
		} finally {
			conn.close();
			log.info(String.format("Indexing poi done in %s ms.", System.currentTimeMillis() - now)); //$NON-NLS-1$
		}
		if(date != null){
			file.setLastModified(date);
		}
		return this;
	}

	public static void insertAmenityIntoPoi(PreparedStatement prep, Map<PreparedStatement, Integer> map, Amenity amenity, int batchSize) throws SQLException {
		prep.setLong(IndexPoiTable.ID.ordinal() + 1, amenity.getId());
		prep.setDouble(IndexPoiTable.LATITUDE.ordinal() + 1, amenity.getLocation().getLatitude());
		prep.setDouble(IndexPoiTable.LONGITUDE.ordinal() + 1, amenity.getLocation().getLongitude());
		prep.setString(IndexPoiTable.NAME_EN.ordinal() + 1, amenity.getEnName());
		prep.setString(IndexPoiTable.NAME.ordinal() + 1, amenity.getName());
		prep.setString(IndexPoiTable.TYPE.ordinal() + 1, AmenityType.valueToString(amenity.getType()));
		prep.setString(IndexPoiTable.SUBTYPE.ordinal() + 1, amenity.getSubType());
		prep.setString(IndexPoiTable.OPENING_HOURS.ordinal() + 1 , amenity.getOpeningHours());
		addBatch(map, prep, batchSize);
	}
	
	public static PreparedStatement createStatementAmenityInsert(Connection conn) throws SQLException{
		assert IndexPoiTable.values().length == 8;
        return conn.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexPoiTable.getTable(), 8));
	}
	
	
	public static void createPoiIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexPoiTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexPoiTable.values()));
        stat.execute("PRAGMA user_version = " + IndexConstants.POI_TABLE_VERSION); //$NON-NLS-1$
        stat.close();
	}
	
	public DataIndexWriter writeAddress() throws IOException, SQLException{
		return writeAddress(IndexConstants.ADDRESS_INDEX_DIR+region.getName()+IndexConstants.ADDRESS_INDEX_EXT, null, true);
	}
	
	public DataIndexWriter writeAddress(String fileName, Long date, boolean writeWayNodes) throws IOException, SQLException{
		File file = checkFile(fileName);
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e); //$NON-NLS-1$
			throw new IllegalStateException(e);
		}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath()); //$NON-NLS-1$
		try {
			createAddressIndexStructure(conn);
	        
	        PreparedStatement prepCity = conn.prepareStatement(
	            IndexConstants.generatePrepareStatementToInsert(IndexCityTable.getTable(), IndexCityTable.values().length));
	        PreparedStatement prepStreet = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexStreetTable.getTable(), IndexStreetTable.values().length));
	        PreparedStatement prepBuilding = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexBuildingTable.getTable(), IndexBuildingTable.values().length));
	        PreparedStatement prepStreetNode = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexStreetNodeTable.getTable(), IndexStreetNodeTable.values().length));
	        Map<PreparedStatement, Integer> count = new HashMap<PreparedStatement, Integer>();
	        count.put(prepStreet, 0);
	        count.put(prepCity, 0);
	        count.put(prepStreetNode, 0);
	        count.put(prepBuilding, 0);
	        conn.setAutoCommit(false);
	        
	        for(CityType t : CityType.values()){
	        	for(City city : region.getCitiesByType(t)) {
	        		if(city.getId() == null || city.getLocation() == null){
						continue;
					}
	        		writeCity(prepCity, count, city, BATCH_SIZE);
					
					for(Street street : city.getStreets()){
						if(street.getId() == null || street.getLocation() == null){
							continue;
						}
						assert IndexStreetTable.values().length == 6;
						prepStreet.setLong(IndexStreetTable.ID.ordinal() + 1, street.getId());
						prepStreet.setString(IndexStreetTable.NAME_EN.ordinal() + 1, street.getEnName());
						prepStreet.setDouble(IndexStreetTable.LATITUDE.ordinal() + 1, street.getLocation().getLatitude());
						prepStreet.setDouble(IndexStreetTable.LONGITUDE.ordinal() + 1, street.getLocation().getLongitude());
						prepStreet.setString(IndexStreetTable.NAME.ordinal() + 1, street.getName());
						prepStreet.setLong(IndexStreetTable.CITY.ordinal() + 1, city.getId());
						addBatch(count, prepStreet);
						if (writeWayNodes) {
							for (Way way : street.getWayNodes()) {
								writeStreetWayNodes(prepStreetNode, count, street.getId(), way, BATCH_SIZE);
							}
						}
						
						for(Building building : street.getBuildings()){
							if(building.getId() == null || building.getLocation() == null){
								continue;
							}
							writeBuilding(prepBuilding, count, street.getId(), building, BATCH_SIZE);
						}
					}
	        		
	        	}
	        }
			
			for(PreparedStatement p : count.keySet()){
				if(count.get(p) > 0){
					p.executeBatch();
				}
				p.close();
			}
			conn.setAutoCommit(true);
		} finally {
			conn.close();
			log.info(String.format("Indexing address done in %s ms.", System.currentTimeMillis() - now)); //$NON-NLS-1$
		}
		if(date != null){
			file.setLastModified(date);
		}
		return this;
	}

	public static void writeStreetWayNodes(PreparedStatement prepStreetNode, Map<PreparedStatement, Integer> count, Long streetId, Way way, int batchSize)
			throws SQLException {
		for (Node n : way.getNodes()) {
			if (n == null) {
				continue;
			}
			assert IndexStreetNodeTable.values().length == 5;
			prepStreetNode.setLong(IndexStreetNodeTable.ID.ordinal() + 1, n.getId());
			prepStreetNode.setDouble(IndexStreetNodeTable.LATITUDE.ordinal() + 1, n.getLatitude());
			prepStreetNode.setDouble(IndexStreetNodeTable.LONGITUDE.ordinal() + 1, n.getLongitude());
			prepStreetNode.setLong(IndexStreetNodeTable.WAY.ordinal() + 1, way.getId());
			prepStreetNode.setLong(IndexStreetNodeTable.STREET.ordinal() + 1, streetId);
			addBatch(count, prepStreetNode, BATCH_SIZE);
		}
	}

	public static void writeBuilding(PreparedStatement prepBuilding, Map<PreparedStatement, Integer> count, Long streetId, 
			Building building, int batchSize)
			throws SQLException {
		assert IndexBuildingTable.values().length == 7;
		prepBuilding.setLong(IndexBuildingTable.ID.ordinal() + 1, building.getId());
		prepBuilding.setDouble(IndexBuildingTable.LATITUDE.ordinal() + 1, building.getLocation().getLatitude());
		prepBuilding.setDouble(IndexBuildingTable.LONGITUDE.ordinal() + 1, building.getLocation().getLongitude());
		prepBuilding.setString(IndexBuildingTable.NAME.ordinal() + 1, building.getName());
		prepBuilding.setString(IndexBuildingTable.NAME_EN.ordinal() + 1, building.getEnName());
		prepBuilding.setLong(IndexBuildingTable.STREET.ordinal() + 1, streetId);
		prepBuilding.setString(IndexBuildingTable.POSTCODE.ordinal()+1, building.getPostcode() == null ? null : building.getPostcode().toUpperCase());
		
		addBatch(count, prepBuilding);
	}

	public static void writeCity(PreparedStatement prepCity, Map<PreparedStatement, Integer> count, City city, int batchSize) throws SQLException {
		assert IndexCityTable.values().length == 6;
		prepCity.setLong(IndexCityTable.ID.ordinal() + 1, city.getId());
		prepCity.setDouble(IndexCityTable.LATITUDE.ordinal() + 1, city.getLocation().getLatitude());
		prepCity.setDouble(IndexCityTable.LONGITUDE.ordinal() + 1, city.getLocation().getLongitude());
		prepCity.setString(IndexCityTable.NAME.ordinal() + 1, city.getName());
		prepCity.setString(IndexCityTable.NAME_EN.ordinal() + 1, city.getEnName());
		prepCity.setString(IndexCityTable.CITY_TYPE.ordinal() + 1, CityType.valueToString(city.getType()));
		addBatch(count, prepCity, batchSize);
	}
	
	
	public static void createAddressIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexCityTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexCityTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexBuildingTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexBuildingTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexStreetNodeTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexStreetNodeTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexStreetTable.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexStreetTable.values()));
        stat.execute("PRAGMA user_version = " + IndexConstants.ADDRESS_TABLE_VERSION); //$NON-NLS-1$
        stat.close();
	}
	
	
	public DataIndexWriter writeTransport() throws IOException, SQLException{
		return writeTransport(IndexConstants.TRANSPORT_INDEX_DIR+region.getName()+IndexConstants.TRANSPORT_INDEX_EXT, null);
	}
	
	public DataIndexWriter writeTransport(String fileName, Long date) throws IOException, SQLException{
		File file = checkFile(fileName);
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e); //$NON-NLS-1$
			throw new IllegalStateException(e);
		}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath()); //$NON-NLS-1$
		try {
			createTransportIndexStructure(conn);
	        
	        PreparedStatement prepRoute = conn.prepareStatement(
	            IndexConstants.generatePrepareStatementToInsert(IndexTransportRoute.getTable(), IndexTransportRoute.values().length));
	        PreparedStatement prepRouteStops = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexTransportRouteStop.getTable(), IndexTransportRouteStop.values().length));
	        PreparedStatement prepStops = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexTransportStop.getTable(), IndexTransportStop.values().length));
	        Map<PreparedStatement, Integer> count = new HashMap<PreparedStatement, Integer>();
	        count.put(prepRouteStops, 0);
	        count.put(prepRoute, 0);
	        count.put(prepStops, 0);
	        conn.setAutoCommit(false);
	        
	        
	        Set<Long> writtenStops = new LinkedHashSet<Long>();
	        for(String t : region.getTransportRoutes().keySet()){
	        	for(TransportRoute r : region.getTransportRoutes().get(t)) {
	        		insertTransportIntoIndex(prepRoute, prepRouteStops, prepStops, writtenStops, r, count, BATCH_SIZE);
	        	}
	        }
			
			for(PreparedStatement p : count.keySet()){
				if(count.get(p) > 0){
					p.executeBatch();
				}
				p.close();
			}
			conn.setAutoCommit(true);
		} finally {
			conn.close();
			log.info(String.format("Indexing transport done in %s ms.", System.currentTimeMillis() - now)); //$NON-NLS-1$
		}
		if(date != null){
			file.setLastModified(date);
		}
		return this;
	}

	private static void writeRouteStops(PreparedStatement prepRouteStops, PreparedStatement prepStops, Map<PreparedStatement, Integer> count,
			Set<Long> writtenStops, TransportRoute r, List<TransportStop> stops, boolean direction) throws SQLException {
		int i = 0;
		for(TransportStop s : stops){
			if (!writtenStops.contains(s.getId())) {
				assert IndexTransportStop.values().length == 5;
				prepStops.setLong(IndexTransportStop.ID.ordinal() + 1, s.getId());
				prepStops.setDouble(IndexTransportStop.LATITUDE.ordinal() + 1, s.getLocation().getLatitude());
				prepStops.setDouble(IndexTransportStop.LONGITUDE.ordinal() + 1, s.getLocation().getLongitude());
				prepStops.setString(IndexTransportStop.NAME.ordinal() + 1, s.getName());
				prepStops.setString(IndexTransportStop.NAME_EN.ordinal() + 1, s.getEnName());
				addBatch(count, prepStops);
				writtenStops.add(s.getId());
			}
			assert IndexTransportRouteStop.values().length == 4;
			prepRouteStops.setLong(IndexTransportRouteStop.ROUTE.ordinal() + 1, r.getId());
			prepRouteStops.setLong(IndexTransportRouteStop.STOP.ordinal() + 1, s.getId());
			prepRouteStops.setInt(IndexTransportRouteStop.DIRECTION.ordinal() + 1, direction ? 1 : 0);
			prepRouteStops.setInt(IndexTransportRouteStop.ORD.ordinal() + 1, i++);
			addBatch(count, prepRouteStops);
		}
	}
	
	
	public static void insertTransportIntoIndex(PreparedStatement prepRoute, PreparedStatement prepRouteStops,
			PreparedStatement prepStops, Set<Long> writtenStops, TransportRoute route, Map<PreparedStatement, Integer> statements,
			int batchSize) throws SQLException {
		assert IndexTransportRoute.values().length == 7;
		prepRoute.setLong(IndexTransportRoute.ID.ordinal() + 1, route.getId());
		prepRoute.setString(IndexTransportRoute.TYPE.ordinal() + 1, route.getType());
		prepRoute.setString(IndexTransportRoute.OPERATOR.ordinal() + 1, route.getOperator());
		prepRoute.setString(IndexTransportRoute.REF.ordinal() + 1, route.getRef());
		prepRoute.setString(IndexTransportRoute.NAME.ordinal() + 1, route.getName());
		prepRoute.setString(IndexTransportRoute.NAME_EN.ordinal() + 1, route.getEnName());
		prepRoute.setInt(IndexTransportRoute.DIST.ordinal() + 1, route.getAvgBothDistance());
		addBatch(statements, prepRoute);
		
		writeRouteStops(prepRouteStops, prepStops, statements, writtenStops, route, route.getForwardStops(), true);
		writeRouteStops(prepRouteStops, prepStops, statements, writtenStops, route, route.getBackwardStops(), false);
		
	}
	
	public static void createTransportIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexTransportRoute.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexTransportRoute.values()));
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexTransportRouteStop.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexTransportRouteStop.values()));
        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexTransportStop.values()));
        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexTransportStop.values()));
        stat.execute("PRAGMA user_version = " + IndexConstants.TRANSPORT_TABLE_VERSION); //$NON-NLS-1$
        stat.close();
	}

	public static void createMapIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
        stat.execute(IndexConstants.generateCreateSQL(IndexMapRenderObject.values()));
        stat.execute(IndexConstants.generateCreateIndexSQL(IndexMapRenderObject.values()));
        stat.execute("CREATE VIRTUAL TABLE "+IndexConstants.indexMapLocationsTable+" USING rtree (id, minLon, maxLon, minLat, maxLat);");
        stat.execute("PRAGMA user_version = " + IndexConstants.MAP_TABLE_VERSION); //$NON-NLS-1$
        stat.close();
	}
	
	public static PreparedStatement createStatementMapWaysInsert(Connection conn) throws SQLException{
		assert IndexMapRenderObject.values().length == 4;
        return conn.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexMapRenderObject.getTable(), 4));
	}
	public static PreparedStatement createStatementMapWaysLocationsInsert(Connection conn) throws SQLException{
        return conn.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexConstants.indexMapLocationsTable, 5));
	}
	
	public static void insertMapRenderObjectIndex(Map<PreparedStatement, Integer> statements, 
			PreparedStatement mapStat, PreparedStatement mapWayLocationsStat, Entity e, String name, int type, boolean writeAsPoint, int batchSize) throws SQLException {
		assert IndexMapRenderObject.values().length == 4;
		if(e instanceof Relation){
			throw new IllegalArgumentException();
		}
		boolean init = false;
		double minLat = 180;
		double maxLat = -180;
		double minLon = 360;
		double maxLon = -360;
		Collection<Node> nodes; 
		if (e instanceof Way) {
			if (writeAsPoint) {
				LatLon center = MapUtils.getCenter(((Way) e));
				nodes = Collections.singleton(new Node(center.getLatitude(), center.getLongitude(), -1));
			} else {
				nodes = ((Way) e).getNodes();
			}
		} else {
			nodes = Collections.singleton((Node) e);
		}
		byte[] bytes = new byte[nodes.size() * 8];
		// generate unique id
		long id = e.getId() << 1;
		if(e instanceof Way){
			id ++;
		}
		int offset = 0;
		for (Node n : nodes) {
			if (n != null) {
				minLat = Math.min(minLat, n.getLatitude());
				maxLat = Math.max(maxLat, n.getLatitude());
				minLon = Math.min(minLon, n.getLongitude());
				maxLon = Math.max(maxLon, n.getLongitude());
				init = true;
				Algoritms.putIntToBytes(bytes, offset, Float.floatToIntBits((float) n.getLatitude()));
				offset += 4;
				Algoritms.putIntToBytes(bytes, offset, Float.floatToIntBits((float) n.getLongitude()));
				offset += 4;
			}
		}
		if (init) {
			mapStat.setLong(IndexMapRenderObject.ID.ordinal() + 1, id);
			mapStat.setInt(IndexMapRenderObject.TYPE.ordinal() + 1, type);
			mapStat.setString(IndexMapRenderObject.NAME.ordinal() + 1, name);
			mapStat.setBytes(IndexMapRenderObject.NODES.ordinal() + 1, bytes);
			addBatch(statements, mapStat);

			mapWayLocationsStat.setLong(1, id);
			mapWayLocationsStat.setFloat(2, (float) minLon);
			mapWayLocationsStat.setFloat(3, (float) maxLon);
			mapWayLocationsStat.setFloat(4, (float) minLat);
			mapWayLocationsStat.setFloat(5, (float) maxLat);
			addBatch(statements, mapWayLocationsStat);
		}
	}

	private static void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p) throws SQLException{
		addBatch(count, p, BATCH_SIZE);
	}
	
	public static void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p, int batchSize) throws SQLException{
		p.addBatch();
		if(count.get(p) >= batchSize){
			p.executeBatch();
			count.put(p, 0);
		} else {
			count.put(p, count.get(p) + 1);
		}
	}

}
