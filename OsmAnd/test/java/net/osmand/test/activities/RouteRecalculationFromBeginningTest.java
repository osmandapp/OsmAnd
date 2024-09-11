package net.osmand.test.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.EspressoUtils.waitForView;
import static net.osmand.test.common.Interactions.openNavigationMenu;
import static net.osmand.test.common.Interactions.setRouteStart;
import static net.osmand.test.common.Interactions.startNavigation;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;

import android.os.Handler;
import android.os.Looper;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;
import net.osmand.test.common.ResourcesImporter;

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

	@Before
	@Override
	public void setup() {
		super.setup();
		enableSimulation(SPEED_KM_PER_HOUR);
		try {
			ResourcesImporter.importGpxAssets(app, Collections.singletonList(SELECTED_GPX_NAME), null);
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
	}

	@Test
	public void test() throws Throwable {
		skipAppStartDialogs(app);

		openNavigationMenu();

		ViewInteraction linearLayout = waitForView(allOf(withId(R.id.map_options_route_button), isDisplayed()));
		linearLayout.perform(click());

		ViewInteraction linearLayout2 = onView(
				childAtPosition(
						allOf(withId(R.id.scrollable_items_container),
								childAtPosition(
										withId(R.id.scroll_view),
										0)),
						6));
		linearLayout2.perform(scrollTo(), click());

		ViewInteraction allTab = onView(allOf(withId(android.R.id.text1),
				withText(app.getString(TrackTabType.ALL.titleId))));
		allTab.perform(click());

		ViewInteraction trackItemView = onView(allOf(withId(R.id.title),
				withText(GpxHelper.INSTANCE.getGpxTitle(SELECTED_GPX_NAME)), isDisplayed()));
		trackItemView.perform(click());

		ViewInteraction closeButton = onView(allOf(withId(R.id.close_button), isDisplayed()));
		closeButton.perform(click());

		setRouteStart(START);
		startNavigation();

		idlingResource = new ObserveDistToFinishIdlingResource(app);
		registerIdlingResources(idlingResource);

		Espresso.onIdle();
	}

	private static class ObserveDistToFinishIdlingResource extends BaseIdlingResource {

		private static final int CHECK_INTERVAL = 1000;
		private static final Range<Integer> EXPECTED_ROUTE_LENGTH = new Range<>(7000, 7200);
		private static final int IDLE_ON_LEFT_DISTANCE = 5900;

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
					}

					if (initialLeftDistance == -1) {
						initialLeftDistance = leftDistance;
						if (EXPECTED_ROUTE_LENGTH.contains(initialLeftDistance)) {
							throw new IllegalStateException("Unexpected route calculated with distance " + initialLeftDistance + " m");
						}
					}

					if (leftDistance > initialLeftDistance) {
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
