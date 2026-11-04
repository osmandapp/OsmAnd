package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.QuadRect;
import net.osmand.router.HHJavaAccess;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.routing.HHRouteDataStructure;
import net.osmand.shared.routing.HHRouteRegionPointsCtx;
import net.osmand.shared.routing.NetworkDBSegment;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link net.osmand.shared.binary.BinaryHHRouteReaderAdapter} is a copy of
 * {@link net.osmand.binary.BinaryHHRouteReaderAdapter}; this opens every obf with an HH section
 * in both readers and compares what they read: the sections' headers, then, for every parameter
 * set of every section, every vertex field for field and every edge of every vertex in both
 * directions - which is the whole section, read the way a route calculation reads it.
 *
 * The obf files the routing tests ship with carry one such section. The real maps in
 * {@code OSMAND_OBF_DIRECTORY}, when the variable is set, are compared too; they are not in the
 * repository.
 */
@RunWith(Parameterized.class)
public class HHReaderCompatTest {

	private static int pointsCompared;
	private static int edgesCompared;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	public HHReaderCompatTest(String name, File file) throws IOException {
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void graphWasCompared() {
		System.out.println("HHReaderCompatTest: " + pointsCompared + " vertices and " + edgesCompared + " edges compared");
		assertTrue("vertices compared: " + pointsCompared, pointsCompared > 1000);
		assertTrue("edges compared: " + edgesCompared, edgesCompared > pointsCompared);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() throws IOException {
		List<File> files = new ArrayList<>(TestObf.files());
		String directory = System.getenv("OSMAND_OBF_DIRECTORY");
		if (directory != null) {
			File[] maps = new File(directory).listFiles((dir, name) -> name.endsWith(".obf"));
			if (maps != null) {
				Arrays.sort(maps);
				files.addAll(Arrays.asList(maps));
			}
		}
		List<Object[]> data = new ArrayList<>();
		for (File file : files) {
			if (!file.exists()) {
				continue;
			}
			BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
			boolean hh = !reader.getHHRoutingIndexes().isEmpty();
			reader.close();
			if (hh) {
				data.add(new Object[] {file.getName(), file});
			}
		}
		assertTrue("no obf with an HH section among the test resources", !data.isEmpty());
		return data;
	}

	@Test
	public void testSections() {
		assertEquals("sections", java.getHHRoutingIndexes().size(), copy.getHHRoutingIndexes().size());
		for (int i = 0; i < java.getHHRoutingIndexes().size(); i++) {
			assertSection("section " + i, java.getHHRoutingIndexes().get(i), copy.getHHRoutingIndexes().get(i));
		}
	}

	@Test
	public void testGraph() throws IOException, SQLException {
		for (int r = 0; r < java.getHHRoutingIndexes().size(); r++) {
			HHRouteRegion jreg = java.getHHRoutingIndexes().get(r);
			net.osmand.shared.binary.HHRouteRegion kreg = copy.getHHRoutingIndexes().get(r);
			for (int profile = 0; profile < jreg.profileParams.size(); profile++) {
				String m = jreg.profile + " [" + jreg.profileParams.get(profile) + "]";
				HHRoutingContext<NetworkDBPoint> jctx = HHJavaAccess.load(java, jreg, profile);
				net.osmand.shared.routing.HHRoutingContext kctx = load(kreg, profile);
				assertSection(m + " after loading", jreg, kreg);
				assertGraph(m, jctx, kctx);
			}
		}
	}

	private net.osmand.shared.routing.HHRoutingContext load(net.osmand.shared.binary.HHRouteRegion region, int routingProfile) {
		net.osmand.shared.routing.HHRoutingContext hctx = new net.osmand.shared.routing.HHRoutingContext();
		hctx.regions.add(new HHRouteRegionPointsCtx((short) 0, region, copy, routingProfile));
		hctx.pointsById = hctx.loadNetworkPoints();
		hctx.clusterOutPoints = HHRouteDataStructure.groupByClusters(hctx.pointsById, true);
		hctx.clusterInPoints = HHRouteDataStructure.groupByClusters(hctx.pointsById, false);
		for (net.osmand.shared.routing.NetworkDBPoint pnt : hctx.pointsById.values()) {
			pnt.markSegmentsNotLoaded();
			hctx.regions.get(pnt.mapId).pntsByFileId.put(pnt.fileId, pnt);
		}
		return hctx;
	}

	private static void assertGraph(String m, HHRoutingContext<NetworkDBPoint> jctx,
			net.osmand.shared.routing.HHRoutingContext kctx) throws IOException, SQLException {
		long[] keys = HHJavaAccess.points(jctx).keys();
		Arrays.sort(keys);
		assertEquals(m + " vertices", keys.length, kctx.pointsById.size());
		for (long key : keys) {
			NetworkDBPoint j = HHJavaAccess.points(jctx).get(key);
			net.osmand.shared.routing.NetworkDBPoint k = kctx.pointsById.get(key);
			String pm = m + " vertex " + key;
			assertNotNull(pm, k);
			assertPoint(pm, j, k);
			pointsCompared++;
		}
		for (long key : keys) {
			NetworkDBPoint j = HHJavaAccess.points(jctx).get(key);
			net.osmand.shared.routing.NetworkDBPoint k = kctx.pointsById.get(key);
			String pm = m + " vertex " + key;
			for (boolean reverse : new boolean[] {false, true}) {
				String em = pm + (reverse ? " in" : " out");
				assertEquals(em + " loaded", jctx.loadNetworkSegmentPoint(j, reverse), kctx.loadNetworkSegmentPoint(k, reverse));
				List<String> jedges = HHJavaAccess.edges(j, reverse);
				List<NetworkDBSegment> kedges = k.connected(reverse);
				if (jedges == null) {
					assertNull(em, kedges);
					continue;
				}
				assertNotNull(em, kedges);
				assertEquals(em + " edges", jedges.size(), kedges.size());
				for (int i = 0; i < jedges.size(); i++) {
					NetworkDBSegment s = kedges.get(i);
					assertEquals(em + " edge " + i, jedges.get(i),
							s.start.index + ">" + s.end.index + " " + s.dist + " " + s.direction + " " + s.shortcut);
					edgesCompared++;
				}
			}
		}
	}

	private static void assertPoint(String m, NetworkDBPoint j, net.osmand.shared.routing.NetworkDBPoint k) {
		assertEquals(m + " index", j.index, k.index);
		assertEquals(m + " fileId", j.fileId, k.fileId);
		assertEquals(m + " mapId", j.mapId, k.mapId);
		assertEquals(m + " clusterId", j.clusterId, k.clusterId);
		assertEquals(m + " incomplete", j.incomplete, k.incomplete);
		assertEquals(m + " roadId", j.roadId, k.roadId);
		assertEquals(m + " start", j.start, k.start);
		assertEquals(m + " end", j.end, k.end);
		assertEquals(m + " startX", j.startX, k.startX);
		assertEquals(m + " startY", j.startY, k.startY);
		assertEquals(m + " endX", j.endX, k.endX);
		assertEquals(m + " endY", j.endY, k.endY);
		assertEquals(m + " dual", j.dualPoint == null ? -1 : j.dualPoint.index, k.dualPoint == null ? -1 : k.dualPoint.index);
		assertEquals(m + " geoPntId", j.getGeoPntId(), k.getGeoPntId());
		assertEquals(m + " point", j.getPoint().getLatitude(), k.getPoint().getLatitude(), 0d);
		assertEquals(m + " point", j.getPoint().getLongitude(), k.getPoint().getLongitude(), 0d);
		if (j.tagValues == null) {
			assertNull(m + " tags", k.tagValues);
		} else {
			assertNotNull(m + " tags", k.tagValues);
			assertEquals(m + " tags", j.tagValues.size(), k.tagValues.size());
			for (int i = 0; i < j.tagValues.size(); i++) {
				TagValuePair jt = j.tagValues.get(i);
				net.osmand.shared.binary.TagValuePair kt = k.tagValues.get(i);
				assertEquals(m + " tag " + i, jt.tag, kt.tag);
				assertEquals(m + " tag " + i + " value", jt.value, kt.value);
				assertEquals(m + " tag " + i + " attribute", jt.additionalAttribute, kt.additionalAttribute);
			}
		}
	}

	private static void assertSection(String m, HHRouteRegion j, net.osmand.shared.binary.HHRouteRegion k) {
		assertEquals(m + " filePointer", j.getFilePointer(), k.getFilePointer());
		assertEquals(m + " length", j.getLength(), k.getLength());
		assertEquals(m + " name", j.getName(), k.getName());
		assertEquals(m + " edition", j.edition, k.edition);
		assertEquals(m + " profile", j.profile, k.profile);
		assertEquals(m + " params", j.profileParams, k.profileParams);
		assertEquals(m + " top present", j.top == null, k.top == null);
		if (j.top != null) {
			// the box's 31 coordinates are package private in java; its lat lon box is them converted
			QuadRect jt = j.top.getLatLonBox();
			KQuadRect kt = k.top.getLatLonBox();
			assertEquals(m + " top left", jt.left, kt.getLeft(), 0d);
			assertEquals(m + " top right", jt.right, kt.getRight(), 0d);
			assertEquals(m + " top top", jt.top, kt.getTop(), 0d);
			assertEquals(m + " top bottom", jt.bottom, kt.getBottom(), 0d);
		}
		QuadRect jb = j.getLatLonBbox();
		KQuadRect kb = k.getLatLonBbox();
		assertEquals(m + " bbox left", jb.left, kb.getLeft(), 0d);
		assertEquals(m + " bbox right", jb.right, kb.getRight(), 0d);
		assertEquals(m + " bbox top", jb.top, kb.getTop(), 0d);
		assertEquals(m + " bbox bottom", jb.bottom, kb.getBottom(), 0d);
		assertEquals(m + " rules", j.encodingRules.size(), k.encodingRules.size());
		for (int i = 0; i < j.encodingRules.size(); i++) {
			assertEquals(m + " rule " + i, j.encodingRules.get(i).tag + "=" + j.encodingRules.get(i).value,
					k.encodingRules.get(i).tag + "=" + k.encodingRules.get(i).value);
		}
		assertEquals(m + " blocks present", j.segments == null, k.segments == null);
		if (j.segments != null) {
			assertEquals(m + " blocks", j.segments.size(), k.segments.size());
		}
	}
}
