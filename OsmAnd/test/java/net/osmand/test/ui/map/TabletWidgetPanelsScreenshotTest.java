package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.PanelsLayoutMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.WidgetUtils;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
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
import java.util.ArrayList;

/**
 * Run command to pull screenshots to a git-ignored directory:
 * <p>
 * mkdir -p build/screenshots/widgets_on_tablets
 * adb pull /data/local/tmp/screenshots/widgets_on_tablets/. build/screenshots/widgets_on_tablets/
 */
@Ignore("Manual visual screenshot test only")
@RunWith(AndroidJUnit4.class)
public class TabletWidgetPanelsScreenshotTest extends AndroidTest {

	private static final Log LOG = PlatformUtil.getLog(TabletWidgetPanelsScreenshotTest.class);
	private static final String TARGET_DIR = "/data/local/tmp/screenshots/widgets_on_tablets";

	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	@Before
	public void setup() {
		super.setup();
		runShellCommand("rm -rf " + TARGET_DIR);
		runShellCommand("mkdir -p " + TARGET_DIR);

		try {
			AssetUtils.copyAssetToFile(testContext, "World_basemap_mini.obf", new File(app.getAppPath(null), "World_basemap_mini.obf"));
			app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
		} catch (Exception e) {
			LOG.error("Failed copying basemap asset", e);
		}
	}

	@After
	public void tearDown() {
		runShellCommand("wm size reset");
		runShellCommand("wm density reset");

		if (app != null) {
			app.runInUIThread(() -> {
				ApplicationMode appMode = app.getSettings().getApplicationMode();
				ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(app);

				app.getSettings().LEFT_WIDGET_PANEL_ORDER.resetModeToDefault(appMode);
				app.getSettings().RIGHT_WIDGET_PANEL_ORDER.resetModeToDefault(appMode);
				app.getSettings().getMapInfoControls(layoutMode).resetModeToDefault(appMode);
				app.getSettings().getCustomWidgetsKeys(layoutMode).resetModeToDefault(appMode);

				MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
				if (mapInfoLayer != null) {
					mapInfoLayer.recreateControls();
				}
			});
		}
	}

	@Test
	public void testCaptureWidgetPanelsOnDifferentScreenSizes() throws Exception {
		MapActivity activity = activityRule.launchActivity(null);

		skipAppStartDialogs(app);

		setupWidgetPanels(activity);

		// 1. 7-inch tablet (sw600dp: 685dp)
		setScreenResolution("1200x1920", "280");
		captureScreenshot("01_tablet_7inch_portrait.png");

		setScreenResolution("1920x1200", "280");
		captureScreenshot("02_tablet_7inch_landscape.png");

		// 2. 10-inch tablet (sw720dp: 753dp)
		setScreenResolution("1600x2560", "340");
		captureScreenshot("03_tablet_10inch_portrait.png");

		setScreenResolution("2560x1600", "340");
		captureScreenshot("04_tablet_10inch_landscape.png");

		// 3. 12.4-inch tablet (sw840dp: 1232dp)
		setScreenResolution("1848x2960", "240");
		captureScreenshot("05_tablet_12inch_portrait.png");

		setScreenResolution("2960x1848", "240");
		captureScreenshot("06_tablet_12inch_landscape.png");

		// Reset resolution back to default
		runShellCommand("wm size reset");
		runShellCommand("wm density reset");
	}

	private void setupWidgetPanels(MapActivity activity) {
		app.runInUIThread(() -> {
			activity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP);

			ApplicationMode appMode = app.getSettings().getApplicationMode();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);

			OsmandDevelopmentPlugin devPlugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
			if (devPlugin != null && !devPlugin.isEnabled()) {
				PluginsHelper.enablePlugin(activity, app, devPlugin, true);
			}

			MapWidgetsFactory factory = new MapWidgetsFactory(activity);
			WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);

			// Add 5 distinct widgets to LEFT panel (Medium size)
			MapWidgetInfo leftBattery = creator.createWidgetInfo(factory, WidgetType.BATTERY.id + MapWidgetInfo.DELIMITER + "left", WidgetType.BATTERY);
			MapWidgetInfo leftTime = creator.createWidgetInfo(factory, WidgetType.CURRENT_TIME.id + MapWidgetInfo.DELIMITER + "left", WidgetType.CURRENT_TIME);
			MapWidgetInfo leftGps = creator.createWidgetInfo(factory, WidgetType.GPS_INFO.id + MapWidgetInfo.DELIMITER + "left", WidgetType.GPS_INFO);
			MapWidgetInfo leftSun = creator.createWidgetInfo(factory, WidgetType.SUN_POSITION.id + MapWidgetInfo.DELIMITER + "left", WidgetType.SUN_POSITION);
			MapWidgetInfo leftTarget = creator.createWidgetInfo(factory, WidgetType.DEV_TARGET_DISTANCE.id + MapWidgetInfo.DELIMITER + "left", WidgetType.DEV_TARGET_DISTANCE);

			if (leftBattery != null)
				WidgetUtils.createNewWidget(activity, leftBattery, WidgetsPanel.LEFT, appMode, layoutMode, false);
			if (leftTime != null)
				WidgetUtils.createNewWidget(activity, leftTime, WidgetsPanel.LEFT, appMode, layoutMode, false);
			if (leftGps != null)
				WidgetUtils.createNewWidget(activity, leftGps, WidgetsPanel.LEFT, appMode, layoutMode, false);
			if (leftSun != null)
				WidgetUtils.createNewWidget(activity, leftSun, WidgetsPanel.LEFT, appMode, layoutMode, false);
			if (leftTarget != null)
				WidgetUtils.createNewWidget(activity, leftTarget, WidgetsPanel.LEFT, appMode, layoutMode, false);

			// Add 5 distinct widgets to RIGHT panel (Large size)
			MapWidgetInfo rightBattery = creator.createWidgetInfo(factory, WidgetType.BATTERY.id + MapWidgetInfo.DELIMITER + "right", WidgetType.BATTERY);
			MapWidgetInfo rightTime = creator.createWidgetInfo(factory, WidgetType.CURRENT_TIME.id + MapWidgetInfo.DELIMITER + "right", WidgetType.CURRENT_TIME);
			MapWidgetInfo rightGps = creator.createWidgetInfo(factory, WidgetType.GPS_INFO.id + MapWidgetInfo.DELIMITER + "right", WidgetType.GPS_INFO);
			MapWidgetInfo rightSun = creator.createWidgetInfo(factory, WidgetType.SUN_POSITION.id + MapWidgetInfo.DELIMITER + "right", WidgetType.SUN_POSITION);
			MapWidgetInfo rightTarget = creator.createWidgetInfo(factory, WidgetType.DEV_TARGET_DISTANCE.id + MapWidgetInfo.DELIMITER + "right", WidgetType.DEV_TARGET_DISTANCE);

			if (rightBattery != null)
				WidgetUtils.createNewWidget(activity, rightBattery, WidgetsPanel.RIGHT, appMode, layoutMode, false);
			if (rightTime != null)
				WidgetUtils.createNewWidget(activity, rightTime, WidgetsPanel.RIGHT, appMode, layoutMode, false);
			if (rightGps != null)
				WidgetUtils.createNewWidget(activity, rightGps, WidgetsPanel.RIGHT, appMode, layoutMode, false);
			if (rightSun != null)
				WidgetUtils.createNewWidget(activity, rightSun, WidgetsPanel.RIGHT, appMode, layoutMode, false);
			if (rightTarget != null)
				WidgetUtils.createNewWidget(activity, rightTarget, WidgetsPanel.RIGHT, appMode, layoutMode, false);

			// Apply Medium size to LEFT panel and Large size to RIGHT panel
			WidgetsSettingsHelper settingsHelper = new WidgetsSettingsHelper(activity, appMode);
			settingsHelper.applyWidgetsSize(WidgetsPanel.LEFT, WidgetSize.MEDIUM);
			settingsHelper.applyWidgetsSize(WidgetsPanel.RIGHT, WidgetSize.LARGE);
			app.getSettings().getPanelsLayoutMode(activity, layoutMode).setModeValue(appMode, PanelsLayoutMode.WIDE);

			// Refresh map controls
			MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateControls();
			}
		});
	}

	private void setScreenResolution(String size, String density) throws Exception {
		runShellCommand("wm size " + size);
		runShellCommand("wm density " + density);
		Thread.sleep(1000); // Wait for activity recreation and layout initialization

		MapActivity currentActivity = activityRule.getActivity();
		if (currentActivity != null) {
			currentActivity.runOnUiThread(() -> {
				currentActivity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP);
				if (app.getOsmandMap() != null && app.getOsmandMap().getMapView() != null) {
					app.getOsmandMap().getMapView().setLatLon(50.452880, 30.514269);
					app.getOsmandMap().getMapView().setIntZoom(6);
					app.getOsmandMap().getMapView().refreshMapComplete();
				}
				MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
				if (mapInfoLayer != null) {
					mapInfoLayer.recreateControls();
				}
				currentActivity.getWindow().getDecorView().requestLayout();
			});
		}
		Thread.sleep(4000); // Wait for map tiles and widgets to render completely
	}

	private void runShellCommand(String command) {
		try {
			UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
			try (ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(command)) {
				if (pfd != null) {
					try (InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
						byte[] buf = new byte[1024];
						while (is.read(buf) != -1) {
							// Consume stream
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed executing shell command: " + command, e);
		}
	}

	private void captureScreenshot(String fileName) {
		runShellCommand("screencap -p " + TARGET_DIR + "/" + fileName);
		LOG.info("SCREENSHOT_SAVED_AT: " + TARGET_DIR + "/" + fileName);
	}
}
