package net.osmand.test.common;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import net.osmand.plus.search.listitems.QuickSearchListItem;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.test.espresso.matcher.BoundedMatcher;

public class Matchers {

	@NonNull
	public static Matcher<View> hasOnClickListener() {
		return new TypeSafeMatcher<>() {

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
		return new TypeSafeMatcher<>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("Child at position " + position + " in parent ");
				parentMatcher.describeTo(description);
			}

			@Override
			public boolean matchesSafely(View view) {
				final ViewParent parent = view.getParent();
				return parent instanceof final ViewGroup _parent
						&& parentMatcher.matches(parent)
						&& view.equals(_parent.getChildAt(position));
			}
		};
	}

	@NonNull
	public static Matcher<QuickSearchListItem> searchItemWithLocaleName(@NonNull String localeName) {
		return new TypeSafeMatcher<>() {
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

	// adapted from https://stackoverflow.com/a/53289078/12982352
	public static Matcher<View> recyclerViewHasItem(final Matcher<View> matcher) {
		return new BoundedMatcher<>(RecyclerView.class) {

			@Override
			public void describeTo(Description description) {
				description.appendText("has item: ");
				matcher.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(final RecyclerView view) {
				final Adapter adapter = view.getAdapter();
				for (int position = 0; position < adapter.getItemCount(); position++) {
					final int type = adapter.getItemViewType(position);
					final ViewHolder holder = adapter.createViewHolder(view, type);
					adapter.onBindViewHolder(holder, position);
					if (matcher.matches(holder.itemView)) {
						return true;
					}
				}
				return false;
			}
		};
	}
}