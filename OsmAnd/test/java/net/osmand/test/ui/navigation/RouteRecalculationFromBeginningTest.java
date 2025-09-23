package net.osmand.test.ui.navigation;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static net.osmand.test.common.Interactions.openNavigationMenu;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.os.Handler;
import android.os.Looper;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;
import net.osmand.util.Algorithms;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RouteRecalculationFromBeginningTest extends AndroidTest {

	private static final int SPEED_KM_PER_HOUR = 500;
	private static final String SELECTED_GPX_NAME = "gpx_recalc_test.gpx";
	private static final LatLon START = new LatLon(50.17356, 18.51406);

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private ObserveDistToFinishIdlingResource idlingResource;

	private GpxFile gpxFile;

	@Before
	@Override
	public void setup() {
		super.setup();
		enableSimulation(SPEED_KM_PER_HOUR);
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME), new SaveImportedGpxListener() {
				@Override
				public void onGpxSaved(@Nullable String error, @NonNull GpxFile gpxFile) {
					if (Algorithms.isEmpty(error) && gpxFile.getError() == null) {
						RouteRecalculationFromBeginningTest.this.gpxFile = gpxFile;

						KQuadRect rect = gpxFile.getRect();
						if (rect.getLeft() != 0 && rect.getRight() != 0) {
							app.getOsmandMap().getMapView().fitRectToMap(rect.getLeft(), rect.getRight(),
									rect.getTop(), rect.getBottom(), (int) rect.width(), (int) rect.height(), 0);
						}
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void cleanUp() {
		super.cleanUp();

		if (idlingResource != null) {
			unregisterIdlingResources(idlingResource);
		}
		app.stopNavigation();
	}

	@Test
	public void test() throws Throwable {
		skipAppStartDialogs(app);

		openNavigationMenu();

		app.getOsmandMap().getMapActions().setGPXRouteParams(gpxFile);
		app.getTargetPointsHelper().setStartPoint(START, true, null);

		app.getOsmandMap().getMapActions().startNavigation();

		idlingResource = new ObserveDistToFinishIdlingResource(app);
		registerIdlingResources(idlingResource);

		Espresso.onIdle();
	}

	private static class ObserveDistToFinishIdlingResource extends BaseIdlingResource {

		private static final int CHECK_INTERVAL = 1000;
		private static final Range<Integer> EXPECTED_ROUTE_LENGTH = new Range<>(7000, 7200);
		private static final int IDLE_ON_LEFT_DISTANCE = 5900;
		private static final int START_POINT_DEVIATION_THRESHOLD = 50;

		private final Handler handler;

		private boolean idle = false;

		public ObserveDistToFinishIdlingResource(@NonNull OsmandApplication app) {
			super(app);
			this.handler = new Handler(Looper.getMainLooper());

			Runnable checkLeftDistanceTask = new Runnable() {

				private int initialLeftDistance = -1;

				@Override
				public void run() throws AssertionError, IllegalStateException {
					int leftDistance = app.getRoutingHelper().getLeftDistance();
					if (leftDistance == 0) {
						// Route is not calculated yet
						handler.postDelayed(this, CHECK_INTERVAL);
						return;
					}

					if (initialLeftDistance == -1) {
						initialLeftDistance = leftDistance;
						if (!EXPECTED_ROUTE_LENGTH.contains(initialLeftDistance)) {
							throw new IllegalStateException("Unexpected route calculated with distance " + initialLeftDistance + " m");
						}
					}

					if (leftDistance > initialLeftDistance + START_POINT_DEVIATION_THRESHOLD) {
						throw new AssertionError("Route recalculated from start of the track with distance " + leftDistance + " m");
					} else {
						if (leftDistance > IDLE_ON_LEFT_DISTANCE) {
							handler.postDelayed(this, CHECK_INTERVAL);
						} else {
							idle = true;
							notifyIdleTransition();
						}
					}
				}
			};
			handler.post(checkLeftDistanceTask);
		}

		@Override
		public boolean isIdleNow() {
			return idle;
		}
	}
}
