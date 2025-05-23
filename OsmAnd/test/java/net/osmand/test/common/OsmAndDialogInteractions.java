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
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringRegularExpression;
import org.hamcrest.core.StringStartsWith;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
public class OsmAndDialogInteractions {

	public static void skipAppStartDialogs(@NonNull Context ctx) {
		skipFirstUsageDialog();
		skipNavigationRestoration(ctx);
		skipWhatsNewDialog(ctx);
		skipArrivedDestinationDialog();
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


	public static void waitForAnyView(long timeoutMs, long pollIntervalMs, Matcher<View>... matchers) {
		long startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() - startTime < timeoutMs) {
			for (Matcher<View> matcher : matchers) {
				try {
					onView(matcher).check(matches(isDisplayed()));
					return; // One of the views is displayed
				} catch (NoMatchingViewException | AssertionError e) {
					// Ignore and try next
				}
			}

			try {
				Thread.sleep(pollIntervalMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for view", e);
			}
		}
	}

	public static boolean isViewVisible(Matcher<View> matcher) {
		try {
			onView(matcher).check(matches(isDisplayed()));
			return true;
		} catch (NoMatchingViewException | AssertionError e) {
			return false;
		}
	}
}
