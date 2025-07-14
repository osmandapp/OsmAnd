package net.osmand.test.common.actions;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

public class GetViewAction implements ViewAction {
	private View[] textHolder;

	public GetViewAction(View[] textHolder) {
		this.textHolder = textHolder;
	}

	@Override
	public Matcher<View> getConstraints() {
		return isAssignableFrom(View.class);
	}

	@Override
	public String getDescription() {
		return "Get view";
	}

	@Override
	public void perform(UiController uiController, View view) {
		textHolder[0] = view;
	}
}
