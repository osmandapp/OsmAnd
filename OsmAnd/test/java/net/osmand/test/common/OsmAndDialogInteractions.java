package net.osmand.test.common;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.Matchers.hasOnClickListener;
import static net.osmand.test.common.SystemDialogInteractions.hasTextEventually;
import static net.osmand.test.common.SystemDialogInteractions.isViewVisible;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;

import org.hamcrest.core.StringRegularExpression;
import org.hamcrest.core.StringStartsWith;

public class OsmAndDialogInteractions {

	public static void skipAppStartDialogs(@NonNull Context ctx) {
		skipFirstUsageDialog();
		skipNavigationRestoration(ctx);
		skipWhatsNewDialog(ctx);
		skipArrivedDestinationDialog();
		skipSendAnalyticsDialog();
	}

	public static void skipFirstUsageDialog() {
		try {
			ViewInteraction skipButton = EspressoUtils.waitForView(allOf(withId(R.id.skip_button),
					hasOnClickListener(),
					isDisplayed()), 500);
			skipButton.perform(click());
		} catch (Throwable ignore) {
		}
	}

	public static void skipSendAnalyticsDialog() {
		try {
			ViewInteraction skipButton = EspressoUtils.waitForView(allOf(withId(R.id.dismiss_button),
					hasOnClickListener(),
					isDisplayed()), 500);
			skipButton.perform(click());
		} catch (Throwable ignore) {
		}
	}

	public static void skipSpeedCamerasBottomSheet() {
		try {
			EspressoUtils.waitForView(allOf(withId(R.id.button_text),
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
			EspressoUtils.waitForView(allOf(
					withText(StringRegularExpression.matchesRegex(regex)),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipWhatsNewDialog(@NonNull Context ctx) {
		try {
			EspressoUtils.waitForView(allOf(
					withText(StringStartsWith.startsWith(ctx.getString(R.string.whats_new))),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipArrivedDestinationDialog() {
		try {
			EspressoUtils.waitForView(allOf(
					withText(R.string.arrived_at_destination),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void clickButtonWithText(@StringRes int textId) {
		onView(CustomMatchers.first(allOf(withText(textId), isDisplayed()))).perform(click());
	}

	public static void clickButtonWithContentDescription(@StringRes int textId) {
		onView(CustomMatchers.first(allOf(withContentDescription(textId), isDisplayed()))).perform(click());
	}

	public static void clickButtonWithId(@IdRes int id) {
		onView(CustomMatchers.first(allOf(withId(id), isDisplayed()))).perform(click());
	}

	public static void clickButtonWithText(@NonNull String textId) {
		onView(CustomMatchers.first(allOf(withText(textId), isDisplayed()))).perform(click());
	}

	public static void clickMapButtonWithId(@IdRes int id) {
		onView(CustomMatchers.first(allOf(withId(id), isDisplayed(), isAssignableFrom(MapButton.class)))).perform(click());
	}

	public static void writeText(@IdRes int id, @NonNull String text) {
		onView(CustomMatchers.first(allOf(withId(id), isDisplayed()))).perform(typeText(text));
	}

	public static void checkViewText(@IdRes int id, @NonNull String text) {
		onView(withId(id)).check(hasTextEventually(text));
	}

	public static void clearText(@IdRes int id) {
		replaceText(id, "");
	}

	public static void replaceText(@IdRes int id, @NonNull String newText) {
		onView(CustomMatchers.first(allOf(withId(id), isDisplayed()))).perform(ViewActions.replaceText(newText));
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

	public static void moveAndZoomMap(@NonNull OsmandApplication app, double latitude, double longitude, int zoom) {
		app.getOsmandMap().getMapView().setLatLon(latitude, longitude);
		app.getOsmandMap().getMapView().setIntZoom(zoom);
	}

	public static boolean waitForRenderingIdle(@NonNull OsmandApplication app, boolean isIdle) {
		long startTime = System.currentTimeMillis();
		boolean idleStateReached;
		while ((idleStateReached = isRenderingIdle(app) != isIdle) && System.currentTimeMillis() - startTime < 2000) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException ignored) {
			}
		}
		return idleStateReached;
	}

	public static boolean isRenderingIdle(@NonNull OsmandApplication app) {
		MapRendererView rendererView = app.getOsmandMap().getMapView().getMapRenderer();
		return rendererView == null || !rendererView.isSymbolsLoadingActive();
	}
}
