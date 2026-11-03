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

import android.database.DataSetObserver;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import net.osmand.data.LatLon;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EditPoiTypeSuggestionsTest extends AndroidTest {

	// French name of shop=bicycle: the part of poi_bicycle in values-fr/phrases.xml before the first ';'
	private static final String BICYCLE_SHOP = "Magasin de vélos";

	@Rule
	public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	@Rule
	public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(ACCESS_FINE_LOCATION);

	private MapActivity mapActivity;
	private AutoCompleteTextView poiTypeEditText;

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

		// Initialize French POI types before MapActivity can cache translations in another language.
		Locale.setDefault(Locale.FRANCE);
		app.reInitPoiTypes();
		app.getPoiTypes().setPoiTranslator(new MapPoiTypesTranslator(app, new Locale("fr")));
		activityRule.launchActivity(null);
		skipAppStartDialogs(app);

		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		assertNotNull(osmEditingPlugin);

		mapActivity = activityRule.getActivity();
		assertNotNull(mapActivity);
		app.runInUIThread(() -> PluginsHelper.enablePlugin(mapActivity, app, osmEditingPlugin, true));

		LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		EditPoiDialogFragment.showAddPoiInstance(mapActivity, latLon.getLatitude(), latLon.getLongitude());

		onDialogView(withId(R.id.poiTypeEditText)).check((view, error) -> {
			if (error != null) {
				throw error;
			}
			poiTypeEditText = (AutoCompleteTextView) view;
		});
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> poiTypeEditText.requestFocus());
		onDialogView(isRoot()).perform(ViewActions.closeSoftKeyboard());

		for (String query : new String[] {"magasin de v", "magasin de ve", "magasin de vel"}) {
			setQueryAndWaitForSuggestions(query);
			waitForAnyView(1000, 10, RootMatchers.isPlatformPopup(), allOf(withText(BICYCLE_SHOP), isDisplayed()));
			onView(withText(BICYCLE_SHOP))
					.inRoot(RootMatchers.isPlatformPopup())
					.check(ViewAssertions.matches(isDisplayed()));
		}
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

	// Wait for the automatic filter to publish this query before checking or clicking its results.
	private void setQueryAndWaitForSuggestions(String query) throws InterruptedException {
		ListAdapter adapter = poiTypeEditText.getAdapter();
		CountDownLatch published = new CountDownLatch(1);
		DataSetObserver observer = new DataSetObserver() {
			@Override
			public void onChanged() {
				published.countDown();
			}
		};
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> adapter.registerDataSetObserver(observer));
		try {
			// Set text on the UI thread without IME input; keep the popup open between queries.
			InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> poiTypeEditText.setText(query));
			assertTrue("No suggestions published for '" + query + "'", published.await(5, TimeUnit.SECONDS));
		} finally {
			InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> adapter.unregisterDataSetObserver(observer));
		}
	}
}
