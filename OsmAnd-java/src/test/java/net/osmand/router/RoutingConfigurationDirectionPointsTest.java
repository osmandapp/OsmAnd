package net.osmand.router;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.data.KQuadTree;
import net.osmand.shared.routing.DirectionPoint;
import net.osmand.shared.routing.NativeDirectionPoint;
import net.osmand.util.MapUtils;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Direction points reach the native router through
 * {@link RoutingConfiguration#getNativeDirectionPoints()}, which `java_wrap.cpp` resolves by the
 * descriptor {@code ()[Lnet/osmand/shared/routing/NativeDirectionPoint;}. Nothing fails to compile
 * when that breaks - the points just stop arriving - so the descriptor is pinned here, from the side
 * that owns the method.
 *
 * The rest of this covers what {@link RoutingConfiguration.Builder} does with the points it is
 * given: it hands every calculation its own copies, because the router writes onto a point as it
 * attaches it to a road.
 */
public class RoutingConfigurationDirectionPointsTest {

	private static final double LAT = 50.0;
	private static final double LON = 10.0;

	private static KQuadRect world() {
		return new KQuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	private static KQuadTree<DirectionPoint> treeWithOnePoint(DirectionPoint point) {
		KQuadTree<DirectionPoint> tree = new KQuadTree<>(world(), 15, 0.5f);
		int x = MapUtils.get31TileNumberX(point.getLongitude());
		int y = MapUtils.get31TileNumberY(point.getLatitude());
		tree.insert(point, new KQuadRect(x, y, x, y));
		return tree;
	}

	private static List<DirectionPoint> pointsOf(RoutingConfiguration config) {
		return config.getDirectionPoints().queryInBox(world(), new ArrayList<>());
	}

	private static RoutingConfiguration build(KQuadTree<DirectionPoint> tree) {
		RoutingConfiguration.Builder builder = new RoutingConfiguration.Builder();
		builder.setDirectionPoints(tree);
		return builder.build("car", new RoutingConfiguration.RoutingMemoryLimits(0, 0));
	}

	@Test
	public void testNativeDirectionPointsDescriptorMatchesTheJniLookup() throws Exception {
		Method method = RoutingConfiguration.class.getMethod("getNativeDirectionPoints");
		assertEquals(NativeDirectionPoint[].class, method.getReturnType());
		assertEquals("net.osmand.shared.routing.NativeDirectionPoint",
				method.getReturnType().getComponentType().getName());
	}

	@Test
	public void testNativeDirectionPointsIsEmptyWithoutPoints() {
		RoutingConfiguration config = new RoutingConfiguration.Builder()
				.build("car", new RoutingConfiguration.RoutingMemoryLimits(0, 0));
		assertEquals(0, config.getNativeDirectionPoints().length);
	}

	@Test
	public void testNativeDirectionPointsCarryPlaceAndTags() {
		DirectionPoint point = new DirectionPoint(LAT, LON);
		point.putTag("osmand_dp", "yes");

		NativeDirectionPoint[] points = build(treeWithOnePoint(point)).getNativeDirectionPoints();
		assertEquals(1, points.length);
		assertEquals(MapUtils.get31TileNumberX(LON), points[0].x31);
		assertEquals(MapUtils.get31TileNumberY(LAT), points[0].y31);
		assertArrayEquals(new String[] {"osmand_dp", "yes"}, points[0].tags[0]);
	}

	@Test
	public void testEachCalculationGetsItsOwnPoints() {
		DirectionPoint source = new DirectionPoint(LAT, LON);
		source.putTag("osmand_dp", "yes");
		KQuadTree<DirectionPoint> tree = treeWithOnePoint(source);

		DirectionPoint first = pointsOf(build(tree)).get(0);
		DirectionPoint second = pointsOf(build(tree)).get(0);

		assertNotSame(source, first);
		assertNotSame(first, second);
		assertEquals(source.getTags(), first.getTags());
		assertEquals(source.getTags(), second.getTags());

		// what the router writes on one route must not be there on the next one
		first.distance = 1;
		first.connectedx = 7;
		first.types.add(3);
		assertTrue(second.distance == Double.MAX_VALUE);
		assertEquals(0, second.connectedx);
		assertEquals(0, second.types.size());
		assertEquals(Double.MAX_VALUE, source.distance, 0);
		assertEquals(0, source.types.size());
	}
}
