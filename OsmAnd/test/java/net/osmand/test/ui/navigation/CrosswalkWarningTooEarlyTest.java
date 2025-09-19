package net.osmand.test.ui.navigation;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.test.common.Interactions.openNavigationMenu;
import static net.osmand.test.common.Interactions.startNavigation;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CrosswalkWarningTooEarlyTest extends AndroidTest {

	private static final int SPEED_KM_PER_HOUR = 80;
	private static final LatLon START = new LatLon(45.92051, 35.20653);
	private static final LatLon END = new LatLon(45.91741, 35.21372);

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Before
	@Override
	public void setup() {
		super.setup();
		enableSimulation(SPEED_KM_PER_HOUR);
		try {
			ResourcesImporter.importObfAssets(app, Collections.singletonList("alarm_test.obf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void crosswalkWarningTooEarlyTest() throws Throwable {
		skipAppStartDialogs(app);

		openNavigationMenu();
		app.getTargetPointsHelper().setStartPoint(START, true, null);
		app.getTargetPointsHelper().navigateToPoint(END, true, -1);
		startNavigation();

		RoutingHelper routingHelper = app.getRoutingHelper();
		ViewInteraction alarmWidget = onView(withId(R.id.map_alarm_warning));
		List<TestWidget> testWidgets = Arrays.asList(
				new TestWidget(R.string.traffic_warning_pedestrian, 720, 550),
				new TestWidget(R.string.tunnel_warning, 480, 300),
				new TestWidget(R.string.traffic_warning_pedestrian, 300, 160),
				new TestWidget(R.string.tunnel_warning, 160, 0));
		float delta = SPEED_KM_PER_HOUR / 3.6f;
		TestWidget widget = testWidgets.get(0);
		int id = 0;
		int leftDistance;
		do {
			leftDistance = routingHelper.getLeftDistance();
			if (leftDistance > 0) {
				if (leftDistance > widget.leftDistanceShow + delta) {
					checkAlarmWidgetNotDisplayed(alarmWidget, leftDistance, widget.descriptionId);
				} else if (leftDistance < widget.leftDistanceShow - delta && leftDistance > widget.leftDistanceHide + delta) {
					checkAlarmWidgetDisplayed(alarmWidget, leftDistance, widget.descriptionId);
				}
				if (id < testWidgets.size() && widget.leftDistanceHide > leftDistance) {
					widget = testWidgets.get(id++);
				}
			}

			try {
				Thread.sleep(1000);
			} catch (Exception ignored) {
			}
		} while (leftDistance > 50);
	}

	@After
	public void cleanUp() {
		super.cleanUp();
		app.stopNavigation();
	}

	private void checkAlarmWidgetNotDisplayed(@NonNull ViewInteraction alarmWidget,
			int leftDistance, @IdRes int descriptionId) {
		try {
			alarmWidget.check(matches(not(isDisplayed())));
		} catch (Throwable e) {
			try {
				alarmWidget.check(matches(not(withContentDescription(descriptionId))));
			} catch (Throwable e1) {
				throw new AssertionError(getString(descriptionId) + " alarm was shown too early ("
						+ leftDistance + " m to finish)");
			}
		}
	}

	private void checkAlarmWidgetDisplayed(@NonNull ViewInteraction alarmWidget, int leftDistance,
			@IdRes int descriptionId) {
		try {
			alarmWidget.check(matches(allOf(isDisplayed(), withContentDescription(descriptionId))));
		} catch (Throwable e) {
			throw new AssertionError(getString(descriptionId)
					+ " alarm still was not shown (" + leftDistance + " m to finish)");
		}
	}

	private String getString(int id) {
		return ApplicationProvider.getApplicationContext().getResources().getString(id);
	}

	private static class TestWidget {
		@IdRes
		int descriptionId;
		int leftDistanceShow;
		int leftDistanceHide;

		public TestWidget(int descriptionId, int leftDistanceShow, int leftDistanceHide) {
			this.descriptionId = descriptionId;
			this.leftDistanceShow = leftDistanceShow;
			this.leftDistanceHide = leftDistanceHide;
		}
	}
}