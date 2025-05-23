package net.osmand.test.common;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.Espresso.onView;


import net.osmand.plus.R;

import org.hamcrest.Matcher;

public class SystemDialogInteractions {

	public static View getViewById(@IdRes int id) {
		final View[] capturedView = new View[1];
		onView(withId(id)).perform(new ViewAction() {
			@Override
			public Matcher<View> getConstraints() {
				return isAssignableFrom(View.class); // or use any matcher suitable
			}

			@Override
			public String getDescription() {
				return "Capture view reference";
			}

			@Override
			public void perform(UiController uiController, View view) {
				capturedView[0] = view;
			}
		});
		return capturedView[0];
	}

	public static ViewAction clickInView(final float x, final float y) {
		return new GeneralClickAction(
				Tap.SINGLE,
				view -> {
					final int[] location = new int[2];
					view.getLocationOnScreen(location);
					return new float[] {location[0] + x, location[1] + y};
				},
				Press.FINGER
		);
	}

}
