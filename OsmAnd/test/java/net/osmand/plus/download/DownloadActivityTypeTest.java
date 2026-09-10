package net.osmand.plus.download;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.util.Algorithms;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DownloadActivityTypeTest {

	@Test
	public void removeFileVersionSuffixOnlyRemovesIntegerSuffix() {
		assertEquals("Ukraine_europe.obf.zip", Algorithms.removeFileVersionSuffix("Ukraine_europe_2.obf.zip"));
		assertEquals("custom_map_test.obf.zip",
				Algorithms.removeFileVersionSuffix("custom_map_test.obf.zip"));
		assertEquals("my.custom_map.obf.zip",
				Algorithms.removeFileVersionSuffix("my.custom_map_2.obf.zip"));
		assertEquals("custom_map_v2", Algorithms.removeFileVersionSuffix("custom_map_v2"));
	}

	@Test
	public void getBasenameHandlesVersionAndSemanticSuffixes() {
		assertEquals("waterway", getBasename("waterway.obf.zip"));
		assertEquals("Ukraine_europe", getBasename("Ukraine_europe_2.obf.zip"));
		assertEquals("Us_nfs_roads", getBasename("Us_nfs_roads.obf.zip"));
		assertEquals("my.file", getBasename("my.file_2.obf.zip"));
		assertEquals("my.file_test", getBasename("my.file_test.obf.zip"));
		assertEquals("foo", getBasename("foo_2.wiki.obf.zip"));
		assertEquals("my_map", getBasename("my_map_2"));
		assertEquals("my_map_test", getBasename("my_map_test"));
	}

	@Test
	public void getTargetFileNameUsesObfBasename() {
		assertEquals("waterway.obf", getTargetFileName("waterway.obf.zip"));
		assertEquals("my.custom_map.obf", getTargetFileName("my.custom_map_2.obf.zip"));
		assertEquals("Us_nfs_roads.obf", getTargetFileName("Us_nfs_roads_1.obf.zip"));
	}

	private String getBasename(String fileName) {
		return DownloadActivityType.NORMAL_FILE.getBasename(fileName, DownloadActivityType.NORMAL_FILE);
	}

	private String getTargetFileName(String fileName) {
		return new IndexItem(fileName, null, 0, null, 0, 0,
				DownloadActivityType.NORMAL_FILE, false, null).getTargetFileName();
	}
}
