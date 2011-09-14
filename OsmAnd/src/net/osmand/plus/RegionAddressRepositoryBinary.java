package net.osmand.plus;

import static net.osmand.plus.CollatorStringMatcher.ccontains;
import static net.osmand.plus.CollatorStringMatcher.cstartsWith;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;

import org.apache.commons.logging.Log;


public class RegionAddressRepositoryBinary implements RegionAddressRepository {
	private static final Log log = LogUtil.getLog(RegionAddressRepositoryBinary.class);
	private BinaryMapIndexReader file;
	private String region;
	
	
	private final LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	private final Map<String, PostCode> postCodes;
	private boolean useEnglishNames = false;
	private final Collator collator;
	
	public RegionAddressRepositoryBinary(BinaryMapIndexReader file, String name) {
		this.file = file;
		this.region = name;
 	    this.collator = Collator.getInstance();
 	    this.collator.setStrength(Collator.PRIMARY); //ignores also case
		this.postCodes = new TreeMap<String, PostCode>(collator);
	}
	
	public void close(){
		this.file = null;
	}


	@Override
	public List<Building> fillWithSuggestedBuildings(PostCode postcode, Street street, String name, ResultMatcher<Building> resultMatcher) {
		List<Building> buildingsToFill = new ArrayList<Building>();
		if (name.length() == 0) {
			preloadBuildings(street, resultMatcher);
			buildingsToFill.addAll(street.getBuildings());
			return buildingsToFill;
		}
		preloadBuildings(street, null);
		name = name.toLowerCase();
		int ind = 0;
		for (Building building : street.getBuildings()) {
			String bName = useEnglishNames ? building.getEnName() : building.getName(); //lower case not needed, collator ensures that
			if (cstartsWith(collator,bName,name)) { 
				buildingsToFill.add(ind, building);
				ind++;
			} else if (ccontains(collator,name,bName)) {
				buildingsToFill.add(building);
			}
		}
		return buildingsToFill;
	}


	private void preloadBuildings(Street street, ResultMatcher<Building> resultMatcher) {
		if(street.getBuildings().isEmpty()){
			try {
				file.preloadBuildings(street, resultMatcher);
				street.sortBuildings();
			} catch (IOException e) {
				log.error("Disk operation failed" , e); //$NON-NLS-1$
			}
		}		
	}


	@Override
	public List<Street> fillWithSuggestedStreets(MapObject o, ResultMatcher<Street> resultMatcher, String... names) {
		assert o instanceof PostCode || o instanceof City;
		City city = (City) (o instanceof City ? o : null); 
		PostCode post = (PostCode) (o instanceof PostCode ? o : null);
		List<Street> streetsToFill = new ArrayList<Street>();	
		if(names.length == 0){
			preloadStreets(o, resultMatcher);
			streetsToFill.addAll(post == null ? city.getStreets() : post.getStreets());
			return streetsToFill;
		}
		preloadStreets(o, null);
		int ind = 0;
		Collection<Street> streets = post == null ? city.getStreets() : post.getStreets() ;
		Iterator<Street> iterator = streets.iterator();
		while(iterator.hasNext()) {
			Street s = iterator.next();
			String sName = useEnglishNames ? s.getEnName() : s.getName(); // lower case not needed, collator ensures that
			for (String name : names) {
				if (cstartsWith(collator, sName, name)) {
					streetsToFill.add(ind, s);
					ind++;
				} else if (ccontains(collator, name, sName) || ccontains(collator, sName, name)) {
					streetsToFill.add(s);
				}
			}
		}
		return streetsToFill;
	}

	private void preloadStreets(MapObject o, ResultMatcher<Street> resultMatcher) {
		assert o instanceof PostCode || o instanceof City;
		Collection<Street> streets = o instanceof PostCode ? ((PostCode) o).getStreets() : ((City) o).getStreets();
		if(!streets.isEmpty()){
			return;
		}
		try {
			if(o instanceof PostCode){
				file.preloadStreets((PostCode) o, resultMatcher);
			} else {
				file.preloadStreets((City) o, resultMatcher);
			}
		} catch (IOException e) {
			log.error("Disk operation failed" , e); //$NON-NLS-1$
		}
		
	}
	

	@Override
	public List<MapObject> fillWithSuggestedCities(String name, ResultMatcher<MapObject> resultMatcher, LatLon currentLocation) {
		List<MapObject> citiesToFill = new ArrayList<MapObject>();
		if (cities.isEmpty()) {
			preloadCities(resultMatcher);
			citiesToFill.addAll(cities.values());
			return citiesToFill;
		} else {
			preloadCities(null);
		}
		try {
			// essentially index is created that cities towns are first in cities map
			int ind = 0;
			if (name.length() >= 2 && Algoritms.containsDigit(name)) {
				// also try to identify postcodes
				String uName = name.toUpperCase();
				for (PostCode code : file.getPostcodes(region, resultMatcher, new ContainsStringMatcher(uName, collator))) {
					if (cstartsWith(collator, code.getName(), uName)) {
						citiesToFill.add(ind++, code);
					} else if (ccontains(collator, code.getName(), uName)) {
						citiesToFill.add(code);
					}
				}
				
			}
			if (name.length() < 3) {
				if (name.length() == 0) {
					citiesToFill.addAll(cities.values());
				} else {
					name = name.toLowerCase();
					for (City c : cities.values()) {
						String cName = useEnglishNames ? c.getEnName() : c.getName(); //lower case not needed, collator ensures that
						if (cstartsWith(collator, cName, name)) {
							if (resultMatcher.publish(c)) {
								citiesToFill.add(c);
							}
						}
					}
				}
			} else {
				name = name.toLowerCase();
				for (City c : cities.values()) {
					String cName = useEnglishNames ? c.getEnName() : c.getName(); //lower case not needed, collator ensures that
					if (cstartsWith(collator,cName,name)) {
						if (resultMatcher.publish(c)) {
							citiesToFill.add(ind, c);
							ind++;
						}
					} else if (ccontains(collator,name,cName)) {
						if (resultMatcher.publish(c)) {
							citiesToFill.add(c);
						}
					}
				}
				
				int initialsize = citiesToFill.size();
				
				for(City c : file.getVillages(region, resultMatcher, new ContainsStringMatcher(name,collator), useEnglishNames )){
					String cName = c.getName(useEnglishNames); //lower case not needed, collator ensures that
					if (cstartsWith(collator,cName,name)) {
						citiesToFill.add(ind, c);
						ind++;
					} else if (ccontains(collator,name, cName)) {
						citiesToFill.add(c);
					}
				}
				log.debug("Loaded citites " + (citiesToFill.size() - initialsize)); //$NON-NLS-1$
			}
		} catch (IOException e) {
			log.error("Disk operation failed" , e); //$NON-NLS-1$
		}
		return citiesToFill;
		
	}

	@Override
	public void fillWithSuggestedStreetsIntersectStreets(City city, Street st, List<Street> streetsToFill) {
		if(city != null){
			preloadStreets(city, null);
			try {
				file.findIntersectedStreets(city, st, streetsToFill);
			} catch (IOException e) {
				log.error("Disk operation failed" , e); //$NON-NLS-1$
			}
		}
	}
	
	@Override
	public LatLon findStreetIntersection(Street street, Street street2) {
		City city = street.getCity();
		if(city != null){
			preloadStreets(city, null);
			try {
				return file.findStreetIntersection(city, street, street2);
			} catch (IOException e) {
				log.error("Disk operation failed" , e); //$NON-NLS-1$
			}
		}
		return null;
	}
	

	@Override
	public Building getBuildingByName(Street street, String name) {
		preloadBuildings(street, null);
		for (Building b : street.getBuildings()) {
			String bName = useEnglishNames ? b.getEnName() : b.getName();
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
	public boolean useEnglishNames() {
		return useEnglishNames;
	}
	


	@Override
	public City getCityById(Long id) {
		if(id == -1){
			// do not preload cities for that case
			return null;
		}
		preloadCities(null);
		return cities.get(id);
	}


	private void preloadCities(ResultMatcher<MapObject> resultMatcher) {
		if (cities.isEmpty()) {
			try {
				List<City> cs = file.getCities(region, resultMatcher);
				for (City c : cs) {
					cities.put(c.getId(), c);
				}
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
			}
		}
	}

	@Override
	public PostCode getPostcode(String name) {
		if(name == null){
			return null;
		}
		String uc = name.toUpperCase();
		if(!postCodes.containsKey(uc)){
			try {
				postCodes.put(uc, file.getPostcodeByName(this.region, name));
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
			}
		}
		return postCodes.get(uc);
	}


	@Override
	public Street getStreetByName(MapObject o, String name) {
		assert o instanceof PostCode || o instanceof City;
		City city = (City) (o instanceof City ? o : null);
		PostCode post = (PostCode) (o instanceof PostCode ? o : null);
		name = name.toLowerCase();
		preloadStreets(o, null);
		Collection<Street> streets = post == null ? city.getStreets() : post.getStreets();
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
	public void addCityToPreloadedList(City city) {
		cities.put(city.getId(), city);
	}

	@Override
	public boolean areCitiesPreloaded() {
		return !cities.isEmpty();
	}

	@Override
	public boolean arePostcodesPreloaded() {
		// postcodes are always preeloaded 
		// do not load them into memory (just cache last used)
		return true;
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
