package net.osmand.test.ui.poi;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.AppSettings.setLocale;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.writeText;
import static org.junit.Assert.assertNotNull;

import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.test.common.AndroidTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class EditPoiTypeSuggestionsTest extends AndroidTest {
	MapActivity mapActivity;

	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

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
	public void testPoiTypeSuggestions() {
		activityRule.launchActivity(null);
		setLocale(app, "fr", "FR");
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

		writeText(R.id.poiTypeEditText, "velo");
		onView(withText("Magasin de vélos"))
				.inRoot(RootMatchers.isPlatformPopup())
				.check(ViewAssertions.matches(isDisplayed()));
	}
}