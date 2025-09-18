package net.osmand.test.common;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CustomMatchers {

	private static final Log log = PlatformUtil.getLog(CustomMatchers.class);

	@NonNull
	public static Matcher<View> first(final Matcher<View> matcher) {
		return new TypeSafeMatcher<>() {

			private View first = null;

			@Override
			protected boolean matchesSafely(View view) {
				if (!matcher.matches(view)) {
					return false;
				}
				if (first == null) {
					first = view;
				}
				return view == first;
			}

			@Override
			public void describeTo(Description description) {
				matcher.describeTo(description.appendText("first matching item"));
			}
		};
	}
}