package net.osmand.plus.resources;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.Street;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;


public class RegionAddressRepositoryBinary implements RegionAddressRepository {
	private static final Log log = PlatformUtil.getLog(RegionAddressRepositoryBinary.class);
	private BinaryMapIndexReader file;
	
	
	private LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	private int ZOOM_QTREE = 10;
	private QuadTree<City> citiesQtree = new QuadTree<City>(new QuadRect(0, 0, 1 << (ZOOM_QTREE + 1),
			1 << (ZOOM_QTREE + 1)), 8, 0.55f);
	private final Map<String, City> postCodes;
	private final Collator collator;
	private String fileName;
	private ResourceManager mgr;
	private OsmandPreference<String> langSetting;
	
	public RegionAddressRepositoryBinary(ResourceManager mgr, BinaryMapIndexReader file, String fileName) {
		this.mgr = mgr;
		langSetting = mgr.getContext().getSettings().MAP_PREFERRED_LOCALE;
		this.file = file;
		this.fileName = fileName;
 	    this.collator = OsmAndCollator.primaryCollator();
		this.postCodes = new TreeMap<String, City>(OsmAndCollator.primaryCollator());
	}
	
	@Override
	public void close(){
		this.file = null;
	}
	
	@Override
	public BinaryMapIndexReader getFile() {
		return file;
	}
	
	@Override
	public synchronized List<GeocodingResult> justifyReverseGeocodingSearch(GeocodingResult r, double minBuildingDistance, final ResultMatcher<GeocodingResult> result) {
		try {
			return new GeocodingUtilities().justifyReverseGeocodingSearch(r, file, minBuildingDistance, result);
		} catch(IOException e) {
			log.error("Disk operation failed", e); //$NON-NLS-1$
		}
		return Collections.emptyList();
	}

	
	@Override
	public synchronized void preloadCities(ResultMatcher<City> resultMatcher) {
		if (cities.isEmpty()) {
			try {
				List<City> cs = file.getCities(BinaryMapIndexReader.buildAddressRequest(resultMatcher), 
						BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
				LinkedHashMap<Long, City> ncities = new LinkedHashMap<Long, City>();
				for (City c : cs) {
					ncities.put(c.getId(), c);
					LatLon loc = c.getLocation();
					if(loc != null) {
						int y31 = MapUtils.get31TileNumberY(loc.getLatitude());
						int x31 = MapUtils.get31TileNumberX(loc.getLongitude());
						int dz = (31 - ZOOM_QTREE);
						citiesQtree.insert(c, new QuadRect((x31 >> dz) - 1, (y31 >> dz) - 1, (x31 >> dz) + 1, (y31 >> dz) + 1));
					}
				}
				cities = ncities;
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
			}
		}
	}
	
	public City getClosestCity(LatLon l, List<City> cache) {
		City closest = null;
		if (l != null) {
			int y31 = MapUtils.get31TileNumberY(l.getLatitude());
			int x31 = MapUtils.get31TileNumberX(l.getLongitude());
			int dz = (31 - ZOOM_QTREE);
			if (cache == null) {
				cache = new ArrayList<City>();
			}
			cache.clear();
			citiesQtree.queryInBox(new QuadRect((x31 >> dz) - 1, (y31 >> dz) - 1, (x31 >> dz) + 1, (y31 >> dz) + 1),
					cache);
			int min = -1;

			for (City c : cache) {
				double d = MapUtils.getDistance(l, c.getLocation());
				if (min == -1 || d < min) {
					min = (int) d;
					closest = c;
				}
			}
		}
		return closest;

	}

	@Override
	public synchronized void preloadBuildings(Street street, ResultMatcher<Building> resultMatcher) {
		if(street.getBuildings().isEmpty() && street.getIntersectedStreets().isEmpty()){
			try {
				file.preloadBuildings(street, BinaryMapIndexReader.buildAddressRequest(resultMatcher));
				street.sortBuildings();
			} catch (IOException e) {
				log.error("Disk operation failed" , e); //$NON-NLS-1$
			}
		}		
	}
	
	@Override
	public void addCityToPreloadedList(City city) {
		if (!cities.containsKey(city.getId())) {
			LinkedHashMap<Long, City> ncities = new LinkedHashMap<Long, City>(cities);
			ncities.put(city.getId(), city);
			cities = ncities;
		}
	}
	
	@Override
	public List<City> getLoadedCities(){
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
	public synchronized List<MapObject> searchMapObjectsByName(String name, ResultMatcher<MapObject> resultMatcher) {
		SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(resultMatcher, name);
		try {
			log.debug("file=" + file + "; req=" + req);
			file.searchAddressDataByName(req);
		} catch (IOException e) {
			log.error("Disk operation failed", e); //$NON-NLS-1$
		}
		return req.getSearchResults();
	}


	@Override
	public synchronized List<City> fillWithSuggestedCities(String name, final ResultMatcher<City> resultMatcher, boolean searchVillages, LatLon currentLocation) {
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
			String lang = getLang();
			// essentially index is created that cities towns are first in cities map
			if (/*name.length() >= 2 && Algorithms.containsDigit(name) && */searchVillages) {
				// also try to identify postcodes
				String uName = name.toUpperCase();
				List<City> foundCities = file.getCities(BinaryMapIndexReader.buildAddressRequest(resultMatcher), 
						new CollatorStringMatcher(uName, StringMatcherMode.CHECK_CONTAINS), lang, 
						BinaryMapAddressReaderAdapter.POSTCODES_TYPE);
				for (City code : foundCities) {
					citiesToFill.add(code);
					if (resultMatcher.isCancelled()) {
						return citiesToFill;
					}
				}

			}
			name = name.toLowerCase();
			for (City c : cities.values()) {
				String cName = c.getName(lang); // lower case not needed, collator ensures that
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
			if (/*name.length() >= 3 && */searchVillages) {
				
				List<City> foundCities = file.getCities(BinaryMapIndexReader.buildAddressRequest(new ResultMatcher<City>() {
					List<City> cache = new ArrayList<City>();
					@Override
					public boolean publish(City c) {
						if(c.getLocation() != null) {
							City ct = getClosestCity(c.getLocation(), cache);
							c.setClosestCity(ct);
						}
						return resultMatcher.publish(c);
					}

					@Override
					public boolean isCancelled() {
						return resultMatcher.isCancelled();
					}
				}), new CollatorStringMatcher(name,StringMatcherMode.CHECK_STARTS_FROM_SPACE), lang, 
						BinaryMapAddressReaderAdapter.VILLAGES_TYPE);
				
				for (City c : foundCities) {
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
	public String getLang() {
		return langSetting.get();
	}

	@Override
	public List<Street> getStreetsIntersectStreets(Street st) {
		preloadBuildings(st, null);
		return st.getIntersectedStreets();
	}
	

	@Override
	public Building getBuildingByName(Street street, String name) {
		preloadBuildings(street, null);
		String lang = getLang();
		for (Building b : street.getBuildings()) {
			String bName = b.getName(lang);
			if (bName.equals(name)) {
				return b;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		if(fileName.indexOf('.') != -1) {
			return fileName.substring(0, fileName.indexOf('.'));
		}
		return fileName;
	}
	
	@Override
	public String getFileName() {
		return fileName;
	}
	
	@Override
	public String toString() {
		return getName() + " repository";
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
				file.getCities(BinaryMapIndexReader.buildAddressRequest(new ResultMatcher<City>() {
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
		String lang = getLang();
		for (Street s : streets) {
			String sName = s.getName(lang).toLowerCase();
			if (collator.equals(sName, name)) {
				return s;
			}
		}
		return null;
	}

	@Override
	public void clearCache() {
		cities = new LinkedHashMap<Long, City>();
		citiesQtree.clear();
		postCodes.clear();
		
	}

	@Override
	public LatLon getEstimatedRegionCenter() {
		return file.getRegionCenter();
	}

	
	
}
