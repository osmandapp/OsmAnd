package net.osmand.data.index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.data.City.CityType;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import rtree.IllegalValueException;
import rtree.LeafElement;
import rtree.RTree;
import rtree.RTreeInsertException;
import rtree.Rect;



public class DataIndexWriter {
	
	private static final int BATCH_SIZE = 1000;


	public static void insertAmenityIntoPoi(PreparedStatement prep, Map<PreparedStatement, Integer> map, Amenity amenity, int batchSize) throws SQLException {
		assert IndexConstants.POI_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		
		prep.setLong(1, amenity.getId());
		prep.setInt(2, MapUtils.get31TileNumberX(amenity.getLocation().getLongitude()));
		prep.setInt(3, MapUtils.get31TileNumberY(amenity.getLocation().getLatitude()));
		prep.setString(4, amenity.getEnName());
		prep.setString(5, amenity.getName());
		prep.setString(6, AmenityType.valueToString(amenity.getType()));
		prep.setString(7, amenity.getSubType());
		prep.setString(8, amenity.getOpeningHours());
		prep.setString(9, amenity.getSite());
		prep.setString(10, amenity.getPhone());
		addBatch(map, prep, batchSize);
	}
	
	public static PreparedStatement createStatementAmenityInsert(Connection conn) throws SQLException{
        return conn.prepareStatement("INSERT INTO " + IndexConstants.POI_TABLE + "(id, x, y, name_en, name, type, subtype, opening_hours, site, phone) " +  //$NON-NLS-1$//$NON-NLS-2$
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	}
	
	public static void createPoiIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
        stat.executeUpdate("create table " + IndexConstants.POI_TABLE +  //$NON-NLS-1$
        		"(id bigint, x int, y int, name_en varchar(255), name varchar(255), " +
        		"type varchar(255), subtype varchar(255), opening_hours varchar(255), phone varchar(255), site varchar(255)," +
        		"primary key(id, type, subtype))");
        stat.executeUpdate("create index poi_loc on poi (x, y, type, subtype)");
        stat.executeUpdate("create index poi_id on poi (id, type, subtype)");
        if(IndexCreator.usingSQLite()){
        	stat.execute("PRAGMA user_version = " + IndexConstants.POI_TABLE_VERSION); //$NON-NLS-1$
        }
        stat.close();
	}

	
	public static PreparedStatement getStreetNodeInsertPreparedStatement(Connection conn) throws SQLException {
		assert IndexConstants.STREET_NODE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return conn.prepareStatement("insert into street_node (id, latitude, longitude, street, way) values (?, ?, ?, ?, ?)");
	}
	
	public static void writeStreetWayNodes(PreparedStatement prepStreetNode, Map<PreparedStatement, Integer> count, Long streetId, Way way, int batchSize)
			throws SQLException {
		assert IndexConstants.STREET_NODE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		for (Node n : way.getNodes()) {
			if (n == null) {
				continue;
			}
			prepStreetNode.setLong(1, n.getId());
			prepStreetNode.setDouble(2, n.getLatitude());
			prepStreetNode.setDouble(3, n.getLongitude());
			prepStreetNode.setLong(5, way.getId());
			prepStreetNode.setLong(4, streetId);
			addBatch(count, prepStreetNode, BATCH_SIZE);
		}
	}
	
	public static PreparedStatement getBuildingInsertPreparedStatement(Connection conn) throws SQLException {
		assert IndexConstants.BUILDING_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return conn.prepareStatement("insert into building (id, latitude, longitude, name, name_en, street, postcode) values (?, ?, ?, ?, ?, ?, ?)");
	}

	public static void writeBuilding(PreparedStatement prepBuilding, Map<PreparedStatement, Integer> count, Long streetId, 
			Building building, int batchSize)
			throws SQLException {
		assert IndexConstants.BUILDING_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		prepBuilding.setLong(1, building.getId());
		prepBuilding.setDouble(2, building.getLocation().getLatitude());
		prepBuilding.setDouble(3, building.getLocation().getLongitude());
		prepBuilding.setString(4, building.getName());
		prepBuilding.setString(5, building.getEnName());
		prepBuilding.setLong(6, streetId);
		prepBuilding.setString(7, building.getPostcode() == null ? null : building.getPostcode().toUpperCase());

		addBatch(count, prepBuilding);
	}
	

	public static PreparedStatement getSearchStreetPreparedStatement(Connection mapConnection) throws SQLException {
		assert IndexConstants.STREET_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return mapConnection.prepareStatement("SELECT ID FROM street WHERE ? = city AND ? = name");
	}

	public static PreparedStatement getSearchBuildingPreparedStatement(Connection mapConnection) throws SQLException {
		assert IndexConstants.BUILDING_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return mapConnection.prepareStatement("SELECT id FROM building where ? = id");
	}

	public static PreparedStatement getStreeNodeSearchPreparedStatement(Connection mapConnection) throws SQLException {
		assert IndexConstants.STREET_NODE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return mapConnection.prepareStatement("SELECT way FROM street_node WHERE ? = way");
	}
	
	public static PreparedStatement getUpdateBuildingPostcodePreparedStatement(Connection mapConnection) throws SQLException {
		assert IndexConstants.BUILDING_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return mapConnection.prepareStatement("UPDATE building SET postcode = ? WHERE id = ?");
	}
	

	public static PreparedStatement getCityInsertPreparedStatement(Connection conn) throws SQLException{
		assert IndexConstants.CITY_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return conn.prepareStatement("insert into city (id, latitude, longitude, name, name_en, city_type) values (?, ?, ?, ?, ?, ?)");
	}
	
	
	public static void writeCity(PreparedStatement prepCity, Map<PreparedStatement, Integer> count, City city, int batchSize) throws SQLException {
		assert IndexConstants.CITY_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		prepCity.setLong(1, city.getId());
		prepCity.setDouble(2, city.getLocation().getLatitude());
		prepCity.setDouble(3, city.getLocation().getLongitude());
		prepCity.setString(4, city.getName());
		prepCity.setString(5, city.getEnName());
		prepCity.setString(6, CityType.valueToString(city.getType()));
		addBatch(count, prepCity, batchSize);
	}
	
	public static PreparedStatement getStreetInsertPreparedStatement(Connection conn) throws SQLException{
		assert IndexConstants.STREET_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		return conn.prepareStatement("insert into street (id, latitude, longitude, name, name_en, city) values (?, ?, ?, ?, ?, ?)");
	}
	
	public static void insertStreetData(PreparedStatement addressStreetStat, long id, String name, String nameEn, double latitude,
			double longitude, Long cityId) throws SQLException {
		assert IndexConstants.STREET_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		addressStreetStat.setLong(1, id);
		addressStreetStat.setString(4, name);
		addressStreetStat.setString(5, nameEn);
		addressStreetStat.setDouble(2, latitude);
		addressStreetStat.setDouble(3, longitude);
		addressStreetStat.setLong(6, cityId);
	}
	
	
	public static void createAddressIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
		assert IndexConstants.CITY_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.STREET_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.STREET_NODE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.STREET_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		
        stat.executeUpdate("create table city (id bigint primary key, latitude double, longitude double, " +
        			"name varchar(255), name_en varchar(255), city_type varchar(32))");
        stat.executeUpdate("create index city_ind on city (id, city_type)");
        
        stat.executeUpdate("create table street (id bigint primary key, latitude double, longitude double, " +
					"name varchar(255), name_en varchar(255), city bigint)");
        stat.executeUpdate("create index street_city on street (city)");
        stat.executeUpdate("create index street_id on street (id)");
        // create index on name ?

        stat.executeUpdate("create table building (id bigint, latitude double, longitude double, " +
						"name varchar(255), name_en varchar(255), street bigint, postcode varchar(255), primary key(street, id))");
        stat.executeUpdate("create index building_postcode on building (postcode)");
        stat.executeUpdate("create index building_street on building (street)");
        stat.executeUpdate("create index building_id on building (id)");
        
        
        stat.executeUpdate("create table street_node (id bigint, latitude double, longitude double, " +
						"street bigint, way bigint)");
        stat.executeUpdate("create index street_node_street on street_node (street)");
        stat.executeUpdate("create index street_node_way on street_node (way)");
        
        if(IndexCreator.usingSQLite()){
        	stat.execute("PRAGMA user_version = " + IndexConstants.ADDRESS_TABLE_VERSION); //$NON-NLS-1$
        }
        stat.close();
	}
	
	public static PreparedStatement createStatementTransportStopInsert(Connection conn) throws SQLException{
		assert IndexConstants.TRANSPORT_STOP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
        return conn.prepareStatement("insert into transport_stop(id, latitude, longitude, name, name_en) values(?, ?, ?, ?, ?)");
	}
	public static PreparedStatement createStatementTransportRouteStopInsert(Connection conn) throws SQLException{
		assert IndexConstants.TRANSPORT_ROUTE_STOP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
        return conn.prepareStatement("insert into transport_route_stop(route, stop, direction, ord) values(?, ?, ?, ?)");
	}
	
	private static void writeRouteStops(RTree transportStopsTree, PreparedStatement prepRouteStops, PreparedStatement prepStops, Map<PreparedStatement, Integer> count,
			Set<Long> writtenStops, TransportRoute r, List<TransportStop> stops, boolean direction) throws SQLException {
		assert IndexConstants.TRANSPORT_STOP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.TRANSPORT_ROUTE_STOP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		int i = 0;
		for(TransportStop s : stops){
			if (!writtenStops.contains(s.getId())) {
				prepStops.setLong(1, s.getId());
				prepStops.setDouble(2, s.getLocation().getLatitude());
				prepStops.setDouble(3, s.getLocation().getLongitude());
				prepStops.setString(4, s.getName());
				prepStops.setString(5, s.getEnName());
				int x = (int) MapUtils.getTileNumberX(24, s.getLocation().getLongitude());
				int y = (int) MapUtils.getTileNumberY(24, s.getLocation().getLatitude());
				addBatch(count, prepStops);
				try {
					transportStopsTree.insert(new LeafElement(new Rect(x, y, x, y), s.getId()));
				} catch (RTreeInsertException e) {
					throw new IllegalArgumentException(e);
				} catch (IllegalValueException e) {
					throw new IllegalArgumentException(e);
				}
				writtenStops.add(s.getId());
			}
			prepRouteStops.setLong(1, r.getId());
			prepRouteStops.setLong(2, s.getId());
			prepRouteStops.setInt(3, direction ? 1 : 0);
			prepRouteStops.setInt(4, i++);
			addBatch(count, prepRouteStops);
		}
	}
	
	public static PreparedStatement createStatementTransportRouteInsert(Connection conn) throws SQLException{
		assert IndexConstants.TRANSPORT_ROUTE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
        return conn.prepareStatement("insert into transport_route(id, type, operator, ref, name, name_en, dist) values(?, ?, ?, ?, ?, ?, ?)");
	}
	
	public static void insertTransportIntoIndex(PreparedStatement prepRoute, PreparedStatement prepRouteStops,
			PreparedStatement prepStops, RTree transportStopsTree, 
			Set<Long> writtenStops, TransportRoute route, Map<PreparedStatement, Integer> statements,
			int batchSize) throws SQLException {
		assert IndexConstants.TRANSPORT_ROUTE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		prepRoute.setLong(1, route.getId());
		prepRoute.setString(2, route.getType());
		prepRoute.setString(3, route.getOperator());
		prepRoute.setString(4, route.getRef());
		prepRoute.setString(5, route.getName());
		prepRoute.setString(6, route.getEnName());
		prepRoute.setInt(7, route.getAvgBothDistance());
		addBatch(statements, prepRoute);
		
		writeRouteStops(transportStopsTree, prepRouteStops, prepStops, statements, writtenStops, route, route.getForwardStops(), true);
		writeRouteStops(transportStopsTree, prepRouteStops, prepStops, statements, writtenStops, route, route.getBackwardStops(), false);
		
	}
	
	public static void createTransportIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
		assert IndexConstants.TRANSPORT_ROUTE_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.TRANSPORT_STOP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.TRANSPORT_ROUTE_STOP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		
        stat.executeUpdate("create table transport_route (id bigint primary key, type varchar(255), operator varchar(255)," +
        		"ref varchar(255), name varchar(255), name_en varchar(255), dist int)");
        stat.executeUpdate("create index transport_route_id on transport_route (id)");
        
        stat.executeUpdate("create table transport_route_stop (stop bigint, route bigint, ord int, direction smallint, primary key (route, ord, direction))");
        stat.executeUpdate("create index transport_route_stop_stop on transport_route_stop (stop)");
        stat.executeUpdate("create index transport_route_stop_route on transport_route_stop (route)");
        
        stat.executeUpdate("create table transport_stop (id bigint primary key, latitude double, longitude double, name varchar(255), name_en varchar(255))");
        stat.executeUpdate("create index transport_stop_id on transport_stop (id)");
        stat.executeUpdate("create index transport_stop_location on transport_stop (latitude, longitude)");
        
        if(IndexCreator.usingSQLite()){
        	stat.execute("PRAGMA user_version = " + IndexConstants.TRANSPORT_TABLE_VERSION); //$NON-NLS-1$
        }
        stat.close();
	}

	
	public static void createMapIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
		assert IndexConstants.BINARY_MAP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		assert IndexConstants.LOW_LEVEL_MAP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
        stat.executeUpdate("create table binary_map_objects (id bigint primary key, name varchar(255), " +
        		"types binary, restrictions binary, nodes binary, highway int)");
        stat.executeUpdate("create index binary_map_objects_ind on binary_map_objects (id)");
        
        stat.executeUpdate("create table low_level_map_objects (id bigint primary key, start_node bigint, " +
		"end_node bigint, name varchar(255), nodes binary, type bigint, level smallint)");
        stat.executeUpdate("create index low_level_map_objects_ind on low_level_map_objects (id)");
        stat.executeUpdate("create index low_level_map_objects_ind_loc on low_level_map_objects (start_node, end_node)");
        stat.close();
	}

	public static PreparedStatement createStatementMapBinaryInsert(Connection conn) throws SQLException{
		assert IndexConstants.BINARY_MAP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
        return conn.prepareStatement("insert into binary_map_objects(id, name, types, restrictions, nodes, highway) values(?, ?, ?, ?, ?, ?)");
	}
	
	public static PreparedStatement createStatementLowLevelMapBinaryInsert(Connection conn) throws SQLException{
		assert IndexConstants.LOW_LEVEL_MAP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
        return conn.prepareStatement("insert into low_level_map_objects(id, start_node, end_node, name, nodes, type, level) values(?, ?, ?, ?, ?, ?, ?)");
	}
	
	public static void insertLowLevelMapBinaryObject(Map<PreparedStatement, Integer> statements, 
			PreparedStatement mapLowLevelBinaryStat, int level,long types, long id, List<Node> nodes, String name) throws SQLException{
		assert IndexConstants.LOW_LEVEL_MAP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		boolean first = true;
		long firstId = -1;
		long lastId = -1;
		ByteArrayOutputStream bnodes = new ByteArrayOutputStream();
		try {
			for (Node n : nodes) {
				if (n != null) {
					if (first) {
						firstId = n.getId();
						first = false;
					}
					lastId = n.getId();
					Algoritms.writeInt(bnodes, Float.floatToRawIntBits((float) n.getLatitude()));
					Algoritms.writeInt(bnodes, Float.floatToRawIntBits((float) n.getLongitude()));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		if(firstId == -1){
			return;
		}
		// conn.prepareStatement("insert into binary_map_objects(id, name, types, restrictions, nodes, highway) values(?, ?, ?, ?, ?, ?)");
		mapLowLevelBinaryStat.setLong(1, id);
		mapLowLevelBinaryStat.setLong(2, firstId);
		mapLowLevelBinaryStat.setLong(3, lastId);
		mapLowLevelBinaryStat.setString(4, name);
		mapLowLevelBinaryStat.setBytes(5, bnodes.toByteArray());
		mapLowLevelBinaryStat.setLong(6, types);
		mapLowLevelBinaryStat.setShort(7, (short) level);
		
		addBatch(statements, mapLowLevelBinaryStat);
	}
	public static void insertBinaryMapRenderObjectIndex(Map<PreparedStatement, Integer> statements, 
			PreparedStatement mapBinaryStat, RTree mapTree, Entity e, String name,
			long id, int type, List<Integer> typeUse, int highwayAttributes, List<Long> restrictions, 	
			boolean inversePath, boolean writeAsPoint, int batchSize) throws SQLException {
		if(e instanceof Relation){
			throw new IllegalArgumentException();
		}
		boolean init = false;
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
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
		if(inversePath){
			nodes = new ArrayList<Node>(nodes);
			Collections.reverse((List<?>) nodes);
		}
		
		ByteArrayOutputStream bnodes = new ByteArrayOutputStream();
		ByteArrayOutputStream btypes = new ByteArrayOutputStream();
		ByteArrayOutputStream brestrictions = new ByteArrayOutputStream();
		
		try {
			Algoritms.writeSmallInt(btypes, type);
			for (Integer i : typeUse) {
				Algoritms.writeSmallInt(btypes, i);
			}
			for (Long i : restrictions) {
				Algoritms.writeLongInt(brestrictions, i);
			}

			for (Node n : nodes) {
				if (n != null) {
					int y = MapUtils.get31TileNumberY(n.getLatitude());
					int x = MapUtils.get31TileNumberX(n.getLongitude());
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					init = true;
					Algoritms.writeInt(bnodes, x);
					Algoritms.writeInt(bnodes, y);
				}
			}
		} catch (IOException es) {
			throw new IllegalStateException(es);
		}
		if (init) {
			assert IndexConstants.BINARY_MAP_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
			// conn.prepareStatement("insert into binary_map_objects(id, name, types, restrictions, nodes, highway) values(?, ?, ?, ?, ?, ?)");
			mapBinaryStat.setLong(1, id);
			mapBinaryStat.setString(2, name);
			mapBinaryStat.setBytes(3, btypes.toByteArray());
			mapBinaryStat.setBytes(4, brestrictions.toByteArray());
			mapBinaryStat.setBytes(5, bnodes.toByteArray());
			mapBinaryStat.setInt(6, highwayAttributes);
			
			addBatch(statements, mapBinaryStat);
			try {
				mapTree.insert(new LeafElement(new Rect(minX, minY, maxX, maxY), id));
			} catch (RTreeInsertException e1) {
				throw new IllegalArgumentException(e1);
			} catch (IllegalValueException e1) {
				throw new IllegalArgumentException(e1);
			}
		}
	}
	private static void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p) throws SQLException{
		addBatch(count, p, BATCH_SIZE);
	}
	
	public static void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p, int batchSize) throws SQLException{
		p.addBatch();
		if(count.get(p) >= batchSize){
			p.executeBatch();
			p.getConnection().commit();
			count.put(p, 0);
		} else {
			count.put(p, count.get(p) + 1);
		}
	}



}
