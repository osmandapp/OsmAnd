package net.osmand.plus.common;

import android.view.View;

import org.hamcrest.Matcher;

import androidx.annotation.NonNull;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.util.TreeIterables;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

public class EspressoUtils {

	@NonNull
	public static ViewInteraction waitForView(@NonNull Matcher<View> viewMatcher) throws Throwable {
		return waitForView(viewMatcher, 5000);
	}

	@NonNull
	public static ViewInteraction waitForView(@NonNull Matcher<View> viewMatcher, long waitMillis) throws Throwable {
		long endTime = System.currentTimeMillis() + waitMillis;
		do {
			try {
				onView(isRoot()).perform(searchFor(viewMatcher));
				return onView(viewMatcher);
			} catch (Throwable t) {
				try {
					Thread.sleep(100);
				} catch (Throwable t1) {
				}
			}
		} while (System.currentTimeMillis() < endTime);

		throw new Exception("Failed to find a view in " + waitMillis + " millis matching " + viewMatcher);
	}

	@NonNull
	public static ViewAction searchFor(@NonNull Matcher<View> viewMatcher) {
		return new ViewAction() {
			@Override
			public String getDescription() {
				return "Searching for view " + viewMatcher + " in the root view";
			}

			@Override
			public Matcher<View> getConstraints() {
				return isRoot();
			}

			@Override
			public void perform(UiController uiController, View view) {
				Iterable<View> childViews = TreeIterables.breadthFirstViewTraversal(view);

				for (View child : childViews) {
					if (viewMatcher.matches(child)) {
						return;
					}
				}

				throw new NoMatchingViewException.Builder()
						.withRootView(view)
						.withViewMatcher(viewMatcher)
						.build();
			}
		};
	}

	public static void waitForDataToPerform(@NonNull Matcher<?> dataMatcher,
	                                        @NonNull Matcher<View> adapterViewMather,
	                                        @NonNull ViewAction action) throws Throwable {
		waitForDataToPerform(dataMatcher, adapterViewMather, action, 5000);
	}

	public static void waitForDataToPerform(@NonNull Matcher<?> dataMatcher,
	                                        @NonNull Matcher<View> adapterViewMather,
	                                        @NonNull ViewAction action,
	                                        long waitMillis) throws Throwable {
		long endTime = System.currentTimeMillis() + waitMillis;
		do {
			try {
				onData(dataMatcher).inAdapterView(adapterViewMather).perform(action);
				return;
			} catch (Throwable e) {
				try {
					Thread.sleep(100);
				} catch (Exception exception) {
				}
			}
		} while (System.currentTimeMillis() < endTime);

		throw new Exception("Failed to list item in " + waitMillis + " millis matching " + dataMatcher);
	}
}