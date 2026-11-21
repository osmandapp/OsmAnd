package net.osmand.test.ui.track;

import static net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER;
import static net.osmand.test.common.OsmAndDialogInteractions.clickViewWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.skipSpeedCamerasBottomSheet;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.UiAutomation;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapScrollHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenuFragment;
import net.osmand.plus.measurementtool.ExitBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.cards.OptionsCard;
import net.osmand.plus.track.fragments.GpsFilterFragment;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.WidgetUtils;
import net.osmand.plus.views.MapActions;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.AnimateDraggingMapThread;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
	private static final String EMPTY_GPX_NAME = "track_menu_empty_track_test.gpx";
	private static final float RATIO_TOLERANCE = 0.05f;
	private static final float TRACK_CENTER_TOLERANCE = 0.03f;
	private static final int ZOOM_INTO_TRACK = 18;
	private static final int PICKED_TWICE_COUNT = 80;
	private static final int SMALL_DRAG_PX = 40;

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private SelectedGpxFile selectedGpxFile;
	private boolean mapLinkedBefore;
	private boolean suggestMapBefore;

	@Before
	@Override
	public void setup() {
		super.setup();
		importAndSelectGpx();
		mapLinkedBefore = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
		scenarioRule.getScenario().onActivity(activity -> app.getMapViewTrackingUtilities().setMapLinkedToLocation(false));
		suggestMapBefore = setSuggestMapEnabled(false);
	}

	@After
	public void closeScreens() {
		scenarioRule.getScenario().onActivity(activity -> {
			MeasurementToolFragment measurement = activity.getFragmentsHelper().getMeasurementToolFragment();
			if (measurement != null) {
				measurement.quit(false);
			}
			activity.getTrackDetailsMenu().hide(false);
			app.getMapViewTrackingUtilities().setMapLinkedToLocation(mapLinkedBefore);
			app.getSettings().SHOW_SUGGEST_MAP_DIALOG.set(suggestMapBefore);
			for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
				assertFalse("Left on screen: " + activity.getSupportFragmentManager().getFragments(),
						fragment instanceof TrackDetailsMenuFragment);
			}
		});
	}

	@Test
	public void testMapCenterMovedAboveTrackMenu() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());

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
			assertSyncButtonAboveSystemBars();

			clickViewWithId(R.id.action_track);
			awaitTrackMenuSettled();
			assertMapCenteredBesideMenu();
			assertZoomButtonsKeepMapCenter();
		} finally {
			setRotation(rotation);
			setSuggestMapEnabled(suggestMapEnabled);
		}
	}

	@Test
	public void testGpsFilterMapControlsAboveSystemBarsInLandscape() throws Throwable {
		assertMapControlsAboveSystemBarsInLandscape(GpsFilterFragment.TAG, activity ->
				GpsFilterFragment.showInstance(activity.getSupportFragmentManager(), selectedGpxFile,
						activity.getFragmentsHelper().getTrackMenuFragment()));
	}

	@Test
	public void testAppearanceMapControlsAboveSystemBarsInLandscape() throws Throwable {
		assertMapControlsAboveSystemBarsInLandscape(TrackAppearanceFragment.TAG, activity ->
				TrackAppearanceFragment.showInstance(activity, selectedGpxFile,
						activity.getFragmentsHelper().getTrackMenuFragment()));
	}

	private void assertMapControlsAboveSystemBarsInLandscape(@NonNull String tag, @NonNull Consumer<MapActivity> open) {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		int rotation = getRotation();
		try {
			turnToLandscape(rotation);
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(open::accept);
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> {
				Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(tag);
				assertNotNull(tag + " is not shown", fragment);
				View controls = fragment.requireView().findViewById(R.id.map_controls_container);
				assertNotNull("No map controls on " + tag, controls);
				WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(controls);
				int bottomInset = insets != null ? insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom : 0;
				int freeBottom = activity.getWindow().getDecorView().getHeight() - bottomInset;
				int contentBottom = AndroidUtils.getViewBoundOnScreen(controls).bottom - controls.getPaddingBottom();
				assertTrue("The map controls of " + tag + " end at " + contentBottom + " px, below the system bars at "
						+ freeBottom + " px", contentBottom <= freeBottom);
			});
		} finally {
			setRotation(rotation);
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

	private void rotateQuarterTurn(int rotation) {
		MapActivity[] activityBefore = new MapActivity[1];
		scenarioRule.getScenario().onActivity(activity -> activityBefore[0] = activity);
		boolean portrait = isPortrait();
		setRotation((rotation + 1) % 4);
		for (int i = 0; i < 50 && isPortrait() == portrait; i++) {
			SystemClock.sleep(100);
		}
		assertNotEquals("The screen did not turn", portrait, isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				assertNotSame("The rotation did not recreate the activity", activityBefore[0], activity));
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

	private float getMapTouchX(float width) {
		if (isPortrait()) {
			return width / 2;
		}
		return isLayoutRtl() ? width * 0.2f : width * 0.8f;
	}

	private boolean isLayoutRtl() {
		boolean[] rtl = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> rtl[0] = AndroidUtils.isLayoutRtl(activity));
		return rtl[0];
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

	@Test
	public void testMapStaysAfterManualMoveUntilSyncButtonTapped() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The menu has its half screen and header states only in portrait", isPortrait());
		assertMapStaysAfterManualMoveUntilSyncButtonTapped();
	}

	@Test
	public void testLegacyRendererFitsTrackAndKeepsMapAfterManualMove() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The menu has its half screen and header states only in portrait", isPortrait());
		boolean openglBefore = app.getSettings().USE_OPENGL_RENDER.get();
		try {
			setOpenGlRenderer(false);
			assertNull(app.getOsmandMap().getMapView().getMapRenderer());

			zoomIntoTrack();
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null, null, null, new Bundle()));
			awaitTrackMenuSettled();
			int[] state = new int[1];
			onTrackMenu(fragment -> state[0] = fragment.getCurrentMenuState());
			assertTrackFittedAboveMenu(state[0]);
			onTrackMenu(fragment -> fragment.dismiss());
			awaitTrackMenuSettled();

			assertMapStaysAfterManualMoveUntilSyncButtonTapped();
		} finally {
			setOpenGlRenderer(openglBefore);
		}
	}

	private void setOpenGlRenderer(boolean enabled) {
		scenarioRule.getScenario().onActivity(activity -> {
			app.getSettings().USE_OPENGL_RENDER.set(enabled);
			app.getOsmandMap().setupRenderingView();
			activity.refreshMapComplete();
		});
		awaitTrackMenuSettled();
	}

	@Test
	public void testTiltedMapSyncFitsTrackAboveMenu() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		assumeTrue(mapView.getMapRenderer() != null);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());
		float elevationBefore = mapView.getElevationAngle();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> mapView.setElevationAngle(45));
			zoomIntoTrack();
			onTrackMenu(fragment -> fragment.setMapInSync(false));

			clickViewWithId(R.id.map_center_on_track);
			awaitTrackMenuSettled();
			assertTrue("The tap has to keep the map tilted, elevation " + mapView.getElevationAngle(),
					mapView.getElevationAngle() < 60);
			assertTrue("The tap has to turn the sync on", isTrackMenuInSync());
			assertTrackOnTiltedMapAboveMenu("After the sync tap");

			for (int state : new int[] {MenuState.HEADER_ONLY, MenuState.HALF_SCREEN}) {
				onTrackMenu(fragment -> fragment.openMenuScreen(state, true));
				awaitTrackMenuSettled();
				assertTrackOnTiltedMapAboveMenu("After moving the menu to state " + state);
			}
		} finally {
			scenarioRule.getScenario().onActivity(activity -> mapView.setElevationAngle(elevationBefore));
		}
	}

	private void assertTrackOnTiltedMapAboveMenu(@NonNull String message) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView renderer = mapView.getMapRenderer();
		assertNotNull(renderer);
		float width = mapView.getRotatedTileBox().getPixWidth();
		float menuTop = getMenuTopRatio() * mapView.getRotatedTileBox().getPixHeight();
		for (WptPt point : selectedGpxFile.getGpxFile().getAllSegmentsPoints()) {
			PointI pixel = NativeUtilities.getScreenPointFromLatLon(renderer, point.getLat(), point.getLon());
			assertNotNull(message + ": the point " + point.getLat() + ", " + point.getLon() + " is off the map", pixel);
			assertTrue(message + ": the point is at " + pixel.getX() + ", " + pixel.getY() + " px, the screen is "
							+ width + " px wide, the menu at " + menuTop + " px",
					pixel.getX() >= 0 && pixel.getX() <= width && pixel.getY() >= 0 && pixel.getY() <= menuTop);
		}
	}

	@Test
	public void testTrackMenuWithoutPointsKeepsSyncOffAndMap() throws Throwable {
		skipAppStartDialogs(app);
		GpxFile gpxFile = new GpxFile((String) null);
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), EMPTY_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile emptyTrack = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		boolean linkedBefore = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					app.getMapViewTrackingUtilities().setMapLinkedToLocation(false));
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, emptyTrack, null, null, null, new Bundle()));
			awaitTrackMenuSettled();
			assertFalse("A track without points has nothing to keep in sync", isTrackMenuInSync());
			onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(not(isSelected())));

			LatLon targetBefore = getMapTarget();
			float zoomBefore = getZoom();
			clickViewWithId(R.id.map_center_on_track);
			awaitTrackMenuSettled();
			assertFalse("A tap must not turn the sync on without points", isTrackMenuInSync());
			onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(not(isSelected())));
			assertEquals(zoomBefore, getZoom(), 0.01f);
			double moved = MapUtils.getDistance(targetBefore, getMapTarget());
			assertTrue("The map moved by " + moved + " m", moved < 1);
		} finally {
			scenarioRule.getScenario().onActivity(activity -> {
				TrackMenuFragment fragment = activity.getFragmentsHelper().getTrackMenuFragment();
				if (fragment != null) {
					fragment.dismiss();
				}
				app.getMapViewTrackingUtilities().setMapLinkedToLocation(linkedBefore);
			});
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	private void assertMapStaysAfterManualMoveUntilSyncButtonTapped() throws Throwable {
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(isSelected()));

		float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
		dragOnScreen(width / 2, height * 0.2f, height * 0.3f);
		awaitTrackMenuSettled();
		onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(not(isSelected())));

		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		LatLon targetBefore = getMapTarget();
		float zoomBefore = mapView.getZoom() + mapView.getZoomFloatPart();

		for (int state : new int[] {MenuState.HALF_SCREEN, MenuState.HEADER_ONLY, MenuState.HALF_SCREEN}) {
			onTrackMenu(fragment -> fragment.openMenuScreen(state, true));
			awaitTrackMenuSettled();
			assertEquals("Zoom after the menu moved to state " + state, zoomBefore,
					mapView.getZoom() + mapView.getZoomFloatPart(), 0.01f);
			double moved = MapUtils.getDistance(targetBefore, getMapTarget());
			assertTrue("The map moved by " + moved + " m after the menu moved to state " + state, moved < 1);
		}

		clickViewWithId(R.id.map_center_on_track);
		awaitTrackMenuSettled();
		onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(isSelected()));
		assertTrackFittedAboveMenu(MenuState.HALF_SCREEN);
	}

	@Test
	public void testMyLocationTurnsSyncOffAndSyncButtonUnlinksMap() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(isSelected()));

		scenarioRule.getScenario().onActivity(activity -> {
			app.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			app.getMapViewTrackingUtilities().setMapLinkedToLocation(true);
		});
		awaitTrackMenuSettled();
		onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(not(isSelected())));

		clickViewWithId(R.id.map_center_on_track);
		awaitTrackMenuSettled();
		onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(isSelected()));
		assertFalse(app.getMapViewTrackingUtilities().isMapLinkedToLocation());
	}

	@Test
	public void testMenuOpenedOnMyLocationKeepsMapAndStartsOutOfSync() throws Throwable {
		assertMenuOpenedOnMyLocationKeepsMap(false);
	}

	@Test
	public void testMenuOpenedWithFitOnMyLocationKeepsMapAndStartsOutOfSync() throws Throwable {
		assertMenuOpenedOnMyLocationKeepsMap(true);
	}

	private void assertMenuOpenedOnMyLocationKeepsMap(boolean openWithFit) throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		boolean linkedBefore = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
		try {
			scenarioRule.getScenario().onActivity(activity -> {
				OsmandMapTileView mapView = activity.getMapView();
				mapView.setIntZoom(ZOOM_INTO_TRACK);
				mapView.refreshMap();
				app.getMapViewTrackingUtilities().setMapLinkedToLocation(true);
			});
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> {
				if (openWithFit) {
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null, null, null, new Bundle());
				} else {
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null);
				}
			});
			awaitTrackMenuSettled();

			assertFalse("The menu opened on My location has to start out of sync", isTrackMenuInSync());
			onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(not(isSelected())));
			assertTrue("The menu must not unlink the map from My location",
					app.getMapViewTrackingUtilities().isMapLinkedToLocation());
			assertEquals("The menu must not zoom the map on My location", ZOOM_INTO_TRACK,
					app.getOsmandMap().getMapView().getZoom(), 0.01f);

			clickViewWithId(R.id.map_center_on_track);
			awaitTrackMenuSettled();
			assertTrue("A tap has to turn the sync on", isTrackMenuInSync());
			assertFalse("A tap has to unlink the map from My location",
					app.getMapViewTrackingUtilities().isMapLinkedToLocation());
			assertTrue("A tap has to fit the whole track", app.getOsmandMap().getMapView().getZoom() < ZOOM_INTO_TRACK);
		} finally {
			scenarioRule.getScenario().onActivity(activity -> app.getMapViewTrackingUtilities().setMapLinkedToLocation(linkedBefore));
		}
	}

	@Test
	public void testSyncButtonZoomsOutWhenZoomedInAndInWhenZoomedOut() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		zoomIntoTrack();
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		clickViewWithId(R.id.map_center_on_track);
		awaitTrackMenuSettled();
		float fitted = app.getOsmandMap().getMapView().getZoom();
		assertTrue("The tap has to zoom out to fit the track, zoom " + fitted, fitted < ZOOM_INTO_TRACK - 2);

		scenarioRule.getScenario().onActivity(activity -> {
			OsmandMapTileView mapView = activity.getMapView();
			mapView.setIntZoom(3);
			mapView.refreshMap();
		});
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		clickViewWithId(R.id.map_center_on_track);
		awaitTrackMenuSettled();
		assertEquals("The tap has to zoom in to fit the track", fitted, app.getOsmandMap().getMapView().getZoom(), 1f);
	}

	@Test
	public void testSyncTapFitsTrackSmoothlyOnSmallZoomChange() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue(app.getOsmandMap().getMapView().getMapRenderer() != null);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		zoomTwoLevelsIntoFittedTrack();
		float startZoom = getRendererZoom();
		OsmandPreference<Boolean> noAnimations = app.getSettings().DO_NOT_USE_ANIMATIONS;
		boolean noAnimationsBefore = noAnimations.get();
		List<Float> zooms = new ArrayList<>();
		try {
			noAnimations.set(false);
			scenarioRule.getScenario().onActivity(activity ->
					findShownView(activity.getWindow().getDecorView(), R.id.map_center_on_track).performClick());
			long end = SystemClock.uptimeMillis() + 2000;
			while (SystemClock.uptimeMillis() < end) {
				zooms.add(getRendererZoom());
				SystemClock.sleep(20);
			}
			awaitTrackMenuSettled();
		} finally {
			noAnimations.set(noAnimationsBefore);
		}
		float endZoom = getRendererZoom();
		assertTrue("The tap has to zoom out to fit the track, zoom " + endZoom, endZoom < startZoom - 1);
		long between = zooms.stream().filter(zoom -> zoom < startZoom - 0.1f && zoom > endZoom + 0.1f).distinct().count();
		assertTrue("The fit went from zoom " + startZoom + " to " + endZoom + " with " + between + " frames between: "
				+ zooms, between >= 3);
		float[] fitted = getTrackVerticalPx();
		float screenHeight = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		assertTrue("The smooth fit has to put the track " + fitted[0] + ".." + fitted[1] + " above the panel",
				!isPortrait() || fitted[0] >= 0 && fitted[1] <= screenHeight * 0.62f);
		assertTrue("The smooth fit has to keep the sync on", isTrackMenuInSync());
	}

	@Test
	public void testSyncTapJumpsOnLargeZoomChange() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue(app.getOsmandMap().getMapView().getMapRenderer() != null);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		zoomIntoTrack();
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		float startZoom = getRendererZoom();
		OsmandPreference<Boolean> noAnimations = app.getSettings().DO_NOT_USE_ANIMATIONS;
		boolean noAnimationsBefore = noAnimations.get();
		List<Float> zooms = new ArrayList<>();
		try {
			noAnimations.set(false);
			scenarioRule.getScenario().onActivity(activity ->
					findShownView(activity.getWindow().getDecorView(), R.id.map_center_on_track).performClick());
			long end = SystemClock.uptimeMillis() + 1000;
			while (SystemClock.uptimeMillis() < end) {
				zooms.add(getRendererZoom());
				SystemClock.sleep(20);
			}
			awaitTrackMenuSettled();
		} finally {
			noAnimations.set(noAnimationsBefore);
		}
		float endZoom = getRendererZoom();
		assertTrue("The tap has to zoom out to fit the track, zoom " + endZoom,
				endZoom <= startZoom - AnimateDraggingMapThread.ZOOM_DIFF_SKIP_ANIMATION);
		long between = zooms.stream().filter(zoom -> zoom < startZoom - 0.1f && zoom > endZoom + 0.1f).distinct().count();
		assertTrue("A change by " + AnimateDraggingMapThread.ZOOM_DIFF_SKIP_ANIMATION
				+ " or more levels has to jump as startMoving does, frames between: " + zooms, between <= 1);
		float[] fitted = getTrackVerticalPx();
		float screenHeight = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		assertTrue("The fit has to put the track " + fitted[0] + ".." + fitted[1] + " above the panel",
				!isPortrait() || fitted[0] >= 0 && fitted[1] <= screenHeight * 0.62f);
		assertTrue("The fit has to keep the sync on", isTrackMenuInSync());
	}

	@Test
	public void testSyncTapMovesMapSmoothlyWithoutZoomChange() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue(app.getOsmandMap().getMapView().getMapRenderer() != null);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		clickViewWithId(R.id.map_center_on_track);
		awaitTrackMenuSettled();
		LatLon fittedTarget = getMapTarget();
		float fittedZoom = getRendererZoom();

		float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
		float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		dragOnScreen(width / 2, height * 0.15f, height * 0.4f);
		awaitTrackMenuSettled();
		assertFalse("A manual move has to turn the sync off", isTrackMenuInSync());
		LatLon movedTarget = getMapTarget();
		double moved = MapUtils.getDistance(fittedTarget, movedTarget);
		assertTrue("The drag has to move the map, moved by " + moved + " m", moved > 10);

		OsmandPreference<Boolean> noAnimations = app.getSettings().DO_NOT_USE_ANIMATIONS;
		boolean noAnimationsBefore = noAnimations.get();
		List<Double> distances = new ArrayList<>();
		try {
			noAnimations.set(false);
			scenarioRule.getScenario().onActivity(activity ->
					findShownView(activity.getWindow().getDecorView(), R.id.map_center_on_track).performClick());
			long end = SystemClock.uptimeMillis() + 2000;
			while (SystemClock.uptimeMillis() < end) {
				distances.add(MapUtils.getDistance(getRendererTarget(), fittedTarget));
				SystemClock.sleep(20);
			}
			awaitTrackMenuSettled();
		} finally {
			noAnimations.set(noAnimationsBefore);
		}
		assertEquals("The fit must keep the zoom", fittedZoom, getRendererZoom(), 0.05f);
		double back = MapUtils.getDistance(fittedTarget, getMapTarget());
		assertTrue("The tap has to bring the track back, " + back + " m off", back < moved * 0.05);
		long between = distances.stream().filter(d -> d > moved * 0.1 && d < moved * 0.9).distinct().count();
		assertTrue("The move went with " + between + " frames between: " + distances, between >= 3);
	}

	private LatLon getRendererTarget() {
		PointI target = app.getOsmandMap().getMapView().getMapRenderer().getTarget();
		return new LatLon(MapUtils.get31LatitudeY(target.getY()), MapUtils.get31LongitudeX(target.getX()));
	}

	@Test
	public void testTouchDuringFitTurnsSyncOff() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue(app.getOsmandMap().getMapView().getMapRenderer() != null);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();

		zoomTwoLevelsIntoFittedTrack();
		float startZoom = getRendererZoom();
		OsmandPreference<Boolean> noAnimations = app.getSettings().DO_NOT_USE_ANIMATIONS;
		boolean noAnimationsBefore = noAnimations.get();
		try {
			noAnimations.set(false);
			scenarioRule.getScenario().onActivity(activity ->
					findShownView(activity.getWindow().getDecorView(), R.id.map_center_on_track).performClick());
			long end = SystemClock.uptimeMillis() + 1000;
			while (getRendererZoom() > startZoom - 0.2f && SystemClock.uptimeMillis() < end) {
				SystemClock.sleep(10);
			}
			float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
			float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
			dragOnScreen(width / 2, height * 0.2f, height * 0.25f);
			SystemClock.sleep(1500);
			awaitTrackMenuSettled();
		} finally {
			noAnimations.set(noAnimationsBefore);
		}
		float stoppedZoom = getRendererZoom();
		assertFalse("A touch during the fit has to turn the sync off", isTrackMenuInSync());
		assertEquals("The tile box has to keep the zoom of the renderer", stoppedZoom,
				app.getOsmandMap().getMapView().getZoom() + app.getOsmandMap().getMapView().getZoomFloatPart(), 0.1f);
	}

	private float getRendererZoom() {
		return app.getOsmandMap().getMapView().getMapRenderer().getZoom();
	}

	@Test
	public void testManualZoomAndMapScrollTurnSyncOff() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
		float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		float mapX = getMapTouchX(width);
		float mapY = height * 0.2f;

		assertManualActionTurnsSyncOff("Zoom in button", () -> scenarioRule.getScenario().onActivity(activity ->
				findShownView(activity.getWindow().getDecorView(), R.id.map_zoom_in_button).performClick()));
		assertManualActionTurnsSyncOff("Zoom out button", () -> scenarioRule.getScenario().onActivity(activity ->
				findShownView(activity.getWindow().getDecorView(), R.id.map_zoom_out_button).performClick()));
		assertManualActionTurnsSyncOff("Mouse wheel", () -> scenarioRule.getScenario().onActivity(activity ->
				activity.getMapView().onGenericMotionEvent(createWheelEvent(mapX, mapY))));
		assertManualActionTurnsSyncOff("Keyboard map scroll", () -> scenarioRule.getScenario().onActivity(activity ->
				activity.getMapScrollHelper().scrollMapAction(MapScrollHelper.ScrollDirection.UP)));
		assertManualActionTurnsSyncOff("Double tap", () -> doubleTapOnScreen(mapX, mapY));
		assertManualActionTurnsSyncOff("Slow double tap", () -> tapTwiceOnScreen(mapX, mapY, mapX, mapY, 250L, 450L));
		assertManualActionTurnsSyncOff("Pinch", () -> pinchOnScreen(mapX, mapY));
	}

	private void assertManualActionTurnsSyncOff(@NonNull String action, @NonNull Runnable perform) {
		onTrackMenu(fragment -> fragment.setMapInSync(true));
		assertTrue(isTrackMenuInSync());
		perform.run();
		awaitTrackMenuSettled();
		assertFalse(action + " has to turn the sync off", isTrackMenuInSync());
	}

	@NonNull
	private static MotionEvent createWheelEvent(float x, float y) {
		MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
		properties.id = 0;
		properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
		MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
		coords.x = x;
		coords.y = y;
		coords.setAxisValue(MotionEvent.AXIS_VSCROLL, 1f);
		long time = SystemClock.uptimeMillis();
		return MotionEvent.obtain(time, time, MotionEvent.ACTION_SCROLL, 1, new MotionEvent.PointerProperties[] {properties},
				new MotionEvent.PointerCoords[] {coords}, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
	}

	private static void doubleTapOnScreen(float x, float y) {
		tapTwiceOnScreen(x, y, x, y);
	}

	private static void tapTwiceOnScreen(float x, float y, float secondX, float secondY) {
		tapTwiceOnScreen(x, y, secondX, secondY, 50L, 100L);
	}

	private static void tapTwiceOnScreen(float x, float y, float secondX, float secondY, long holdTime, long downInterval) {
		UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		long time = SystemClock.uptimeMillis();
		for (int tap = 0; tap < 2; tap++) {
			long downTime = time + tap * downInterval;
			float tapX = tap == 0 ? x : secondX;
			float tapY = tap == 0 ? y : secondY;
			for (int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
				long eventTime = action == MotionEvent.ACTION_DOWN ? downTime : downTime + holdTime;
				MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, tapX, tapY, 0);
				event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				uiAutomation.injectInputEvent(event, true);
				event.recycle();
			}
		}
	}

	private static void tapOnScreen(float x, float y) {
		UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		long downTime = SystemClock.uptimeMillis();
		for (int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
			MotionEvent event = MotionEvent.obtain(downTime, downTime + (action == MotionEvent.ACTION_UP ? 50L : 0L), action, x, y, 0);
			event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
			uiAutomation.injectInputEvent(event, true);
			event.recycle();
		}
	}

	@Test
	public void testQuickTapsAtDifferentPlacesKeepSync() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("In landscape a tap on the map closes the track menu", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		assertTrue(isTrackMenuInSync());
		float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
		float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		float x = getMapTouchX(width);
		tapTwiceOnScreen(x - width * 0.15f, height * 0.15f, x + width * 0.15f, height * 0.35f);
		awaitTrackMenuSettled();
		assertTrue("Two quick taps at different places are not a double tap", isTrackMenuInSync());
	}

	@Test
	public void testTapDuringSmoothFitTurnsSyncOff() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue(app.getOsmandMap().getMapView().getMapRenderer() != null);
		assumeTrue("In landscape a tap on the map closes the track menu", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		zoomTwoLevelsIntoFittedTrack();
		PointF tap = getMapTouchPointBesideTrack();
		boolean inSyncAfterTap;
		float startZoom = getRendererZoom();
		OsmandPreference<Boolean> noAnimations = app.getSettings().DO_NOT_USE_ANIMATIONS;
		boolean noAnimationsBefore = noAnimations.get();
		try {
			noAnimations.set(false);
			scenarioRule.getScenario().onActivity(activity ->
					findShownView(activity.getWindow().getDecorView(), R.id.map_center_on_track).performClick());
			long end = SystemClock.uptimeMillis() + 1000;
			while (getRendererZoom() > startZoom - 0.2f && SystemClock.uptimeMillis() < end) {
				SystemClock.sleep(10);
			}
			tapOnScreen(tap.x, tap.y);
			inSyncAfterTap = isTrackMenuInSync();
			SystemClock.sleep(1500);
			awaitTrackMenuSettled();
		} finally {
			noAnimations.set(noAnimationsBefore);
		}
		// The stopped startMoving completes its zoom at once, so the confirmed single tap may then land on the track
		// line and open its menu again, in sync. The sync is checked right after the tap, before that.
		assertFalse("A tap that stops the fit on the way has to turn the sync off", inSyncAfterTap);
	}

	private static void pinchOnScreen(float x, float y) {
		UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		long downTime = SystemClock.uptimeMillis();
		int pointerDown = MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
		int pointerUp = MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
		injectPinchEvent(uiAutomation, downTime, 0, MotionEvent.ACTION_DOWN, x, y, 50f, 1);
		injectPinchEvent(uiAutomation, downTime, 20, pointerDown, x, y, 50f, 2);
		for (int step = 1; step <= 5; step++) {
			injectPinchEvent(uiAutomation, downTime, 20 + step * 20, MotionEvent.ACTION_MOVE, x, y, 50f + step * 20f, 2);
		}
		injectPinchEvent(uiAutomation, downTime, 140, pointerUp, x, y, 150f, 2);
		injectPinchEvent(uiAutomation, downTime, 160, MotionEvent.ACTION_UP, x, y, 150f, 1);
	}

	private static void injectPinchEvent(@NonNull UiAutomation uiAutomation, long downTime, long offset, int action,
	                                     float x, float y, float spread, int pointerCount) {
		MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
		MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointerCount];
		for (int i = 0; i < pointerCount; i++) {
			properties[i] = new MotionEvent.PointerProperties();
			properties[i].id = i;
			properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
			coords[i] = new MotionEvent.PointerCoords();
			coords[i].x = i == 0 ? x - spread : x + spread;
			coords[i].y = y;
			coords[i].pressure = 1f;
			coords[i].size = 1f;
		}
		MotionEvent event = MotionEvent.obtain(downTime, downTime + offset, action, pointerCount, properties, coords,
				0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
		uiAutomation.injectInputEvent(event, true);
		event.recycle();
	}

	@Test
	public void testEmptyPlanRouteSyncTapKeepsMapLinkedToLocation() throws Throwable {
		skipAppStartDialogs(app);
		boolean linkedBefore = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
		try {
			scenarioRule.getScenario().onActivity(activity -> {
				MeasurementToolFragment.showInstance(activity.getSupportFragmentManager());
				app.getMapViewTrackingUtilities().setMapLinkedToLocation(true);
			});
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> getTopSyncButton(activity).performClick());
			awaitTrackMenuSettled();
			assertFalse("A tap without a track must not turn the sync on", isTopSyncButtonSelected());
			assertTrue("A tap without a track must keep the map linked to the location",
					app.getMapViewTrackingUtilities().isMapLinkedToLocation());
		} finally {
			scenarioRule.getScenario().onActivity(activity -> app.getMapViewTrackingUtilities().setMapLinkedToLocation(linkedBefore));
		}
	}

	@Test
	public void testTallTrackRefittedAboveOverviewAfterMenuMoveAndReturn() throws Throwable {
		skipAppStartDialogs(app);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());
		GpxFile gpxFile = createTallTrack();
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), TALL_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile defaultTrack = selectedGpxFile;
		selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null, null, null, new Bundle()));
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> fragment.openMenuScreen(MenuState.HEADER_ONLY, true));
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> assertEquals("Menu state", MenuState.HEADER_ONLY, fragment.getCurrentMenuState()));

			scenarioRule.getScenario().onActivity(this::openAppearance);
			awaitTrackMenuSettled();
			Espresso.pressBack();
			awaitTrackMenuSettled();
			assertTrue("The track menu has to stay in sync after the return", isTrackMenuInSync());
			assertRectFittedAboveMenu("Tall track after the return to the Overview", gpxFile.getRect());
		} finally {
			selectedGpxFile = defaultTrack;
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	@Test
	public void testGpsFilterSyncButton() throws Throwable {
		assertSyncButtonOnScreen("gpsfilter", this::openGpsFilter, true, true);
	}

	@Test
	public void testAppearanceSyncButton() throws Throwable {
		assertSyncButtonOnScreen("appearance", this::openAppearance, true, true);
	}

	@Test
	public void testPlanRouteSyncButton() throws Throwable {
		assertSyncButtonOnScreen("planroute", this::openPlanRoute, false, false);
	}

	@Test
	public void testAnalyzeOnMapSyncButton() throws Throwable {
		assertSyncButtonOnScreen("analyze", this::openAnalyze, true, true);
	}

	@Test
	public void testGpsFilterKeepsSyncOffFromTrackMenu() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		zoomIntoTrack();
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		LatLon target = getMapTarget();
		float zoom = app.getOsmandMap().getMapView().getZoom();

		scenarioRule.getScenario().onActivity(this::openGpsFilter);
		awaitTrackMenuSettled();

		assertFalse("GPS filter has to start with the sync off", isTopSyncButtonSelected());
		assertTrue("GPS filter must not move the map while the sync is off", MapUtils.getDistance(target, getMapTarget()) < 1);
		assertEquals("GPS filter must not zoom while the sync is off", zoom, app.getOsmandMap().getMapView().getZoom(), 0.01f);
	}

	@Test
	public void testPlanRouteGpsFilterShowsOneSyncButton() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(this::openPlanRoute);
		awaitTrackMenuSettled();
		assertEquals("Plan route shows one sync button", 1, countShownSyncButtons());

		scenarioRule.getScenario().onActivity(activity -> {
			Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(MeasurementToolFragment.TAG);
			((MeasurementToolFragment) fragment).gpsFilterOnClick();
		});
		awaitTrackMenuSettled();
		assertEquals("GPS filter over Plan route shows one sync button", 1, countShownSyncButtons());
	}

	@Test
	public void testGpsFilterSyncButtonInLandscape() throws Throwable {
		assertSyncButtonOnScreenInLandscape("gpsfilter", this::openGpsFilter, true, true);
	}

	@Test
	public void testAppearanceSyncButtonInLandscape() throws Throwable {
		assertSyncButtonOnScreenInLandscape("appearance", this::openAppearance, true, true);
	}

	@Test
	public void testPlanRouteSyncButtonInLandscape() throws Throwable {
		assertSyncButtonOnScreenInLandscape("planroute", this::openPlanRoute, false, false);
	}

	@Test
	public void testAnalyzeOnMapSyncButtonInLandscape() throws Throwable {
		assertSyncButtonOnScreenInLandscape("analyze", this::openAnalyze, true, true);
	}

	@Test
	public void testReturnFromGpsFilterKeepsSyncState() throws Throwable {
		assertReturnKeepsSyncState(this::openGpsFilter);
	}

	@Test
	public void testReturnFromAppearanceKeepsSyncState() throws Throwable {
		assertReturnKeepsSyncState(this::openAppearance);
	}

	@Test
	public void testReturnFromAnalyzeKeepsSyncState() throws Throwable {
		assertReturnKeepsSyncState(this::openAnalyze);
	}

	private void assertReturnKeepsSyncState(@NonNull Consumer<MapActivity> open) {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null, null, null, new Bundle()));
		awaitTrackMenuSettled();

		scenarioRule.getScenario().onActivity(open::accept);
		awaitTrackMenuSettled();
		zoomIntoTrack();
		Espresso.pressBack();
		awaitTrackMenuSettled();
		assertTrue("The track menu has to stay in sync after the return", isTrackMenuInSync());
		int[] state = new int[1];
		onTrackMenu(fragment -> state[0] = fragment.getCurrentMenuState());
		assertTrackFittedAboveMenu(state[0]);

		onTrackMenu(fragment -> fragment.setMapInSync(false));
		scenarioRule.getScenario().onActivity(open::accept);
		awaitTrackMenuSettled();
		zoomIntoTrack();
		LatLon target = getMapTarget();
		Espresso.pressBack();
		awaitTrackMenuSettled();
		assertFalse("The track menu has to stay out of sync after the return", isTrackMenuInSync());
		assertEquals("The return must not zoom while the sync is off", ZOOM_INTO_TRACK,
				app.getOsmandMap().getMapView().getZoom(), 0.01f);
		double moved = MapUtils.getDistance(target, getMapTarget());
		assertTrue("The return moved the map by " + moved + " m while the sync is off", moved < 1);
	}

	private boolean isTrackMenuInSync() {
		boolean[] inSync = new boolean[1];
		onTrackMenu(fragment -> inSync[0] = fragment.isMapInSync());
		return inSync[0];
	}

	@Test
	public void testAnalyzeChartMovesMapOnlyInSync() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		GpxFile gpxFile = createTallTrack();
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), TALL_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile defaultTrack = selectedGpxFile;
		selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		OsmandPreference<GPXDataSetAxisType> xAxis = app.getSettings().TRACK_CHART_X_AXIS;
		GPXDataSetAxisType xAxisBefore = xAxis.get();
		xAxis.set(GPXDataSetAxisType.DISTANCE);
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			clickViewWithId(R.id.action_track);
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(this::openAnalyze);
			awaitTrackMenuSettled();
			awaitAnalyzeChartLaidOut();
			zoomIntoTrack();

			float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
			float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
			dragOnScreen(getMapTouchX(width), height * 0.2f, height * 0.3f);
			awaitTrackMenuSettled();
			assertFalse("A manual move has to turn the sync off", isTopSyncButtonSelected());
			LatLon target = getMapTarget();
			for (boolean chartEnd : new boolean[] {true, false}) {
				tapAnalyzeChart(chartEnd);
				awaitTrackMenuSettled();
				double moved = MapUtils.getDistance(target, getMapTarget());
				assertTrue("A chart point moved the map by " + moved + " m while the sync is off", moved < 1);
			}

			scenarioRule.getScenario().onActivity(activity -> getTopSyncButton(activity).performClick());
			awaitTrackMenuSettled();
			zoomIntoTrack();
			assertTrue("A programmatic zoom must keep the sync on", isTopSyncButtonSelected());
			double maxMoved = 0;
			for (boolean chartEnd : new boolean[] {true, false}) {
				LatLon before = getMapTarget();
				tapAnalyzeChart(chartEnd);
				awaitTrackMenuSettled();
				maxMoved = Math.max(maxMoved, MapUtils.getDistance(before, getMapTarget()));
				LatLon point = getTrackEnd(gpxFile, chartEnd);
				RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
				float x = tileBox.getPixXFromLatLon(point.getLatitude(), point.getLongitude());
				float y = tileBox.getPixYFromLatLon(point.getLatitude(), point.getLongitude());
				assertTrue("The chart point picked in sync is at " + x + ", " + y + " px, off the map",
						x >= 0 && x <= tileBox.getPixWidth() && y >= 0 && y <= tileBox.getPixHeight());
			}
			assertTrue("Picking the ends of the chart in sync has to move the map, moved " + maxMoved + " m", maxMoved > 1);
		} finally {
			xAxis.set(xAxisBefore);
			selectedGpxFile = defaultTrack;
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	@NonNull
	private static LatLon getTrackEnd(@NonNull GpxFile gpxFile, boolean end) {
		List<WptPt> points = gpxFile.getTracks().get(0).getSegments().get(0).getPoints();
		WptPt point = points.get(end ? points.size() - 1 : 0);
		return new LatLon(point.getLatitude(), point.getLongitude());
	}

	private void awaitAnalyzeChartLaidOut() {
		float[] range = new float[2];
		for (int i = 0; i < 50; i++) {
			scenarioRule.getScenario().onActivity(activity -> {
				Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TrackDetailsMenuFragment.TAG);
				Chart<?> chart = fragment != null && fragment.getView() != null ? findShownChart(fragment.getView()) : null;
				range[0] = chart != null ? chart.getXChartMin() : 0;
				range[1] = chart != null ? chart.getXChartMax() : 0;
			});
			if (range[1] > range[0] + 0.01f) {
				return;
			}
			SystemClock.sleep(100);
		}
		throw new AssertionError("The chart of Analyze on map has no distance range: " + range[0] + ".." + range[1]);
	}

	private void tapAnalyzeChart(boolean chartEnd) {
		scenarioRule.getScenario().onActivity(activity -> {
			Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TrackDetailsMenuFragment.TAG);
			assertNotNull("Analyze on map is not shown", fragment);
			Chart<?> chart = findShownChart(fragment.requireView());
			assertNotNull("Analyze on map shows no chart", chart);
			tapChart(chart, chartEnd);
		});
		SystemClock.sleep(ViewConfiguration.getDoubleTapTimeout());
	}

	@Test
	public void testTrackTabChartPointKeepsMapWhenSyncOff() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		clickViewWithId(R.id.action_track);
		awaitTrackMenuSettled();
		zoomIntoTrack();
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		LatLon target = getMapTarget();

		TrkSegment segment = selectedGpxFile.getGpxFile().getTracks().get(0).getSegments().get(0);
		List<WptPt> points = segment.getPoints();
		for (WptPt point : new WptPt[] {points.get(0), points.get(points.size() - 1)}) {
			onTrackMenu(fragment -> fragment.onChartPointSelected(segment, point.getLat(), point.getLon()));
			awaitTrackMenuSettled();
			double moved = MapUtils.getDistance(target, getMapTarget());
			assertTrue("A chart point moved the map by " + moved + " m while the sync is off", moved < 1);
		}
	}

	@Test
	public void testCenterOnWaypointTurnsSyncOff() throws Throwable {
		skipAppStartDialogs(app);
		GpxFile gpxFile = createTallTrack();
		WptPt waypoint = new WptPt(50.1855, 18.452);
		waypoint.setName("Waypoint");
		gpxFile.addPoint(waypoint);
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), TALL_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile track = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, track, null));
			awaitTrackMenuSettled();
			onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(isSelected()));
			clickViewWithId(R.id.action_points);
			awaitTrackMenuSettled();

			scenarioRule.getScenario().onActivity(activity -> {
				View menuView = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG).requireView();
				View icon = findShownView(menuView, R.id.go_to_point_location_icon);
				assertNotNull("The Points tab shows no waypoint", icon);
				icon.performClick();
			});
			awaitTrackMenuSettled();
			onView(allOf(withId(R.id.map_center_on_track), isDisplayed())).check(matches(not(isSelected())));
			double distance = MapUtils.getDistance(getMapTarget(), new LatLon(waypoint.getLat(), waypoint.getLon()));
			assertTrue("The map center is " + distance + " m from the waypoint", distance < 5);
		} finally {
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	@Test
	public void testFollowTrackShowsNoSyncButtonAndKeepsGraphInSync() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.FOLLOW_TRACK_MODE));
		awaitTrackMenuSettled();

		assertEquals("Follow track shows no sync button", 0, countShownSyncButtons());
		boolean[] inSync = new boolean[1];
		scenarioRule.getScenario().onActivity(activity ->
				inSync[0] = activity.getFragmentsHelper().getMeasurementToolFragment().isMapInSync());
		assertTrue("The graph of Follow track has to move the map", inSync[0]);
	}

	@Test
	public void testPlanRouteClearAllTurnsSyncOff() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.PLAN_ROUTE_MODE));
		awaitTrackMenuSettled();
		boolean[] inSync = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> {
			MeasurementToolFragment fragment = activity.getFragmentsHelper().getMeasurementToolFragment();
			fragment.setMapInSync(true);
			fragment.clearAllOnClick();
			inSync[0] = fragment.isMapInSync();
		});
		awaitTrackMenuSettled();
		assertFalse("Clear all has to turn the sync off", inSync[0]);
	}

	@Test
	public void testHiddenTrackMenuKeepsMapOnRotation() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		int rotation = getRotation();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.PLAN_ROUTE_MODE));
			awaitTrackMenuSettled();
			zoomIntoTrack();

			rotateQuarterTurn(rotation);
			awaitTrackMenuSettled();
			assertEquals("The hidden track menu must not fit the track after the rotation", ZOOM_INTO_TRACK,
					app.getOsmandMap().getMapView().getZoom(), 0.01f);
			assertEquals("Plan route shows its sync button after the rotation", 1, countShownSyncButtons());
		} finally {
			setRotation(rotation);
		}
	}

	@Test
	public void testBackAfterRotationCollapsesTrackMenu() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("Back collapses the menu only in portrait", isPortrait());
		int rotation = getRotation();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			turnToLandscape(rotation);
			awaitTrackMenuSettled();
			setRotation(rotation);
			for (int i = 0; i < 50 && !isPortrait(); i++) {
				SystemClock.sleep(100);
			}
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> fragment.openMenuScreen(MenuState.HALF_SCREEN, true));
			awaitTrackMenuSettled();

			Espresso.pressBack();
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> assertEquals("Back after a rotation has to collapse the track menu",
					MenuState.HEADER_ONLY, fragment.getCurrentMenuState()));
		} finally {
			setRotation(rotation);
		}
	}

	@Test
	public void testBackAfterRotationReturnsFromGpsFilterToPlanRoute() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		int rotation = getRotation();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(this::openPlanRoute);
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity ->
					activity.getFragmentsHelper().getMeasurementToolFragment().gpsFilterOnClick());
			awaitTrackMenuSettled();

			rotateQuarterTurn(rotation);
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> assertNotNull("Plan route is gone after the rotation, fragments "
					+ activity.getSupportFragmentManager().getFragments(), activity.getFragmentsHelper().getMeasurementToolFragment()));
			assertEquals("GPS filter over Plan route shows one sync button after the rotation", 1, countShownSyncButtons());
			Espresso.pressBack();
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				assertNull("Back after the rotation left GPS filter open", manager.findFragmentByTag(GpsFilterFragment.TAG));
				Fragment planRoute = activity.getFragmentsHelper().getMeasurementToolFragment();
				assertTrue("Back after the rotation has to return to Plan route, fragments " + manager.getFragments(),
						planRoute != null && planRoute.isVisible());
			});
			assertEquals("Plan route shows its sync button again", 1, countShownSyncButtons());
			scenarioRule.getScenario().onActivity(activity -> {
				View hud = activity.findViewById(R.id.measurement_buttons);
				assertNotNull("No Plan route buttons, fragments " + activity.getSupportFragmentManager().getFragments(), hud);
				assertTrue("The sync button of Plan route is hidden", hud.findViewById(R.id.map_center_on_track).isShown());
			});
		} finally {
			setRotation(rotation);
		}
	}

	@Test
	public void testBackFromAddWaypointHidesTemporarilySelectedTrack() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assertBackFromAddWaypointHidesTemporarilySelectedTrack();
	}

	@Test
	public void testBackFromAddWaypointHidesTrackWhileSearchIsHidden() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				activity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW, false));
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(activity -> {
			QuickSearchDialogFragment search = activity.getFragmentsHelper().getQuickSearchDialogFragment();
			assertNotNull("Search is not open", search);
			search.hide();
		});
		awaitTrackMenuSettled();
		try {
			assertBackFromAddWaypointHidesTemporarilySelectedTrack();
		} finally {
			scenarioRule.getScenario().onActivity(activity -> activity.getFragmentsHelper().closeQuickSearch());
		}
	}

	private void assertBackFromAddWaypointHidesTemporarilySelectedTrack() throws Throwable {
		scenarioRule.getScenario().onActivity(activity -> TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.setTemporarySelected(true));
		try {
			clickViewWithId(R.id.action_points);
			awaitTrackMenuSettled();
			Espresso.onView(ViewMatchers.withId(R.id.add_waypoint_button)).perform(ViewActions.scrollTo(), ViewActions.click());
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity ->
					assertTrue("Add Waypoint mode is not on", activity.getMapLayers().getContextMenuLayer().isInAddGpxPointMode()));
			onTrackMenu(fragment -> assertTrue("The track menu is not hidden in Add Waypoint mode", fragment.isHidden()));
			String path = selectedGpxFile.getGpxFile().getPath();
			Espresso.pressBackUnconditionally();
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity ->
					assertFalse("Back did not quit Add Waypoint mode", activity.getMapLayers().getContextMenuLayer().isInAddGpxPointMode()));
			Fragment[] trackMenuCheck = new Fragment[1];
			scenarioRule.getScenario().onActivity(activity ->
					trackMenuCheck[0] = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG));
			if (trackMenuCheck[0] != null) {
				Espresso.pressBackUnconditionally();
				awaitTrackMenuSettled();
			}
			scenarioRule.getScenario().onActivity(activity -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				assertNull("Back did not close the track menu, fragments " + manager.getFragments(),
						manager.findFragmentByTag(TrackMenuFragment.TAG));
				assertNull("Back did not hide the track that was not shown on the map",
						app.getSelectedGpxHelper().getSelectedFileByPath(path));
			});
		} finally {
			scenarioRule.getScenario().onActivity(activity -> {
				if (activity.getMapLayers().getContextMenuLayer().isInAddGpxPointMode()) {
					activity.getMapLayers().getContextMenuLayer().getAddGpxPointBottomSheetHelper().hide();
					activity.getMapLayers().getContextMenuLayer().quitAddGpxPoint();
				}
			});
		}
	}

	@Test
	public void testCancelledSaveFromExitDialogKeepsTrackMenuHidden() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean mapLinkedToLocation = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
		scenarioRule.getScenario().onActivity(activity -> TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.PLAN_ROUTE_MODE));
		awaitTrackMenuSettled();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					assertNotNull("Plan route is not open", activity.getFragmentsHelper().getMeasurementToolFragment()));
			onTrackMenu(fragment -> assertTrue("Plan route did not hide the track menu", fragment.isHidden()));
			scenarioRule.getScenario().onActivity(activity ->
					activity.getFragmentsHelper().getMeasurementToolFragment().onActivityResult(
							ExitBottomSheetDialogFragment.REQUEST_CODE, ExitBottomSheetDialogFragment.SAVE_RESULT_CODE, null));
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> {
				Fragment saveSheet = activity.getSupportFragmentManager()
						.findFragmentByTag(SaveAsNewTrackBottomSheetDialogFragment.TAG);
				if (saveSheet instanceof DialogFragment) {
					((DialogFragment) saveSheet).dismiss();
				}
			});
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity ->
					activity.getFragmentsHelper().getMeasurementToolFragment().directionsOnClick());
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity ->
					assertNull("Navigation did not close Plan route", activity.getFragmentsHelper().getMeasurementToolFragment()));
			onTrackMenu(fragment -> assertTrue("The cancelled save showed the hidden track menu when Plan route closed", fragment.isHidden()));
		} finally {
			scenarioRule.getScenario().onActivity(activity -> {
				Fragment trackMenu = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG);
				if (trackMenu instanceof TrackMenuFragment) {
					((TrackMenuFragment) trackMenu).dismiss();
				}
			});
			stopNavigationAlongTrack(appMode, mapLinkedToLocation);
		}
	}

	@Test
	public void testSaveFromExitDialogShowsHiddenTrackMenu() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		File savedFile = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), "track_menu_exit_save_test" + IndexConstants.GPX_FILE_EXT);
		scenarioRule.getScenario().onActivity(activity -> TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.PLAN_ROUTE_MODE));
		awaitTrackMenuSettled();
		try {
			scenarioRule.getScenario().onActivity(activity -> {
				MeasurementToolFragment planRoute = activity.getFragmentsHelper().getMeasurementToolFragment();
				assertNotNull("Plan route is not open", planRoute);
				planRoute.onActivityResult(ExitBottomSheetDialogFragment.REQUEST_CODE, ExitBottomSheetDialogFragment.SAVE_RESULT_CODE, null);
			});
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> {
				Fragment saveSheet = activity.getSupportFragmentManager()
						.findFragmentByTag(SaveAsNewTrackBottomSheetDialogFragment.TAG);
				assertTrue("Save in the exit sheet did not open the save sheet", saveSheet instanceof DialogFragment);
				((DialogFragment) saveSheet).dismiss();
				activity.getFragmentsHelper().getMeasurementToolFragment().onSaveAsNewTrack("", "track_menu_exit_save_test", false, false);
			});
			boolean[] planRouteOpen = {true};
			for (int i = 0; i < 100 && planRouteOpen[0]; i++) {
				SystemClock.sleep(100);
				scenarioRule.getScenario().onActivity(activity ->
						planRouteOpen[0] = activity.getFragmentsHelper().getMeasurementToolFragment() != null);
			}
			assertFalse("The save did not close Plan route", planRouteOpen[0]);
			awaitTrackMenuSettled();
			onTrackMenu(fragment -> assertFalse("The save chosen in the exit sheet did not show the hidden track menu", fragment.isHidden()));
		} finally {
			scenarioRule.getScenario().onActivity(activity -> {
				Fragment trackMenu = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG);
				if (trackMenu instanceof TrackMenuFragment) {
					((TrackMenuFragment) trackMenu).dismiss();
				}
			});
			if (savedFile.exists()) {
				FileUtils.removeGpxFile(app, savedFile);
			}
		}
	}

	@Test
	public void testBackAfterPlanRouteDoneHidesTemporarilySelectedTrack() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity -> TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.setTemporarySelected(true));
		onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.PLAN_ROUTE_MODE));
		awaitTrackMenuSettled();
		String path = selectedGpxFile.getGpxFile().getPath();
		boolean[] doneShown = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> doneShown[0] = activity.findViewById(R.id.widget_top_bar_save).isShown());
		assumeTrue("Plan route shows its Done button in the top toolbar", doneShown[0]);
		clickViewWithId(R.id.widget_top_bar_save);
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(activity ->
				assertNull("Done did not close Plan route", activity.getFragmentsHelper().getMeasurementToolFragment()));
		onTrackMenu(fragment -> assertTrue("Done showed the hidden track menu", fragment.isHidden()));
		Espresso.pressBackUnconditionally();
		awaitTrackMenuSettled();
		Fragment[] trackMenuCheck = new Fragment[1];
		scenarioRule.getScenario().onActivity(activity ->
				trackMenuCheck[0] = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG));
		if (trackMenuCheck[0] != null) {
			Espresso.pressBackUnconditionally();
			awaitTrackMenuSettled();
		}
		scenarioRule.getScenario().onActivity(activity -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			assertNull("Back did not close the track menu, fragments " + manager.getFragments(),
					manager.findFragmentByTag(TrackMenuFragment.TAG));
			assertNull("Back did not hide the track that was not shown on the map",
					app.getSelectedGpxHelper().getSelectedFileByPath(path));
		});
	}

	@Test
	public void testBackInPlanRouteAfterGpsFilterClosesPlanRoute() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity -> TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		onTrackMenu(fragment -> fragment.openPlanRoute(0, MeasurementToolFragment.PLAN_ROUTE_MODE));
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(activity ->
				activity.getFragmentsHelper().getMeasurementToolFragment().gpsFilterOnClick());
		awaitTrackMenuSettled();
		Espresso.pressBackUnconditionally();
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(activity -> {
			assertNull("Back did not close GPS filter", activity.getSupportFragmentManager().findFragmentByTag(GpsFilterFragment.TAG));
			assertNotNull("Back from GPS filter closed Plan route", activity.getFragmentsHelper().getMeasurementToolFragment());
		});
		boolean[] planRouteOpen = {true};
		for (int i = 0; i < 2 && planRouteOpen[0]; i++) {
			Espresso.pressBackUnconditionally();
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity ->
					planRouteOpen[0] = activity.getFragmentsHelper().getMeasurementToolFragment() != null);
		}
		assertFalse("Back in Plan route after GPS filter did not close Plan route", planRouteOpen[0]);
		onTrackMenu(fragment -> assertFalse("Plan route closed without showing the hidden track menu", fragment.isHidden()));
	}

	@Test
	public void testBackAfterRotationReturnsFromGpsFilterToTrackMenu() throws Throwable {
		assertBackAfterRotationReturnsToTrackMenu(this::openGpsFilter, GpsFilterFragment.TAG);
	}

	@Test
	public void testBackAfterRotationReturnsFromAppearanceToTrackMenu() throws Throwable {
		assertBackAfterRotationReturnsToTrackMenu(this::openAppearance, TrackAppearanceFragment.TAG);
	}

	private void assertBackAfterRotationReturnsToTrackMenu(@NonNull Consumer<MapActivity> open, @NonNull String tag) {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		int rotation = getRotation();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(open::accept);
			awaitTrackMenuSettled();

			rotateQuarterTurn(rotation);
			awaitTrackMenuSettled();
			ContextMenuFragment[] opened = new ContextMenuFragment[1];
			scenarioRule.getScenario().onActivity(activity ->
					opened[0] = (ContextMenuFragment) activity.getSupportFragmentManager().findFragmentByTag(tag));
			assertNotNull(tag + " is gone after the rotation", opened[0]);
			Espresso.pressBack();
			awaitTrackMenuSettled();
			scenarioRule.getScenario().onActivity(activity -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				assertNull("Back after the rotation left " + tag + " open", manager.findFragmentByTag(tag));
				assertTrue("Back after the rotation bypassed " + tag, opened[0].isDismissing());
				Fragment menu = manager.findFragmentByTag(TrackMenuFragment.TAG);
				assertTrue("Back after the rotation has to return to the track menu, fragments " + manager.getFragments(),
						menu != null && menu.isVisible());
			});
		} finally {
			setRotation(rotation);
		}
	}

	@Test
	public void testPlanRouteTrackFittedAbovePointsPanel() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		assumeTrue("The points panel is below the map only in portrait", isPortrait());
		assertPlanRouteTrackFittedWithPointsPanel();
	}

	@Test
	public void testPlanRouteTrackFittedBesidePointsPanelInLandscape() throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		int rotation = getRotation();
		try {
			turnToLandscape(rotation);
			assertPlanRouteTrackFittedWithPointsPanel();
		} finally {
			setRotation(rotation);
		}
	}

	private void assertPlanRouteTrackFittedWithPointsPanel() {
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(this::openPlanRoute);
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(activity -> activity.getFragmentsHelper().getMeasurementToolFragment()
				.requireView().findViewById(R.id.up_down_row).performClick());
		awaitTrackMenuSettled();
		zoomIntoTrack();
		scenarioRule.getScenario().onActivity(activity -> getTopSyncButton(activity).performClick());
		awaitTrackMenuSettled();

		Rect part = getMapPartBesidePlanRoutePanels();
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
		float left = tileBox.getPixXFromLatLon(rect.centerY(), rect.getLeft());
		float right = tileBox.getPixXFromLatLon(rect.centerY(), rect.getRight());
		float top = tileBox.getPixYFromLatLon(rect.getTop(), rect.centerX());
		float bottom = tileBox.getPixYFromLatLon(rect.getBottom(), rect.centerX());
		assertTrue("The track is at " + left + ".." + right + " x " + top + ".." + bottom
						+ " px, the map beside the Plan route panels " + part.toShortString(),
				left >= part.left - 2 && right <= part.right + 2 && top >= part.top - 2 && bottom <= part.bottom + 2);
	}

	@NonNull
	private Rect getMapPartBesidePlanRoutePanels() {
		Rect[] part = new Rect[1];
		scenarioRule.getScenario().onActivity(activity -> {
			OsmandMapTileView mapView = activity.getMapView();
			RotatedTileBox tileBox = mapView.getRotatedTileBox();
			int[] mapLocation = AndroidUtils.getLocationOnScreen(mapView.getView());
			Rect free = new Rect(0, 0, tileBox.getPixWidth(), tileBox.getPixHeight());
			View fragmentView = activity.getFragmentsHelper().getMeasurementToolFragment().requireView();
			Rect content = AndroidUtils.getViewBoundOnScreen(fragmentView.findViewById(R.id.main_content));
			free.bottom = Math.min(free.bottom, content.top - mapLocation[1]);
			View topBar = activity.findViewById(R.id.widget_top_bar);
			if (topBar != null && topBar.isShown()) {
				free.top = Math.max(free.top, AndroidUtils.getViewBoundOnScreen(topBar).bottom - mapLocation[1]);
			}
			boolean portrait = AndroidUiHelper.isOrientationPortrait(activity);
			View panel = portrait ? fragmentView.findViewById(R.id.cards_container) : activity.findViewById(R.id.left_side_menu);
			assertTrue("The points panel is not open", panel != null && panel.isShown());
			if (!portrait) {
				Rect panelRect = AndroidUtils.getViewBoundOnScreen(panel);
				panelRect.offset(-mapLocation[0], -mapLocation[1]);
				if (panelRect.centerX() < free.centerX()) {
					free.left = Math.max(free.left, panelRect.right);
				} else {
					free.right = Math.min(free.right, panelRect.left);
				}
			}
			part[0] = free;
		});
		return part[0];
	}

	@Test
	public void testOverviewMenuOnScreenAndTrackFittedAfterWindowResize() throws Throwable {
		skipAppStartDialogs(app);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());
		GpxFile gpxFile = createTallTrack();
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), TALL_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile tallTrack = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		Point size = new Point();
		scenarioRule.getScenario().onActivity(activity -> activity.getWindowManager().getDefaultDisplay().getRealSize(size));
		boolean[] resized = new boolean[1];
		String restoreSizeCommand = getSizeOverrideCommand();
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, tallTrack, null, null, null, new Bundle()));
			awaitTrackMenuSettled();
			assertOverviewOnScreen("before the resize");

			int mapHeight = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
			runShellCommand("wm size " + size.x + "x" + size.y * 4 / 5);
			resized[0] = true;
			awaitMapHeightChanged(mapHeight);
			awaitTrackMenuSettled();
			assertOverviewOnScreen("after the resize");
			assertRectFittedAboveMenu("Tall track after the resize", gpxFile.getRect());
		} finally {
			if (resized[0]) {
				runShellCommand(restoreSizeCommand);
				awaitDisplayHeight(size.y);
			}
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	private void assertOverviewOnScreen(@NonNull String when) {
		scenarioRule.getScenario().onActivity(activity -> {
			View menuView = activity.getSupportFragmentManager().findFragmentByTag(TrackMenuFragment.TAG).requireView();
			View overview = findShownView(menuView, R.id.recycler_overview);
			assertNotNull("The Overview tab is not shown " + when, overview);
			Rect overviewRect = AndroidUtils.getViewBoundOnScreen(overview);
			Rect windowRect = AndroidUtils.getViewBoundOnScreen(activity.getWindow().getDecorView());
			assertTrue("The Overview " + overviewRect.toShortString() + " goes below the window "
					+ windowRect.toShortString() + " " + when, overviewRect.bottom <= windowRect.bottom);
		});
	}

	private void awaitDisplayHeight(int height) {
		Point current = new Point();
		for (int i = 0; i < 100; i++) {
			scenarioRule.getScenario().onActivity(activity -> activity.getWindowManager().getDefaultDisplay().getRealSize(current));
			if (current.y == height) {
				break;
			}
			SystemClock.sleep(100);
		}
	}

	private void awaitMapHeightChanged(int mapHeight) {
		for (int i = 0; i < 100 && app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight() == mapHeight; i++) {
			SystemClock.sleep(100);
		}
		assertTrue("The map height did not change", app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight() != mapHeight);
	}

	@NonNull
	private static String runShellCommand(@NonNull String command) {
		UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))) {
			return Algorithms.readFromInputStream(stream).toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@NonNull
	private static String getSizeOverrideCommand() {
		for (String line : runShellCommand("wm size").split("\n")) {
			if (line.startsWith("Override size:")) {
				return "wm size " + line.substring("Override size:".length()).trim();
			}
		}
		return "wm size reset";
	}

	private void openGpsFilter(@NonNull MapActivity activity) {
		TrackMenuFragment menu = activity.getFragmentsHelper().getTrackMenuFragment();
		GpsFilterFragment.showInstance(activity.getSupportFragmentManager(), selectedGpxFile, menu);
	}

	private void openAppearance(@NonNull MapActivity activity) {
		TrackMenuFragment menu = activity.getFragmentsHelper().getTrackMenuFragment();
		TrackAppearanceFragment.showInstance(activity, selectedGpxFile, menu);
	}

	private void openPlanRoute(@NonNull MapActivity activity) {
		activity.getFragmentsHelper().getTrackMenuFragment().dismiss();
		MeasurementToolFragment.showInstance(activity.getSupportFragmentManager(),
				selectedGpxFile.getGpxFile().getPath(), false);
	}

	private void openAnalyze(@NonNull MapActivity activity) {
		TrackMenuFragment menu = activity.getFragmentsHelper().getTrackMenuFragment();
		OptionsCard optionsCard = new OptionsCard(activity, menu.getDisplayHelper(), selectedGpxFile);
		menu.onCardButtonPressed(optionsCard, OptionsCard.ANALYZE_ON_MAP_BUTTON_INDEX);
	}

	private void assertSyncButtonOnScreenInLandscape(@NonNull String name, @NonNull Consumer<MapActivity> open,
	                                                 boolean syncOnOpen, boolean checkOpenFit) throws Throwable {
		int rotation = getRotation();
		try {
			turnToLandscape(rotation);
			assertSyncButtonOnScreen(name + "-land", open, syncOnOpen, checkOpenFit);
		} finally {
			setRotation(rotation);
		}
	}

	private void assertSyncButtonOnScreen(@NonNull String name, @NonNull Consumer<MapActivity> open, boolean syncOnOpen,
	                                      boolean checkOpenFit) throws Throwable {
		skipAppStartDialogs(app);
		assertNotNull(selectedGpxFile);
		scenarioRule.getScenario().onActivity(activity ->
				TrackMenuFragment.showInstance(activity, selectedGpxFile, null));
		awaitTrackMenuSettled();
		scenarioRule.getScenario().onActivity(open::accept);
		awaitTrackMenuSettled();
		assertEquals("The sync button state on the opened screen", syncOnOpen, isTopSyncButtonSelected());
		assertSyncButtonAboveSystemBars();
		if (checkOpenFit && isPortrait()) {
			float[] fittedOnOpen = getTrackVerticalPx();
			float openHeight = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
			assertTrue("The track " + fittedOnOpen[0] + ".." + fittedOnOpen[1] + " has to be fitted above the panel when the screen opens",
					fittedOnOpen[0] >= 0 && fittedOnOpen[1] <= openHeight * 0.62f);
		}

		if (checkOpenFit && !isPortrait()) {
			assertTrackBesideTopPanel("when the screen opens");
		}

		float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
		dragOnScreen(getMapTouchX(width), height * 0.2f, height * 0.3f);
		awaitTrackMenuSettled();
		assertFalse("A manual move has to turn the sync off", isTopSyncButtonSelected());

		zoomIntoTrack();
		scenarioRule.getScenario().onActivity(activity -> getTopSyncButton(activity).performClick());
		awaitTrackMenuSettled();
		assertTrue("The sync button has to be selected after a tap", isTopSyncButtonSelected());
		if (checkOpenFit && !isPortrait()) {
			assertTrackBesideTopPanel("after the tap");
		}
		float[] fitted = getTrackVerticalPx();
		float zoomFitted = app.getOsmandMap().getMapView().getZoom();
		assertTrue("The tap has to fit the whole track, zoom " + zoomFitted, zoomFitted < ZOOM_INTO_TRACK);
		float screenHeight = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
		assertTrue("The track " + fitted[0] + ".." + fitted[1] + " has to be in the map part above the panel",
				!isPortrait() || fitted[0] >= 0 && fitted[1] <= screenHeight * 0.62f);

		dragOnScreen(isPortrait() ? width / 2 : isLayoutRtl() ? width * 0.05f : width * 0.95f, height * (isPortrait() ? 0.2f : 0.5f), height * (isPortrait() ? 0.3f : 0.6f));
		awaitTrackMenuSettled();
		assertFalse("A manual move has to turn the sync off", isTopSyncButtonSelected());
		scenarioRule.getScenario().onActivity(activity -> getTopSyncButton(activity).performClick());
		awaitTrackMenuSettled();
		float[] fittedAgain = getTrackVerticalPx();
		assertEquals("Zoom after the second tap", zoomFitted, app.getOsmandMap().getMapView().getZoom(), 0.5f);
		assertEquals("Track top after the second tap", fitted[0], fittedAgain[0], 40f);
		assertEquals("Track bottom after the second tap", fitted[1], fittedAgain[1], 40f);
	}

	private void assertTrackBesideTopPanel(@NonNull String when) {
		float[] fitted = getTrackHorizontalPx();
		int panelWidth = getTopPanelWidth();
		int mapWidth = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
		float left = isLayoutRtl() ? 0 : panelWidth - 2;
		float right = isLayoutRtl() ? mapWidth - panelWidth + 2 : mapWidth;
		assertTrue("The track " + fitted[0] + ".." + fitted[1] + " has to be fitted beside the panel of width "
				+ panelWidth + " " + when, fitted[0] >= left && fitted[1] <= right);
	}

	private void assertSyncButtonAboveSystemBars() {
		scenarioRule.getScenario().onActivity(activity -> {
			View button = getTopSyncButton(activity);
			WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(button);
			int bottomInset = insets != null ? insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom : 0;
			int freeBottom = activity.getWindow().getDecorView().getHeight() - bottomInset;
			Rect bounds = AndroidUtils.getViewBoundOnScreen(button);
			assertTrue("The sync button " + bounds.toShortString() + " is under the system bars from " + freeBottom + " px",
					bounds.bottom <= freeBottom);
		});
	}

	private boolean isTopSyncButtonSelected() {
		boolean[] selected = new boolean[1];
		scenarioRule.getScenario().onActivity(activity -> selected[0] = getTopSyncButton(activity).isSelected());
		return selected[0];
	}

	@NonNull
	private static View getTopSyncButton(@NonNull MapActivity activity) {
		List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
		for (int i = fragments.size() - 1; i >= 0; i--) {
			View root = fragments.get(i).getView();
			View button = root != null ? root.findViewById(R.id.map_center_on_track) : null;
			if (button != null && button.isShown()) {
				return button;
			}
		}
		for (int containerId : new int[] {R.id.track_map_sync_button, R.id.measurement_buttons}) {
			View container = activity.findViewById(containerId);
			View button = container != null ? container.findViewById(R.id.map_center_on_track) : null;
			if (button != null && button.isShown()) {
				return button;
			}
		}
		throw new AssertionError("No sync button on screen");
	}

	private int getTopPanelWidth() {
		int[] width = new int[1];
		scenarioRule.getScenario().onActivity(activity -> {
			List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
			for (int i = fragments.size() - 1; i >= 0 && width[0] == 0; i--) {
				Fragment fragment = fragments.get(i);
				if (fragment instanceof ContextMenuFragment && fragment.isVisible()) {
					width[0] = ((ContextMenuFragment) fragment).getWidth();
				} else if (fragment instanceof TrackDetailsMenuFragment && fragment.isVisible()) {
					width[0] = ((TrackDetailsMenuFragment) fragment).getWidth();
				}
			}
		});
		return width[0];
	}

	private int countShownSyncButtons() {
		int[] count = new int[1];
		scenarioRule.getScenario().onActivity(activity ->
				count[0] = countShownViews(activity.getWindow().getDecorView(), R.id.map_center_on_track));
		return count[0];
	}

	private static int countShownViews(@NonNull View view, int id) {
		int count = view.getId() == id && view.isShown() ? 1 : 0;
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				count += countShownViews(group.getChildAt(i), id);
			}
		}
		return count;
	}

	private float[] getTrackHorizontalPx() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
		double centerLat = (rect.getTop() + rect.getBottom()) / 2;
		return new float[] {tileBox.getPixXFromLatLon(centerLat, rect.getLeft()),
				tileBox.getPixXFromLatLon(centerLat, rect.getRight())};
	}

	private float[] getTrackVerticalPx() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		KQuadRect rect = selectedGpxFile.getGpxFile().getRect();
		return new float[] {tileBox.getPixYFromLatLon(rect.getTop(), rect.centerX()),
				tileBox.getPixYFromLatLon(rect.getBottom(), rect.centerX())};
	}

	/**
	 * A tap on the track line selects the track and replaces the track menu, so the tap goes to the point of the
	 * upper map part that is the farthest from the track line.
	 */
	@NonNull
	private PointF getMapTouchPointBesideTrack() {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		List<PointF> line = new ArrayList<>();
		for (WptPt point : selectedGpxFile.getGpxFile().getAllSegmentsPoints()) {
			line.add(new PointF(tileBox.getPixXFromLatLon(point.getLatitude(), point.getLongitude()),
					tileBox.getPixYFromLatLon(point.getLatitude(), point.getLongitude())));
		}
		float width = tileBox.getPixWidth();
		float height = tileBox.getPixHeight();
		PointF best = null;
		double bestDistance = -1;
		for (float x = width * 0.25f; x <= width * 0.75f; x += width * 0.05f) {
			for (float y = height * 0.15f; y <= height * 0.4f; y += height * 0.025f) {
				double distance = Double.MAX_VALUE;
				for (int i = 1; i < line.size(); i++) {
					PointF a = line.get(i - 1);
					PointF b = line.get(i);
					distance = Math.min(distance, getDistanceToSegmentPx(x, y, a, b));
				}
				if (distance > bestDistance) {
					bestDistance = distance;
					best = new PointF(x, y);
				}
			}
		}
		assumeTrue("No point of the upper map part is far enough from the track line", bestDistance > width * 0.08f);
		return best;
	}

	private static double getDistanceToSegmentPx(float x, float y, @NonNull PointF a, @NonNull PointF b) {
		float dx = b.x - a.x;
		float dy = b.y - a.y;
		float lengthSquared = dx * dx + dy * dy;
		float t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * dx + (y - a.y) * dy) / lengthSquared));
		return Math.hypot(x - (a.x + t * dx), y - (a.y + t * dy));
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

	/**
	 * Fits the track with the sync button, turns the sync off and zooms two levels in, so that the next fit
	 * changes the zoom by less than AnimateDraggingMapThread.ZOOM_DIFF_SKIP_ANIMATION and is animated.
	 */
	private float zoomTwoLevelsIntoFittedTrack() throws Throwable {
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		clickViewWithId(R.id.map_center_on_track);
		awaitTrackMenuSettled();
		float fittedZoom = getRendererZoom();
		onTrackMenu(fragment -> fragment.setMapInSync(false));
		scenarioRule.getScenario().onActivity(activity -> {
			OsmandMapTileView mapView = activity.getMapView();
			mapView.setIntZoom((int) fittedZoom + 2);
			mapView.refreshMap();
		});
		awaitTrackMenuSettled();
		return fittedZoom;
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

	@Test
	public void testTallTrackFittedAboveOverviewMenuWhenOpenedAndOnSyncTap() throws Throwable {
		skipAppStartDialogs(app);
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait());

		GpxFile gpxFile = createTallTrack();
		File file = new File(ImportHelper.getGpxDestinationDir(app, true), TALL_GPX_NAME);
		assertNull(SharedUtil.writeGpxFile(file, gpxFile));
		gpxFile.setPath(file.getAbsolutePath());
		SelectedGpxFile tallTrack = app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams());
		try {
			scenarioRule.getScenario().onActivity(activity ->
					TrackMenuFragment.showInstance(activity, tallTrack, null, null, null, new Bundle()));
			awaitTrackMenuSettled();
			assertRectFittedAboveMenu("Tall track when the menu opens", gpxFile.getRect());

			float height = app.getOsmandMap().getMapView().getRotatedTileBox().getPixHeight();
			float width = app.getOsmandMap().getMapView().getRotatedTileBox().getPixWidth();
			dragOnScreen(width / 2, height * 0.2f, height * 0.3f);
			awaitTrackMenuSettled();
			assertFalse("A manual move has to turn the sync off", isTopSyncButtonSelected());
			scenarioRule.getScenario().onActivity(activity -> getTopSyncButton(activity).performClick());
			awaitTrackMenuSettled();
			assertRectFittedAboveMenu("Tall track after the sync button tap", gpxFile.getRect());
		} finally {
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap());
			file.delete();
		}
	}

	private void assertRectFittedAboveMenu(@NonNull String message, @NonNull KQuadRect rect) {
		RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		float top = tileBox.getPixYFromLatLon(rect.getTop(), rect.centerX());
		float bottom = tileBox.getPixYFromLatLon(rect.getBottom(), rect.centerX());
		float menuTop = getMenuTopRatio() * tileBox.getPixHeight();
		assertTrue(message + ": the track is at " + top + ".." + bottom + " px, the menu at " + menuTop + " px",
				top >= 0 && bottom <= menuTop);
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
			assertTrue("Opening the Track tab has to keep the track centered", MapUtils.getDistance(target, getMapTarget()) < 1);
			scenarioRule.getScenario().onActivity(activity -> activity.getMapView().setIntZoom(ZOOM_INTO_TRACK));
			awaitTrackMenuSettled();
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
	private static Chart<?> findShownChart(@NonNull View view) {
		if (view instanceof Chart<?> chart && view.getId() == R.id.chart && !chart.isEmpty() && view.isShown()
				&& view.getGlobalVisibleRect(new Rect())) {
			return chart;
		}
		if (view instanceof ViewGroup group) {
			for (int i = 0; i < group.getChildCount(); i++) {
				Chart<?> chart = findShownChart(group.getChildAt(i));
				if (chart != null) {
					return chart;
				}
			}
		}
		return null;
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

	private boolean areScreensReady() {
		MapActivity activity = app.getOsmandMap().getMapView().getMapActivity();
		if (activity == null) {
			return false;
		}
		FragmentManager manager = activity.getSupportFragmentManager();
		if (manager.isStateSaved()) {
			return true;
		}
		for (Fragment fragment : manager.getFragments()) {
			View view = fragment.getView();
			if (fragment.isRemoving() || view != null && !fragment.isHidden()
					&& (!view.isLaidOut() || view.isLayoutRequested())) {
				return false;
			}
		}
		return true;
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
				if (menuTop == lastMenuTop && !app.getOsmandMap().getMapView().getAnimatedDraggingThread().isAnimating()
						&& areScreensReady()) {
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
