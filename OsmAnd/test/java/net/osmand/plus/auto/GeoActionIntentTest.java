package net.osmand.plus.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.helpers.GeoActionHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GeoActionIntentTest {

	@Test
	public void testIsGeoActionUri() {
		assertTrue(GeoActionHelper.isGeoActionUri(Uri.parse("geo.action:?act=exit_navigation")));
		assertTrue(GeoActionHelper.isGeoActionUri(Uri.parse("geo.action.offline:?act=mute")));
		assertFalse(GeoActionHelper.isGeoActionUri(Uri.parse("geo:52.52,13.40")));
		assertFalse(GeoActionHelper.isGeoActionUri(Uri.parse("https://osmand.net")));
		assertFalse(GeoActionHelper.isGeoActionUri(null));
	}

	@Test
	public void testParseExitNavigation() {
		// Official Google Assistant query parameter format
		assertEquals("exit_navigation", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=exit_navigation")));
		assertEquals("exit_navigation", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=exit_navigation")));
		assertEquals("exit_navigation", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=exit_navigation&foo=bar")));
		assertEquals("exit_navigation", GeoActionHelper.parseAction(Uri.parse("geo.action:?foo=bar&act=exit_navigation")));

		// Hierarchical URI format
		assertEquals("exit_navigation", GeoActionHelper.parseAction(Uri.parse("geo.action:///?act=exit_navigation")));
	}

	@Test
	public void testParseMuteAndUnmute() {
		// Official Google Assistant query parameter format
		assertEquals("mute", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=mute")));
		assertEquals("mute", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=mute")));
		assertEquals("mute", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=mute&source=assistant")));
		assertEquals("unmute", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=unmute")));
		assertEquals("unmute", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=unmute")));
		assertEquals("unmute", GeoActionHelper.parseAction(Uri.parse("geo.action:?source=assistant&act=unmute")));
	}

	@Test
	public void testParseRoutePreferences() {
		assertEquals("avoid_tolls", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=avoid_tolls")));
		assertEquals("allow_tolls", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=allow_tolls")));
		assertEquals("avoid_highways", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=avoid_highways")));
		assertEquals("allow_highways", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=allow_highways")));
		assertEquals("avoid_ferries", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=avoid_ferries")));
		assertEquals("allow_ferries", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=allow_ferries")));
	}

	@Test
	public void testParseMapActions() {
		assertEquals("show_alternates", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=show_alternates")));
		assertEquals("route_overview", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=route_overview")));
		assertEquals("show_directions_list", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=show_directions_list")));
		assertEquals("follow_mode", GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=follow_mode")));
		assertEquals("go_back", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=go_back")));
	}

	@Test
	public void testParseStatusQueries() {
		assertEquals(GeoActionHelper.ACTION_ETA, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=eta")));
		assertEquals(GeoActionHelper.ACTION_TIME_TO_DESTINATION, GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=time_to_destination")));
		assertEquals(GeoActionHelper.ACTION_DISTANCE_TO_DESTINATION, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=distance_to_destination")));
		assertEquals(GeoActionHelper.ACTION_TIME_TO_NEXT_TURN, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=time_to_next_turn")));
		assertEquals(GeoActionHelper.ACTION_DISTANCE_TO_NEXT_TURN, GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=distance_to_next_turn")));
		assertEquals(GeoActionHelper.ACTION_QUERY_NEXT_TURN, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=query_next_turn")));
		assertEquals(GeoActionHelper.ACTION_QUERY_DESTINATION, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=query_destination")));
		assertEquals(GeoActionHelper.ACTION_QUERY_CURRENT_ROAD, GeoActionHelper.parseAction(Uri.parse("geo.action.offline:?act=query_current_road")));
	}

	@Test
	public void testParseUnsupportedActions() {
		assertEquals(GeoActionHelper.ACTION_REPORT_CRASH, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=report_crash")));
		assertEquals(GeoActionHelper.ACTION_REPORT_HAZARD, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=report_hazard")));
		assertEquals(GeoActionHelper.ACTION_REPORT_POLICE, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=report_police")));
		assertEquals(GeoActionHelper.ACTION_REPORT_TRAFFIC, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=report_traffic")));
		assertEquals(GeoActionHelper.ACTION_REPORT_ROAD_CLOSURE, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=report_road_closure")));
		assertEquals(GeoActionHelper.ACTION_SHOW_TRAFFIC, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=show_traffic")));
		assertEquals(GeoActionHelper.ACTION_HIDE_TRAFFIC, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=hide_traffic")));
		assertEquals(GeoActionHelper.ACTION_SHOW_SATELLITE, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=show_satellite")));
		assertEquals(GeoActionHelper.ACTION_HIDE_SATELLITE, GeoActionHelper.parseAction(Uri.parse("geo.action:?act=hide_satellite")));
	}

	@Test
	public void testParseUnsupportedAndEdgeCases() {
		assertEquals("unknown_action", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=unknown_action")));
		assertEquals("", GeoActionHelper.parseAction(Uri.parse("geo:52.52,13.40")));
		assertEquals("", GeoActionHelper.parseAction(Uri.parse("geo.action:")));
		assertEquals("", GeoActionHelper.parseAction(null));
	}
}
