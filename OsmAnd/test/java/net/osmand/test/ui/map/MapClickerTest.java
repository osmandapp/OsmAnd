package net.osmand.test.ui.map;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.test.common.AppSettings.isShowWikiOnMap;
import static net.osmand.test.common.AppSettings.showWikiOnMap;
import static net.osmand.test.common.AssetUtils.copyAssetToFile;
import static net.osmand.test.common.AssetUtils.listAssetFiles;
import static net.osmand.test.common.OsmAndDialogInteractions.isRenderingIdle;
import static net.osmand.test.common.OsmAndDialogInteractions.moveAndZoomMap;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.waitForRenderingIdle;
import static net.osmand.test.common.SystemDialogInteractions.clickInView;
import static net.osmand.test.common.SystemDialogInteractions.doubleClickInView;
import static net.osmand.test.common.SystemDialogInteractions.findDescendantOfType;
import static net.osmand.test.common.SystemDialogInteractions.getViewById;
import static net.osmand.test.common.SystemDialogInteractions.isViewVisible;
import static net.osmand.test.common.SystemDialogInteractions.longClickInView;
import static net.osmand.test.common.SystemDialogInteractions.waitForAnyView;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.pm.PackageInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesOnlineProvider;
import net.osmand.plus.mapcontextmenu.other.MenuObject;
import net.osmand.plus.utils.FileUtils;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.PoiTypesInitIdlingResource;
import net.osmand.test.common.actions.GetViewAction;

import org.junit.After;
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
public class MapClickerTest extends AndroidTest {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExplorePlacesOnlineProvider.class);
	// Rule to launch the main activity before each test
	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	private PoiTypesInitIdlingResource poiTypesInitIdlingResource; // Declare the IdlingResource

	@Before
	public void setup() {
		super.setup();
		copyObfsFromAssetToAppFolder();
		poiTypesInitIdlingResource = new PoiTypesInitIdlingResource("PoiTypesInit", app);
		IdlingRegistry.getInstance().register(poiTypesInitIdlingResource);
	}

	private void copyObfsFromAssetToAppFolder() {
		try {
			File wikiFolder = new File(app.getAppPath(null), "wiki");
			wikiFolder.mkdir();
			for (String name : listAssetFiles(testContext, "")) {
				if (name.endsWith(".obf")) {
					copyAssetToFile(testContext, name, new File(app.getAppPath(null), name));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void unregisterIdlingResource() {
		if (poiTypesInitIdlingResource != null) {
			IdlingRegistry.getInstance().unregister(poiTypesInitIdlingResource);
		}
	}

	@Test
	public void testClickOnMApPoint() throws Throwable {
		showWikiOnMap(app);
		assertTrue(isShowWikiOnMap(app));
		activityRule.launchActivity(null);
		double lattitude = 50.452880;
		double longitude = 30.514269;
		int zoom = 14;
		skipAppStartDialogs(app);
		MapRendererView rendererView = app.getOsmandMap().getMapView().getMapRenderer();
		assertNotNull(rendererView);
		List<ClickData> clicks = parseClicksJson("clicks.json");
		ClickTestResult testResult = new ClickTestResult();
		PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
		testResult.appVersion = pi.versionName;
		testResult.events = new ArrayList<>();

		for (ClickData click : clicks) {
			lattitude = click.latitude;
			longitude = click.longitude;
			zoom = click.zoom;
			testResult.events.add(new LocationAction(new LatLon(lattitude, longitude), zoom, LocationActionType.MOVE_LOCATION));
			moveAndZoomMap(app, lattitude, longitude, zoom);
			testResult.events.add(new WaitIdleRenderingEvent(false, waitForRenderingIdle(app, false)));
			testResult.events.add(new WaitIdleRenderingEvent(true, waitForRenderingIdle(app, true)));
			float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
			float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
			testResult.events.add(new LocationAction(new LatLon(lattitude, longitude), zoom, LocationActionType.CLICK_LOCATION));
			if (click.clickType != null) {
				switch (click.clickType) {
					case SINGLE -> {
						onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
					}
					case LONG -> {
						onView(withId(R.id.map_view_with_layers)).perform(longClickInView(x, y));
					}
					case DOUBLE -> {
						onView(withId(R.id.map_view_with_layers)).perform(doubleClickInView(x, y));
					}
				}
			} else {
				onView(withId(R.id.map_view_with_layers)).perform(clickInView(x, y));
			}
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
			testResult.events.add(actionResult);
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(testResult);
		FileUtils.saveJsonToDownloadsFolder(json, app, "check_result.json");
		LOG.debug("\n\n\n\ntestClickOnMpPoint: \n" + json);
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

	public List<ClickData> parseClicksJson(String fileName) {
		String jsonString;
		try {
			InputStream is = testContext.getAssets().open(fileName);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			jsonString = new String(buffer, StandardCharsets.UTF_8);
		} catch (IOException e) {
			fail("Can't read clicks.json");
			return null;
		}
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(jsonString, ClicksData.class).clicks;
	}

	public class ClicksData {
		public List<ClickData> clicks;
	}

	public record ClickData(double latitude, double longitude, int zoom,
	                        @Nullable ClickType clickType) {
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

	public enum ClickType {
		SINGLE,
		LONG,
		DOUBLE
	}

	private class ClickTestResult {
		private String appVersion;
		private List<Event> events;
		private long totalMemory;

		public ClickTestResult() {
			Runtime runtime = Runtime.getRuntime();
			totalMemory = runtime.totalMemory();
		}

	}

	public abstract class Event {
		private final long timestamp;
		private final float cpuLoad;
		private final long usedMemory;
		private int frameId = 0;
		private boolean renderingIdle;
		private String name;

		public Event() {
			MapRendererView renderer = app.getOsmandMap().getMapView().getMapRenderer();
			float cpuBasic = 0;
			if (renderer != null) {
				cpuBasic = renderer.getBasicThreadsCPULoad();
				frameId = renderer.getFrameId();
			}
			cpuLoad = cpuBasic > 0 ? cpuBasic : 0;
			Runtime runtime = Runtime.getRuntime();
			usedMemory = runtime.totalMemory() - runtime.freeMemory();
			timestamp = System.currentTimeMillis();
			renderingIdle = isRenderingIdle(app);
			name = getClass().getSimpleName();
		}

		public long getTimestamp() {
			return timestamp;
		}

		public float getCpuLoad() {
			return cpuLoad;
		}

		public int getFrameId() {
			return frameId;
		}

		public long getUsedMemory() {
			return usedMemory;
		}
	}

	private final class WaitIdleRenderingEvent extends Event {
		private final boolean targetState;
		private final boolean isSuccess;

		public WaitIdleRenderingEvent(boolean targetState, boolean isSuccess) {
			super();
			this.targetState = targetState;
			this.isSuccess = isSuccess;
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