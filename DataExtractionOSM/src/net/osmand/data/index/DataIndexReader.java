package net.osmand.data.index;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.Street;
import net.osmand.data.City.CityType;
import net.osmand.osm.Node;
import net.osmand.plus.LogUtil;

import org.apache.commons.logging.Log;


public class DataIndexReader {
	private static final Log log = LogUtil.getLog(DataIndexReader.class);
	
	public Connection getConnection(File file) throws SQLException{
		try {
			Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e); //$NON-NLS-1$
			throw new IllegalStateException(e);
		}
        return DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath()); //$NON-NLS-1$
	}
	
	
	public List<City> readCities(Connection c) throws SQLException{
		List<City> cities = new ArrayList<City>();
		Statement stat = c.createStatement();
		ResultSet set = stat.executeQuery("select id, latitude, longitude , name , name_en , city_type from city"); //$NON-NLS-1$
		while(set.next()){
			City city = new City(CityType.valueFromString(set.getString(6)));
			city.setName(set.getString(4));
			city.setEnName(set.getString(5));
			city.setLocation(set.getDouble(2), 
					set.getDouble(3));
			city.setId(set.getLong(1));
			cities.add(city);
			
		}
		set.close();
		stat.close();
		return cities;
	}
	

	
	
	
	public PreparedStatement getStreetsBuildingPreparedStatement(Connection c) throws SQLException{
		return c.prepareStatement("SELECT A.id, A.name, A.name_en, A.latitude, A.longitude, "+ //$NON-NLS-1$
				"B.id, B.name, B.name_en, B.latitude, B.longitude, B.postcode "+ //$NON-NLS-1$
				"FROM street A left JOIN building B ON B.street = A.id WHERE A.city = ?"); //$NON-NLS-1$
	}
	
	public List<Street> readStreetsBuildings(PreparedStatement streetBuildingsStat, City city, List<Street> streets) throws SQLException {
		return readStreetsBuildings(streetBuildingsStat, city, streets, null, null, null);
	}
	
	public PreparedStatement getStreetsWayNodesPreparedStatement(Connection c) throws SQLException{
		return c.prepareStatement("SELECT A.id, A.latitude, A.longitude FROM street_node A WHERE A.street = ? "); //$NON-NLS-1$
	}

	public List<Street> readStreetsBuildings(PreparedStatement streetBuildingsStat, City city, List<Street> streets,
			PreparedStatement waynodesStat, Map<Street, List<Node>> streetNodes, List<City> citySuburbs) throws SQLException {
		Map<Long, Street> visitedStreets = new LinkedHashMap<Long, Street>();
		//read streets for city
		readStreatsByBuildingsForCity(streetBuildingsStat, city, streets,
				waynodesStat, streetNodes, visitedStreets);
		//read streets for suburbs of the city
		if (citySuburbs != null) {
			for (City suburb : citySuburbs) {
				readStreatsByBuildingsForCity(streetBuildingsStat, suburb, streets, waynodesStat, streetNodes, visitedStreets);
			}
		}
		return streets;
	}


	private void readStreatsByBuildingsForCity(
			PreparedStatement streetBuildingsStat, City city,
			List<Street> streets, PreparedStatement waynodesStat,
			Map<Street, List<Node>> streetNodes,
			Map<Long, Street> visitedStreets) throws SQLException {
		streetBuildingsStat.setLong(1, city.getId());
		ResultSet set = streetBuildingsStat.executeQuery();
		while (set.next()) {
			long streetId = set.getLong(1);
			if (!visitedStreets.containsKey(streetId)) {
				Street street = new Street(null);
				street.setName(set.getString(2));
				street.setEnName(set.getString(3));
				street.setLocation(set.getDouble(4), set.getDouble(5));
				street.setId(streetId);
				streets.add(street);
				visitedStreets.put(streetId, street);
				if (waynodesStat != null && streetNodes != null) {
					ArrayList<Node> list = new ArrayList<Node>();
					streetNodes.put(street, list);
					waynodesStat.setLong(1, street.getId());
					ResultSet rs = waynodesStat.executeQuery();
					while (rs.next()) {
						list.add(new Node(rs.getDouble(2), rs.getDouble(3), rs.getLong(1)));
					}
					rs.close();
				}
			}
			if (set.getObject(6) != null) {
				Street s = visitedStreets.get(streetId);
				Building b = new Building();
				b.setId(set.getLong(6));
				b.setName(set.getString(7));
				b.setEnName(set.getString(8));
				b.setLocation(set.getDouble(9), set.getDouble(10));
				b.setPostcode(set.getString(11));
				s.registerBuilding(b);
			}
		}

		set.close();
	}
	
	public PreparedStatement getStreetsPreparedStatement(Connection c) throws SQLException{
		return c.prepareStatement("select id, latitude, longitude , name, name_en, city from street where city = ?"); //$NON-NLS-1$
	}
	
	public List<Street> readStreets(PreparedStatement streetsStat, City city, List<Street> streets) throws SQLException{
		streetsStat.setLong(1, city.getId());
		ResultSet set = streetsStat.executeQuery();
		while(set.next()){
			Street street = new Street(city);
			street.setName(set.getString(4));
			street.setEnName(set.getString(5));
			street.setLocation(set.getDouble(2), 
					set.getDouble(3));
			street.setId(set.getLong(1));
			streets.add(street);
		}
		set.close();
		return streets;
	}
	
	public PreparedStatement getBuildingsPreparedStatement(Connection c) throws SQLException{
		return c.prepareStatement("select id, latitude, longitude, name, name_en, street, postcode from building where street = ?"); //$NON-NLS-1$
	}
	
	
	public List<Building> readBuildings(PreparedStatement buildingStat, Street street, List<Building> buildings) throws SQLException{
		buildingStat.setLong(1, street.getId());
		ResultSet set = buildingStat.executeQuery();
		while(set.next()){
			Building building = new Building();
			building.setName(set.getString(4));
			building.setEnName(set.getString(5));
			building.setLocation(set.getDouble(2), 
					set.getDouble(3));
			building.setId(set.getLong(1));
			building.setPostcode(set.getString(7));
			buildings.add(building);
		}
		set.close();
		return buildings;
	}
	
	public void testIndex(File f) throws SQLException {
		Connection c = getConnection(f);
		try {
			ArrayList<Street> streets = new ArrayList<Street>();
//			ArrayList<Building> buildings = new ArrayList<Building>();
			PreparedStatement streetstat = getStreetsBuildingPreparedStatement(c);
			int countCity = 0;
			int countStreets = 0;
			int countBuildings = 0;
			List<City> cities = readCities(c);
			for (City city : cities) {
				countCity ++;
//				System.out.println("CITY " + city.getName()); //$NON-NLS-1$
				if(city.getType() != CityType.CITY){
					continue;
				}
				streets.clear();
//				long time = System.currentTimeMillis();
				readStreetsBuildings(streetstat, city, streets);
				if(!streets.isEmpty()){
					System.out.println(city.getName());
				} else {
					System.out.print(".");
				}
				for (Street s : streets) {
					countStreets ++;
//					System.out.println("\tSTREET " + s.getName()); //$NON-NLS-1$
//					buildings.clear();
					countBuildings += s.getBuildings().size();
//					for (Building b : s.getBuildings()) {
//						countBuildings ++;
//						System.out.println("\t\tBULDING " + b.getName()); //$NON-NLS-1$
//					}
				}

			}
			System.out.println(countCity + " " +  countStreets + " " + countBuildings);
		} finally {
			c.close();
		}
	}

}
