package net.osmand.test.ui.poi;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.AppSettings.setLocale;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.OsmAndDialogInteractions.checkViewText;
import static net.osmand.test.common.OsmAndDialogInteractions.clearText;
import static net.osmand.test.common.OsmAndDialogInteractions.isContextMenuOpened;
import static net.osmand.test.common.OsmAndDialogInteractions.replaceText;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.writeText;
import static net.osmand.test.common.SystemDialogInteractions.waitForAnyView;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.test.common.AndroidTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Ignore("Is being fixed")
public class EditPoiTypeSuggestionsTest extends AndroidTest {
	MapActivity mapActivity;

	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	@Rule
	public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
			Manifest.permission.ACCESS_FINE_LOCATION
	);

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
	public void testPoiTypeSuggestions() throws InterruptedException {
		setLocale(app, Locale.FRANCE);
		activityRule.launchActivity(null);
		if(app.getPoiTypes().isInit()) {
			MapPoiTypes.reInit();
		}
		while (!app.getPoiTypes().isInit()){
			Thread.sleep(50);
		}
		skipAppStartDialogs(app);

		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		assertNotNull(osmEditingPlugin);

		mapActivity = activityRule.getActivity();
		assertNotNull(mapActivity);
		app.runInUIThread(() -> PluginsHelper.enablePlugin(mapActivity, app, osmEditingPlugin, true));

		LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		EditPoiDialogFragment editPoiDialogFragment =
				EditPoiDialogFragment.createAddPoiInstance(latLon.getLatitude(), latLon.getLongitude(),
						app);
		editPoiDialogFragment.show(mapActivity.getSupportFragmentManager(),
				EditPoiDialogFragment.TAG);

		//check: magasin de v
		writeText(R.id.poiTypeEditText, "magasin de v");
		onView(withText("Magasin de vélos"))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));
		//clear
		clearText(R.id.poiTypeEditText);
		//check: magasin de ve
		writeText(R.id.poiTypeEditText, "magasin de ve");
		onView(withText("Magasin de vélos"))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));

		//clear
		clearText(R.id.poiTypeEditText);
		//check: magasin de vé
		replaceText(R.id.poiTypeEditText, "magasin de vé");
		writeText(R.id.poiTypeEditText, "l");
		onView(withText("Magasin de vélos"))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));
		//select item
		onView(withText("Magasin de vélos"))
				.inRoot(RootMatchers.isPlatformPopup())
				.perform(ViewActions.click());
		checkViewText(R.id.poiTypeEditText, "Magasin de vélos");

		onView(withId(R.id.saveButton)).perform(ViewActions.click());
		waitForAnyView(2000, 50, withId(R.id.context_menu_layout));
		assertTrue(isContextMenuOpened());
	}
}