package net.osmand.test.common;

import android.view.View;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class CustomMatchers {
	public static Matcher<View> first(final Matcher<View> matcher) {
		return new BaseMatcher<>() {
			boolean isFirst = true;

			@Override
			public boolean matches(final Object item) {
				if (isFirst && matcher.matches(item)) {
					isFirst = false;
					return true;
				}
				return false;
			}

			@Override
			public void describeTo(final Description description) {
				description.appendText("first matching item");
			}
		};
	}
}