package net.osmand.data.preparation;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.IndexConstants;
import net.osmand.osm.Entity;
import net.osmand.osm.MapUtils;
import net.osmand.osm.OSMSettings.OSMTagKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexPoiCreator extends AbstractIndexPartCreator {
	
	private static final Log log = LogFactory.getLog(IndexPoiCreator.class);
	
	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;
	
	private List<Amenity> tempAmenityList = new ArrayList<Amenity>();

	public IndexPoiCreator(){
	}
	
	public void iterateEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException{
		tempAmenityList.clear();
		tempAmenityList = Amenity.parseAmenities(e, tempAmenityList);
		if (!tempAmenityList.isEmpty() && poiPreparedStatement != null) {
			// load data for way (location etc...)
			ctx.loadEntityData(e, false);
			for (Amenity a : tempAmenityList) {
				checkEntity(e);
				a.setEntity(e);
				if (a.getLocation() != null) {
					// do not convert english name
					// convertEnglishName(a);
					insertAmenityIntoPoi(a);
				}
			}
		}
	}
	
	public void commitAndClosePoiFile(Long lastModifiedDate) throws SQLException {
		closeAllPreparedStatements();
		if (poiConnection != null) {
			poiConnection.commit();
			poiConnection.close();
			poiConnection = null;
			if (lastModifiedDate != null) {
				poiIndexFile.setLastModified(lastModifiedDate);
			}
		}
	}
	
	private void checkEntity(Entity e){
		String name = e.getTag(OSMTagKey.NAME);
		if (name == null){
			String msg = "";
			Collection<String> keys = e.getTagKeySet();
			int cnt = 0;
			for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
				String key = iter.next();
				if (key.startsWith("name:") && key.length() <= 8) {
					// ignore specialties like name:botanical
					if (cnt == 0)
						msg += "Entity misses default name tag, but it has localized name tag(s):\n";
					msg += key + "=" + e.getTag(key) + "\n";
					cnt++;
				}
			}
			if (cnt > 0) {
				msg += "Consider adding the name tag at " + e.getOsmUrl();
				log.warn(msg);
			}
		}
	}
	
	private void insertAmenityIntoPoi(Amenity amenity) throws SQLException {
		assert IndexConstants.POI_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		
		poiPreparedStatement.setLong(1, amenity.getId());
		poiPreparedStatement.setInt(2, MapUtils.get31TileNumberX(amenity.getLocation().getLongitude()));
		poiPreparedStatement.setInt(3, MapUtils.get31TileNumberY(amenity.getLocation().getLatitude()));
		poiPreparedStatement.setString(4, amenity.getEnName());
		poiPreparedStatement.setString(5, amenity.getName());
		poiPreparedStatement.setString(6, AmenityType.valueToString(amenity.getType()));
		poiPreparedStatement.setString(7, amenity.getSubType());
		poiPreparedStatement.setString(8, amenity.getOpeningHours());
		poiPreparedStatement.setString(9, amenity.getSite());
		poiPreparedStatement.setString(10, amenity.getPhone());
		addBatch(poiPreparedStatement);
	}
	

	public void createDatabaseStructure(File poiIndexFile) throws SQLException {
		this.poiIndexFile = poiIndexFile;
		// delete previous file to save space
		if (poiIndexFile.exists()) {
			Algoritms.removeAllFiles(poiIndexFile);
		}
		poiIndexFile.getParentFile().mkdirs();
		// creating connection
		poiConnection = DBDialect.SQLITE.getDatabaseConnection(poiIndexFile.getAbsolutePath(), log);
		
		// create database structure
		Statement stat = poiConnection.createStatement();
        stat.executeUpdate("create table " + IndexConstants.POI_TABLE +  //$NON-NLS-1$
        		"(id bigint, x int, y int, name_en varchar(255), name varchar(255), " +
        		"type varchar(255), subtype varchar(255), opening_hours varchar(255), phone varchar(255), site varchar(255)," +
        		"primary key(id, type, subtype))");
        stat.executeUpdate("create index poi_loc on poi (x, y, type, subtype)");
        stat.executeUpdate("create index poi_id on poi (id, type, subtype)");
        stat.execute("PRAGMA user_version = " + IndexConstants.POI_TABLE_VERSION); //$NON-NLS-1$
        stat.close();
        
        // create prepared statment
		poiPreparedStatement = poiConnection
				.prepareStatement("INSERT INTO " + IndexConstants.POI_TABLE + "(id, x, y, name_en, name, type, subtype, opening_hours, site, phone) " + //$NON-NLS-1$//$NON-NLS-2$
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		pStatements.put(poiPreparedStatement, 0);
		
		
		poiConnection.setAutoCommit(false);
	}
	
}
