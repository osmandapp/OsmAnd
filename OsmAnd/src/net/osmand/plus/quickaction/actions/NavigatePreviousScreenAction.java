package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAVIGATE_PREVIOUS_SCREEN_ACTION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class NavigatePreviousScreenAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NAVIGATE_PREVIOUS_SCREEN_ACTION,
			"navigate.previous.screen", NavigatePreviousScreenAction.class)
			.nameActionRes(R.string.quick_action_verb_navigate)
			.nameRes(R.string.quick_action_previous_screen_title)
			.iconRes(R.drawable.ic_action_previous_screen)
			.category(QuickActionType.INTERFACE)
			.nonEditable();

	public NavigatePreviousScreenAction() {
		super(TYPE);
	}

	public NavigatePreviousScreenAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		mapActivity.onBackPressed();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_previous_screen_desc);
		parent.addView(view);
	}

}
