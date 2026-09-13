package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.routing.RouteTypeRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link net.osmand.shared.binary.BinaryMapIndexReader} is a copy of the routing part of
 * {@link BinaryMapIndexReader}; this opens every obf the routing tests ship with in both and
 * compares what they read: the routing sections with their rules and box trees, and then every
 * road of every box, field for field.
 *
 * Both readers are opened fresh for every case, so that what they have read so far - the box trees
 * expand as they are searched - is the same on both sides and can be compared too.
 */
@RunWith(Parameterized.class)
public class ObfReaderCompatTest {

	private static int roadsCompared;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	public ObfReaderCompatTest(String name, File file) throws IOException {
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void roadsWereCompared() {
		System.out.println("ObfReaderCompatTest: " + roadsCompared + " roads compared");
		assertTrue("roads compared: " + roadsCompared, roadsCompared > 10000);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() {
		List<Object[]> data = new ArrayList<>();
		for (File file : TestObf.files()) {
			data.add(new Object[] {file.getName(), file});
		}
		return data;
	}

	@Test
	public void testStructure() {
		assertEquals("version", java.getVersion(), copy.getVersion());
		assertEquals("dateCreated", java.getDateCreated(), copy.getDateCreated());
		assertEquals("hh routing", !java.getHHRoutingIndexes().isEmpty(), copy.hasHHRoutingIndexes());
		assertEquals("routing sections", java.getRoutingIndexes().size(), copy.getRoutingIndexes().size());
		for (int i = 0; i < java.getRoutingIndexes().size(); i++) {
			assertRegion("region " + i, java.getRoutingIndexes().get(i), copy.getRoutingIndexes().get(i));
		}
	}

	@Test
	public void testRoads() throws IOException {
		SearchRequest<BinaryMapDataObject> jreq = BinaryMapIndexReader.buildSearchRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 16, null);
		net.osmand.shared.binary.SearchRequest kreq = net.osmand.shared.binary.SearchRequest.buildSearchRouteRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
		for (int r = 0; r < java.getRoutingIndexes().size(); r++) {
			RouteRegion jregion = java.getRoutingIndexes().get(r);
			net.osmand.shared.routing.RouteRegion kregion = copy.getRoutingIndexes().get(r);
			for (int base = 0; base < 2; base++) {
				String m = "region " + r + (base == 1 ? " base" : "");
				List<RouteSubregion> jboxes = java.searchRouteIndexTree(jreq,
						base == 1 ? jregion.getBaseSubregions() : jregion.getSubregions());
				List<net.osmand.shared.routing.RouteSubregion> kboxes = copy.searchRouteIndexTree(kreq,
						base == 1 ? kregion.getBaseSubregions() : kregion.getSubregions());
				assertEquals(m + " boxes to load", jboxes.size(), kboxes.size());
				for (int b = 0; b < jboxes.size(); b++) {
					String bm = m + " box " + b;
					assertBox(bm, jboxes.get(b), kboxes.get(b));
					List<RouteDataObject> jroads = java.loadRouteIndexData(jboxes.get(b));
					List<net.osmand.shared.routing.RouteDataObject> kroads = copy.loadRouteIndexData(kboxes.get(b));
					assertEquals(bm + " roads", jroads.size(), kroads.size());
					for (int i = 0; i < jroads.size(); i++) {
						assertRoad(bm + " road " + i, jroads.get(i), kroads.get(i));
						if (jroads.get(i) != null) {
							roadsCompared++;
						}
					}
				}
			}
			// what the reader knows about the region after the roads went through it
			assertRegion("region " + r + " after loading", jregion, kregion);
		}
	}

	private static void assertRegion(String m, RouteRegion j, net.osmand.shared.routing.RouteRegion k) {
		assertEquals(m + " name", j.getName(), k.getName());
		assertEquals(m + " filePointer", j.getFilePointer(), k.getFilePointer());
		assertEquals(m + " length", j.getLength(), k.getLength());
		assertEquals(m + " regionsRead", j.regionsRead, k.regionsRead);
		assertEquals(m + " routeEncodingRulesBytes", j.routeEncodingRulesBytes, k.routeEncodingRulesBytes);
		assertEquals(m + " rules", j.routeEncodingRules.size(), k.routeEncodingRules.size());
		for (int i = 0; i < j.routeEncodingRules.size(); i++) {
			BinaryMapRouteReaderAdapter.RouteTypeRule jr = j.routeEncodingRules.get(i);
			RouteTypeRule kr = k.quickGetEncodingRule(i);
			assertEquals(m + " rule " + i + " present", jr == null, kr == null);
			if (jr != null) {
				assertEquals(m + " rule " + i + " tag", jr.getTag(), kr.getTag());
				assertEquals(m + " rule " + i + " value", jr.getValue(), kr.getValue());
				assertEquals(m + " rule " + i + " type", jr.getType(), kr.getType());
				assertEquals(m + " rule " + i + " conditional", jr.conditional(), kr.conditional());
			}
		}
		assertEquals(m + " nameTypeRule", j.getNameTypeRule(), k.getNameTypeRule());
		assertEquals(m + " refTypeRule", j.getRefTypeRule(), k.getRefTypeRule());
		assertEquals(m + " directionForward", j.directionForward, k.directionForward);
		assertEquals(m + " directionBackward", j.directionBackward, k.directionBackward);
		assertEquals(m + " maxheightForward", j.maxheightForward, k.maxheightForward);
		assertEquals(m + " maxheightBackward", j.maxheightBackward, k.maxheightBackward);
		assertEquals(m + " directionTrafficSignalsForward", j.directionTrafficSignalsForward, k.directionTrafficSignalsForward);
		assertEquals(m + " directionTrafficSignalsBackward", j.directionTrafficSignalsBackward, k.directionTrafficSignalsBackward);
		assertEquals(m + " trafficSignals", j.trafficSignals, k.trafficSignals);
		assertEquals(m + " stopSign", j.stopSign, k.stopSign);
		assertEquals(m + " stopMinor", j.stopMinor, k.stopMinor);
		assertEquals(m + " giveWaySign", j.giveWaySign, k.giveWaySign);
		assertBoxes(m + " subregions", j.getSubregions(), k.getSubregions());
		assertBoxes(m + " basesubregions", j.getBaseSubregions(), k.getBaseSubregions());
	}

	private static void assertBoxes(String m, List<RouteSubregion> j, List<net.osmand.shared.routing.RouteSubregion> k) {
		assertEquals(m + " present", j == null, k == null);
		if (j == null) {
			return;
		}
		assertEquals(m + " size", j.size(), k.size());
		for (int i = 0; i < j.size(); i++) {
			assertBox(m + "[" + i + "]", j.get(i), k.get(i));
		}
	}

	/** The box, and the tree under it as far as both have read it. */
	private static void assertBox(String m, RouteSubregion j, net.osmand.shared.routing.RouteSubregion k) {
		assertEquals(m + " filePointer", j.filePointer, k.filePointer);
		assertEquals(m + " length", j.length, k.length);
		assertEquals(m + " left", j.left, k.left);
		assertEquals(m + " right", j.right, k.right);
		assertEquals(m + " top", j.top, k.top);
		assertEquals(m + " bottom", j.bottom, k.bottom);
		assertEquals(m + " shiftToData", j.shiftToData, k.shiftToData);
		assertBoxes(m + " children", j.subregions, k.subregions);
	}

	static void assertRoad(String m, RouteDataObject j, net.osmand.shared.routing.RouteDataObject k) {
		assertEquals(m + " present", j == null, k == null);
		if (j == null) {
			return;
		}
		assertEquals(m + " id", j.id, k.id);
		assertArrayEquals(m + " pointsX", j.pointsX, k.pointsX);
		assertArrayEquals(m + " pointsY", j.pointsY, k.pointsY);
		assertArrayEquals(m + " types", j.types, k.types);
		assertArrayEquals(m + " nameIds", j.nameIds, k.nameIds);
		assertArrayEquals(m + " restrictions", j.restrictions, k.restrictions);
		assertArrayEquals(m + " restrictionsVia", j.restrictionsVia, k.restrictionsVia);
		assertEquals(m + " pointTypes", Arrays.deepToString(j.pointTypes), Arrays.deepToString(k.pointTypes));
		assertEquals(m + " pointNameTypes", Arrays.deepToString(j.pointNameTypes), Arrays.deepToString(k.pointNameTypes));
		assertEquals(m + " pointNames", Arrays.deepToString(j.pointNames), Arrays.deepToString(k.pointNames));
		if (j.names == null) {
			assertNull(m + " names", k.names);
		} else {
			assertNotNull(m + " names", k.names);
			int[] jkeys = j.names.keys();
			int[] kkeys = k.names.keys();
			Arrays.sort(jkeys);
			Arrays.sort(kkeys);
			assertArrayEquals(m + " name keys", jkeys, kkeys);
			for (int key : jkeys) {
				assertEquals(m + " name " + key, j.names.get(key), k.names.get(key));
			}
		}
	}
}
