package com.osmand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.index.IndexConstants.IndexPoiTable;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.Node;
import com.osmand.osm.io.IOsmStorageFilter;
import com.osmand.osm.io.OsmBaseStorage;

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
		String squery = "? < latitude AND latitude < ? AND ? < longitude AND longitude < ?"; //$NON-NLS-1$
		
		if(filter != null){
			String sql = filter.buildSqlWhereFilter();
			if(sql != null){
				squery += " AND " + sql; //$NON-NLS-1$
			}
		}
		if(limit != -1){
			squery += " ORDER BY RANDOM() LIMIT " +limit; //$NON-NLS-1$
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
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, amenities.size())); //$NON-NLS-1$
		}
		return amenities;
	}
	
	public boolean addAmenity(long id, double latitude, double longitude, String name, String nameEn, AmenityType t, String subType, String openingHours){
		assert IndexPoiTable.values().length == 8;
		db.execSQL("INSERT INTO " + IndexPoiTable.getTable() + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",  //$NON-NLS-1$ //$NON-NLS-2$
				new Object[]{id, latitude, longitude, openingHours, name, nameEn,AmenityType.valueToString(t), subType});
		return true;
	}
	
	public boolean updateAmenity(long id, double latitude, double longitude, String name, String nameEn, AmenityType t, String subType, String openingHours){
		StringBuilder b = new StringBuilder();
		b.append("UPDATE " + IndexPoiTable.getTable() + " SET "); //$NON-NLS-1$ //$NON-NLS-2$
		b.append(IndexPoiTable.LATITUDE.name()).append(" = ?").append(", "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(IndexPoiTable.LONGITUDE.name()).append(" = ?").append(", "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(IndexPoiTable.OPENING_HOURS.name()).append(" = ?").append(", "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(IndexPoiTable.NAME.name()).append(" = ?").append(", "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(IndexPoiTable.NAME_EN.name()).append(" = ?").append(", "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(IndexPoiTable.TYPE.name()).append(" = ?").append(", "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(IndexPoiTable.SUBTYPE.name()).append(" = ?").append(" "). //$NON-NLS-1$ //$NON-NLS-2$
		  append(" WHERE ").append(IndexPoiTable.ID.name()).append(" = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		
		db.execSQL(b.toString(),			
				new Object[]{latitude, longitude, openingHours, name, nameEn,AmenityType.valueToString(t), subType, id});
		return true;
	}
	
	public boolean deleteAmenity(long id){
		db.execSQL("DELETE FROM " + IndexPoiTable.getTable()+ " WHERE id="+id); //$NON-NLS-1$ //$NON-NLS-2$
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
		boolean noNeedToSearch = inside &&  Algoritms.objectEquals(filterId, cFilterId);
		if((inside || fillFound) && toFill != null && Algoritms.objectEquals(filterId, cFilterId)){
			for(Amenity a : cachedAmenities){
				LatLon location = a.getLocation();
				if (location.getLatitude() <= topLatitude && location.getLongitude() >= leftLongitude && location.getLongitude() <= rightLongitude
						&& location.getLatitude() >= bottomLatitude) {
					toFill.add(a);
				}
			}
		}
		return noNeedToSearch;
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
			db.close();
			db = null;
			return false;
		}
		
		Cursor query = db.query(IndexPoiTable.getTable(), new String[]{"MAX(latitude)", "MAX(longitude)", "MIN(latitude)", "MIN(longitude)"}, null, null,null, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if(query.moveToFirst()){
			dataTopLatitude = query.getDouble(0);
			dataRightLongitude = query.getDouble(1);
			dataBottomLatitude = query.getDouble(2);
			dataLeftLongitude = query.getDouble(3);
		}
		query.close();
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	
	private void bindString(SQLiteStatement s, int i, String v){
		if(v == null){
			s.bindNull(i);
		} else {
			s.bindString(i, v);
		}
	}
	
	public boolean updateAmenities(List<Amenity> amenities, double leftLon, double topLat, double rightLon, double bottomLat){
		String latCol = IndexPoiTable.LATITUDE.name();
		String lonCol = IndexPoiTable.LONGITUDE.name();
		db.execSQL("DELETE FROM " + IndexPoiTable.getTable() + " WHERE " + //$NON-NLS-1$ //$NON-NLS-2$
				lonCol + ">= ? AND ? <=" + lonCol + " AND " + //$NON-NLS-1$//$NON-NLS-2$
				latCol + ">= ? AND ? <=" + latCol, new Double[] { leftLon, rightLon, bottomLat, topLat }); //$NON-NLS-1$
		
		SQLiteStatement stat = db.compileStatement(IndexConstants.generatePrepareStatementToInsert(IndexPoiTable.getTable(), 8));
		for (Amenity a : amenities) {
			stat.bindLong(IndexPoiTable.ID.ordinal() + 1, a.getId());
			stat.bindDouble(IndexPoiTable.LATITUDE.ordinal() + 1, a.getLocation().getLatitude());
			stat.bindDouble(IndexPoiTable.LONGITUDE.ordinal() + 1, a.getLocation().getLongitude());
			bindString(stat, IndexPoiTable.NAME_EN.ordinal() + 1, a.getEnName());
			bindString(stat, IndexPoiTable.NAME.ordinal() + 1, a.getName());
			bindString(stat, IndexPoiTable.TYPE.ordinal() + 1, AmenityType.valueToString(a.getType()));
			bindString(stat, IndexPoiTable.SUBTYPE.ordinal() + 1, a.getSubType());
			bindString(stat, IndexPoiTable.OPENING_HOURS.ordinal() + 1 , a.getOpeningHours());
			stat.execute();
		}
		stat.close();
		return true;
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


	private final static String SITE_API = "http://api.openstreetmap.org/"; //$NON-NLS-1$
	
	public static boolean loadingPOIs(List<Amenity> amenities, double leftLon, double topLat, double righLon, double bottomLat) {
		try {
			// bbox=left,bottom,right,top
			String u = SITE_API+"api/0.6/map?bbox="+leftLon+","+bottomLat+","+righLon+","+topLat;  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			URL url = new URL(u);
			log.info("Start loading poi : " + u); //$NON-NLS-1$
			InputStream is = url.openStream();
			OsmBaseStorage st = new OsmBaseStorage();
			final List<Entity> amen = new ArrayList<Entity>();
			st.getFilters().add(new IOsmStorageFilter(){
				@Override
				public boolean acceptEntityToLoad(OsmBaseStorage storage, Entity entity) {
					if(Amenity.isAmenity(entity)){
						amen.add(entity);
						return true;
					}
					// to 
					return entity instanceof Node;
				}
			});
			st.parseOSM(is, null, null, false);
			for (Entity e : amen) {
				amenities.add(new Amenity(e));
			}
			log.info("Loaded " +amenities.size() + " amenities");  //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			log.error("Loading nodes failed", e); //$NON-NLS-1$
			return false;
		} catch (SAXException e) {
			log.error("Loading nodes failed", e); //$NON-NLS-1$
			return false;
		}
		return true;
	}
}
