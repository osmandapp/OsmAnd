package net.osmand.plus.common;

import android.view.View;

import net.osmand.data.LatLon;
import net.osmand.plus.R;

import org.hamcrest.Matcher;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.test.espresso.ViewInteraction;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static net.osmand.plus.common.EspressoUtils.waitForDataToPerform;
import static net.osmand.plus.common.EspressoUtils.waitForView;
import static net.osmand.plus.common.Matchers.childAtPosition;
import static net.osmand.plus.common.Matchers.hasOnClickListener;
import static net.osmand.plus.common.Matchers.searchItemWithLocaleName;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class Interactions {

	public static void openNavigationMenu() throws Throwable {
		ViewInteraction appCompatImageButton = waitForView(allOf(withId(R.id.map_route_info_button),
				hasOnClickListener(),
				isDisplayed()));
		appCompatImageButton.perform(click());
	}

	public static void startNavigation() {
		ViewInteraction frameLayout = onView(
				allOf(withId(R.id.start_button),
						childAtPosition(
								childAtPosition(
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
				allOf(withId(id),
						childAtPosition(
								childAtPosition(
										withId(R.id.route_menu_top_shadow_all),
										1),
								position),
						isDisplayed()));
		linearLayout.perform(click());
	}

	public static void openRoutePointSearch() {
		ViewInteraction linearLayout = onView(
				allOf(withId(R.id.first_item),
						childAtPosition(
								childAtPosition(
										withClassName(is("android.widget.LinearLayout")),
										0),
								0)));
		linearLayout.perform(scrollTo(), click());
	}

	public static void searchCoordinate(@NonNull String coordinate) {
		ViewInteraction appCompatEditText = onView(
				allOf(withId(R.id.searchEditText),
						childAtPosition(
								allOf(withId(R.id.search_container),
										childAtPosition(
												withId(R.id.toolbar),
												0)),
								0),
						isDisplayed()));
		appCompatEditText.perform(replaceText(coordinate), closeSoftKeyboard());
	}

	public static void selectSearchedCoordinate(@NonNull String coordinate) throws Throwable {
		Matcher<View> adapterMatcher = allOf(withId(android.R.id.list),
				childAtPosition(
						allOf(withClassName(is("android.widget.LinearLayout")), withParent(withId(R.id.search_view))),
						0));
		waitForDataToPerform(searchItemWithLocaleName(coordinate), adapterMatcher, click());
	}
}