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
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.DataInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;

import org.hamcrest.Matcher;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FavoritesSearchTest extends AndroidTest {

    @Test
    public void assertIssue19242IsFixed_a_prepare() {
        try (final ActivityScenario<MapActivity> activityScenario = ActivityScenario.launch(MapActivity.class)) {
            activityScenario.onActivity(activity -> importFavorite(new File("favorites.gpx"), activity));
        }
    }

    // assert that https://github.com/osmandapp/OsmAnd/issues/19242 is fixed
    @Test
    public void assertIssue19242IsFixed_b() {
        // Given
        final String favorite = "Morgenstelle";
        ActivityScenario.launch(MapActivity.class);
        skipAppStartDialogs(app);
        navigateToFavoritesSearchUI();

        // When
        onView(searchQueryTextField()).perform(replaceText(favorite), closeSoftKeyboard()); // results in UI state A
        displayFavoriteOnMap();
        pressBack();
        pressBack();

        // Then UI state A is restored
        onView(searchQueryTextField()).check(matches(withText(favorite)));
    }

    private static void navigateToFavoritesSearchUI() {
        onView(mapMenu()).perform(click());
        myPlaces().perform(click());
        onView(search()).perform(click());
    }

    private static Matcher<View> mapMenu() {
        return allOf(
                withId(R.id.map_menu_button),
                withContentDescription(R.string.backToMenu),
                childAtPosition(
                        childAtPosition(
                                withId(R.id.bottom_controls_container),
                                3),
                        0),
                isDisplayed());
    }

    private static DataInteraction myPlaces() {
        return onData(anything())
                .inAdapterView(
                        allOf(
                                withId(R.id.menuItems),
                                childAtPosition(
                                        withId(R.id.drawer_relative_layout),
                                        0)))
                .atPosition(3);
    }

    private static Matcher<View> search() {
        return allOf(
                withContentDescription("Filter"),
                childAtPosition(
                        childAtPosition(
                                withId(me.zhanghai.android.materialprogressbar.R.id.action_bar),
                                3),
                        0),
                isDisplayed());
    }

    private static Matcher<View> searchQueryTextField() {
        return allOf(
                withId(R.id.searchEditText),
                withParent(
                        allOf(
                                withId(R.id.search_container),
                                withParent(withId(R.id.toolbar)))),
                isDisplayed());
    }

    private static void displayFavoriteOnMap() {
        final DataInteraction favorite =
                onData(anything())
                        .inAdapterView(
                                allOf(
                                        withId(android.R.id.list),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)))
                        .atPosition(2);
        favorite.perform(click());
    }

    private void importFavorite(final File favoriteAssetFile, final FragmentActivity activity) {
        try {
            ResourcesImporter.importFavorite(favoriteAssetFile, app, activity);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
