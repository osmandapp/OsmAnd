package net.osmand.search;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.search.core.SearchCoreFactory.TownCitiesCache;
import net.osmand.util.MapUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TownCitiesCacheTest {

	private static final String SRC_OBF = "src/test/resources/Turn_lanes_test.obf";

	// Files of different maps may share the same region name (ex. combined German state maps
	// of 2025 are all "Germany"), cities should be loaded from each file anyway (issue #25993)
	@Test
	public void testCachePerFileNotPerRegionName() throws IOException {
		File dir = Files.createTempDirectory("town-cities-cache").toFile();
		// file names are different on purpose: getRegionName() falls back to the file name
		// when the file has no region names inside, such fallback would give "first" / "second"
		File first = copy(dir, "first_region_2.obf");
		File second = copy(dir, "second_region_2.obf");
		BinaryMapIndexReader firstReader = read(first);
		BinaryMapIndexReader secondReader = read(second);
		try {
			// region name is read from inside the file, so both copies report the same one
			Assert.assertEquals(firstReader.getRegionName(), secondReader.getRegionName());

			TownCitiesCache cache = new TownCitiesCache();
			Assert.assertFalse(cache.contains(firstReader));
			cache.add(firstReader);

			Assert.assertTrue(cache.contains(firstReader));
			Assert.assertFalse(cache.contains(secondReader));

			cache.add(secondReader);
			Assert.assertTrue(cache.contains(secondReader));
		} finally {
			firstReader.close();
			secondReader.close();
			first.delete();
			second.delete();
			dir.delete();
		}
	}

	private static final int MAX_DIST = 100 * 1000;
	private static final LatLon SAINT_FERREOL = new LatLon(45.7700, 6.3170); // village near Annecy, France

	// Village found far from the search center (Amsterdam) must not be labeled with a city
	// near the search center, but with a close city of its own map (issue #25285)
	@Test
	public void testRelatedCityIsNearVillageNotNearSearchCenter() {
		Object netherlands = "netherlands.obf";
		Object france = "france.obf";
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.CITY, "Hilversum", new LatLon(52.2292, 5.1669), netherlands);
		insert(cache, CityType.CITY, "Amsterdam", new LatLon(52.3730, 4.8924), netherlands);
		insert(cache, CityType.CITY, "Paris", new LatLon(48.8566, 2.3522), france);
		insert(cache, CityType.TOWN, "Annecy", new LatLon(45.8992, 6.1294), france);
		insert(cache, CityType.TOWN, "Albertville", new LatLon(45.6755, 6.3925), france);

		City city = cache.findRelatedCity(SAINT_FERREOL, france, MAX_DIST);
		Assert.assertNotNull(city);
		Assert.assertEquals("Albertville", city.getName());
	}

	@Test
	public void testRelatedCityFromOtherMapIsIgnored() {
		Object switzerland = "switzerland.obf";
		Object france = "france.obf";
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.CITY, "Genève", new LatLon(46.2044, 6.1432), switzerland);
		insert(cache, CityType.TOWN, "Annecy", new LatLon(45.8992, 6.1294), france);

		City city = cache.findRelatedCity(SAINT_FERREOL, france, MAX_DIST);
		Assert.assertNotNull(city);
		Assert.assertEquals("Annecy", city.getName());
	}

	@Test
	public void testNoRelatedCityFartherThanMaxDistance() {
		Object france = "france.obf";
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.CITY, "Paris", new LatLon(48.8566, 2.3522), france);

		Assert.assertNull(cache.findRelatedCity(SAINT_FERREOL, france, MAX_DIST));
	}

	@Test
	public void testCityIsPreferredOverCloserTown() {
		Object france = "france.obf";
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.TOWN, "Albertville", new LatLon(45.6755, 6.3925), france); // ~12 km
		insert(cache, CityType.CITY, "Chambéry", new LatLon(45.5646, 5.9178), france); // ~38 km

		Assert.assertEquals("Chambéry", cache.findRelatedCity(SAINT_FERREOL, france, MAX_DIST).getName());
	}

	private void insert(TownCitiesCache cache, CityType type, String name, LatLon location, Object file) {
		City city = new City(type);
		city.setName(name);
		city.setLocation(location);
		city.setReferenceFile(file);
		int x = MapUtils.get31TileNumberX(location.getLongitude());
		int y = MapUtils.get31TileNumberY(location.getLatitude());
		cache.insertCityQR(city, new QuadRect(x, y, x, y));
	}

	private File copy(File dir, String name) throws IOException {
		File file = new File(dir, name);
		Files.copy(new File(SRC_OBF).toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return file;
	}

	private BinaryMapIndexReader read(File file) throws IOException {
		return new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
	}
}
