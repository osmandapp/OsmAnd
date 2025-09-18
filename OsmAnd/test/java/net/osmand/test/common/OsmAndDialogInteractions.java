package net.osmand.test.common;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.EspressoUtils.waitForView;
import static net.osmand.test.common.Matchers.hasOnClickListener;
import static net.osmand.test.common.SystemDialogInteractions.hasTextEventually;
import static net.osmand.test.common.SystemDialogInteractions.isViewVisible;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringRegularExpression;
import org.hamcrest.core.StringStartsWith;

public class OsmAndDialogInteractions {

	public static void skipAppStartDialogs(@NonNull Context ctx) {
		skipFirstUsageDialog();
		skipNavigationRestoration(ctx);
		skipWhatsNewDialog(ctx);
		skipArrivedDestinationDialog();
	}

	public static void skipFirstUsageDialog() {
		try {
			ViewInteraction skipButton = waitForView(allOf(withId(R.id.skip_button),
					hasOnClickListener(),
					isDisplayed()), 500);
			skipButton.perform(click());
		} catch (Throwable ignore) {
		}
	}

	public static void skipSpeedCamerasBottomSheet() {
		try {
			waitForView(allOf(withId(R.id.button_text),
					withText(R.string.keep_active),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipNavigationRestoration(@NonNull Context ctx) {
		try {
			String template = EspressoUtils.escapeRegex(ctx.getString(R.string.continue_follow_previous_route_auto, "<NUMBER>"));
			String regex = template.replace("<NUMBER>", "\\d+");
			waitForView(allOf(
					withText(StringRegularExpression.matchesRegex(regex)),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipWhatsNewDialog(@NonNull Context ctx) {
		try {
			waitForView(allOf(
					withText(StringStartsWith.startsWith(ctx.getString(R.string.whats_new))),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipArrivedDestinationDialog() {
		try {
			waitForView(allOf(
					withText(R.string.arrived_at_destination),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void clickViewWithText(@StringRes int textId) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withText(textId), isDisplayed()))).perform(click());
	}

	public static void clickViewWithContentDescription(@StringRes int textId) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withContentDescription(textId), isDisplayed()))).perform(click());
	}

	public static void clickViewWithId(@IdRes int id) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withId(id), isDisplayed()))).perform(click());
	}

	public static void clickViewWithText(@NonNull String textId) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withText(textId), isDisplayed()))).perform(click());
	}

	public static void clickMapButtonWithId(@IdRes int id) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withId(id), isDisplayed(), isAssignableFrom(MapButton.class)))).perform(click());
	}

	public static void writeText(@IdRes int id, @NonNull String text) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withId(id), isDisplayed()))).perform(typeText(text));
	}

	public static void checkViewText(@IdRes int id, @NonNull String text) throws Throwable {
		waitForView(withId(id)).check(hasTextEventually(text));
	}

	public static void clearText(@IdRes int id) throws Throwable {
		replaceText(id, "");
	}

	public static void replaceText(@IdRes int id, @NonNull String newText) throws Throwable {
		waitForView(CustomMatchers.first(allOf(withId(id), isDisplayed()))).perform(ViewActions.replaceText(newText));
	}

	public static Matcher<View> isDisplayed() {
		return withEffectiveVisibility(VISIBLE);
	}

	public static boolean isContextMenuOpened() {
		return isViewVisible(withId(R.id.context_menu_layout));
	}

	public static boolean isMultiSelectionMenuOpened() {
		return isViewVisible(withId(R.id.multi_selection_main_view));
	}

	public static void refreshMap(@NonNull OsmandApplication app) {
		app.getOsmandMap().getMapView().refreshMap();
	}

	public static void moveAndZoomMap(@NonNull OsmandApplication app, double latitude,
			double longitude, int zoom) {
		app.getOsmandMap().getMapView().setLatLon(latitude, longitude);
		app.getOsmandMap().getMapView().setIntZoom(zoom);
	}
}
