package net.osmand.plus.mapcontextmenu.editors.icon;

import static net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController.LAST_USED_KEY;
import static net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController.TRAVEL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Categories of the Favorite / waypoint icon picker: every poi_categories.json icon that has a drawable
 * has to be selectable in a permanent category, not only in "Last used" (#26509).
 */
@RunWith(AndroidJUnit4.class)
public class EditorIconControllerTest {

	private static final long POI_TYPES_INIT_TIMEOUT_MS = 30_000;

	private OsmandApplication app;

	@Before
	public void setUp() throws InterruptedException {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
		long deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS;
		while (!app.getPoiTypes().isInit() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertTrue("POI types are not initialized", app.getPoiTypes().isInit());
	}

	@Test
	public void assetsIconsAreSelectableOutsideLastUsed() throws IOException, JSONException {
		Set<String> selectableIcons = new HashSet<>();
		for (IconsCategory category : createController().getCategories()) {
			if (!LAST_USED_KEY.equals(category.getKey())) {
				selectableIcons.addAll(category.getIconKeys());
			}
		}
		List<String> missingIcons = new ArrayList<>();
		JSONObject groups = readPoiCategories().getJSONObject("categories");
		for (Iterator<String> it = groups.keys(); it.hasNext(); ) {
			String group = it.next();
			JSONArray icons = groups.getJSONObject(group).getJSONArray("icons");
			for (int i = 0; i < icons.length(); i++) {
				String icon = icons.getString(i);
				if (RenderingIcons.containsBigIcon(icon) && !selectableIcons.contains(icon)) {
					missingIcons.add(group + "/" + icon);
				}
			}
		}
		assertEquals("Icons that cannot be selected", "", String.join(", ", missingIcons));
	}

	@Test
	public void assetsIconsArePlacedInMatchingCategories() {
		EditorIconController controller = createController();
		assertCategoryContains(controller, "transportation", "special_wagon");
		assertCategoryContains(controller, "transportation", "special_sail_boat");
		assertCategoryContains(controller, "seamark", "special_sail_boat");
		assertCategoryContains(controller, "man_made", "generator_source_wind");
		assertCategoryContains(controller, "sport", "special_skiing");
		assertCategoryContains(controller, "amenity", "vending_drinks");
	}

	@Test
	public void poiTravelCategoryJoinsAssetsTravelCategory() throws IOException, JSONException {
		List<IconsCategory> travelCategories = new ArrayList<>();
		for (IconsCategory category : createController().getCategories()) {
			if (TRAVEL_KEY.equals(category.getKey())) {
				travelCategories.add(category);
			}
		}
		assertEquals("Categories with key " + TRAVEL_KEY, 1, travelCategories.size());

		List<String> travelIcons = travelCategories.get(0).getIconKeys();
		JSONArray assetsIcons = readPoiCategories().getJSONObject("categories").getJSONObject(TRAVEL_KEY).getJSONArray("icons");
		for (int i = 0; i < assetsIcons.length(); i++) {
			assertEquals("Assets travel icon at " + i, assetsIcons.getString(i), travelIcons.get(i));
		}
		for (PoiType poiType : app.getPoiTypes().getPoiCategoryByName(TRAVEL_KEY).getPoiTypes()) {
			EditorIconUtils.retrieveIconKey(poiType, iconKey ->
					assertTrue(iconKey + " is not in " + TRAVEL_KEY, travelIcons.contains(iconKey)));
		}
		assertEquals("Icons listed twice", new HashSet<>(travelIcons).size(), travelIcons.size());
	}

	@NonNull
	private EditorIconController createController() {
		EditorIconController controller = new EditorIconController(app);
		controller.initIconCategories();
		return controller;
	}

	private void assertCategoryContains(@NonNull EditorIconController controller,
	                                    @NonNull String categoryKey, @NonNull String iconKey) {
		IconsCategory category = findCategory(controller, categoryKey);
		assertNotNull("No category " + categoryKey, category);
		assertTrue(iconKey + " is not in " + categoryKey, category.containsIcon(iconKey));
	}

	@Nullable
	private IconsCategory findCategory(@NonNull EditorIconController controller, @NonNull String key) {
		for (IconsCategory category : controller.getCategories()) {
			if (key.equals(category.getKey())) {
				return category;
			}
		}
		return null;
	}

	@NonNull
	private JSONObject readPoiCategories() throws IOException, JSONException {
		try (InputStream is = app.getAssets().open("poi_categories.json")) {
			return new JSONObject(Algorithms.readFromInputStream(is).toString());
		}
	}
}
