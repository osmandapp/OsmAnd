package net.osmand.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.shared.compat.TestObf;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * The app lists the route types and route point categories of every installed map at startup:
 * that scan must not leave the poi tables of all the maps in memory.
 */
public class PoiSubTypesByPrefixTest {

	@BeforeClass
	public static void readPoiTypes() {
		MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
	}

	@Test
	public void scanKeepsNoTablesAndFindsTheSameSubTypes() throws IOException {
		int found = 0;
		for (File file : TestObf.files()) {
			BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
			try {
				List<List<String>> scanned = new ArrayList<>();
				for (char c = 'a'; c <= 'z'; c++) {
					scanned.add(names(reader.searchPoiSubTypesByPrefix(String.valueOf(c))));
				}
				for (PoiRegion region : reader.getPoiIndexes()) {
					assertTrue(file.getName() + ": poi tables kept after the scan", region.getCategories().isEmpty());
				}
				reader.initCategories();
				for (char c = 'a'; c <= 'z'; c++) {
					List<String> expected = names(reader.searchPoiSubTypesByPrefix(String.valueOf(c)));
					assertEquals(file.getName() + " '" + c + "'", expected, scanned.get(c - 'a'));
					found += expected.size();
				}
			} finally {
				reader.close();
			}
		}
		assertTrue("subtypes found: " + found, found > 0);
	}

	private static List<String> names(List<PoiSubType> subTypes) {
		List<String> names = new ArrayList<>();
		for (PoiSubType subType : subTypes) {
			names.add(subType.name);
		}
		return names;
	}
}
