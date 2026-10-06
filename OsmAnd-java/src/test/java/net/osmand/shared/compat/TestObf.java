package net.osmand.shared.compat;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The obf files the routing tests ship with, opened once, and the roads in them - real input for the
 * java-versus-copy tests, which otherwise would have to invent roads and would invent the easy ones.
 */
public final class TestObf {

	private static List<BinaryMapIndexReader> readers;

	private TestObf() {
	}

	/**
	 * The obf files, in a fixed order: the turn lanes map first, then the routing maps by name.
	 *
	 * {@code OSMAND_OBF_CORPUS} replaces them with the files it names, separated by {@code :}. The
	 * files that ship with the tests are small and were built years ago: none carries poi tag
	 * groups or a top index, so the branches of the poi reader that read those are only reached by
	 * pointing this at a real regional map:
	 * <pre>
	 * OSMAND_OBF_CORPUS=/path/Ukraine_transcarpathia_europe.obf \
	 *   ./gradlew :OsmAnd-java:cleanTest :OsmAnd-java:test --tests "*PoiSearchCompatTest"
	 * </pre>
	 */
	public static List<File> files() {
		String corpus = System.getenv("OSMAND_OBF_CORPUS");
		if (corpus != null && !corpus.isEmpty()) {
			List<File> named = new ArrayList<>();
			for (String path : corpus.split(":")) {
				named.add(new File(path));
			}
			return named;
		}
		List<File> files = new ArrayList<>();
		File resources = new File("src/test/resources");
		files.add(new File(resources, "Turn_lanes_test.obf"));
		File[] routing = new File(resources, "routing").listFiles((dir, name) -> name.endsWith(".obf"));
		if (routing != null) {
			Arrays.sort(routing);
			files.addAll(Arrays.asList(routing));
		}
		return files;
	}

	public static synchronized List<BinaryMapIndexReader> readers() throws IOException {
		if (readers == null) {
			List<BinaryMapIndexReader> opened = new ArrayList<>();
			for (File f : files()) {
				if (f.exists()) {
					opened.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
				}
			}
			readers = opened;
		}
		return readers;
	}

	/** Every routing region of every file, in file order. */
	public static List<RouteRegion> regions() throws IOException {
		List<RouteRegion> regions = new ArrayList<>();
		for (BinaryMapIndexReader reader : readers()) {
			regions.addAll(reader.getRoutingIndexes());
		}
		return regions;
	}

	/** Up to {@code perFile} roads out of each file, whole regions at a time, in file order. */
	public static List<RouteDataObject> roads(int perFile) throws IOException {
		List<RouteDataObject> roads = new ArrayList<>();
		for (BinaryMapIndexReader reader : readers()) {
			int fromThisFile = 0;
			SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
					0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 16, null);
			for (RouteRegion region : reader.getRoutingIndexes()) {
				for (RouteSubregion subregion : reader.searchRouteIndexTree(req, region.getSubregions())) {
					for (RouteDataObject road : reader.loadRouteIndexData(subregion)) {
						if (road != null && fromThisFile < perFile) {
							roads.add(road);
							fromThisFile++;
						}
					}
					if (fromThisFile >= perFile) {
						break;
					}
				}
				if (fromThisFile >= perFile) {
					break;
				}
			}
		}
		return roads;
	}
}
