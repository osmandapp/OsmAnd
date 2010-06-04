package com.osmand.data.index;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.osmand.LogUtil;
import com.osmand.data.Amenity;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.Amenity.AmenityType;
import com.osmand.data.City.CityType;
import com.osmand.data.index.IndexConstants.IndexBuildingTable;
import com.osmand.data.index.IndexConstants.IndexCityTable;
import com.osmand.data.index.IndexConstants.IndexPoiTable;
import com.osmand.data.index.IndexConstants.IndexStreetNodeTable;
import com.osmand.data.index.IndexConstants.IndexStreetTable;
import com.osmand.osm.Node;
import com.osmand.osm.Way;


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
		f.mkdirs();
		// remove existing file
		if (f.exists()) {
			log.warn("Remove existing index : " + f.getAbsolutePath());
			f.delete();
		}
		return f;
	}
	
	
	public DataIndexWriter writePOI() throws IOException, SQLException { 
		File file = checkFile(IndexConstants.POI_INDEX_DIR+region.getName()+IndexConstants.POI_INDEX_EXT);
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e);
			throw new IllegalStateException(e);
		}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
		try {
			Statement stat = conn.createStatement();
			assert IndexPoiTable.values().length == 8;
	        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexPoiTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexPoiTable.values()));
	        stat.close();
	        
	        PreparedStatement prep = conn.prepareStatement(
	            IndexConstants.generatePrepareStatementToInsert(IndexPoiTable.getTable(), 8));
	        conn.setAutoCommit(false);
	        int currentCount = 0;
			for (Amenity a : region.getAmenityManager().getAllObjects()) {
				prep.setLong(IndexPoiTable.ID.ordinal() + 1, a.getId());
				prep.setDouble(IndexPoiTable.LATITUDE.ordinal() + 1, a.getLocation().getLatitude());
				prep.setDouble(IndexPoiTable.LONGITUDE.ordinal() + 1, a.getLocation().getLongitude());
				prep.setString(IndexPoiTable.NAME_EN.ordinal() + 1, a.getEnName());
				prep.setString(IndexPoiTable.NAME.ordinal() + 1, a.getName());
				prep.setString(IndexPoiTable.TYPE.ordinal() + 1, AmenityType.valueToString(a.getType()));
				prep.setString(IndexPoiTable.SUBTYPE.ordinal() + 1, a.getSubType());
				prep.setString(IndexPoiTable.OPENING_HOURS.ordinal() + 1 , a.getOpeningHours());
				prep.addBatch();
				currentCount++;
				if(currentCount >= BATCH_SIZE){
					prep.executeBatch();
					currentCount = 0;
				}
			}
			if(currentCount > 0){
				prep.executeBatch();
			}
			prep.close();
			conn.setAutoCommit(true);
		} finally {
			conn.close();
			log.info(String.format("Indexing poi done in %s ms.", System.currentTimeMillis() - now));
		}
		return this;
	}
	
	public DataIndexWriter writeAddress() throws IOException, SQLException{
		File file = checkFile(IndexConstants.ADDRESS_INDEX_DIR+region.getName()+IndexConstants.ADDRESS_INDEX_EXT);
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e);
			throw new IllegalStateException(e);
		}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
		try {
			Statement stat = conn.createStatement();
			assert IndexCityTable.values().length == 6;
			assert IndexBuildingTable.values().length == 6;
			assert IndexStreetNodeTable.values().length == 5;
			assert IndexStreetTable.values().length == 6;
			
	        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexCityTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexCityTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexBuildingTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexBuildingTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexStreetNodeTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexStreetNodeTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateSQL(IndexStreetTable.values()));
	        stat.executeUpdate(IndexConstants.generateCreateIndexSQL(IndexStreetTable.values()));
	        stat.close();
	        
	        PreparedStatement prepCity = conn.prepareStatement(
	            IndexConstants.generatePrepareStatementToInsert(IndexCityTable.getTable(), 6));
	        PreparedStatement prepStreet = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexStreetTable.getTable(), 6));
	        PreparedStatement prepBuilding = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexBuildingTable.getTable(), 6));
	        PreparedStatement prepStreetNode = conn.prepareStatement(
		            IndexConstants.generatePrepareStatementToInsert(IndexStreetNodeTable.getTable(), 5));
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
	        		prepCity.setLong(IndexCityTable.ID.ordinal() + 1, city.getId());
					prepCity.setDouble(IndexCityTable.LATITUDE.ordinal() + 1, city.getLocation().getLatitude());
					prepCity.setDouble(IndexCityTable.LONGITUDE.ordinal() + 1, city.getLocation().getLongitude());
					prepCity.setString(IndexCityTable.NAME.ordinal() + 1, city.getName());
					prepCity.setString(IndexCityTable.NAME_EN.ordinal() + 1, city.getEnName());
					prepCity.setString(IndexCityTable.CITY_TYPE.ordinal() + 1, CityType.valueToString(city.getType()));
					addBatch(count, prepCity);
					
					for(Street street : city.getStreets()){
						if(street.getId() == null || street.getLocation() == null){
							continue;
						}
						prepStreet.setLong(IndexStreetTable.ID.ordinal() + 1, street.getId());
						prepStreet.setString(IndexStreetTable.NAME_EN.ordinal() + 1, street.getEnName());
						prepStreet.setDouble(IndexStreetTable.LATITUDE.ordinal() + 1, street.getLocation().getLatitude());
						prepStreet.setDouble(IndexStreetTable.LONGITUDE.ordinal() + 1, street.getLocation().getLongitude());
						prepStreet.setString(IndexStreetTable.NAME.ordinal() + 1, street.getName());
						prepStreet.setLong(IndexStreetTable.CITY.ordinal() + 1, city.getId());
						addBatch(count, prepStreet);
						for(Way way : street.getWayNodes()){
							for(Node n : way.getNodes()){
								if(n == null){
									continue;
								}
								prepStreetNode.setLong(IndexStreetNodeTable.ID.ordinal() + 1, n.getId());
								prepStreetNode.setDouble(IndexStreetNodeTable.LATITUDE.ordinal() + 1, n.getLatitude());
								prepStreetNode.setDouble(IndexStreetNodeTable.LONGITUDE.ordinal() + 1, n.getLongitude());
								prepStreetNode.setLong(IndexStreetNodeTable.WAY.ordinal() + 1, way.getId());
								prepStreetNode.setLong(IndexStreetNodeTable.STREET.ordinal() + 1, street.getId());
								addBatch(count, prepStreetNode);
							}
							
						}
						
						for(Building building : street.getBuildings()){
							if(building.getId() == null || building.getLocation() == null){
								continue;
							}
							prepBuilding.setLong(IndexBuildingTable.ID.ordinal() + 1, building.getId());
							prepBuilding.setDouble(IndexBuildingTable.LATITUDE.ordinal() + 1, building.getLocation().getLatitude());
							prepBuilding.setDouble(IndexBuildingTable.LONGITUDE.ordinal() + 1, building.getLocation().getLongitude());
							prepBuilding.setString(IndexBuildingTable.NAME.ordinal() + 1, building.getName());
							prepBuilding.setString(IndexBuildingTable.NAME_EN.ordinal() + 1, building.getEnName());
							prepBuilding.setLong(IndexBuildingTable.STREET.ordinal() + 1, street.getId());
							addBatch(count, prepBuilding);
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
			log.info(String.format("Indexing address done in %s ms.", System.currentTimeMillis() - now));
		}
		return this;
	}
	
	private void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p) throws SQLException{
		p.addBatch();
		if(count.get(p) >= BATCH_SIZE){
			p.executeBatch();
			count.put(p, 0);
		} else {
			count.put(p, count.get(p) + 1);
		}
	}
	
	
}
