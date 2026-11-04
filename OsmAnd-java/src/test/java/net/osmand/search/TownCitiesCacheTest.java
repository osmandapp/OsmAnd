package net.osmand.search;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.search.core.SearchCoreFactory.TownCitiesCache;

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

	private File copy(File dir, String name) throws IOException {
		File file = new File(dir, name);
		Files.copy(new File(SRC_OBF).toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return file;
	}

	private BinaryMapIndexReader read(File file) throws IOException {
		return new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
	}
}
