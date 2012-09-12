package net.osmand.data;

import static org.junit.Assert.*;

import net.osmand.osm.LatLon;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

import org.junit.Before;
import org.junit.Test;

public class MultipolygonTest {

	private Multipolygon testee;
	private Way poly1_1_of_2;
	private Way poly1_2_of_2;
	private int wayid;
	private Way poly2;
	private Way openedBaseCircle;
	private Way closedBaseCircle;

	@Before
	public void setUp() 
	{
		testee = new Multipolygon();
		poly1_1_of_2 = polygon(n(0,0),n(1,0),n(1,1),n(1,2));
		poly1_2_of_2 = polygon(n(1,2),n(0,2),n(-1,2),n(0,0));
		poly2 = polygon(n(4,4), n(4,5), n(3,5), n(4,4));
		openedBaseCircle = polygon(n(1,-1), n(1,1), n(-1,1), n(-1,-1));
		closedBaseCircle = polygon(n(1,-1), n(1,1), n(-1,1), n(-1,-1), n(1,-1));
	}
	
	public Way polygon(Node... n) {
		Way way = new Way(wayid++);
		for (Node nn : n) {
			way.addNode(nn);
		}
		return way;
	}

	public Way scale(int i, Way w) {
		Way way = new Way(wayid++);
		for (Node nn : w.getNodes()) {
			way.addNode(n(i*(int)nn.getLatitude(),i*(int)nn.getLongitude()));
		}
		return way;
	}

	public Way move(int i, int j, Way w) {
		Way way = new Way(wayid++);
		for (Node nn : w.getNodes()) {
			way.addNode(n(i+(int)nn.getLatitude(),j+(int)nn.getLongitude()));
		}
		return way;
	}

	public Node n(int i, int j) {
		return new Node(i, j, i*i + j*j + i*j + i + j); //Node has ID derived from i,j
	}

	@Test
	public void test_twoWayPolygon() {
		testee.addOuterWay(poly1_1_of_2);
		testee.addOuterWay(poly1_2_of_2);
		assertEquals(1, testee.countOuterPolygons());
		assertFalse(testee.hasOpenedPolygons());
	}

	@Test
	public void test_oneWayPolygon() {
		testee.addOuterWay(poly2);
		assertEquals(1, testee.countOuterPolygons());
		assertFalse(testee.hasOpenedPolygons());
	}

	@Test
	public void test_containsPoint()
	{
		testee.addOuterWay(scale(4,poly2));
		LatLon center = testee.getCenterPoint();
		assertTrue(testee.containsPoint(center));
	}
	
	@Test
	public void test_containsPointOpenedCircle()
	{
		testee.addOuterWay(scale(4,openedBaseCircle));
		LatLon center = testee.getCenterPoint();
		assertTrue(testee.containsPoint(center));
	}
	
	@Test
	public void test_containsPointClosedCircle()
	{
		testee.addOuterWay(scale(4,openedBaseCircle));
		LatLon center = testee.getCenterPoint();
		assertTrue(testee.containsPoint(center));
	}
	
	@Test
	public void test_oneInnerRingOneOuterRingOpenedCircle()
	{
		test_oneInnerRingOneOuterRing(openedBaseCircle);
	}

	@Test
	public void test_oneInnerRingOneOuterRingClosedCircle()
	{
		test_oneInnerRingOneOuterRing(closedBaseCircle);
	}

	public void test_oneInnerRingOneOuterRing(Way polygon)
	{
		testee.addOuterWay(scale(4,polygon));
		LatLon center = testee.getCenterPoint();
		assertTrue(testee.containsPoint(center));

		Multipolygon mpoly2 = new Multipolygon();
		mpoly2.addOuterWay(polygon);
		
		assertTrue(testee.containsPoint(mpoly2.getCenterPoint()));
		
		testee.addInnerWay(polygon);
		
		assertFalse(testee.containsPoint(mpoly2.getCenterPoint()));
	}

	@Test
	public void test_twoInnerRingsOneOuterRingOpenedCircle()
	{
		test_twoInnerRingsOneOuterRing(openedBaseCircle);
	}
	
	@Test
	public void test_twoInnerRingsOneOuterRingClosedCircle()
	{
		test_twoInnerRingsOneOuterRing(closedBaseCircle);
	}
	
	public void test_twoInnerRingsOneOuterRing(Way polygon)
	{
		testee.addOuterWay(scale(40,polygon));
		LatLon center = testee.getCenterPoint();
		assertTrue(testee.containsPoint(center));
		
		Multipolygon mpoly2 = new Multipolygon();
		mpoly2.addOuterWay(polygon);
		Multipolygon movepoly2 = new Multipolygon();
		movepoly2.addOuterWay(move(10,10,polygon));

		assertTrue(testee.containsPoint(mpoly2.getCenterPoint()));
		assertTrue(testee.containsPoint(movepoly2.getCenterPoint()));

		testee.addInnerWay(polygon);
		testee.addInnerWay(move(10,10,polygon));

		assertFalse(testee.containsPoint(mpoly2.getCenterPoint()));
		assertFalse(testee.containsPoint(movepoly2.getCenterPoint()));
	}

	@Test
	public void test_multipolygon1twoWay2oneWay()
	{
		testee.addOuterWay(poly1_1_of_2);
		testee.addOuterWay(poly1_2_of_2);
		testee.addOuterWay(poly2);
		assertEquals(2, testee.countOuterPolygons());
		assertFalse(testee.hasOpenedPolygons());
	}

	@Test
	public void test_firstEmptyWayThanOpenedWay()
	{
		testee.addOuterWay(new Way(111));
		testee.addOuterWay(poly1_1_of_2);
		assertEquals(1, testee.countOuterPolygons());
		// FIXME assertTrue(testee.hasOpenedPolygons());
	}

	@Test
	public void test_mergingExistingPolygons()
	{
		Way part1 = polygon(n(1,1),n(1,2),n(1,3));
		Way part2 = polygon(n(1,3),n(1,4),n(1,5));
		Way part3 = polygon(n(1,5),n(1,6),n(1,2));
		testee.addOuterWay(part1);
		testee.addOuterWay(part3);
		testee.addOuterWay(part2);
		assertEquals(1, testee.countOuterPolygons());
		assertTrue(testee.hasOpenedPolygons());
	}

	@Test
	public void test_mergingExistingPolygonsReversed()
	{
		Way part1 = polygon(n(1,3),n(1,2),n(1,1));
		Way part2 = polygon(n(1,3),n(1,4),n(1,5));
		Way part3 = polygon(n(1,5),n(1,6),n(1,2));
		testee.addOuterWay(part1);
		testee.addOuterWay(part3);
		testee.addOuterWay(part2);
		assertEquals(1, testee.countOuterPolygons());
		assertTrue(testee.hasOpenedPolygons());
	}
	

}
