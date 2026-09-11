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
	public void testParseUnsupportedAndEdgeCases() {
		assertEquals("unknown_action", GeoActionHelper.parseAction(Uri.parse("geo.action:?act=unknown_action")));
		assertEquals("", GeoActionHelper.parseAction(Uri.parse("geo:52.52,13.40")));
		assertEquals("", GeoActionHelper.parseAction(Uri.parse("geo.action:")));
		assertEquals("", GeoActionHelper.parseAction(null));
	}
}
