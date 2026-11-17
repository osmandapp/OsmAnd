package net.osmand.test.common;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static net.osmand.test.common.OsmAndDialogInteractions.skipSpeedCamerasBottomSheet;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.test.espresso.ViewInteraction;

import net.osmand.data.LatLon;
import net.osmand.plus.R;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class Interactions {

	public static void openNavigationMenu() throws Throwable {
		ViewInteraction appCompatImageButton = EspressoUtils.waitForView(Matchers.allOf(withId(R.id.map_route_info_button),
				net.osmand.test.common.Matchers.hasOnClickListener(),
				isDisplayed()));
		appCompatImageButton.perform(click());

		skipSpeedCamerasBottomSheet();
	}

	public static void startNavigation() throws Throwable {
		ViewInteraction frameLayout = EspressoUtils.waitForView(
				Matchers.allOf(withId(R.id.start_button),
						net.osmand.test.common.Matchers.childAtPosition(
								net.osmand.test.common.Matchers.childAtPosition(
										withId(R.id.control_buttons),
										1),
								2),
						isDisplayed()));
		frameLayout.perform(click());
	}

	public static void setRouteStart(@NonNull LatLon start) throws Throwable {
		setRouteStartEnd(start, true);
	}

	public static void setRouteEnd(@NonNull LatLon end) throws Throwable {
		setRouteStartEnd(end, false);
	}

	private static void setRouteStartEnd(@NonNull LatLon latLon, boolean start) throws Throwable {
		String coordinate = latLon.getLatitude() + ", " + latLon.getLongitude();
		openRouteStartEndDialog(start);
		openRoutePointSearch();
		searchCoordinate(coordinate);
		selectSearchedCoordinate(coordinate);
	}

	private static void openRouteStartEndDialog(boolean start) {
		@IdRes
		int id = start ? R.id.FromLayout : R.id.ToLayout;
		int position = start ? 0 : 4;
		ViewInteraction linearLayout = onView(
				Matchers.allOf(withId(id),
						net.osmand.test.common.Matchers.childAtPosition(
								net.osmand.test.common.Matchers.childAtPosition(
										withId(R.id.route_menu_top_shadow_all),
										1),
								position),
						isDisplayed()));
		linearLayout.perform(click());
	}

	public static void openRoutePointSearch() {
		ViewInteraction linearLayout = onView(
				Matchers.allOf(withId(R.id.first_item),
						net.osmand.test.common.Matchers.childAtPosition(
								net.osmand.test.common.Matchers.childAtPosition(
										withClassName(is("android.widget.LinearLayout")),
										0),
								0)));
		linearLayout.perform(scrollTo(), click());
	}

	public static void searchCoordinate(@NonNull String coordinate) {
		ViewInteraction appCompatEditText = onView(
				Matchers.allOf(withId(R.id.searchEditText),
						net.osmand.test.common.Matchers.childAtPosition(
								Matchers.allOf(withId(R.id.search_container),
										net.osmand.test.common.Matchers.childAtPosition(
												withId(R.id.toolbar),
												0)),
								0),
						isDisplayed()));
		appCompatEditText.perform(replaceText(coordinate), closeSoftKeyboard());
	}

	public static void selectSearchedCoordinate(@NonNull String coordinate) throws Throwable {
		Matcher<View> adapterMatcher = Matchers.allOf(withId(android.R.id.list),
				net.osmand.test.common.Matchers.childAtPosition(
						allOf(withClassName(is("android.widget.LinearLayout")), withParent(withId(R.id.search_view))),
						0));
		EspressoUtils.waitForDataToPerform(net.osmand.test.common.Matchers.searchItemWithLocaleName(coordinate), adapterMatcher, click());
	}
}