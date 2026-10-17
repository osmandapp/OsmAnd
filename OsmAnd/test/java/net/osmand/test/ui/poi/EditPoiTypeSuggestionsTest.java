package net.osmand.test.ui.poi;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.EspressoUtils.onDialogView;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.SystemDialogInteractions.hasTextEventually;
import static net.osmand.test.common.SystemDialogInteractions.waitForAnyView;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import net.osmand.data.LatLon;
import net.osmand.osm.PoiType;
import net.osmand.plus.MapPoiTypesTranslator;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.AppSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class EditPoiTypeSuggestionsTest extends AndroidTest {

	@Rule
	public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	@Rule
	public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(ACCESS_FINE_LOCATION);

	private MapActivity mapActivity;

	@Before
	public void setup() {
		super.setup();
		try {
			copyAssetToFile(app, "World_basemap_mini.obf", new File(app.getAppPath(null), "World_basemap_mini.obf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testPoiTypeSuggestions() throws Throwable {
		AppSettings.setLocale(app, Locale.FRANCE);

		activityRule.launchActivity(null);
		if (app.getPoiTypes().isInit()) {
			app.reInitPoiTypes();
		}
		while (!app.getPoiTypes().isInit()) {
			Thread.sleep(50);
		}
		app.getPoiTypes().setPoiTranslator(new MapPoiTypesTranslator(app, new Locale("fr")));
		skipAppStartDialogs(app);

		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		assertNotNull(osmEditingPlugin);

		mapActivity = activityRule.getActivity();
		assertNotNull(mapActivity);
		app.runInUIThread(() -> PluginsHelper.enablePlugin(mapActivity, app, osmEditingPlugin, true));

		LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		EditPoiDialogFragment.showAddPoiInstance(mapActivity, latLon.getLatitude(), latLon.getLongitude());

		// The French name comes from Weblate (was "Magasin de vélos", now "Boutique de vélos"),
		// so take it from the app instead of hardcoding it.
		PoiType bicycleShop = app.getPoiTypes().getPoiTypeByKey("bicycle");
		assertNotNull(bicycleShop);
		String name = bicycleShop.getTranslation();
		int accent = name.toLowerCase().indexOf("vé");
		assertTrue("French bicycle shop name has no \"vé\": " + name, accent > 0);
		// "<...> v", "<...> ve", "<...> vel": the typed text has no accent, the suggestion has one
		String prefix = name.substring(0, accent).toLowerCase();
		String[] queries = {prefix + "v", prefix + "ve", prefix + "vel"};
		for (int i = 0; i < queries.length; i++) {
			if (i > 0) {
				onDialogView(withId(R.id.poiTypeEditText)).perform(ViewActions.replaceText(""));
			}
			onDialogView(withId(R.id.poiTypeEditText)).perform(ViewActions.typeText(queries[i]));
			waitForAnyView(1000, 10, RootMatchers.isPlatformPopup(), allOf(withText(name), isDisplayed()));
			onView(withText(name))
					.inRoot(RootMatchers.isPlatformPopup())
					.check(ViewAssertions.matches(isDisplayed()));
		}
		//select item
		onView(withText(name))
				.inRoot(RootMatchers.isPlatformPopup())
				.perform(ViewActions.click());
		onDialogView(withId(R.id.poiTypeEditText)).check(hasTextEventually(name));

		onDialogView(isRoot()).perform(ViewActions.closeSoftKeyboard());

		onDialogView(withId(R.id.saveButton)).perform(ViewActions.click());
//
//		waitForAnyView(2000, 50, withId(R.id.context_menu_layout));
//		assertTrue(isContextMenuOpened());
	}
}