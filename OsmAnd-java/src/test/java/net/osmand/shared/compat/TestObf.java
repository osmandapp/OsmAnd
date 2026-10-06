package net.osmand.shared.compat;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

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

	/**
	 * The obf files of the search tests, {@code src/test/resources/search/*.obf.gz}, which
	 * {@code collectTestResources} copies out of the resources repository before the tests run:
	 * small extracts of real maps, each with the address section its search case needs. They are
	 * unpacked into {@code build/search-obf} and unpacked again only when the archive is newer.
	 * Empty when {@code OSMAND_OBF_CORPUS} is set, so that the corpus is all a test reads.
	 */
	public static List<File> searchFiles() throws IOException {
		List<File> files = new ArrayList<>();
		String corpus = System.getenv("OSMAND_OBF_CORPUS");
		File[] archives = new File("src/test/resources/search").listFiles((dir, name) -> name.endsWith(".obf.gz"));
		if ((corpus != null && !corpus.isEmpty()) || archives == null) {
			return files;
		}
		Arrays.sort(archives);
		File dir = new File("build/search-obf");
		if (!dir.isDirectory() && !dir.mkdirs()) {
			throw new IOException("Cannot create " + dir);
		}
		for (File archive : archives) {
			String name = archive.getName();
			File obf = new File(dir, name.substring(0, name.length() - ".gz".length()));
			if (!obf.exists() || obf.lastModified() < archive.lastModified()) {
				File tmp = new File(dir, obf.getName() + ".tmp");
				try (InputStream in = new GZIPInputStream(new FileInputStream(archive));
					 OutputStream out = new FileOutputStream(tmp)) {
					in.transferTo(out);
				}
				if (obf.exists() && !obf.delete()) {
					throw new IOException("Cannot replace " + obf);
				}
				if (!tmp.renameTo(obf)) {
					throw new IOException("Cannot move " + tmp + " to " + obf);
				}
			}
			files.add(obf);
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
