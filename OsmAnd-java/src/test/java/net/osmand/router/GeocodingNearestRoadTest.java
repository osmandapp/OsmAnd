package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Reverse geocoding keeps the road nearest to the point. {@link RoutePlannerFrontEnd#findRouteSegment}
 * takes the segment it returns out of the list it fills (3f2ab03914); when
 * {@link GeocodingUtilities#reverseGeocodingSearch} read only that list, the nearest road counted only
 * when the list held it twice, from two files.
 *
 * Over {@code Issue-11345-geo-Westbourne.obf} of the search tests, which gradle copies into
 * {@code src/test/resources/search}.
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "net.osmand.binary.GeocodingNearestRoadTest"
 * </pre>
 */
public class GeocodingNearestRoadTest {

	private static final File MAP = new File("src/test/resources/search/Issue-11345-geo-Westbourne.obf.gz");
	/** Building 9 of Westbourne Gardens. */
	private static final double LAT = 51.51745;
	private static final double LON = -0.18947124;

	private static File obf;
	private static BinaryMapIndexReader reader;
	private static RoutingContext ctx;

	@BeforeClass
	public static void open() throws IOException {
		obf = File.createTempFile("geocoding-nearest-road", ".obf");
		try (InputStream in = new GZIPInputStream(new FileInputStream(MAP))) {
			Files.copy(in, obf.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		reader = new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf);
		ctx = GeocodingUtilities.buildDefaultContextForPOI(reader);
	}

	@AfterClass
	public static void close() throws IOException {
		if (ctx != null) {
			for (BinaryMapIndexReader r : ctx.map.keySet()) {
				r.close();
			}
		}
		if (reader != null) {
			reader.close();
		}
		if (obf != null) {
			obf.delete();
		}
	}

	@Test
	public void nearestRoadIsAmongTheRoadsFound() throws IOException {
		RouteSegmentPoint nearest = new RoutePlannerFrontEnd().findRouteSegment(LAT, LON, ctx, null, false, true);
		assertNotNull(nearest);
		List<GeocodingResult> found = new GeocodingUtilities().reverseGeocodingSearch(ctx, LAT, LON, true);
		boolean listed = found.stream().anyMatch(r -> r.point.getRoad().getId() == nearest.getRoad().getId());
		assertTrue("the nearest road, " + road(nearest) + ", is not among " + found.stream().map(r -> road(r.point))
				.collect(Collectors.joining(", ", "[", "]")), listed);
	}

	/** Without the nearest road, Newton Road at 171 m, the building got Monmouth Place at 276 m. */
	@Test
	public void buildingGetsTheNameOfTheNearestRoad() throws IOException {
		GeocodingUtilities utils = new GeocodingUtilities();
		List<GeocodingResult> found = utils.sortGeocodingResults(Collections.singletonList(reader),
				utils.reverseGeocodingSearch(ctx, LAT, LON, false));
		assertFalse(found.isEmpty());
		assertEquals(found.toString(), "Newton Road", found.get(0).streetName);
	}

	private static String road(RouteSegmentPoint p) {
		return p.getRoad().getName() + " " + p.getRoad().getId();
	}
}
