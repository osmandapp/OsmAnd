package net.osmand.data.preparation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapIndexWriter;
import net.osmand.binary.OsmandOdb;
import net.osmand.binary.OsmandOdb.OsmAndCategoryTable;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.IndexConstants;
import net.osmand.data.Street;
import net.osmand.data.City.CityType;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.swing.Messages;

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
				// do not add that check because it is too much printing for batch creation
				// by statistic < 1% creates maps manually
				// checkEntity(e);
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
		poiConnection = (Connection) DBDialect.SQLITE.getDatabaseConnection(poiIndexFile.getAbsolutePath(), log);
		
		// create database structure
		Statement stat = poiConnection.createStatement();
        stat.executeUpdate("create table " + IndexConstants.POI_TABLE +  //$NON-NLS-1$
        		"(id bigint, x int, y int, name_en varchar(1024), name varchar(1024), " +
        		"type varchar(1024), subtype varchar(1024), opening_hours varchar(1024), phone varchar(1024), site varchar(1024)," +
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
	

	public void writeBinaryPoiIndex(BinaryMapIndexWriter writer, String regionName, IProgress progress) throws SQLException, IOException {
		if(poiPreparedStatement != null){
			closePreparedStatements(poiPreparedStatement);
		}
		poiConnection.commit();

		Statement categoriesAndSubcategories = poiConnection.createStatement();
		ResultSet rs = categoriesAndSubcategories.executeQuery("SELECT DISTINCT type, subtype FROM poi");
		Map<String, Map<String, Integer>> categories = new LinkedHashMap<String, Map<String, Integer>>();
		while (rs.next()) {
			String category = rs.getString(1);
			String subcategory = rs.getString(2).trim();
			if (!categories.containsKey(category)) {
				categories.put(category, new LinkedHashMap<String, Integer>());
			}
			if (subcategory.contains(";") || subcategory.contains(",")) {
				String[] split = subcategory.split(",|;");
				for (String sub : split) {
					categories.get(category).put(sub.trim(), 0);
				}
			} else {
				categories.get(category).put(subcategory.trim(), 0);
			}
		}
		
		rs.close();
		categoriesAndSubcategories.close();

		
		writer.startWritePOIIndex(regionName);
		Map<String, Integer> catIndexes = writer.writePOICategoriesTable(categories);
		
		for (String s : categories.keySet()) {
			System.out.println(s + " " + categories.get(s).size());
		}
		
		
		writer.endWritePOIIndex();
		
	}
	
	
	public static void main(String[] args) throws SQLException, FileNotFoundException, IOException {
		IndexPoiCreator poiCreator = new IndexPoiCreator();
		poiCreator.poiConnection  = (Connection) DBDialect.SQLITE.getDatabaseConnection("/home/victor/projects/OsmAnd/data/osm-gen/POI/Ru-mow.poi.odb", log);
		BinaryMapIndexWriter writer = new BinaryMapIndexWriter(new RandomAccessFile("/home/victor/projects/OsmAnd/data/osm-gen/POI/Test-Ru.poi.obf", "rw"));
		poiCreator.poiConnection.setAutoCommit(false);
		poiCreator.writeBinaryPoiIndex(writer, "Ru-mow", new ConsoleProgressImplementation());
		writer.close();
	}
	
}
