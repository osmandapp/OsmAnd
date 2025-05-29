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
import static net.osmand.test.common.SystemDialogInteractions.findDescendantOfType;
import static net.osmand.test.common.SystemDialogInteractions.getViewById;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.PlatformUtil;
import net.osmand.data.BackgroundType;
import net.osmand.data.DataSourceType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesOnlineProvider;
import net.osmand.plus.mapcontextmenu.other.MenuObject;
import net.osmand.plus.utils.FileUtils;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.actions.GetViewAction;

import static org.hamcrest.CoreMatchers.anything;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
			File wikiFolder = new File(app.getAppPath(null), "wiki");
			wikiFolder.mkdir();

			copyAssetToFile(testContext, "wiki/Ukraine_kyiv_europe_2.wiki.obf", new File(app.getAppPath(null), "wiki/Ukraine_kyiv_europe_2.wiki.obf"));
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
//		app.getSettings().WIKI_DATA_SOURCE_TYPE.set(DataSourceType.ONLINE);
		activityRule.launchActivity(null);
		double lattitude = 50.452880;
		double longitude = 30.514269;
		int zoom = 14;

		FavouritePoint favouritePoint = new FavouritePoint(lattitude, longitude, "TestFavorite", "");
		app.getFavoritesHelper().doAddFavorite("TestFavorite", "", "Test description for test favorite", "", 0xffff0000, BackgroundType.CIRCLE, FavouritePoint.DEFAULT_UI_ICON_ID, favouritePoint);

		skipAppStartDialogs(app);

		List<ClickData> clicks = parseClicksJson("clicks.json");

		ArrayList<Event> events = new ArrayList<>();

		for (ClickData click : clicks) {
			lattitude = click.latitude;
			longitude = click.longitude;
			zoom = click.zoom;
			events.add(new LocationAction(new LatLon(lattitude, longitude), zoom, LocationActionType.MOVE_LOCATION));
			moveAndZoomMap(lattitude, longitude, zoom);
			float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
			float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
			events.add(new LocationAction(new LatLon(lattitude, longitude), zoom, LocationActionType.CLICK_LOCATION));
			onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
			waitForAnyView(
					5000,                         // max wait time in ms
					50,                                     // polling interval in ms
					withId(R.id.context_menu_layout),       // first possible view
					withId(R.id.multi_selection_main_view)  // second possible view
			);
			boolean menuOpened = false;
			ActionResult actionResult = null;
			if (isViewVisible(withId(R.id.context_menu_layout))) {
				ViewGroup menuLayout = (ViewGroup) getViewById(R.id.context_menu_layout);
				menuOpened = true;
				ViewGroup menuBottomView = menuLayout.findViewById(R.id.context_menu_bottom_view);
				MenuDescription menuDescription = new MenuDescription(MenuType.Menu);
				for (int i = 0; i < menuBottomView.getChildCount(); i++) {
					View child = menuBottomView.getChildAt(i); // item
					MenuItem item = new MenuItem(
							getImageViewDescription(findDescendantOfType(child, ImageView.class, 0)),
							getTextViewDescription(findDescendantOfType(child, TextView.class, 0)),
							getTextViewDescription(findDescendantOfType(child, TextView.class, 1))
					);
					menuDescription.addItem(item);
				}
				actionResult = new ActionResult(ActionResultType.OPEN, menuDescription);
			}
			if (isViewVisible(withId(R.id.multi_selection_main_view))) {
				menuOpened = true;
				ViewGroup menuLayout = (ViewGroup) getViewById(R.id.multi_selection_main_view);
				ListView menuList = menuLayout.findViewById(R.id.list);
				MenuDescription menuDescription = new MenuDescription(MenuType.MS);
				int itemsCount = menuList.getAdapter().getCount();
				for (int i = 1; i < itemsCount; i++) { // skip header
					View[] viewHolder = new View[1];
					onData(anything())
							.inAdapterView(withId(R.id.list))
							.atPosition(i)
							.perform(new GetViewAction(viewHolder));
					View itemView = viewHolder[0];
					MenuObject item = (MenuObject) menuList.getAdapter().getItem(i);
					if (item != null) {
						MenuItem itemDescription = new MenuItem(
								getIconWithIdDescription(item.getRightIconId()),
								((TextView) itemView.findViewById(R.id.context_menu_line1)).getText().toString(),
								((TextView) itemView.findViewById(R.id.context_menu_line2)).getText().toString()
						);
						menuDescription.addItem(itemDescription);
					}
				}
				actionResult = new ActionResult(ActionResultType.OPEN, menuDescription);
			}
			if (menuOpened) {
				pressBack();
			} else {
				actionResult = new ActionResult(ActionResultType.NOT_OPEN);
			}
			events.add(actionResult);
		}


		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(events);

//		File file = new File("/sdcard/0/check_result.json");
//		file.mkdirs();
//		FileOutputStream fos = new FileOutputStream(file);
//		fos.write(json.getBytes());
//		fos.close();


/*
		ContentResolver resolver = app.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "check_result.json");
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/json"); // Or "text/plain"
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); // Or DIRECTORY_DOCUMENTS

		Uri uri = null;
		try {
			uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
			if (uri != null) {
				OutputStream os = resolver.openOutputStream(uri);
				if (os != null) {
					os.write(json.getBytes());
					os.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (uri != null) {
				resolver.delete(uri, null, null);
			}
		}


*/


		FileUtils.saveFileToDownloads(json, app);

		LOG.debug("\n\n\n\ntestClickOnMApPoint: \n" + json);
	}

	private String getIconWithIdDescription(int iconId) {
		if (iconId > 0) {
			return "icon " + app.getResources().getResourceEntryName(iconId);
		} else {
			return "NO_ICON";
		}
	}

	private String getImageViewDescription(@Nullable ImageView imageView) {
		if (imageView == null) {
			return "NO_ICON";
		} else {
			return "HAS_ICON";
		}
	}

	private String getTextViewDescription(@Nullable TextView textView) {
		if (textView == null) {
			return "NO_TEXT";
		} else {
			return "text \"" + textView.getText() + "\"";
		}
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

	public enum MenuType {
		MS,
		Menu
	}

	public record MenuItem(String icon, String text1, String text2) {
	}

	public class MenuDescription {
		private final MenuType type;
		private final List<MenuItem> items;

		public MenuDescription(MenuType type) {
			this.type = type;
			this.items = new ArrayList<>();
		}

		public MenuType getType() {
			return type;
		}

		public List<MenuItem> getItems() {
			return items;
		}

		public void addItem(MenuItem newItem) {
			this.items.add(newItem);
		}

		public void addAllItems(List<MenuItem> itemsToAdd) {
			this.items.addAll(itemsToAdd);
		}
	}

	public enum LocationActionType {
		CLICK_LOCATION,
		MOVE_LOCATION
	}

	public enum ActionResultType {
		OPEN,
		NOT_OPEN
	}

	public abstract class Event {
		private final long timestamp;

		public Event() {
			this.timestamp = System.currentTimeMillis();
		}

		public long getTimestamp() {
			return timestamp;
		}
	}

	public final class LocationAction extends Event {
		private final LatLon location;
		private final int zoom;
		private final LocationActionType type;

		public LocationAction(LatLon location, int zoom, LocationActionType type) {
			super();
			this.location = location;
			this.zoom = zoom;
			this.type = type;
		}

		public LatLon getLocation() {
			return location;
		}

		public int getZoom() {
			return zoom;
		}

		public LocationActionType getType() {
			return type;
		}
	}

	public final class ActionResult extends Event {
		private final MenuDescription menuDescription;
		private final ActionResultType type;

		public ActionResult(@NonNull ActionResultType type) {
			this(type, null);
		}

		public ActionResult(@NonNull ActionResultType type, @Nullable MenuDescription menuDescription) {
			super();
			this.menuDescription = menuDescription;
			this.type = type;
		}

		@Nullable
		public MenuDescription getMenuDescription() {
			return menuDescription;
		}

		public ActionResultType getType() {
			return type;
		}
	}
}