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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Ignore("Not for automatic run with others")
@LargeTest
@RunWith(AndroidJUnit4.class)
public class MapClickerTest extends AndroidTest {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(MapClickerTest.class);
	// Rule to launch the main activity before each test
	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	private PoiTypesInitIdlingResource poiTypesInitIdlingResource; // Declare the IdlingResource

	@Before
	public void setup() {
		super.setup();
		poiTypesInitIdlingResource = new PoiTypesInitIdlingResource("PoiTypesInit", app);
		IdlingRegistry.getInstance().register(poiTypesInitIdlingResource);
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

		LOG.info("MapClickerTest start. found " + clicks.size() + " clicks");
		int progress = 0;
		for (ClickData click : clicks) {
			float clickIndex = clicks.indexOf(click);
			int clicksProgress = (int) (clickIndex / clicks.size()) * 100;
			if (clicksProgress > progress + 10) {
				progress += 10;
				LOG.info("MapClickerTest progress " + progress);
			}
			lattitude = click.latitude;
			longitude = click.longitude;
			LatLon location = new LatLon(lattitude, longitude);
			zoom = click.zoom;
			long startMoveToLocation = System.currentTimeMillis();
			moveAndZoomMap(app, lattitude, longitude, zoom);
			boolean renderingStarted = waitForRenderingIdle(app, false);
			waitForRenderingIdle(app, true);
			long endMoveToLocation = System.currentTimeMillis();
			testResult.events.add(new MoveToLocationEvent(location, click.zoom, startMoveToLocation, endMoveToLocation, renderingStarted));
			float x = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixXFromLatLon(lattitude, longitude);
			float y = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getPixYFromLatLon(lattitude, longitude);
			long startOpenMenu = System.currentTimeMillis();
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
			long endOpenMenu = System.currentTimeMillis();
			boolean menuOpened = false;
			OpenLocationEvent openLocationEvent = new OpenLocationEvent(location, zoom, startOpenMenu, endOpenMenu);
			if (isViewVisible(withId(R.id.context_menu_layout))) {
				ViewGroup menuLayout = (ViewGroup) getViewById(R.id.context_menu_layout);
				menuOpened = true;
				openLocationEvent.openType = OpenMenuResultType.OPEN;
				openLocationEvent.type = MenuType.MENU;
				ViewGroup menuBottomView = menuLayout.findViewById(R.id.context_menu_bottom_view);
				for (int i = 0; i < menuBottomView.getChildCount(); i++) {
					View child = menuBottomView.getChildAt(i); // item
					List<View> l = null;
					if (child instanceof ViewGroup) {
						l = getAllChildren((ViewGroup) child);
					}
					if (l == null) {
						continue;
					}
					List<String> textFields = new ArrayList<>();
					for (View view : l) {
						if (view instanceof TextView) {
							TextView tv = (TextView) view;
							textFields.add(tv.getText().toString());
						}
					}
					String iconIdDescription = null;
					for (View view : l) {
						if (view instanceof ImageView) {
							Object tag = view.getTag(R.id.testId);
							if (tag != null) {
								iconIdDescription = (String) tag;
								break;
							}
						}
					}
					MenuItem item = new MenuItem(
							getIconIdDescription(iconIdDescription),
							textFields
					);
					openLocationEvent.addRow(item);
				}
			}
			if (isViewVisible(withId(R.id.multi_selection_main_view))) {
				menuOpened = true;
				openLocationEvent.openType = OpenMenuResultType.OPEN;
				openLocationEvent.type = MenuType.MS;
				ViewGroup menuLayout = (ViewGroup) getViewById(R.id.multi_selection_main_view);
				ListView menuList = menuLayout.findViewById(R.id.list);
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
								Arrays.asList(((TextView) itemView.findViewById(R.id.context_menu_line1)).getText().toString(),
										((TextView) itemView.findViewById(R.id.context_menu_line1)).getText().toString())
						);
						openLocationEvent.addRow(itemDescription);
					}
				}
			}
			testResult.events.add(openLocationEvent);
			if (menuOpened) {
				pressBack();
			}
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(testResult);
		FileUtils.saveToFile(json, app, "check_result.json");
		LOG.info("MapClickerTest finish");
	}

	private String getIconWithIdDescription(int iconId) {
		if (iconId > 0) {
			return "icon " + app.getResources().getResourceEntryName(iconId);
		} else {
			return "NO_ICON";
		}
	}

	public static List<View> getAllChildren(ViewGroup parentView) {
		List<View> allChildren = new ArrayList<>();
		if (parentView == null) {
			return allChildren;
		}
		collectChildren(parentView, allChildren);
		return allChildren;
	}

	private static void collectChildren(ViewGroup viewGroup, List<View> collectedViews) {
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View child = viewGroup.getChildAt(i);
			collectedViews.add(child);
			if (child instanceof ViewGroup) {
				collectChildren((ViewGroup) child, collectedViews);
			}
		}
	}

	@NonNull
	private String getIconIdDescription(@Nullable String iconIdDescription) {
		return Objects.requireNonNullElse(iconIdDescription, "NO_ICON");
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
		MENU
	}

	public record MenuItem(String icon, List<String> textFields) {
	}

	public enum OpenMenuResultType {
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

	private abstract class Event {
		private final long start;
		private final long end;
		private final float cpuLoad;
		private final long usedMemory;
		private int frameId = 0;
		private boolean renderingIdle;
		private String name;
		private EventType eventType;

		public Event(@NonNull EventType eventType, long start, long end) {
			MapRendererView renderer = app.getOsmandMap().getMapView().getMapRenderer();
			float cpuBasic = 0;
			if (renderer != null) {
				cpuBasic = renderer.getBasicThreadsCPULoad();
				frameId = renderer.getFrameId();
			}
			cpuLoad = cpuBasic > 0 ? cpuBasic : 0;
			Runtime runtime = Runtime.getRuntime();
			usedMemory = runtime.totalMemory() - runtime.freeMemory();
			this.start = start;
			this.end = end;
			renderingIdle = isRenderingIdle(app);
			name = getClass().getSimpleName();
			this.eventType = eventType;
		}
	}

	private enum EventType {
		MOVE_TO_LOCATION,
		OPEN_LOCATION
	}

	private class LocationEvent extends Event {
		private final LatLon location;
		private final int zoom;

		public LocationEvent(@NonNull LatLon location, int zoom, @NonNull EventType eventType, long start, long end) {
			super(eventType, start, end);
			this.location = location;
			this.zoom = zoom;
		}
	}

	private class MoveToLocationEvent extends LocationEvent {
		private boolean withRendering;

		public MoveToLocationEvent(@NonNull LatLon location, int zoom, long start, long end, boolean withRendering) {
			super(location, zoom, EventType.MOVE_TO_LOCATION, start, end);
			this.withRendering = withRendering;
		}
	}

	private class OpenLocationEvent extends LocationEvent {
		private OpenMenuResultType openType = OpenMenuResultType.NOT_OPEN;
		private MenuType type;
		private List<MenuItem> rows = new ArrayList<>();

		public OpenLocationEvent(@NonNull LatLon location, int zoom, long start, long end) {
			super(location, zoom, EventType.OPEN_LOCATION, start, end);
		}

		public void setType(@NonNull MenuType type) {
			this.type = type;
		}

		public void setOpenType(OpenMenuResultType openType) {
			this.openType = openType;
		}

		public void addRow(@NonNull MenuItem row) {
			rows.add(row);
		}
	}
}