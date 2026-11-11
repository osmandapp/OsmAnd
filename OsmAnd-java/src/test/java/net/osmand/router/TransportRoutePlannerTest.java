package net.osmand.router;

import java.util.Comparator;
import java.util.PriorityQueue;

import gnu.trove.map.hash.TLongObjectHashMap;
import junit.framework.TestCase;

import net.osmand.data.TransportRoute;
import net.osmand.router.TransportRoutePlanner.TransportRouteSegment;

public class TransportRoutePlannerTest extends TestCase {

	public void testQueueKeepsOnlyCheapestEquivalentSegment() {
		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<>(
				Comparator.comparingDouble((TransportRouteSegment segment) -> segment.distFromStart));
		TLongObjectHashMap<TransportRouteSegment> queuedSegments = new TLongObjectHashMap<>();
		TransportRouteSegment parent = createSegment(128, 0, 10, null);
		TransportRouteSegment expensive = createSegment(20, 1, 100, parent);
		TransportRouteSegment cheaper = createSegment(20, 1, 80, parent);

		assertTrue(TransportRoutePlanner.addToQueueIfBetter(queue, queuedSegments, expensive));
		for (int i = 0; i < 1000; i++) {
			TransportRouteSegment duplicate = createSegment(20, 1, 100 + i, parent);
			assertFalse(TransportRoutePlanner.addToQueueIfBetter(queue, queuedSegments, duplicate));
		}
		assertEquals(1, queue.size());
		assertTrue(TransportRoutePlanner.addToQueueIfBetter(queue, queuedSegments, cheaper));

		assertSame(cheaper, TransportRoutePlanner.pollFromQueue(queue, queuedSegments));
		assertNull(TransportRoutePlanner.pollFromQueue(queue, queuedSegments));
	}

	public void testQueueKeepsSegmentsReachedFromDifferentParentRoutes() {
		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<>(
				Comparator.comparingDouble((TransportRouteSegment segment) -> segment.distFromStart));
		TLongObjectHashMap<TransportRouteSegment> queuedSegments = new TLongObjectHashMap<>();
		TransportRouteSegment first = createSegment(20, 1, 100, createSegment(128, 0, 10, null));
		TransportRouteSegment second = createSegment(20, 1, 100, createSegment(256, 0, 10, null));

		assertTrue(TransportRoutePlanner.addToQueueIfBetter(queue, queuedSegments, first));
		assertTrue(TransportRoutePlanner.addToQueueIfBetter(queue, queuedSegments, second));
		assertEquals(2, queue.size());
	}

	private TransportRouteSegment createSegment(long routeId, int stopIndex, double distFromStart,
			TransportRouteSegment parent) {
		TransportRoute route = new TransportRoute();
		route.setId(routeId);
		TransportRouteSegment segment = new TransportRouteSegment(route, stopIndex);
		segment.distFromStart = distFromStart;
		segment.parentRoute = parent;
		return segment;
	}
}
