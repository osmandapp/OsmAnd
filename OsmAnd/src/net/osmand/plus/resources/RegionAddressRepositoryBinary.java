package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.Street;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class RegionAddressRepositoryBinary implements RegionAddressRepository {
	private static final Log log = PlatformUtil.getLog(RegionAddressRepositoryBinary.class);

	private LinkedHashMap<Long, City> cities = new LinkedHashMap<Long, City>();
	private final int POSTCODE_MIN_QUERY_LENGTH = 2;
	private final int ZOOM_QTREE = 10;
	private final QuadTree<City> citiesQtree = new QuadTree<City>(new QuadRect(0, 0, 1 << (ZOOM_QTREE + 1),
			1 << (ZOOM_QTREE + 1)), 8, 0.55f);
	private final Map<String, City> postCodes;
	private final Collator collator;
	private final OsmandPreference<String> langSetting;
	private final OsmandPreference<Boolean> transliterateSetting;
	private final BinaryMapReaderResource resource;


	public RegionAddressRepositoryBinary(ResourceManager mgr, BinaryMapReaderResource resource ) {
		this.resource = resource;
		langSetting = mgr.getContext().getSettings().MAP_PREFERRED_LOCALE;
		transliterateSetting = mgr.getContext().getSettings().MAP_TRANSLITERATE_NAMES;
		this.collator = OsmAndCollator.primaryCollator();
		this.postCodes = new TreeMap<String, City>(OsmAndCollator.primaryCollator());
	}

	@Override
	public void close() {
	}


	@Override
	public synchronized void preloadCities(ResultMatcher<City> resultMatcher) {
		if (cities.isEmpty()) {
			try {
				BinaryMapIndexReader reader = getOpenFile();
				if (reader != null) {
					List<City> cs = reader.getCities(BinaryMapIndexReader.buildAddressRequest(resultMatcher),
							BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
					LinkedHashMap<Long, City> ncities = new LinkedHashMap<Long, City>();
					for (City c : cs) {
						ncities.put(c.getId(), c);
						LatLon loc = c.getLocation();
						if (loc != null) {
							int y31 = MapUtils.get31TileNumberY(loc.getLatitude());
							int x31 = MapUtils.get31TileNumberX(loc.getLongitude());
							int dz = (31 - ZOOM_QTREE);
							citiesQtree.insert(c, new QuadRect((x31 >> dz) - 1, (y31 >> dz) - 1, (x31 >> dz) + 1, (y31 >> dz) + 1));
						}
					}
					cities = ncities;
				}
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
			}
		}
	}

	@Nullable
	private BinaryMapIndexReader getOpenFile() {
		return resource.getReader(BinaryMapReaderResourceType.ADDRESS);
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
		if (street.getBuildings().isEmpty() && street.getIntersectedStreets().isEmpty()) {
			try {
				BinaryMapIndexReader reader = getOpenFile();
				if (reader != null) {
					reader.preloadBuildings(street, BinaryMapIndexReader.buildAddressRequest(resultMatcher));
					street.sortBuildings();
				}
			} catch (IOException e) {
				log.error("Disk operation failed", e); //$NON-NLS-1$
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
	public List<City> getLoadedCities() {
		return new ArrayList<City>(cities.values());
	}

	@Override
	public synchronized void preloadStreets(City o, ResultMatcher<Street> resultMatcher) {
		Collection<Street> streets = o.getStreets();
		if (!streets.isEmpty()) {
			return;
		}
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				reader.preloadStreets(o, BinaryMapIndexReader.buildAddressRequest(resultMatcher));
			}
		} catch (IOException e) {
			log.error("Disk operation failed", e);  //$NON-NLS-1$
		}
	}

//	// not use contains It is really slow, takes about 10 times more than other steps
//	private StringMatcherMode[] streetsCheckMode = new StringMatcherMode[] {StringMatcherMode.CHECK_ONLY_STARTS_WITH,
//			StringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING};

	public synchronized List<MapObject> searchMapObjectsByName(String name, ResultMatcher<MapObject> resultMatcher, List<Integer> typeFilter) {
		SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(resultMatcher, name,
				StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				reader.searchAddressDataByName(req, typeFilter);
			}
		} catch (IOException e) {
			log.error("Disk operation failed", e); //$NON-NLS-1$
		}
		return req.getSearchResults();
	}

	@Override
	public synchronized List<MapObject> searchMapObjectsByName(String name, ResultMatcher<MapObject> resultMatcher) {
		return searchMapObjectsByName(name, resultMatcher, null);
	}

	private List<City> fillWithCities(String name, ResultMatcher<City> resultMatcher, List<Integer> typeFilter) throws IOException {
		List<City> result = new ArrayList<City>();
		ResultMatcher<MapObject> matcher = new ResultMatcher<MapObject>() {
			final List<City> cache = new ArrayList<City>();

			@Override
			public boolean publish(MapObject o) {
				City c = (City) o;
				City.CityType type = c.getType();
				if (type != null && type.ordinal() >= City.CityType.VILLAGE.ordinal()) {
					if (c.getLocation() != null) {
						City ct = getClosestCity(c.getLocation(), cache);
						c.setClosestCity(ct);
					}
				}
				return resultMatcher.publish(c);
			}

			@Override
			public boolean isCancelled() {
				return resultMatcher.isCancelled();
			}
		};
		List<MapObject> foundCities = searchMapObjectsByName(name, matcher, typeFilter);

		for (MapObject o : foundCities) {
			result.add((City) o);
			if (resultMatcher.isCancelled()) {
				return result;
			}
		}
		return result;
	}

	private List<Integer> getCityTypeFilter(String name, boolean searchVillages) {
		List<Integer> cityTypes = new ArrayList<>();
		cityTypes.add(BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
		if (searchVillages) {
			cityTypes.add(BinaryMapAddressReaderAdapter.VILLAGES_TYPE);
			if (name.length() >= POSTCODE_MIN_QUERY_LENGTH) {
				cityTypes.add(BinaryMapAddressReaderAdapter.POSTCODES_TYPE);
			}
		}
		return cityTypes;
	}

	@Override
	public synchronized List<City> fillWithSuggestedCities(String name, ResultMatcher<City> resultMatcher, boolean searchVillages, LatLon currentLocation) {
		List<City> citiesToFill = new ArrayList<>(cities.values());
		try {
			citiesToFill.addAll(fillWithCities(name, resultMatcher, getCityTypeFilter(name, searchVillages)));
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
	public boolean isTransliterateNames() {
		return transliterateSetting.get();
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
		boolean transliterateNames = isTransliterateNames();
		for (Building b : street.getBuildings()) {
			String bName = b.getName(lang, transliterateNames);
			if (bName.equals(name)) {
				return b;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		String fileName = getFileName();
		if (fileName.indexOf('.') != -1) {
			return fileName.substring(0, fileName.indexOf('.'));
		}
		return fileName;
	}

	@Override
	public String getCountryName() {
		BinaryMapIndexReader shallowReader = resource.getShallowReader();
		return shallowReader != null ? shallowReader.getCountryName() : "";
	}

	@Override
	public String getFileName() {
		return resource.getFileName();
	}

	@NonNull
	@Override
	public String toString() {
		return getName() + " repository";
	}

	@Override
	public City getCityById(long id, String name) {
		if (id == -1) {
			// do not preload cities for that case
			return null;
		}
		if (id < -1 && name != null) {
			name = name.toUpperCase();
		}
		String cmpName = name;
		preloadCities(null);
		if (!cities.containsKey(id)) {
			try {
				BinaryMapIndexReader reader = getOpenFile();
				if (reader != null) {
					reader.getCities(BinaryMapIndexReader.buildAddressRequest(new ResultMatcher<City>() {
						boolean canceled;

						@Override
						public boolean isCancelled() {
							return canceled;
						}

						@Override
						public boolean publish(City object) {
							if (id < -1) {
								if (object.getName().toUpperCase().equals(cmpName)) {
									addCityToPreloadedList(object);
									canceled = true;
								}
							} else if (object.getId() != null && object.getId().longValue() == id) {
								addCityToPreloadedList(object);
								canceled = true;
							}
							return false;
						}
					}), id < -1 ? BinaryMapAddressReaderAdapter.POSTCODES_TYPE : BinaryMapAddressReaderAdapter.VILLAGES_TYPE);
				}
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
		Collection<Street> streets = o.getStreets();
		String lang = getLang();
		boolean transliterateNames = isTransliterateNames();
		for (Street s : streets) {
			String sName = s.getName(lang, transliterateNames).toLowerCase();
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

	@Nullable
	@Override
	public LatLon getEstimatedRegionCenter() {
		BinaryMapIndexReader shallowReader = resource.getShallowReader();
		return shallowReader != null ? shallowReader.getRegionCenter() : null;
	}

}
