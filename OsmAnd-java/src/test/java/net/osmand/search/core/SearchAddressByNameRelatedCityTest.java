package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchCoreFactory.SearchAddressByNameAPI;
import net.osmand.search.core.SearchCoreFactory.TownCitiesCache;
import net.osmand.util.MapUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Closest city of a village found by SearchAddressByNameAPI (issue #25285)
public class SearchAddressByNameRelatedCityTest {

	private static final File SRC_OBF = new File("src/test/resources/Turn_lanes_test.obf");
	private static final LatLon SAINT_FERREOL = new LatLon(45.7700, 6.3170); // village near Annecy, France
	private static final LatLon AMSTERDAM = new LatLon(52.3730, 4.8924);
	private static final LatLon GENEVE = new LatLon(46.2044, 6.1432);
	private static final String VILLAGE = "Saint-Ferreol";

	private final List<VillageReader> readers = new ArrayList<>();
	private TownCitiesCache cache;
	private VillageReader villageMap;
	private City village;

	@Before
	public void setUp() throws IOException {
		cache = new TownCitiesCache();
		villageMap = reader();
		village = villageMap.addVillage(VILLAGE, SAINT_FERREOL);
	}

	@After
	public void close() throws IOException {
		for (VillageReader r : readers) {
			r.close();
			r.getFile().delete();
		}
	}

	// village found far from the user gets the closest city around the village, not a far city
	// (town counts 10 times farther: town in 80 km lost to a city in 723 km), its region is taken from its own map
	@Test
	public void testClosestCityAroundVillageForAnySearchCenter() throws Exception {
		VillageReader userMap = reader();
		cache.add(villageMap);
		cache.add(userMap);
		City near = insertCity(villageMap, CityType.TOWN, "Town", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 80000, 180));
		insertCity(userMap, CityType.CITY, "Hilversum", new LatLon(52.2292, 5.1669));

		for (LatLon origin : new LatLon[] { AMSTERDAM, GENEVE, SAINT_FERREOL }) {
			SearchResult res = searchVillage(origin);
			Assert.assertSame("from " + origin, near, res.relatedObject);
			Assert.assertEquals("from " + origin, MapUtils.getDistance(near.getLocation(), SAINT_FERREOL),
					res.distRelatedObjectName, 0.001);
			Assert.assertSame("from " + origin, villageMap, village.getReferenceFile());
		}
	}

	// closest city may be in another map (Genève): the region of the village still comes from its own map
	@Test
	public void testClosestCityFromAnotherMap() throws Exception {
		VillageReader swissMap = reader();
		cache.add(villageMap);
		cache.add(swissMap);
		insertCity(villageMap, CityType.TOWN, "Annecy", new LatLon(45.8992, 6.1294));
		City geneve = insertCity(swissMap, CityType.CITY, "Genève", GENEVE);

		SearchResult res = searchVillage(GENEVE);
		Assert.assertSame(geneve, res.relatedObject);
		Assert.assertSame(villageMap, village.getReferenceFile());
	}

	// bbox of the cache query must contain the whole 100 km circle around the village
	@Test
	public void testClosestCityInEveryDirectionUpTo100Km() throws Exception {
		cache.add(villageMap);
		for (int distance : new int[] { 1000, 60000, 90000, 99000, 101000, 110000 }) {
			for (int bearing = 0; bearing < 360; bearing += 45) {
				TownCitiesCache dirCache = new TownCitiesCache();
				dirCache.add(villageMap);
				cache = dirCache;
				City city = insertCity(villageMap, CityType.CITY, "candidate",
						MapUtils.rhumbDestinationPoint(SAINT_FERREOL, distance, bearing));
				boolean expected = MapUtils.getDistance(SAINT_FERREOL, city.getLocation()) <= 100000;

				SearchResult res = searchVillage(SAINT_FERREOL);
				Assert.assertSame(distance + " m, bearing " + bearing, expected ? city : villageMap, res.relatedObject);
			}
		}
	}

	// cities of the village's map aren't loaded: the village keeps the region name
	@Test
	public void testNoClosestCityWhenVillageMapNotLoaded() throws Exception {
		VillageReader otherMap = reader();
		cache.add(otherMap);
		insertCity(otherMap, CityType.CITY, "Near", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 5000, 0));

		SearchResult res = searchVillage(AMSTERDAM);
		Assert.assertSame(villageMap, res.relatedObject);
		Assert.assertSame(villageMap, village.getReferenceFile());
	}

	private SearchResult searchVillage(LatLon origin) throws Exception {
		// radius level 5 covers villages ~1000 km away from the search center
		SearchSettings settings = new SearchSettings(readers).setLang("en", false)
				.setOriginalLocation(origin).setRadiusLevel(5);
		SearchPhrase phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase(VILLAGE, settings);
		AtomicInteger requestNumber = new AtomicInteger();
		SearchResultMatcher matcher = new SearchResultMatcher(null, phrase, requestNumber.get(), requestNumber, -1);
		Method searchByName = SearchAddressByNameAPI.class.getDeclaredMethod("searchByName", SearchPhrase.class,
				SearchResultMatcher.class);
		searchByName.setAccessible(true);
		searchByName.invoke(new SearchAddressByNameAPI(null, null, false, cache), phrase, matcher);
		for (SearchResult res : matcher.getRequestResults()) {
			if (res.object == village) {
				return res;
			}
		}
		Assert.fail("village not found from " + origin);
		return null;
	}

	private City insertCity(BinaryMapIndexReader file, CityType type, String name, LatLon location) {
		City city = createCity(type, name, location);
		city.setReferenceFile(file);
		int x = MapUtils.get31TileNumberX(location.getLongitude());
		int y = MapUtils.get31TileNumberY(location.getLatitude());
		cache.insertCityQR(city, new QuadRect(x, y, x, y));
		return city;
	}

	// every map is a separate file: TownCitiesCache tells maps by file
	private VillageReader reader() throws IOException {
		File file = File.createTempFile("closest-city-", ".obf");
		Files.copy(SRC_OBF.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		VillageReader r = new VillageReader(file);
		readers.add(r);
		return r;
	}

	private static City createCity(CityType type, String name, LatLon location) {
		City city = new City(type);
		city.setName(name);
		city.setLocation(location);
		return city;
	}

	private static class VillageReader extends BinaryMapIndexReader {

		private final List<City> villages = new ArrayList<>();

		VillageReader(File file) throws IOException {
			super(new RandomAccessFile(file, "r"), file);
		}

		City addVillage(String name, LatLon location) {
			City v = createCity(CityType.VILLAGE, name, location);
			villages.add(v);
			return v;
		}

		@Override
		public boolean containsPoiData(int left31x, int top31y, int right31x, int bottom31y) {
			return true;
		}

		@Override
		public boolean containsAddressData() {
			return true;
		}

		@Override
		public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req) {
			for (City v : villages) {
				req.publish(v);
			}
			return new ArrayList<>(villages);
		}
	}
}
