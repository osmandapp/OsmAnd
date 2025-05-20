package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.clickButtonWithContentDescription;
import static net.osmand.test.common.OsmAndDialogInteractions.clickButtonWithText;
import static net.osmand.test.common.OsmAndDialogInteractions.clickMapButtonWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.writeText;

import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.test.common.AndroidTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.test.espresso.Espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

/**
 * Test for enabling the POI overlay layer via the main menu and Configure Map screen.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class POIClickTest extends AndroidTest {

	// Rule to launch the main activity before each test
	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	@Before
	public void setup() {
		super.setup();
		try {
			copyAssetToFile("World_basemap_mini.obf", new File(app.getAppPath(null), "World_basemap_mini.obf"));
			copyAssetToFile("Ukraine_kyiv-city_europe.obf", new File(app.getAppPath(null), "Ukraine_kyiv-city_europe.obf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void copyAssetToFile(String assetName, File destFile) throws IOException {
		try (InputStream in = testContext.getAssets().open(assetName);
		     FileOutputStream out = new FileOutputStream(destFile)) {
			byte[] buffer = new byte[4096];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
		}
	}

	private void showWikiOnMap() {
		PoiFiltersHelper helper = app.getPoiFilters();
		WikipediaPlugin plugin = PluginsHelper.getActivePlugin(WikipediaPlugin.class);
		if (plugin != null) {
			PoiUIFilter filter = plugin.getTopWikiPoiFilter();
			if (filter != null) {
				helper.loadSelectedPoiFilters();
				helper.addSelectedPoiFilter(filter);
			}
		}
	}


	@Test
	public void testClickOnMApPoint() throws Throwable {
		showWikiOnMap();
		app.getSettings().SHOW_FAVORITES.set(true);
		activityRule.launchActivity(null);
		double lattitude = 50.452880;
		double longitude = 30.514269;

		FavouritePoint favouritePoint = new FavouritePoint(lattitude, longitude, "TestFavorite", "");
		app.getFavoritesHelper().doAddFavorite("TestFavorite", "", "Test description for test favorite", "", 0xffff0000, BackgroundType.CIRCLE, FavouritePoint.DEFAULT_UI_ICON_ID, favouritePoint);

		skipAppStartDialogs(app);

		app.getOsmandMap().getMapView().setLatLon(lattitude, longitude);
		app.getOsmandMap().getMapView().refreshMap();
		app.getOsmandMap().getMapView().setIntZoom(14);

//		Thread.sleep(5000);
//		app.getOsmandMap().getMapView().setIntZoom(16);
//		Thread.sleep(5000);
//		app.getOsmandMap().getMapView().setIntZoom(15);
//		Thread.sleep(5000);
//		app.getOsmandMap().getMapView().setIntZoom(14);
//		Thread.sleep(5000);
//		app.getOsmandMap().getMapView().setIntZoom(13);
//		Thread.sleep(5000);

//		app.getOsmandMap().getMapView().setView();

		float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
		float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
		onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
		Thread.sleep(10000);
//		clickMapButtonWithId(R.id.map_menu_button);
//		clickButtonWithText(R.string.maps_and_resources);
//
//		clickButtonWithContentDescription(R.string.shared_string_search);
//		writeText(R.id.searchEditText, "Kyiv");
//
//		clickButtonWithText("Kyiv");

	}

	public static ViewAction clickInView(final float x, final float y) {
		return new GeneralClickAction(
				Tap.SINGLE,
				new CoordinatesProvider() {
					@Override
					public float[] calculateCoordinates(View view) {
						final int[] location = new int[2];
						view.getLocationOnScreen(location);
						return new float[] {location[0] + x, location[1] + y};
					}
				},
				Press.FINGER
		);
	}
}