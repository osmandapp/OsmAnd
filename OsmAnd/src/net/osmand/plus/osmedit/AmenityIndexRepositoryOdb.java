package net.osmand.plus.osmedit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityParser;
import net.osmand.osm.edit.Node;
import net.osmand.osm.io.IOsmStorageFilter;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.resources.AmenityIndexRepository;
import net.osmand.plus.resources.BaseLocationIndexRepository;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class AmenityIndexRepositoryOdb extends BaseLocationIndexRepository<Amenity> implements AmenityIndexRepository {
	private static final Log log = PlatformUtil.getLog(AmenityIndexRepositoryOdb.class);
	public final static int LIMIT_AMENITIES = 500;

		
	// cache amenities
	private String cFilterId;
	
	
	private final String[] columns = new String[]{"id", "x", "y", "name", "name_en", "type", "subtype", "opening_hours", "phone", "site"};        //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$//$NON-NLS-6$//$NON-NLS-7$//$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	private boolean changes = false;
	
	@Override
	public List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom, PoiFilter filter, 
			List<Amenity> amenities, ResultMatcher<Amenity> matcher){
		long now = System.currentTimeMillis();
		String squery = "? < y AND y < ? AND ? < x AND x < ?"; //$NON-NLS-1$
		
		if(filter != null){
			String sql = filter.buildSqlWhereFilter();
			if(sql != null){
				squery += " AND " + sql; //$NON-NLS-1$
			}
		}
		int limit = -1;
		if(zoom != -1){
			limit = 200;
			squery += " ORDER BY RANDOM() LIMIT 200"; //$NON-NLS-1$
		}
		Cursor query = db.query(IndexConstants.POI_TABLE, columns, squery, 
				new String[]{stop+"",  //$NON-NLS-1$
				sbottom+"", sleft+"",  //$NON-NLS-1$ //$NON-NLS-2$
				sright+""}, null, null, null); //$NON-NLS-1$
		if(query.moveToFirst()){
			do {
				Amenity am = new Amenity();
				am.setId(query.getLong(0));
				am.setLocation(MapUtils.get31LatitudeY(query.getInt(2)), 
						MapUtils.get31LongitudeX(query.getInt(1)));
				am.setName(query.getString(3 ));
				am.setEnName(query.getString(4));
				if(am.getEnName().length() == 0){
					am.setEnName(Junidecode.unidecode(am.getName()));
				}
				am.setType(AmenityType.fromString(query.getString(5)));
				am.setSubType(query.getString(6));
				am.setOpeningHours(query.getString(7));
				am.setPhone(query.getString(8));
				am.setSite(query.getString(9));
				if (matcher == null || matcher.publish(am)) {
					amenities.add(am);
				}
				if(limit != -1 && amenities.size() >= limit){
					break;
				}
			} while(query.moveToNext());
		}
		query.close();
		
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					MapUtils.get31LatitudeY(stop) + " " + MapUtils.get31LongitudeX(sleft), System.currentTimeMillis() - now, amenities.size())); //$NON-NLS-1$
		}
		return amenities;
	}
	
	
	@Override
	public synchronized void clearCache(){
		super.clearCache();
		cFilterId = null;
	}
	
	@Override
	public void evaluateCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom,
			PoiFilter filter, ResultMatcher<Amenity> matcher) {
		cTopLatitude = topLatitude;
		cBottomLatitude = bottomLatitude;
		cLeftLongitude = leftLongitude;
		cRightLongitude = rightLongitude;
		cFilterId = filter == null ? null : filter.getFilterId();
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<Amenity> tempList = new ArrayList<Amenity>();
		int sleft = MapUtils.get31TileNumberX(cLeftLongitude);
		int sright = MapUtils.get31TileNumberX(cRightLongitude);
		int sbottom = MapUtils.get31TileNumberY(cBottomLatitude);
		int stop = MapUtils.get31TileNumberY(cTopLatitude);
		searchAmenities(stop, sleft, sbottom, sright, cZoom, filter, tempList, matcher);
		synchronized (this) {
			cachedObjects.clear();
			cachedObjects.addAll(tempList);
		}
	}

	@Override
	public synchronized boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, String filterId, List<Amenity> toFill, boolean fillFound){
		if (db == null) {
			return true;
		}
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && zoom == cZoom;
		boolean noNeedToSearch = inside &&  Algorithms.objectEquals(filterId, cFilterId);
		if((inside || fillFound) && toFill != null && Algorithms.objectEquals(filterId, cFilterId)){
			for(Amenity a : cachedObjects){
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
		return super.initialize(progress, file, IndexConstants.POI_TABLE_VERSION, IndexConstants.POI_TABLE, true);
	}
	
	// Update functionality
	public boolean addAmenity(Amenity a){
		insertAmenities(Collections.singleton(a));
		return true;
	}
	
	public boolean updateAmenity(Amenity a){
		StringBuilder b = new StringBuilder();
		b.append("UPDATE " + IndexConstants.POI_TABLE + " SET "); //$NON-NLS-1$ //$NON-NLS-2$
		b.append(" x = ?, "). //$NON-NLS-1$
		  append(" y = ?, "). //$NON-NLS-1$
		  append(" opening_hours = ?, "). //$NON-NLS-1$
		  append(" name = ?, "). //$NON-NLS-1$
		  append(" name_en = ?, ").//$NON-NLS-1$
		  append(" type = ?, "). //$NON-NLS-1$
		  append(" subtype = ? "). //$NON-NLS-1$
		  append(" site = ? "). //$NON-NLS-1$
		  append(" phone = ? "). //$NON-NLS-1$
		  append(" WHERE append( id = ?"); //$NON-NLS-1$
		
		db.execSQL(b.toString(),			
				new Object[] { MapUtils.get31TileNumberX(a.getLocation().getLongitude()), MapUtils.get31TileNumberY(a.getLocation().getLatitude()),  
			a.getOpeningHours(), a.getName(), a.getEnName(), AmenityType.valueToString(a.getType()), a.getSubType(),
			a.getSite(), a.getPhone(),  a.getId()});
		
		changes = true;
		return true;
	}
	
	public boolean deleteAmenities(long id){
		db.execSQL("DELETE FROM " + IndexConstants.POI_TABLE+ " WHERE id="+id); //$NON-NLS-1$ //$NON-NLS-2$
		changes = true;
		return true;
	}
	
	
	public boolean updateAmenities(List<Amenity> amenities, double leftLon, double topLat, double rightLon, double bottomLat){
		int l = MapUtils.get31TileNumberX(leftLon);
		int r = MapUtils.get31TileNumberX(rightLon);
		int t = MapUtils.get31TileNumberY(topLat);
		int b = MapUtils.get31TileNumberY(bottomLat);
		db.execSQL("DELETE FROM " + IndexConstants.POI_TABLE + " WHERE " + //$NON-NLS-1$ //$NON-NLS-2$
				" x >= ? AND ? >= x  AND " + //$NON-NLS-1$
				" y >= ? AND ? >= y ", new Integer[] { l, r, t, b }); //$NON-NLS-1$
		
		insertAmenities(amenities);
		return true;
	}

	private void insertAmenities(Collection<Amenity> amenities) {
		SQLiteStatement stat = db.compileStatement("DELETE FROM " + IndexConstants.POI_TABLE + " WHERE id = ?");  //$NON-NLS-1$//$NON-NLS-2$
		for (Amenity a : amenities) {
			stat.bindLong(1, a.getId());
			stat.execute();
		}
		stat.close();
		stat = db.compileStatement("INSERT INTO " + IndexConstants.POI_TABLE +  //$NON-NLS-1$
				"(id, x, y, name_en, name, type, subtype, opening_hours, site, phone) values(?,?,?,?,?,?,?,?,?,?)"); //$NON-NLS-1$
		for (Amenity a : amenities) {
			stat.bindLong(1, a.getId());
			stat.bindDouble(2, MapUtils.get31TileNumberX(a.getLocation().getLongitude()));
			stat.bindDouble(3, MapUtils.get31TileNumberY(a.getLocation().getLatitude()));
			dataBottomLatitude = Math.min(a.getLocation().getLatitude() - 0.5, dataBottomLatitude); 
			dataTopLatitude = Math.max(a.getLocation().getLatitude() + 0.5, dataTopLatitude);
			dataLeftLongitude = Math.min(a.getLocation().getLongitude() - 0.5, dataLeftLongitude);
			dataRightLongitude = Math.max(a.getLocation().getLongitude() + 0.5, dataRightLongitude);
			bindString(stat, 4, a.getEnName());
			bindString(stat, 5, a.getName());
			bindString(stat, 6, AmenityType.valueToString(a.getType()));
			bindString(stat, 7, a.getSubType());
			bindString(stat, 8 , a.getOpeningHours());
			bindString(stat, 9, a.getSite());
			bindString(stat, 10, a.getPhone());
			stat.execute();
		}
		stat.close();
		updateMaxMinBoundaries(IndexConstants.POI_TABLE);
		changes = true;
	}

	@Override
	public boolean hasChange() {
		return changes;
	}
	
	@Override
	public void clearChange() {
		changes = false;
	}
	
	private final static String SITE_API = "http://api.openstreetmap.org/"; //$NON-NLS-1$
	
	public static void createAmenityIndexRepository(File file) {
		SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY);
		db.execSQL("create table " + IndexConstants.POI_TABLE + //$NON-NLS-1$
				"(id bigint, x int, y int, name_en varchar(1024), name varchar(1024), "
				+ "type varchar(1024), subtype varchar(1024), opening_hours varchar(1024), phone varchar(1024), site varchar(1024),"
				+ "primary key(id, type, subtype))");
		db.execSQL("create index poi_loc on poi (x, y, type, subtype)");
		db.execSQL("create index poi_id on poi (id, type, subtype)");
		db.setVersion(IndexConstants.POI_TABLE_VERSION);
		db.close();
	}
	
	public static boolean loadingPOIs(List<Amenity> amenities, double leftLon, double topLat, double righLon, double bottomLat) {
		try {
			// bbox=left,bottom,right,top
			String u = SITE_API+"api/0.6/map?bbox="+leftLon+","+bottomLat+","+righLon+","+topLat;  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			URL url = new URL(u);
			log.info("Start loading poi : " + u); //$NON-NLS-1$
			InputStream is = url.openStream();
			OsmBaseStorage st = new OsmBaseStorage();
			final Map<Amenity, Entity> amen = new LinkedHashMap<Amenity, Entity>();
			final List<Amenity> tempList = new ArrayList<Amenity>();
			final MapRenderingTypes def = MapRenderingTypes.getDefault();
			st.getFilters().add(new IOsmStorageFilter(){
				@Override
				public boolean acceptEntityToLoad(OsmBaseStorage storage, Entity.EntityId id, Entity entity) {
					EntityParser.parseAmenities(def, entity, tempList);
					if(!tempList.isEmpty()){
						for(Amenity a : tempList){
							amen.put(a, entity);
						}
						tempList.clear();
						return true;
					}
					// to 
					return entity instanceof Node;
				}
			});
			st.parseOSM(is, null, null, false);
			for (Amenity am : amen.keySet()) {
				// update location (when all nodes of way are loaded)
				EntityParser.parseAmenity(am, amen.get(am));
				if(am.getEnName().length() == 0){
					am.setEnName(Junidecode.unidecode(am.getName()));
				}
				amenities.add(am);
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
