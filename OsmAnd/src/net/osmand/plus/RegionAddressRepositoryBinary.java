package net.osmand.plus;


import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.LogUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;

import org.apache.commons.logging.Log;


public class RegionAddressRepositoryBinary implements RegionAddressRepository {
	private static final Log log = LogUtil.getLog(RegionAddressRepositoryBinary.class);
	private BinaryMapIndexReader file;
	private String region;
	
	
	private final LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	private final Map<String, City> postCodes;
	private boolean useEnglishNames = false;
	private final Collator collator;
	
	public RegionAddressRepositoryBinary(BinaryMapIndexReader file, String name) {
		this.file = file;
		this.region = name;
 	    this.collator = Collator.getInstance(Locale.US);
 	    this.collator.setStrength(Collator.PRIMARY); //ignores also case
 	    Collator sortcollator = Collator.getInstance();
 	    sortcollator.setStrength(Collator.PRIMARY); //ignores also case
		this.postCodes = new TreeMap<String, City>(sortcollator);
	}
	
	@Override
	public void close(){
		this.file = null;
	}

	
	@Override
	public synchronized void preloadCities(ResultMatcher<City> resultMatcher) {
		if (cities.isEmpty()) {
			try {
				List<City> cs = file.getCities(region, BinaryMapIndexReader.buildAddressRequest(resultMatcher), 
						BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
				for (City c : cs) {
					cities.put(c.getId(), c);
				}
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
			}
		}
	}

	@Override
	public synchronized void preloadBuildings(Street street, ResultMatcher<Building> resultMatcher) {
		if(street.getBuildings().isEmpty()){
			try {
				file.preloadBuildings(street, BinaryMapIndexReader.buildAddressRequest(resultMatcher));
				street.sortBuildings();
			} catch (IOException e) {
				log.error("Disk operation failed" , e); //$NON-NLS-1$
			}
		}		
	}
	
	@Override
	public synchronized void addCityToPreloadedList(City city) {
		cities.put(city.getId(), city);
	}
	
	@Override
	public synchronized List<City> getLoadedCities(){
		return new ArrayList<City>(cities.values());
	}
	
	@Override
	public synchronized void preloadStreets(City o, ResultMatcher<Street> resultMatcher) {
		Collection<Street> streets = o.getStreets();
		if(!streets.isEmpty()){
			return;
		}
		try {
			file.preloadStreets(o, BinaryMapIndexReader.buildAddressRequest(resultMatcher));
		} catch (IOException e) {
			log.error("Disk operation failed" , e); //$NON-NLS-1$
		}
		
	}

//	// not use ccontains It is really slow, takes about 10 times more than other steps
//	private StringMatcherMode[] streetsCheckMode = new StringMatcherMode[] {StringMatcherMode.CHECK_ONLY_STARTS_WITH,
//			StringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING};
	
	@Override
	public List<MapObject> searchMapObjectsByName(String name, ResultMatcher<MapObject> resultMatcher) {
		SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(resultMatcher, name);
		try {
			file.searchAddressDataByName(req);
		} catch (IOException e) {
			log.error("Disk operation failed", e); //$NON-NLS-1$
		}
		return req.getSearchResults();
	}


	@Override
	public List<City> fillWithSuggestedCities(String name, ResultMatcher<City> resultMatcher, LatLon currentLocation) {
		List<City> citiesToFill = new ArrayList<City>();
		if (cities.isEmpty()) {
			preloadCities(resultMatcher);
			citiesToFill.addAll(cities.values());
			return citiesToFill;
		}

		preloadCities(null);
		if (name.length() == 0) {
			citiesToFill.addAll(cities.values());
			return citiesToFill;
		}
		try {
			// essentially index is created that cities towns are first in cities map
			if (name.length() >= 2 && Algoritms.containsDigit(name)) {
				// also try to identify postcodes
				String uName = name.toUpperCase();
				for (City code : file.getCities(region, BinaryMapIndexReader.buildAddressRequest(resultMatcher), 
						new CollatorStringMatcher(collator, uName, StringMatcherMode.CHECK_CONTAINS), false, 
						BinaryMapAddressReaderAdapter.POSTCODES_TYPE)) {
					citiesToFill.add(code);
					if (resultMatcher.isCancelled()) {
						return citiesToFill;
					}
				}

			}
			name = name.toLowerCase();
			for (City c : cities.values()) {
				String cName = c.getName(useEnglishNames); // lower case not needed, collator ensures that
				if (CollatorStringMatcher.cmatches(collator, cName, name, StringMatcherMode.CHECK_STARTS_FROM_SPACE)) {
					if (resultMatcher.publish(c)) {
						citiesToFill.add(c);
					}
				}
				if (resultMatcher.isCancelled()) {
					return citiesToFill;
				}
			}

			int initialsize = citiesToFill.size();
			if (name.length() >= 3) {
				for (City c : file.getCities(region, BinaryMapIndexReader.buildAddressRequest(resultMatcher),
						new CollatorStringMatcher(collator, name,StringMatcherMode.CHECK_STARTS_FROM_SPACE), useEnglishNames, 
						BinaryMapAddressReaderAdapter.VILLAGES_TYPE)) {
					citiesToFill.add(c);
					if (resultMatcher.isCancelled()) {
						return citiesToFill;
					}
				}
			}
			log.debug("Loaded citites " + (citiesToFill.size() - initialsize)); //$NON-NLS-1$
		} catch (IOException e) {
			log.error("Disk operation failed", e); //$NON-NLS-1$
		}
		return citiesToFill;
	}

	@Override
	public List<Street> getStreetsIntersectStreets(Street st) {
		preloadBuildings(st, null);
		return st.getIntersectedStreets();
	}
	

	@Override
	public Building getBuildingByName(Street street, String name) {
		preloadBuildings(street, null);
		for (Building b : street.getBuildings()) {
			String bName = b.getName(useEnglishNames);
			if (bName.equals(name)) {
				return b;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return region;
	}
	
	@Override
	public String toString() {
		return getName() + " repository";
	}

	@Override
	public boolean useEnglishNames() {
		return useEnglishNames;
	}
	
	@Override
	public City getCityById(final long id, String name) {
		if (id == -1) {
			// do not preload cities for that case
			return null;
		}
		if(id < -1 && name != null){
			name = name.toUpperCase();
		}
		final String cmpName = name;
		preloadCities(null);
		if (!cities.containsKey(id)) {
			try {
				file.getCities(region, BinaryMapIndexReader.buildAddressRequest(new ResultMatcher<City>() {
					boolean canceled = false;

					@Override
					public boolean isCancelled() {
						return canceled;
					}

					@Override
					public boolean publish(City object) {
						if(id < -1) {
							if(object.getName().toUpperCase().equals(cmpName)) {
								addCityToPreloadedList(object);
								canceled = true;
							}
						} else if (object.getId() != null && object.getId().longValue() == id) {
							addCityToPreloadedList((City) object);
							canceled = true;
						}
						return false;
					}
				}), id < -1 ? BinaryMapAddressReaderAdapter.POSTCODES_TYPE : BinaryMapAddressReaderAdapter.VILLAGES_TYPE);
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
			}
		}
		return cities.get(id);
	}


	@Override
	public Street getStreetByName(City o, String name) {
		name = name.toLowerCase();
		preloadStreets(o, null);
		Collection<Street> streets = o.getStreets() ;
		for (Street s : streets) {
			String sName = useEnglishNames ? s.getEnName() : s.getName(); //lower case not needed, collator ensures that
			if (collator.equals(sName,name)) {
				return s;
			}
		}
		return null;
	}



	@Override
	public void setUseEnglishNames(boolean useEnglishNames) {
		this.useEnglishNames = useEnglishNames;
	}

	@Override
	public void clearCache() {
		cities.clear();
		postCodes.clear();
		
	}

	@Override
	public LatLon getEstimatedRegionCenter() {
		return file.getRegionCenter(region);
	}

	
	
}
