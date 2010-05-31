package com.osmand;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.Street;
import com.osmand.data.City.CityType;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.index.IndexConstants.IndexBuildingTable;
import com.osmand.data.index.IndexConstants.IndexCityTable;
import com.osmand.data.index.IndexConstants.IndexStreetTable;


public class RegionAddressRepository {
	private static final Log log = LogUtil.getLog(RegionAddressRepository.class);
	private SQLiteDatabase db;
	private String name;
	private LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	private TreeMap<CityType, List<City>> cityTypes = new TreeMap<CityType, List<City>>();
	
	public void initialize(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		if(db != null){
			// close previous db
			db.close();
		}
		db = SQLiteDatabase.openOrCreateDatabase(file, null);
		name = file.getName().substring(0, file.getName().indexOf('.'));
		if (log.isDebugEnabled()) {
			log.debug("Initializing address db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms");
		}
	}
	
	public void close(){
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
	}
	
	public City getCityById(Long id){
		if(id == -1){
			// do not preload cities for that case
			return null;
		}
		preloadCities();
		return cities.get(id); 
	}
	
	public Street getStreetByName(City city, String name){
		if(city.isEmptyWithStreets()){
			preloadStreets(city);
		}
		return city.getStreet(name);
	}
	
	public Building getBuildingByName(Street street, String name){
		if(street.getBuildings().isEmpty()){
			preloadBuildings(street);
		}
		for(Building b : street.getBuildings()){
			if(b.getName().equals(name)){
				return b;
			}
		}
		return null;
	}
	
	public void fillWithSuggestedBuildings(Street street, String name, List<Building> buildingsToFill){
		preloadBuildings(street);
		name = name.toLowerCase();
		int ind = 0;
		if(name.length() == 0){
			buildingsToFill.addAll(street.getBuildings());
			return;
		}
		for (Building building : street.getBuildings()) {
			String lowerCase = building.getName().toLowerCase();
			if (lowerCase.startsWith(name)) {
				buildingsToFill.add(ind, building);
				ind++;
			} else if (lowerCase.contains(name)) {
				buildingsToFill.add(building);
			}
		}
	}
	
	public void fillWithSuggestedStreets(City c, String name, List<Street> streetsToFill){
		preloadStreets(c);
		name = name.toLowerCase();
		int ind = 0;
		if(name.length() == 0){
			streetsToFill.addAll(c.getStreets());
			return;
		}
		for (Street s : c.getStreets()) {
			String lowerCase = s.getName().toLowerCase();
			if (lowerCase.startsWith(name)) {
				streetsToFill.add(ind, s);
				ind++;
			} else if (lowerCase.contains(name)) {
				streetsToFill.add(s);
			}
		}
	}
	
	public void fillWithSuggestedCities(String name, List<City> citiesToFill){
		preloadCities();
		if(name.length() < 3){
			EnumSet<CityType> set = EnumSet.of(CityType.CITY, CityType.TOWN); 
			for(CityType t : set){
				if(name.length() == 0){
					citiesToFill.addAll(cityTypes.get(t));
				} else {
					name = name.toLowerCase();
					for (City c : cityTypes.get(t)) {
						String lowerCase = c.getName().toLowerCase();
						if(lowerCase.startsWith(name)){
							citiesToFill.add(c);
						}
					}
				}
			}
		} else {
			// essentially index is created that cities towns are first in cities map
			int ind = 0;
			name = name.toLowerCase();
			Collection<City> src = cities.values();
			for (City c : src) {
				String lowerCase = c.getName().toLowerCase();
				if (lowerCase.startsWith(name)) {
					citiesToFill.add(ind, c);
					ind++;
				} else if (lowerCase.contains(name)) {
					citiesToFill.add(c);
				}
			}
			int initialsize = citiesToFill.size();
			log.debug("Start loading cities for " +getName() + " filter " + name);
			// lower function in SQLite requires ICU extension
			name = Algoritms.capitalizeFirstLetterAndLowercase(name);
			StringBuilder where = new StringBuilder(80);
			where.
				  append(IndexCityTable.CITY_TYPE.toString()).append(" not in (").
				  append('\'').append(CityType.valueToString(CityType.CITY)).append('\'').append(", ").
				  append('\'').append(CityType.valueToString(CityType.TOWN)).append('\'').append(") and ").
				  append(IndexCityTable.NAME.toString()).append(" LIKE '"+name+"%'");
			Cursor query = db.query(IndexCityTable.getTable(), IndexConstants.generateColumnNames(IndexCityTable.values()), 
					where.toString(), null, null, null, null);
			if (query.moveToFirst()) {
				do {
					citiesToFill.add(parseCityFromCursor(query));
				} while (query.moveToNext());
			}
			query.close();

			
			log.debug("Loaded citites " + (citiesToFill.size() - initialsize));
		}
	}
	
	public void preloadBuildings(Street street){
		if (street.getBuildings().isEmpty()) {
			Cursor query = db.query(IndexBuildingTable.getTable(), IndexConstants.generateColumnNames(IndexBuildingTable.values()), "? = street",
					new String[] { street.getId() + "" }, null, null, null);
			log.debug("Start loading buildings for "  + street.getName());
			if (query.moveToFirst()) {
				do {
					Building building = new Building();
					building.setId(query.getLong(IndexBuildingTable.ID.ordinal()));
					building.setLocation(query.getDouble(IndexBuildingTable.LATITUDE.ordinal()), query.getDouble(IndexBuildingTable.LONGITUDE
							.ordinal()));
					building.setName(query.getString(IndexBuildingTable.NAME.ordinal()));
					street.registerBuilding(building);
				} while (query.moveToNext());
			}
			query.close();
			log.debug("Loaded " + street.getBuildings().size() + " buildings");
		}
	}
	
	public void preloadStreets(City city){
		if (city.isEmptyWithStreets()) {
			log.debug("Start loading streets for "  + city.getName());
			Cursor query = db.query(IndexStreetTable.getTable(), IndexConstants.generateColumnNames(IndexStreetTable.values()), "? = city",
					new String[] { city.getId() + "" }, null, null, null);
			if (query.moveToFirst()) {
				do {
					Street street = new Street(city);
					street.setId(query.getLong(IndexStreetTable.ID.ordinal()));
					street.setLocation(query.getDouble(IndexStreetTable.LATITUDE.ordinal()), query.getDouble(IndexStreetTable.LONGITUDE
							.ordinal()));
					street.setName(query.getString(IndexStreetTable.NAME.ordinal()));
					city.registerStreet(street);
				} while (query.moveToNext());
			}
			query.close();
			log.debug("Loaded " + city.getStreets().size() + " streets");
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
			city.setLocation(query.getDouble(IndexCityTable.LATITUDE.ordinal()), query.getDouble(IndexCityTable.LONGITUDE
					.ordinal()));
			city.setName(query.getString(IndexCityTable.NAME.ordinal()));
			return city;
		}
		return null;
	}
	
	public void preloadCities(){
		if (cities.isEmpty()) {
			log.debug("Start loading cities for " +getName());
			StringBuilder where = new StringBuilder();
			where.append(IndexCityTable.CITY_TYPE.toString()).append('=').
				  append('\'').append(CityType.valueToString(CityType.CITY)).append('\'').append(" or ").
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
			log.debug("Loaded " + cities.size() + " cities");
			query.close();
		}
	}
}
