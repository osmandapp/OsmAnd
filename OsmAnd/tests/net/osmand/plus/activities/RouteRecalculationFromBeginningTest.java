package net.osmand.plus.activities;

import android.os.Handler;
import android.os.Looper;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.common.AndroidTest;
import net.osmand.plus.common.BaseIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.plus.common.EspressoUtils.waitForView;
import static net.osmand.plus.common.Interactions.openNavigationMenu;
import static net.osmand.plus.common.Interactions.setRouteStart;
import static net.osmand.plus.common.Interactions.startNavigation;
import static net.osmand.plus.common.Matchers.childAtPosition;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RouteRecalculationFromBeginningTest extends AndroidTest {

	private static final LatLon START = new LatLon(50.17356, 18.51406);

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule =
			new ActivityScenarioRule<>(MapActivity.class);

	private ObserveDistToFinishIdlingResource observeDistToFinishIdlingResource;

	@Before
	@Override
	public void setup() {
		super.setup();
		IdlingPolicies.setIdlingResourceTimeout(40, TimeUnit.SECONDS);
		enableSimulation(500);
	}

	@After
	public void cleanUp() {
		super.cleanUp();
		if (observeDistToFinishIdlingResource != null) {
			unregisterIdlingResources(observeDistToFinishIdlingResource);
		}
	}

	@Test
	public void test() throws Throwable {
		openNavigationMenu();

		ViewInteraction linearLayout = waitForView(allOf(withId(R.id.map_options_route_button),
				isDisplayed()));
		linearLayout.perform(click());

		ViewInteraction linearLayout2 = onView(
				childAtPosition(
						allOf(withId(R.id.scrollable_items_container),
								childAtPosition(
										withId(R.id.scroll_view),
										0)),
						6));
		linearLayout2.perform(scrollTo(), click());

		ViewInteraction recyclerView = onView(
				allOf(withId(R.id.track_list),
						childAtPosition(
								withId(R.id.prev_route_card),
								1)));
		recyclerView.perform(actionOnItemAtPosition(1, click()));

		ViewInteraction appCompatImageButton2 = onView(
				allOf(withId(R.id.close_button), withContentDescription("Navigate up"),
						childAtPosition(
								childAtPosition(
										withId(R.id.route_menu_top_shadow_all),
										1),
								0),
						isDisplayed()));
		appCompatImageButton2.perform(click());

		setRouteStart(START);
		startNavigation();

		observeDistToFinishIdlingResource = new ObserveDistToFinishIdlingResource(app);
		registerIdlingResources(observeDistToFinishIdlingResource);
		Espresso.onIdle();
	}

	private class ObserveDistToFinishIdlingResource extends BaseIdlingResource {

		private static final int CHECK_INTERVAL = 1000;
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
						if (initialLeftDistance <= IDLE_ON_LEFT_DISTANCE) {
							throw new IllegalStateException("Unexpected route calculated");
						}
					}

					if (leftDistance > initialLeftDistance) {
						throw new AssertionError("Route recalculated from start of the track");
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
