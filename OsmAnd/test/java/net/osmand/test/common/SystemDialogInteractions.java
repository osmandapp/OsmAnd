package net.osmand.test.common;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.action.Tapper;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.jetbrains.annotations.NotNull;

public class SystemDialogInteractions {
	private static final long DEFAULT_TIMEOUT_MS = 5000;
	private static final long POLLING_INTERVAL_MS = 100;

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
		return createClickInView(Tap.SINGLE, x, y);
	}

	public static ViewAction longClickInView(final float x, final float y) {
		return createClickInView(Tap.LONG, x, y);
	}

	public static ViewAction doubleClickInView(final float x, final float y) {
		return createClickInView(Tap.DOUBLE, x, y);
	}

	private static ViewAction createClickInView(@NonNull Tapper tapper, final float x, final float y) {
		return new GeneralClickAction(
				tapper,
				view -> {
					final int[] location = new int[2];
					view.getLocationOnScreen(location);
					return new float[] {location[0] + x, location[1] + y};
				},
				Press.FINGER
		);
	}

	//finds n-th child of given type at one hierarchy level
	public static <T extends View> T findDescendantOfType(View parent, Class<T> targetClass, int targetIndex) {
		if (targetIndex < 0) {
			throw new IllegalArgumentException("targetIndex should be 0 or more");
		}
		if (targetClass.isInstance(parent) && targetIndex == 0) {
			return targetClass.cast(parent);
		}

		if (parent instanceof ViewGroup group) {
			int levelTargetIndex = targetIndex;
			for (int i = 0; i < group.getChildCount(); i++) {
				View child = group.getChildAt(i);
				T result = findDescendantOfType(child, targetClass, targetIndex);
				if (result != null) {
					if (levelTargetIndex == 0) {
						return result;
					} else {
						levelTargetIndex--;
					}
				}
			}
		}
		return null;
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

	public static boolean waitForViewDisappeared(long timeoutMs, long pollIntervalMs, @NotNull Matcher<View> matcher) {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeoutMs) {
			try {
				onView(matcher).check(matches(isDisplayed()));
			} catch (NoMatchingViewException | AssertionError e) {
				return true; // View is not displayed
			}

			try {
				Thread.sleep(pollIntervalMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for view", e);
			}
		}
		return false;
	}

	public static boolean isViewVisible(Matcher<View> matcher) {
		try {
			onView(matcher).check(matches(isDisplayed()));
			return true;
		} catch (NoMatchingViewException | AssertionError e) {
			return false;
		}
	}


	public static ViewAssertion hasTextEventually(@NonNull String text) {
		Matcher<String> textMatcher = Matchers.equalTo(text);
		return (view, noViewFoundException) -> {
			if (noViewFoundException != null) {
				throw noViewFoundException;
			}
			if (!(view instanceof TextView)) {
				throw new IllegalArgumentException("The view is not a TextView or a subclass (e.g., EditText). Found: " + view.getClass().getName());
			}
			TextView textView = (TextView) view;
			long startTime = System.currentTimeMillis();
			String currentText = textView.getText().toString();
			while (System.currentTimeMillis() - startTime < DEFAULT_TIMEOUT_MS) {
				if (textMatcher.matches(currentText)) {
					Log.d("Corwin", "found text within " + (System.currentTimeMillis() - startTime) + " ms");
					return;
				}
				try {
					Thread.sleep(POLLING_INTERVAL_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Waiting for text to change was interrupted.", e);
				}
				currentText = textView.getText().toString();
			}

			// If we reach here, the timeout occurred. Throw an assertion error.
			StringDescription description = new StringDescription();
			textMatcher.describeTo(description);
			throw new AssertionError(
					"Waited " + (DEFAULT_TIMEOUT_MS / 1000) + " seconds for view's text to match '" + description +
							"', but found '" + currentText + "'."
			);
		};
	}
}
