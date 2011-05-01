package net.osmand.data.preparation;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.index.DataIndexWriter;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.Entity;
import net.osmand.osm.MapUtils;
import net.osmand.osm.OSMSettings.OSMTagKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexPoiCreator {
	
	private static final int BATCH_SIZE = 1000;
	private static final Log log = LogFactory.getLog(IndexPoiCreator.class);
	private final IndexCreator creator;
	
	private Connection poiConnection;
	private File poiIndexFile;
	private PreparedStatement poiPreparedStatement;
	
	private List<Amenity> tempAmenityList = new ArrayList<Amenity>();

	public IndexPoiCreator(IndexCreator creator){
		this.creator = creator;
	}
	
	public void iterateEntity(Entity e, Map<PreparedStatement, Integer> pStatements) throws SQLException{
		tempAmenityList.clear();
		tempAmenityList = Amenity.parseAmenities(e, tempAmenityList);
		if (!tempAmenityList.isEmpty() && poiPreparedStatement != null) {
			// load data for way (location etc...)
			creator.loadEntityData(e, false);
			for (Amenity a : tempAmenityList) {
				checkEntity(e);
				a.setEntity(e);
				if (a.getLocation() != null) {
					// do not convert english name
					// convertEnglishName(a);
					insertAmenityIntoPoi(pStatements, a);
				}
			}
		}
	}
	
	public void commitAndClosePoiFile(Long lastModifiedDate) throws SQLException {
		if (poiConnection != null) {
			poiConnection.commit();
			poiConnection.close();
			poiConnection = null;
			if (lastModifiedDate != null) {
				poiIndexFile.setLastModified(lastModifiedDate);
			}
		}
		
	}
	
	public void insertAmenityIntoPoi( Map<PreparedStatement, Integer> map, Amenity amenity) throws SQLException {
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
		DataIndexWriter.addBatch(map, poiPreparedStatement, BATCH_SIZE);
	}
	
	public void checkEntity(Entity e){
		String name = e.getTag(OSMTagKey.NAME);
		if (name == null){
			String msg = "";
			Collection<String> keys = e.getTagKeySet();
			int cnt = 0;
			for (Iterator iter = keys.iterator(); iter.hasNext();) {
				String key = (String) iter.next();
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
	
	public void createDatabaseStructure(File poiIndexFile, Map<PreparedStatement, Integer> pStatements) throws SQLException {
		this.poiIndexFile = poiIndexFile;
		// to save space
		if (poiIndexFile.exists()) {
			Algoritms.removeAllFiles(poiIndexFile);
		}
		poiIndexFile.getParentFile().mkdirs();
		// creating nodes db to fast access for all nodes
		poiConnection = creator.getDatabaseConnection(poiIndexFile.getAbsolutePath(), DBDialect.SQLITE);
		createPoiIndexStructure(poiConnection);
		poiPreparedStatement = createStatementAmenityInsert(poiConnection);
		pStatements.put(poiPreparedStatement, 0);
		poiConnection.setAutoCommit(false);
	}
	
	public void createPoiIndexStructure(Connection conn) throws SQLException{
		Statement stat = conn.createStatement();
        stat.executeUpdate("create table " + IndexConstants.POI_TABLE +  //$NON-NLS-1$
        		"(id bigint, x int, y int, name_en varchar(255), name varchar(255), " +
        		"type varchar(255), subtype varchar(255), opening_hours varchar(255), phone varchar(255), site varchar(255)," +
        		"primary key(id, type, subtype))");
        stat.executeUpdate("create index poi_loc on poi (x, y, type, subtype)");
        stat.executeUpdate("create index poi_id on poi (id, type, subtype)");
        stat.execute("PRAGMA user_version = " + IndexConstants.POI_TABLE_VERSION); //$NON-NLS-1$
        stat.close();
	}
	
	public PreparedStatement createStatementAmenityInsert(Connection conn) throws SQLException{
        return conn.prepareStatement("INSERT INTO " + IndexConstants.POI_TABLE + "(id, x, y, name_en, name, type, subtype, opening_hours, site, phone) " +  //$NON-NLS-1$//$NON-NLS-2$
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	}

}
