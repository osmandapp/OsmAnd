package net.osmand.test.common;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import net.osmand.plus.search.listitems.QuickSearchListItem;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import androidx.annotation.NonNull;

public class Matchers {

	@NonNull
	public static Matcher<View> hasOnClickListener() {
		return new TypeSafeMatcher<View>() {

			@Override
			protected boolean matchesSafely(View item) {
				return item.hasOnClickListeners();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("view.hasOnClickListeners()");
			}
		};
	}

	@NonNull
	public static Matcher<View> childAtPosition(@NonNull Matcher<View> parentMatcher, int position) {
		return new TypeSafeMatcher<View>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("Child at position " + position + " in parent ");
				parentMatcher.describeTo(description);
			}

			@Override
			public boolean matchesSafely(View view) {
				ViewParent parent = view.getParent();
				return parent instanceof ViewGroup && parentMatcher.matches(parent)
						&& view.equals(((ViewGroup) parent).getChildAt(position));
			}
		};
	}

	@NonNull
	public static Matcher<QuickSearchListItem> searchItemWithLocaleName(@NonNull String localeName) {
		return new TypeSafeMatcher<QuickSearchListItem>() {
			@Override
			protected boolean matchesSafely(QuickSearchListItem item) {
				return item.getSearchResult() != null && localeName.equals(item.getSearchResult().localeName);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("locale name '" + localeName + "'");
			}
		};
	}
}