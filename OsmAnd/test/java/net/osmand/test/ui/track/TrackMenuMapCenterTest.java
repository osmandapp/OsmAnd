package net.osmand.test.ui.track;

import static net.osmand.test.common.OsmAndDialogInteractions.clickViewWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * The map center has to be moved into the visible part of the map as soon as the track menu
 * covers it, without waiting for the app to be restored from the background
 * (https://github.com/osmandapp/OsmAnd/issues/25590).
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class TrackMenuMapCenterTest extends AndroidTest {

	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";
	private static final float RATIO_TOLERANCE = 0.05f;

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
	 * Waits until the track menu stops moving, so that the checked map ratio is the settled one.
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
				if (menuTop == lastMenuTop) {
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
