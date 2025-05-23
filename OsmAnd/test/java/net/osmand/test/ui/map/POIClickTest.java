package net.osmand.test.ui.map;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.test.common.AppSettings.showFavorites;
import static net.osmand.test.common.AppSettings.showWikiOnMap;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.SystemDialogInteractions.clickInView;
import static net.osmand.test.common.OsmAndDialogInteractions.isViewVisible;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.waitForAnyView;
import static net.osmand.test.common.SystemDialogInteractions.getViewById;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.data.BackgroundType;
import net.osmand.data.DataSourceType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.MenuObject;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.actions.GetViewAction;

import static org.hamcrest.CoreMatchers.anything;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
			File wikiFolder = new File(app.getAppPath(null), "wiki");
			wikiFolder.mkdir();

			copyAssetToFile(testContext, "wiki/Ukraine_kyiv_europe.wiki.obf", new File(app.getAppPath(null), "wiki/Ukraine_kyiv_europe.wiki.obf"));
			copyAssetToFile(testContext, "World_basemap_mini.obf", new File(app.getAppPath(null), "World_basemap_mini.obf"));
			copyAssetToFile(testContext, "Ukraine_kyiv-city_europe.obf", new File(app.getAppPath(null), "Ukraine_kyiv-city_europe.obf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testClickOnMApPoint() throws Throwable {
		showWikiOnMap(app);
		showFavorites(app);
		app.getSettings().WIKI_DATA_SOURCE_TYPE.set(DataSourceType.ONLINE);
		activityRule.launchActivity(null);
		double lattitude = 50.452880;
		double longitude = 30.514269;
		int zoom = 14;

		FavouritePoint favouritePoint = new FavouritePoint(lattitude, longitude, "TestFavorite", "");
		app.getFavoritesHelper().doAddFavorite("TestFavorite", "", "Test description for test favorite", "", 0xffff0000, BackgroundType.CIRCLE, FavouritePoint.DEFAULT_UI_ICON_ID, favouritePoint);

		skipAppStartDialogs(app);

		List<ClickData> clicks = parseClicksJson("clicks.json");

		for (ClickData click : clicks) {
//			if (clicks.indexOf(click) != 8) {
//				continue;
//			}
			lattitude = click.latitude;
			longitude = click.longitude;
			zoom = click.zoom;
			moveAndZoomMap(lattitude, longitude, zoom);
//			Thread.sleep(5000);
			float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
			float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
			onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
			waitForAnyView(
					2000,                         // max wait time in ms
					50,                          // polling interval in ms
					withId(R.id.context_menu_layout),    // first possible view
					withId(R.id.multi_selection_main_view)       // second possible view
			);
			boolean menuOpened = false;
			if (isViewVisible(withId(R.id.context_menu_layout))) {
				menuOpened = true;
				ViewGroup menuLayout = (ViewGroup) getViewById(R.id.context_menu_layout);
				Log.d("Corwin", "testClickOnMApPoint: opened menu " + menuLayout.getChildCount());
			}
			if (isViewVisible(withId(R.id.multi_selection_main_view))) {
				menuOpened = true;
				ViewGroup menuLayout = (ViewGroup) getViewById(R.id.multi_selection_main_view);
				ListView menuList = menuLayout.findViewById(R.id.list);
				int itemsCount = menuList.getAdapter().getCount();
				Log.d("Corwin", "testClickOnMApPoint: opened multi-selection " + itemsCount);
				for (int i = 1; i < itemsCount; i++) { // skip header
					View[] viewHolder = new View[1];
					onData(anything())
							.inAdapterView(withId(R.id.list))
							.atPosition(i)
							.perform(new GetViewAction(viewHolder));
					View itemView = viewHolder[0];
					MenuObject item = (MenuObject) menuList.getAdapter().getItem(i);
					if (item != null) {
						Log.d("Corwin", "item " + i + " title " + ((TextView) itemView.findViewById(R.id.context_menu_line1)).getText());
						Log.d("Corwin", "item " + i + " description " + ((TextView) itemView.findViewById(R.id.context_menu_line2)).getText());
						if(item.getRightIconId() != 0) {
							Log.d("Corwin", "item " + i + " icon " + app.getResources().getResourceEntryName(item.getRightIconId()));
						} else {
							Log.d("Corwin", "item " + i + " no icon");
						}
					}
				}


			}
			if (menuOpened) {
				pressBack();
			} else {
				Log.d("Corwin", "testClickOnMApPoint: nothing opened");
			}
		}


//		moveAndZoomMap(lattitude, longitude, zoom);
//		float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
//		float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
//		onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));

	}

	private void moveAndZoomMap(double latitude, double longitude, int zoom) {
		app.getOsmandMap().getMapView().setLatLon(latitude, longitude);
		app.getOsmandMap().getMapView().refreshMap();
		app.getOsmandMap().getMapView().setIntZoom(zoom);
	}

	public List<ClickData> parseClicksJson(String fileName) {
		List<ClickData> clickList = new ArrayList<>();
		String jsonString;
		try {
			InputStream is = testContext.getAssets().open(fileName);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			jsonString = new String(buffer, StandardCharsets.UTF_8);
		} catch (IOException e) {
			Assert.fail("Can't read clicks.json");
			return null;
		}

		try {
			JSONObject jsonObject = new JSONObject(jsonString);
			JSONArray clicksArray = jsonObject.getJSONArray("clicks");

			for (int i = 0; i < clicksArray.length(); i++) {
				JSONObject clickObject = clicksArray.getJSONObject(i);
				double latitude = clickObject.getDouble("latitude");
				double longitude = clickObject.getDouble("longitude");
				int zoom = clickObject.getInt("zoom");
				clickList.add(new ClickData(latitude, longitude, zoom));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Assert.fail("Can't parse clicks.json");
		}
		return clickList;
	}

	public record ClickData(double latitude, double longitude, int zoom) {
	}
}