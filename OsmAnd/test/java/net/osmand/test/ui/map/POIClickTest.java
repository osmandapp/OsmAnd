package net.osmand.test.ui.map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.EspressoUtils.waitForView;
import static net.osmand.test.common.OsmAndDialogInteractions.isContextMenuOpened;
import static net.osmand.test.common.OsmAndDialogInteractions.isMultiSelectionMenuOpened;
import static net.osmand.test.common.OsmAndDialogInteractions.moveAndZoomMap;
import static net.osmand.test.common.OsmAndDialogInteractions.refreshMap;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.SystemDialogInteractions.clickInView;
import static net.osmand.test.common.SystemDialogInteractions.getViewById;
import static net.osmand.test.common.SystemDialogInteractions.waitForViewDisappeared;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.ViewGroup;
import android.widget.ListView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.OnResultCallback;
import net.osmand.PlatformUtil;
import net.osmand.data.BackgroundType;
import net.osmand.data.DataSourceType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesOnlineProvider;
import net.osmand.plus.mapcontextmenu.other.MenuObject;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.AppSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class POIClickTest extends AndroidTest {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExplorePlacesOnlineProvider.class);
	// Rule to launch the main activity before each test
	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	private double lattitude = 50.452880;
	private double longitude = 30.514269;
	private int zoom = 14;

	@Before
	public void setup() {
		super.setup();
		try {
			copyAssetToFile(app, "World_basemap_mini.obf", new File(app.getAppPath(null), "World_basemap_mini.obf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		app.getAppInitializer().addOnFinishListener(new OnResultCallback<AppInitializer>() {
			@Override
			public void onResult(AppInitializer result) {
				AppSettings.showFavorites(app, true);
				app.getSettings().WIKI_DATA_SOURCE_TYPE.set(DataSourceType.ONLINE);

				FavouritePoint favouritePoint = new FavouritePoint(lattitude, longitude, "TestFavorite", "");
				app.getFavoritesHelper().doAddFavorite("TestFavorite", "", "Test description for test favorite", "", 0xffff0000, BackgroundType.CIRCLE, FavouritePoint.DEFAULT_UI_ICON_ID, favouritePoint);
			}
		});
	}

	@Test
	public void testClickOnMApPoint() throws Throwable {
		activityRule.launchActivity(null);

		skipAppStartDialogs(app);

		moveAndZoomMap(app, lattitude, longitude, zoom);

		WikipediaPlugin plugin = PluginsHelper.getPlugin(WikipediaPlugin.class);
		plugin.toggleWikipediaPoi(true, null);

		float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
		float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
		Thread.sleep(10000);
		app.getOsmandMap().getMapView().refreshMapComplete();
		Thread.sleep(5000);

		onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
		Set<PoiUIFilter> selectedPoiFilters = app.getPoiFilters().getSelectedPoiFilters();

		app.showToastMessage("selectedPoiFilters " + selectedPoiFilters);

		waitForView(withId(R.id.multi_selection_main_view));
		assertTrue(isMultiSelectionMenuOpened());

		ViewGroup menuLayout = (ViewGroup) getViewById(R.id.multi_selection_main_view);
		ListView menuList = menuLayout.findViewById(R.id.list);
		int itemsCount = menuList.getAdapter().getCount();
		assertEquals(4, itemsCount);
		//0 - shadow
		//1 - "What's here:"
		MenuObject item = (MenuObject) menuList.getAdapter().getItem(2);
		assertNotNull(item);
		pressBack();

		boolean isClosed = waitForViewDisappeared(5000, 100, withId(R.id.context_menu_layout));
		assertTrue(isClosed);
		AppSettings.showFavorites(app, false);
		refreshMap(app);
		onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));

		waitForView(withId(R.id.context_menu_layout));
		assertTrue(isContextMenuOpened());
	}
}