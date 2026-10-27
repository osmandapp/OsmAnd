package net.osmand.test.ui.track;

import static net.osmand.test.common.OsmAndDialogInteractions.clickViewWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.UiAutomation;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The map center has to be moved into the visible part of the map as soon as the track menu
 * covers it, without waiting for the app to be restored from the background
 * (https://github.com/osmandapp/OsmAnd/issues/25590).
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class TrackMenuMapCenterTest extends AndroidTest {

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";
	private static final String TALL_GPX_NAME = "track_menu_chart_point_test.gpx";
	private static final float RATIO_TOLERANCE = 0.05f;
	private static final float TRACK_CENTER_TOLERANCE = 0.03f;
	private static final int ZOOM_INTO_TRACK = 18;
	private static final int PICKED_TWICE_COUNT = 80;

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private SelectedGpxFile selectedGpxFile;

	@Before
	@Override
	public void setup() {
		super.setup();
		importAndSelectGpx();
	}

	@Test
	public void testMapCenterMovedAboveTrackMenu() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);

		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		float headerOnlyMenuTop = getMenuTopRatio();
		assertMapCenteredAboveMenu(headerOnlyMenuTop);

		clickViewWithId(R.id.action_track);
		awaitTrackMenuSettled();

		float expandedMenuTop = getMenuTopRatio();
		assertTrue("Opening the Track tab is expected to expand the menu, but it went from "
				+ headerOnlyMenuTop + " to " + expandedMenuTop, expandedMenuTop < headerOnlyMenuTop);
		assertMapCenteredAboveMenu(expandedMenuTop);
	}

	/**
	 * The expanded menu hides the top widgets panel, so once the menu is closed the map center has to return
	 * below the panel, where it was before the menu.
	 */
	@Test
	public void testMapCenterRestoredBelowTopPanelAfterMenuClosed() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		// without a map of the track region, the map download suggestion hides the top panel once the track is fitted
		boolean suggestMapEnabled = setSuggestMapEnabled(false);
		boolean widgetEnabled = setTopPanelWidget(true);
		try {
			awaitTrackMenuSettled();
			boolean[] panelShown = new boolean[1];
			scenarioRule.getScenario().onActivity(activity ->
					panelShown[0] = activity.findViewById(R.id.top_widgets_panel).isShown());
			assumeTrue("A top toolbar hides the top panel", panelShown[0]);
			float ratioBeforeMenu = getMapRatio().y;

			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			clickViewWithId(R.id.action_track);
			awaitTrackMenuSettled();
			// in portrait Back collapses the menu to its header first
			for (int i = 0; i < 3 && getTrackMenuView() != null; i++) {
				Espresso.pressBack();
				awaitTrackMenuSettled();
			}
			assertNull("Track menu is not closed", getTrackMenuView());
			assertEquals("Map center after the menu is closed", ratioBeforeMenu, getMapRatio().y, 0.005f);
		} finally {
			setTopPanelWidget(widgetEnabled);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	/**
	 * In landscape the menu beside the map hides the top widgets panel, so the map center must not stay below
	 * the hidden panel.
	 */
	@Test
	public void testMapCenterNotBelowTopPanelHiddenInLandscape() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		boolean suggestMapEnabled = setSuggestMapEnabled(false);
		boolean widgetEnabled = setTopPanelWidget(true);
		UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		int[] rotation = new int[1];
		scenarioRule.getScenario().onActivity(activity -> rotation[0] = activity.getWindowManager().getDefaultDisplay().getRotation());
		try {
			// ROTATION_FREEZE_0..270 are the Surface rotations: a quarter turn of a portrait screen is landscape,
			// whatever the natural orientation of the device
			uiAutomation.setRotation(isPortrait() ? (rotation[0] + 1) % 4 : UiAutomation.ROTATION_FREEZE_CURRENT);
			for (int i = 0; i < 50 && isPortrait(); i++) {
				SystemClock.sleep(100);
			}
			assertFalse("The screen did not turn to landscape", isPortrait());
			awaitTrackMenuSettled();
			assumeTrue("A top toolbar hides the top panel", isTopPanelShown());

			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			assertFalse("The track menu is expected to hide the top panel", isTopPanelShown());
			assertEquals("Map center with the top panel hidden", 0.5f, getMapRatio().y, 0.005f);
		} finally {
			uiAutomation.setRotation(rotation[0]);
			setTopPanelWidget(widgetEnabled);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	private boolean isPortrait() {
		boolean[] portrait = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> portrait[0] = AndroidUiHelper.isOrientationPortrait(activity));
		return portrait[0];
	}

	private boolean isTopPanelShown() {
		boolean[] shown = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> shown[0] = activity.findViewById(R.id.top_widgets_panel).isShown());
		return shown[0];
	}

	/**
	 * Allows or forbids the top toolbar that suggests to download the map of the region on the screen.
	 *
	 * @return whether it was allowed before
	 */
	private boolean setSuggestMapEnabled(boolean enabled) {
		OsmandPreference<Boolean> preference = app.getSettings().SHOW_SUGGEST_MAP_DIALOG;
		boolean enabledBefore = preference.get();
		preference.set(enabled);
		if (!enabled) {
			scenarioRule.getScenario().onActivity(activity -> activity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP));
		}
		return enabledBefore;
	}

	/**
	 * Shows or hides the map center coordinates widget in the top panel.
	 *
	 * @return whether it was shown before
	 */
	private boolean setTopPanelWidget(boolean enabled) {
		boolean[] enabledBefore = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> {
			MapWidgetRegistry registry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
			for (MapWidgetInfo widgetInfo : registry.getWidgetInfoForType(WidgetType.COORDINATES_MAP_CENTER)) {
				enabledBefore[0] |= widgetInfo.isEnabledForAppMode(app.getSettings().getApplicationMode(),
						MapWidgetInfo.getWidgetsVisibility(app, app.getSettings().getApplicationMode(), null));
				registry.enableDisableWidgetForMode(app.getSettings().getApplicationMode(), widgetInfo, enabled, null, true);
			}
		});
		return enabledBefore[0];
	}

	/**
	 * A menu state change fits the track into the map part above the menu, but the map center moves there only
	 * after the menu has moved, so the fitted track must not be shifted by it.
	 */
	@Test
	public void testTrackFittedAboveMenuAfterMenuStateChange() {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		boolean[] portrait = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> portrait[0] = AndroidUiHelper.isOrientationPortrait(activity));
		assumeTrue("The menu has its half screen and header states only in portrait", portrait[0]);

		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		for (int state : new int[] {MenuState.HALF_SCREEN, MenuState.HEADER_ONLY, MenuState.HALF_SCREEN}) {
			// a track that is not fully on the screen is fitted again when the menu changes its state
			zoomIntoTrack();
			onTrackMenu(fragment -> fragment.openMenuScreen(state, true));
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> assertEquals("Menu state", state, fragment.getCurrentMenuState()));
			assertTrackFittedAboveMenu(state);
		}
	}

	private void zoomIntoTrack() {
		KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
		scenarioRule.getScenario().onActivity(activity -> {
			OsmandMapTileView mapView = activity.getMapView();
			mapView.setIntZoom(ZOOM_INTO_TRACK);
			mapView.setLatLon(rect.centerY(), rect.centerX());
			mapView.refreshMap();
		});
		awaitTrackMenuSettled();
	}

	private void assertTrackFittedAboveMenu(int state) {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
		float top = tileBox.getPixYFromLatLon(rect.getTop(), rect.centerX());
		float bottom = tileBox.getPixYFromLatLon(rect.getBottom(), rect.centerX());
		float height = tileBox.getPixHeight();
		float menuTop = getMenuTopRatio() * height;
		String message = "After moving the menu to state " + state + " the track is at " + top + ".." + bottom
				+ " px, the map center at " + getMapRatio().y * height + " px, the menu at " + menuTop + " px";
		assertEquals(message, getMapRatio().y, (top + bottom) / 2 / height, TRACK_CENTER_TOLERANCE);
		assertTrue(message, top >= 0 && bottom <= menuTop);
	}

	/**
	 * A point picked on the Track tab chart has to be shown in the map part above the menu. The track is taller
	 * than that part, and neither tapping the track nor opening the tab fits it.
	 */
	@Test
	public void testChartPointShownAboveMenu() throws Throwable {
		skipAppStartDialogs(app);
		boolean[] portrait = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> portrait[0] = AndroidUiHelper.isOrientationPortrait(activity));
		assumeTrue("The menu covers the lower part of the map only in portrait", portrait[0]);

		GpxFile gpxFile = createTallTrack();
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), TALL_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile tallTrack = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		try {
			List<WptPt> points = gpxFile.getTracks().get(0).getSegments().get(0).getPoints();
			KQuadRect rect = gpxFile.getRect();
			scenarioRule.getScenario().onActivity(activity -> activity.getMapView()
					.fitRectToMap(rect.getLeft(), rect.getRight(), rect.getTop(), rect.getBottom(), 0, 0, 0));
			awaitTrackMenuSettled();
			// as GPXLayer opens the track menu when the track is tapped, here at its southern end
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, tallTrack, new SelectedGpxPoint(tallTrack, points.get(0))));
			awaitTrackMenuSettled();
			LatLon target = getMapTarget();

			clickViewWithId(R.id.action_track);
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> assertEquals("Menu state", MenuState.HALF_SCREEN, fragment.getCurrentMenuState()));
			assertTrue("Opening the Track tab must not move the map", MapUtils.getDistance(target, getMapTarget()) < 1);
			boolean[] chartShown = new boolean[1];
			onTrackMenu(fragment -> chartShown[0] = findChart(fragment.requireView()) != null);
			assumeTrue("The Track tab list creates the chart only when it fits into the half screen menu", chartShown[0]);

			// the track goes from south to north, so the end of the chart is its northern end
			assertChartPointShownAboveMenu(true, points.get(points.size() - 1));
			assertChartPointShownAboveMenu(false, points.get(0));
			assertPointPickedTwiceShownAboveMenu(gpxFile.getTracks().get(0).getSegments().get(0), points);
		} finally {
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	private void assertChartPointShownAboveMenu(boolean chartEnd, @NonNull WptPt point) {
		float zoom = getZoom();
		onTrackMenu(fragment -> tapChart(findChart(fragment.requireView()), chartEnd));
		awaitTrackMenuSettled();
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		float x = tileBox.getPixXFromLatLon(point.getLat(), point.getLon());
		float y = tileBox.getPixYFromLatLon(point.getLat(), point.getLon());
		float menuTop = getMenuTopRatio() * tileBox.getPixHeight();
		assertTrue("Chart " + (chartEnd ? "end" : "start") + ": the track point is at " + x + ", " + y
				+ " px, the menu at " + menuTop + " px", x >= 0 && x <= tileBox.getPixWidth() && y >= 0 && y <= menuTop);
		assertEquals("Picking a chart point must not change the zoom", zoom, getZoom(), 0.01f);
	}

	/**
	 * A tap on the chart reports the tapped point twice, a few milliseconds apart, the second time while the map is
	 * already moving to it. With Version 1 of the map rendering engine the running animation could overwrite or shift
	 * the second report, so the map stayed where it was or went past the point. Picks the ends of the track in turn,
	 * the reports 0 to 4 ms apart.
	 */
	private void assertPointPickedTwiceShownAboveMenu(@NonNull TrkSegment segment, @NonNull List<WptPt> points) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (int i = 0; i < PICKED_TWICE_COUNT; i++) {
			WptPt point = points.get(i % 2 == 0 ? points.size() - 1 : 0);
			long gapNanos = i % 40 * 100_000L;
			onTrackMenu(fragment -> {
				fragment.onChartPointSelected(segment, point.getLat(), point.getLon());
				long start = System.nanoTime();
				while (System.nanoTime() - start < gapNanos) {
					// busy wait: the gap is shorter than the sleep granularity
				}
				fragment.onChartPointSelected(segment, point.getLat(), point.getLon());
			});
			SystemClock.sleep(100);
			for (int j = 0; j < 50 && mapView.getAnimatedDraggingThread().isAnimating(); j++) {
				SystemClock.sleep(50);
			}
			RotatedTileBox tileBox = mapView.getRotatedTileBox();
			float x = tileBox.getPixXFromLatLon(point.getLat(), point.getLon());
			float y = tileBox.getPixYFromLatLon(point.getLat(), point.getLon());
			float menuTop = getMenuTopRatio() * tileBox.getPixHeight();
			assertTrue("Picked twice " + gapNanos / 1000 + " us apart, pick " + i + ": the track point is at " + x + ", "
					+ y + " px, the menu at " + menuTop + " px", x >= 0 && x <= tileBox.getPixWidth() && y >= 0 && y <= menuTop);
		}
	}

	/**
	 * A north-south zigzag of about 1.0 x 0.4 km, taller than the map part above the half screen menu, with
	 * altitude: the Track tab shows its chart only for a track with altitude or speed.
	 */
	@NonNull
	private static GpxFile createTallTrack() {
		TrkSegment segment = new TrkSegment();
		for (int i = 0; i < 21; i++) {
			WptPt point = new WptPt(50.1855 + 0.00045 * i, i % 2 == 0 ? 18.437 : 18.443);
			point.setEle(200 + 20 * Math.sin(i));
			segment.getPoints().add(point);
		}
		Track track = new Track();
		track.getSegments().add(segment);
		GpxFile gpxFile = new GpxFile((String) null);
		gpxFile.getTracks().add(track);
		return gpxFile;
	}

	/**
	 * Sends a tap at the first or the last value of the chart to the chart view, as a finger would. The chart may
	 * be below the part of the menu that is on the screen, so the events go to the view directly.
	 */
	private static void tapChart(@Nullable Chart<?> chart, boolean chartEnd) {
		assertNotNull("The Track tab shows no chart", chart);
		ViewPortHandler handler = chart.getViewPortHandler();
		float x = chartEnd ? handler.contentRight() - 1 : handler.contentLeft() + 1;
		float y = handler.getContentCenter().y;
		long downTime = SystemClock.uptimeMillis();
		for (int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
			MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
			chart.dispatchTouchEvent(event);
			event.recycle();
		}
	}

	@Nullable
	private static Chart<?> findChart(@NonNull View view) {
		if (view instanceof Chart<?> chart && view.getId() == R.id.chart && !chart.isEmpty() && view.getWidth() > 0) {
			return chart;
		}
		if (view instanceof ViewGroup group) {
			for (int i = 0; i < group.getChildCount(); i++) {
				Chart<?> chart = findChart(group.getChildAt(i));
				if (chart != null) {
					return chart;
				}
			}
		}
		return null;
	}

	@NonNull
	private LatLon getMapTarget() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		return new LatLon(tileBox.getLatitude(), tileBox.getLongitude());
	}

	private float getZoom() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		return (float) (tileBox.getZoom() + tileBox.getZoomFloatPart());
	}

	private void onTrackMenu(@NonNull Consumer<TrackMenuFragment> action) {
		scenarioRule.getScenario().onActivity(activity -> {
			Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG);
			assertTrue("Track menu is not shown", fragment instanceof TrackMenuFragment);
			action.accept((TrackMenuFragment) fragment);
		});
	}

	private void assertMapCenteredAboveMenu(float menuTopRatio) {
		assertEquals("Map center is not centered in the visible part of the map",
				menuTopRatio / 2, getMapRatio().y, RATIO_TOLERANCE);
	}

	@NonNull
	private PointF getMapRatio() {
		return getDisplayPositionManager().getMapRatio();
	}

	@NonNull
	private MapDisplayPositionManager getDisplayPositionManager() {
		return app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
	}

	private float getMenuTopRatio() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		View mapViewContainer = mapView.getView();
		assertNotNull(mapViewContainer);

		View menuView = getTrackMenuView();
		assertNotNull("Track menu is not shown", menuView);

		Rect menuRect = AndroidUtils.getViewBoundOnScreen(menuView);
		int mapTop = AndroidUtils.getLocationOnScreen(mapViewContainer)[1];
		return (menuRect.top - mapTop) / (float) tileBox.getPixHeight();
	}

	@Nullable
	private View getTrackMenuView() {
		Fragment[] fragment = new Fragment[1];
		scenarioRule.getScenario().onActivity(activity ->
				fragment[0] = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG));
		View view = fragment[0] != null ? fragment[0].getView() : null;
		return view != null ? view.findViewById(R.id.main_view) : null;
	}

	private void awaitTrackMenuSettled() {
		IdlingResource idlingResource = new TrackMenuSettledIdlingResource(app);
		registerIdlingResources(idlingResource);
		Espresso.onIdle();
		unregisterIdlingResources(idlingResource);
	}

	private void importAndSelectGpx() {
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME), new SaveImportedGpxListener() {
				@Override
				public void onGpxSaved(@Nullable String error, @NotNull GpxFile gpxFile) {
					if (Algorithms.isEmpty(error)) {
						File file = new File(ImportHelper.getGpxDestinationDir(app, true), SELECTED_GPX_NAME);
						gpxFile.setPath(file.getAbsolutePath());
						selectedGpxFile = app.getSelectedGpxHelper()
								.selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Waits until the track menu and the map stop moving, so that the checked map ratio is the settled one.
	 */
	private class TrackMenuSettledIdlingResource extends BaseIdlingResource {

		private static final int CHECK_INTERVAL_MS = 100;
		private static final int STABLE_CHECKS_COUNT = 3;

		private int stableChecks;
		private int lastMenuTop = Integer.MIN_VALUE;

		public TrackMenuSettledIdlingResource(@NonNull OsmandApplication app) {
			super(app);
			scheduleCheck();
		}

		@Override
		public boolean isIdleNow() {
			return stableChecks >= STABLE_CHECKS_COUNT;
		}

		private void scheduleCheck() {
			new Handler(Looper.getMainLooper()).postDelayed(() -> {
				View menuView = getTrackMenuView();
				int menuTop = menuView != null ? AndroidUtils.getViewBoundOnScreen(menuView).top : Integer.MIN_VALUE;
				if (menuTop == lastMenuTop && !app.getOsmandMap().getMapView().getAnimatedDraggingThread().isAnimating()) {
					stableChecks++;
				} else {
					stableChecks = 0;
					lastMenuTop = menuTop;
				}
				if (isIdleNow()) {
					notifyIdleTransition();
				} else {
					scheduleCheck();
				}
			}, CHECK_INTERVAL_MS);
		}
	}
}
