package net.osmand.data.preparation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class DBStreetDAO extends AbstractIndexPartCreator
{
	public static class SimpleStreet {
		private final long id;
		private final String name;
		private final String cityPart;
		private LatLon location;

		public SimpleStreet(long id, String name, String cityPart, double latitude, double longitude) {
			this(id,name,cityPart, new LatLon(latitude,longitude));
		}
		public SimpleStreet(long id, String name, String cityPart,
				LatLon location) {
			this.id = id;
			this.name = name;
			this.cityPart = cityPart;
			this.location = location;
		}
		public String getCityPart() {
			return cityPart;
		}
		public long getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public LatLon getLocation() {
			return location;
		}
	}
	
	protected PreparedStatement addressStreetStat;
	private PreparedStatement addressStreetNodeStat;
	private PreparedStatement addressBuildingStat;
	private PreparedStatement addressSearchStreetStat;
	private PreparedStatement addressSearchBuildingStat;
	private PreparedStatement addressSearchStreetNodeStat;
	private PreparedStatement addressSearchStreetStatWithoutCityPart;

	private Connection mapConnection;
	private PreparedStatement addressStreetUpdateCityPart;

	public void createDatabaseStructure(Connection mapConnection, DBDialect dialect) throws SQLException {
		this.mapConnection = mapConnection;
		Statement stat = mapConnection.createStatement();
        stat.executeUpdate("create table street (id bigint primary key, latitude double, longitude double, " +
					"name varchar(1024), name_en varchar(1024), city bigint, citypart varchar(1024))");
	    stat.executeUpdate("create index street_cnp on street (city,citypart,name,id)");
        stat.executeUpdate("create index street_city on street (city)");
        stat.executeUpdate("create index street_id on street (id)");
        
        // create index on name ?
        stat.executeUpdate("create table building (id bigint, latitude double, longitude double, " +
						"name varchar(1024), name_en varchar(1024), street bigint, postcode varchar(1024), primary key(street, id))");
        stat.executeUpdate("create index building_postcode on building (postcode)");
        stat.executeUpdate("create index building_street on building (street)");
        stat.executeUpdate("create index building_id on building (id)");
        
        stat.executeUpdate("create table street_node (id bigint, latitude double, longitude double, " +
						"street bigint, way bigint)");
        stat.executeUpdate("create index street_node_street on street_node (street)");
        stat.executeUpdate("create index street_node_way on street_node (way)");
        stat.close();
        
		addressStreetStat = createPrepareStatement(mapConnection,"insert into street (id, latitude, longitude, name, name_en, city, citypart) values (?, ?, ?, ?, ?, ?, ?)");
		addressStreetNodeStat = createPrepareStatement(mapConnection,"insert into street_node (id, latitude, longitude, street, way) values (?, ?, ?, ?, ?)");
		addressBuildingStat = createPrepareStatement(mapConnection,"insert into building (id, latitude, longitude, name, name_en, street, postcode) values (?, ?, ?, ?, ?, ?, ?)");
		addressSearchStreetStat = createPrepareStatement(mapConnection,"SELECT id,latitude,longitude FROM street WHERE ? = city AND ? = citypart AND ? = name");
		addressSearchStreetStatWithoutCityPart = createPrepareStatement(mapConnection,"SELECT id,name,citypart,latitude,longitude FROM street WHERE ? = city AND ? = name");
		addressStreetUpdateCityPart = createPrepareStatement(mapConnection,"UPDATE street SET citypart = ? WHERE id = ?");
		addressSearchBuildingStat = createPrepareStatement(mapConnection,"SELECT id FROM building where ? = id");
		addressSearchStreetNodeStat = createPrepareStatement(mapConnection,"SELECT way FROM street_node WHERE ? = way");
	}
	
	protected void writeStreetWayNodes(Set<Long> streetIds, Way way) throws SQLException {
		for (Long streetId : streetIds) {
			for (Node n : way.getNodes()) {
				if (n == null) {
					continue;
				}
				addressStreetNodeStat.setLong(1, n.getId());
				addressStreetNodeStat.setDouble(2, n.getLatitude());
				addressStreetNodeStat.setDouble(3, n.getLongitude());
				addressStreetNodeStat.setLong(5, way.getId());
				addressStreetNodeStat.setLong(4, streetId);
				addBatch(addressStreetNodeStat);
			}
		}
	}
	
	protected void writeBuilding(Set<Long> streetIds, Building building) throws SQLException {
		for (Long streetId : streetIds) {
			addressBuildingStat.setLong(1, building.getId());
			addressBuildingStat.setDouble(2, building.getLocation().getLatitude());
			addressBuildingStat.setDouble(3, building.getLocation().getLongitude());
			addressBuildingStat.setString(4, building.getName());
			addressBuildingStat.setString(5, building.getEnName());
			addressBuildingStat.setLong(6, streetId);
			addressBuildingStat.setString(7, building.getPostcode() == null ? null : building.getPostcode().toUpperCase());
			addBatch(addressBuildingStat);
		}
	}

	public DBStreetDAO.SimpleStreet findStreet(String name, City city) throws SQLException {
		addressSearchStreetStatWithoutCityPart.setLong(1, city.getId());
		addressSearchStreetStatWithoutCityPart.setString(2, name);
		ResultSet rs = addressSearchStreetStatWithoutCityPart.executeQuery();
		DBStreetDAO.SimpleStreet foundId = null;
		if (rs.next()) {
			foundId = new SimpleStreet(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getDouble(4),rs.getDouble(5));
		}
		rs.close();
		return foundId;
	}

	public DBStreetDAO.SimpleStreet findStreet(String name, City city, String cityPart) throws SQLException {
		addressSearchStreetStat.setLong(1, city.getId());
		addressSearchStreetStat.setString(2, cityPart);
		addressSearchStreetStat.setString(3, name);
		ResultSet rs = addressSearchStreetStat.executeQuery();
		DBStreetDAO.SimpleStreet foundId = null;
		if (rs.next()) {
			foundId = new SimpleStreet(rs.getLong(1),name,cityPart,rs.getDouble(2),rs.getDouble(3));
		}
		rs.close();
		return foundId;
	}

	private long streetIdSequence = 0;

	public long insertStreet(String name, String nameEn, LatLon location,
			City city, String cityPart) throws SQLException {
		long streetId = fillInsertStreetStatement(name, nameEn, location, city, cityPart);
		// execute the insert statement
		addressStreetStat.execute();
		// commit immediately to search after
		mapConnection.commit();

		return streetId;
	}

	protected long fillInsertStreetStatement(String name, String nameEn,
			LatLon location, City city, String cityPart)
			throws SQLException {
		long streetId = streetIdSequence++;
		addressStreetStat.setLong(1, streetId);
		addressStreetStat.setString(4, name);
		addressStreetStat.setString(5, nameEn);
		addressStreetStat.setDouble(2, location.getLatitude());
		addressStreetStat.setDouble(3, location.getLongitude());
		addressStreetStat.setLong(6, city.getId());
		addressStreetStat.setString(7, cityPart);
		return streetId;
	}

	public boolean findBuilding(Entity e) throws SQLException {
		commit(); //we are doing batch adds, to search, we must commit
		addressSearchBuildingStat.setLong(1, e.getId());
		ResultSet rs = addressSearchBuildingStat.executeQuery();
		boolean exist = rs.next();
		rs.close();
		return exist;
	}

	public boolean findStreetNode(Entity e) throws SQLException {
		commit(); //we are doing batch adds, to search, we must commit
		addressSearchStreetNodeStat.setLong(1, e.getId());
		ResultSet rs = addressSearchStreetNodeStat.executeQuery();
		boolean exist = rs.next();
		rs.close();
		return exist;
	}

	public void commit() throws SQLException {
		if (executePendingPreparedStatements()) {
			mapConnection.commit();
		}
	}

	public void close() throws SQLException {
		closePreparedStatements(addressStreetStat, addressStreetNodeStat, addressBuildingStat);
	}

	public DBStreetDAO.SimpleStreet updateStreetCityPart(DBStreetDAO.SimpleStreet street, City city, String cityPart) throws SQLException {
		addressStreetUpdateCityPart.setString(1, cityPart);
		addressStreetUpdateCityPart.setLong(2, street.getId());
		addressStreetUpdateCityPart.executeUpdate();
		mapConnection.commit();
		return 	new SimpleStreet(street.getId(),street.getName(),cityPart,street.getLocation());
	}
}