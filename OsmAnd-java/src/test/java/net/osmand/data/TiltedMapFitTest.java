package net.osmand.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.util.MapUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks TiltedMapFit against a pinhole camera like the one of OsmAnd v2: the camera is set to the fitted target
 * and zoom, and the route is measured on its screen.
 */
public class TiltedMapFitTest {

	private static final int WIDTH = 1008;
	private static final int HEIGHT = 2244;
	private static final double TARGET_ROW = 0.85 * HEIGHT;
	private static final double FIELD_OF_VIEW = 16.5;
	// Map part above the half open route menu, without 10% margins
	private static final double BOX_LEFT = 0.1 * WIDTH;
	private static final double BOX_RIGHT = 0.9 * WIDTH;
	private static final double BOX_TOP = 149 + 0.1 * (1110 - 149);
	private static final double BOX_BOTTOM = 1110 - 0.1 * (1110 - 149);
	// 31 units per pixel near zoom 15
	private static final double UNITS_PER_PIXEL = 256;
	private static final double TOLERANCE = 0.01;

	@Test
	public void fitsRouteIntoBoxOnTiltedMap() {
		Camera camera = new Camera(45, -30, UNITS_PER_PIXEL, false);
		TiltedMapFit fit = fit(camera, kyivRoute());
		assertEquals(BOX_TOP, fit.getBoxTop(), 0.001);
		double[] bbox = fittedScreenBBox(camera, fit, kyivRoute());
		assertFitted(bbox, fit.getBoxTop());
	}

	@Test
	public void lowersBoxTopOnLowTilt() {
		Camera camera = new Camera(20, -30, UNITS_PER_PIXEL, false);
		TiltedMapFit fit = fit(camera, kyivRoute());
		double top = fit.getBoxTop();
		assertTrue("box top " + top + " is lowered", top > BOX_TOP);
		assertTrue("box top " + top + " is below the horizon " + camera.getHorizon(), top > camera.getHorizon());
		double squeeze = camera.getVerticalGroundPerPixel(top) / camera.getVerticalGroundPerPixel(BOX_BOTTOM);
		assertEquals(5, squeeze, 5 * TOLERANCE);
		assertFitted(fittedScreenBBox(camera, fit, kyivRoute()), top);
	}

	@Test
	public void keepsBoxTopWhenAlmostTopDown() {
		Camera camera = new Camera(89, 0, UNITS_PER_PIXEL, false);
		TiltedMapFit fit = fit(camera, kyivRoute());
		assertEquals(BOX_TOP, fit.getBoxTop(), 0.001);
		assertFitted(fittedScreenBBox(camera, fit, kyivRoute()), BOX_TOP);
	}

	@Test
	public void zoomsOutWhenRouteIsBehindCamera() {
		// Zoomed in far to the north of the route, looking north: the route is behind the camera
		Camera camera = new Camera(30, 0, 2, false);
		camera.targetY -= 2_000_000;
		List<LatLon> route = kyivRoute();
		TiltedMapFit fit = fit(camera, route);
		assertTrue(fit.getScale() > 10);
		assertFitted(fittedScreenBBox(camera, fit, route), fit.getBoxTop());
	}

	@Test
	public void fitsRaisedRouteOnRelief() {
		Camera camera = new Camera(25, -30, UNITS_PER_PIXEL, true);
		// The route is on the screen now, so its heights can be measured
		camera.targetX = MapUtils.get31TileNumberX(30.505);
		camera.targetY = MapUtils.get31TileNumberY(50.430);
		TiltedMapFit fit = fit(camera, kyivRoute());
		double[] raised = fittedScreenBBox(camera, fit, kyivRoute());
		assertFitted(raised, fit.getBoxTop());

		Camera flat = new Camera(25, -30, UNITS_PER_PIXEL, false);
		double[] flatBBox = fittedScreenBBox(flat, fit, kyivRoute());
		assertTrue("relief raises the route", flatBBox[1] - raised[1] > 50);
	}

	@Test
	public void centersAtGivenScale() {
		Camera camera = new Camera(45, -30, UNITS_PER_PIXEL, false);
		TiltedMapFit fit = fit(camera, kyivRoute());
		double scale = fit.getScale() * 1.2;
		assertTrue(fit.center(scale));
		assertEquals(scale, fit.getScale(), 0);
		double[] bbox = fittedScreenBBox(camera, fit, kyivRoute());
		double boxHeight = BOX_BOTTOM - fit.getBoxTop();
		assertEquals((BOX_LEFT + BOX_RIGHT) / 2, (bbox[0] + bbox[2]) / 2, (BOX_RIGHT - BOX_LEFT) * TOLERANCE);
		assertEquals((fit.getBoxTop() + BOX_BOTTOM) / 2, (bbox[1] + bbox[3]) / 2, boxHeight * TOLERANCE);
	}

	private static TiltedMapFit fit(Camera camera, List<LatLon> route) {
		TiltedMapFit fit = new TiltedMapFit(camera, camera.targetX, camera.targetY,
				Math.toDegrees(camera.elevation), route,
				BOX_LEFT, BOX_TOP, BOX_RIGHT, BOX_BOTTOM);
		assertTrue("fitted", fit.fit());
		return fit;
	}

	/** Screen bbox {left, top, right, bottom} of the route with the camera moved to the fitted target and zoom */
	private static double[] fittedScreenBBox(Camera camera, TiltedMapFit fit, List<LatLon> route) {
		Camera moved = camera.copy();
		moved.targetX = fit.getTarget()[0];
		moved.targetY = fit.getTarget()[1];
		moved.unitsPerPixel = camera.unitsPerPixel * fit.getScale();
		double[] bbox = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
		for (LatLon p : route) {
			double[] pixel = moved.getElevatedPixel(MapUtils.get31TileNumberX(p.getLongitude()),
					MapUtils.get31TileNumberY(p.getLatitude()));
			bbox[0] = Math.min(bbox[0], pixel[0]);
			bbox[1] = Math.min(bbox[1], pixel[1]);
			bbox[2] = Math.max(bbox[2], pixel[0]);
			bbox[3] = Math.max(bbox[3], pixel[1]);
		}
		return bbox;
	}

	/** Inside the box, centered in it, and as wide or as high as it */
	private static void assertFitted(double[] bbox, double boxTop) {
		double boxWidth = BOX_RIGHT - BOX_LEFT;
		double boxHeight = BOX_BOTTOM - boxTop;
		assertTrue("left " + bbox[0], bbox[0] >= BOX_LEFT - boxWidth * TOLERANCE);
		assertTrue("right " + bbox[2], bbox[2] <= BOX_RIGHT + boxWidth * TOLERANCE);
		assertTrue("top " + bbox[1], bbox[1] >= boxTop - boxHeight * TOLERANCE);
		assertTrue("bottom " + bbox[3], bbox[3] <= BOX_BOTTOM + boxHeight * TOLERANCE);
		assertEquals("center x", (BOX_LEFT + BOX_RIGHT) / 2, (bbox[0] + bbox[2]) / 2, boxWidth * TOLERANCE);
		assertEquals("center y", (boxTop + BOX_BOTTOM) / 2, (bbox[1] + bbox[3]) / 2, boxHeight * TOLERANCE);
		double ratio = Math.max((bbox[2] - bbox[0]) / boxWidth, (bbox[3] - bbox[1]) / boxHeight);
		assertEquals("size", 1, ratio, TOLERANCE);
	}

	/** Route of 2.7 km in Kyiv from the issue: from the railway station area to Bulvarno-Kudriavska street */
	private static List<LatLon> kyivRoute() {
		double[][] route = {{50.4392, 30.5040}, {50.4398, 30.5005}, {50.4405, 30.4968}, {50.4413, 30.4930},
				{50.4420, 30.4895}, {50.4440, 30.4885}, {50.4460, 30.4900}, {50.4478, 30.4925},
				{50.4495, 30.4950}, {50.4510, 30.4975}, {50.4522, 30.5000}, {50.4535, 30.5020}};
		List<LatLon> result = new ArrayList<>();
		for (double[] p : route) {
			result.add(new LatLon(p[0], p[1]));
		}
		return result;
	}

	/**
	 * Pinhole camera looking at the map target under the elevation angle, the target at TARGET_ROW. With relief,
	 * the ground is raised by about 150-200 m times 1.5, in 31 units.
	 */
	private static class Camera implements TiltedMapFit.Projection {

		private final double elevation;
		private final double azimuth;
		private final boolean relief;
		private final double focus = HEIGHT / 2.0 / Math.tan(Math.toRadians(FIELD_OF_VIEW));
		double targetX = MapUtils.get31TileNumberX(30.49);
		double targetY = MapUtils.get31TileNumberY(50.43);
		double unitsPerPixel;

		Camera(double elevation, double azimuth, double unitsPerPixel, boolean relief) {
			this.elevation = Math.toRadians(elevation);
			this.azimuth = Math.toRadians(azimuth);
			this.unitsPerPixel = unitsPerPixel;
			this.relief = relief;
		}

		Camera copy() {
			Camera c = new Camera(Math.toDegrees(elevation), Math.toDegrees(azimuth), unitsPerPixel, relief);
			c.targetX = targetX;
			c.targetY = targetY;
			return c;
		}

		double getHorizon() {
			return TARGET_ROW - focus * Math.tan(elevation);
		}

		/** Ground length of a pixel down the screen column at the center */
		double getVerticalGroundPerPixel(double row) {
			double[] a = getLocation(WIDTH / 2.0, row);
			double[] b = getLocation(WIDTH / 2.0, row + 1);
			return Math.hypot(b[0] - a[0], b[1] - a[1]);
		}

		@Override
		public double[] getPixel(double x31, double y31) {
			return project(x31, y31, 0);
		}

		@Override
		public double[] getElevatedPixel(double x31, double y31) {
			return project(x31, y31, relief ? getHeight(x31, y31) : 0);
		}

		private double getHeight(double x31, double y31) {
			double lon = MapUtils.get31LongitudeX((int) x31);
			double lat = MapUtils.get31LatitudeY((int) y31);
			double meters = 175 + 25 * Math.sin(lon * 300) * Math.cos(lat * 200);
			// 31 units per meter in Kyiv
			return 1.5 * meters * 84;
		}

		private double[] project(double x31, double y31, double height) {
			// Screen up is the azimuth; 31 y grows to the south
			double east = x31 - targetX;
			double north = targetY - y31;
			double x = east * Math.cos(azimuth) - north * Math.sin(azimuth);
			double y = east * Math.sin(azimuth) + north * Math.cos(azimuth);
			double distance = unitsPerPixel * focus;
			double depth = y * Math.cos(elevation) + distance - height * Math.sin(elevation);
			if (depth == 0) {
				return null;
			}
			return new double[] {WIDTH / 2.0 + focus * x / depth,
					TARGET_ROW - focus * (y * Math.sin(elevation) + height * Math.cos(elevation)) / depth};
		}

		@Override
		public double[] getLocation(double px, double py) {
			double a = (px - WIDTH / 2.0) / focus;
			double b = (TARGET_ROW - py) / focus;
			double denominator = Math.sin(elevation) - b * Math.cos(elevation);
			if (denominator <= 0) {
				return null;
			}
			double distance = unitsPerPixel * focus;
			double y = b * distance / denominator;
			double x = a * (y * Math.cos(elevation) + distance);
			double east = x * Math.cos(azimuth) + y * Math.sin(azimuth);
			double north = -x * Math.sin(azimuth) + y * Math.cos(azimuth);
			return new double[] {targetX + east, targetY - north};
		}
	}
}
