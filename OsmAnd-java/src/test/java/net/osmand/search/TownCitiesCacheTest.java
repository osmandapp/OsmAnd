package net.osmand.search;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.TownCitiesCache;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TownCitiesCacheTest {

	private static final String SRC_OBF = "src/test/resources/Turn_lanes_test.obf";
	private static final String CITY_NAME = "Turn lanes test";
	private static final LatLon CITY_LOCATION = new LatLon(45.7222, 35.5385);

	@BeforeClass
	public static void setUp() {
		SearchUICoreTest.defaultSetup();
	}

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

	// Cities are loaded from every address file now, so several files of the same region
	// (ex. a map next to its live updates named by day) must not produce duplicate results
	@Test
	public void testNoDuplicateCitiesWithSeveralFilesOfSameRegion() throws IOException {
		File dir = Files.createTempDirectory("town-cities-cache-dup").toFile();
		List<File> files = new ArrayList<>();
		files.add(copy(dir, "same_region_2.obf"));
		files.add(copy(dir, "same_region_26_09_12.obf"));
		files.add(copy(dir, "same_region_26_09_13.obf"));
		files.add(copy(dir, "same_region_26_09_14.obf"));
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		try {
			for (File file : files) {
				readers.add(read(file));
			}
			SearchSettings settings = new SearchSettings(new ArrayList<>()).setLang("en", false);
			settings.setOfflineIndexes(readers);
			settings = settings.setOriginalLocation(CITY_LOCATION);

			SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
			core.init();
			// second search reuses the cache filled by the first one
			for (int i = 0; i < 2; i++) {
				Assert.assertEquals(1, countCities(search(core, settings), CITY_NAME));
			}
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
			for (File file : files) {
				file.delete();
			}
			dir.delete();
		}
	}

	private List<SearchResult> search(SearchUICore core, SearchSettings settings) throws IOException {
		SearchPhrase phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase(CITY_NAME, settings);
		SearchResultMatcher matcher = new SearchResultMatcher(new ResultMatcher<SearchResult>() {
			@Override
			public boolean publish(SearchResult object) {
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		}, phrase, 1, new AtomicInteger(1), -1);
		core.searchInternal(phrase, matcher);
		SearchResultCollection collection = new SearchResultCollection(phrase);
		collection.addSearchResults(matcher.getRequestResults(), true, true);
		return collection.getCurrentSearchResults();
	}

	private int countCities(List<SearchResult> results, String name) {
		int count = 0;
		for (SearchResult result : results) {
			if (result.objectType == ObjectType.CITY && name.equals(result.localeName)) {
				count++;
			}
		}
		return count;
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
