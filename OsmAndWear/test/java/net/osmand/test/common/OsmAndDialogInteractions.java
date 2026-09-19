package net.osmand.test.common;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.Matchers.hasOnClickListener;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;

import net.osmand.plus.R;

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
			ViewInteraction skipButton = EspressoUtils.waitForView(org.hamcrest.Matchers.allOf(withId(R.id.skip_button),
					hasOnClickListener(),
					isDisplayed()), 500);
			skipButton.perform(click());
		} catch (Throwable ignore) {
		}
	}

	public static void skipSpeedCamerasBottomSheet() {
		try {
			EspressoUtils.waitForView(org.hamcrest.Matchers.allOf(withId(R.id.button_text),
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
			EspressoUtils.waitForView(org.hamcrest.Matchers.allOf(
					withText(StringRegularExpression.matchesRegex(regex)),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipWhatsNewDialog(@NonNull Context ctx) {
		try {
			EspressoUtils.waitForView(org.hamcrest.Matchers.allOf(
					withText(StringStartsWith.startsWith(ctx.getString(R.string.whats_new))),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}

	public static void skipArrivedDestinationDialog() {
		try {
			EspressoUtils.waitForView(org.hamcrest.Matchers.allOf(
					withText(R.string.arrived_at_destination),
					isDisplayed()), 500);
			Espresso.pressBack();
		} catch (Throwable ignore) {
		}
	}
}
