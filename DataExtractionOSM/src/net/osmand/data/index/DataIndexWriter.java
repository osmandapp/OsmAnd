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
import net.osmand.data.index.IndexConstants.IndexBinaryMapRenderObject;
import net.osmand.data.index.IndexConstants.IndexBuildingTable;
import net.osmand.data.index.IndexConstants.IndexCityTable;
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
import rtree.IllegalValueException;
import rtree.LeafElement;
import rtree.RTree;
import rtree.RTreeInsertException;
import rtree.Rect;



public class DataIndexWriter {
	
	
	private static final int BATCH_SIZE = 1000;


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
	
	
	private static void writeRouteStops(RTree transportStopsTree, PreparedStatement prepRouteStops, PreparedStatement prepStops, Map<PreparedStatement, Integer> count,
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
				int x = (int) MapUtils.getTileNumberX(24, s.getLocation().getLongitude());
				int y = (int) MapUtils.getTileNumberX(24, s.getLocation().getLatitude());
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
			assert IndexTransportRouteStop.values().length == 4;
			prepRouteStops.setLong(IndexTransportRouteStop.ROUTE.ordinal() + 1, r.getId());
			prepRouteStops.setLong(IndexTransportRouteStop.STOP.ordinal() + 1, s.getId());
			prepRouteStops.setInt(IndexTransportRouteStop.DIRECTION.ordinal() + 1, direction ? 1 : 0);
			prepRouteStops.setInt(IndexTransportRouteStop.ORD.ordinal() + 1, i++);
			addBatch(count, prepRouteStops);
		}
	}
	
	
	public static void insertTransportIntoIndex(PreparedStatement prepRoute, PreparedStatement prepRouteStops,
			PreparedStatement prepStops, RTree transportStopsTree, 
			Set<Long> writtenStops, TransportRoute route, Map<PreparedStatement, Integer> statements,
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
		
		writeRouteStops(transportStopsTree, prepRouteStops, prepStops, statements, writtenStops, route, route.getForwardStops(), true);
		writeRouteStops(transportStopsTree, prepRouteStops, prepStops, statements, writtenStops, route, route.getBackwardStops(), false);
		
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
        stat.execute(IndexConstants.generateCreateSQL(IndexBinaryMapRenderObject.values()));
        stat.execute(IndexConstants.generateCreateIndexSQL(IndexBinaryMapRenderObject.values()));
        stat.close();
	}

	public static PreparedStatement createStatementMapBinaryInsert(Connection conn) throws SQLException{
		assert IndexBinaryMapRenderObject.values().length == 6;
        return conn.prepareStatement(IndexConstants.generatePrepareStatementToInsert(IndexBinaryMapRenderObject.getTable(), 6));
	}

	
	
	public static void insertBinaryMapRenderObjectIndex(Map<PreparedStatement, Integer> statements, 
			PreparedStatement mapBinaryStat, RTree mapTree, Entity e, String name,
			long id, int type, List<Integer> typeUse, int highwayAttributes, List<Long> restrictions, 	
			boolean inversePath, boolean writeAsPoint, int batchSize) throws SQLException {
		assert IndexBinaryMapRenderObject.values().length == 6;
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
			mapBinaryStat.setLong(IndexBinaryMapRenderObject.ID.ordinal() + 1, id);
			mapBinaryStat.setBytes(IndexBinaryMapRenderObject.TYPES.ordinal() + 1, btypes.toByteArray());
			mapBinaryStat.setBytes(IndexBinaryMapRenderObject.RESTRICTIONS.ordinal() + 1, brestrictions.toByteArray());
			mapBinaryStat.setBytes(IndexBinaryMapRenderObject.NODES.ordinal() + 1, bnodes.toByteArray());
			mapBinaryStat.setInt(IndexBinaryMapRenderObject.HIGHWAY.ordinal() + 1, highwayAttributes);
			mapBinaryStat.setString(IndexBinaryMapRenderObject.NAME.ordinal() + 1, name);
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
			count.put(p, 0);
		} else {
			count.put(p, count.get(p) + 1);
		}
	}
	
}
