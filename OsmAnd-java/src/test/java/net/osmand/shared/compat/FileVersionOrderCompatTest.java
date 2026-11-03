package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.shared.util.KAlgorithms;
import net.osmand.util.Algorithms;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * {@link KAlgorithms#compareFileVersions} against {@code Algorithms.getStringVersionComparator}.
 *
 * The order matters beyond tidiness: it is the order the app consults its obf files in, so the
 * newest build of a region answers first and a live update lands on top of the file it belongs to.
 * iOS builds the same list for the shared travel helper, and a list ordered differently there would
 * show a route from a stale file while android showed the fresh one.
 */
public class FileVersionOrderCompatTest {

	@Test
	public void orderOfRealFileNamesIsTheSame() {
		assertSameOrder(names());
	}

	@Test
	public void orderOfTheTestObfFilesIsTheSame() {
		List<String> names = new ArrayList<>();
		for (File file : TestObf.files()) {
			names.add(file.getName());
		}
		assertSameOrder(names);
	}

	/** Names built out of the pieces that decide the order, in every combination that can occur. */
	@Test
	public void orderOfGeneratedNamesIsTheSame() {
		List<String> names = new ArrayList<>();
		for (String region : new String[] {"Slovakia", "slovakia", "Slovakia_east", "Czech-republic", "World"}) {
			for (String stamp : new String[] {"", "_23_09_15", "_00_00_00", "_2"}) {
				for (String ext : new String[] {".obf", ".travel.obf", ".road.obf", ""}) {
					names.add(region + stamp + ext);
				}
			}
		}
		assertSameOrder(names);
	}

	/** Every pair compared both ways round, which a sort alone would not reach. */
	@Test
	public void everyPairComparesTheSame() {
		List<String> names = names();
		int pairs = 0;
		for (String a : names) {
			for (String b : names) {
				int java = Algorithms.getStringVersionComparator().compare(a, b);
				int copy = KAlgorithms.INSTANCE.compareFileVersions(a, b);
				assertEquals("compare '" + a + "' to '" + b + "'", Integer.signum(java), Integer.signum(copy));
				pairs++;
			}
		}
		assertTrue("pairs compared: " + pairs, pairs > 400);
	}

	/** The same shuffled list sorted by both comparators comes out in the same order. */
	@Test
	public void aShuffledListSortsTheSame() {
		List<String> names = names();
		Random random = new Random(20260919);
		for (int round = 0; round < 50; round++) {
			Collections.shuffle(names, random);
			assertSameOrder(names);
		}
	}

	private static void assertSameOrder(List<String> names) {
		List<String> byJava = new ArrayList<>(names);
		byJava.sort(Algorithms.getStringVersionComparator());

		List<String> byCopy = new ArrayList<>(names);
		byCopy.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return KAlgorithms.INSTANCE.compareFileVersions(o1, o2);
			}
		});

		assertEquals("sorted order", byJava, byCopy);
	}

	/** Names of the shape the resources manager hands over, including the awkward ones. */
	private static List<String> names() {
		List<String> names = new ArrayList<>();
		Collections.addAll(names,
				"Slovakia_europe_2.obf", "Slovakia_europe.obf", "Slovakia_europe_23_09_15.obf",
				"Czech-republic_europe_2.obf", "Czech-republic_europe.obf",
				"World_basemap_mini.obf", "World_basemap.obf", "World_seamarks.obf",
				"World_wikivoyage.travel.obf", "Slovakia_europe.travel.obf",
				"Austria_europe_2.road.obf", "Austria_europe.road.obf",
				"Slovakia_europe_23_09_15.live.obf", "slovakia_europe_2.obf",
				"noDigitsHere.obf", "noDigitsHere", "2", "_2", "", ".obf", "a.b.c.obf");
		return names;
	}
}
