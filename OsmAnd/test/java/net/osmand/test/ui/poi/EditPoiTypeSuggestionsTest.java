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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.database.DataSetObserver;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
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

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EditPoiTypeSuggestionsTest extends AndroidTest {

	// French name of shop=bicycle: the part of poi_bicycle in values-fr/phrases.xml before the first ';'
	private static final String BICYCLE_SHOP = "Boutique de vélos";

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

		// A POI type caches its translation on first use, so re-init the types with the French translator
		// before the map starts using them: types translated in the system language during the app start,
		// or while the new types have no translator yet, would keep those names in the suggestions
		Locale.setDefault(Locale.FRANCE);
		app.reInitPoiTypes();
		app.getPoiTypes().setPoiTranslator(new MapPoiTypesTranslator(app, new Locale("fr")));
		PoiType bicycleShop = app.getPoiTypes().getPoiTypeByKey("bicycle");
		assertNotNull(bicycleShop);
		assertEquals("French translation of shop=bicycle changed, update the typed queries",
				BICYCLE_SHOP, bicycleShop.getTranslation());

		activityRule.launchActivity(null);
		skipAppStartDialogs(app);

		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		assertNotNull(osmEditingPlugin);

		mapActivity = activityRule.getActivity();
		assertNotNull(mapActivity);
		app.runInUIThread(() -> PluginsHelper.enablePlugin(mapActivity, app, osmEditingPlugin, true));

		LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		EditPoiDialogFragment.showAddPoiInstance(mapActivity, latLon.getLatitude(), latLon.getLongitude());

		// The queries are set with replaceText, not typed: typed keys go through the IME and get lost
		// (Gboard on the API 36 emulator takes an injected tap on the field for a stylus and swallows
		// them, and on devices the first keys after an IME restart can be dropped). The field gets focus
		// without a tap, as the dropdown is shown only for a focused field, and the keyboard is hidden.
		// The text is not cleared between queries, so the dropdown stays open: a tap on a dropdown that
		// has just been shown can arrive before its window accepts input and be lost.
		onDialogView(withId(R.id.poiTypeEditText)).perform(requestFocus(), ViewActions.closeSoftKeyboard());
		//check: boutique de v
		setQueryAndWaitForSuggestions("boutique de v");
		waitForAnyView(1000, 10, RootMatchers.isPlatformPopup(), allOf(withText(BICYCLE_SHOP), isDisplayed()));
		onView(withText(BICYCLE_SHOP))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));
		//check: boutique de ve
		setQueryAndWaitForSuggestions("boutique de ve");
		waitForAnyView(1000, 10, RootMatchers.isPlatformPopup(), allOf(withText(BICYCLE_SHOP), isDisplayed()));
		onView(withText(BICYCLE_SHOP))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));

		//check: boutique de vel
		setQueryAndWaitForSuggestions("boutique de vel");
		waitForAnyView(1000, 10, RootMatchers.isPlatformPopup(), allOf(withText(BICYCLE_SHOP), isDisplayed()));
		onView(withText(BICYCLE_SHOP))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));
		//select item
		onView(withText(BICYCLE_SHOP))
				.inRoot(RootMatchers.isPlatformPopup())
				.perform(ViewActions.click());
		onDialogView(withId(R.id.poiTypeEditText)).check(hasTextEventually(BICYCLE_SHOP));

		onDialogView(isRoot()).perform(ViewActions.closeSoftKeyboard());

		onDialogView(withId(R.id.saveButton)).perform(ViewActions.click());
//
//		waitForAnyView(2000, 50, withId(R.id.context_menu_layout));
//		assertTrue(isContextMenuOpened());
	}

	// Espresso does not wait for the filter thread of the dropdown adapter: without this, the checks
	// could see the suggestions of the previous query, and a tap that comes right before the new
	// results are published is dropped by the list
	private static void setQueryAndWaitForSuggestions(String query) throws InterruptedException {
		AutoCompleteTextView[] field = new AutoCompleteTextView[1];
		onDialogView(withId(R.id.poiTypeEditText)).check((view, noViewFoundException) -> {
			if (noViewFoundException != null) {
				throw noViewFoundException;
			}
			field[0] = (AutoCompleteTextView) view;
		});
		ListAdapter adapter = field[0].getAdapter();
		CountDownLatch published = new CountDownLatch(1);
		DataSetObserver observer = new DataSetObserver() {
			@Override
			public void onChanged() {
				published.countDown();
			}
		};
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> adapter.registerDataSetObserver(observer));
		try {
			onDialogView(withId(R.id.poiTypeEditText)).perform(ViewActions.replaceText(query));
			assertTrue("No suggestions published for '" + query + "'", published.await(5, TimeUnit.SECONDS));
		} finally {
			InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> adapter.unregisterDataSetObserver(observer));
		}
	}

	private static ViewAction requestFocus() {
		return new ViewAction() {
			@Override
			public Matcher<View> getConstraints() {
				return isDisplayed();
			}

			@Override
			public String getDescription() {
				return "request focus";
			}

			@Override
			public void perform(UiController uiController, View view) {
				view.requestFocus();
				uiController.loopMainThreadUntilIdle();
			}
		};
	}
}