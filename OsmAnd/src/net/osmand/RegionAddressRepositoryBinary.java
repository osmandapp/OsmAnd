package net.osmand;

import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
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
	
	
	private LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	private Map<String, PostCode> postCodes = new TreeMap<String, PostCode>(Collator.getInstance());
	private boolean useEnglishNames = false;
	private Collator collator;
	
	public RegionAddressRepositoryBinary(BinaryMapIndexReader file, String name) {
		this.file = file;
		this.region = name;
		this.collator = Collator.getInstance();
		this.collator.setStrength(Collator.PRIMARY);
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
			String bName = useEnglishNames ? building.getEnName() : building.getName();
			String lowerCase = bName.toLowerCase();
			if (cstartsWith(lowerCase,name)) { 
				buildingsToFill.add(ind, building);
				ind++;
			} else if (ccontains(name, lowerCase)) {
				buildingsToFill.add(building);
			}
		}
	}

	/**
	 * check part contains in base
	 * @return
	 */
	private boolean ccontains(String part, String base) {
		int pos = 0;
		if(part.length() > 3){
			// improve searching by searching first 3 characters
			pos = cindexOf(pos, part.substring(0, 3), base);
			if(pos == -1){
				return false;
			}
		}
		pos = cindexOf(pos, part, base);
		if(pos == -1){
			return false;
		}
		return true;
	}
	
	private int cindexOf(int start, String part, String base){
		for (int pos = start; pos <= base.length() - part.length(); pos++) {
			if (collator.equals(base.substring(pos, pos + part.length()), part)) {
				return pos;
			}
		}
		return -1;
	}
		
	private boolean cstartsWith(String lowerCase, String name) {
		//simulate starts with for collator
		return collator.equals(lowerCase.substring(0, Math.min(lowerCase.length(), name.length())), name);
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
	public void fillWithSuggestedStreets(MapObject o, String name, List<Street> streetsToFill) {
		assert o instanceof PostCode || o instanceof City;
		City city = (City) (o instanceof City ? o : null); 
		PostCode post = (PostCode) (o instanceof PostCode ? o : null);
		name = name.toLowerCase();
		preloadStreets(o);
		Collection<Street> streets = post == null ? city.getStreets() : post.getStreets() ;
		
		if(name.length() == 0){
			streetsToFill.addAll(streets);
		} else {
			int ind = 0;
			for (Street s : streets) {
				String sName = useEnglishNames ? s.getEnName() : s.getName();
				String lowerCase = sName.toLowerCase();
				if (cstartsWith(lowerCase,name)) {
					streetsToFill.add(ind, s);
					ind++;
				} else if (ccontains(name, lowerCase) || ccontains(lowerCase, name)) {
					streetsToFill.add(s);
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
						String cName = useEnglishNames ? c.getEnName() : c.getName();
						String lowerCase = cName.toLowerCase();
						if (cstartsWith(lowerCase,name)) {
							citiesToFill.add(c);
						}
					}
				}
			} else {
				name = name.toLowerCase();
				Collection<City> src = cities.values();
				for (City c : src) {
					String cName = useEnglishNames ? c.getEnName() : c.getName();
					String lowerCase = cName.toLowerCase();
					if (cstartsWith(lowerCase,name)) {
						citiesToFill.add(ind, c);
						ind++;
					} else if (ccontains(name, lowerCase)) {
						citiesToFill.add(c);
					}
				}
				int initialsize = citiesToFill.size();
				
				for(City c : file.getVillages(region, name, useEnglishNames )){
					String cName = c.getName(useEnglishNames).toLowerCase();
					if (cName.startsWith(name)) {
						citiesToFill.add(ind, c);
						ind++;
					} else if (ccontains(name, cName)) {
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
		return cities.get(id);
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
			String sName = useEnglishNames ? s.getEnName() : s.getName();
			String lowerCase = sName.toLowerCase();
			if (collator.equals(lowerCase,name)) {
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
