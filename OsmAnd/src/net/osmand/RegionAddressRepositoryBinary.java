package net.osmand;

import static net.osmand.CollatorStringMatcher.ccontains;
import static net.osmand.CollatorStringMatcher.cstartsWith;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	public boolean isMapRepository() {
		return true;
	}

	@Override
	public void fillWithSuggestedBuildings(PostCode postcode, Street street, String name, List<Building> buildingsToFill) {
		preloadBuildings(street);
		if(name.length() == 0){
			buildingsToFill.addAll(street.getBuildings());
			return;
		}
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
	}


	private void preloadBuildings(Street street) {
		if(street.getBuildings().isEmpty()){
			try {
				file.preloadBuildings(street);
				street.sortBuildings();
			} catch (IOException e) {
				log.error("Disk operation failed" , e); //$NON-NLS-1$
			}
		}		
	}


	@Override
	public void fillWithSuggestedStreets(String name, List<Street> streetsToFill) {
		try {
			Collection<City> citiesToLook = cities.isEmpty() ? file.getCities(region) : cities.values();
			fillWithSuggestedStreetsInCities(citiesToLook, name, streetsToFill);
			List<City> villages = file.getVillages(region);
			fillWithSuggestedStreetsInCities(villages, name, streetsToFill);
			Collections.sort(streetsToFill, new Comparator<Street>() {
				@Override
						public int compare(Street object1, Street object2) {
							String name1 = object1.getName(useEnglishNames);
							String name2 = object2.getName(useEnglishNames);
							return collator.compare(name1,name2);
						}
			});
		} catch (IOException e) {
			log.error("Disk operation failed" , e); 
		}
	}

	private void fillWithSuggestedStreetsInCities(
			Collection<City> citiesToLook, String name,
			List<Street> streetsToFill) throws IOException {
		for (City city : citiesToLook) {
			boolean preloaded = false;
			if (city.getStreets().isEmpty()) {
				preloaded = true;
				file.preloadStreets(city);
			}
			fillWithSuggestedStreets(name, streetsToFill, city.getStreets(), true);
			if (preloaded) {
				city.removeAllStreets(); //so that we don't have memory issues
			}
		}
	}
	
	@Override
	public void fillWithSuggestedStreets(MapObject o, String name, List<Street> streetsToFill) {
		assert o instanceof PostCode || o instanceof City;
		City city = (City) (o instanceof City ? o : null); 
		PostCode post = (PostCode) (o instanceof PostCode ? o : null);
		name = name.toLowerCase();
		preloadStreets(o);
		Collection<Street> streets = post == null ? city.getStreets() : post.getStreets() ;
		
		fillWithSuggestedStreets(name, streetsToFill, streets, false);
	}

	@Override
	public void fillWithSuggestedStreets(final String name,
			List<Street> streetsToFill, Collection<Street> streets, boolean onlyStartCheck) {
		if(name.length() == 0){
			streetsToFill.addAll(streets);
		} else {
			int ind = 0;
			if (onlyStartCheck) {
				//we assume the streets are ordered!
				ArrayList<Street> streetList = new ArrayList<Street>(streets);
				Comparator<Street> startCompare = new Comparator<Street>() {
					int length = name.length();
					@Override
							public int compare(Street object1, Street object2) {
								String name1 = object1.getName(useEnglishNames);
								String name2 = object2.getName(useEnglishNames);
								return collator.compare(
										name1.substring(0, Math.min(
												name1.length(), length)),
										name2.substring(0, Math.min(
												name2.length(), length)));
							}
				};
				Street searchedStreet = new Street(null,name);
				int index = Collections.binarySearch(streetList, searchedStreet, startCompare);
				if (index > -1) {
					streetsToFill.add(streetList.get(index));
					boolean found = true;
					int i = 1;
					while (found) {
						found = false;
						if (index + i < streetList.size()) {
							if (startCompare.compare(streetList.get(index+i),searchedStreet) == 0) {
								streetsToFill.add(streetList.get(index+i));
								found = true;
							}
						}
						if (index - i >= 0) {
							if (startCompare.compare(streetList.get(index-i),searchedStreet) == 0) {
								streetsToFill.add(streetList.get(index-i));
								found = true;
							}
						}
						i++;
					}
				}
			} else {
				for (Street s : streets) {
					String sName = useEnglishNames ? s.getEnName() : s.getName(); //lower case not needed, collator ensures that
					if (cstartsWith(collator,sName,name)) {
						streetsToFill.add(ind, s);
						ind++;
					} else if (onlyStartCheck && (ccontains(collator,name,sName) || ccontains(collator,sName,name))) {
						streetsToFill.add(s);
					}
				}
			}
		}
	}

	private void preloadStreets(MapObject o) {
		assert o instanceof PostCode || o instanceof City;
		Collection<Street> streets = o instanceof PostCode ? ((PostCode) o).getStreets() : ((City) o).getStreets();
		if(!streets.isEmpty()){
			return;
		}
		try {
			if(o instanceof PostCode){
				file.preloadStreets((PostCode) o);
			} else {
				file.preloadStreets((City) o);
			}
		} catch (IOException e) {
			log.error("Disk operation failed" , e); //$NON-NLS-1$
		}
		
	}
	

	@Override
	public void fillWithSuggestedCities(String name, List<MapObject> citiesToFill, LatLon currentLocation) {
		preloadCities();
		try {
			// essentially index is created that cities towns are first in cities map
			int ind = 0;
			if (name.length() >= 2 &&
					   Character.isDigit(name.charAt(0)) &&
					   Character.isDigit(name.charAt(1))) {
				// also try to identify postcodes
				String uName = name.toUpperCase();
				for (PostCode code : file.getPostcodes(region)) {
					if (code.getName().startsWith(uName)) {
						citiesToFill.add(ind++, code);
					} else if(code.getName().contains(uName)){
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
						if (cstartsWith(collator,cName,name)) {
							citiesToFill.add(c);
						}
					}
				}
			} else {
				name = name.toLowerCase();
				Collection<City> src = cities.values();
				for (City c : src) {
					String cName = useEnglishNames ? c.getEnName() : c.getName(); //lower case not needed, collator ensures that
					if (cstartsWith(collator,cName,name)) {
						citiesToFill.add(ind, c);
						ind++;
					} else if (ccontains(collator,name,cName)) {
						citiesToFill.add(c);
					}
				}
				int initialsize = citiesToFill.size();
				
				for(City c : file.getVillages(region, new ContainsStringMatcher(name,collator), useEnglishNames )){
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
		
	}

	@Override
	public void fillWithSuggestedStreetsIntersectStreets(City city, Street st, List<Street> streetsToFill) {
		if(city != null){
			preloadStreets(city);
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
			preloadStreets(city);
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
		preloadBuildings(street);
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
		preloadCities();
		City city = cities.get(id);
		if (city == null) {
			return getVilageById(id);
		} else {
			return city;
		}
	}

	private City getVilageById(Long id) {
		try {
			for (City c : file.getVillages(region)) {
				if (c.getId().equals(id)) {
					return c;
				}
			}
		} catch (IOException e) {
			log.error("Disk operation failed", e); //$NON-NLS-1$
		}
		return null;
	}


	private void preloadCities() {
		if (cities.isEmpty()) {
			try {
				List<City> cs = file.getCities(region);
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
		preloadStreets(o);
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
		// postcodes are always preloaded 
		// do not load them into memory (just cache last used)
		return true;
	}

	@Override
	public void clearCache() {
		cities.clear();
		postCodes.clear();
		
	}



}
