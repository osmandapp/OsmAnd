package com.osmand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.index.IndexConstants.IndexPoiTable;
import com.osmand.osm.LatLon;

public class AmenityIndexRepository {
	private static final Log log = LogUtil.getLog(AmenityIndexRepository.class);
	public final static int LIMIT_AMENITIES = 500;

	
	private SQLiteDatabase db;
	private double dataTopLatitude;
	private double dataBottomLatitude;
	private double dataLeftLongitude;
	private double dataRightLongitude;
	
	private String name;
	
	// cache amenities
	private List<Amenity> cachedAmenities = new ArrayList<Amenity>();
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	private int cZoom;
	private String cFilterId;
	
	
	
	private final String[] columns = IndexConstants.generateColumnNames(IndexPoiTable.values());
	public List<Amenity> searchAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int limit, PoiFilter filter, List<Amenity> amenities){
		long now = System.currentTimeMillis();
		String squery = "? < latitude AND latitude < ? AND ? < longitude AND longitude < ?";
		
		if(filter != null){
			String sql = filter.buildSqlWhereFilter();
			if(sql != null){
				squery += " AND " + sql;
			}
		}
		Cursor query = db.query(IndexPoiTable.getTable(), columns, squery, 
				new String[]{Double.toString(bottomLatitude), 
				Double.toString(topLatitude), Double.toString(leftLongitude), Double.toString(rightLongitude)}, null, null, null);
		if(query.moveToFirst()){
			do {
				Amenity am = new Amenity();
				am.setId(query.getLong(IndexPoiTable.ID.ordinal()));
				am.setLocation(query.getDouble(IndexPoiTable.LATITUDE.ordinal()), 
							query.getDouble(IndexPoiTable.LONGITUDE.ordinal()));
				am.setName(query.getString(IndexPoiTable.NAME.ordinal() ));
				am.setEnName(query.getString(IndexPoiTable.NAME_EN.ordinal()));
				am.setType(AmenityType.fromString(query.getString(IndexPoiTable.TYPE.ordinal())));
				am.setSubType(query.getString(IndexPoiTable.SUBTYPE.ordinal()));
				am.setOpeningHours(query.getString(IndexPoiTable.OPENING_HOURS.ordinal()));
				amenities.add(am);
				if(limit != -1 && amenities.size() >= limit){
					break;
				}
			} while(query.moveToNext());
		}
		query.close();
		
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.", 
					topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, amenities.size()));
		}
		return amenities;
	}
	
	public boolean addAmenity(long id, double latitude, double longitude, String name, String nameEn, AmenityType t, String subType, String openingHours){
		assert IndexPoiTable.values().length == 8;
		db.execSQL("INSERT INTO " + IndexPoiTable.getTable() + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", 
				new Object[]{id, latitude, longitude, openingHours, name, nameEn,AmenityType.valueToString(t), subType});
		return true;
	}
	
	public boolean updateAmenity(long id, double latitude, double longitude, String name, String nameEn, AmenityType t, String subType, String openingHours){
		StringBuilder b = new StringBuilder();
		b.append("UPDATE " + IndexPoiTable.getTable() + " SET ");
		b.append(IndexPoiTable.LATITUDE.name()).append(" = ?").append(", ").
		  append(IndexPoiTable.LONGITUDE.name()).append(" = ?").append(", ").
		  append(IndexPoiTable.OPENING_HOURS.name()).append(" = ?").append(", ").
		  append(IndexPoiTable.NAME.name()).append(" = ?").append(", ").
		  append(IndexPoiTable.NAME_EN.name()).append(" = ?").append(", ").
		  append(IndexPoiTable.TYPE.name()).append(" = ?").append(", ").
		  append(IndexPoiTable.SUBTYPE.name()).append(" = ?").append(" ").
		  append(" WHERE ").append(IndexPoiTable.ID.name()).append(" = ?");
		
		db.execSQL(b.toString(),			
				new Object[]{latitude, longitude, openingHours, name, nameEn,AmenityType.valueToString(t), subType, id});
		return true;
	}
	
	public boolean deleteAmenity(long id){
		db.execSQL("DELETE FROM " + IndexPoiTable.getTable()+ " WHERE id="+id);
		return true;
	}
	
	
	public synchronized void clearCache(){
		cachedAmenities.clear();
		cTopLatitude = 0;
		cBottomLatitude = 0;
		cRightLongitude = 0;
		cLeftLongitude = 0;
		cFilterId = null;
		cZoom = 0;
	}
	
	public void evaluateCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, int limit,  PoiFilter filter, List<Amenity> toFill){
		cTopLatitude = topLatitude + (topLatitude -bottomLatitude);
		cBottomLatitude = bottomLatitude - (topLatitude -bottomLatitude);
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cFilterId = filter == null? null :filter.getFilterId();
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<Amenity> tempList = new ArrayList<Amenity>();
		searchAmenities(cTopLatitude, cLeftLongitude, cBottomLatitude, cRightLongitude, limit, filter, tempList);
		synchronized (this) {
			cachedAmenities.clear();
			cachedAmenities.addAll(tempList);
		}
		
		checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, cZoom, filter.getFilterId(), toFill);
	}

	public synchronized boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, String filterId, List<Amenity> toFill, boolean fillFound){
		if (db == null) {
			return true;
		}
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && zoom == cZoom;
		if((inside || fillFound) && toFill != null && Algoritms.objectEquals(filterId, cFilterId)){
			for(Amenity a : cachedAmenities){
				LatLon location = a.getLocation();
				if (location.getLatitude() <= topLatitude && location.getLongitude() >= leftLongitude && location.getLongitude() <= rightLongitude
						&& location.getLatitude() >= bottomLatitude) {
					toFill.add(a);
				}
			}
		}
		return inside;
	}
	public boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, String filterId, List<Amenity> toFill){
		return checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, filterId, toFill, false);
	}

	public boolean initialize(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		if(db != null){
			// close previous db
			db.close();
		}
		db = SQLiteDatabase.openOrCreateDatabase(file, null);
		name = file.getName().substring(0, file.getName().indexOf('.'));
		if(db.getVersion() != IndexConstants.POI_TABLE_VERSION){
			return false;
		}
		
		Cursor query = db.query(IndexPoiTable.getTable(), new String[]{"MAX(latitude)", "MAX(longitude)", "MIN(latitude)", "MIN(longitude)"}, null, null,null, null, null);
		if(query.moveToFirst()){
			dataTopLatitude = query.getDouble(0);
			dataRightLongitude = query.getDouble(1);
			dataBottomLatitude = query.getDouble(2);
			dataLeftLongitude = query.getDouble(3);
		}
		query.close();
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms");
		}
		return true;
	}
	
	public synchronized void close(){
		if(db != null){
			db.close();
			dataRightLongitude  = dataLeftLongitude = dataBottomLatitude= dataTopLatitude = 0;
			cachedAmenities.clear();
			cTopLatitude = cBottomLatitude = cLeftLongitude = cRightLongitude = 0; 
		}
	}
	
	public String getName() {
		return name;
	}
	
	public boolean checkContains(double latitude, double longitude){
		if(latitude < dataTopLatitude && latitude > dataBottomLatitude && longitude > dataLeftLongitude && longitude < dataRightLongitude){
			return true;
		}
		return false;
	}
	
	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude){
		if(rightLongitude < dataLeftLongitude || leftLongitude > dataRightLongitude){
			return false;
		}
		if(topLatitude < dataBottomLatitude || bottomLatitude > dataTopLatitude){
			return false;
		}
		return true;
	}


	
}
