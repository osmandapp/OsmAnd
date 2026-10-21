package net.osmand.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.core.util.Pair;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.plugins.rastermaps.MapOverlayAction;
import net.osmand.plus.plugins.rastermaps.MapSourceAction;
import net.osmand.plus.plugins.rastermaps.MapUnderlayAction;
import net.osmand.plus.quickaction.SwitchableAction;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Change map source / overlay / underlay" quick actions keep their items as a JSON list of pairs.
 * The list used to be read with Gson reflection over {@link Pair}, so after R8 renamed the fields of
 * Pair the items saved by earlier builds were read as pairs of nulls and the quick actions widget
 * crashed (#26429).
 */
@RunWith(AndroidJUnit4.class)
public class MapSourceActionsParamsTest {

	private static final String SOURCE_KEY = "source";
	private static final String OVERLAYS_KEY = "overlays";
	private static final String UNDERLAYS_KEY = "underlays";

	@Test
	public void readsItemsSavedWithFieldNamesAsKeys() {
		String json = "[{\"first\":\"OsmAnd (online tiles)\",\"second\":\"OsmAnd (online tiles)\"},"
				+ "{\"first\":\"LAYER_OSM_VECTOR\",\"second\":\"Offline vector maps\"}]";

		assertItems(load(new MapSourceAction(), SOURCE_KEY, json));
		assertItems(load(new MapOverlayAction(), OVERLAYS_KEY, json));
		assertItems(load(new MapUnderlayAction(), UNDERLAYS_KEY, json));
	}

	@Test
	public void readsItemsSavedWithObfuscatedKeys() {
		String json = "[{\"a\":\"OsmAnd (online tiles)\",\"b\":\"OsmAnd (online tiles)\"},"
				+ "{\"a\":\"LAYER_OSM_VECTOR\",\"b\":\"Offline vector maps\"}]";

		assertItems(load(new MapSourceAction(), SOURCE_KEY, json));
		assertItems(load(new MapOverlayAction(), OVERLAYS_KEY, json));
		assertItems(load(new MapUnderlayAction(), UNDERLAYS_KEY, json));
	}

	@Test
	public void skipsItemsWithoutId() {
		String json = "[{},{\"second\":\"Name only\"},"
				+ "{\"first\":\"LAYER_OSM_VECTOR\",\"second\":\"Offline vector maps\"}]";

		List<Pair<String, String>> items = load(new MapSourceAction(), SOURCE_KEY, json);

		assertEquals(1, items.size());
		assertEquals("LAYER_OSM_VECTOR", items.get(0).first);
		assertEquals("Offline vector maps", items.get(0).second);
	}

	@Test
	public void returnsEmptyListForMissingOrBrokenJson() {
		assertTrue(load(new MapSourceAction(), SOURCE_KEY, null).isEmpty());
		assertTrue(load(new MapSourceAction(), SOURCE_KEY, "").isEmpty());
		assertTrue(load(new MapSourceAction(), SOURCE_KEY, "null").isEmpty());
		assertTrue(load(new MapSourceAction(), SOURCE_KEY, "[{\"first\":").isEmpty());
	}

	private static List<Pair<String, String>> load(SwitchableAction<Pair<String, String>> action,
			String listKey, String json) {
		Map<String, String> params = new HashMap<>();
		if (json != null) {
			params.put(listKey, json);
		}
		action.setParams(params);
		return action.loadListFromParams();
	}

	private static void assertItems(List<Pair<String, String>> items) {
		assertEquals(2, items.size());
		assertEquals("OsmAnd (online tiles)", items.get(0).first);
		assertEquals("OsmAnd (online tiles)", items.get(0).second);
		assertEquals("LAYER_OSM_VECTOR", items.get(1).first);
		assertEquals("Offline vector maps", items.get(1).second);
	}
}
