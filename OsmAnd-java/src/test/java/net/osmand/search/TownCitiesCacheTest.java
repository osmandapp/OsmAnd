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
	// near the search center, but with a city close to the village (issue #25285)
	@Test
	public void testRelatedCityIsNearVillageNotNearSearchCenter() {
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.CITY, "Hilversum", new LatLon(52.2292, 5.1669));
		insert(cache, CityType.CITY, "Amsterdam", new LatLon(52.3730, 4.8924));
		insert(cache, CityType.CITY, "Paris", new LatLon(48.8566, 2.3522));
		insert(cache, CityType.TOWN, "Annecy", new LatLon(45.8992, 6.1294));
		insert(cache, CityType.TOWN, "Albertville", new LatLon(45.6755, 6.3925));

		Assert.assertEquals("Albertville", name(cache.findRelatedCity(SAINT_FERREOL, MAX_DIST)));
	}

	// related city may be in another map and across the border: the region of the village
	// is taken from its own map, so a near Genève doesn't make the village Swiss
	@Test
	public void testRelatedCityFromAnyMap() {
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.CITY, "Genève", new LatLon(46.2044, 6.1432));
		insert(cache, CityType.TOWN, "Annecy", new LatLon(45.8992, 6.1294));

		Assert.assertEquals("Genève", name(cache.findRelatedCity(SAINT_FERREOL, MAX_DIST)));
	}

	@Test
	public void testNoRelatedCityFartherThanMaxDistance() {
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.CITY, "Paris", new LatLon(48.8566, 2.3522));

		Assert.assertNull(cache.findRelatedCity(SAINT_FERREOL, MAX_DIST));
	}

	@Test
	public void testCityIsPreferredOverCloserTown() {
		TownCitiesCache cache = new TownCitiesCache();
		insert(cache, CityType.TOWN, "Albertville", new LatLon(45.6755, 6.3925)); // ~12 km
		insert(cache, CityType.CITY, "Chambéry", new LatLon(45.5646, 5.9178)); // ~38 km

		Assert.assertEquals("Chambéry", name(cache.findRelatedCity(SAINT_FERREOL, MAX_DIST)));
	}

	// the candidates bbox must contain the whole circle: the inscribed square of
	// calculate31BboxUsingRhumb lost a city 90 km to the east
	@Test
	public void testRelatedCityInEveryDirectionUpToMaxDistance() {
		LatLon[] villages = { new LatLon(0.5, 10), SAINT_FERREOL, new LatLon(69.65, 18.96), new LatLon(78.22, 15.65) };
		int[] distances = { 1000, 60000, 70700, 90000, 99000, 101000, 110000 };
		for (LatLon village : villages) {
			for (int distance : distances) {
				for (int bearing = 0; bearing < 360; bearing += 45) {
					LatLon location = MapUtils.rhumbDestinationPoint(village, distance, bearing);
					boolean expected = MapUtils.getDistance(village, location) <= MAX_DIST;
					String msg = village + " " + distance + " m, bearing " + bearing;
					for (boolean area : new boolean[] { false, true }) {
						TownCitiesCache cache = new TownCitiesCache();
						City city = createCity(CityType.CITY, "candidate", location);
						int x = MapUtils.get31TileNumberX(location.getLongitude());
						int y = MapUtils.get31TileNumberY(location.getLatitude());
						int d = area ? 1 << 12 : 0; // ~1 km city area
						cache.insertCityQR(city, new QuadRect(x - d, y - d, x + d, y + d));
						Assert.assertSame(msg + (area ? " area" : " point"), expected ? city : null,
								cache.findRelatedCity(village, MAX_DIST));
					}
				}
			}
		}
	}

	@Test
	public void testRelatedCityDoesntDependOnInsertOrder() {
		LatLon[] locations = { new LatLon(45.6755, 6.3925), new LatLon(45.5646, 5.9178), new LatLon(46.2044, 6.1432) };
		CityType[] types = { CityType.TOWN, CityType.CITY, CityType.CITY };
		String[] names = { "Albertville", "Chambéry", "Genève" };
		TownCitiesCache direct = new TownCitiesCache();
		TownCitiesCache reverse = new TownCitiesCache();
		for (int i = 0; i < names.length; i++) {
			insert(direct, types[i], names[i], locations[i]);
			int j = names.length - 1 - i;
			insert(reverse, types[j], names[j], locations[j]);
		}
		Assert.assertEquals(name(direct.findRelatedCity(SAINT_FERREOL, MAX_DIST)),
				name(reverse.findRelatedCity(SAINT_FERREOL, MAX_DIST)));
	}

	// a reopened reader of the same file is "already loaded": cities loaded with the first reader must be found
	@Test
	public void testRelatedCityAfterReaderReopen() throws IOException {
		File file = new File(SRC_OBF);
		BinaryMapIndexReader first = read(file);
		BinaryMapIndexReader second = read(file);
		try {
			TownCitiesCache cache = new TownCitiesCache();
			cache.add(first);
			City city = insert(cache, CityType.CITY, "nearby", MapUtils.rhumbDestinationPoint(SAINT_FERREOL, 10000, 0));
			city.setReferenceFile(first);

			Assert.assertTrue(cache.contains(second));
			Assert.assertSame(city, cache.findRelatedCity(SAINT_FERREOL, MAX_DIST));
		} finally {
			first.close();
			second.close();
		}
	}

	private City insert(TownCitiesCache cache, CityType type, String name, LatLon location) {
		City city = createCity(type, name, location);
		int x = MapUtils.get31TileNumberX(location.getLongitude());
		int y = MapUtils.get31TileNumberY(location.getLatitude());
		cache.insertCityQR(city, new QuadRect(x, y, x, y));
		return city;
	}

	private City createCity(CityType type, String name, LatLon location) {
		City city = new City(type);
		city.setName(name);
		city.setLocation(location);
		return city;
	}

	private String name(City city) {
		return city == null ? null : city.getName();
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
