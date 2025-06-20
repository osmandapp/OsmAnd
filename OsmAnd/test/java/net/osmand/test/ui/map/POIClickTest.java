package net.osmand.test.ui.map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.test.common.AppSettings.showFavorites;
import static net.osmand.test.common.AppSettings.showWikiOnMap;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.OsmAndDialogInteractions.isContextMenuOpened;
import static net.osmand.test.common.OsmAndDialogInteractions.isMultiSelectionMenuOpened;
import static net.osmand.test.common.OsmAndDialogInteractions.moveAndZoomMap;
import static net.osmand.test.common.OsmAndDialogInteractions.refreshMap;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.SystemDialogInteractions.clickInView;
import static net.osmand.test.common.SystemDialogInteractions.getViewById;
import static net.osmand.test.common.SystemDialogInteractions.waitForAnyView;
import static net.osmand.test.common.SystemDialogInteractions.waitForViewDisappeared;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.ViewGroup;
import android.widget.ListView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.PlatformUtil;
import net.osmand.data.BackgroundType;
import net.osmand.data.DataSourceType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesOnlineProvider;
import net.osmand.plus.mapcontextmenu.other.MenuObject;
import net.osmand.test.common.AndroidTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class POIClickTest extends AndroidTest {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExplorePlacesOnlineProvider.class);
	// Rule to launch the main activity before each test
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
	public void testClickOnMApPoint() throws Throwable {
		showWikiOnMap(app);
		showFavorites(app, true);
		app.getSettings().WIKI_DATA_SOURCE_TYPE.set(DataSourceType.ONLINE);
		activityRule.launchActivity(null);
		double lattitude = 50.452880;
		double longitude = 30.514269;
		int zoom = 14;

		FavouritePoint favouritePoint = new FavouritePoint(lattitude, longitude, "TestFavorite", "");
		app.getFavoritesHelper().doAddFavorite("TestFavorite", "", "Test description for test favorite", "", 0xffff0000, BackgroundType.CIRCLE, FavouritePoint.DEFAULT_UI_ICON_ID, favouritePoint);

		skipAppStartDialogs(app);
		moveAndZoomMap(app, lattitude, longitude, zoom);
		float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
		float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
		Thread.sleep(1000);
		onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
		waitForAnyView(2000, 50, withId(R.id.multi_selection_main_view));
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
		boolean isClosed = waitForViewDisappeared(2000, 50, withId(R.id.context_menu_layout));
		assertTrue(isClosed);
		showFavorites(app, false);
		refreshMap(app);
		onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
		waitForAnyView(2000, 50, withId(R.id.context_menu_layout));
		assertTrue(isContextMenuOpened());
	}
}