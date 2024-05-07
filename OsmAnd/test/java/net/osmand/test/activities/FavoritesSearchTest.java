package net.osmand.test.activities;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.Matchers.childAtPosition;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FavoritesSearchTest extends AndroidTest {

    // assert that https://github.com/osmandapp/OsmAnd/issues/19242 is fixed
    @Test
    public void assertIssue19242IsFixed() {
        final ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class);
        scenario.onActivity(this::importFavorites);
        final ViewInteraction appCompatImageButton =
                onView(
                        allOf(
                                withId(R.id.map_menu_button),
                                withContentDescription(R.string.backToMenu),
                                childAtPosition(
                                        childAtPosition(
                                                withId(R.id.bottom_controls_container),
                                                3),
                                        0),
                                isDisplayed()));
        appCompatImageButton.perform(click());

        final DataInteraction linearLayout =
                onData(anything())
                        .inAdapterView(
                                allOf(
                                        withId(R.id.menuItems),
                                        childAtPosition(
                                                withId(R.id.drawer_relative_layout),
                                                0)))
                        .atPosition(3);
        linearLayout.perform(click());

        final ViewInteraction actionMenuItemView =
                onView(
                        allOf(
                                withContentDescription("Filter"),
                                childAtPosition(
                                        childAtPosition(
                                                withId(me.zhanghai.android.materialprogressbar.R.id.action_bar),
                                                3),
                                        0),
                                isDisplayed()));
        actionMenuItemView.perform(click());

        final ViewInteraction appCompatEditText =
                onView(
                        allOf(
                                withId(R.id.searchEditText),
                                childAtPosition(
                                        allOf(
                                                withId(R.id.search_container),
                                                childAtPosition(
                                                        withId(R.id.toolbar),
                                                        0)),
                                        0),
                                isDisplayed()));
        appCompatEditText.perform(replaceText("Morgenstelle"), closeSoftKeyboard());

        final DataInteraction linearLayout2 =
                onData(anything())
                        .inAdapterView(
                                allOf(
                                        withId(android.R.id.list),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)))
                        .atPosition(2);
        linearLayout2.perform(click());

        pressBack();

        pressBack();

        final ViewInteraction editText =
                onView(
                        allOf(
                                withId(R.id.searchEditText), withText("Morgenstelle"),
                                withParent(
                                        allOf(
                                                withId(R.id.search_container),
                                                withParent(withId(R.id.toolbar)))),
                                isDisplayed()));
        editText.check(matches(withText("Morgenstelle")));
    }

    private void importFavorites(final FragmentActivity activity) {
        try {
            ResourcesImporter.importFavorite(new File("favorites.gpx"), app, activity);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
