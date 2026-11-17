package net.osmand.render;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;

import org.junit.Before;
import org.junit.Test;

/**
 * A switched-off network class (Configure map → Cycle routes → Cycling networks) must hide the route line,
 * also when the route has its own track_color (route marker) that the style could fall through to.
 */
public class RouteNetworkClassTest {

	private static final String ICN_CLASS = ".route.bicycle.road_network.icn";
	private static final String LCN_CLASS = ".route.bicycle.road_network.lcn";
	private static final int ZOOM = 14;

	private RenderingRulesStorage storage;
	private BinaryMapDataObject icnRoute;

	@Before
	public void setUp() {
		storage = RenderingRulesStorage.initWithStylesFromResources("default.render.xml", "routes.addon.render.xml");
		MapIndex mapIndex = new MapIndex();
		mapIndex.initMapEncodingRule(0, 1, "route_type", "cycling");
		mapIndex.initMapEncodingRule(0, 2, "route_activity_type", "road_cycling");
		mapIndex.initMapEncodingRule(0, 3, "network", "icn");
		mapIndex.initMapEncodingRule(0, 4, "track_color", "blue");
		icnRoute = new BinaryMapDataObject(1, new int[] {0, 0, 100, 100}, null, 2, false,
				new int[] {1}, new int[] {2, 3, 4}, 0, 0);
		icnRoute.setMapIndex(mapIndex);
	}

	@Test
	public void icnRouteIsDrawnWithAllClassesOn() {
		assertTrue(isLineDrawn(createRequest()));
	}

	@Test
	public void icnRouteIsHiddenWhenIcnClassIsOff() {
		RenderingRuleSearchRequest request = createRequest();
		request.setClassProperty(ICN_CLASS, "false");
		assertFalse(isLineDrawn(request));
	}

	@Test
	public void icnRouteIsDrawnWhenOtherClassIsOff() {
		RenderingRuleSearchRequest request = createRequest();
		request.setClassProperty(LCN_CLASS, "false");
		assertTrue(isLineDrawn(request));
	}

	private RenderingRuleSearchRequest createRequest() {
		RenderingRuleSearchRequest request = new RenderingRuleSearchRequest(storage);
		request.setBooleanFilter(storage.PROPS.get("showCycleRoutes"), true);
		request.saveState();
		return request;
	}

	// the renderers' drawPolyline condition: a line rule gives the line a width and a color
	private boolean isLineDrawn(RenderingRuleSearchRequest request) {
		request.setInitialTagValueZoom("route_type", "cycling", ZOOM, icnRoute);
		request.setIntFilter(request.ALL.R_LAYER, icnRoute.getSimpleLayer());
		return request.search(RenderingRulesStorage.LINE_RULES)
				&& request.isSpecified(request.ALL.R_STROKE_WIDTH) && request.isSpecified(request.ALL.R_COLOR);
	}
}
