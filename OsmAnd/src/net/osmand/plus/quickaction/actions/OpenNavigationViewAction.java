package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.OPEN_NAVIGATION_VIEW_ACTION;

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

public class OpenNavigationViewAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(OPEN_NAVIGATION_VIEW_ACTION,
			"navigation.view.showhide", OpenNavigationViewAction.class)
			.nameActionRes(R.string.shared_string_open)
			.nameRes(R.string.quick_action_navigation_view_title)
			.iconRes(R.drawable.ic_action_gdirections_dark)
			.category(QuickActionType.INTERFACE)
			.nonEditable();

	public OpenNavigationViewAction() {
		super(TYPE);
	}

	public OpenNavigationViewAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		mapActivity.getMapActions().doRoute();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_navigation_view_desc);
		parent.addView(view);
	}
}
