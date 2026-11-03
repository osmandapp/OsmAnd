package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.PanelsLayoutMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.WidgetUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.AssetUtils;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Run command to pull screenshots to a git-ignored directory:
 * <p>
 * mkdir -p build/screenshots/widgets_on_tablets
 * adb pull /data/local/tmp/osmand/screenshots/widgets_on_tablets/. build/screenshots/widgets_on_tablets/
 */
@Ignore("Manual visual screenshot test only")
@RunWith(AndroidJUnit4.class)
public class TabletWidgetPanelsScreenshotTest extends AndroidTest {

	private static final Log LOG = PlatformUtil.getLog(TabletWidgetPanelsScreenshotTest.class);

	private static final String BASEMAP_ASSET = "World_basemap_mini.obf";
	private static final WidgetType[] WIDGET_TYPES = {
			WidgetType.BATTERY,
			WidgetType.CURRENT_TIME,
			WidgetType.GPS_INFO,
			WidgetType.SUN_POSITION,
			WidgetType.DEV_TARGET_DISTANCE
	};

	// Max time for the window manager to apply size, density and rotation and recreate the activity
	private static final long SCREEN_APPLY_TIMEOUT_MS = 15000;
	private static final long SCREEN_POLL_INTERVAL_MS = 250;
	// Time for map tiles to render; there is no idle signal for the native renderer
	private static final long MAP_RENDER_DELAY_MS = 7000;
	// The first render after launch also loads map resources and takes longer than later frames
	private static final long MAP_WARM_UP_DELAY_MS = 5000;

	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
	private final List<PreferenceState<?>> preferenceStates = new ArrayList<>();

	private String targetDir;
	@Nullable
	private File copiedBasemap;
	private boolean devPluginEnabledByTest;
	private boolean naturalPortrait;
	private String previousAccelerometerRotation;
	private String previousUserRotation;
	private boolean mapPositionSaved;
	private double previousLatitude;
	private double previousLongitude;
	private int previousZoom;

	@Before
	public void setup() {
		super.setup();
		targetDir = "/data/local/tmp/osmand/screenshots/widgets_on_tablets";
		runShellCommand("rm -rf " + targetDir);
		runShellCommand("mkdir -p " + targetDir);
		runShellCommand("chmod 777 " + targetDir);

		// "wm size" limits each side to twice the physical side in the natural orientation,
		// so sizes are always set in the natural orientation and landscape comes from rotation
		DisplayManager displayManager = app.getSystemService(DisplayManager.class);
		Display.Mode mode = displayManager.getDisplay(Display.DEFAULT_DISPLAY).getMode();
		naturalPortrait = mode.getPhysicalWidth() < mode.getPhysicalHeight();

		// The analytics request dialog appears 5-30 days after install and would cover the panels
		CommonPreference<Boolean> analyticsRequestProcessed = (CommonPreference<Boolean>) settings.SEND_ANONYMOUS_DATA_REQUEST_PROCESSED;
		preferenceStates.add(new PreferenceState<>(analyticsRequestProcessed, settings.getApplicationMode()));
		analyticsRequestProcessed.set(true);

		previousAccelerometerRotation = runShellCommand("settings get system accelerometer_rotation");
		previousUserRotation = runShellCommand("settings get system user_rotation");
		runShellCommand("settings put system accelerometer_rotation 0");

		File basemap = new File(app.getAppPath(null), BASEMAP_ASSET);
		if (!basemap.exists()) {
			try {
				AssetUtils.copyAssetToFile(testContext, BASEMAP_ASSET, basemap);
				copiedBasemap = basemap;
				app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
			} catch (Exception e) {
				LOG.error("Failed copying basemap asset", e);
			}
		}
	}

	@After
	public void tearDown() {
		runShellCommand("wm size reset");
		runShellCommand("wm density reset");
		restoreSystemSetting("user_rotation", previousUserRotation);
		restoreSystemSetting("accelerometer_rotation", previousAccelerometerRotation);

		instrumentation.runOnMainSync(() -> {
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			for (PreferenceState<?> state : preferenceStates) {
				state.restore(appMode);
			}
			preferenceStates.clear();
			restoreMapPosition();

			OsmandDevelopmentPlugin devPlugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
			if (devPluginEnabledByTest && devPlugin != null) {
				PluginsHelper.enablePlugin(activityRule.getActivity(), app, devPlugin, false);
			}

			MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateControls();
			}
		});

		if (copiedBasemap != null) {
			if (!copiedBasemap.delete()) {
				LOG.error("Failed deleting copied basemap " + copiedBasemap);
			}
			copiedBasemap = null;
			app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
		}
	}

	@Test
	public void testCaptureWidgetPanelsOnDifferentScreenSizes() throws Exception {
		MapActivity activity = activityRule.launchActivity(null);

		skipAppStartDialogs(app);

		setupWidgetPanels(activity);
		instrumentation.runOnMainSync(() -> {
			saveMapPosition();
			centerMap();
		});
		Thread.sleep(MAP_WARM_UP_DELAY_MS);

		// 1. 7-inch tablet (1200x1920, sw600dp: 685dp)
		setScreenResolution(1200, 1920, 280, false);
		captureScreenshot("01_tablet_7inch_portrait.png");

		setScreenResolution(1200, 1920, 280, true);
		captureScreenshot("02_tablet_7inch_landscape.png");

		// 2. 10-inch tablet (1600x2560, sw720dp: 753dp)
		setScreenResolution(1600, 2560, 340, false);
		captureScreenshot("03_tablet_10inch_portrait.png");

		setScreenResolution(1600, 2560, 340, true);
		captureScreenshot("04_tablet_10inch_landscape.png");

		// 3. 12.4-inch tablet (1848x2960, sw840dp: 1232dp)
		setScreenResolution(1848, 2960, 240, false);
		captureScreenshot("05_tablet_12inch_portrait.png");

		setScreenResolution(1848, 2960, 240, true);
		captureScreenshot("06_tablet_12inch_landscape.png");
	}

	private void setupWidgetPanels(@NonNull MapActivity activity) {
		instrumentation.runOnMainSync(() -> {
			activity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP);

			ApplicationMode appMode = app.getSettings().getApplicationMode();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);

			CommonPreference<PanelsLayoutMode> panelsLayoutMode = app.getSettings().getPanelsLayoutMode(activity, layoutMode);
			preferenceStates.add(new PreferenceState<>(app.getSettings().LEFT_WIDGET_PANEL_ORDER, appMode));
			preferenceStates.add(new PreferenceState<>(app.getSettings().RIGHT_WIDGET_PANEL_ORDER, appMode));
			preferenceStates.add(new PreferenceState<>(app.getSettings().getMapInfoControls(layoutMode), appMode));
			preferenceStates.add(new PreferenceState<>(app.getSettings().getCustomWidgetsKeys(layoutMode), appMode));
			preferenceStates.add(new PreferenceState<>(panelsLayoutMode, appMode));

			OsmandDevelopmentPlugin devPlugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
			if (devPlugin != null && !devPlugin.isEnabled()) {
				devPluginEnabledByTest = PluginsHelper.enablePlugin(activity, app, devPlugin, true);
			}

			MapWidgetsFactory factory = new MapWidgetsFactory(activity);
			WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);
			addWidgets(activity, factory, creator, WidgetsPanel.LEFT, "left", appMode, layoutMode);
			addWidgets(activity, factory, creator, WidgetsPanel.RIGHT, "right", appMode, layoutMode);

			// Sizes are applied to every enabled widget of the panel, including the ones added before the test
			saveWidgetSizes(activity, appMode, layoutMode);

			// Apply Medium size to LEFT panel and Large size to RIGHT panel
			WidgetsSettingsHelper settingsHelper = new WidgetsSettingsHelper(activity, appMode);
			settingsHelper.applyWidgetsSize(WidgetsPanel.LEFT, WidgetSize.MEDIUM);
			settingsHelper.applyWidgetsSize(WidgetsPanel.RIGHT, WidgetSize.LARGE);
			panelsLayoutMode.setModeValue(appMode, PanelsLayoutMode.WIDE);

			// Refresh map controls
			MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateControls();
			}
		});
		instrumentation.waitForIdleSync();
	}

	private void addWidgets(@NonNull MapActivity activity, @NonNull MapWidgetsFactory factory,
			@NonNull WidgetInfoCreator creator, @NonNull WidgetsPanel panel, @NonNull String idSuffix,
			@NonNull ApplicationMode appMode, @NonNull ScreenLayoutMode layoutMode) {
		for (WidgetType widgetType : WIDGET_TYPES) {
			String widgetId = widgetType.id + MapWidgetInfo.DELIMITER + idSuffix;
			MapWidgetInfo widgetInfo = creator.createWidgetInfo(factory, widgetId, widgetType);
			if (widgetInfo != null) {
				WidgetUtils.createNewWidget(activity, widgetInfo, panel, appMode, layoutMode, false);
			}
		}
	}

	private void saveWidgetSizes(@NonNull MapActivity activity, @NonNull ApplicationMode appMode,
			@NonNull ScreenLayoutMode layoutMode) {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		int filter = MapWidgetRegistry.ENABLED_MODE | MapWidgetRegistry.AVAILABLE_MODE | MapWidgetRegistry.MATCHING_PANELS_MODE;
		List<WidgetsPanel> panels = Arrays.asList(WidgetsPanel.LEFT, WidgetsPanel.RIGHT);
		for (MapWidgetInfo widgetInfo : widgetRegistry.getWidgetsForPanel(activity, appMode, layoutMode, filter, panels)) {
			if (widgetInfo.widget instanceof ISupportWidgetResizing resizableWidget && resizableWidget.allowResize()
					&& resizableWidget.getWidgetSizePref() instanceof CommonPreference<WidgetSize> sizePref) {
				preferenceStates.add(new PreferenceState<>(sizePref, appMode));
			}
		}
	}

	private void setScreenResolution(int shortSidePx, int longSidePx, int densityDpi, boolean landscape) throws Exception {
		String size = naturalPortrait ? shortSidePx + "x" + longSidePx : longSidePx + "x" + shortSidePx;
		int rotation = landscape == naturalPortrait ? Surface.ROTATION_90 : Surface.ROTATION_0;
		// Rotate first: MapActivity locks its current orientation while it is recreated after a size change
		runShellCommand("settings put system user_rotation " + rotation);
		waitForRotation(rotation);
		runShellCommand("wm size " + size);
		runShellCommand("wm density " + densityDpi);

		int expectedWidth = landscape ? longSidePx : shortSidePx;
		int expectedHeight = landscape ? shortSidePx : longSidePx;
		MapActivity activity = waitForScreen(expectedWidth, expectedHeight, densityDpi);

		instrumentation.runOnMainSync(() -> {
			activity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP);
			centerMap();
			MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateControls();
			}
			activity.getWindow().getDecorView().requestLayout();
		});
		instrumentation.waitForIdleSync();
		Thread.sleep(MAP_RENDER_DELAY_MS);
	}

	private void saveMapPosition() {
		OsmandMapTileView mapView = app.getOsmandMap() != null ? app.getOsmandMap().getMapView() : null;
		if (mapView != null) {
			previousLatitude = mapView.getLatitude();
			previousLongitude = mapView.getLongitude();
			previousZoom = mapView.getZoom();
			mapPositionSaved = true;
		}
	}

	private void restoreMapPosition() {
		OsmandMapTileView mapView = app.getOsmandMap() != null ? app.getOsmandMap().getMapView() : null;
		if (mapPositionSaved && mapView != null) {
			mapView.setLatLon(previousLatitude, previousLongitude);
			mapView.setIntZoom(previousZoom);
			mapView.refreshMapComplete();
			mapPositionSaved = false;
		}
	}

	private void centerMap() {
		if (app.getOsmandMap() != null && app.getOsmandMap().getMapView() != null) {
			app.getOsmandMap().getMapView().setLatLon(50.452880, 30.514269);
			app.getOsmandMap().getMapView().setIntZoom(6);
			app.getOsmandMap().getMapView().refreshMapComplete();
		}
	}

	private void waitForRotation(int rotation) throws InterruptedException {
		long deadline = SystemClock.uptimeMillis() + SCREEN_APPLY_TIMEOUT_MS;
		while (SystemClock.uptimeMillis() < deadline) {
			instrumentation.waitForIdleSync();
			if (getDisplayState()[0] == rotation) {
				return;
			}
			Thread.sleep(SCREEN_POLL_INTERVAL_MS);
		}
		int[] state = getDisplayState();
		throw new AssertionError("Rotation was not applied: expected " + rotation
				+ ", actual " + state[0] + ", requested orientation " + state[1]);
	}

	/**
	 * @return display rotation and requested orientation of the resumed MapActivity (-2 if there is none)
	 */
	@NonNull
	private int[] getDisplayState() {
		int[] state = {-2, -2};
		instrumentation.runOnMainSync(() -> {
			MapActivity activity = getResumedMapActivity();
			if (activity != null) {
				state[0] = activity.getWindowManager().getDefaultDisplay().getRotation();
				state[1] = activity.getRequestedOrientation();
			}
		});
		return state;
	}

	/**
	 * Waits until the resumed MapActivity runs with the requested display size and density,
	 * so a screenshot is never taken with a size the device refused or has not applied yet.
	 */
	@NonNull
	private MapActivity waitForScreen(int width, int height, int densityDpi) throws InterruptedException {
		String expected = width + "x" + height + " @" + densityDpi + "dpi";
		String actual = "no resumed MapActivity";
		long deadline = SystemClock.uptimeMillis() + SCREEN_APPLY_TIMEOUT_MS;
		while (SystemClock.uptimeMillis() < deadline) {
			instrumentation.waitForIdleSync();
			MapActivity[] resumed = new MapActivity[1];
			Point realSize = new Point();
			int[] activityDensity = new int[1];
			instrumentation.runOnMainSync(() -> {
				resumed[0] = getResumedMapActivity();
				if (resumed[0] != null) {
					resumed[0].getWindowManager().getDefaultDisplay().getRealSize(realSize);
					activityDensity[0] = resumed[0].getResources().getConfiguration().densityDpi;
				}
			});
			if (resumed[0] != null) {
				int[] state = getDisplayState();
				actual = realSize.x + "x" + realSize.y + " @" + activityDensity[0] + "dpi"
						+ ", rotation " + state[0] + ", requested orientation " + state[1];
				if (realSize.x == width && realSize.y == height && activityDensity[0] == densityDpi) {
					return resumed[0];
				}
			}
			Thread.sleep(SCREEN_POLL_INTERVAL_MS);
		}
		throw new AssertionError("Screen was not applied: expected " + expected + ", actual " + actual);
	}

	@Nullable
	private MapActivity getResumedMapActivity() {
		for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
			if (activity instanceof MapActivity mapActivity) {
				return mapActivity;
			}
		}
		return null;
	}

	private void restoreSystemSetting(@NonNull String name, @Nullable String value) {
		if (value == null) {
			// The value was never read, so the test did not change it
			return;
		}
		if (value.isEmpty() || "null".equals(value)) {
			runShellCommand("settings delete system " + name);
		} else {
			runShellCommand("settings put system " + name + " " + value);
		}
	}

	@NonNull
	private String runShellCommand(@NonNull String command) {
		StringBuilder output = new StringBuilder();
		try {
			UiAutomation uiAutomation = instrumentation.getUiAutomation();
			try (ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(command)) {
				if (pfd != null) {
					try (InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
						byte[] buf = new byte[1024];
						int read;
						while ((read = is.read(buf)) != -1) {
							output.append(new String(buf, 0, read, StandardCharsets.UTF_8));
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed executing shell command: " + command, e);
		}
		return output.toString().trim();
	}

	private void captureScreenshot(@NonNull String fileName) {
		runShellCommand("screencap -p " + targetDir + "/" + fileName);
		LOG.info("SCREENSHOT_SAVED_AT: " + targetDir + "/" + fileName);
	}

	private static class PreferenceState<T> {

		private final CommonPreference<T> preference;
		private final boolean wasSet;
		private final T value;

		PreferenceState(@NonNull CommonPreference<T> preference, @NonNull ApplicationMode appMode) {
			this.preference = preference;
			this.wasSet = preference.isGlobal() ? preference.isSet() : preference.isSetForMode(appMode);
			this.value = preference.getModeValue(appMode);
		}

		void restore(@NonNull ApplicationMode appMode) {
			if (wasSet) {
				preference.setModeValue(appMode, value);
			} else {
				preference.resetModeToDefault(appMode);
			}
		}
	}
}
