package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.osm.edit.Node;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.data.KQuadTree;
import net.osmand.shared.routing.DirectionPoint;
import net.osmand.shared.routing.NativeDirectionPoint;
import net.osmand.shared.routing.RoutingConfiguration;
import net.osmand.util.MapUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link DirectionPoint} is a copy of {@link net.osmand.router.RoutingConfiguration.DirectionPoint},
 * with one difference by design: java's is an OSM {@link Node}, the copy carries the place and the
 * tags itself. This gives both builders the same points and compares what a configuration hands the
 * router - the points, their tags and angle, the flattened form - and that each build gets its own.
 */
public class DirectionPointsCompatTest {

	private static final double[][] PLACES = {{50.0, 10.0}, {50.001, 10.002}, {-33.9, 151.2}, {0, 0}, {89.9, -179.9}};

	private static final String[][] TAGS = {
			{"osmand_dp", "yes"}, {"Osmand_DP", "yes"}, {"apply_direction_angle", "90"},
			{"apply_direction_angle", "-45.5"}, {"highway", "residential"}, {"NAME", "Mixed Case"},
	};

	private static QuadRect world() {
		return new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	private static KQuadRect kworld() {
		return new KQuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	private static net.osmand.router.RoutingConfiguration javaConfig(QuadTree<Node> tree) {
		net.osmand.router.RoutingConfiguration.Builder builder = net.osmand.router.RoutingConfiguration.getDefault();
		builder.setDirectionPoints(tree);
		return builder.build("car", new net.osmand.router.RoutingConfiguration.RoutingMemoryLimits(30, 256));
	}

	private static RoutingConfiguration copyConfig(KQuadTree<DirectionPoint> tree) {
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		builder.setDirectionPoints(tree);
		return builder.build("car", new RoutingConfiguration.RoutingMemoryLimits(30, 256));
	}

	private static List<net.osmand.router.RoutingConfiguration.DirectionPoint> javaPoints(net.osmand.router.RoutingConfiguration c) {
		List<net.osmand.router.RoutingConfiguration.DirectionPoint> points = c.getDirectionPoints().queryInBox(world(), new ArrayList<>());
		points.sort(Comparator.comparingDouble(Node::getLatitude).thenComparingDouble(Node::getLongitude));
		return points;
	}

	private static List<DirectionPoint> copyPoints(RoutingConfiguration c) {
		List<DirectionPoint> points = c.getDirectionPoints().queryInBox(kworld(), new ArrayList<>());
		points.sort(Comparator.comparingDouble(DirectionPoint::getLatitude).thenComparingDouble(DirectionPoint::getLongitude));
		return points;
	}

	@Test
	public void testNoPointsFlattenToNothingOnBothSides() {
		assertEquals(0, net.osmand.router.RoutingConfiguration.getDefault()
				.build("car", new net.osmand.router.RoutingConfiguration.RoutingMemoryLimits(30, 256)).getNativeDirectionPoints().length);
		assertEquals(0, RoutingConfiguration.getDefault()
				.build("car", new RoutingConfiguration.RoutingMemoryLimits(30, 256)).getNativeDirectionPoints().length);
	}

	@Test
	public void testPointsComeOutTheSame() {
		QuadTree<Node> jtree = new QuadTree<>(world(), 15, 0.5f);
		KQuadTree<DirectionPoint> ktree = new KQuadTree<>(kworld(), 15, 0.5f);
		for (int i = 0; i < PLACES.length; i++) {
			Node node = new Node(PLACES[i][0], PLACES[i][1], i + 1);
			DirectionPoint point = new DirectionPoint(PLACES[i][0], PLACES[i][1]);
			for (int t = 0; t <= i && t < TAGS.length; t++) {
				node.putTag(TAGS[t][0], TAGS[t][1]);
				point.putTag(TAGS[t][0], TAGS[t][1]);
			}
			int x = MapUtils.get31TileNumberX(PLACES[i][1]);
			int y = MapUtils.get31TileNumberY(PLACES[i][0]);
			jtree.insert(node, new QuadRect(x, y, x, y));
			ktree.insert(point, new KQuadRect(x, y, x, y));
		}
		net.osmand.router.RoutingConfiguration j = javaConfig(jtree);
		RoutingConfiguration k = copyConfig(ktree);

		List<net.osmand.router.RoutingConfiguration.DirectionPoint> jp = javaPoints(j);
		List<DirectionPoint> kp = copyPoints(k);
		assertEquals(PLACES.length, jp.size());
		assertEquals(jp.size(), kp.size());
		for (int i = 0; i < jp.size(); i++) {
			String m = "point " + i;
			assertEquals(m, jp.get(i).getLatitude(), kp.get(i).getLatitude(), 0d);
			assertEquals(m, jp.get(i).getLongitude(), kp.get(i).getLongitude(), 0d);
			assertEquals(m, jp.get(i).getTags(), kp.get(i).getTags());
			assertEquals(m, jp.get(i).getAngle(), kp.get(i).getAngle(), 0d);
			assertEquals(m, jp.get(i).distance, kp.get(i).distance, 0d);
			assertEquals(m, jp.get(i).connectedx, kp.get(i).connectedx);
			assertEquals(m, jp.get(i).types.size(), kp.get(i).types.size());
		}

		net.osmand.NativeLibrary.NativeDirectionPoint[] jn = j.getNativeDirectionPoints();
		NativeDirectionPoint[] kn = k.getNativeDirectionPoints();
		assertEquals(jn.length, kn.length);
		for (int i = 0; i < jn.length; i++) {
			String m = "native point " + i;
			assertEquals(m, jn[i].x31, kn[i].x31);
			assertEquals(m, jn[i].y31, kn[i].y31);
			assertEquals(m, jn[i].tags.length, kn[i].tags.length);
			for (int t = 0; t < jn[i].tags.length; t++) {
				assertEquals(m + " tag " + t, jn[i].tags[t][0], kn[i].tags[t][0]);
				assertEquals(m + " tag " + t, jn[i].tags[t][1], kn[i].tags[t][1]);
			}
		}

		// each build hands out fresh points on both sides, so what one route writes the next does not see
		net.osmand.router.RoutingConfiguration.DirectionPoint jFirst = javaPoints(javaConfig(jtree)).get(0);
		net.osmand.router.RoutingConfiguration.DirectionPoint jSecond = javaPoints(javaConfig(jtree)).get(0);
		DirectionPoint kFirst = copyPoints(copyConfig(ktree)).get(0);
		DirectionPoint kSecond = copyPoints(copyConfig(ktree)).get(0);
		assertNotSame(jFirst, jSecond);
		assertNotSame(kFirst, kSecond);
		jFirst.distance = 1;
		kFirst.distance = 1;
		jFirst.types.add(3);
		kFirst.types.add(3);
		assertEquals(Double.MAX_VALUE, jSecond.distance, 0d);
		assertEquals(Double.MAX_VALUE, kSecond.distance, 0d);
		assertEquals(0, jSecond.types.size());
		assertEquals(0, kSecond.types.size());
	}
}
