package net.osmand;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.CityComparator;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.data.City.CityType;
import net.osmand.data.index.IndexConstants;
import net.osmand.data.index.IndexConstants.IndexBuildingTable;
import net.osmand.data.index.IndexConstants.IndexCityTable;
import net.osmand.data.index.IndexConstants.IndexStreetNodeTable;
import net.osmand.data.index.IndexConstants.IndexStreetTable;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public class RegionAddressRepository {
	private static final Log log = LogUtil.getLog(RegionAddressRepository.class);
	private SQLiteDatabase db;
	private String name;
	private LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	
	private Map<CityType, List<City>> cityTypes = new HashMap<CityType, List<City>>();
	private Map<String, PostCode> postCodes = new TreeMap<String, PostCode>(Collator.getInstance());
	
	private boolean useEnglishNames = false;
	
	public static class MapObjectNameDistanceComparator implements Comparator<MapObject> {
		
		private final boolean useEnName;
		private Collator collator = Collator.getInstance();
		private final LatLon location;

		public MapObjectNameDistanceComparator(boolean useEnName, LatLon location){
			this.useEnName = useEnName;
			this.location = location;
		}

		@Override
		public int compare(MapObject object1, MapObject object2) {
			if(object1 == null || object2 == null){
				return object2 == object1 ? 0 : (object1 == null ? -1 : 1); 
			} else {
				int c = collator.compare(object1.getName(useEnName), object2.getName(useEnName));
				if(c == 0 && location != null){
					LatLon l1 = object1.getLocation();
					LatLon l2 = object2.getLocation();
					if(l1 == null || l2 == null){
						return l2 == l1 ? 0 : (l1 == null ? -1 : 1);
					}
					return Double.compare(MapUtils.getDistance(location, l1), MapUtils.getDistance(location, l2));
				}
				return c;
			}
		}
		
	}
	
	
	public boolean initialize(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		if(db != null){
			// close previous db
			db.close();
		}
		db = SQLiteDatabase.openOrCreateDatabase(file, null);
		name = file.getName().substring(0, file.getName().indexOf('.'));
		if(db.getVersion() != IndexConstants.ADDRESS_TABLE_VERSION){
			db.close();
			db = null;
			return false;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Initializing address db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return true;
	}
	
	public void close(){
		clearCities();
		if(db != null){
			db.close();
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void clearCities(){
		cities.clear();
		cityTypes.clear();
		postCodes.clear();
	}
	
	public boolean areCitiesPreloaded(){
		return !cities.isEmpty();
	}
	public boolean arePostcodesPreloaded(){
		return !postCodes.isEmpty();
	}
	
	public PostCode getPostcode(String name){
		if(name == null){
			return null;
		}
		preloadPostcodes();
		return postCodes.get(name.toUpperCase());
	}
	
	public City getCityById(Long id){
		if(id == -1){
			// do not preload cities for that case
			return null;
		}
		preloadCities();
		return cities.get(id); 
	}
	
	
	public Street getStreetByName(MapObject city, String name) {
		preloadStreets(city);
		if (city instanceof City) {
			return ((City) city).getStreet(name);
		} else {
			return ((PostCode) city).getStreet(name);
		}
	}
	
	public Building getBuildingByName(Street street, String name){
		if(street.getBuildings().isEmpty()){
			preloadBuildings(street);
		}
		for(Building b : street.getBuildings()){
			String bName = useEnglishNames ? b.getEnName() : b.getName();
			if(bName.equals(name)){
				return b;
			}
		}
		return null;
	}
	
	public boolean useEnglishNames(){
		return useEnglishNames;
	}
	
	public void setUseEnglishNames(boolean useEnglishNames) {
		this.useEnglishNames = useEnglishNames;
		// sort streets
		for (City c : cities.values()) {
			if (!c.isEmptyWithStreets()) {
				ArrayList<Street> list = new ArrayList<Street>(c.getStreets());
				c.removeAllStreets();
				for (Street s : list) {
					c.registerStreet(s, useEnglishNames);
				}
			}
		}
		// sort cities
		ArrayList<City> list = new ArrayList<City>(cities.values());
		Collections.sort(list,  new CityComparator(useEnglishNames));
		cities.clear();
		cityTypes.clear();
		for(City c : list){
			registerCity(c);
		}
	}
	
	public void fillWithSuggestedBuildings(PostCode postcode, Street street, String name, List<Building> buildingsToFill){
		preloadBuildings(street);
		name = name.toLowerCase();
		int ind = 0;
		boolean empty = name.length() == 0;
		if(empty && postcode == null){
			buildingsToFill.addAll(street.getBuildings());
			return;
		}
		for (Building building : street.getBuildings()) {
			if(postcode != null && !postcode.getName().equals(building.getPostcode())){
				continue;
			} else if(empty){
				buildingsToFill.add(building);
				continue;
			}
			String bName = useEnglishNames ? building.getEnName() : building.getName();
			String lowerCase = bName.toLowerCase();
			if (lowerCase.startsWith(name)) {
				buildingsToFill.add(ind, building);
				ind++;
			} else if (lowerCase.contains(name)) {
				buildingsToFill.add(building);
			}
		}
	}
	
	public void fillWithSuggestedStreetsIntersectStreets(City city, Street st, List<Street> streetsToFill) {
		if (st != null) {
			Set<Long> strIds = new TreeSet<Long>();
			log.debug("Start loading instersection streets for " + city.getName()); //$NON-NLS-1$
			Cursor query = db.rawQuery("SELECT B.STREET FROM street_node A JOIN street_node B ON A.ID = B.ID WHERE A.STREET = ?",  //$NON-NLS-1$
					new String[] { st.getId() + "" }); //$NON-NLS-1$
			if (query.moveToFirst()) {
				do {
					if (st.getId() != query.getLong(0)) {
						strIds.add(query.getLong(0));
					}
				} while (query.moveToNext());
			}
			query.close();
			for (Street s : city.getStreets()) {
				if (strIds.contains(s.getId())) {
					streetsToFill.add(s);
				}
			}
			log.debug("Loaded " + strIds.size() + " streets"); //$NON-NLS-1$ //$NON-NLS-2$
			preloadWayNodes(st);
		}
	}
	
	
	public void fillWithSuggestedStreets(MapObject o, String name, List<Street> streetsToFill){
		assert o instanceof PostCode || o instanceof City;
		City city = (City) (o instanceof City ? o : null); 
		PostCode post = (PostCode) (o instanceof PostCode ? o : null);
		preloadStreets(o);
		name = name.toLowerCase();
		
		Collection<Street> streets = post == null ? city.getStreets() : post.getStreets() ; 
		int ind = 0;
		if(name.length() == 0){
			streetsToFill.addAll(streets);
			return;
		}
		ind = 0;
		for (Street s : streets) {
			String sName = useEnglishNames ? s.getEnName() : s.getName();
			String lowerCase = sName.toLowerCase();
			if (lowerCase.startsWith(name)) {
				streetsToFill.add(ind, s);
				ind++;
			} else if (lowerCase.contains(name)) {
				streetsToFill.add(s);
			}
		}
	}
	
	public void fillWithSuggestedCities(String name, List<MapObject> citiesToFill, LatLon currentLocation){
		preloadCities();
		// essentially index is created that cities towns are first in cities map
		int ind = 0;
		if (name.length() >= 2 &&
				   Character.isDigit(name.charAt(0)) &&
				   Character.isDigit(name.charAt(1))) {
			preloadPostcodes();
			// also try to identify postcodes
			String uName = name.toUpperCase();
			for (String code : postCodes.keySet()) {
				if (code.startsWith(uName)) {
					citiesToFill.add(ind++, postCodes.get(code));
				} else if(code.contains(uName)){
					citiesToFill.add(postCodes.get(code));
				}
			}
			
		}
		if(name.length() < 3){
			EnumSet<CityType> set = EnumSet.of(CityType.CITY, CityType.TOWN); 
			for(CityType t : set){
				List<City> list = cityTypes.get(t);
				if(list == null){
					continue;
				}
				if(name.length() == 0){
					citiesToFill.addAll(list);
				} else {
					name = name.toLowerCase();
					for (City c : list) {
						String cName = useEnglishNames ? c.getEnName() : c.getName();
						String lowerCase = cName.toLowerCase();
						if(lowerCase.startsWith(name)){
							citiesToFill.add(c);
						}
					}
				}
			}
		} else {
			name = name.toLowerCase();
			Collection<City> src = cities.values();
			for (City c : src) {
				String cName = useEnglishNames ? c.getEnName() : c.getName();
				String lowerCase = cName.toLowerCase();
				if (lowerCase.startsWith(name)) {
					citiesToFill.add(ind, c);
					ind++;
				} else if (lowerCase.contains(name)) {
					citiesToFill.add(c);
				}
			}
			int initialsize = citiesToFill.size();
			log.debug("Start loading cities for " +getName() + " filter " + name); //$NON-NLS-1$ //$NON-NLS-2$
			// lower function in SQLite requires ICU extension
			name = Algoritms.capitalizeFirstLetterAndLowercase(name);
			StringBuilder where = new StringBuilder(80);
			where.
				  append(IndexCityTable.CITY_TYPE.toString()).append(" not in ("). //$NON-NLS-1$
				  append('\'').append(CityType.valueToString(CityType.CITY)).append('\'').append(", "). //$NON-NLS-1$
				  append('\'').append(CityType.valueToString(CityType.TOWN)).append('\'').append(") and "). //$NON-NLS-1$
				  append(useEnglishNames ? IndexCityTable.NAME_EN.toString() : IndexCityTable.NAME.toString()).append(" LIKE '"+name+"%'"); //$NON-NLS-1$ //$NON-NLS-2$
			Cursor query = db.query(IndexCityTable.getTable(), IndexConstants.generateColumnNames(IndexCityTable.values()), 
					where.toString(), null, null, null, null);
			if (query.moveToFirst()) {
				List<City> hamlets = new ArrayList<City>();
				do {
					hamlets.add(parseCityFromCursor(query));
				} while (query.moveToNext());
				Collections.sort(hamlets, new MapObjectNameDistanceComparator(useEnglishNames, currentLocation));
				citiesToFill.addAll(hamlets);
			}
			query.close();
			
			
			
			log.debug("Loaded citites " + (citiesToFill.size() - initialsize)); //$NON-NLS-1$
		}
	}
	
    public void preloadWayNodes(Street street){
    	if(street.getWayNodes().isEmpty()){
    		Cursor query = db.query(IndexStreetNodeTable.getTable(), IndexConstants.generateColumnNames(IndexStreetNodeTable.values()), "? = street", //$NON-NLS-1$
					new String[] { street.getId() + "" }, null, null, null); //$NON-NLS-1$
			log.debug("Start loading waynodes for "  + street.getName()); //$NON-NLS-1$
			Map<Long, Way> ways = new LinkedHashMap<Long, Way>();
			if (query.moveToFirst()) {
				do {
					Node n = new Node(query.getDouble(IndexStreetNodeTable.LATITUDE.ordinal()),
							query.getDouble(IndexBuildingTable.LONGITUDE.ordinal()),
							query.getLong(IndexStreetNodeTable.ID.ordinal()));
					long way = query.getLong(IndexStreetNodeTable.WAY.ordinal());
					if(!ways.containsKey(way)){
						ways.put(way, new Way(way));
					}
					ways.get(way).addNode(n);
				} while (query.moveToNext());
			}
			query.close();
			for(Way w : ways.values()){
				street.getWayNodes().add(w);
			}
			log.debug("Loaded " + ways.size() + " ways"); //$NON-NLS-1$ //$NON-NLS-2$
    	}
		
	}
    
    public void preloadPostcodes() {
    	if (postCodes.isEmpty()) {
			// check if it possible to load postcodes
			Cursor query = db.query(true, IndexBuildingTable.getTable(), new String[] { IndexBuildingTable.POSTCODE.toString() }, null,
					null, null, null, null, null);
			log.debug("Start loading postcodes for "); //$NON-NLS-1$
			if (query.moveToFirst()) {
				do {
					String postcode = query.getString(0);
					if (postcode != null) {
						postCodes.put(postcode, new PostCode(postcode));
					}
				} while (query.moveToNext());
			}
			query.close();
			log.debug("Loaded " + postCodes.size() + " postcodes "); //$NON-NLS-1$ //$NON-NLS-2$
		}
    }
	
	public void preloadBuildings(Street street){
		if (street.getBuildings().isEmpty()) {
			Cursor query = db.query(IndexBuildingTable.getTable(), IndexConstants.generateColumnNames(IndexBuildingTable.values()), "? = street", //$NON-NLS-1$
						new String[] { street.getId() + "" }, null, null, null); //$NON-NLS-1$
			log.debug("Start loading buildings for "  + street.getName()); //$NON-NLS-1$
			if (query.moveToFirst()) {
				do {
					Building building = new Building();
					building.setId(query.getLong(IndexBuildingTable.ID.ordinal()));
					building.setLocation(query.getDouble(IndexBuildingTable.LATITUDE.ordinal()), query.getDouble(IndexBuildingTable.LONGITUDE
							.ordinal()));
					building.setName(query.getString(IndexBuildingTable.NAME.ordinal()));
					building.setEnName(query.getString(IndexBuildingTable.NAME_EN.ordinal()));
					building.setPostcode(query.getString(IndexBuildingTable.POSTCODE.ordinal()));
					street.registerBuilding(building);
				} while (query.moveToNext());
				street.sortBuildings();
			}
			query.close();
			log.debug("Loaded " + street.getBuildings().size() + " buildings"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public void preloadStreets(MapObject o){
		assert o instanceof PostCode || o instanceof City;
		City city = (City) (o instanceof City ? o : null); 
		PostCode post = (PostCode) (o instanceof PostCode ? o : null);
		if (city != null && city.isEmptyWithStreets()) {
			log.debug("Start loading streets for "  + city.getName()); //$NON-NLS-1$
			Cursor query = db.query(IndexStreetTable.getTable(), IndexConstants.generateColumnNames(IndexStreetTable.values()), "? = city", //$NON-NLS-1$
					new String[] { city.getId() + "" }, null, null, null); //$NON-NLS-1$
			if (query.moveToFirst()) {
				do {
					Street street = new Street(city);
					street.setId(query.getLong(IndexStreetTable.ID.ordinal()));
					street.setLocation(query.getDouble(IndexStreetTable.LATITUDE.ordinal()), query.getDouble(IndexStreetTable.LONGITUDE.ordinal()));
					street.setName(query.getString(IndexStreetTable.NAME.ordinal()));
					street.setEnName(query.getString(IndexStreetTable.NAME_EN.ordinal()));
					city.registerStreet(street, useEnglishNames);
				} while (query.moveToNext());
			}
			query.close();
			log.debug("Loaded " + city.getStreets().size() + " streets"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if(post != null && post.isEmptyWithStreets()){
			log.debug("Start loading streets for "  + post.getName()); //$NON-NLS-1$
			Cursor query = db.rawQuery("SELECT B.CITY, B.ID,B.LATITUDE, B.LONGITUDE, B.NAME, B.NAME_EN FROM building A JOIN street B ON A.street = B.ID WHERE A.postcode = ?",  //$NON-NLS-1$
					new String[] { post.getName() + "" }); //$NON-NLS-1$
			if (query.moveToFirst()) {
				do {
					city = getCityById(query.getLong(0));
					Street street = null;
					if(city != null){
						preloadStreets(city);
						street = city.getStreet(useEnglishNames ? query.getString(5) : query.getString(4));
					} 
					if(street == null){
						street = new Street(city);
						street.setId(query.getLong(1));
						street.setLocation(query.getDouble(2), query.getDouble(3));
						street.setName(query.getString(4));
						street.setEnName(query.getString(5));
						if(city != null){
							city.registerStreet(street, useEnglishNames);
						}
					}
					post.registerStreet(street, useEnglishNames);
				} while (query.moveToNext());
			}
			query.close();
			log.debug("Loaded " +post.getStreets().size() + " streets"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public void registerCity(City city){
		cities.put(city.getId(), city);
		
		if(!cityTypes.containsKey(city.getType())){
			cityTypes.put(city.getType(), new ArrayList<City>());
		}
		cityTypes.get(city.getType()).add(city);
	}
	
	protected City parseCityFromCursor(Cursor query){
		CityType type = CityType.valueFromString(query.getString(IndexCityTable.CITY_TYPE.ordinal()));
		if (type != null) {
			City city = new City(type);
			city.setId(query.getLong(IndexCityTable.ID.ordinal()));
			city.setLocation(query.getDouble(IndexCityTable.LATITUDE.ordinal()), 
					query.getDouble(IndexCityTable.LONGITUDE.ordinal()));
			city.setName(query.getString(IndexCityTable.NAME.ordinal()));
			city.setEnName(query.getString(IndexCityTable.NAME_EN.ordinal()));
			return city;
		}
		return null;
	}
	
	public void preloadCities(){
		if (cities.isEmpty()) {
			log.debug("Start loading cities for " +getName()); //$NON-NLS-1$
			StringBuilder where = new StringBuilder();
			where.append(IndexCityTable.CITY_TYPE.toString()).append('=').
				  append('\'').append(CityType.valueToString(CityType.CITY)).append('\'').append(" or "). //$NON-NLS-1$
				  append(IndexCityTable.CITY_TYPE.toString()).append('=').
				  append('\'').append(CityType.valueToString(CityType.TOWN)).append('\'');
			Cursor query = db.query(IndexCityTable.getTable(), IndexConstants.generateColumnNames(IndexCityTable.values()), 
					where.toString(), null, null, null, null);
			if(query.moveToFirst()){
				do {
					City city = parseCityFromCursor(query);
					if (city != null) {
						cities.put(city.getId(), city);
						
						if(!cityTypes.containsKey(city.getType())){
							cityTypes.put(city.getType(), new ArrayList<City>());
						}
						cityTypes.get(city.getType()).add(city);
					}
					
				} while(query.moveToNext());
			}
			log.debug("Loaded " + cities.size() + " cities"); //$NON-NLS-1$ //$NON-NLS-2$
			query.close();
		}
	}
}
