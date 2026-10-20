package net.osmand.search.core;

import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReaderStats.SearchStat;
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
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Related city of a village found by SearchAddressByNameAPI (issue #25285)
public class SearchAddressByNameRelatedCityTest {

	private static final File SRC_OBF = new File("src/test/resources/Turn_lanes_test.obf");
	private static final LatLon SAINT_FERREOL = new LatLon(45.7700, 6.3170);
	private static final LatLon AMSTERDAM = new LatLon(52.3730, 4.8924);
	private static final LatLon GENEVE = new LatLon(46.2044, 6.1432);
	private static final String VILLAGE = "Saint-Ferreol";

	private final List<ControlledReader> readers = new ArrayList<>();

	@After
	public void close() throws IOException {
		for (ControlledReader r : readers) {
			r.close();
			r.getFile().delete();
		}
	}

	// village map has only a far town, the near city is in the neighbour map, a city near the user is in another one:
	// the village keeps the region of its own map and is related to the near city wherever the search starts
	@Test
	public void testRelatedCityAroundVillageForAnySearchCenter() throws Exception {
		for (LatLon origin : new LatLon[] { AMSTERDAM, GENEVE, SAINT_FERREOL }) {
			ControlledReader villageMap = reader(SAINT_FERREOL, 60000);
			City village = villageMap.addVillage(VILLAGE, SAINT_FERREOL);
			villageMap.addCity(CityType.TOWN, "Far town", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 50000, 180));
			ControlledReader neighbourMap = reader(MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 80000, 0), 60000);
			City near = neighbourMap.addCity(CityType.CITY, "Near city", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 5000, 0));
			ControlledReader userMap = reader(AMSTERDAM, 100000);
			userMap.addCity(CityType.CITY, "Hilversum", new LatLon(52.2292, 5.1669));

			SearchResultMatcher matcher = searchByName(origin, new AtomicInteger(), villageMap, neighbourMap, userMap);

			SearchResult res = findVillage(matcher);
			Assert.assertSame("from " + origin, village, res.object);
			Assert.assertSame("from " + origin, villageMap, village.getReferenceFile());
			Assert.assertSame("from " + origin, near, res.relatedObject);
			Assert.assertEquals("from " + origin, MapUtils.getDistance(near.getLocation(), SAINT_FERREOL),
					res.distRelatedObjectName, 0.001);
			Assert.assertEquals("from " + origin, 0, userMap.cityReads);
		}
	}

	@Test
	public void testNoRelatedCityFartherThan100Km() throws Exception {
		ControlledReader villageMap = reader(SAINT_FERREOL, 200000);
		villageMap.addVillage(VILLAGE, SAINT_FERREOL);
		villageMap.addCity(CityType.CITY, "Far city", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 120000, 90));

		SearchResult res = findVillage(searchByName(SAINT_FERREOL, new AtomicInteger(), villageMap));
		Assert.assertSame(villageMap, res.relatedObject); // region name of the village map
		Assert.assertEquals(0, res.distRelatedObjectName, 0);
	}

	// warm cache: city blocks of a map are read once
	@Test
	public void testCityBlocksReadOnce() throws Exception {
		ControlledReader villageMap = reader(SAINT_FERREOL, 60000);
		villageMap.addVillage(VILLAGE, SAINT_FERREOL);
		villageMap.addVillage(VILLAGE + " 2", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 3000, 90));
		City city = villageMap.addCity(CityType.CITY, "City", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 5000, 0));
		SearchAddressByNameAPI api = new SearchAddressByNameAPI(null, null, false, new TownCitiesCache());

		for (int i = 0; i < 2; i++) {
			SearchResult res = findVillage(searchByName(api, SAINT_FERREOL, new AtomicInteger(), villageMap));
			Assert.assertSame(city, res.relatedObject);
		}
		Assert.assertEquals(2, villageMap.cityReads); // CITY_TOWN and BOUNDARY blocks
	}

	// search cancelled while the name search of the file: no city blocks are read after that
	@Test
	public void testNoCityBlocksReadAfterCancel() throws Exception {
		AtomicInteger requestNumber = new AtomicInteger();
		ControlledReader villageMap = reader(SAINT_FERREOL, 60000);
		villageMap.addVillage(VILLAGE, SAINT_FERREOL);
		villageMap.addCity(CityType.CITY, "City", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 5000, 0));
		villageMap.cancelOnNameSearch = requestNumber;

		SearchResultMatcher matcher = searchByName(SAINT_FERREOL, requestNumber, villageMap);

		Assert.assertTrue(matcher.isCancelled());
		Assert.assertEquals(0, villageMap.cityReads);
	}

	// city blocks are read after the name search, never from inside of it
	@Test
	public void testNoReadsInsideNameSearch() throws Exception {
		ControlledReader villageMap = reader(SAINT_FERREOL, 60000);
		villageMap.addVillage(VILLAGE, SAINT_FERREOL);
		villageMap.addCity(CityType.CITY, "City", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 5000, 0));

		searchByName(SAINT_FERREOL, new AtomicInteger(), villageMap);

		Assert.assertTrue(villageMap.cityReads > 0);
		Assert.assertFalse(villageMap.reentrantRead);
	}

	// failed read of city blocks doesn't mark the map as loaded: next search reads it again
	@Test
	public void testFailedCityReadIsRetried() throws Exception {
		ControlledReader villageMap = reader(SAINT_FERREOL, 60000);
		villageMap.addVillage(VILLAGE, SAINT_FERREOL);
		City city = villageMap.addCity(CityType.CITY, "City", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 5000, 0));
		villageMap.failBoundaryRead = true;
		SearchAddressByNameAPI api = new SearchAddressByNameAPI(null, null, false, new TownCitiesCache());

		try {
			searchByName(api, SAINT_FERREOL, new AtomicInteger(), villageMap);
			Assert.fail("IOException expected");
		} catch (IOException e) {
			// expected
		}
		villageMap.failBoundaryRead = false;
		Assert.assertSame(city, findVillage(searchByName(api, SAINT_FERREOL, new AtomicInteger(), villageMap)).relatedObject);
	}

	// every map is a separate file: TownCitiesCache tells maps by file
	private ControlledReader reader(LatLon center, int radius) throws IOException {
		File file = File.createTempFile("related-city-", ".obf");
		Files.copy(SRC_OBF.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		ControlledReader r = new ControlledReader(file, MapUtils.calculate31BboxUsingRhumb(radius, center));
		readers.add(r);
		return r;
	}

	private SearchResultMatcher searchByName(LatLon origin, AtomicInteger requestNumber, ControlledReader... files)
			throws Exception {
		return searchByName(new SearchAddressByNameAPI(null, null, false, new TownCitiesCache()), origin,
				requestNumber, files);
	}

	private SearchResultMatcher searchByName(SearchAddressByNameAPI api, LatLon origin, AtomicInteger requestNumber,
	                                         ControlledReader... files) throws Exception {
		// radius level 5 covers villages ~1000 km away from the search center
		SearchSettings settings = new SearchSettings(Arrays.asList(files)).setLang("en", false)
				.setOriginalLocation(origin).setRadiusLevel(5);
		SearchPhrase phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase(VILLAGE, settings);
		SearchResultMatcher matcher = new SearchResultMatcher(null, phrase, requestNumber.get(), requestNumber, -1);
		Method method = SearchAddressByNameAPI.class.getDeclaredMethod("searchByName", SearchPhrase.class,
				SearchResultMatcher.class);
		method.setAccessible(true);
		try {
			method.invoke(api, phrase, matcher);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException io) {
				throw io;
			}
			throw e;
		}
		return matcher;
	}

	private SearchResult findVillage(SearchResultMatcher matcher) {
		for (SearchResult res : matcher.getRequestResults()) {
			if (res.objectType == ObjectType.VILLAGE && VILLAGE.equals(res.localeName)) {
				return res;
			}
		}
		Assert.fail("village not found in " + matcher.getRequestResults());
		return null;
	}

	private static class ControlledReader extends BinaryMapIndexReader {

		private final QuadRect area;
		private final List<City> villages = new ArrayList<>();
		private final List<City> cities = new ArrayList<>();
		private AtomicInteger cancelOnNameSearch;
		private boolean failBoundaryRead;
		private boolean inNameSearch;
		private boolean reentrantRead;
		private int cityReads;

		ControlledReader(File file, QuadRect area) throws IOException {
			super(new RandomAccessFile(file, "r"), file);
			this.area = area;
		}

		City addVillage(String name, LatLon location) {
			City village = createCity(CityType.VILLAGE, name, location);
			villages.add(village);
			return village;
		}

		City addCity(CityType type, String name, LatLon location) {
			City city = createCity(type, name, location);
			cities.add(city);
			return city;
		}

		@Override
		public boolean containsPoiData(int left31x, int top31y, int right31x, int bottom31y) {
			return QuadRect.intersects(area, new QuadRect(left31x, top31y, right31x, bottom31y));
		}

		@Override
		public boolean containsAddressData() {
			return true;
		}

		@Override
		public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req) {
			inNameSearch = true;
			try {
				for (City village : villages) {
					req.publish(village);
				}
				if (cancelOnNameSearch != null) {
					cancelOnNameSearch.incrementAndGet();
				}
				return new ArrayList<>(villages);
			} finally {
				inNameSearch = false;
			}
		}

		@Override
		public List<City> getCities(SearchRequest<City> resultMatcher, CityBlocks type, AddressRegion onlyRegion,
		                            SearchStat stat) throws IOException {
			if (inNameSearch) {
				reentrantRead = true;
			}
			cityReads++;
			if (type == CityBlocks.BOUNDARY_TYPE) {
				if (failBoundaryRead) {
					throw new IOException("boundary read failed");
				}
				return Collections.emptyList();
			}
			return new ArrayList<>(cities);
		}

		private static City createCity(CityType type, String name, LatLon location) {
			City city = new City(type);
			city.setName(name);
			city.setLocation(location);
			return city;
		}
	}
}
