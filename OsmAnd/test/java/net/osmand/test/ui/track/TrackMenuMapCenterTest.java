package net.osmand.test.ui.track;

import static net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER;
import static net.osmand.test.common.OsmAndDialogInteractions.clickViewWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.skipSpeedCamerasBottomSheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.UiAutomation;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.Location;
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
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.WidgetUtils;
import net.osmand.plus.views.MapActions;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
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
import java.util.concurrent.TimeUnit;
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
	// the start of gpx_recalc_test.gpx, as in RouteRecalculationFromBeginningTest
	private static final LatLon TRACK_START = new LatLon(50.17356, 18.51406);
	private static final long ROUTE_TIMEOUT_MS = 30_000;
	private static final String BOTTOM_WIDGET_ID = WidgetType.CURRENT_TIME.id + MapWidgetInfo.DELIMITER + "bottom_panel_test";
	private static final float RATIO_TOLERANCE = 0.05f;
	private static final float TRACK_CENTER_TOLERANCE = 0.03f;
	private static final int ZOOM_INTO_TRACK = 18;
	private static final int PICKED_TWICE_COUNT = 80;
	private static final int SMALL_DRAG_PX = 40;

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
		assertZoomButtonsKeepMapCenter();

		clickViewWithId(R.id.action_track);
		awaitTrackMenuSettled();

		float expandedMenuTop = getMenuTopRatio();
		assertTrue("Opening the Track tab is expected to expand the menu, but it went from "
				+ headerOnlyMenuTop + " to " + expandedMenuTop, expandedMenuTop < headerOnlyMenuTop);
		assertMapCenteredAboveMenu(expandedMenuTop);
		assertZoomButtonsKeepMapCenter();
	}

	@Test
	public void testOnlyTrackMenuZoomButtonsShownInPortrait() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The track menu has its own zoom buttons in portrait only", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> assertEquals("Menu state", MenuState.HEADER_ONLY, fragment.getCurrentMenuState()));

		assertEquals("Shown zoom in buttons", 1, countShownMapButtons(ZoomInButton.class));
		assertEquals("Shown zoom out buttons", 1, countShownMapButtons(ZoomOutButton.class));
		assertEquals("Shown zoom in buttons of the main map", 0, countShownMainMapButtons(ZoomInButton.class));
		assertEquals("Shown zoom out buttons of the main map", 0, countShownMainMapButtons(ZoomOutButton.class));
	}

	/**
	 * The tabs of the menu change its height, and the map center has to follow it.
	 */
	@Test
	public void testMapCenterAboveMenuOnEveryTab() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		for (int tabId : new int[] {R.id.action_track, R.id.action_points, R.id.action_options, R.id.action_overview}) {
			clickViewWithId(tabId);
			awaitTrackMenuSettled();
			int[] state = new int[1];
			onTrackMenu(fragment -> state[0] = fragment.getCurrentMenuState());
			// a full screen menu leaves no map to center
			if (state[0] != MenuState.FULL_SCREEN) {
				assertMapCenteredAboveMenu(getMenuTopRatio());
			}
		}
	}

	/**
	 * Opened with a fit, as from the list of tracks, the menu has to fit the track into the map part above it.
	 */
	@Test
	public void testTrackFittedAboveMenuWhenOpened() {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());

		zoomIntoTrack();
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null, null, null, new Bundle()));
		awaitTrackMenuSettled();
		int[] state = new int[1];
		onTrackMenu(fragment -> state[0] = fragment.getCurrentMenuState());
		assertTrackFittedAboveMenu(state[0]);
	}

	/**
	 * The menu dragged a little and let go returns to its state, and the map center has to stay where it was.
	 */
	@Test
	public void testMapCenterKeptAfterSmallMenuDrag() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("Only the menu at the bottom of the screen is dragged", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		clickViewWithId(R.id.action_track);
		awaitTrackMenuSettled();
		float menuTop = getMenuTopRatio();
		float ratio = getMapRatio().y;
		LatLon target = getMapTarget();

		View menuView = getTrackMenuView();
		assertNotNull("Track menu is not shown", menuView);
		Rect menuRect = AndroidUtils.getViewBoundOnScreen(menuView);
		dragOnScreen(menuRect.exactCenterX(), menuRect.top + SMALL_DRAG_PX, menuRect.top - SMALL_DRAG_PX);
		awaitTrackMenuSettled();
		assertEquals("Menu after a small drag", menuTop, getMenuTopRatio(), 0.005f);
		assertEquals("Map center after a small drag of the menu", ratio, getMapRatio().y, 0.005f);
		double moved = MapUtils.getDistance(target, getMapTarget());
		assertTrue("A small drag of the menu moved the map by " + moved + " m", moved < 1);
	}

	/**
	 * The map center is already in the visible part of the map, so going to the background and back must not move
	 * it. Before the fix of the issue it got there only when the app came back.
	 */
	@Test
	public void testMapCenterKeptAfterReturnFromBackground() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		clickViewWithId(R.id.action_track);
		awaitTrackMenuSettled();
		PointF ratio = new PointF(getMapRatio().x, getMapRatio().y);
		LatLon target = getMapTarget();

		scenarioRule.getScenario().moveToState(Lifecycle.State.CREATED);
		scenarioRule.getScenario().moveToState(Lifecycle.State.RESUMED);
		awaitTrackMenuSettled();
		assertEquals("Map center after the return from the background", ratio.x, getMapRatio().x, 0.005f);
		assertEquals("Map center after the return from the background", ratio.y, getMapRatio().y, 0.005f);
		double moved = MapUtils.getDistance(target, getMapTarget());
		assertTrue("The return from the background moved the map by " + moved + " m", moved < 1);
	}

	/**
	 * The +/- buttons on the screen zoom around the map center, so the map center has to stay where it is.
	 */
	private void assertZoomButtonsKeepMapCenter() {
		for (int buttonId : new int[] {R.id.map_zoom_in_button, R.id.map_zoom_out_button}) {
			String button = app.getResources().getResourceEntryName(buttonId);
			LatLon target = getMapTarget();
			float zoom = getZoom();
			// the button's click handler, not an Espresso tap: on the tablet such a tap changed the zoom by 0.2
			scenarioRule.getScenario().onActivity(activity -> {
				View view = findShownView(activity.getWindow().getDecorView(), buttonId);
				assertNotNull(button + " is not shown", view);
				view.performClick();
			});
			awaitTrackMenuSettled();
			assertEquals(button + " changes the zoom by one", 1f, Math.abs(getZoom() - zoom), 0.1f);
			double moved = MapUtils.getDistance(target, getMapTarget());
			assertTrue(button + " moved the map center by " + moved + " m", moved < 1);
		}
	}

	@Nullable
	private static View findShownView(@NonNull View view, int id) {
		if (view.getId() == id && view.isShown()) {
			return view;
		}
		if (view instanceof ViewGroup group) {
			for (int i = 0; i < group.getChildCount(); i++) {
				View shownView = findShownView(group.getChildAt(i), id);
				if (shownView != null) {
					return shownView;
				}
			}
		}
		return null;
	}

	/**
	 * Drags a finger on the screen from fromY to toY and lifts it, as a user would.
	 */
	private static void dragOnScreen(float x, float fromY, float toY) {
		UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		long downTime = SystemClock.uptimeMillis();
		int steps = 10;
		for (int i = 0; i <= steps + 1; i++) {
			int action = i == 0 ? MotionEvent.ACTION_DOWN : i <= steps ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_UP;
			float y = fromY + (toY - fromY) * Math.min(i, steps) / steps;
			MotionEvent event = MotionEvent.obtain(downTime, downTime + i * 16L, action, x, y, 0);
			event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
			uiAutomation.injectInputEvent(event, true);
			event.recycle();
		}
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
	 * The return from the background (Home or the lock screen) recreates the map widgets, so both panels are hidden
	 * and shown again, the top one first. The map center must stay where it was, above the bottom panel (#27180).
	 */
	@Test
	public void testMapCenterKeptAboveBottomPanelAfterReturnFromBackground() throws Throwable {
		skipAppStartDialogs(app);
		boolean suggestMapEnabled = setSuggestMapEnabled(false);
		boolean widgetEnabled = setTopPanelWidget(true);
		setBottomPanelWidget(true);
		try {
			assertMapCenterKeptBetweenPanelsAfterReturnFromBackground();
		} finally {
			setBottomPanelWidget(false);
			setTopPanelWidget(widgetEnabled);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	/**
	 * The same in navigation, where the map center is the location marker: it went under the bottom panel (#27180).
	 */
	@Test
	public void testLocationMarkerKeptAboveBottomPanelAfterReturnFromBackgroundInNavigation() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		boolean suggestMapEnabled = setSuggestMapEnabled(false);
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean mapLinkedToLocation = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
		try {
			startNavigationAlongTrack();
			// navigation switches to its own profile, so the widgets are shown in that one
			boolean widgetEnabled = setTopPanelWidget(true);
			setBottomPanelWidget(true);
			try {
				assertMapCenterKeptBetweenPanelsAfterReturnFromBackground();
				assertTrackStartInVisibleMapRect();
			} finally {
				setBottomPanelWidget(false);
				setTopPanelWidget(widgetEnabled);
			}
		} finally {
			stopNavigationAlongTrack(appMode, mapLinkedToLocation);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	private void assertMapCenterKeptBetweenPanelsAfterReturnFromBackground() {
		awaitTrackMenuSettled();
		assumeTrue("A top toolbar hides the top panel", isTopPanelShown());
		assertTrue("Bottom panel is not shown", isBottomPanelShown());
		assertVisibleMapRectFreeOfPanels("Before the return from the background");
		float ratioBefore = getMapRatio().y;

		scenarioRule.getScenario().moveToState(Lifecycle.State.CREATED);
		scenarioRule.getScenario().moveToState(Lifecycle.State.RESUMED);
		awaitTrackMenuSettled();
		assertTrue("Top panel is not shown after the return from the background", isTopPanelShown());
		assertTrue("Bottom panel is not shown after the return from the background", isBottomPanelShown());
		assertVisibleMapRectFreeOfPanels("After the return from the background");
		assertEquals("Map center after the return from the background", ratioBefore, getMapRatio().y, 0.005f);
	}

	/**
	 * The map center is placed in the visible part of the map, so that part must not reach under a shown panel.
	 */
	private void assertVisibleMapRectFreeOfPanels(@NonNull String when) {
		scenarioRule.getScenario().onActivity(activity -> {
			Rect visibleRect = getDisplayPositionManager().getVisibleMapRect();
			assertNotNull("No visible map part", visibleRect);
			int[] mapOnScreen = AndroidUtils.getLocationOnScreen(app.getOsmandMap().getMapView().getView());
			for (int panelId : new int[] {R.id.top_widgets_panel, R.id.map_bottom_widgets_panel}) {
				View panel = activity.findViewById(panelId);
				if (panel.isShown()) {
					Rect panelRect = AndroidUtils.getViewBoundOnScreen(panel);
					panelRect.offset(-mapOnScreen[0], -mapOnScreen[1]);
					assertFalse(when + ": the visible map part " + visibleRect + " reaches under "
							+ activity.getResources().getResourceEntryName(panelId) + " " + panelRect,
							Rect.intersects(visibleRect, panelRect));
				}
			}
		});
	}

	private void assertTrackStartInVisibleMapRect() {
		scenarioRule.getScenario().onActivity(activity -> {
			RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
			int x = (int) tileBox.getPixXFromLatLon(TRACK_START.getLatitude(), TRACK_START.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(TRACK_START.getLatitude(), TRACK_START.getLongitude());
			Rect visibleRect = getDisplayPositionManager().getVisibleMapRect();
			assertTrue("The location marker at " + x + "," + y + " is out of the visible map part " + visibleRect,
					visibleRect != null && visibleRect.contains(x, y));
		});
	}

	/**
	 * Starts navigation along the selected track from its start. It does not go through the route screen, whose
	 * button may be hidden on the map.
	 */
	private void startNavigationAlongTrack() {
		scenarioRule.getScenario().onActivity(activity -> {
			// without it the device's own position is used, and the route is calculated from there
			app.getLocationProvider().setCustomLocation(new Location(SIMULATED_PROVIDER,
					TRACK_START.getLatitude(), TRACK_START.getLongitude()), TimeUnit.MINUTES.toMillis(5));
			MapActions mapActions = app.getOsmandMap().getMapActions();
			// the profile in use may be public transport, which calculates no route along a track
			mapActions.enterRoutePlanningModeGivenGpx(selectedGpxFile.getGpxFile(), ApplicationMode.CAR, TRACK_START, null,
					false, false, MapRouteInfoMenu.DEFAULT_MENU_STATE, null);
			mapActions.startNavigation();
		});
		skipSpeedCamerasBottomSheet();
		long deadline = SystemClock.elapsedRealtime() + ROUTE_TIMEOUT_MS;
		while (!app.getRoutingHelper().isRouteCalculated() && SystemClock.elapsedRealtime() < deadline) {
			SystemClock.sleep(200);
		}
		assertTrue("The route along the track is not calculated", app.getRoutingHelper().isRouteCalculated());
		assertTrue("Navigation is not started", app.getRoutingHelper().isFollowingMode());
	}

	/**
	 * Stops navigation and puts back what it changed, so that the tests that follow do not start with them.
	 */
	private void stopNavigationAlongTrack(@NonNull ApplicationMode appMode, boolean mapLinkedToLocation) {
		scenarioRule.getScenario().onActivity(activity -> {
			app.stopNavigation();
			app.getSettings().setApplicationMode(appMode);
			// navigation makes the map follow the location: left on, the map moves to the device's own position
			app.getMapViewTrackingUtilities().setMapLinkedToLocation(mapLinkedToLocation);
			app.getLocationProvider().setCustomLocation(null, 0);
		});
	}

	private boolean isBottomPanelShown() {
		boolean[] shown = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> shown[0] = activity.findViewById(R.id.map_bottom_widgets_panel).isShown());
		return shown[0];
	}

	/**
	 * Adds the widget to the bottom panel or removes it, as the Configure screen does.
	 */
	private void setBottomPanelWidget(boolean enabled) {
		scenarioRule.getScenario().onActivity(activity -> {
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
			MapWidgetRegistry registry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
			if (enabled) {
				WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);
				MapWidgetInfo widgetInfo = creator.createWidgetInfo(new MapWidgetsFactory(activity), BOTTOM_WIDGET_ID, WidgetType.CURRENT_TIME);
				assertNotNull(widgetInfo);
				WidgetUtils.createNewWidget(activity, widgetInfo, WidgetsPanel.BOTTOM, appMode, layoutMode, true);
			} else {
				MapWidgetInfo widgetInfo = registry.getWidgetInfoById(BOTTOM_WIDGET_ID);
				if (widgetInfo != null) {
					registry.removeWidget(activity, appMode, widgetInfo, layoutMode);
				}
			}
		});
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
		int rotation = getRotation();
		try {
			turnToLandscape(rotation);
			awaitTrackMenuSettled();
			assumeTrue("A top toolbar hides the top panel", isTopPanelShown());

			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			assertFalse("The track menu is expected to hide the top panel", isTopPanelShown());
			assertEquals("Map center with the top panel hidden", 0.5f, getMapRatio().y, 0.005f);
		} finally {
			setRotation(rotation);
			setTopPanelWidget(widgetEnabled);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	/**
	 * In landscape the menu is a panel beside the map. Opened with a fit, it has to fit the track into the map part
	 * beside it; on the Track tab the map center has to be in the middle of that part, and the +/- buttons must not
	 * move it.
	 */
	@Test
	public void testTrackAndMapCenterBesideMenuInLandscape() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		boolean suggestMapEnabled = setSuggestMapEnabled(false);
		int rotation = getRotation();
		try {
			turnToLandscape(rotation);
			zoomIntoTrack();
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null, null, null, new Bundle()));
			awaitTrackMenuSettled();
			assertTrackFittedBesideMenu();

			clickViewWithId(R.id.action_track);
			awaitTrackMenuSettled();
			assertMapCenteredBesideMenu();
			assertZoomButtonsKeepMapCenter();
		} finally {
			setRotation(rotation);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	private int getRotation() {
		int[] rotation = new int[1];
		scenarioRule.getScenario().onActivity(activity -> rotation[0] = activity.getWindowManager().getDefaultDisplay().getRotation());
		return rotation[0];
	}

	private void turnToLandscape(int rotation) {
		// ROTATION_FREEZE_0..270 are the Surface rotations: a quarter turn of a portrait screen is landscape,
		// whatever the natural orientation of the device
		setRotation(isPortrait() ? (rotation + 1) % 4 : UiAutomation.ROTATION_FREEZE_CURRENT);
		for (int i = 0; i < 50 && isPortrait(); i++) {
			SystemClock.sleep(100);
		}
		assertFalse("The screen did not turn to landscape", isPortrait());
	}

	private static void setRotation(int rotation) {
		InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(rotation);
	}

	/**
	 * Returns the part of the map that the menu beside it, in landscape, leaves visible, in map pixels.
	 */
	@NonNull
	private Rect getMapPartBesideMenu() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		View menuView = getTrackMenuView();
		assertNotNull("Track menu is not shown", menuView);
		Rect menu = AndroidUtils.getViewBoundOnScreen(menuView);
		int[] mapLocation = AndroidUtils.getLocationOnScreen(mapView.getView());
		menu.offset(-mapLocation[0], -mapLocation[1]);
		Rect part = new Rect(0, 0, tileBox.getPixWidth(), tileBox.getPixHeight());
		if (menu.centerX() < part.centerX()) {
			part.left = menu.right;
		} else {
			part.right = menu.left;
		}
		return part;
	}

	private void assertMapCenteredBesideMenu() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		Rect part = getMapPartBesideMenu();
		PointF ratio = getMapRatio();
		String message = "The map center is at " + ratio.x * tileBox.getPixWidth() + ", "
				+ ratio.y * tileBox.getPixHeight() + " px, the map beside the menu " + part.toShortString();
		assertEquals(message, part.exactCenterX() / tileBox.getPixWidth(), ratio.x, RATIO_TOLERANCE);
		assertEquals(message, part.exactCenterY() / tileBox.getPixHeight(), ratio.y, RATIO_TOLERANCE);
	}

	private void assertTrackFittedBesideMenu() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
		float left = tileBox.getPixXFromLatLon(rect.centerY(), rect.getLeft());
		float right = tileBox.getPixXFromLatLon(rect.centerY(), rect.getRight());
		float top = tileBox.getPixYFromLatLon(rect.getTop(), rect.centerX());
		float bottom = tileBox.getPixYFromLatLon(rect.getBottom(), rect.centerX());
		Rect part = getMapPartBesideMenu();
		String message = "The track is at " + left + ".." + right + " x " + top + ".." + bottom
				+ " px, the map beside the menu " + part.toShortString();
		assertTrue(message, left >= part.left && right <= part.right && top >= part.top && bottom <= part.bottom);
		assertEquals(message, part.exactCenterX(), (left + right) / 2, TRACK_CENTER_TOLERANCE * tileBox.getPixWidth());
		assertEquals(message, part.exactCenterY(), (top + bottom) / 2, TRACK_CENTER_TOLERANCE * tileBox.getPixHeight());
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
			WptPt start = points.get(0);
			WptPt end = points.get(points.size() - 1);
			assertChartPointShownAboveMenu("Chart end tapped", chart -> tapChart(chart, true), end);
			assertChartPointShownAboveMenu("Chart start tapped", chart -> tapChart(chart, false), start);
			assertChartPointShownAboveMenu("Chart dragged to its end", TrackMenuMapCenterTest::dragChartToEnd, end);
			assertPointPickedTwiceShownAboveMenu(gpxFile.getTracks().get(0).getSegments().get(0), points);
		} finally {
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	private void assertChartPointShownAboveMenu(@NonNull String gesture, @NonNull Consumer<Chart<?>> pick,
	                                            @NonNull WptPt point) {
		float zoom = getZoom();
		onTrackMenu(fragment -> pick.accept(findChart(fragment.requireView())));
		awaitTrackMenuSettled();
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		float x = tileBox.getPixXFromLatLon(point.getLat(), point.getLon());
		float y = tileBox.getPixYFromLatLon(point.getLat(), point.getLon());
		float menuTop = getMenuTopRatio() * tileBox.getPixHeight();
		assertTrue(gesture + ": the track point is at " + x + ", " + y
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

	/**
	 * Drags a finger along the chart from its first value to its last, as the user moves the slider. Like the tap,
	 * the events go to the chart view directly.
	 */
	private static void dragChartToEnd(@Nullable Chart<?> chart) {
		assertNotNull("The Track tab shows no chart", chart);
		ViewPortHandler handler = chart.getViewPortHandler();
		float left = handler.contentLeft() + 1;
		float right = handler.contentRight() - 1;
		float y = handler.getContentCenter().y;
		long downTime = SystemClock.uptimeMillis();
		int steps = 20;
		for (int i = 0; i <= steps + 1; i++) {
			int action = i == 0 ? MotionEvent.ACTION_DOWN : i <= steps ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_UP;
			float x = left + (right - left) * Math.min(i, steps) / steps;
			MotionEvent event = MotionEvent.obtain(downTime, downTime + i * 16L, action, x, y, 0);
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

	private int countShownMapButtons(@NonNull Class<? extends MapButton> buttonClass) {
		int[] count = new int[1];
		scenarioRule.getScenario().onActivity(activity ->
				count[0] = countShownViews(activity.getWindow().getDecorView(), buttonClass));
		return count[0];
	}

	private int countShownMainMapButtons(@NonNull Class<? extends MapButton> buttonClass) {
		int[] count = new int[1];
		scenarioRule.getScenario().onActivity(activity ->
				count[0] = countShownViews(activity.findViewById(R.id.map_hud_layout), buttonClass));
		return count[0];
	}

	private static int countShownViews(@NonNull View view, @NonNull Class<? extends View> viewClass) {
		int count = viewClass.isInstance(view) && view.isShown() ? 1 : 0;
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				count += countShownViews(group.getChildAt(i), viewClass);
			}
		}
		return count;
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
		TrackMenuSettledIdlingResource idlingResource = new TrackMenuSettledIdlingResource(app);
		registerIdlingResources(idlingResource);
		try {
			Espresso.onIdle();
		} finally {
			// after a timeout it would go on checking the activity, which the end of the test destroys
			idlingResource.stop();
			unregisterIdlingResources(idlingResource);
		}
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
		private volatile boolean stopped;

		public TrackMenuSettledIdlingResource(@NonNull OsmandApplication app) {
			super(app);
			scheduleCheck();
		}

		@Override
		public boolean isIdleNow() {
			return stableChecks >= STABLE_CHECKS_COUNT;
		}

		private void stop() {
			stopped = true;
		}

		private void scheduleCheck() {
			new Handler(Looper.getMainLooper()).postDelayed(() -> {
				if (stopped) {
					return;
				}
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
