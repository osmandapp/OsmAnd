package net.osmand.test.activities;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.osmand.test.common.Interactions.openNavigationMenu;
import static net.osmand.test.common.Interactions.setRouteEnd;
import static net.osmand.test.common.Interactions.setRouteStart;
import static net.osmand.test.common.Interactions.startNavigation;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import androidx.annotation.NonNull;
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CrosswalkWarningTooEarlyTest extends AndroidTest {

	private static final LatLon START = new LatLon(45.92051, 35.20653);
	private static final LatLon END = new LatLon(45.91882, 35.20774);

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule =
			new ActivityScenarioRule<>(MapActivity.class);

    @Before
    @Override
    public void setup() {
        super.setup();
        enableSimulation(80);
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
		setRouteStart(START);
		setRouteEnd(END);
        startNavigation();

		RoutingHelper routingHelper = app.getRoutingHelper();
		do {
			int leftDistance = routingHelper.getLeftDistance();
			if (leftDistance > 0) {
				if (leftDistance < 50) {
					break;
				}
				ViewInteraction alarmWidget = onView(withId(R.id.map_alarm_warning));
				if (leftDistance > 200) {
					checkCrosswalkAlarmNotDisplayed(alarmWidget, leftDistance);
				} else if (leftDistance < 150) {
					checkCrosswalkAlarmDisplayed(alarmWidget, leftDistance);
					break;
				}
			}

			try {
				Thread.sleep(1000);
			} catch (Exception ignored) {
			}
		} while (true);
	}

	private void checkCrosswalkAlarmNotDisplayed(@NonNull ViewInteraction alarmWidget, int leftDistance) {
		try {
			alarmWidget.check(matches(not(isDisplayed())));
		} catch (Throwable e) {
			try {
				alarmWidget.check(matches(not(withContentDescription(R.string.traffic_warning_pedestrian))));
			} catch (Throwable e1) {
				throw new AssertionError("Crosswalk alarm was shown too early (" + leftDistance + " m to finish)");
			}
		}
	}

	private void checkCrosswalkAlarmDisplayed(@NonNull ViewInteraction alarmWidget, int leftDistance) {
		try {
			alarmWidget.check(matches(allOf(isDisplayed(), withContentDescription(R.string.traffic_warning_pedestrian))));
		} catch (Throwable e) {
			throw new AssertionError("Crosswalk alarm still was not shown (" + leftDistance + " m to finish)");
		}
	}
}